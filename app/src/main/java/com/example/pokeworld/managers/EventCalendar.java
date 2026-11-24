package com.example.pokeworld.managers;

import java.util.Calendar;

/** Calendário semanal: cada dia da semana tem um evento com um bônus global (só consulta a data do aparelho). */
public final class EventCalendar {
    private EventCalendar() {}

    public static final class Event {
        public final String name, icon, desc;
        public final double xp, gold, capture, bossDrop, towerGold, baseReward, expeditionTime, shiny;
        Event(String icon, String name, String desc, double xp, double gold, double capture, double bossDrop, double towerGold, double baseReward, double expeditionTime, double shiny) {
            this.icon = icon; this.name = name; this.desc = desc; this.xp = xp; this.gold = gold; this.capture = capture;
            this.bossDrop = bossDrop; this.towerGold = towerGold; this.baseReward = baseReward; this.expeditionTime = expeditionTime; this.shiny = shiny;
        }
    }

    /** Índice 0 = domingo ... 6 = sábado (igual ao Calendar.DAY_OF_WEEK - 1). */
    public static final Event[] WEEK = {
        new Event("✨", "Domingo Brilhante", "Pokémon shiny aparecem 3x mais.", 0, 0, 0, 0, 0, 0, 0, 2.0),
        new Event("🧠", "Segunda do Conhecimento", "+50% de XP para o companheiro.", 0.5, 0, 0, 0, 0, 0, 0, 0),
        new Event("🪙", "Terça do Ouro", "+50% de ouro dos inimigos.", 0, 0.5, 0, 0, 0, 0, 0, 0),
        new Event("🔴", "Quarta da Captura", "+20% de chance de captura.", 0, 0, 0.20, 0, 0, 0, 0, 0),
        new Event("👑", "Quinta dos Chefes", "Chefes soltam o dobro de itens raros (pedras, runas e gemas).", 0, 0, 0, 1.0, 0, 0, 0, 0),
        new Event("🗼", "Sexta da Torre", "Ouro da Torre em dobro.", 0, 0, 0, 0, 1.0, 0, 0, 0),
        new Event("🏰", "Sábado da Base", "Invasões pagam +60% e as expedições levam metade do tempo.", 0, 0, 0, 0, 0, 0.6, 0.5, 0),
    };

    public static int dayIndex() { return Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1; }
    public static Event today() { return WEEK[dayIndex()]; }

    public static double xpBonus() { return today().xp; }
    public static double goldBonus() { return today().gold; }
    public static double captureBonus() { return today().capture; }
    public static double bossDropBonus() { return today().bossDrop; }
    public static double towerGoldBonus() { return today().towerGold; }
    public static double baseRewardBonus() { return today().baseReward; }
    public static double expeditionTimeCut() { return today().expeditionTime; }
    public static double shinyBonus() { return today().shiny; }

    public static final String[] DAY_NAMES = {"Domingo", "Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado"};
}
