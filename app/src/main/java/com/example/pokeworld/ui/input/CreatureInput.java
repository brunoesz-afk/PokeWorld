package com.example.pokeworld.ui.input;

import android.graphics.RectF;
import com.example.pokeworld.core.GameView;
import com.example.pokeworld.entities.CapturedCreature;
import com.example.pokeworld.entities.Equipment;
import com.example.pokeworld.entities.PetData;
import com.example.pokeworld.ui.UIManager;
import com.example.pokeworld.ui.handlers.CreatureUI;

import java.util.List;

public class CreatureInput implements InputExtension {
    @Override
    public boolean handle(float x, float y, GameView gv) {
        CreatureUI ui = (CreatureUI) gv.ui.getHandler(UIManager.UIState.CREATURES);
        if (ui == null) return false;

        if (ui.exitBtn.contains(x, y)) {
            gv.ui.state = UIManager.UIState.EXPLORING;
            return true;
        }

        List<CapturedCreature> party = gv.player.getParty();
        float startY = 150;
        float cardH = 80;
        float spacing = 12;

        for (int i = 0; i < party.size(); i++) {
            RectF card = new RectF(80, startY + (i * (cardH + spacing)), gv.getWidth() - 80, startY + (i * (cardH + spacing)) + cardH);
            if (card.contains(x, y)) {
                CapturedCreature selected = party.get(i);

                // Salva pet atual
                gv.activity.dbHelper.savePet(
                    gv.companion.getSpecies(),
                    gv.companion.getLevel(),
                    gv.companion.getExp(),
                    gv.companion.getForgeAttackBonus(),
                    gv.companion.getWeapon() != null ? gv.companion.getWeapon().getName() : "Nenhum",
                    gv.companion.getArmor() != null ? gv.companion.getArmor().getName() : "Nenhum",
                    gv.companion.isShiny(),
                    gv.companion.getPersonality(),
                    gv.companion.getRune() != null ? gv.companion.getRune().getName() : "Nenhum",
                    gv.companion.getFriendship(),
                    gv.companion.getType2() != null ? gv.companion.getType2().name() : "Nenhum",
                    gv.companion.getRarity(),
                    gv.companion.getGender()
                );

                String species = selected.name;
                gv.companion.setSpecies(species);
                PetData data = gv.activity.dbHelper.getPetData(species);
                gv.companion.loadProgress(data.level, data.exp, data.forgeBonus, selected.isShiny, data.friendship, data.type2, data.rarity, selected.gender);
                gv.companion.setWeapon(Equipment.create(data.weapon));
                gv.companion.setArmor(Equipment.create(data.armor));
                gv.applyKingdomBonuses();
                gv.player.setMounted(false);
                gv.ui.showMessage("🐾 Companheiro ativo: " + species);
                gv.activity.saveCurrentGame();
                return true;
            }
        }

        gv.ui.state = UIManager.UIState.EXPLORING;
        return true;
    }
}
