package com.example.pokeworld.ui.handlers;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import com.example.pokeworld.core.GameView;

public class SettingsUI extends BaseUI {
    public RectF toggleVibrationBtn = new RectF(100, 150, 450, 230);
    public RectF toggleJoystickBtn = new RectF(100, 250, 450, 330);
    public RectF toggleSoundBtn = new RectF(100, 450, 450, 530);
    public RectF toggleNightBtn = new RectF(100, 550, 450, 630);
    public RectF saveExitBtn = new RectF(100, 350, 450, 430);
    
    public RectF skinRedBtn = new RectF(600, 200, 850, 280);
    public RectF skinBlueBtn = new RectF(600, 300, 850, 380);
    public RectF skinGreenBtn = new RectF(600, 400, 850, 480);

    @Override
    public void draw(Canvas canvas, GameView gv) {
        drawOverlay(canvas, "SISTEMA E CUSTOMIZAÇÃO", Color.argb(245, 20, 20, 30));
        
        String vibStatus = gv.vibration.isEnabled() ? "ON" : "OFF";
        drawButton(canvas, toggleVibrationBtn, "VIBRAÇÃO: " + vibStatus);
        drawButton(canvas, toggleJoystickBtn, "JOYSTICK: FIXO");
        drawButton(canvas, toggleSoundBtn, "SONS: " + (com.example.pokeworld.managers.SoundManager.getInstance(gv.activity).isEnabled() ? "ON" : "OFF"));
        drawButton(canvas, toggleNightBtn, "DIA/NOITE: " + (gv.time.isCycleEnabled() ? "ON" : "OFF"));
        drawButton(canvas, saveExitBtn, "SAIR E SALVAR");
        
        // Customização
        paint.setColor(Color.WHITE); paint.setTextSize(24); paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("COR DA ROUPA (500 🪙):", 725, 170, paint);
        drawButton(canvas, skinRedBtn, "🔴 VERMELHO");
        drawButton(canvas, skinBlueBtn, "🔵 AZUL");
        drawButton(canvas, skinGreenBtn, "🟢 VERDE");
        
        paint.setColor(Color.LTGRAY); paint.setTextSize(20);
        canvas.drawText("Versão: 1.6.0 - Base & Survival", canvas.getWidth()/2f, canvas.getHeight() - 80, paint);
    }
}
