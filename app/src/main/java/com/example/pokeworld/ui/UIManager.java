package com.example.pokeworld.ui;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.entities.CapturedCreature;
import com.example.pokeworld.ui.handlers.*;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UIManager {
    private Paint paint = new Paint();
    public enum UIState { EXPLORING, INVENTORY, CREATURES, SHOP, MISSIONS, FORGE, BESTIARY, TALENTS, ACHIEVEMENTS, CLAN, SUMMON, BREEDING, VICTORY, RUNES, BOUNTIES, TELEPORT, SETTINGS, CRAFTING, POKEMON_DETAILS, STORAGE, EVOLUTION, PLACEMENT, MOUNT_FACTORY, GYM, AINCRAD, ITEM_STORAGE, TRAINER_SKILLS, STARTER_SELECTION, TOWER_SELECT, BUILD }
    public UIState state = UIState.EXPLORING;

    public String pendingBiomeName = "";
    public int currentBiomeFloor = 1;

    private Map<UIState, UIHandler> handlers = new HashMap<>();

    public boolean isDialogueActive = false;
    public String currentDialogue = "", currentSpeaker = "";
    public String feedbackMsg = "";
    public long msgTime = 0;
    
    public String placementBuildingName = "";
    public double placementX, placementY;

    private List<String> messageLog = new ArrayList<>();
    private static final int MAX_LOG_MESSAGES = 5;

    public float fadeAlpha = 0;
    private boolean fadingIn = false;

    public UIManager() {
        handlers.put(UIState.EXPLORING, new ExploringUI());
        handlers.put(UIState.INVENTORY, new InventoryUI());
        handlers.put(UIState.CREATURES, new CreatureUI());
        handlers.put(UIState.SHOP, new ShopUI());
        handlers.put(UIState.MISSIONS, new MissionUI());
        handlers.put(UIState.FORGE, new ForgeUI());
        handlers.put(UIState.BESTIARY, new BestiaryUI());
        handlers.put(UIState.TALENTS, new TalentUI());
        handlers.put(UIState.ACHIEVEMENTS, new AchievementUI());
        handlers.put(UIState.CLAN, new ClanSelectionUI());
        handlers.put(UIState.SUMMON, new SummonUI());
        handlers.put(UIState.BREEDING, new BreedingUI());
        handlers.put(UIState.VICTORY, new VictoryUI());
        handlers.put(UIState.RUNES, new RuneUI());
        handlers.put(UIState.BOUNTIES, new BountyUI());
        handlers.put(UIState.TELEPORT, new TeleportUI());
        handlers.put(UIState.SETTINGS, new SettingsUI());
        handlers.put(UIState.CRAFTING, new CraftingUI());
        handlers.put(UIState.POKEMON_DETAILS, new DexUI());
        handlers.put(UIState.STORAGE, new StorageUI());
        handlers.put(UIState.EVOLUTION, new EvolutionItemsUI());
        handlers.put(UIState.PLACEMENT, new PlacementUI());
        handlers.put(UIState.ITEM_STORAGE, new ItemStorageUI());
        handlers.put(UIState.MOUNT_FACTORY, new MountFactoryUI());
        handlers.put(UIState.GYM, new GymUI());
        handlers.put(UIState.AINCRAD, new AincradUI());
        handlers.put(UIState.TRAINER_SKILLS, new TrainerSkillsUI());
        handlers.put(UIState.STARTER_SELECTION, new com.example.pokeworld.ui.handlers.StarterSelectionUI());
        handlers.put(UIState.TOWER_SELECT, new TowerSelectUI());
        handlers.put(UIState.BUILD, new BuildUI());
    }
    
    public UIHandler getHandler(UIState state) {
        return handlers.get(state);
    }

    public List<String> getMessageLog() {
        return messageLog;
    }

    public void draw(Canvas canvas, GameView gv) {
        UIHandler handler = handlers.get(state);
        if (handler != null) {
            handler.draw(canvas, gv);
        }
        
        drawFeedback(canvas);
        drawFade(canvas);
    }

    private void drawFeedback(Canvas canvas) {
        if (System.currentTimeMillis() - msgTime < 2000) {
            paint.setColor(Color.WHITE); paint.setTextSize(40); paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(feedbackMsg, canvas.getWidth() / 2f, 120, paint);
        }
    }

    private void drawFade(Canvas canvas) {
        if (fadeAlpha > 0) {
            paint.setColor(Color.BLACK); paint.setAlpha((int)fadeAlpha);
            canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), paint);
            paint.setAlpha(255);
        }
    }

    public void showMessage(String m) { 
        feedbackMsg = m; 
        msgTime = System.currentTimeMillis(); 
        
        messageLog.add(0, m);
        if (messageLog.size() > MAX_LOG_MESSAGES) messageLog.remove(MAX_LOG_MESSAGES);
    }

    public void startFade() { fadeAlpha = 0; fadingIn = true; }
    public void updateFade() {
        if (fadingIn) {
            fadeAlpha += 25;
            if (fadeAlpha >= 255) { fadeAlpha = 255; fadingIn = false; }
        } else if (fadeAlpha > 0) {
            fadeAlpha -= 15;
            if (fadeAlpha < 0) fadeAlpha = 0;
        }
    }
    public boolean isFadeComplete() { return fadeAlpha >= 255; }

    public void selectForDex(CapturedCreature c) {
        DexUI dexUI = (DexUI) handlers.get(UIState.POKEMON_DETAILS);
        if (dexUI != null) {
            dexUI.selectedCreatureForDex = c;
            this.state = UIState.POKEMON_DETAILS;
        }
    }
}
