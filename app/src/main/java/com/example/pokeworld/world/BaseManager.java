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
            for (BasePiece p : saved) {
                if (p.gx < 0) homeless.add(p); else layout.add(p);
            }
            if (layout.find(BaseKind.QUARTEL) == null) {
                BasePiece q = new BasePiece(BaseKind.QUARTEL, 78, 38, 1);
                layout.add(q); store.insert(q);
            }
            for (BasePiece p : homeless) {
                int[] at = layout.nearestFree(p.kind, 80 - p.kind.w / 2, 46);
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
    public int guardCapacity() { return 4 + 4 * hq(); }

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
