package com.example.pokeworld.managers;

import java.util.ArrayList;
import java.util.List;

/** Skins cosméticas (nenhuma dá vantagem em combate). Compradas com Pontos ou ganhas no Passe. */
public final class SkinManager {
    private SkinManager() {}

    public enum Kind { POKEMON, CHAR }

    public static final class Skin {
        public final String id, name, icon, source;   // source != null → exclusiva (não está à venda)
        public final Kind kind;
        public final int price;
        public final float hue, sat, r, g, b;
        public final boolean rainbow;
        public final int aura;                        // 0 = sem aura
        Skin(String id, Kind kind, String name, String icon, int price, float hue, float sat, float r, float g, float b, boolean rainbow, int aura, String source) {
            this.id = id; this.kind = kind; this.name = name; this.icon = icon; this.price = price;
            this.hue = hue; this.sat = sat; this.r = r; this.g = g; this.b = b; this.rainbow = rainbow; this.aura = aura; this.source = source;
        }
        public boolean forSale() { return source == null; }
    }

    public static final List<Skin> ALL = new ArrayList<>();
    private static void add(Skin s) { ALL.add(s); }

    static {
        // Pokémon (a ordem NÃO pode mudar: o índice é o que fica salvo; só acrescente no fim)
        add(new Skin("p_gelo",    Kind.POKEMON, "Gelo",         "❄️", 3, 170, 1.0f, 0.9f, 1.0f, 1.15f, false, 0x8870D8FF, null));
        add(new Skin("p_chama",   Kind.POKEMON, "Chama",        "🔥", 3, -35, 1.3f, 1.15f, 0.95f, 0.8f, false, 0x88FF7A30, null));
        add(new Skin("p_floresta",Kind.POKEMON, "Floresta",     "🌿", 3,  85, 1.1f, 0.9f, 1.1f, 0.9f, false, 0, null));
        add(new Skin("p_sombra",  Kind.POKEMON, "Sombra",       "🌑", 5,   0, 0.35f, 0.55f, 0.5f, 0.7f, false, 0x889040FF, null));
        add(new Skin("p_ouro",    Kind.POKEMON, "Dourado",      "👑", 6,   0, 0.6f, 1.3f, 1.1f, 0.55f, false, 0x99FFD23C, null));
        add(new Skin("p_arco",    Kind.POKEMON, "Arco-íris",    "🌈", 8,   0, 1.2f, 1.0f, 1.0f, 1.0f, true, 0x88FFFFFF, null));
        // Personagem
        add(new Skin("c_azul",    Kind.CHAR, "Traje Azul",      "🔵", 4, 190, 1.1f, 1.0f, 1.0f, 1.1f, false, 0, null));
        add(new Skin("c_carmesim",Kind.CHAR, "Carmesim",        "🔴", 4, -40, 1.3f, 1.1f, 0.95f, 0.9f, false, 0, null));
        add(new Skin("c_esmeralda",Kind.CHAR,"Esmeralda",       "🟢", 4,  95, 1.1f, 0.95f, 1.05f, 0.95f, false, 0, null));
        add(new Skin("c_noturna", Kind.CHAR, "Noturna",         "🌙", 6,   0, 0.3f, 0.5f, 0.5f, 0.75f, false, 0x889040FF, null));
        add(new Skin("c_realeza", Kind.CHAR, "Realeza Dourada", "👑", 8,   0, 0.6f, 1.3f, 1.1f, 0.55f, false, 0x99FFD23C, null));
        add(new Skin("c_neon",    Kind.CHAR, "Neon",            "🌈", 10,  0, 1.3f, 1.0f, 1.0f, 1.0f, true, 0x88FFFFFF, null));
        // Exclusivas do Passe
        add(new Skin("p_aurora",  Kind.POKEMON, "Aurora",       "🌌", 0, 200, 1.2f, 0.9f, 1.05f, 1.2f, false, 0xAA66FFE0, "Passe • nível 10"));
        add(new Skin("c_eclipse", Kind.CHAR, "Eclipse",         "🌘", 0, 270, 1.2f, 0.85f, 0.8f, 1.15f, false, 0xAAB070FF, "Passe • nível 20"));
        add(new Skin("p_lendario",Kind.POKEMON, "Lendário",     "🐉", 0,   0, 1.3f, 1.15f, 1.1f, 0.7f, true, 0xCCFFE066, "Passe • nível 30"));
    }

    public static Skin byId(String id) {
        for (Skin s : ALL) if (s.id.equals(id)) return s;
        return null;
    }

    public static List<Skin> of(Kind k) {
        List<Skin> out = new ArrayList<>();
        for (Skin s : ALL) if (s.kind == k) out.add(s);
        return out;
    }

    // ------------------------------------------------------------------ posse e uso
    public static boolean owned(RewardManager.Store st, Skin s) { return st.get("skin_own_" + s.id, 0) == 1; }
    public static void grant(RewardManager.Store st, Skin s) { st.set("skin_own_" + s.id, 1); }

    /** 0 = comprou; 1 = já tinha; 2 = não está à venda; 3 = pontos insuficientes. */
    public static int buy(RewardManager.Store st, Skin s) {
        if (owned(st, s)) return 1;
        if (!s.forSale()) return 2;
        if (!PointsManager.spend(st, s.price)) return 3;
        grant(st, s);
        return 0;
    }

    public static Skin equipped(RewardManager.Store st, Kind k) {
        if (st == null) return null;
        int idx = st.get("skin_eq_" + k.name(), 0) - 1;
        if (idx < 0 || idx >= ALL.size()) return null;
        Skin s = ALL.get(idx);
        return s.kind == k && owned(st, s) ? s : null;
    }

    /** Equipa a skin; se ela já era a equipada, tira (volta ao visual normal). Retorna true se ficou equipada. */
    public static boolean toggle(RewardManager.Store st, Skin s) {
        if (!owned(st, s)) return false;
        Skin cur = equipped(st, s.kind);
        if (cur == s) { st.set("skin_eq_" + s.kind.name(), 0); return false; }
        st.set("skin_eq_" + s.kind.name(), ALL.indexOf(s) + 1);
        return true;
    }
}
