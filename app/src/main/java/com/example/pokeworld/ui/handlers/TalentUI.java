package com.example.pokeworld.ui.handlers;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import com.example.pokeworld.core.GameView;
import com.example.pokeworld.entities.Player;
import java.util.Map;

public class TalentUI extends BaseUI {
    public RectF domadorTalentBtn = new RectF(300, 200, 900, 270);
    public RectF guerreiroTalentBtn = new RectF(300, 300, 900, 370);
    public RectF exploradorTalentBtn = new RectF(300, 400, 900, 470);

    @Override
    public void draw(Canvas canvas, GameView gv) {
        Player p = gv.player;
        drawOverlay(canvas, "ÁRVORE DE TALENTOS", Color.argb(245, 30, 0, 60));
        paint.setColor(Color.WHITE); paint.setTextSize(30);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("Pontos Disponíveis: " + p.getTalentPoints(), canvas.getWidth()/2f, 130, paint);
        Map<String, Integer> t = p.getTalents();
        drawButton(canvas, domadorTalentBtn, "DOMADOR (Nv." + t.get("Domador") + ") - +10% Captura");
        drawButton(canvas, guerreiroTalentBtn, "GUERREIRO (Nv." + t.get("Guerreiro") + ") - +20% Dano Crítico");
        drawButton(canvas, exploradorTalentBtn, "EXPLORADOR (Nv." + t.get("Explorador") + ") - +10% Velocidade");
    }
}
