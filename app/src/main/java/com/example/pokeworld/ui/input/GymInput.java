package com.example.pokeworld.ui.input;

import android.graphics.Color;
import com.example.pokeworld.core.GameView;
import com.example.pokeworld.ui.UIManager;
import com.example.pokeworld.ui.handlers.GymUI;

public class GymInput implements InputExtension {
    @Override
    public boolean handle(float x, float y, GameView gv) {
        GymUI ui = (GymUI) gv.ui.getHandler(UIManager.UIState.GYM);
        
        if (ui.startBattleBtn.contains(x, y)) {
            startLeaderBattle(gv);
            return true;
        }

        gv.ui.state = UIManager.UIState.EXPLORING;
        return true;
    }

    private void startLeaderBattle(GameView gv) {
        gv.ui.showMessage("⚔️ LÍDER BROCK DESAFIA VOCÊ!");
        gv.world.enemies.clear();
        gv.world.addEnemy("Onix_Lider", gv.player.getPositionX() + 300, gv.player.getPositionY(), 70, Color.GRAY, true, true, gv.player.getLevel() + 3, 0);
        gv.ui.state = UIManager.UIState.EXPLORING;
    }
}
