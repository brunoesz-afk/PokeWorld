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
            player.addMission(new Mission("O Despertar", "Derrote 3 Pokémons na Floresta", "Bulbasaur", 3, 150));
            player.addMission(new Mission("O Chamado da Base", "Construa a Oficina na sua Base", "Oficina", 1, 300));
            player.addMission(new Mission("Mestre dos Mares", "Pesque 1 peixe no Lago", "Magikarp", 1, 100));
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
            boolean allAto1Completed = true;
            for (Mission m : player.getActiveMissions()) {
                if (m.getTitle().equals("O Despertar") || m.getTitle().equals("O Chamado da Base") || m.getTitle().equals("Mestre dos Mares")) {
                    if (m.isCompleted() && !m.isRewarded()) {
                        m.setRewarded(true);
                        player.addGold(m.getRewardGold());
                        ui.showMessage("💰 Recompensa: +" + m.getRewardGold() + " Ouro");
                    }
                    if (!m.isRewarded()) allAto1Completed = false;
                }
            }

            if (allAto1Completed) {
                giveAto2MissionsIfNeeded();
            }
        }
    }

    private void giveAto2MissionsIfNeeded() {
        boolean hasAto2 = false;
        for (Mission m : player.getActiveMissions()) {
            if (m.getTitle().equals("Desbravando as Minas")) {
                hasAto2 = true;
                break;
            }
        }
        if (!hasAto2) {
            player.addMission(new Mission("Desbravando as Minas", "Explore a Caverna MINE e colete minérios", "Ferro", 5, 250));
            player.addMission(new Mission("O Guardião de Pedra", "Derrote o Chefe Onix Corrompido na Mina", "Onix", 1, 500));
            ui.showMessage("📜 NOVO CAPÍTULO DESBLOQUEADO: Ato II - A Expansão do Domínio!");
        }
    }
}
