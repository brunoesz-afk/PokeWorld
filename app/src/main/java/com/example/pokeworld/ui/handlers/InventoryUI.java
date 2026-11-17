package com.example.pokeworld.ui.handlers;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.entities.Player;

import java.util.Map;

public class InventoryUI extends BaseUI {
    public RectF exitBtn = new RectF();

    @Override
    public void draw(Canvas canvas, GameView gv) {
        Player p = gv.player;
        float w = canvas.getWidth();
        float h = canvas.getHeight();

        drawOverlay(canvas, "INVENTÁRIO DE SUPRIMENTOS & ITENS", Color.argb(245, 15, 22, 32));

        exitBtn.set(w - 180, 30, w - 40, 90);
        drawExitButton(canvas, exitBtn);

        paint.setColor(Color.WHITE); paint.setTextSize(18); paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("Toque em um item para usá-lo ou ativá-lo imediatamente", w / 2f, 115, paint);

        Map<String, Integer> inventory = p.getInventory();
        if (inventory.isEmpty()) {
            paint.setColor(Color.GRAY); paint.setTextSize(24);
            canvas.drawText("Seu inventário está vazio!", w / 2f, h / 2f, paint);
            return;
        }

        int cols = 3;
        float startX = 80;
        float startY = 150;
        float cardW = (w - 160 - (cols - 1) * 20) / cols;
        float cardH = 95;
        float spacing = 20;

        int index = 0;
        for (Map.Entry<String, Integer> entry : inventory.entrySet()) {
            String itemName = entry.getKey();
            int qty = entry.getValue();

            int r = index / cols;
            int c = index % cols;

            float cx = startX + c * (cardW + spacing);
            float cy = startY + r * (cardH + spacing);

            RectF card = new RectF(cx, cy, cx + cardW, cy + cardH);

            // Fundo do card
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(180, 35, 48, 65));
            canvas.drawRoundRect(card, 14, 14, paint);

            // Borda
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(3);
            paint.setColor(itemName.contains("Poção") ? Color.GREEN : itemName.contains("Pedra") ? Color.YELLOW : Color.CYAN);
            canvas.drawRoundRect(card, 14, 14, paint);

            // Ícone Emoji baseado no nome
            String icon = "📦";
            if (itemName.contains("Poção")) icon = "🧪";
            else if (itemName.contains("Pokébola")) icon = "🔴";
            else if (itemName.contains("Fruta") || itemName.contains("Doce")) icon = "🍎";
            else if (itemName.contains("Peixe")) icon = "🐟";
            else if (itemName.contains("Pedra")) icon = "💎";
            else if (itemName.contains("Madeira")) icon = "🪵";
            else if (itemName.contains("Ferro")) icon = "⛓️";

            paint.setStyle(Paint.Style.FILL);
            paint.setTextSize(36);
            paint.setTextAlign(Paint.Align.LEFT);
            canvas.drawText(icon, card.left + 20, card.centerY() + 12, paint);

            // Nome do Item
            paint.setColor(Color.WHITE);
            paint.setTextSize(18);
            paint.setFakeBoldText(true);
            canvas.drawText(itemName, card.left + 85, card.top + 32, paint);

            // Quantidade
            paint.setColor(Color.YELLOW);
            paint.setTextSize(16);
            canvas.drawText("Qtd: " + qty, card.left + 85, card.bottom - 20, paint);

            paint.setFakeBoldText(false);
            index++;
        }
    }
}
