package com.example.pokeworld.ui.input;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.ui.UIManager;
import com.example.pokeworld.ui.handlers.MissionUI;

public class MissionInput implements InputExtension {
    @Override
    public boolean handle(float x, float y, GameView gv) {
        MissionUI ui = (MissionUI) gv.ui.getHandler(UIManager.UIState.MISSIONS);
        if (ui != null) {
            if (ui.tabJourney.contains(x, y)) { ui.showDaily = false; ui.showEvents = false; return true; }
            if (ui.tabDaily.contains(x, y)) { ui.showDaily = true; ui.showEvents = false; return true; }
            if (ui.tabEvents.contains(x, y)) { ui.showEvents = true; ui.showDaily = false; return true; }
            if (ui.exitBtn.contains(x, y)) { gv.ui.state = UIManager.UIState.EXPLORING; return true; }
            if (!ui.claimAllBtn.isEmpty() && ui.claimAllBtn.contains(x, y)) {
                int n = gv.missions.claimAllQuests(gv) + gv.missions.claimAllDailies(gv);
                if (n == 0) gv.ui.showMessage("Nada para reivindicar agora.");
                return true;
            }
            for (int i = 0; i < ui.claimRects.size(); i++) {
                if (!ui.claimRects.get(i).contains(x, y)) continue;
                String k = ui.claimKeys.get(i);
                if (k.startsWith("q:")) {
                    for (com.example.pokeworld.managers.MissionManager.Quest q : com.example.pokeworld.managers.MissionManager.QUESTS)
                        if (q.id.equals(k.substring(2))) { gv.missions.claimQuest(q, gv); break; }
                } else if (k.startsWith("d:")) gv.missions.claimDaily(Integer.parseInt(k.substring(2)), gv);
                else if (k.equals("bonus")) gv.missions.claimDailyBonus(gv);
                return true;
            }
            return true;
        }
        gv.ui.state = UIManager.UIState.EXPLORING;
        return true;
    }
}
