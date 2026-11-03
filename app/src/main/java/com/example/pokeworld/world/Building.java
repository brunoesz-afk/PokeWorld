package com.example.pokeworld.world;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;

import com.example.pokeworld.entities.Player;
import com.example.pokeworld.managers.SpriteManager;

public class Building {
    private String name;
    private int level;
    private double positionX, positionY;
    private float width = 640, height = 640; // Aumentado para 10x10 tiles
    private RectF bounds;
    private boolean isConstructed = false; 
    private long constructionFinishTime = 0;
    private String customSpriteKey = null;
    private boolean isDefending = false;

    public Building(String name, double x, double y, int level) {
        this.name = name;
        this.level = level;
        this.positionX = x;
        this.positionY = y;
        
        if (name.toLowerCase().contains("montaria")) {
            this.width = 896; // 14 tiles
            this.height = 896;
        } else if (name.toLowerCase().contains("tocha")) {
            this.width = 64; this.height = 64; // Tamanho de 1 tile
        } else if (name.toLowerCase().contains("baú")) {
            this.width = 128; this.height = 128; // 2x2 tiles
        } else if (name.toLowerCase().contains("academia") || name.toLowerCase().contains("mural")) {
            this.width = 320; this.height = 320; // 5x5 tiles
        }

        // Hitbox de colisão precisa ser precisa
        float collisionSize = 256;
        if (name.toLowerCase().contains("montaria")) collisionSize = 384;
        else if (name.toLowerCase().contains("tocha")) collisionSize = 32;
        else if (name.toLowerCase().contains("baú")) collisionSize = 96;
        else if (name.toLowerCase().contains("academia") || name.toLowerCase().contains("mural")) collisionSize = 192;

        float offsetX = (width - collisionSize) / 2;
        float offsetY = (height - collisionSize) / 2;
        this.bounds = new RectF((float)x + offsetX, (float)y + offsetY, (float)x + width - offsetX, (float)y + height - offsetY);
    }

    public void setConstructed(boolean constructed) {
        this.isConstructed = constructed;
    }

    public void setConstructionFinishTime(long time) {
        this.constructionFinishTime = time;
    }

    public long getConstructionFinishTime() {
        return constructionFinishTime;
    }

    public boolean isConstructed() {
        return isConstructed;
    }

    public void setCustomSpriteKey(String key) {
        this.customSpriteKey = key;
    }

    public void draw(Canvas canvas, Player player) {
        String key = name.toLowerCase().trim();
        long now = System.currentTimeMillis();
        
        // Se ainda estiver em construção
        if (!isConstructed && constructionFinishTime > now) {
            key = "oficina_construcao"; // Sprite de obra
            drawTimer(canvas);
        } else if (!isConstructed && constructionFinishTime > 0 && now >= constructionFinishTime) {
            isConstructed = true;
        }

        if (!isConstructed) {
            key = "oficina_construcao"; 
        } else {
            if (key.contains("oficina")) key = "oficina_pokebola";
            else if (key.contains("daycare")) key = "construcao_daycare";
            else if (key.contains("centro") || key.contains("cura") || key.contains("hospital")) key = "construcao_hospitaljoy";
            else if (key.contains("laboratorio")) key = "laboratorio";
            else if (key.contains("montaria")) key = "fabrica_montaria";
            else if (key.contains("choque")) key = "torre_choque";
            else if (key.contains("tocha")) key = "torch_placed";
            else if (key.contains("baú")) key = "chest_building";
            else if (key.contains("ginásio") || key.contains("ginasio") || key.contains("gym") || key.contains("academia")) key = "ginasio_building";
            else if (key.contains("mural") || key.contains("bounty") || key.contains("recompensa")) key = "bounty_board";
        }
        
        if (customSpriteKey != null) key = customSpriteKey;

        android.graphics.Bitmap sprite = SpriteManager.getInstance().getBuildingSprite(key);
        if (sprite != null) {
            canvas.drawBitmap(sprite, null, new RectF((float)positionX, (float)positionY, (float)positionX + width, (float)positionY + height), null);
            
            // Especial: Desenha o Pikachu no topo da Torre de Choque se houver um na base e estiver atacando
            if (key.equals("torre_choque") && isDefending) {
                com.example.pokeworld.entities.CapturedCreature electric = null;
                for (com.example.pokeworld.entities.CapturedCreature c : player.getCreatureCollection()) {
                    if (c.name.equalsIgnoreCase("Pikachu") || c.name.equalsIgnoreCase("Raichu")) {
                        electric = c; break;
                    }
                }
                
                if (electric != null) {
                    Bitmap pikachu = SpriteManager.getInstance().getUnitSprite(electric.name, 0, 0, electric.isShiny);
                    if (pikachu != null) {
                        float pkR = 32;
                        canvas.drawBitmap(pikachu, null, new RectF((float)getCenterX() - pkR, (float)positionY - pkR, (float)getCenterX() + pkR, (float)positionY + pkR), null);
                    }
                }
            }
        } else {
            // Desenha um retângulo colorido se a sprite estiver faltando
            android.graphics.Paint p = new android.graphics.Paint();
            p.setColor(android.graphics.Color.argb(150, 100, 100, 100));
            canvas.drawRoundRect(new RectF((float)positionX, (float)positionY, (float)positionX + width, (float)positionY + height), 10, 10, p);
            p.setStyle(android.graphics.Paint.Style.STROKE);
            p.setColor(android.graphics.Color.WHITE);
            canvas.drawRoundRect(new RectF((float)positionX, (float)positionY, (float)positionX + width, (float)positionY + height), 10, 10, p);
            
            p.setStyle(android.graphics.Paint.Style.FILL);
            p.setTextAlign(android.graphics.Paint.Align.CENTER);
            p.setTextSize(20);
            canvas.drawText(name, (float)positionX + width/2, (float)positionY + height/2, p);
        }
    }

    private void drawTimer(Canvas canvas) {
        android.graphics.Paint p = new android.graphics.Paint();
        p.setColor(android.graphics.Color.WHITE);
        p.setTextSize(30);
        p.setTextAlign(android.graphics.Paint.Align.CENTER);
        p.setFakeBoldText(true);

        long remaining = (constructionFinishTime - System.currentTimeMillis()) / 1000;
        if (remaining < 0) remaining = 0;
        
        String timeStr = String.format("🏗️ %02d:%02d", remaining / 60, remaining % 60);
        canvas.drawText(timeStr, (float)getCenterX(), (float)positionY - 20, p);
    }

    public void update(com.example.pokeworld.core.MainActivity activity) {
        if (!isConstructed && constructionFinishTime > 0 && System.currentTimeMillis() >= constructionFinishTime) {
            isConstructed = true;
            activity.dbHelper.updateBuilding(name, level, 1, positionX, positionY, 0);
            activity.gameView.ui.showMessage("✅ " + name.toUpperCase() + " CONCLUÍDA!");
            activity.gameView.particles.addExplosion(getCenterX(), getCenterY(), android.graphics.Color.YELLOW, 50);
        }
    }

    public boolean isTouched(float x, float y) {
        return bounds.contains(x, y);
    }

    public boolean isNearby(Player player) {
        double centerX = positionX + width / 2;
        double centerY = positionY + height / 2;
        double dist = Math.sqrt(Math.pow(centerX - player.getPositionX(), 2) + Math.pow(centerY - player.getPositionY(), 2));
        return dist < 300;
    }

    public void upgrade() {
        level++;
    }

    public int getUpgradeCost() {
        return level * 200;
    }

    public String getName() { return name; }
    public int getLevel() { return level; }
    public double getPositionX() { return positionX; }
    public double getPositionY() { return positionY; }
    public double getCenterX() { return positionX + width / 2; }
    public double getCenterY() { return positionY + height / 2; }
    public void setDefending(boolean d) { this.isDefending = d; }
}
