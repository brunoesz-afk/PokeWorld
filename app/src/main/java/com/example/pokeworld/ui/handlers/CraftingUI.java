package com.example.pokeworld.ui.handlers;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import com.example.pokeworld.core.GameView;
import com.example.pokeworld.entities.Player;

public class CraftingUI extends BaseUI {
    public RectF ballItem = new RectF();
    public RectF potionItem = new RectF();
    public RectF crystalItem = new RectF();
    public RectF mountFactoryBtn = new RectF();
    public RectF axeUpgradeBtn = new RectF();
    public RectF pickaxeUpgradeBtn = new RectF();
    public RectF stoveBtn = new RectF();
    public RectF cookBtn = new RectF();
    public RectF thermalCapeBtn = new RectF();
    public RectF shockTowerBtn = new RectF();
    public RectF torchBtn = new RectF();
    public RectF chestBtn = new RectF();
    public RectF pavementBtn = new RectF();

    @Override
    public void draw(Canvas canvas, GameView gv) {
        Player p = gv.player;
        drawOverlay(canvas, "FÁBRICA DO REINO", Color.argb(245, 45, 40, 50));
        
        float w = canvas.getWidth();
        float mid = w / 2;

        // --- PAINEL DE RECURSOS (ESQUERDA) ---
        paint.setColor(Color.argb(100, 0, 0, 0));
        canvas.drawRoundRect(new RectF(50, 150, mid - 20, 550), 20, 20, paint);
        
        paint.setColor(Color.WHITE); paint.setTextSize(26); paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("📦 ESTOQUE DE MATERIAIS:", 80, 200, paint);
        
        paint.setTextSize(22);
        canvas.drawText("🪵 Madeira: " + p.getInventory().getOrDefault("Madeira", 0), 100, 260, paint);
        canvas.drawText("🪨 Pedra: " + p.getInventory().getOrDefault("Pedra", 0), 100, 310, paint);
        canvas.drawText("⛓️ Ferro: " + p.getInventory().getOrDefault("Ferro", 0), 100, 360, paint);
        canvas.drawText("🍎 Fruta: " + p.getInventory().getOrDefault("Fruta", 0), 100, 410, paint);
        canvas.drawText("💎 Gemas: " + p.getGems(), 100, 460, paint);

        // --- PAINEL DE CRAFT (DIREITA) ---
        paint.setColor(Color.argb(100, 0, 0, 0));
        canvas.drawRoundRect(new RectF(mid + 20, 150, w - 50, 550), 20, 20, paint);

        // Itens para Fabricar
        ballItem.set(mid + 50, 180, mid + 350, 260);
        drawButton(canvas, ballItem, "🔴 Pokébola x5");

        potionItem.set(mid + 50, 280, mid + 350, 360);
        drawButton(canvas, potionItem, "🧪 Poção x3");

        crystalItem.set(mid + 50, 380, mid + 350, 460);
        drawButton(canvas, crystalItem, "✨ Cristal");

        // Melhorias de Ferramentas
        int axeLv = p.getInventory().getOrDefault("Machado_Lv", 1);
        int pickLv = p.getInventory().getOrDefault("Picareta_Lv", 1);
        
        axeUpgradeBtn.set(mid + 400, 280, w - 80, 360);
        drawButton(canvas, axeUpgradeBtn, "🪓 Machado Lv." + axeLv + "\n(20 Madeiras)");

        pickaxeUpgradeBtn.set(mid + 400, 380, w - 80, 460);
        drawButton(canvas, pickaxeUpgradeBtn, "⛏️ Picareta Lv." + pickLv + "\n(20 Ferros)");

        // Culinária
        if (p.getInventory().containsKey("Fogão")) {
            cookBtn.set(mid + 50, 480, mid + 350, 560);
            drawButton(canvas, cookBtn, "🍲 Cozinhar Doce\n(5 Frutas)");
        } else {
            stoveBtn.set(mid + 50, 480, mid + 350, 560);
            drawButton(canvas, stoveBtn, "🍳 Construir Fogão\n(15 Ferros)");
        }

        // Novos itens: Capa e Torre
        thermalCapeBtn.set(mid - 350, 580, mid - 50, 660);
        drawButton(canvas, thermalCapeBtn, "🧣 Capa Térmica\n(10 Pelo)");

        // (Torre Choque, Tocha e Baú agora são construídas pelo menu 🔨 da base)

        // Outros itens
        pavementBtn.set(mid + 150, 680, mid + 350, 760);
        drawButton(canvas, pavementBtn, "🧱 Piso Pedra (2 Pedras)");

        // Construções (Se estiver na HOME)
        if (gv.world.tileMap.getMapName().equals("HOME")) {
            mountFactoryBtn.set(mid + 400, 180, w - 80, 260);
            drawButton(canvas, mountFactoryBtn, "🔨 Construir na base");
        }
    }
}
