package com.example.pokeworld.ui.input;

import android.graphics.Color;
import com.example.pokeworld.core.GameView;
import com.example.pokeworld.ui.UIManager;
import com.example.pokeworld.ui.handlers.SettingsUI;

public class SettingsInput implements InputExtension {
    @Override
    public boolean handle(float x, float y, GameView gv) {
        SettingsUI ui = (SettingsUI) gv.ui.getHandler(UIManager.UIState.SETTINGS);
        if (ui.toggleVibrationBtn.contains(x, y)) {
            gv.vibration.setEnabled(!gv.vibration.isEnabled());
            gv.activity.dbHelper.setKv("set_vib", gv.vibration.isEnabled() ? 1 : 0);
            gv.ui.showMessage("VIBRAÇÃO: " + (gv.vibration.isEnabled() ? "ON" : "OFF"));
            return true;
        } else if (ui.toggleSoundBtn.contains(x, y)) {
            com.example.pokeworld.managers.SoundManager sm = com.example.pokeworld.managers.SoundManager.getInstance(gv.activity);
            sm.setEnabled(!sm.isEnabled());
            gv.activity.dbHelper.setKv("set_sound", sm.isEnabled() ? 1 : 0);
            gv.ui.showMessage("SONS: " + (sm.isEnabled() ? "ON" : "OFF"));
            return true;
        } else if (ui.toggleNightBtn.contains(x, y)) {
            gv.time.setCycleEnabled(!gv.time.isCycleEnabled());
            gv.activity.dbHelper.setKv("set_night", gv.time.isCycleEnabled() ? 1 : 0);
            gv.ui.showMessage("DIA/NOITE: " + (gv.time.isCycleEnabled() ? "ON (escurece e inimigos mais fortes à noite)" : "OFF"));
            return true;
        } else if (ui.saveExitBtn.contains(x, y)) {
            gv.activity.saveCurrentGame();
            gv.activity.finish(); 
            return true;
        }
        
        // Troca de Skin
        if (ui.skinRedBtn.contains(x, y)) changeSkin(gv, Color.RED);
        else if (ui.skinBlueBtn.contains(x, y)) changeSkin(gv, Color.BLUE);
        else if (ui.skinGreenBtn.contains(x, y)) changeSkin(gv, Color.GREEN);
        else gv.ui.state = UIManager.UIState.EXPLORING;

        return true;
    }

    private void changeSkin(GameView gv, int color) {
        if (gv.player.getGold() >= 500) {
            gv.player.addGold(-500);
            gv.player.setSkinColor(color);
            gv.ui.showMessage("ROUPA ALTERADA!");
            gv.activity.saveCurrentGame();
        } else gv.ui.showMessage("Ouro insuficiente (500)!");
    }
}
