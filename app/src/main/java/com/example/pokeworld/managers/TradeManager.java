package com.example.pokeworld.managers;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.entities.CapturedCreature;
import com.example.pokeworld.entities.Rarity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Troca de Pokémon duplicados por itens (Mercado de Trocas da base). */
public final class TradeManager {
    private TradeManager() {}

    public static final class Offer {
        public String species, rarityLabel, rewardText; public int rarityColor, spare; public boolean shiny; public Rarity rarity;
    }

    private static String key(CapturedCreature c) { return c.name.toLowerCase() + "|" + c.rarity.ordinal() + "|" + c.isShiny; }

    /** Cada grupo (espécie+raridade+brilho) com 2+ cópias oferece (cópias fora do time sem boost, sempre sobrando 1). */
    public static List<Offer> offers(GameView gv) {
        Map<String, List<CapturedCreature>> groups = new LinkedHashMap<>();
        for (CapturedCreature c : gv.player.getCreatureCollection()) {
            List<CapturedCreature> l = groups.get(key(c));
            if (l == null) { l = new ArrayList<>(); groups.put(key(c), l); }
            l.add(c);
        }
        List<Offer> out = new ArrayList<>();
        for (List<CapturedCreature> g : groups.values()) {
            if (g.size() < 2) continue;
            int free = 0;
            for (CapturedCreature c : g) if (!c.isInParty && c.boost == 0) free++;
            int spare = Math.min(free, g.size() - 1);
            if (spare <= 0) continue;
            CapturedCreature c = g.get(0);
            Offer o = new Offer();
            o.species = c.name; o.shiny = c.isShiny; o.rarity = c.rarity; o.rarityLabel = c.rarity.getLabel(); o.rarityColor = c.rarity.getColor(); o.spare = spare;
            o.rewardText = describe(c.rarity, c.isShiny);
            out.add(o);
        }
        return out;
    }

    private static int gold(Rarity r) { return new int[]{30, 60, 120, 250, 500}[r.ordinal()]; }

    private static String describe(Rarity r, boolean shiny) {
        StringBuilder sb = new StringBuilder("🪙 " + Economy.scale(gold(r) * (shiny ? 2 : 1), Economy.REWARD));
        switch (r) {
            case COMMON: sb.append("  +2 Poção"); break;
            case UNCOMMON: sb.append("  +2 Great Ball"); break;
            case RARE: sb.append("  +1 Pedra, +3 Ferro"); break;
            case EPIC: sb.append("  +1 Gema de Boost"); break;
            case LEGENDARY: sb.append("  +2 Gema de Boost"); break;
        }
        if (shiny) sb.append("  +1 Fragmento Shiny");
        return sb.toString();
    }

    public static String trade(GameView gv, Offer o) {
        CapturedCreature give = null;
        int total = 0;
        for (CapturedCreature c : gv.player.getCreatureCollection()) {
            if (c.name.equalsIgnoreCase(o.species) && c.rarity == o.rarity && c.isShiny == o.shiny) {
                total++;
                if (give == null && !c.isInParty && c.boost == 0) give = c;
            }
        }
        if (give == null || total < 2) return "Você não tem mais cópias sobrando.";
        gv.player.getCreatureCollection().remove(give);
        int g = Economy.scale(gold(o.rarity) * (o.shiny ? 2 : 1), Economy.REWARD);
        gv.player.addGold(g);
        switch (o.rarity) {
            case COMMON: gv.player.addItem("Poção", 2); break;
            case UNCOMMON: gv.player.addItem("Great Ball", 2); break;
            case RARE: gv.player.addItem("Pedra", 1); gv.player.addItem("Ferro", 3); break;
            case EPIC: gv.player.addItem("Gema de Boost", 1); break;
            case LEGENDARY: gv.player.addItem("Gema de Boost", 2); break;
        }
        if (o.shiny) gv.player.addItem("Fragmento Shiny", 1);
        gv.world.refreshGuards(gv.player);
        SoundManager.play("buy");
        gv.activity.saveCurrentGame();
        return "🔁 " + o.species + " trocado: " + describe(o.rarity, o.shiny);
    }
}
