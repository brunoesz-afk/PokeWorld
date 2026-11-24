"use strict";
/**
 * Carteira de pontos (1 real = 1 ponto) + depósito por Pix via Mercado Pago.
 *
 * Regras de segurança:
 *  - o saldo mora aqui (Postgres em produção), nunca no aparelho;
 *  - ponto só é creditado depois que o servidor CONSULTA o pagamento na API do Mercado Pago com o
 *    access token e vê status "approved", valor e referência corretos (o webhook é só um aviso);
 *  - cada pagamento credita uma única vez (ref única no livro-caixa), mesmo com webhook repetido;
 *  - o servidor só consulta pagamentos que ele mesmo criou.
 */
const fs = require("fs");
const crypto = require("crypto");

const PACKS = [5, 10, 20, 35, 50, 100];                 // pontos == reais
const ITEM_RE = /^(vip|pass:\d{6}|skin:[A-Za-z0-9_-]{1,40})$/;
const FINAL_BAD = ["rejected", "cancelled", "refunded", "charged_back", "expired"];
const now = () => Date.now();

// ---------------------------------------------------------------------------------------------
// Armazenamento em arquivo (desenvolvimento / testes). Em hospedagem gratuita o disco some: use Postgres.
// ---------------------------------------------------------------------------------------------
class FileStore {
    constructor(file) {
        this.file = file;
        this.d = { accounts: {}, wallets: {}, ledger: [], refs: {}, payments: {} };
        try { this.d = { ...this.d, ...JSON.parse(fs.readFileSync(file, "utf8")) }; } catch (e) { /* novo */ }
    }
    _save() {
        const tmp = this.file + ".tmp";
        fs.writeFileSync(tmp, JSON.stringify(this.d));
        fs.renameSync(tmp, this.file);
    }
    async init() {}
    async auth(code, hash) {
        const a = this.d.accounts[code];
        if (!a) { this.d.accounts[code] = hash; this._save(); return true; }
        return a === hash;
    }
    async balance(code) { return this.d.wallets[code] || 0; }
    async credit(code, points, ref) {
        if (ref && this.d.refs[ref]) return { dup: true, balance: this.d.wallets[code] || 0 };
        if (ref) this.d.refs[ref] = 1;
        this.d.wallets[code] = (this.d.wallets[code] || 0) + points;
        this.d.ledger.push({ code, delta: points, kind: "credit", ref: ref || null, at: now() });
        this._save();
        return { dup: false, balance: this.d.wallets[code] };
    }
    async spend(code, points, item) {
        const bal = this.d.wallets[code] || 0;
        if (points <= 0 || bal < points) return { ok: false, balance: bal };
        this.d.wallets[code] = bal - points;
        this.d.ledger.push({ code, delta: -points, kind: "spend:" + item, ref: null, at: now() });
        this._save();
        return { ok: true, balance: this.d.wallets[code] };
    }
    async items(code) {
        return this.d.ledger.filter((l) => l.code === code && l.kind.startsWith("spend:"))
            .map((l) => ({ item: l.kind.slice(6), at: l.at }));
    }
    async savePayment(p) { this.d.payments[p.id] = { ...p }; this._save(); }
    async getPayment(id) { return this.d.payments[id] ? { ...this.d.payments[id] } : null; }
    async setStatus(id, status) { if (this.d.payments[id]) { this.d.payments[id].status = status; this._save(); } }
    async openPayments(code, since, limit) {
        return Object.values(this.d.payments)
            .filter((p) => p.status === "pending" && p.created > since && (!code || p.code === code))
            .sort((a, b) => b.created - a.created).slice(0, limit).map((p) => ({ ...p }));
    }
    async countPayments(code, since) {
        return Object.values(this.d.payments).filter((p) => p.code === code && p.created > since).length;
    }
}

// ---------------------------------------------------------------------------------------------
// Postgres. Cada operação é UMA instrução SQL (CTE), portanto atômica sem precisar de transação explícita.
// `db` é qualquer objeto com query(text, params) -> {rows} (o Pool do pacote "pg").
// ---------------------------------------------------------------------------------------------
class PgStore {
    constructor(db) { this.db = db; }
    async init() {
        const q = (t) => this.db.query(t);
        await q("create table if not exists w_accounts (code text primary key, hash text not null)");
        await q("create table if not exists w_wallets (code text primary key, points integer not null default 0 check (points >= 0))");
        await q("create table if not exists w_ledger (id bigserial primary key, code text not null, delta integer not null, kind text not null, ref text unique, at bigint not null)");
        await q("create index if not exists w_ledger_code on w_ledger (code)");
        await q("create table if not exists w_payments (id text primary key, code text not null, points integer not null, status text not null, created bigint not null)");
        await q("create index if not exists w_payments_code on w_payments (code, created)");
    }
    async auth(code, hash) {
        await this.db.query("insert into w_accounts (code, hash) values ($1::text, $2::text) on conflict (code) do nothing", [code, hash]);
        const r = await this.db.query("select hash from w_accounts where code = $1::text", [code]);
        return r.rows.length > 0 && r.rows[0].hash === hash;
    }
    async balance(code) {
        const r = await this.db.query("select points from w_wallets where code = $1::text", [code]);
        return r.rows.length ? Number(r.rows[0].points) : 0;
    }
    async credit(code, points, ref) {
        const r = await this.db.query(
            "with ins as (insert into w_ledger (code, delta, kind, ref, at) values ($1::text, $2::int, 'credit', $3::text, $4::bigint) on conflict (ref) do nothing returning 1), " +
            "upd as (insert into w_wallets (code, points) select $1::text, $2::int from ins on conflict (code) do update set points = w_wallets.points + excluded.points returning points) " +
            "select (select count(*) from ins) as inserted, coalesce((select points from upd), (select points from w_wallets where code = $1::text), 0) as balance",
            [code, points, ref || null, now()]);
        return { dup: Number(r.rows[0].inserted) === 0, balance: Number(r.rows[0].balance) };
    }
    async spend(code, points, item) {
        const r = await this.db.query(
            "with upd as (update w_wallets set points = points - $2::int where code = $1::text and points >= $2::int returning points), " +
            "ins as (insert into w_ledger (code, delta, kind, ref, at) select $1::text, -$2::int, $3::text, null, $4::bigint from upd returning 1) " +
            "select (select count(*) from upd) as ok, coalesce((select points from upd), (select points from w_wallets where code = $1::text), 0) as balance",
            [code, points, "spend:" + item, now()]);
        return { ok: Number(r.rows[0].ok) === 1, balance: Number(r.rows[0].balance) };
    }
    async items(code) {
        const r = await this.db.query("select kind, at from w_ledger where code = $1::text and kind like 'spend:%' order by id", [code]);
        return r.rows.map((x) => ({ item: String(x.kind).slice(6), at: Number(x.at) }));
    }
    async savePayment(p) {
        await this.db.query("insert into w_payments (id, code, points, status, created) values ($1::text, $2::text, $3::int, $4::text, $5::bigint) on conflict (id) do nothing",
            [String(p.id), p.code, p.points, p.status, p.created]);
    }
    _pay(x) { return x ? { id: x.id, code: x.code, points: Number(x.points), status: x.status, created: Number(x.created) } : null; }
    async getPayment(id) {
        const r = await this.db.query("select id, code, points, status, created from w_payments where id = $1::text", [String(id)]);
        return this._pay(r.rows[0]);
    }
    async setStatus(id, status) { await this.db.query("update w_payments set status = $2::text where id = $1::text", [String(id), status]); }
    async openPayments(code, since, limit) {
        const r = await this.db.query(
            "select id, code, points, status, created from w_payments where status = 'pending' and created > $1::bigint and ($2::text is null or code = $2::text) order by created desc limit $3::int",
            [since, code || null, limit]);
        return r.rows.map((x) => this._pay(x));
    }
    async countPayments(code, since) {
        const r = await this.db.query("select count(*) as n from w_payments where code = $1::text and created > $2::bigint", [code, since]);
        return Number(r.rows[0].n);
    }
}

// ---------------------------------------------------------------------------------------------
// Cliente mínimo da API do Mercado Pago (fetch nativo do Node 18+).
// ---------------------------------------------------------------------------------------------
function makeMp(accessToken, base) {
    base = base || "https://api.mercadopago.com";
    async function call(method, path, body, extra) {
        const res = await fetch(base + path, {
            method,
            headers: { Authorization: "Bearer " + accessToken, "Content-Type": "application/json", Accept: "application/json", ...(extra || {}) },
            body: body ? JSON.stringify(body) : undefined,
        });
        const text = await res.text();
        let json = null;
        try { json = JSON.parse(text); } catch (e) { /* resposta não-JSON */ }
        if (!res.ok) {
            const err = new Error("MP " + res.status + ": " + (json && (json.message || json.error) || text).toString().slice(0, 200));
            err.status = res.status;
            throw err;
        }
        return json;
    }
    return {
        async createPayment({ amount, description, email, externalRef, notificationUrl, idempotencyKey }) {
            const body = {
                transaction_amount: amount,
                description,
                payment_method_id: "pix",
                payer: { email },
                external_reference: externalRef,
            };
            if (notificationUrl) body.notification_url = notificationUrl;
            const p = await call("POST", "/v1/payments", body, { "X-Idempotency-Key": idempotencyKey });
            const t = (p.point_of_interaction && p.point_of_interaction.transaction_data) || {};
            return { id: String(p.id), status: p.status, qr_code: t.qr_code || "", qr_base64: t.qr_code_base64 || "", ticket_url: t.ticket_url || "" };
        },
        async getPayment(id) {
            const p = await call("GET", "/v1/payments/" + encodeURIComponent(id));
            return { id: String(p.id), status: p.status, amount: Number(p.transaction_amount), externalRef: p.external_reference == null ? "" : String(p.external_reference) };
        },
    };
}

// ---------------------------------------------------------------------------------------------
// Regras da carteira
// ---------------------------------------------------------------------------------------------
function createWallet({ store, mp, publicUrl, webhookSecret, log }) {
    log = log || console;
    const DAY = 86400000;

    async function summary(code) {
        const [balance, items, pending] = await Promise.all([store.balance(code), store.items(code), store.openPayments(code, now() - DAY, 5)]);
        return { ok: true, balance, items: items.slice(-200), pending: pending.map((p) => ({ id: p.id, points: p.points })) };
    }

    async function createPix(code, points) {
        if (!mp) return { ok: false, error: "Depósito indisponível no momento." };
        points = parseInt(points, 10);
        if (!PACKS.includes(points)) return { ok: false, error: "Valor inválido." };
        if ((await store.countPayments(code, now() - 3600000)) >= 8) return { ok: false, error: "Muitos depósitos na última hora. Tente mais tarde." };
        if ((await store.openPayments(code, now() - DAY, 10)).length >= 3) return { ok: false, error: "Você já tem Pix em aberto. Pague ou aguarde expirar." };
        let p;
        try {
            p = await mp.createPayment({
                amount: points,
                description: "PokeWorld - " + points + " pontos",
                email: code.toLowerCase() + "@pokeworld.game",
                externalRef: code,
                notificationUrl: publicUrl ? publicUrl.replace(/\/$/, "") + "/mp/webhook" : undefined,
                idempotencyKey: crypto.randomUUID(),
            });
        } catch (e) {
            log.error("[pix] erro ao criar:", e.message);
            return { ok: false, error: "Não foi possível gerar o Pix agora." };
        }
        if (!p.qr_code) return { ok: false, error: "O Mercado Pago não devolveu o QR Code." };
        await store.savePayment({ id: p.id, code, points, status: "pending", created: now() });
        return { ok: true, id: p.id, points, qr_code: p.qr_code, qr_base64: p.qr_base64, ticket_url: p.ticket_url };
    }

    /** Consulta o pagamento no Mercado Pago e credita (uma única vez) se estiver aprovado. */
    async function settle(id) {
        id = String(id);
        const rec = await store.getPayment(id);
        if (!rec) return { ok: false, error: "Pagamento desconhecido." };
        if (rec.status === "approved") return { ok: true, status: "approved", credited: false, balance: await store.balance(rec.code) };
        if (!mp) return { ok: false, error: "Indisponível." };
        let m;
        try { m = await mp.getPayment(id); } catch (e) { log.error("[pix] erro ao consultar", id, e.message); return { ok: false, error: "Não foi possível consultar o pagamento." }; }
        if (m.externalRef !== rec.code || Math.round(m.amount * 100) !== rec.points * 100) {
            log.error("[pix] pagamento", id, "não confere (ref/valor); ignorado");
            await store.setStatus(id, "invalid");
            return { ok: false, error: "Pagamento não confere." };
        }
        if (m.status === "approved") {
            const r = await store.credit(rec.code, rec.points, "mp:" + id);
            await store.setStatus(id, "approved");
            if (!r.dup) log.log("[pix] +" + rec.points + " pontos para " + rec.code + " (pagamento " + id + ")");
            return { ok: true, status: "approved", credited: !r.dup, balance: r.balance };
        }
        if (FINAL_BAD.includes(m.status)) await store.setStatus(id, m.status);
        return { ok: true, status: m.status, credited: false, balance: await store.balance(rec.code) };
    }

    async function checkPix(code, id) {
        const rec = await store.getPayment(String(id));
        if (!rec || rec.code !== code) return { ok: false, error: "Pagamento não encontrado." };
        return settle(id);
    }

    async function spend(code, points, item) {
        points = parseInt(points, 10);
        item = String(item || "");
        if (!(points >= 1 && points <= 200) || !ITEM_RE.test(item)) return { ok: false, error: "Compra inválida." };
        const r = await store.spend(code, points, item);
        if (!r.ok) return { ok: false, error: "Saldo insuficiente.", balance: r.balance };
        return { ok: true, balance: r.balance };
    }

    /** Verifica (sem rejeitar) o x-signature do webhook; a segurança de verdade é a consulta à API. */
    function signatureLooksValid(headers, dataId) {
        if (!webhookSecret) return null;
        try {
            const sig = String(headers["x-signature"] || ""), reqId = String(headers["x-request-id"] || "");
            const parts = Object.fromEntries(sig.split(",").map((s) => s.trim().split("=")));
            if (!parts.ts || !parts.v1) return false;
            const id = /^[a-z0-9]+$/i.test(String(dataId)) ? String(dataId).toLowerCase() : String(dataId);
            const manifest = "id:" + id + ";request-id:" + reqId + ";ts:" + parts.ts + ";";
            const calc = crypto.createHmac("sha256", webhookSecret).update(manifest).digest("hex");
            return calc === parts.v1;
        } catch (e) { return false; }
    }

    /** Chamado pela rota /mp/webhook. Responde rápido; o crédito acontece depois, após consultar a API. */
    async function onWebhook({ query, body, headers }) {
        const type = (body && body.type) || (query && query.type) || "";
        const id = (query && (query["data.id"] || query.id)) || (body && body.data && body.data.id);
        if (!id || (type && type !== "payment")) return { handled: false };
        const sigOk = signatureLooksValid(headers || {}, id);
        if (sigOk === false) log.warn("[pix] assinatura do webhook não bateu (id " + id + "); confirmando direto na API");
        const r = await settle(id);
        return { handled: true, result: r };
    }

    /** Varredura: confere Pix em aberto (webhook perdido, app fechado). */
    async function sweep() {
        const open = await store.openPayments(null, now() - 2 * DAY, 30);
        for (const p of open) {
            try {
                if (now() - p.created > DAY + 3600000) { await store.setStatus(p.id, "expired"); continue; }
                await settle(p.id);
            } catch (e) { log.error("[pix] sweep:", e.message); }
        }
    }

    return { PACKS, store, summary, createPix, checkPix, settle, spend, onWebhook, sweep, signatureLooksValid };
}

module.exports = { createWallet, FileStore, PgStore, makeMp, PACKS, ITEM_RE };
