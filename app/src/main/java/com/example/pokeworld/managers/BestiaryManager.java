package com.example.pokeworld.managers;

import com.example.pokeworld.entities.*;
import com.example.pokeworld.core.*;

import java.util.HashSet;
import java.util.Set;

public class BestiaryManager {
    private Set<String> seenCreatures = new HashSet<>();
    private Set<String> capturedCreatures = new HashSet<>();
    private DatabaseHelper dbHelper;

    public BestiaryManager(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
        this.seenCreatures.addAll(dbHelper.getBestiary());
        // Inicialmente, sincroniza capturados da coleção
        for (CapturedCreature c : dbHelper.getCollection()) {
            capturedCreatures.add(c.name);
        }
    }

    public void markAsSeen(String species) {
        if (!seenCreatures.contains(species)) {
            seenCreatures.add(species);
            dbHelper.addToBestiary(species);
        }
    }

    public void markAsCaptured(String species) {
        markAsSeen(species); // Se capturou, certamente viu
        capturedCreatures.add(species);
    }

    public Set<String> getSeenCreatures() {
        return seenCreatures;
    }

    public Set<String> getCapturedCreatures() {
        return capturedCreatures;
    }

    public boolean isSeen(String species) {
        return seenCreatures.contains(species);
    }

    public boolean isCaptured(String species) {
        return capturedCreatures.contains(species);
    }
}
