package com.example.pokeworld.entities;

public enum ElementalType {
    NORMAL, FIRE, WATER, PLANT, ELECTRIC, ICE, ROCK, POISON, PSYCHIC, DRAGON;

    public static double getMultiplier(ElementalType attack, ElementalType def1, ElementalType def2) {
        double m1 = getMultiplierSingle(attack, def1);
        double m2 = def2 == null ? 1.0 : getMultiplierSingle(attack, def2);
        return m1 * m2;
    }

    private static double getMultiplierSingle(ElementalType attacker, ElementalType defender) {
        if (attacker == FIRE) {
            if (defender == PLANT || defender == ICE) return 2.0;
            if (defender == WATER || defender == ROCK || defender == DRAGON) return 0.5;
        }
        if (attacker == WATER) {
            if (defender == FIRE || defender == ROCK) return 2.0;
            if (defender == PLANT || defender == ELECTRIC || defender == DRAGON) return 0.5;
        }
        if (attacker == PLANT) {
            if (defender == WATER || defender == ROCK) return 2.0;
            if (defender == FIRE || defender == POISON || defender == DRAGON) return 0.5;
        }
        if (attacker == ELECTRIC) {
            if (defender == WATER) return 2.0;
            if (defender == ROCK || defender == PLANT || defender == DRAGON) return 0.5;
        }
        if (attacker == ICE) {
            if (defender == PLANT || defender == DRAGON) return 2.0;
            if (defender == FIRE || defender == WATER) return 0.5;
        }
        if (attacker == ROCK) {
            if (defender == FIRE || defender == ELECTRIC || defender == ICE) return 2.0;
            if (defender == PLANT) return 0.5;
        }
        if (attacker == PSYCHIC) {
            if (defender == POISON) return 2.0;
            if (defender == PSYCHIC) return 0.5;
        }
        if (attacker == DRAGON) {
            if (defender == DRAGON) return 2.0;
        }
        return 1.0;
    }
}
