package com.example.pokeworld.entities;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

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
        
        // Pequena animação idle para os NPCs
        int frameIndex = (int)((System.currentTimeMillis() / 500) % 3);
        
        Bitmap frame = SpriteManager.getInstance().getUnitSprite(spriteName, 0, frameIndex, false);
        if (frame != null) {
            int r = (int)radius;
            canvas.drawBitmap(frame, null, new Rect((int)positionX - r, (int)positionY - r, (int)positionX + r, (int)positionY + r), null);
        }
        
        // Desenha o nome acima do NPC
        canvas.drawText(name, (float)positionX, (float)positionY - (float)radius - 10, paint);

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
