package com.example.pokeworld.ui.input;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.entities.CapturedCreature;
import com.example.pokeworld.ui.UIManager;
import com.example.pokeworld.ui.handlers.DexUI;

import java.util.List;

public class DexInput implements InputExtension {
    @Override
    public boolean handle(float x, float y, GameView gv) {
        DexUI ui = (DexUI) gv.ui.getHandler(UIManager.UIState.POKEMON_DETAILS);
        int w = gv.getWidth();
        int h = gv.getHeight();
        
        if (ui.closeBtn.contains(x, y)) {
            gv.ui.state = UIManager.UIState.CREATURES;
            return true;
        }

        // Setas de Navegação
        List<CapturedCreature> collection = gv.player.getCreatureCollection();
        if (ui.nextBtn.contains(x, y)) {
            int currentIndex = collection.indexOf(ui.selectedCreatureForDex);
            if (currentIndex < collection.size() - 1) {
                gv.ui.selectForDex(collection.get(currentIndex + 1));
            }
            return true;
        }
        if (ui.prevBtn.contains(x, y)) {
            int currentIndex = collection.indexOf(ui.selectedCreatureForDex);
            if (currentIndex > 0) {
                gv.ui.selectForDex(collection.get(currentIndex - 1));
            }
            return true;
        }

        // Seleção na Lista Esquerda (Respeitando o filtro de busca)
        float startY = h * 0.25f;
        float itemH = h * 0.12f;
        float spacing = h * 0.02f;
        
        if (x > w * 0.04f && x < w * 0.31f && y > startY) {
            int drawnIndex = (int)((y - startY) / (itemH + spacing));
            
            int count = 0;
            for (CapturedCreature c : collection) {
                if (!ui.searchQuery.isEmpty() && !c.name.toLowerCase().contains(ui.searchQuery.toLowerCase())) continue;
                if (count == drawnIndex) {
                    gv.ui.selectForDex(c);
                    return true;
                }
                count++;
            }
        }

        // Clique na barra de pesquisa (abre teclado ou limpa)
        if (ui.searchBar.contains(x, y)) {
            ui.searchQuery = ""; // Por enquanto limpa a busca ao clicar
            return true;
        }

        if (x < w * 0.03f || x > w * 0.97f || y < h * 0.03f || y > h * 0.97f) {
            gv.ui.state = UIManager.UIState.CREATURES;
        }
        return true;
    }
}
