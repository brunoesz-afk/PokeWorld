package com.example.pokeworld.entities;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

import com.example.pokeworld.managers.SpriteManager;

public class NPC {
    public enum Type { HEALER, SHOPKEEPER, GUARD, BLACKSMITH }
    public enum QuestIcon { NONE, MISSION_AVAILABLE, MISSION_READY }
    
    private String name;
    private String dialogue;
    private double positionX, positionY, radius;
    private Paint paint;
    private Type type;
    private QuestIcon questIcon = QuestIcon.NONE;
    private String action = "";      // o que o NPC abre ao ser tocado (vazio = diálogo normal)
    private String sprite = null;    // sprite fixo (sobrepõe o escolhido pelo nome/tipo)
    private String role = "";        // etiqueta embaixo do NPC, ex.: "🎉 EVENTOS"
    private int roleColor = Color.WHITE;

    public NPC(String name, String dialogue, double x, double y, Type type) {
        this.name = name;
        this.dialogue = dialogue;
        this.positionX = x;
        this.positionY = y;
        this.radius = 32;
        this.type = type;
        this.paint = new Paint();
        this.paint.setTextAlign(Paint.Align.CENTER);
        this.paint.setTextSize(30);
        this.paint.setColor(Color.WHITE);
    }

    public void draw(Canvas canvas) {
        String spriteName = "charmander"; // Default
        if (type == Type.HEALER) spriteName = "enfermeira";
        else if (type == Type.BLACKSMITH) spriteName = "bulbasaur";
        else if (type == Type.SHOPKEEPER) spriteName = "personagem"; 
        
        // Mapeamento para os Professores do Reino
        if (name.contains("Engenharia")) spriteName = "treinador_pokebola";
        else if (name.contains("Criador")) spriteName = "treinador_daycare";
        else if (name.contains("Invocador")) spriteName = "treinador_laboratorio";
        else if (name.contains("Montaria")) spriteName = "treinador_montaria";
        else if (name.contains("Estoque")) spriteName = "prof_bau";
        else if (name.contains("Batalha")) spriteName = "prof_ginasio";
        else if (name.contains("Caçador")) spriteName = "prof_mural";
        else if (name.contains("Treino")) spriteName = "treinador_academia";
        
        if (sprite != null) spriteName = sprite;

        // Pequena animação idle para os NPCs
        int frameIndex = (int)((System.currentTimeMillis() / 500) % 3);
        
        Bitmap frame = SpriteManager.getInstance().getUnitSprite(spriteName, 0, frameIndex, false);
        if (frame != null) {
            int r = (int)radius;
            canvas.drawBitmap(frame, null, new Rect((int)positionX - r, (int)positionY - r, (int)positionX + r, (int)positionY + r), null);
        }
        
        // Desenha o nome acima do NPC
        canvas.drawText(name, (float)positionX, (float)positionY - (float)radius - 10, paint);

        // Etiqueta de função embaixo dos pés
        if (!role.isEmpty()) {
            paint.setTextSize(20);
            float tw = paint.measureText(role);
            float ry = (float) positionY + (float) radius + 8;
            paint.setColor(Color.argb(170, 0, 0, 0));
            canvas.drawRoundRect(new RectF((float) positionX - tw / 2 - 12, ry, (float) positionX + tw / 2 + 12, ry + 30), 14, 14, paint);
            paint.setColor(roleColor);
            canvas.drawText(role, (float) positionX, ry + 22, paint);
            paint.setColor(Color.WHITE);
            paint.setTextSize(30);
        }

        // Desenha Indicador de Missão
        if (questIcon != QuestIcon.NONE) {
            drawQuestIcon(canvas);
        }
    }

    private void drawQuestIcon(Canvas canvas) {
        float bounce = (float) Math.sin(System.currentTimeMillis() / 150.0) * 8f;
        paint.setTextSize(40);
        String icon = questIcon == QuestIcon.MISSION_AVAILABLE ? "!" : "?";
        paint.setColor(questIcon == QuestIcon.MISSION_AVAILABLE ? Color.YELLOW : Color.GREEN);
        canvas.drawText(icon, (float)positionX, (float)positionY - (float)radius - 50 + bounce, paint);
        paint.setColor(Color.WHITE); // Reset
        paint.setTextSize(30);
    }

    public void setQuestIcon(QuestIcon icon) { this.questIcon = icon; }
    public void setAction(String a) { this.action = a == null ? "" : a; }
    public String getAction() { return action; }
    public void setSprite(String s) { this.sprite = s; }
    public void setRole(String r, int color) { this.role = r == null ? "" : r; this.roleColor = color; }

    public boolean isNearby(Player player) {
        double distance = Math.sqrt(Math.pow(positionX - player.getPositionX(), 2) + 
                                   Math.pow(positionY - player.getPositionY(), 2));
        return distance < 100;
    }

    public String getDialogue() { return dialogue; }
    public String getName() { return name; }
    public Type getType() { return type; }
    public double getPositionX() { return positionX; }
    public double getPositionY() { return positionY; }
}
