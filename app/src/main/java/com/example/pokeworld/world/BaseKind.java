package com.example.pokeworld.world;

/**
 * Tipos de peças que o jogador pode colocar na base (grade de tiles).
 * Tudo que é número de balanceamento fica aqui, em um lugar só.
 */
public enum BaseKind {
    //          nome                 ícone  w  h  categoria        nvMax tempo madeira ferro ouro  sprite
    QUARTEL    ("Quartel",           "🏰",  4, 4, Cat.NUCLEO,      5,   120,  40,   20,  500,  null),
    MURALHA    ("Muralha",           "🧱",  1, 1, Cat.DEFESA,      6,   0,    0,    0,   0,    null),
    TORRE_VIGIA("Torre de Vigia",    "🗼",  2, 2, Cat.DEFESA,      5,   20,   15,   10,  150,  null),
    TORRE_CHOQUE("Torre Choque",     "⚡",  3, 3, Cat.DEFESA,      5,   30,   0,    40,  300,  "torre_choque"),
    TOCHA      ("Tocha",             "🔥",  1, 1, Cat.UTIL,        1,   3,    5,    0,   0,    null),
    HORTA      ("Horta",             "🌱",  2, 2, Cat.UTIL,        5,   5,    5,    0,   0,    null),
    BAU        ("Baú",               "📦",  2, 2, Cat.UTIL,        3,   10,   10,   0,   0,    null),
    OFICINA    ("Oficina",           "🔨",  5, 4, Cat.UTIL,        5,   5,    5,    2,   0,    "oficina_pokebola"),
    DAYCARE    ("Daycare",           "🥚",  6, 3, Cat.UTIL,        5,   30,   15,   5,   0,    "construcao_daycare"),
    LABORATORIO("Laboratório",       "🧪",  4, 4, Cat.UTIL,        5,   60,   30,   15,  0,    null),
    FABRICA    ("Fábrica de Montaria","🐎", 6, 4, Cat.UTIL,        5,   60,   50,   0,   0,    "fabrica_montaria"),
    ACADEMIA   ("Academia",          "🏋️", 5, 4, Cat.UTIL,        5,   30,   20,   10,  0,    "ginasio_building");

    public enum Cat { NUCLEO, DEFESA, UTIL }

    public final String label, icon, sprite;
    public final int w, h, maxLevel, buildSeconds, wood, iron, gold;
    public final Cat cat;

    BaseKind(String label, String icon, int w, int h, Cat cat, int maxLevel, int buildSeconds,
             int wood, int iron, int gold, String sprite) {
        this.label = label; this.icon = icon; this.w = w; this.h = h; this.cat = cat;
        this.maxLevel = maxLevel; this.buildSeconds = buildSeconds;
        this.wood = wood; this.iron = iron; this.gold = gold; this.sprite = sprite;
    }

    // ----- Muralha: tabela própria (6 níveis) -----
    public static final String[] WALL_NAMES = {"Madeira", "Pedra", "Ferro", "Aço", "Ouro", "Obsidiana"};
    private static final int[][] WALL_COST = {   // madeira, ferro, ouro
            {1, 0, 0}, {2, 1, 0}, {0, 3, 0}, {0, 6, 100}, {0, 10, 400}, {0, 16, 1200}};
    public static final int[] WALL_HP = {100, 300, 600, 1000, 1600, 2500};

    /** Custo para chegar ao nível dado (nível 1 = custo de construir). {madeira, ferro, ouro}. */
    public int[] cost(int level) {
        level = Math.max(1, Math.min(level, maxLevel));
        if (this == MURALHA) return WALL_COST[level - 1].clone();
        double m = Math.pow(level, 1.6);
        return new int[]{(int) Math.round(wood * m), (int) Math.round(iron * m), (int) Math.round(gold * m)};
    }

    /** Tempo (s) para construir (level==1) ou melhorar até o nível dado. */
    public int seconds(int level) {
        if (this == MURALHA) return 0;
        if (level <= 1) return buildSeconds;
        return Math.max(10, buildSeconds) * (level - 1);
    }

    /** Nome usado pelo sistema antigo (missões, bônus do reino). */
    public String legacyName() {
        switch (this) {
            case OFICINA: return "Oficina";
            case DAYCARE: return "Daycare";
            case LABORATORIO: return "Laboratorio";
            case FABRICA: return "Fabrica de Montaria";
            case TORRE_CHOQUE: return "Torre Choque";
            case TOCHA: return "Tocha";
            case BAU: return "Baú";
            case ACADEMIA: return "Academia";
            default: return label;
        }
    }

    /** Quanto tempo uma plantação leva para amadurecer (a Horta mais evoluída cresce mais rápido). */
    public static int cropSeconds(int level) { return Math.max(30, 90 - 12 * (Math.max(1, level) - 1)); }

    public boolean isProcedural() { return sprite == null; }
    public boolean isWall() { return this == MURALHA; }
    public boolean isDefense() { return cat == Cat.DEFESA; }

    /** Quantidade máxima permitida deste tipo para o nível do Quartel. */
    public int maxCount(int hq) {
        switch (this) {
            case QUARTEL: return 1;
            case MURALHA: return 200 + 100 * (hq - 1);
            case TORRE_VIGIA: return 4 + 2 * (hq - 1);
            case TORRE_CHOQUE: return 1 + hq / 2;
            case TOCHA: return 4 + 2 * hq;
            case HORTA: return 2 + 2 * hq;
            case BAU: return 1 + hq;
            default: return 1;
        }
    }

    /** Nível máximo permitido para o nível do Quartel (o Quartel limita todo o resto). */
    public int levelCap(int hq) {
        if (this == QUARTEL) return maxLevel;
        if (this == MURALHA) return Math.min(maxLevel, 2 + hq);
        return Math.min(maxLevel, hq);
    }

    public String description() {
        switch (this) {
            case QUARTEL: return "Centro da base. Seu nível libera mais peças, níveis e guardiões.";
            case MURALHA: return "Barra invasores. Liga sozinha com as vizinhas. Quanto maior o nível, mais resistente.";
            case TORRE_VIGIA: return "Atira em inimigos que chegam perto da base.";
            case TORRE_CHOQUE: return "Dispara raios. Exige um Pikachu/Raichu.";
            case TOCHA: return "Ilumina a base à noite.";
            case HORTA: return "Plante 1 Fruta e colha várias. Frutas viram Pokébolas, Poções e Doces.";
            case BAU: return "Guarda itens da base.";
            case OFICINA: return "Fabrica Pokébolas e ferramentas.";
            case DAYCARE: return "Cruza seus Pokémons.";
            case LABORATORIO: return "Invoca novos Pokémons.";
            case FABRICA: return "Sela e equipamentos de montaria.";
            case ACADEMIA: return "Treina os Pokémons guardados na base.";
        }
        return "";
    }

    /** Nome antigo (tabela buildings) -> tipo. null se não existir. */
    public static BaseKind fromLegacyName(String n) {
        if (n == null) return null;
        String s = n.trim().toLowerCase();
        switch (s) {
            case "oficina": return OFICINA;
            case "daycare": return DAYCARE;
            case "laboratorio": case "laboratório": return LABORATORIO;
            case "fabrica de montaria": case "fábrica de montaria": return FABRICA;
            case "torre choque": return TORRE_CHOQUE;
            case "tocha": return TOCHA;
            case "baú": case "bau": return BAU;
            case "academia": return ACADEMIA;
        }
        return null;
    }

    public static BaseKind fromId(String id) {
        try { return valueOf(id); } catch (Exception e) { return null; }
    }
}
