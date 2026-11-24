package com.example.pokeworld.ui.input;

import android.graphics.RectF;
import android.view.MotionEvent;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.ui.UIManager;
import com.example.pokeworld.ui.handlers.BuildUI;
import com.example.pokeworld.world.BaseKind;
import com.example.pokeworld.world.BaseManager;
import com.example.pokeworld.world.BasePiece;
import com.example.pokeworld.world.PlayerWallet;

/**
 * Gestos do modo construção:
 *  - arrastar o mapa = andar a câmera;  toque numa peça = selecionar;
 *  - peça nova/em movimento: arrastar em cima dela leva a peça; arrastar longe dela anda a câmera;
 *    tocar num ponto livre leva a peça até lá;
 *  - muralha: arrastar pinta uma linha de muralhas (botão alterna pintar/mover o mapa);
 *  - faixa de baixo: arrastar rola o menu, tocar numa carta escolhe a peça.
 */
public class BuildInput implements InputExtension {
    private enum Mode { NONE, PAN, GHOST, PAINT, MENU, BUTTON }

    private Mode mode = Mode.NONE;
    private float downX, downY, lastX, lastY;
    private boolean moved;
    private int lastPaintX = Integer.MIN_VALUE, lastPaintY = Integer.MIN_VALUE;
    private boolean paintStopped;
    private RectF pressedBtn;

    @Override
    public boolean handle(float x, float y, GameView gv) { return true; } // tudo é tratado em handleEvent

    @Override
    public boolean handleEvent(MotionEvent e, GameView gv) {
        BuildUI ui = (BuildUI) gv.ui.getHandler(UIManager.UIState.BUILD);
        BaseManager base = gv.world.base;
        if (ui == null || base == null) return false;
        float x = e.getX(), y = e.getY();

        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = lastX = x; downY = lastY = y; moved = false; paintStopped = false;
                lastPaintX = lastPaintY = Integer.MIN_VALUE;
                pressedBtn = buttonAt(ui, x, y);
                if (pressedBtn != null) { mode = Mode.BUTTON; return true; }
                if (ui.panel.contains(x, y)) {
                    mode = (base.placing == null && base.selected == null) ? Mode.MENU : Mode.NONE;
                    return true;
                }
                int cx = cellX(gv, x), cy = cellY(gv, y);
                if (base.placing != null && base.placing.isWall()) {
                    if (base.paintWalls) { mode = Mode.PAINT; paintWall(gv, base, cx, cy); }
                    else mode = Mode.PAN;
                } else if (base.placing != null || base.movingSelected) {
                    mode = nearGhost(base, cx, cy) ? Mode.GHOST : Mode.PAN;
                    if (mode == Mode.GHOST) base.aimGhost(cx, cy);
                } else {
                    mode = Mode.PAN;
                }
                return true;

            case MotionEvent.ACTION_MOVE: {
                float dx = x - lastX, dy = y - lastY;
                if (Math.abs(x - downX) + Math.abs(y - downY) > 14) moved = true;
                lastX = x; lastY = y;
                switch (mode) {
                    case PAN:
                        if (moved) {
                            float z = gv.world.getCurrentZoom();
                            gv.world.setBuildCamera(gv.world.getBuildCamX() - dx / z, gv.world.getBuildCamY() - dy / z);
                        }
                        break;
                    case GHOST: base.aimGhost(cellX(gv, x), cellY(gv, y)); break;
                    case PAINT: paintWall(gv, base, cellX(gv, x), cellY(gv, y)); break;
                    case MENU: ui.menuScroll = Math.max(0, Math.min(ui.menuMaxScroll, ui.menuScroll - dx)); break;
                    default: break;
                }
                return true;
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                Mode m = mode; mode = Mode.NONE;
                if (e.getActionMasked() == MotionEvent.ACTION_CANCEL) return true;
                if (m == Mode.BUTTON) {
                    if (pressedBtn != null && pressedBtn.contains(x, y)) onButton(gv, ui, base, pressedBtn);
                    pressedBtn = null;
                } else if (m == Mode.MENU && !moved) {
                    for (int i = 0; i < ui.cardRects.size(); i++)
                        if (ui.cardRects.get(i).contains(x, y)) { chooseKind(gv, base, ui.cardKinds.get(i)); break; }
                } else if (m == Mode.PAN && !moved) {
                    int ux = cellX(gv, x), uy = cellY(gv, y);
                    if (base.placing != null || base.movingSelected) base.aimGhost(ux, uy);   // toque = leva a peça até ali
                    else base.selected = base.pieceAt(ux, uy);                                   // toque = seleciona (ou solta)
                }
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ botões
    private RectF buttonAt(BuildUI ui, float x, float y) {
        RectF[] all = {ui.raidBtn, ui.formationBtn, ui.confirmBtn, ui.cancelBtn, ui.paintToggleBtn, ui.moveBtn, ui.upgradeBtn, ui.sellBtn, ui.closeBtn, ui.wallAllBtn};
        for (RectF r : all) if (!r.isEmpty() && r.contains(x, y)) return r;
        return null;
    }

    private void onButton(GameView gv, BuildUI ui, BaseManager base, RectF b) {
        long now = System.currentTimeMillis();
        PlayerWallet wallet = new PlayerWallet(gv.player);

        if (b == ui.formationBtn) {
            int f = (gv.activity.dbHelper.getKv("base_formation", 0) + 1) % 3;
            gv.activity.dbHelper.setKv("base_formation", f);
            gv.world.refreshGuards(gv.player);
            gv.ui.showMessage(new String[]{"🛡 Defensores espalhados pelo pátio", "🛡 Linha de frente: perto das muralhas", "🛡 Recuados: protegem o Quartel"}[f]);
            return;
        }
        if (b == ui.raidBtn) {
            if (gv.world.defense == null) return;
            if (gv.world.defense.isActive()) { gv.ui.showMessage("A invasão já está em andamento!"); return; }
            gv.world.defense.start(now);
            gv.ui.state = UIManager.UIState.EXPLORING; // volta ao jogo para acompanhar a defesa
            return;
        }
        if (b == ui.paintToggleBtn) { base.paintWalls = !base.paintWalls; return; }
        if (b == ui.cancelBtn || b == ui.closeBtn) { base.cancelAction(); return; }

        if (b == ui.confirmBtn) {
            if (base.movingSelected && base.selected != null) {
                String err = base.moveSelected(base.ghostX, base.ghostY);
                if (err != null) { gv.ui.showMessage("❌ " + err); return; }
                base.movingSelected = false;
                gv.world.refreshBaseNpcs();
                gv.ui.showMessage("✅ Movido!");
            } else if (base.placing != null) {
                if (base.placing.isWall()) { base.cancelAction(); return; } // "Pronto"
                BaseKind k = base.placing;
                String err = base.place(k, base.ghostX, base.ghostY, wallet, now);
                if (err != null) { gv.ui.showMessage("❌ " + err); return; }
                afterBuilt(gv, base, k);
                if (base.blockedReason(k) != null) base.cancelAction(); // atingiu o limite
            }
            return;
        }

        BasePiece p = base.selected;
        if (p == null) return;
        if (b == ui.moveBtn) {
            if (p.isBusy(now) && !p.upgrading) { gv.ui.showMessage("Espere a obra terminar"); return; }
            base.movingSelected = true;
            base.ghostX = p.gx; base.ghostY = p.gy;
        } else if (b == ui.upgradeBtn) {
            String err = base.upgrade(p, wallet, now);
            if (err != null) { gv.ui.showMessage("❌ " + err); return; }
            gv.ui.showMessage(p.kind.isWall() ? "⬆ Muralha Lv." + p.level : "⬆ Melhoria iniciada!");
            gv.particles.addExplosion(p.centerX(), p.centerY(), android.graphics.Color.YELLOW, 25);
            gv.applyKingdomBonuses();
            gv.activity.saveCurrentGame();
        } else if (b == ui.wallAllBtn) {
            int from = p.level;
            String err = base.upgradeAllWalls(from, wallet);
            if (err != null) { gv.ui.showMessage("❌ " + err); return; }
            gv.ui.showMessage("⬆ Muralhas melhoradas! (" + base.countWalls(from + 1) + " no Lv." + (from + 1) + ")");
            gv.activity.saveCurrentGame();
        } else if (b == ui.sellBtn) {
            double cx = p.centerX(), cy = p.centerY();
            String err = base.sell(p, wallet);
            if (err != null) { gv.ui.showMessage("❌ " + err); return; }
            gv.particles.addExplosion(cx, cy, android.graphics.Color.GRAY, 25);
            gv.world.refreshBaseNpcs();
            gv.applyKingdomBonuses();
            gv.ui.showMessage("💰 Vendido!");
            gv.activity.saveCurrentGame();
        }
    }

    private void chooseKind(GameView gv, BaseManager base, BaseKind k) {
        String why = base.blockedReason(k);
        if (why != null) { gv.ui.showMessage("🔒 " + why); return; }
        base.choose(k);
        // posiciona o fantasma no centro da tela
        base.aimGhost((int) Math.floor(gv.world.getBuildCamX() / 64.0), (int) Math.floor(gv.world.getBuildCamY() / 64.0));
        if (k.isWall()) gv.ui.showMessage("Arraste o dedo no mapa para pintar muralhas");
    }

    private void afterBuilt(GameView gv, BaseManager base, BaseKind k) {
        gv.particles.addExplosion((base.ghostX + k.w / 2.0) * 64, (base.ghostY + k.h / 2.0) * 64, android.graphics.Color.WHITE, 30);
        gv.ui.showMessage(k.buildSeconds > 0 ? "🏗️ " + k.label + " em construção (" + k.seconds(1) + "s)" : "✅ " + k.label + " construído!");
        if (gv.missions != null) gv.missions.updateProgress(k.legacyName());
        gv.world.refreshBaseNpcs();
        gv.applyKingdomBonuses();
        gv.activity.saveCurrentGame();
    }

    // ------------------------------------------------------------------ muralha em linha
    private void paintWall(GameView gv, BaseManager base, int cx, int cy) {
        if (paintStopped || (cx == lastPaintX && cy == lastPaintY)) return;
        lastPaintX = cx; lastPaintY = cy;
        BaseKind k = BaseKind.MURALHA;
        if (!base.layout.isFree(k, cx, cy, null)) return; // ocupado/fora do terreno: só ignora
        String err = base.place(k, cx, cy, new PlayerWallet(gv.player), System.currentTimeMillis());
        if (err != null) {
            paintStopped = true; // não fica gritando a cada célula
            gv.ui.showMessage("❌ " + err);
        } else if (base.layout.count(k) % 10 == 0) {
            gv.activity.saveCurrentGame();
        }
    }

    // ------------------------------------------------------------------ geometria
    private int cellX(GameView gv, float sx) { return (int) Math.floor(gv.world.screenToWorldX(sx) / 64.0); }
    private int cellY(GameView gv, float sy) { return (int) Math.floor(gv.world.screenToWorldY(sy) / 64.0); }

    private boolean nearGhost(BaseManager base, int cx, int cy) {
        BaseKind k = base.placing != null ? base.placing : (base.selected != null ? base.selected.kind : null);
        if (k == null) return false;
        return cx >= base.ghostX - 1 && cx < base.ghostX + k.w + 1 && cy >= base.ghostY - 1 && cy < base.ghostY + k.h + 1;
    }
}
