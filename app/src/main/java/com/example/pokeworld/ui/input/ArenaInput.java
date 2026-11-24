package com.example.pokeworld.ui.input;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.managers.ArenaDuel;
import com.example.pokeworld.managers.ArenaLadder;
import com.example.pokeworld.managers.PointsManager;
import com.example.pokeworld.managers.RewardManager;
import com.example.pokeworld.ui.UIManager;
import com.example.pokeworld.ui.handlers.ArenaBattleUI;
import com.example.pokeworld.ui.handlers.ArenaUI;

import java.util.List;

/** Toques da lista de lutadores da Arena. */
public class ArenaInput implements InputExtension {
    @Override
    public boolean handle(float x, float y, GameView gv) {
        ArenaUI ui = (ArenaUI) gv.ui.getHandler(UIManager.UIState.ARENA);
        RewardManager.Store s = PointsManager.store();
        if (ui == null || s == null) return false;
        if (ui.exitBtn.contains(x, y)) { gv.ui.state = UIManager.UIState.COLISEUM; return true; }
        if (ui.rankBtn.contains(x, y)) { openRank(gv); return true; }
        List<ArenaLadder.Fighter> list = ui.fighters();
        if (list == null) return true;
        for (int i = 0; i < ui.fightBtns.length && i < list.size(); i++) {
            if (!ui.fightBtns[i].contains(x, y)) continue;
            if (ArenaLadder.beaten(s, i)) { gv.ui.showMessage("✔ Você já venceu este lutador hoje. Volte amanhã!"); return true; }
            if (ArenaLadder.triesLeft(s) <= 0) { gv.ui.showMessage("⛔ Acabaram as tentativas de hoje. Volte amanhã!"); return true; }
            if (gv.player.getParty().isEmpty()) { gv.ui.showMessage("Você precisa de um Pokémon no time."); return true; }
            ArenaLadder.Fighter f = list.get(i);
            ArenaBattleUI b = (ArenaBattleUI) gv.ui.getHandler(UIManager.UIState.ARENA_BATTLE);
            b.begin(gv, f, ArenaLadder.teamOf(gv.activity, f));
            return true;
        }
        return true;
    }

    public static void openRank(GameView gv) {
        com.example.pokeworld.ui.handlers.ArenaRankUI r = (com.example.pokeworld.ui.handlers.ArenaRankUI) gv.ui.getHandler(UIManager.UIState.ARENA_RANK);
        if (r != null) r.open();
        gv.ui.state = UIManager.UIState.ARENA_RANK;
    }
}
