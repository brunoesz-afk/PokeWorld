package com.example.pokeworld.ui.handlers;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import com.example.pokeworld.core.GameView;
import com.example.pokeworld.entities.BuildingData;
import java.util.Map;

public class ShopUI extends BaseUI {
    public RectF buyPotionBtn = new RectF(200, 150, 600, 220);
    public RectF buyCrystalBtn = new RectF(200, 240, 600, 310);
    public RectF buySmokeBtn = new RectF(200, 330, 600, 400);
    public RectF buyRageBtn = new RectF(200, 420, 600, 490);
    public RectF sellBtn = new RectF(700, 420, 1100, 490);
    public RectF buyBallBtn = new RectF(700, 150, 1100, 220);
    public RectF buyGreatBallBtn = new RectF(700, 240, 1100, 310);
    public RectF buyUltraBallBtn = new RectF(700, 330, 1100, 400);

    @Override
    public void draw(Canvas canvas, GameView gv) {
        drawOverlay(canvas, "MERCADO", Color.argb(245, 30, 20, 0));
        Map<String, BuildingData> buildings = gv.activity.dbHelper.getBuildings();
        int shopLv = buildings.containsKey("Loja") ? buildings.get("Loja").level : 1;
        int potionPrice = gv.kingdom.calculateDiscountedPrice(50, shopLv);
        int crystalPrice = gv.kingdom.calculateDiscountedPrice(200, shopLv);
        int ballPrice = 100;
        int greatBallPrice = 300;
        int ultraBallPrice = 800;
        
        drawButton(canvas, buyPotionBtn, "POÇÃO (" + potionPrice + " 🪙)");
        drawButton(canvas, buyCrystalBtn, "CRISTAL (" + crystalPrice + " 🪙)");
        drawButton(canvas, buyBallBtn, "POKÉBOLA (" + ballPrice + " 🪙)");
        drawButton(canvas, buyGreatBallBtn, "GREAT BALL (" + greatBallPrice + " 🪙)");
        drawButton(canvas, buyUltraBallBtn, "ULTRA BALL (" + ultraBallPrice + " 🪙)");
        drawButton(canvas, buySmokeBtn, "BOMBA FUMAÇA (100 🪙)");
        drawButton(canvas, sellBtn, "VENDER TUDO (💰)");
    }
}
