package com.example.pokeworld.managers;

import com.example.pokeworld.core.GameView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class HomeManager {
    private GameView gv;
    private List<String> trophies = new ArrayList<>();

    public HomeManager(GameView gv) {
        this.gv = gv;
    }

    public void updateTrophies(Set<String> discovered) {
        trophies.clear();
        if (discovered.contains("KRAKEN")) trophies.add("Tentáculo de Ouro");
        if (discovered.contains("RAIQUAZA")) trophies.add("Estátua Celestial");
        if (discovered.contains("FÊNIX DE FOGO")) trophies.add("Chama Eterna");
        if (discovered.contains("ENT ANCIÃO")) trophies.add("Semente de Carvalho");
        if (discovered.contains("GOLEM REI")) trophies.add("Mão de Pedra");
    }

    public List<String> getTrophies() {
        return trophies;
    }

    public void rest() {
        com.example.pokeworld.managers.PartyManager.healAll(gv);
        gv.ui.showMessage("💤 Você descansou. HP e Fúria restaurados!");
        gv.vibration.vibrate(100);
    }
}
