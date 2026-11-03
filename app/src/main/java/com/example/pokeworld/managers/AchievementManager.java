package com.example.pokeworld.managers;

import com.example.pokeworld.core.DatabaseHelper;
import com.example.pokeworld.entities.Companion;
import com.example.pokeworld.entities.Player;
import com.example.pokeworld.ui.UIManager;

import java.util.HashSet;
import java.util.Set;

public class AchievementManager {
    private Set<String> unlockedAchievements = new HashSet<>();
    private UIManager ui;
    private DatabaseHelper dbHelper;

    public AchievementManager(UIManager ui, DatabaseHelper dbHelper) {
        this.ui = ui;
        this.dbHelper = dbHelper;
        // Carregar conquistas do banco se necessário, por enquanto usamos volátil
    }

    public void checkAchievements(Player player, Companion companion, BestiaryManager bestiary) {
        // Conquista: Colecionador Iniciante
        if (player.getCreatureCollection().size() >= 3) {
            unlock("Colecionador Iniciante", "Possua 3 criaturas na coleção");
        }

        // Conquista: Magnata
        if (player.getGold() >= 1000) {
            unlock("Magnata", "Acumule 1000 moedas de ouro");
        }

        // Conquista: Mestre Treinador
        if (companion.getLevel() >= 10) {
            unlock("Mestre Treinador", "Alcance o nível 10 com um pet");
        }

        // Conquista: Explorador de Biomas
        if (bestiary.getSeenCreatures().size() >= 10) {
            unlock("Explorador", "Descubra 10 espécies diferentes");
        }
    }

    private void unlock(String title, String desc) {
        if (!unlockedAchievements.contains(title)) {
            unlockedAchievements.add(title);
            ui.showMessage("🏆 CONQUISTA: " + title);
            // Poderia adicionar recompensa em ouro aqui
        }
    }

    public Set<String> getUnlockedAchievements() {
        return unlockedAchievements;
    }
}
