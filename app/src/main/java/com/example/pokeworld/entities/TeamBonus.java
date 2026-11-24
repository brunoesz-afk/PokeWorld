package com.example.pokeworld.entities;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Bônus passivo do time: cada Pokémon que NÃO está lutando ajuda de acordo com seu tipo (mais forte com raridade e boost),
 * e combinações de tipos dão extras. Tudo calculado a partir dos 6 do time.
 */
public final class TeamBonus {
    public float atk, hp, regen, crit, cdr, xp, luck, resist, status;
    public final List<String> notes = new ArrayList<>();

    /** Chave curta para saber se o resultado mudou (evita recalcular os stats à toa). */
    public String signature() {
        return String.format(java.util.Locale.US, "%.3f|%.3f|%.3f|%.3f|%.3f|%.3f|%.3f|%.3f|%.3f", atk, hp, regen, crit, cdr, xp, luck, resist, status);
    }

    public static TeamBonus compute(List<CapturedCreature> party, String activeSpecies) {
        TeamBonus b = new TeamBonus();
        Set<ElementalType> types = EnumSet.noneOf(ElementalType.class);
        int[] count = new int[ElementalType.values().length];
        for (CapturedCreature c : party) {
            ElementalType t = SpeciesTypes.of(c.name);
            types.add(t); count[t.ordinal()]++;
            if (c.name.equalsIgnoreCase(activeSpecies)) continue;          // quem luta não "apoia"
            float k = (1f + 0.25f * c.rarity.ordinal()) * (1f + 0.05f * c.boost);   // raridade e boost fortalecem o apoio
            switch (t) {
                case NORMAL: b.luck += 0.04f * k; break;
                case FIRE: b.atk += 0.04f * k; break;
                case WATER: b.regen += 0.30f * k; break;
                case PLANT: b.hp += 0.04f * k; break;
                case ELECTRIC: b.cdr += 0.04f * k; break;
                case ICE: b.status += 0.04f * k; break;
                case ROCK: b.resist += 0.04f * k; break;
                case POISON: b.crit += 0.03f * k; break;
                case PSYCHIC: b.xp += 0.05f * k; break;
                case DRAGON: b.atk += 0.03f * k; b.crit += 0.02f * k; break;
            }
        }
        // combos
        if (types.size() >= 4) { b.atk += 0.06f; b.hp += 0.06f; b.notes.add("Time Completo (4+ tipos): +6% ataque e vida"); }
        else if (types.size() == 3) { b.atk += 0.03f; b.hp += 0.03f; b.notes.add("Time Variado (3 tipos): +3% ataque e vida"); }
        if (types.contains(ElementalType.FIRE) && types.contains(ElementalType.WATER) && types.contains(ElementalType.PLANT)) {
            b.atk += 0.08f; b.notes.add("Trio Inicial (fogo+água+planta): +8% ataque");
        }
        if (types.contains(ElementalType.ELECTRIC) && types.contains(ElementalType.WATER)) { b.crit += 0.03f; b.notes.add("Tempestade (elétrico+água): +3% crítico"); }
        for (ElementalType t : ElementalType.values()) {
            if (count[t.ordinal()] >= 2) { b.hp += 0.02f; b.notes.add("Sinergia de " + t.name().toLowerCase() + " (2+): +2% vida"); break; }
        }
        b.cdr = Math.min(b.cdr, 0.35f); b.resist = Math.min(b.resist, 0.30f);
        return b;
    }

    /** Resumo curto para a tela do Time. */
    public String summary() {
        StringBuilder sb = new StringBuilder();
        if (atk > 0) sb.append("⚔ +").append(Math.round(atk * 100)).append("%  ");
        if (hp > 0) sb.append("❤ +").append(Math.round(hp * 100)).append("%  ");
        if (regen > 0) sb.append("💧 regen +").append(Math.round(regen * 100)).append("%  ");
        if (cdr > 0) sb.append("⏱ -").append(Math.round(cdr * 100)).append("% recarga  ");
        if (crit > 0) sb.append("⭐ crít +").append(Math.round(crit * 100)).append("%  ");
        if (resist > 0) sb.append("🛡 +").append(Math.round(resist * 100)).append("%  ");
        if (xp > 0) sb.append("✨ XP +").append(Math.round(xp * 100)).append("%  ");
        if (luck > 0) sb.append("🍀 +").append(Math.round(luck * 100)).append("%  ");
        if (status > 0) sb.append("❄ estados +").append(Math.round(status * 100)).append("%  ");
        return sb.length() == 0 ? "sem bônus" : sb.toString().trim();
    }
}
