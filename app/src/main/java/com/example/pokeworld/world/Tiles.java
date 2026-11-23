package com.example.pokeworld.world;

/**
 * Constantes de tiles e predicados usados pelos geradores de mapa (MapBuilder)
 * e pelo renderizador (TileRenderer). Sem dependências do Android para poder
 * ser testado no computador.
 */
public final class Tiles {
    private Tiles() {}

    // Tiles originais (mesmos valores de antes, nada quebra)
    public static final int GRASS = 0, TREE = 1, PORTAL = 2, WATER = 3, FLOOR = 4, ROCK = 5, SAND = 6, SNOW = 7,
            BRICK = 8, LAVA = 9, DEEP_WATER = 10, CLOUD = 11, OBSIDIAN = 12, BUSH = 13, ROAD = 14, SIGN = 15,
            SOIL = 16, BERRY_TREE = 17, STONE_PAVEMENT = 18, CARPET = 19, SNOW_CRYSTAL = 20;
    public static final int BASALT_FLOOR = 21, ASH_GROUND = 22, CRACKED_OBSIDIAN = 23, MAGMA_BRIDGE = 24,
            SULFUR_CRYSTAL = 25, MAGMA_VENT = 26;

    // Tiles novos (conectáveis)
    /** Muro de construção (tijolos). Muda de aparência conforme os vizinhos. */
    public static final int WALL = 27;
    /** Penhasco / parede de rocha. Forma degraus de altura e se liga aos vizinhos. */
    public static final int CLIFF = 28;
    /** Escada (caminhável). Corta penhascos e muros ligando dois níveis. */
    public static final int STAIRS = 29;
    /** Ponte de madeira (caminhável) sobre água ou lava. */
    public static final int BRIDGE = 30;
    public static final int FLOWERS = 31;
    /** Telhado de casa (bloqueia). Fica sempre acima de HOUSE. */
    public static final int ROOF = 32;
    /** Parede de casa com janela (bloqueia). */
    public static final int HOUSE = 33;
    public static final int DOOR = 34;
    public static final int PILLAR = 35;
    public static final int BRAZIER = 36;
    public static final int DEAD_TREE = 37;
    public static final int CACTUS = 38;
    public static final int FOUNTAIN = 39;
    public static final int TOMBSTONE = 40;

    public static final int COUNT = 41;

    /** Massa sólida desenhada como parede (rosto + topo). */
    public static boolean isMass(int t) {
        return t == CLIFF || t == WALL || t == BRICK || t == ROOF || t == HOUSE || t == DOOR || t == CRACKED_OBSIDIAN;
    }

    public static boolean isWater(int t) { return t == WATER || t == DEEP_WATER; }

    public static boolean isLiquid(int t) { return t == WATER || t == DEEP_WATER || t == LAVA || t == MAGMA_VENT; }

    /** Chão simples onde se pode pisar e que recebe transições suaves. */
    public static boolean isGround(int t) {
        switch (t) {
            case GRASS: case SAND: case SNOW: case ROAD: case STONE_PAVEMENT: case FLOOR: case SOIL: case CARPET:
            case ASH_GROUND: case BASALT_FLOOR: case OBSIDIAN: case FLOWERS: case CLOUD:
                return true;
            default:
                return false;
        }
    }

    /** Objetos que ficam em cima de um chão (precisam de chão por baixo). */
    public static boolean isObject(int t) {
        switch (t) {
            case TREE: case BERRY_TREE: case BUSH: case ROCK: case SIGN: case SNOW_CRYSTAL: case SULFUR_CRYSTAL:
            case PILLAR: case BRAZIER: case DEAD_TREE: case CACTUS: case TOMBSTONE: case PORTAL:
                return true;
            default:
                return false;
        }
    }

    /** Objetos altos que ultrapassam o tile e são desenhados numa segunda passada. */
    public static boolean isTall(int t) {
        return t == TREE || t == BERRY_TREE || t == SNOW_CRYSTAL || t == DEAD_TREE || t == PILLAR || t == CACTUS;
    }

    public static boolean blocksMovement(int t) {
        switch (t) {
            case TREE: case ROCK: case SIGN: case BERRY_TREE: case BRICK: case SNOW_CRYSTAL: case CRACKED_OBSIDIAN:
            case SULFUR_CRYSTAL: case WALL: case CLIFF: case ROOF: case HOUSE: case DOOR: case PILLAR: case BRAZIER:
            case DEAD_TREE: case CACTUS: case FOUNTAIN: case TOMBSTONE:
                return true;
            default:
                return false;
        }
    }
}
