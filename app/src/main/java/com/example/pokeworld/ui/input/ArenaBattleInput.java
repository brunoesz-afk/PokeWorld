package com.example.pokeworld.ui.input;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.managers.ArenaDuel;
import com.example.pokeworld.ui.UIManager;
import com.example.pokeworld.ui.handlers.ArenaBattleUI;

/**
 * Toques da sala da Arena (o joystick é tratado pelo InputHandler): painel central (ESCOLHER), botões Pokémon / PRONTO /
 * Fugir, lançar o Pokémon tocando na sua metade da arena e CONTINUAR no resultado.
 */
public class ArenaBattleInput implements InputExtension {
    @Override
    public boolean handle(float x, float y, GameView gv) {
        ArenaBattleUI ui = (ArenaBattleUI) gv.ui.getHandler(UIManager.UIState.ARENA_BATTLE);
        if (ui == null || ui.duel == null) return false;
        ArenaDuel d = ui.duel;
        if (d.phase == ArenaDuel.Phase.RESULT) {
            if (ui.continueBtn.contains(x, y)) ui.leave(gv);
            return true;
        }
        if (ui.modal != ArenaBattleUI.Modal.NONE) return handleModal(ui, d, x, y, gv);
        if (ui.exitBtn.contains(x, y) || ui.fleeBtn.contains(x, y)) { ui.leave(gv); return true; }
        if (ui.pokeBtn.contains(x, y)) {
            if (d.phase == ArenaDuel.Phase.PREP) {
                if (d.meReady) gv.ui.showMessage("Você já está PRONTO.");
                else ui.modal = ArenaBattleUI.Modal.TEAM;
            } else if (d.phase == ArenaDuel.Phase.FIGHT) {
                if (d.me.swapCd > 0 && d.me.active != -1) gv.ui.showMessage("🔄 Troca em " + String.format("%.1f", d.me.swapCd / 1000f) + "s");
                else ui.modal = ArenaBattleUI.Modal.SWAP;
            }
            return true;
        }
        if (d.phase == ArenaDuel.Phase.PREP) {
            if (ui.readyBtn.contains(x, y)) {
                if (d.meReady) return true;
                if (ui.picks.isEmpty()) { gv.ui.showMessage("Escolha ao menos 1 Pokémon."); return true; }
                if (!ui.thrown) { gv.ui.showMessage("Lance o Pokémon na arena primeiro (toque na sua metade)."); return true; }
                if (!ui.ready(gv)) { gv.ui.showMessage("⛔ Acabaram as tentativas de hoje."); ui.leave(gv); }
                return true;
            }
            if (gv.world != null) ui.tapWorld((float) gv.world.screenToWorldX(x), (float) gv.world.screenToWorldY(y));   // lançar o Pokémon
        }
        return true;
    }

    private boolean handleModal(ArenaBattleUI ui, ArenaDuel d, float x, float y, GameView gv) {
        if (ui.modalClose.contains(x, y) && ui.modalClosable()) { ui.modal = ArenaBattleUI.Modal.NONE; return true; }
        if (ui.modal == ArenaBattleUI.Modal.STARTER && ui.modalTeamBtn.contains(x, y)) { ui.modal = ArenaBattleUI.Modal.TEAM; return true; }
        if (ui.modal == ArenaBattleUI.Modal.TEAM && ui.modalOk.contains(x, y)) { ui.modal = ui.thrown ? ArenaBattleUI.Modal.NONE : ArenaBattleUI.Modal.STARTER; return true; }
        for (int i = 0; i < ui.cards.length; i++) {
            if (!ui.cards[i].contains(x, y) && !ui.okBtns[i].contains(x, y)) continue;
            switch (ui.modal) {
                case STARTER: ui.chooseStarter(i); break;
                case TEAM: if (i < ui.roster.size()) ui.toggleMember(i); break;
                case SWAP: ui.swapTo(i); break;
                default: break;
            }
            return true;
        }
        return true;      // o painel come os toques
    }
}
