package com.example.pokeworld.ui.handlers;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import com.example.pokeworld.core.GameView;
import com.example.pokeworld.entities.Player;
import com.example.pokeworld.entities.TrainerSkill;
import java.util.ArrayList;
import java.util.List;

public class TrainerSkillsUI implements UIHandler {
    private Paint paint = new Paint();
    public List<RectF> skillButtons = new ArrayList<>();
    public RectF backBtn = new RectF(50, 50, 200, 120);

    @Override
    public void draw(Canvas canvas, GameView gv) {
        canvas.drawColor(Color.argb(200, 0, 0, 0));
        Player p = gv.player;
        List<TrainerSkill> skills = p.getTrainerSkills();

        paint.setColor(Color.WHITE);
        paint.setTextSize(50);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("MAGIAS DO TREINADOR", canvas.getWidth() / 2f, 80, paint);

        skillButtons.clear();
        float startY = 150;
        float btnH = 120;
        float btnW = 600;
        float centerX = canvas.getWidth() / 2f;

        for (int i = 0; i < skills.size(); i++) {
            TrainerSkill s = skills.get(i);
            RectF r = new RectF(centerX - btnW/2, startY + (i * (btnH + 20)), centerX + btnW/2, startY + (i * (btnH + 20)) + btnH);
            skillButtons.add(r);

            paint.setColor(Color.DKGRAY);
            canvas.drawRect(r, paint);

            // Barra de Cooldown
            float progress = s.getCooldownProgress();
            if (progress < 1.0f) {
                paint.setColor(Color.argb(100, 100, 100, 100));
                canvas.drawRect(r.left, r.top, r.left + (r.width() * progress), r.bottom, paint);
            }

            paint.setColor(s.getColor());
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(3);
            canvas.drawRect(r, paint);
            paint.setStyle(Paint.Style.FILL);

            paint.setColor(Color.WHITE);
            paint.setTextSize(30);
            canvas.drawText(s.getName() + " [Custo: " + s.getStaminaCost() + " ST]", r.centerX(), r.centerY() - 5, paint);
            
            paint.setTextSize(20);
            paint.setColor(Color.LTGRAY);
            String desc = getSkillDesc(s.getEffectType());
            canvas.drawText(desc, r.centerX(), r.centerY() + 30, paint);
        }

        paint.setColor(Color.RED);
        canvas.drawRect(backBtn, paint);
        paint.setColor(Color.WHITE);
        paint.setTextSize(25);
        canvas.drawText("VOLTAR", backBtn.centerX(), backBtn.centerY() + 10, paint);
    }

    private String getSkillDesc(String type) {
        switch(type) {
            case "HEAL": return "Recupera 30% do HP do seu Pokémon.";
            case "BUFF": return "+50% de Dano por 15 segundos.";
            case "SHIELD": return "Invulnerabilidade por 5 segundos.";
            default: return "";
        }
    }
}
