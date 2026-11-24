package com.example.pokeworld.ui.input;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.managers.WalletManager;
import com.example.pokeworld.ui.UIManager;
import com.example.pokeworld.ui.handlers.DepositUI;

public class DepositInput implements InputExtension {
    @Override
    public boolean handle(float x, float y, GameView gv) {
        DepositUI ui = (DepositUI) gv.ui.getHandler(UIManager.UIState.DEPOSIT);
        if (ui == null) return false;
        if (ui.exitBtn.contains(x, y)) { gv.ui.state = UIManager.UIState.POINTS_SHOP; return true; }
        if (ui.copyBtn.contains(x, y) && WalletManager.hasPix()) {
            ClipboardManager cm = (ClipboardManager) gv.activity.getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("pix", WalletManager.pixCode));
            gv.ui.showMessage("📋 Código Pix copiado! Cole no app do seu banco.");
            return true;
        }
        if (ui.newBtn.contains(x, y)) { WalletManager.clearPix(); WalletManager.creditedPoints = 0; return true; }
        if (WalletManager.hasPix() || WalletManager.busy) return true;
        for (int i = 0; i < ui.packs.length; i++) if (ui.packs[i].contains(x, y)) { ui.selected = i; WalletManager.creditedPoints = 0; return true; }
        if (ui.createBtn.contains(x, y)) { WalletManager.creditedPoints = 0; WalletManager.createPix(WalletManager.PACKS[ui.selected]); return true; }
        return true;
    }
}
