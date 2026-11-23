package com.example.pokeworld.managers;

import com.example.pokeworld.entities.*;
import com.example.pokeworld.core.*;

import java.util.Random;

public class CombatManager {
    private Random random = new Random();
    private GameView gv;

    public CombatManager(GameView gv) {
        this.gv = gv;
    }

    public void attemptCaptureDefeated(com.example.pokeworld.entities.Loot target) {
        Integer ballsObj = gv.player.getInventory().getOrDefault("Pokébola", 0);
        int balls = ballsObj != null ? ballsObj : 0;
        
        if (balls <= 0) {
            gv.ui.showMessage("❌ Você não tem Pokébolas!");
            return;
        }

        int pct = (int) Math.round(100 * com.example.pokeworld.entities.Progression.captureChance(
                target.getOwnerLevel(), target.isOwnerBoss(), target.isShiny(), gv.companion.getLevel(),
                com.example.pokeworld.entities.TalentTree.bonus(gv.player, "Domador") * 0.8));
        gv.ui.showMessage("Tentando capturar... (chance " + pct + "%)");
        
        // Dispara o efeito visual da Pokébola
        gv.effects.addCaptureEffect(gv.player.getPositionX(), gv.player.getPositionY(), target, () -> {
            processCaptureDefeated(target);
        });
    }

    private void processCaptureDefeated(com.example.pokeworld.entities.Loot target) {
        if (!gv.world.loots.contains(target)) return;

        // Quanto mais forte o alvo (nível, chefe, shiny), mais difícil; seu nível e o talento Domador ajudam
        double finalChance = com.example.pokeworld.entities.Progression.captureChance(
                target.getOwnerLevel(), target.isOwnerBoss(), target.isShiny(), gv.companion.getLevel(),
                com.example.pokeworld.entities.TalentTree.bonus(gv.player, "Domador") * 0.8);

        if (random.nextDouble() < finalChance) {
            if (random.nextDouble() >= com.example.pokeworld.entities.TalentTree.bonus(gv.player, "Coletor")) gv.player.useItem("Pokébola");
            
            Rarity rarity = Rarity.COMMON;
            double r = random.nextDouble();
            if (r < 0.005) rarity = Rarity.LEGENDARY;
            else if (r < 0.03) rarity = Rarity.EPIC;
            else if (r < 0.10) rarity = Rarity.RARE;
            else if (r < 0.30) rarity = Rarity.UNCOMMON;

            int gender = random.nextDouble() < 0.5 ? 0 : 1;
            // Mantém o estado Shiny do corpo original
            gv.player.addCreatureToCollection(target.getName(), target.isShiny(), rarity, gender); 
            gv.bestiary.markAsCaptured(target.getName());
            gv.world.loots.remove(target);
            gv.ui.showMessage("✨ " + target.getName() + " CAPTURADO!");
            gv.missions.updateProgress(target.getName());
            if (gv.tutorial != null) gv.tutorial.onTutorialRattataCaptured();
            gv.particles.addExplosion(target.getPositionX(), target.getPositionY(), android.graphics.Color.GREEN, 40);
        } else {
            gv.player.useItem("Pokébola");
            gv.ui.showMessage("❌ A captura falhou!");
            gv.world.loots.remove(target);
            // Efeito de Pokébola quebrando (Partículas Cinzas e Brancas)
            gv.particles.addExplosion(target.getPositionX(), target.getPositionY(), android.graphics.Color.WHITE, 20);
            gv.particles.addExplosion(target.getPositionX(), target.getPositionY(), android.graphics.Color.LTGRAY, 15);
        }

    }

    /**
     * Lógica de Captura com Bônus de Talento "Domador" e Chaining
     */
    public void attemptCapture() {
        Integer ballsObj = gv.player.getInventory().getOrDefault("Pokébola", 0);
        int balls = ballsObj != null ? ballsObj : 0;
        
        if (balls <= 0) {
            gv.ui.showMessage("❌ Você não tem Pokébolas!");
            return;
        }

        Enemy target = null;
        for (Enemy e : gv.world.enemies) {
            if (e.isSelected() && e.isAlive()) {
                target = e;
                break;
            }
        }

        if (target == null) return;
        if (target.isBoss) {
            gv.ui.showMessage("❌ Bosses não podem ser capturados!");
            return;
        }

        // Lança a Pokébola visualmente
        gv.effects.addProjectile("pokebola", gv.player.getPositionX(), gv.player.getPositionY(), target.positionX, target.positionY, 25, 0, null);

        // Inicia animação de balanço
        gv.ui.showMessage("Tentando capturar " + target.name + "...");
        gv.startShake(30, 5); // Tremor leve simulando o balanço
        
        final Enemy captureTarget = target;
        // Simulação de delay para a animação de balanço
        new android.os.Handler().postDelayed(() -> {
            processCapture(captureTarget);
        }, 1500);
    }

    private void processCapture(Enemy target) {
        if (!target.isAlive || !gv.world.enemies.contains(target)) return;

        double ballMultiplier = 1.0;
        String ballUsed = "Pokébola";
        if (gv.player.getInventory().getOrDefault("Ultra Ball", 0) > 0) {
            ballMultiplier = 2.0; ballUsed = "Ultra Ball";
        } else if (gv.player.getInventory().getOrDefault("Great Ball", 0) > 0) {
            ballMultiplier = 1.5; ballUsed = "Great Ball";
        }

        // Chance baseada no HP
        double healthRatio = (double) target.hp / target.maxHp;
        double baseChance = (0.3 + (1.0 - healthRatio) * 0.5) * ballMultiplier;
        
        // Bônus do Talento Domador (+10% por nível)
        int domadorLevel = gv.player.getTalents().getOrDefault("Domador", 0);
        double talentBonus = domadorLevel * 0.10;
        
        // Bônus de Chain (Combo de Captura)
        double chainBonus = gv.world.getCaptureChain() * 0.02; 
        
        double finalChance = baseChance + talentBonus + chainBonus;

        if (random.nextDouble() < finalChance) {
            if (random.nextDouble() >= com.example.pokeworld.entities.TalentTree.bonus(gv.player, "Coletor")) gv.player.useItem(ballUsed); // Consome a bola usada (Coletor pode poupar)
            gv.world.onCapture(target.name); 
            
            // ... (resto da lógica de captura)
            gv.ui.showMessage("✨ " + target.name + " CAPTURADO COM " + ballUsed.toUpperCase() + "!");
            gv.missions.updateProgress(target.name);
            gv.particles.addExplosion(target.positionX, target.positionY, android.graphics.Color.GREEN, 40);
        } else {
            gv.player.useItem("Pokébola"); // Consome a pokébola na falha também
            gv.ui.showMessage("❌ O " + target.name + " escapou!");
            gv.particles.addExplosion(target.positionX, target.positionY, android.graphics.Color.WHITE, 10);
            // Inimigo fica mais agressivo ao escapar
            target.hp = Math.min(target.maxHp, target.hp + 20); 
        }
    }
}
