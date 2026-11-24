const express = require("express");
const http = require("http");
const fs = require("fs");
const path = require("path");
const crypto = require("crypto");
const { Server } = require("socket.io");
const { createWallet, FileStore, PgStore, makeMp } = require("./wallet");

const app = express();
const server = http.createServer(app);
const io = new Server(server, {
    cors: {
        origin: "*",
        methods: ["GET", "POST"]
    }
});

const PORT = process.env.PORT || 3000;
const players = {};   // socket.id -> estado visível no mapa
const online = {};    // código de amigo -> socket.id

// ---------------------------------------------------------------------------
// AMIGOS: cada jogador tem um código (8 letras/números) + um segredo gerado no aparelho.
// O servidor guarda só o hash do segredo (impede que outra pessoa se passe por você),
// o nome, os pedidos pendentes e uma caixa de entrada para quem estava offline.
// Salvo em arquivo: em hospedagem gratuita (disco efêmero) o arquivo pode sumir num reinício,
// nesse caso quem entrar primeiro com um código volta a ser o dono dele.
// ---------------------------------------------------------------------------
const DATA_FILE = process.env.DATA_FILE || path.join(__dirname, "data.json");
const CODE_RE = /^[A-Z0-9]{8}$/;
const SECRET_RE = /^[A-Za-z0-9]{16,64}$/;

let db = { accounts: {}, inbox: {}, reqs: {} };
try {
    db = { ...db, ...JSON.parse(fs.readFileSync(DATA_FILE, "utf8")) };
} catch (e) { /* primeiro uso */ }

let saveTimer = null;
function save() {
    if (saveTimer) return;
    saveTimer = setTimeout(() => {
        saveTimer = null;
        fs.writeFile(DATA_FILE, JSON.stringify(db), () => {});
    }, 2000);
}

const normCode = (s) => String(s || "").toUpperCase().replace(/[^A-Z0-9]/g, "");
const cleanName = (s) => String(s || "").replace(/[\u0000-\u001f<>]/g, "").trim().slice(0, 16) || "Treinador";
const hash = (s) => crypto.createHash("sha256").update(String(s)).digest("hex");

function allow(socket) {
    const now = Date.now();
    const recent = (socket.data.reqTimes || []).filter((t) => now - t < 60000);
    if (recent.length >= 10) { socket.data.reqTimes = recent; return false; }
    recent.push(now);
    socket.data.reqTimes = recent;
    return true;
}

// Coloca um aviso (pedido, aceite, remoção) na caixa de entrada do destino e entrega se ele estiver online.
function push(code, item) {
    const box = db.inbox[code] || (db.inbox[code] = []);
    if (!box.some((i) => i.type === item.type && i.fromCode === item.fromCode)) {
        box.push({ ...item, at: Date.now() });
        if (box.length > 50) box.shift();
    }
    save();
    deliver(code);
}

function deliver(code) {
    const sid = online[code];
    const box = db.inbox[code];
    if (!sid || !box || !box.length) return;
    const sock = io.sockets.sockets.get(sid);
    if (!sock) return;
    const snapshot = box.slice();
    // só apaga depois que o aparelho confirmar o recebimento
    sock.timeout(8000).emit("friendInbox", snapshot, (err) => {
        if (err) return;
        const rest = (db.inbox[code] || []).filter(
            (i) => !snapshot.some((s) => s.at === i.at && s.type === i.type && s.fromCode === i.fromCode));
        if (rest.length) db.inbox[code] = rest; else delete db.inbox[code];
        save();
    });
}

const noop = () => {};
const asFn = (cb) => (typeof cb === "function" ? cb : noop);

app.get("/", (req, res) => {
    res.send("Servidor Multiplayer do PokeWorld está rodando online com Co-op na Torre e Amigos!");
});

// ---------------------------------------------------------------------------
// CARTEIRA / PIX (ver wallet.js). Variáveis de ambiente:
//   DATABASE_URL     Postgres (obrigatório para cobrar de verdade; o disco do Render grátis é apagado)
//   MP_ACCESS_TOKEN  access token do Mercado Pago (teste: TEST-..., produção: APP_USR-...)
//   PUBLIC_URL       endereço público do servidor (no Render usa RENDER_EXTERNAL_URL sozinho)
//   MP_WEBHOOK_SECRET (opcional) chave secreta do webhook para conferir a assinatura
//   WALLET_FILE_OK=1 só para testes locais: guarda a carteira em arquivo
// ---------------------------------------------------------------------------
let wallet = null;
(async () => {
    try {
        let store = null;
        if (process.env.DATABASE_URL) {
            const { Pool } = require("pg");
            const local = /localhost|127\.0\.0\.1/.test(process.env.DATABASE_URL);
            const pool = new Pool({ connectionString: process.env.DATABASE_URL, ssl: local || process.env.PGSSLMODE === "disable" ? false : { rejectUnauthorized: false }, max: 5 });
            store = new PgStore(pool);
        } else if (process.env.WALLET_FILE_OK === "1") {
            store = new FileStore(process.env.WALLET_FILE || path.join(__dirname, "wallet.json"));
        }
        if (!store) { console.log("[carteira] desligada: defina DATABASE_URL (Postgres)."); return; }
        await store.init();
        const mp = process.env.MP_ACCESS_TOKEN ? makeMp(process.env.MP_ACCESS_TOKEN, process.env.MP_BASE_URL) : null;
        if (!mp) console.log("[carteira] sem MP_ACCESS_TOKEN: saldo e compras funcionam, depósito não.");
        wallet = createWallet({ store, mp, publicUrl: process.env.PUBLIC_URL || process.env.RENDER_EXTERNAL_URL || "", webhookSecret: process.env.MP_WEBHOOK_SECRET || "" });
        setInterval(() => wallet.sweep().catch((e) => console.error("[pix] sweep", e.message)), 120000);
        console.log("[carteira] ligada (" + (process.env.DATABASE_URL ? "Postgres" : "arquivo") + ")");
    } catch (e) { console.error("[carteira] falha ao iniciar:", e.message); wallet = null; }
})();

app.post("/mp/webhook", express.json({ limit: "100kb" }), (req, res) => {
    res.sendStatus(200);                       // responde logo (o Mercado Pago espera até 22 s)
    if (!wallet) return;
    wallet.onWebhook({ query: req.query, body: req.body, headers: req.headers })
        .catch((e) => console.error("[pix] webhook:", e.message));
});

io.on("connection", (socket) => {
    console.log(`[+] Jogador conectado: ${socket.id}`);

    socket.on("join", (data) => {
        data = data || {};
        const name = cleanName(data.name);
        const code = normCode(data.code);
        const secret = String(data.secret || "");

        let identified = false;
        if (CODE_RE.test(code) && SECRET_RE.test(secret)) {
            const acc = db.accounts[code];
            if (!acc) {
                db.accounts[code] = { secretHash: hash(secret), name, lastSeen: Date.now() };
                identified = true;
            } else if (acc.secretHash === hash(secret)) {
                acc.name = name;
                acc.lastSeen = Date.now();
                identified = true;
            } else {
                socket.emit("identityRejected", { code });
            }
        }
        socket.data.code = identified ? code : null;
        socket.data.walletAuth = identified ? { code, hash: hash(secret) } : null;
        if (identified) { online[code] = socket.id; save(); }

        players[socket.id] = {
            id: socket.id,
            name,
            code: identified ? code : "",
            mapName: "CITY",
            x: 0,
            y: 0,
            direction: 0,
            frameIndex: 1,
            skinColor: -1,
            isMounted: false,
            petSpecies: "",
            petX: 0,
            petY: 0,
            petDirection: 0,
            petFrameIndex: 1,
            isShiny: false
        };

        socket.emit("currentPlayers", players);
        socket.broadcast.emit("playerJoined", players[socket.id]);
        if (identified) deliver(code);
    });

    // Só campos de movimento/aparência: o cliente não pode trocar id, nome nem código.
    const MOVE_FIELDS = ["mapName", "x", "y", "direction", "frameIndex", "skinColor", "isMounted",
        "petSpecies", "petX", "petY", "petDirection", "petFrameIndex", "isShiny"];
    socket.on("move", (data) => {
        if (!players[socket.id] || !data) return;
        for (const f of MOVE_FIELDS) if (data[f] !== undefined) players[socket.id][f] = data[f];
        socket.broadcast.emit("playerMoved", players[socket.id]);
    });

    // --- AMIGOS ---
    socket.on("rename", (data) => {
        const me = socket.data.code;
        if (!me || !db.accounts[me]) return;
        const name = cleanName(data && data.name);
        db.accounts[me].name = name;
        if (players[socket.id]) players[socket.id].name = name;
        save();
    });

    socket.on("friendRequest", (data, cb) => {
        cb = asFn(cb);
        const me = socket.data.code;
        if (!me) return cb({ ok: false, error: "Sem identidade online. Tente reconectar." });
        if (!allow(socket)) return cb({ ok: false, error: "Muitos pedidos. Aguarde um minuto." });
        const target = normCode(data && data.code);
        if (!CODE_RE.test(target)) return cb({ ok: false, error: "Código inválido." });
        if (target === me) return cb({ ok: false, error: "Esse é o seu próprio código!" });
        const acc = db.accounts[target];
        if (!acc) return cb({ ok: false, error: "Código não encontrado." });

        (db.reqs[target] || (db.reqs[target] = {}))[me] = Date.now();
        push(target, { type: "request", fromCode: me, fromName: db.accounts[me].name });
        cb({ ok: true, name: acc.name });
    });

    socket.on("friendRespond", (data, cb) => {
        cb = asFn(cb);
        const me = socket.data.code;
        if (!me) return cb({ ok: false, error: "Sem identidade online. Tente reconectar." });
        const target = normCode(data && data.code);
        const acc = db.accounts[target];
        const pending = db.reqs[me] && db.reqs[me][target];
        if (!acc || !pending) return cb({ ok: false, error: "Esse pedido não existe mais." });

        delete db.reqs[me][target];
        if (!Object.keys(db.reqs[me]).length) delete db.reqs[me];
        // tira o pedido da própria caixa, se ainda estiver lá
        if (db.inbox[me]) db.inbox[me] = db.inbox[me].filter((i) => !(i.type === "request" && i.fromCode === target));

        if (data.accept) push(target, { type: "accepted", fromCode: me, fromName: db.accounts[me].name });
        save();
        cb({ ok: true, name: acc.name });
    });

    socket.on("friendRemove", (data, cb) => {
        cb = asFn(cb);
        const me = socket.data.code;
        if (!me) return cb({ ok: false, error: "Sem identidade online." });
        const target = normCode(data && data.code);
        if (db.accounts[target]) push(target, { type: "removed", fromCode: me, fromName: db.accounts[me].name });
        cb({ ok: true });
    });

    socket.on("friendsSync", (data, cb) => {
        cb = asFn(cb);
        if (!socket.data.code) return cb({ ok: false, friends: [] });
        const codes = (Array.isArray(data && data.codes) ? data.codes : []).slice(0, 100).map(normCode);
        const friends = codes.filter((c) => db.accounts[c]).map((c) => {
            const sid = online[c];
            const p = sid && players[sid];
            return { code: c, name: db.accounts[c].name, online: !!sid, mapName: p ? p.mapName : "", lastSeen: db.accounts[c].lastSeen || 0 };
        });
        cb({ ok: true, friends });
    });

    // --- CARTEIRA / PIX ---
    const walletEvent = (name, fn) => socket.on(name, async (data, cb) => {
        cb = asFn(cb);
        try {
            if (!wallet) return cb({ ok: false, error: "Depósitos ainda não estão ativos neste servidor." });
            const au = socket.data.walletAuth;
            if (!au) return cb({ ok: false, error: "Sem identidade online. Reabra o jogo conectado." });
            const t = Date.now(), recent = (socket.data.wTimes || []).filter((x) => t - x < 60000);
            if (recent.length >= 40) return cb({ ok: false, error: "Muitas requisições. Aguarde um instante." });
            recent.push(t); socket.data.wTimes = recent;
            if (!(await wallet.store.auth(au.code, au.hash))) return cb({ ok: false, error: "A carteira desta conta pertence a outro aparelho." });
            cb(await fn(au.code, data || {}));
        } catch (e) { console.error("[carteira]", name, e.message); cb({ ok: false, error: "Erro no servidor." }); }
    });
    walletEvent("walletGet", (code) => wallet.summary(code));
    walletEvent("pixCreate", (code, d) => wallet.createPix(code, d.points));
    walletEvent("pixCheck", (code, d) => wallet.checkPix(code, d.id));
    walletEvent("walletSpend", (code, d) => wallet.spend(code, d.points, d.item));

    // --- ARENA: ranking global (pontos reportados pelo aparelho; o servidor só limita e ordena) ---
    socket.on("arenaScore", (data, cb) => {
        cb = asFn(cb);
        const me = socket.data.code;
        if (!me || !db.accounts[me]) return cb({ ok: false });
        if (!allow(socket)) return cb({ ok: false, error: "Muitas requisições." });
        const acc = db.accounts[me];
        const rp = Math.max(0, Math.min(99999, parseInt(data && data.rp, 10) || 0));
        // limite de plausibilidade: no máximo +60 pontos por envio em relação ao último valor aceito
        const prev = (acc.arena && acc.arena.rp) || 0;
        acc.arena = {
            rp: Math.min(rp, prev + 60),
            wins: Math.max(0, parseInt(data && data.wins, 10) || 0),
            losses: Math.max(0, parseInt(data && data.losses, 10) || 0),
            power: Math.max(0, Math.min(999999, parseInt(data && data.power, 10) || 0)),
            at: Date.now()
        };
        save();
        cb({ ok: true, rp: acc.arena.rp });
    });

    socket.on("arenaTop", (data, cb) => {
        cb = asFn(cb);
        const me = socket.data.code;
        const rows = Object.keys(db.accounts)
            .filter((c) => db.accounts[c].arena && db.accounts[c].arena.rp > 0)
            .map((c) => ({ code: c, name: db.accounts[c].name, ...db.accounts[c].arena }))
            .sort((a, b) => b.rp - a.rp || b.wins - a.wins);
        const idx = me ? rows.findIndex((r) => r.code === me) : -1;
        cb({
            ok: true,
            total: rows.length,
            myRank: idx >= 0 ? idx + 1 : 0,
            top: rows.slice(0, 50).map((r, i) => ({ rank: i + 1, name: r.name, rp: r.rp, wins: r.wins, losses: r.losses, power: r.power, me: r.code === me }))
        });
    });

    // --- SISTEMA DE CO-OP NA TORRE ---
    socket.on("inviteTower", (data) => {
        const targetId = data.targetId;
        if (targetId) {
            io.to(targetId).emit("towerInvitation", {
                fromId: socket.id,
                fromName: players[socket.id] ? players[socket.id].name : "Treinador",
                floor: data.floor || 1
            });
        }
    });

    socket.on("acceptTower", (data) => {
        const hostId = data.hostId;
        if (hostId) {
            io.to(hostId).emit("towerAccepted", {
                friendId: socket.id,
                friendName: players[socket.id] ? players[socket.id].name : "Treinador"
            });
        }
    });

    socket.on("disconnect", () => {
        console.log(`[-] Jogador desconectado: ${socket.id}`);
        const code = socket.data.code;
        if (code && online[code] === socket.id) {
            delete online[code];
            if (db.accounts[code]) { db.accounts[code].lastSeen = Date.now(); save(); }
        }
        delete players[socket.id];
        io.emit("playerLeft", socket.id);
    });
});

server.listen(PORT, () => {
    console.log(`Servidor rodando na porta ${PORT}`);
});

module.exports = { io, db };
