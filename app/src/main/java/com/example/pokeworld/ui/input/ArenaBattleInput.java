package com.example.pokeworld.ui.input;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.managers.ArenaDuel;
import com.example.pokeworld.ui.UIManager;
import com.example.pokeworld.ui.handlers.ArenaBattleUI;

/**
 * Toques da sala da Arena (o joystick é tratado pelo InputHandler): cartões e PRONTO no preparo, lançar o Pokémon
 * tocando na sua metade da arena, trocar de Pokémon na luta e CONTINUAR no resultado.
 */
public class ArenaBattleInput implements InputExtension {
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
                if (gv.world != null) ui.tapWorld((float) gv.world.screenToWorldX(x), (float) gv.world.screenToWorldY(y));   // lançar o Pokémon
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
