package com.example.pokeworld.world;

/** Bônus passivos das decorações da base (Fonte e Estátua). */
public final class BaseBonus {
    private BaseBonus() {}

    /** Regeneração extra de todos os Pokémon (+25% por nível da Fonte). */
    public static float regen(BaseManager b) {
        if (b == null) return 0;
        float v = 0;
        for (BasePiece p : b.layout.getPieces()) if (p.kind == BaseKind.FONTE && p.isUsable()) v += 0.25f * p.level;
        return v;
    }

    /** XP extra do companheiro (+4% por nível de cada Estátua). */
    public static float xp(BaseManager b) {
        if (b == null) return 0;
        float v = 0;
        for (BasePiece p : b.layout.getPieces()) if (p.kind == BaseKind.ESTATUA && p.isUsable()) v += 0.04f * p.level;
        return v;
    }
}
