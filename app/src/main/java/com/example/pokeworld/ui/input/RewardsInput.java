package com.example.pokeworld.ui.input;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.managers.RewardManager;
import com.example.pokeworld.ui.UIManager;
import com.example.pokeworld.ui.handlers.RewardsUI;

public class RewardsInput implements InputExtension {
    @Override
    public boolean handle(float x, float y, GameView gv) {
        RewardsUI ui = (RewardsUI) gv.ui.getHandler(UIManager.UIState.REWARDS);
        if (ui == null) return false;
        if (ui.exitBtn.contains(x, y)) { gv.ui.state = UIManager.UIState.EXPLORING; return true; }
        RewardManager.Store s = RewardManager.of(gv.activity.dbHelper);
        if (ui.giftBtn.contains(x, y) && RewardManager.giftReady(s)) RewardActions.claimGift(gv);
        else if (ui.chestBtn.contains(x, y) && RewardManager.chestReady(s)) RewardActions.claimChest(gv);
        else if (ui.rechargeBtn.contains(x, y)) RewardActions.claimRecharge(gv);
        else if (ui.allBtn.contains(x, y)) RewardActions.claimAll(gv);
        return true;
    }
}
