package com.example.pokeworld.entities;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import com.example.pokeworld.managers.SpriteManager;

public class Loot {
    public enum Type { GOLD, ITEM, CHEST, DEFEATED }
    
    private double positionX, positionY;
    private double radius = 15;
    private String name;
    private boolean collected = false;
    private int amount = -1; // -1 = padrão do tipo (ouro 100, item 1)
    private Paint paint = new Paint();
    private Type type;
    private String ownerSpecies = ""; // Espécie do Pokémon derrotado
    private boolean isShiny = false;
    private boolean visible = true;
    private long spawnTime;
    private static final long DEFEATED_LIFETIME = 60000; // 60 segundos

    public Loot(String name, double x, double y) {
        this(name, x, y, Type.GOLD);
    }

    public Loot(String name, double x, double y, Type type) {
        this.name = name;
        this.positionX = x;
        this.positionY = y;
        this.type = type;
        this.spawnTime = System.currentTimeMillis();
        
        switch (type) {
            case CHEST: paint.setColor(Color.rgb(139, 69, 19)); radius = 25; break;
            case DEFEATED: radius = 65; break; // Reduzido ligeiramente para um tamanho mais moderado e proporcional
            case ITEM: paint.setColor(Color.CYAN); break;
            case GOLD: default: paint.setColor(Color.YELLOW); break;
        }
    }

    public void setOwnerSpecies(String species) {
        this.ownerSpecies = species.toLowerCase();
    }

    public Loot withAmount(int n) { this.amount = n; return this; }
    public int getAmount() { return amount >= 0 ? amount : (type == Type.GOLD ? 100 : 1); }

    public void setShiny(boolean shiny) { this.isShiny = shiny; }
    public boolean isShiny() { return isShiny; }

    public boolean isExpired() {
        return type == Type.DEFEATED && (System.currentTimeMillis() - spawnTime > DEFEATED_LIFETIME);
    }

    public void setRadius(double r) { this.radius = r; }
    public double getRadius() { return radius; }

    public void setVisible(boolean v) { this.visible = v; }
    public boolean isVisible() { return visible; }

    public void draw(Canvas canvas) {
        if (!visible) return;
        if (type == Type.DEFEATED) {
            // Desenha o corpo do Pokémon derrotado
            String suffix = isShiny ? "_shiny_death" : "_death";
            String deathSpriteName = ownerSpecies + suffix;
            Bitmap deathSprite = SpriteManager.getInstance().getMiscSprite(deathSpriteName);
            
            // Fallback para o padrão se não encontrar a espécie específica
            if (deathSprite == null) {
                deathSprite = SpriteManager.getInstance().getMiscSprite(isShiny ? "charmander_shiny_death" : "charmander_death");
            }
            
            if (deathSprite != null) {
                int r = (int)radius;
                canvas.drawBitmap(deathSprite, null, new Rect((int)positionX - r, (int)positionY - r, (int)positionX + r, (int)positionY + r), null);
            }
            
            // Brilho Shiny se for o caso
            if (isShiny && System.currentTimeMillis() % 500 < 250) {
                paint.setColor(Color.YELLOW);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(3);
                canvas.drawCircle((float)positionX, (float)positionY, (float)radius + 5, paint);
            }

            // Texto indicativo
            paint.setColor(isShiny ? Color.YELLOW : Color.WHITE);
            paint.setStyle(Paint.Style.FILL);
            paint.setTextSize(18);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(isShiny ? "Shiny Derrotado" : "Derrotado", (float)positionX, (float)positionY + (float)radius + 15, paint);
            return;
        }

        float bounce = (float) Math.sin(System.currentTimeMillis() / 200.0) * 10f;
        
        paint.setStyle(Paint.Style.FILL);
        // Sombra do item
        paint.setColor(Color.argb(50, 0, 0, 0));
        canvas.drawOval((float)positionX - 15, (float)positionY + 10, (float)positionX + 15, (float)positionY + 20, paint);

        String icon = "💰";
        if (type == Type.ITEM) icon = name.equalsIgnoreCase("aincrad_portal") ? "🎁" : ItemCatalog.get(name).icon;
        else if (type == Type.CHEST) icon = "📦";

        paint.setTextSize(35);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(icon, (float)positionX, (float)positionY + bounce, paint);
    }

    public void update(Player player) {
        if (type == Type.DEFEATED) return;
        double dx = player.getPositionX() - positionX;
        double dy = player.getPositionY() - positionY;
        double distance = Math.sqrt(dx * dx + dy * dy);
        // Ímã de coleta: puxa itens em um raio de 250 pixels em direção ao jogador
        if (distance < 250 && distance > 10) {
            positionX += (dx / distance) * 6.0;
            positionY += (dy / distance) * 6.0;
        }
    }

    public boolean isCollectedBy(Player player) {
        double distance = Math.sqrt(Math.pow(positionX - player.getPositionX(), 2) + 
                                   Math.pow(positionY - player.getPositionY(), 2));
        if (distance < (type == Type.CHEST ? 100 : 120)) {
            collected = true;
            return true;
        }
        return false;
    }

    public String getName() { return name; }
    public Type getType() { return type; }
    public double getPositionX() { return positionX; }
    public double getPositionY() { return positionY; }
    public void setPosition(double x, double y) { this.positionX = x; this.positionY = y; }
}
