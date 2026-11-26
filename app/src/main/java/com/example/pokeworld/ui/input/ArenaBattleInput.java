package com.example.pokeworld.ui.input;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.managers.ArenaDuel;
import com.example.pokeworld.ui.UIManager;
import com.example.pokeworld.ui.handlers.ArenaBattleUI;

/** Toques do duelo: escolher time, posicionar no mapa e PRONTO na preparação; trocar de Pokémon durante a luta. */
public class ArenaBattleInput implements InputExtension {
    private boolean walking = false;

    /** Arrastar o dedo dentro do retângulo do treinador faz o seu personagem andar (livre, sempre dentro do limite). */
    @Override
    public boolean handleEvent(android.view.MotionEvent e, GameView gv) {
        ArenaBattleUI ui = (ArenaBattleUI) gv.ui.getHandler(UIManager.UIState.ARENA_BATTLE);
        if (ui == null || ui.duel == null) return false;
        int a = e.getActionMasked();
        float x = e.getX(e.getActionIndex()), y = e.getY(e.getActionIndex());
        if (a == android.view.MotionEvent.ACTION_DOWN || a == android.view.MotionEvent.ACTION_POINTER_DOWN) {
            if (ui.meBox.contains(x, y)) { walking = true; ui.walkTo(x, y); return true; }
            walking = false;
            return false;
        }
        if (a == android.view.MotionEvent.ACTION_MOVE && walking) {
            int i = e.getActionIndex();
            ui.walkTo(e.getX(i), e.getY(i));
            return true;
        }
        if ((a == android.view.MotionEvent.ACTION_UP || a == android.view.MotionEvent.ACTION_POINTER_UP) && walking) { walking = false; return true; }
        return false;
    }

    @Override
    public boolean handle(float x, float y, GameView gv) {
        ArenaBattleUI ui = (ArenaBattleUI) gv.ui.getHandler(UIManager.UIState.ARENA_BATTLE);
        if (ui == null || ui.duel == null) return false;
        if (ui.exitBtn.contains(x, y)) { ui.leave(gv); return true; }
        ArenaDuel d = ui.duel;
        switch (d.phase) {
            case PREP:
                if (ui.readyBtn.contains(x, y)) {
                    if (d.meReady) return true;
                    if (ui.picks.isEmpty()) { gv.ui.showMessage("Escolha ao menos 1 Pokémon."); return true; }
                    if (!ui.thrown) { gv.ui.showMessage("Lance o Pokémon na sua metade da arena primeiro."); return true; }
                    if (!ui.ready(gv)) { gv.ui.showMessage("⛔ Acabaram as tentativas de hoje."); ui.leave(gv); }
                    return true;
                }
                for (int i = 0; i < ui.cards.length && i < ui.roster.size(); i++)
                    if (ui.cards[i].contains(x, y)) { if (d.meReady) gv.ui.showMessage("Você já está PRONTO."); else ui.togglePick(i); return true; }
                ui.tapField(x, y, gv);      // posicionar o inicial atrás da sua linha
                return true;
            case FIGHT:
                for (int i = 0; i < ui.cards.length && i < d.me.team.size(); i++) {
                    if (!ui.cards[i].contains(x, y)) continue;
                    if (i == d.me.active) return true;
                    if (!d.me.team.get(i).alive()) { gv.ui.showMessage("Esse Pokémon desmaiou."); return true; }
                    if (!d.swap(i)) gv.ui.showMessage("🔄 Troca em " + String.format("%.1f", d.me.swapCd / 1000f) + "s");
                    else ui.onSwap();       // o treinador lança outra bola
                    return true;
                }
                return true;
            case RESULT:
                if (ui.continueBtn.contains(x, y)) ui.leave(gv);
                return true;
            default:
                return true;
        }
    }
}
