package com.example.pokeworld.entities;

/** Estados alterados dos golpes (e de alguns inimigos): dano contínuo, atordoamento ou lentidão. */
public enum Status {
    BURN("burn", "🔥", "QUEIMADO", 6000, 0xFFFF6A20),
    POISON("poison", "☠️", "ENVENENADO", 8000, 0xFFB050E0),
    SLEEP("sleep", "💤", "DORMINDO", 3500, 0xFFFF80C0),
    PARALYZE("paralyze", "⚡", "PARALISADO", 5000, 0xFFFFE030),
    FREEZE("freeze", "❄️", "CONGELADO", 2500, 0xFF80E0FF);

    public final String key, icon, label;
    public final long durationMs;
    public final int color;

    Status(String key, String icon, String label, long durationMs, int color) {
        this.key = key; this.icon = icon; this.label = label; this.durationMs = durationMs; this.color = color;
    }

    /** Pokémon do tipo certo ignora o estado (fogo não queima, gelo não congela...). */
    public boolean immune(ElementalType t) {
        if (t == null) return false;
        switch (this) {
            case BURN: return t == ElementalType.FIRE;
            case POISON: return t == ElementalType.POISON;
            case PARALYZE: return t == ElementalType.ELECTRIC;
            case FREEZE: return t == ElementalType.ICE;
            default: return false;
        }
    }

    /** Estados que impedem agir. */
    public boolean stuns() { return this == SLEEP || this == FREEZE; }
}
