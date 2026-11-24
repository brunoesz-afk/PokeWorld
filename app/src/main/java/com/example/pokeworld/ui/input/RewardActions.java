package com.example.pokeworld.ui.input;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.managers.RewardManager;
import com.example.pokeworld.ui.UIManager;
import com.example.pokeworld.ui.handlers.ExploringUI;
import com.example.pokeworld.ui.handlers.MissionUI;
import com.example.pokeworld.ui.handlers.RewardBar;

/** O que cada ícone da barra de recompensas (e cada cartão da Central) faz ao ser tocado. */
public final class RewardActions {
    private RewardActions() {}

    private static RewardManager.Store store(GameView gv) { return RewardManager.of(gv.activity.dbHelper); }

    public static void claimGift(GameView gv) {
        RewardManager.Prize p = RewardManager.claimGift(gv, store(gv));
        if (p == null) { gv.ui.showMessage("🎁 O presente de hoje já foi pego. Volte amanhã!"); return; }
        gv.ui.showMessage("🎁 Presente diário: " + p.text());
        gv.activity.saveCurrentGame();
    }

    public static void claimChest(GameView gv) {
        RewardManager.Prize p = RewardManager.claimChest(gv, store(gv));
        if (p == null) { gv.ui.showMessage("🎉 O baú de hoje já foi aberto."); return; }
        gv.ui.showMessage(p.icon + " Baú do evento: " + p.text());
        gv.activity.saveCurrentGame();
    }

    public static void claimRecharge(GameView gv) {
        RewardManager.Store s = store(gv);
        String r = RewardManager.claimRecharge(gv, s);
        if (r == null) gv.ui.showMessage("🔋 Próxima recarga em " + RewardManager.countdown(RewardManager.rechargeSecondsLeft(s)));
        else { gv.ui.showMessage(r); gv.activity.saveCurrentGame(); }
    }

    public static void claimAll(GameView gv) {
        int n = RewardManager.claimAll(gv, store(gv));
        if (n == 0) gv.ui.showMessage("Nada para reivindicar agora.");
        else gv.ui.showMessage("✨ " + n + (n == 1 ? " recompensa reivindicada!" : " recompensas reivindicadas!"));
    }

    /** Toque num ícone da barra do topo. */
    public static void bar(GameView gv, int which) {
        ExploringUI eui = (ExploringUI) gv.ui.getHandler(UIManager.UIState.EXPLORING);
        if (which == RewardBar.TOGGLE) { if (eui != null) eui.rewardBar.toggle(gv); return; }
        RewardManager.Store s = store(gv);
        switch (which) {
            case RewardBar.GIFT:
                if (RewardManager.giftReady(s)) claimGift(gv); else gv.ui.state = UIManager.UIState.REWARDS;
                break;
            case RewardBar.EVENT:
                if (RewardManager.chestReady(s)) claimChest(gv); else openMissions(gv, false, true);
                break;
            case RewardBar.DAILY: openMissions(gv, true, false); break;
            case RewardBar.JOURNEY: openMissions(gv, false, false); break;
            case RewardBar.ACH: gv.ui.state = UIManager.UIState.ACHIEVEMENTS; break;
            case RewardBar.RECHARGE: claimRecharge(gv); break;
            case RewardBar.PASS: gv.ui.state = UIManager.UIState.PASS; break;
            case RewardBar.SHOP: gv.ui.state = UIManager.UIState.POINTS_SHOP; break;
            case RewardBar.ARENA: {
                com.example.pokeworld.ui.handlers.ArenaUI au = (com.example.pokeworld.ui.handlers.ArenaUI) gv.ui.getHandler(UIManager.UIState.ARENA);
                if (au != null) au.invalidate();
                gv.ui.state = UIManager.UIState.ARENA;
                break;
            }
            case RewardBar.ALL: claimAll(gv); break;
            default: break;
        }
        ExploringUI ui = (ExploringUI) gv.ui.getHandler(UIManager.UIState.EXPLORING);
        if (ui != null) ui.rewardBar.invalidate();
    }

    private static void openMissions(GameView gv, boolean daily, boolean events) {
        MissionUI m = (MissionUI) gv.ui.getHandler(UIManager.UIState.MISSIONS);
        if (m != null) { m.showDaily = daily; m.showEvents = events; }
        gv.ui.state = UIManager.UIState.MISSIONS;
    }
}
