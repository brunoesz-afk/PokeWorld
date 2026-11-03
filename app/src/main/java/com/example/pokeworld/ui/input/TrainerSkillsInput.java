package com.example.pokeworld.ui.input;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.entities.Companion;
import com.example.pokeworld.entities.Player;
import com.example.pokeworld.entities.TrainerSkill;
import com.example.pokeworld.ui.UIManager;
import com.example.pokeworld.ui.handlers.TrainerSkillsUI;
import java.util.List;

public class TrainerSkillsInput implements InputExtension {
    @Override
    public boolean handle(float x, float y, GameView gv) {
        TrainerSkillsUI ui = (TrainerSkillsUI) gv.ui.getHandler(UIManager.UIState.TRAINER_SKILLS);
        
        if (ui.backBtn.contains(x, y)) {
            gv.ui.state = UIManager.UIState.EXPLORING;
            return true;
        }

        Player p = gv.player;
        Companion c = gv.companion;
        List<TrainerSkill> skills = p.getTrainerSkills();

        for (int i = 0; i < ui.skillButtons.size(); i++) {
            if (ui.skillButtons.get(i).contains(x, y)) {
                TrainerSkill s = skills.get(i);
                if (s.canUse(p.getStamina())) {
                    executeSkill(s, p, c, gv);
                    s.use();
                    gv.ui.state = UIManager.UIState.EXPLORING;
                    return true;
                } else {
                    gv.ui.showMessage("❌ Sem Stamina ou em Cooldown!");
                }
            }
        }
        
        return true;
    }

    private void executeSkill(TrainerSkill s, Player p, Companion c, GameView gv) {
        p.useStamina(s.getStaminaCost());

        switch(s.getEffectType()) {
            case "HEAL":
                int heal = (int)(c.getMaxHp() * 0.3);
                c.heal(heal);
                gv.ui.showMessage("✨ CURA DIVINA!");
                gv.particles.addExplosion(c.getPositionX(), c.getPositionY(), s.getColor(), 40);
                break;
            case "BUFF":
                c.applyAttackBuff(1.5f, 15000);
                gv.ui.showMessage("🔥 GRITO DE GUERRA!");
                gv.particles.addExplosion(c.getPositionX(), c.getPositionY(), s.getColor(), 50);
                break;
            case "SHIELD":
                c.setShield(5000);
                gv.ui.showMessage("🛡️ ESCUDO ARCANO!");
                gv.particles.addExplosion(c.getPositionX(), c.getPositionY(), s.getColor(), 60);
                break;
        }
    }
}
