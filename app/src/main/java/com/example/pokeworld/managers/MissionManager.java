package com.example.pokeworld.managers;

import com.example.pokeworld.entities.Mission;
import com.example.pokeworld.entities.NPC;
import com.example.pokeworld.entities.Player;
import com.example.pokeworld.ui.UIManager;

public class MissionManager {
    private Player player;
    private UIManager ui;

    public MissionManager(Player player, UIManager ui) {
        this.player = player;
        this.ui = ui;
    }

    public void giveInitialMission() {
        if (player.getActiveMissions().isEmpty()) {
            player.addMission(new Mission("Primeira Caçada", "Capture 1 Slime", "Slime", 1, 100));
            player.addMission(new Mission("Pescador", "Pesque 2 Peixes", "Peixe", 2, 50));
            player.addMission(new Mission("Construir Oficina", "Construa a Oficina na sua Base", "Oficina", 1, 200));
        }
    }

    public void updateProgress(String type) {
        for (Mission m : player.getActiveMissions()) {
            if (!m.isCompleted()) {
                m.updateProgress(type);
                if (m.isCompleted()) {
                    ui.showMessage("Missão Concluída: " + m.getTitle());
                }
            }
        }
        updateNPCIcons();
    }

    private void updateNPCIcons() {
        if (player == null) return;
        // Essa lógica será chamada pelo WorldManager para atualizar os ícones dos NPCs no mapa
    }

    public void syncNPCs(java.util.List<NPC> npcs) {
        boolean hasCompleted = false;
        for (Mission m : player.getActiveMissions()) {
            if (m.isCompleted() && !m.isRewarded()) hasCompleted = true;
        }

        for (NPC n : npcs) {
            if (n.getType() == NPC.Type.GUARD) {
                n.setQuestIcon(hasCompleted ? NPC.QuestIcon.MISSION_READY : NPC.QuestIcon.NONE);
            }
        }
    }

    public void checkRewards(NPC npc) {
        if (npc.getName().equals("Mestre") || npc.getType() == NPC.Type.GUARD) {
            for (Mission m : player.getActiveMissions()) {
                if (m.isCompleted() && !m.isRewarded()) {
                    m.setRewarded(true);
                    player.addGold(m.getRewardGold());
                    ui.showMessage("💰 Recompensa: +" + m.getRewardGold() + " Ouro");
                }
            }
        }
    }
}
