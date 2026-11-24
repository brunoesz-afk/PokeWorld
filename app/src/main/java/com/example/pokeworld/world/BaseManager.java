package com.example.pokeworld.world;

import android.graphics.Canvas;

import java.util.ArrayList;
import java.util.List;

/**
 * Dono da base do jogador: guarda a grade, aplica as regras de construção (custo, limite,
 * nível máximo), persiste e desenha. NÃO conhece Player/GameView: a carteira e o banco chegam por
 * interfaces, então toda a regra pode ser testada fora do aparelho.
 */
public class BaseManager implements MovementBlocker {

    /** Recursos do jogador. */
    public interface Wallet {
        int count(String item);
        void take(String item, int n);
        int gold();
        void addGold(int g);
        void give(String item, int n);
    }

    /** Persistência das peças. */
    public interface Store {
        void insert(BasePiece p);
        void update(BasePiece p);
        void delete(BasePiece p);
        void insertAll(List<BasePiece> pieces);
        void updateAll(List<BasePiece> pieces);
    }

    public static final String WOOD = "Madeira", IRON = "Ferro";

    public final BaseLayout layout;
    public final BaseRenderer renderer = new BaseRenderer();
    private Store store;

    // ---- estado do modo construção (UI) ----
    public boolean editing = false;
    public BaseKind placing = null;     // tipo escolhido no menu (aguardando posição)
    public BasePiece selected = null;   // peça existente selecionada
    public boolean movingSelected = false;
    public int ghostX = BaseLayout.PLOT_X + 20, ghostY = BaseLayout.PLOT_Y + 20;
    /** Muralha: arrastar pinta muralhas; false = arrastar move o mapa. */
    public boolean paintWalls = true;

    public BaseManager(int rows, int cols) { layout = new BaseLayout(rows, cols); }

    // ================================================================= carga / persistência

    /**
     * Carrega as peças salvas. Banco vazio = jogador novo (ou migrado): cria a base inicial.
     * Peças antigas sem posição (gx = -1) ganham o lugar livre mais próximo do centro.
     */
    public void load(List<BasePiece> saved, Store store, long now) {
        this.store = store;
        layout.clear();
        List<BasePiece> fresh = new ArrayList<>();
        if (saved == null || saved.isEmpty()) {
            fresh = BaseLayout.defaultPieces();
            for (BasePiece p : fresh) layout.add(p);
            store.insertAll(fresh);
        } else {
            List<BasePiece> homeless = new ArrayList<>();
            boolean removedRing = false;
            for (BasePiece p : saved) {
                boolean inside = p.gx >= 0 && BaseLayout.inPlot(p.gx, p.gy, p.kind.w, p.kind.h);
                if (inside) { layout.add(p); continue; }
                if (p.gx < 0) { homeless.add(p); continue; }
                // Peça de uma versão antiga do mapa (fora do terreno atual): muralhas/torres somem (o anel é refeito);
                // o resto ganha um lugar livre
                if (p.kind.isWall() || p.kind == BaseKind.TORRE_VIGIA) { store.delete(p); removedRing = true; }
                else if (p.kind == BaseKind.QUARTEL) { p.gx = BaseLayout.QUARTEL_X; p.gy = BaseLayout.QUARTEL_Y; layout.add(p); store.update(p); }
                else homeless.add(p);
            }
            if (layout.find(BaseKind.QUARTEL) == null) {
                BasePiece q = new BasePiece(BaseKind.QUARTEL, BaseLayout.QUARTEL_X, BaseLayout.QUARTEL_Y, 1);
                layout.add(q); store.insert(q);
            }
            if (removedRing && layout.count(BaseKind.MURALHA) == 0) {
                for (BasePiece rp : BaseLayout.defaultRing()) {
                    if (layout.isFree(rp.kind, rp.gx, rp.gy, null)) { layout.add(rp); store.insert(rp); }
                }
            }
            for (BasePiece p : homeless) {
                int[] at = layout.nearestFree(p.kind, BaseLayout.QUARTEL_X + 2 - p.kind.w / 2, BaseLayout.QUARTEL_Y + 8);
                if (at == null) { store.delete(p); continue; }
                p.gx = at[0]; p.gy = at[1];
                layout.add(p); store.update(p);
            }
        }
        tick(now);
    }

    /** Conclui obras/melhorias que já terminaram. Retorna as peças concluídas. */
    public List<BasePiece> tick(long now) {
        List<BasePiece> done = null;
        for (BasePiece p : layout.getPieces()) {
            if (p.isDone(now)) {
                if (done == null) done = new ArrayList<>();
                p.complete();
                done.add(p);
            }
        }
        if (done == null) return java.util.Collections.emptyList();
        for (BasePiece p : done) store.update(p);
        return done;
    }

    // ================================================================= consultas

    public int hq() { return layout.hqLevel(); }

    /** Quantos guardiões a base comporta (o Quartel é quem libera). */
    public int guardCapacity() { return capacityFor(layout.getPieces()); }

    /** 2 + 2 por nível do Quartel + 1 por construção útil ou Torre Choque (muralha, tocha e torres comuns não contam): a base cresce, a defesa cresce. */
    public static int capacityFor(java.util.List<BasePiece> pieces) {
        int hq = 1, extras = 0;
        for (BasePiece p : pieces) {
            if (p.kind == BaseKind.QUARTEL) hq = Math.max(hq, p.level);
            else if (p.isUsable() && (p.kind.cat == BaseKind.Cat.UTIL && p.kind != BaseKind.TOCHA || p.kind == BaseKind.TORRE_CHOQUE)) extras++;
        }
        return 2 + 2 * hq + extras;
    }

    @Override
    public boolean blocks(double wx, double wy) { return layout.blocks(wx, wy); }

    public BasePiece pieceAt(int gx, int gy) { return layout.at(gx, gy); }

    /** Custo para a PRÓXIMA etapa: construir (nível 1) ou subir de nível. */
    public int[] nextCost(BasePiece p) { return p.kind.cost(p.level + 1); }

    public String costText(int[] c) {
        StringBuilder sb = new StringBuilder();
        if (c[0] > 0) sb.append(c[0]).append("🪵 ");
        if (c[1] > 0) sb.append(c[1]).append("⛓️ ");
        if (c[2] > 0) sb.append(c[2]).append("🪙 ");
        return sb.length() == 0 ? "grátis" : sb.toString().trim();
    }

    public boolean canAfford(Wallet w, int[] c) {
        return w.count(WOOD) >= c[0] && w.count(IRON) >= c[1] && w.gold() >= c[2];
    }

    private String missing(Wallet w, int[] c) {
        StringBuilder sb = new StringBuilder("Faltam recursos:");
        if (w.count(WOOD) < c[0]) sb.append(" ").append(c[0] - w.count(WOOD)).append(" Madeira");
        if (w.count(IRON) < c[1]) sb.append(" ").append(c[1] - w.count(IRON)).append(" Ferro");
        if (w.gold() < c[2]) sb.append(" ").append(c[2] - w.gold()).append(" Ouro");
        return sb.toString();
    }

    private void pay(Wallet w, int[] c) {
        if (c[0] > 0) w.take(WOOD, c[0]);
        if (c[1] > 0) w.take(IRON, c[1]);
        if (c[2] > 0) w.addGold(-c[2]);
    }

    /** Por que este tipo não pode ser construído agora? null = pode. */
    public String blockedReason(BaseKind k) {
        if (layout.count(k) >= k.maxCount(hq())) {
            return k == BaseKind.QUARTEL ? "Já existe um Quartel" : "Limite atingido (" + k.maxCount(hq()) + "). Suba o Quartel.";
        }
        return null;
    }

    // ================================================================= ações

    /** Constrói uma peça nova. Retorna null se deu certo, senão a mensagem de erro. */
    public String place(BaseKind k, int gx, int gy, Wallet w, long now) {
        String why = blockedReason(k);
        if (why != null) return why;
        if (!layout.isFree(k, gx, gy, null)) return "Local ocupado ou fora do terreno";
        int[] cost = k.cost(1);
        if (!canAfford(w, cost)) return missing(w, cost);
        pay(w, cost);
        BasePiece p = new BasePiece(k, gx, gy, 1);
        int secs = k.seconds(1);
        if (secs > 0) { p.finishTime = now + secs * 1000L; p.upgrading = false; }
        layout.add(p);
        store.insert(p);
        return null;
    }

    /** Move a peça selecionada (de graça). */
    public String moveSelected(int gx, int gy) {
        BasePiece p = selected;
        if (p == null) return "Nada selecionado";
        if (!layout.isFree(p.kind, gx, gy, p)) return "Local ocupado ou fora do terreno";
        layout.move(p, gx, gy);
        store.update(p);
        return null;
    }

    /** Melhora uma peça: muralha sobe na hora; o resto vira obra com tempo. */
    public String upgrade(BasePiece p, Wallet w, long now) {
        if (p.isBusy(now)) return "Em obra";
        if (p.level >= p.kind.maxLevel) return "Já está no nível máximo";
        if (p.level >= p.kind.levelCap(hq())) return "Suba o Quartel para liberar o nível " + (p.level + 1);
        int[] cost = nextCost(p);
        if (!canAfford(w, cost)) return missing(w, cost);
        pay(w, cost);
        int secs = p.kind.seconds(p.level + 1);
        if (secs <= 0) { p.level++; }
        else { p.finishTime = now + secs * 1000L; p.upgrading = true; }
        store.update(p);
        return null;
    }

    /** Sobe TODAS as muralhas do mesmo nível da selecionada (o que o dinheiro der). Retorna mensagem. */
    public String upgradeAllWalls(int fromLevel, Wallet w) {
        if (fromLevel >= BaseKind.MURALHA.maxLevel) return "As muralhas já estão no nível máximo";
        if (fromLevel >= BaseKind.MURALHA.levelCap(hq())) return "Suba o Quartel para liberar o nível " + (fromLevel + 1);
        int[] cost = BaseKind.MURALHA.cost(fromLevel + 1);
        List<BasePiece> changed = new ArrayList<>();
        for (BasePiece p : layout.getPieces()) {
            if (!p.kind.isWall() || p.level != fromLevel) continue;
            if (!canAfford(w, cost)) break;
            pay(w, cost);
            p.level++;
            changed.add(p);
        }
        if (changed.isEmpty()) return missing(w, cost);
        store.updateAll(changed);
        return null;
    }

    public int countWalls(int level) {
        int n = 0;
        for (BasePiece p : layout.getPieces()) if (p.kind.isWall() && p.level == level) n++;
        return n;
    }

    /** Devolve 50% do que foi gasto. Retorna o reembolso {madeira, ferro, ouro} ou null se não pode vender. */
    public int[] refund(BasePiece p) {
        if (p.kind == BaseKind.QUARTEL) return null;
        int[] r = new int[3];
        int top = p.finishTime > 0 && !p.upgrading ? 0 : p.level;
        for (int l = 1; l <= Math.max(top, 1); l++) {
            int[] c = p.kind.cost(l);
            for (int i = 0; i < 3; i++) r[i] += c[i] / 2;
        }
        return r;
    }

    public String sell(BasePiece p, Wallet w) {
        int[] r = refund(p);
        if (r == null) return "O Quartel não pode ser vendido";
        if (r[0] > 0) w.give(WOOD, r[0]);
        if (r[1] > 0) w.give(IRON, r[1]);
        if (r[2] > 0) w.addGold(r[2]);
        layout.remove(p);
        store.delete(p);
        if (selected == p) { selected = null; movingSelected = false; }
        return null;
    }

    // ================================================================= plantação (Horta)

    private final java.util.Random rnd = new java.util.Random();

    /** Resultado de mexer numa horta: mensagem para o jogador e se algo mudou. */
    public static class CropResult { public String message; public boolean harvested, planted; public int fruit, balls; }

    /**
     * Toque numa horta: planta (custa 1 Fruta de semente), espera crescer, ou colhe.
     * A colheita rende várias Frutas e, às vezes, Pokébolas prontas.
     */
    public CropResult tendCrop(BasePiece p, Wallet w, long now) {
        CropResult r = new CropResult();
        if (p == null || p.kind != BaseKind.HORTA) return null;
        if (!p.isUsable()) { r.message = "🏗️ A horta ainda está em obra"; return r; }
        if (p.cropReady == 0) {
            if (w.count("Fruta") < 1) { r.message = "Precisa de 1 🍎 Fruta como semente"; return r; }
            w.take("Fruta", 1);
            p.cropReady = now + BaseKind.cropSeconds(p.level) * 1000L;
            store.update(p);
            r.planted = true;
            r.message = "🌱 Plantado! Pronto em " + BaseKind.cropSeconds(p.level) + "s";
            return r;
        }
        if (now < p.cropReady) {
            r.message = "⏳ Crescendo... " + ((p.cropReady - now + 999) / 1000) + "s";
            return r;
        }
        r.fruit = 3 + 2 * p.level + rnd.nextInt(p.level + 1);
        if (rnd.nextDouble() < 0.10 + 0.04 * p.level) r.balls = 1 + rnd.nextInt(2);
        w.give("Fruta", r.fruit);
        if (r.balls > 0) w.give("Pokébola", r.balls);
        p.cropReady = 0;
        store.update(p);
        r.harvested = true;
        r.message = "🍎 Colheu +" + r.fruit + " Frutas" + (r.balls > 0 ? "  +" + r.balls + " 🔴 Pokébola" : "");
        return r;
    }

    // ---------------------------------------------------------------- Estufa (produção passiva)

    public static int greenhouseInterval(int level) { return Math.max(60, 300 - 45 * (Math.max(1, level) - 1)); }   // segundos por unidade
    public static int greenhouseCap(int level) { return 6 + 4 * Math.max(1, level); }

    /** Unidades prontas no estoque da Estufa (cropReady guarda o instante da última coleta). */
    public static int greenhouseStock(BasePiece p, long now) {
        if (p.kind != BaseKind.ESTUFA || !p.isUsable() || p.cropReady == 0) return 0;
        long n = (now - p.cropReady) / (greenhouseInterval(p.level) * 1000L);
        return (int) Math.max(0, Math.min(greenhouseCap(p.level), n));
    }

    public CropResult collectGreenhouse(BasePiece p, Wallet w, long now) {
        CropResult r = new CropResult();
        if (p == null || p.kind != BaseKind.ESTUFA) return null;
        if (!p.isUsable()) { r.message = "🏗️ A estufa ainda está em obra"; return r; }
        long step = greenhouseInterval(p.level) * 1000L;
        if (p.cropReady == 0) {
            p.cropReady = now; store.update(p);
            r.message = "🌿 Estufa ligada! Volte daqui a " + (greenhouseInterval(p.level) / 60) + " min para a primeira colheita.";
            return r;
        }
        int n = greenhouseStock(p, now);
        if (n <= 0) { r.message = "🌱 Ainda crescendo... próxima unidade em " + Math.max(1, (step - (now - p.cropReady) % step) / 1000) + "s"; return r; }
        int fruit = 0, potions = 0, dust = 0;
        for (int i = 0; i < n; i++) {
            fruit += 2;
            if (i % 4 == 3) potions++;
            if (p.level >= 3 && rnd.nextDouble() < 0.25) dust++;
        }
        w.give("Fruta", fruit);
        if (potions > 0) w.give("Poção", potions);
        if (dust > 0) w.give("Poeira Mística", dust);
        p.cropReady = Math.min(now, p.cropReady + n * step);   // o tempo que ainda não virou unidade continua contando
        store.update(p);
        r.harvested = true; r.fruit = fruit;
        r.message = "🌿 Estufa: +" + fruit + " 🍎" + (potions > 0 ? "  +" + potions + " 🧪" : "") + (dust > 0 ? "  +" + dust + " ✨ Poeira Mística" : "");
        return r;
    }

    /** 0 = vazia, 1 = crescendo, 2 = pronta para colher. */
    public static int cropStage(BasePiece p, long now) {
        if (p.kind != BaseKind.HORTA || !p.isUsable() || p.cropReady == 0) return 0;
        return now >= p.cropReady ? 2 : 1;
    }

    // ================================================================= edição (estado da UI)

    public void setEditing(boolean e) {
        if (editing == e) return;
        editing = e;
        if (!e) { placing = null; selected = null; movingSelected = false; }
    }

    public void choose(BaseKind k) {
        placing = k; selected = null; movingSelected = false;
        paintWalls = true;
    }

    public void cancelAction() { placing = null; selected = null; movingSelected = false; }

    /** Centraliza o fantasma do tipo (ou da peça movida) em torno de uma célula do toque. */
    public void aimGhost(int cellX, int cellY) {
        BaseKind k = placing != null ? placing : (selected != null ? selected.kind : null);
        if (k == null) return;
        ghostX = cellX - k.w / 2;
        ghostY = cellY - k.h / 2;
    }

    // ================================================================= desenho (dentro da transformação do mundo)

    public void drawWorld(Canvas c, long now, float vl, float vt, float vr, float vb) {
        renderer.draw(c, layout, now, vl, vt, vr, vb);
    }

    /** Por cima de tudo, ainda no mundo: barras de obra, níveis, grade e fantasma. */
    public void drawOverlay(Canvas c, long now, float vl, float vt, float vr, float vb) {
        renderer.drawStatus(c, layout, now, vl, vt, vr, vb, editing);
        if (!editing) return;
        renderer.drawGrid(c, layout, vl, vt, vr, vb);
        if (selected != null && !movingSelected) {
            renderer.drawFootprint(c, selected.gx, selected.gy, selected.kind.w, selected.kind.h, true, true);
            if (selected.kind == BaseKind.TORRE_VIGIA || selected.kind == BaseKind.TORRE_CHOQUE)
                renderer.drawRange(c, (float) selected.centerX(), (float) selected.centerY(), BaseDefense.towerRange(selected) );
        }
        BaseKind k = placing != null ? placing : (movingSelected && selected != null ? selected.kind : null);
        if (k != null && !(k.isWall() && placing != null)) {
            boolean ok = layout.isFree(k, ghostX, ghostY, movingSelected ? selected : null);
            int lvl = movingSelected && selected != null ? selected.level : 1;
            renderer.drawPiece(c, k, lvl, ghostX, ghostY, 0, 170, now);
            renderer.drawFootprint(c, ghostX, ghostY, k.w, k.h, ok, false);
            if (k == BaseKind.TORRE_VIGIA || k == BaseKind.TORRE_CHOQUE)
                renderer.drawRange(c, (ghostX + k.w / 2f) * 64f, (ghostY + k.h / 2f) * 64f, BaseDefense.towerRange(k, lvl));
        }
    }
}
