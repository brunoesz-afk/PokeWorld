package com.example.pokeworld.ui.input;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.ui.UIManager;
import com.example.pokeworld.ui.handlers.TeleportUI;

public class TeleportInput implements InputExtension {
    @Override
    public boolean handle(float x, float y, GameView gv) {
        TeleportUI ui = (TeleportUI) gv.ui.getHandler(UIManager.UIState.TELEPORT);
        
        if (ui.backBtn.contains(x, y)) {
            gv.ui.state = UIManager.UIState.EXPLORING;
            return true;
        }

        if (ui.nextBtn.contains(x, y) && ui.currentPage < TeleportUI.PAGE_COUNT - 1) {
            ui.currentPage++;
            return true;
        }

        if (ui.prevBtn.contains(x, y) && ui.currentPage > 0) {
            ui.currentPage--;
            return true;
        }

        for (TeleportUI.MapPoint p : ui.points) {
            if (p.page == ui.currentPage && p.bounds.contains(x, y)) {
                if (gv.player.getLevel() < p.unlockLv) {
                    gv.ui.showMessage("❌ Nível insuficiente! Requer Lv." + p.unlockLv);
                    return true;
                }

                // Carrega o mapa diretamente sem seleção de andares
                gv.world.loadMap(p.biomeKey, gv.player, gv.companion, "CENTER");
                gv.ui.state = UIManager.UIState.EXPLORING;
                gv.ui.showMessage("Viajando para: " + p.name);
                return true;
            }
        }
        
        return true;
    }
}
