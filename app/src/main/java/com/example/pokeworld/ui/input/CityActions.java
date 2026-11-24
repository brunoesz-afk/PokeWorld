package com.example.pokeworld.ui.input;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.ui.UIManager;
import com.example.pokeworld.ui.handlers.MissionUI;
import com.example.pokeworld.world.CityDistricts;

/** O que cada NPC da cidade abre ao ser tocado (ver CityDistricts.SPOTS). */
public final class CityActions {
    private CityActions() {}

    public static void run(GameView gv, String action) {
        UIManager ui = gv.ui;
        switch (action) {
            case "INTRO_RANGER": gv.tutorial.onRangerTalk(); break;
            case "EVENTS": openMissions(gv, false, true); CityDistricts.markEventSeen(gv); break;
            case "JOURNEY": openMissions(gv, false, false); break;
            case "DAILY": openMissions(gv, true, false); break;
            case "BOUNTIES": ui.state = UIManager.UIState.BOUNTIES; break;
            case "TRADE": ui.state = UIManager.UIState.TRADE; break;
            case "FORGE": ui.state = UIManager.UIState.FORGE; break;
            case "CLAN": ui.state = UIManager.UIState.CLAN; break;
            case "GYM": ui.state = UIManager.UIState.GYM; break;
            case "TOWER": ui.state = UIManager.UIState.TOWER_SELECT; break;
            case "COLISEUM": ui.state = UIManager.UIState.COLISEUM; break;
            case "RIVALS": ui.state = UIManager.UIState.EXPEDITION; break;
            case "SHOP": ui.state = UIManager.UIState.SHOP; break;
            default: break;
        }
    }

    private static void openMissions(GameView gv, boolean daily, boolean events) {
        MissionUI m = (MissionUI) gv.ui.getHandler(UIManager.UIState.MISSIONS);
        if (m != null) { m.showDaily = daily; m.showEvents = events; }
        gv.ui.state = UIManager.UIState.MISSIONS;
    }
}
