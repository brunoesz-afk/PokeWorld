package com.example.pokeworld.ui.input;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.entities.Enemy;
import com.example.pokeworld.ui.UIManager;
import com.example.pokeworld.ui.handlers.ClanSelectionUI;

public class ClanInput implements InputExtension {
    @Override
    public boolean handle(float x, float y, GameView gv) {
        ClanSelectionUI ui = (ClanSelectionUI) gv.ui.getHandler(UIManager.UIState.CLAN);
        if (ui.rubiBtn.contains(x, y)) gv.player.setClan("Rubi");
        else if (ui.safiraBtn.contains(x, y)) gv.player.setClan("Safira");
        else if (ui.esmeraldaBtn.contains(x, y)) gv.player.setClan("Esmeralda");
        
        if (!gv.player.getClan().equals("Nenhum")) {
            gv.applyKingdomBonuses();
            gv.ui.showMessage("🔥 Bem-vindo ao Clã " + gv.player.getClan() + "!");
            gv.ui.state = UIManager.UIState.EXPLORING;
        }
        return true;
    }

    public static void executeClanSkill(GameView gv) {
        String clan = gv.player.getClan();
        if (clan.equals("Rubi")) {
            gv.ui.showMessage("☄️ Chuva de Meteoros!");
            for (Enemy e : gv.world.enemies) {
                if (e.isAlive()) e.takeDamage(200);
            }
            gv.startShake(20, 10);
        } else if (clan.equals("Safira")) {
            gv.ui.showMessage("💧 Manto de Bolha!");
            gv.companion.healFull();
        } else if (clan.equals("Esmeralda")) {
            gv.ui.showMessage("🌿 Bênção da Terra!");
            gv.companion.healFull();
        }
    }
}
