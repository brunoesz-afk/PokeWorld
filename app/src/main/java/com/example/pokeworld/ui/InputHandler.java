package com.example.pokeworld.ui;

import android.graphics.RectF;
import android.view.MotionEvent;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.ui.input.*;

import java.util.HashMap;
import java.util.Map;

/**
 * InputHandler: Centraliza o processamento de toques na tela.
 * Gerencia a alternância entre menus e as interações com o mundo.
 */
public class InputHandler {
    private GameView gv;
    public Joystick joystick;
    private Map<UIManager.UIState, InputExtension> handlers = new HashMap<>();

    public InputHandler(GameView gv) {
        this.gv = gv;
        // Joystick em posição fixa no canto inferior esquerdo
        this.joystick = new Joystick(250, 550, 140, 70);

        handlers.put(UIManager.UIState.EXPLORING, new ExploringInput());
        handlers.put(UIManager.UIState.INVENTORY, new InventoryInput());
        handlers.put(UIManager.UIState.CREATURES, new CreatureInput());
        handlers.put(UIManager.UIState.SHOP, new ShopInput());
        handlers.put(UIManager.UIState.MISSIONS, new MissionInput());
        handlers.put(UIManager.UIState.FORGE, new ForgeInput());
        handlers.put(UIManager.UIState.TALENTS, new TalentInput());
        handlers.put(UIManager.UIState.SUMMON, new SummonInput());
        handlers.put(UIManager.UIState.BREEDING, new BreedingInput());
        handlers.put(UIManager.UIState.RUNES, new RuneInput());
        handlers.put(UIManager.UIState.TELEPORT, new TeleportInput());
        handlers.put(UIManager.UIState.SETTINGS, new SettingsInput());
        handlers.put(UIManager.UIState.CRAFTING, new CraftingInput());
        handlers.put(UIManager.UIState.POKEMON_DETAILS, new DexInput());
        handlers.put(UIManager.UIState.STORAGE, new StorageInput());
        handlers.put(UIManager.UIState.CLAN, new ClanInput());
        handlers.put(UIManager.UIState.EVOLUTION, new EvolutionItemsInput());
        handlers.put(UIManager.UIState.PLACEMENT, new PlacementInput());
        handlers.put(UIManager.UIState.MOUNT_FACTORY, new MountFactoryInput());
        handlers.put(UIManager.UIState.GYM, new GymInput());
        handlers.put(UIManager.UIState.AINCRAD, new AincradInput());
        handlers.put(UIManager.UIState.ITEM_STORAGE, new ItemStorageInput());
        handlers.put(UIManager.UIState.TRAINER_SKILLS, new TrainerSkillsInput());
        handlers.put(UIManager.UIState.STARTER_SELECTION, new com.example.pokeworld.ui.input.StarterSelectionInput());
        handlers.put(UIManager.UIState.TOWER_SELECT, new TowerSelectInput());
        handlers.put(UIManager.UIState.BUILD, new BuildInput());
        
        InputExtension exploreFallback = new InputExtension() {
            @Override
            public boolean handle(float x, float y, GameView gv) {
                gv.ui.state = UIManager.UIState.EXPLORING;
                return true;
            }
        };
        handlers.put(UIManager.UIState.BESTIARY, exploreFallback);
        handlers.put(UIManager.UIState.ACHIEVEMENTS, exploreFallback);
        handlers.put(UIManager.UIState.VICTORY, exploreFallback);
    }

    private int joystickPointerId = -1;

    public boolean handle(MotionEvent event) {
        int action = event.getActionMasked();
        int pointerIndex = event.getActionIndex();
        float x = event.getX(pointerIndex);
        float y = event.getY(pointerIndex);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                if (gv.ui.isDialogueActive) {
                    gv.ui.isDialogueActive = false;
                    handleDialogueTransition();
                    return true;
                }

                if (gv.ui.state != UIManager.UIState.EXPLORING) {
                    float backW = 160, backH = 70;
                    RectF globalBackBtn = new RectF(gv.getWidth() - backW - 20, 20, gv.getWidth() - 20, 20 + backH);
                    
                    if (globalBackBtn.contains(x, y)) {
                        gv.ui.state = UIManager.UIState.EXPLORING;
                        return true;
                    }
                }

                InputExtension handler = handlers.get(gv.ui.state);
                if (handler != null && handler.handleEvent(event, gv)) {
                    return true;
                }

                if (gv.ui.state == UIManager.UIState.EXPLORING) {
                    if (joystick.isPressed(x, y) && !gv.fishing.isSelectingFishingSpot && joystickPointerId == -1) {
                        joystickPointerId = event.getPointerId(pointerIndex);
                        joystick.setIsPressed(true);
                    } else {
                        if (handler != null) {
                            handler.handle(x, y, gv);
                        }
                    }
                } else {
                    if (handler != null) {
                        handler.handle(x, y, gv);
                    }
                }
                return true;

            case MotionEvent.ACTION_MOVE:
                InputExtension moveHandler = handlers.get(gv.ui.state);
                if (moveHandler != null && moveHandler.handleEvent(event, gv)) {
                    return true;
                }

                if (joystickPointerId != -1) {
                    int pointerIdx = event.findPointerIndex(joystickPointerId);
                    if (pointerIdx != -1) {
                        joystick.setActuator(event.getX(pointerIdx), event.getY(pointerIdx));
                    }
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                InputExtension upHandler = handlers.get(gv.ui.state);
                if (upHandler != null && upHandler.handleEvent(event, gv)) {
                    return true;
                }

                if (joystickPointerId != -1 && event.getPointerId(pointerIndex) == joystickPointerId) {
                    joystick.setIsPressed(false);
                    joystick.resetActuator();
                    joystickPointerId = -1;
                }
                return true;
        }
        return true;
    }

    private void handleDialogueTransition() {
        if (gv.tutorial != null && gv.tutorial.isTutorialActive()) {
            gv.tutorial.onDialogueAdvanced();
            return;
        }
        if (gv.ui.currentSpeaker.equals("Mercador")) gv.ui.state = UIManager.UIState.SHOP;
        else if (gv.ui.currentSpeaker.equals("Treinador")) gv.ui.state = UIManager.UIState.CREATURES;
        else if (gv.ui.currentSpeaker.equals("Ferreiro")) gv.ui.state = UIManager.UIState.FORGE;
        else if (gv.ui.currentSpeaker.equals("Grande Mestre")) gv.ui.state = UIManager.UIState.CLAN;
        else if (gv.ui.currentSpeaker.equals("Invocador") || gv.ui.currentSpeaker.contains("Invocador")) gv.ui.state = UIManager.UIState.SUMMON;
        else if (gv.ui.currentSpeaker.equals("Criador") || gv.ui.currentSpeaker.contains("Criador")) {
            if (!gv.player.hasSeenDaycareIntro()) {
                gv.player.setDaycareIntroSeen(true);
                gv.activity.dbHelper.setDaycareIntroSeen();
                android.content.Intent intent = new android.content.Intent(gv.activity, com.example.pokeworld.core.VideoActivity.class);
                gv.activity.startActivity(intent);
            }
            gv.ui.state = UIManager.UIState.BREEDING;
        }
        else if (gv.ui.currentSpeaker.contains("Engenharia")) gv.ui.state = UIManager.UIState.CRAFTING;
        else if (gv.ui.currentSpeaker.contains("Montaria")) gv.ui.state = UIManager.UIState.MOUNT_FACTORY;
        else if (gv.ui.currentSpeaker.contains("Estoque")) gv.ui.state = UIManager.UIState.ITEM_STORAGE;
        else if (gv.ui.currentSpeaker.contains("Batalha")) gv.ui.state = UIManager.UIState.GYM;
        else if (gv.ui.currentSpeaker.contains("Caçador")) gv.ui.state = UIManager.UIState.BOUNTIES;
        else if (gv.ui.currentSpeaker.contains("Treino")) gv.ui.state = UIManager.UIState.STORAGE;
        else if (gv.ui.currentSpeaker.equals("Mestre da Base")) gv.ui.state = UIManager.UIState.STORAGE;
        else if (gv.ui.currentSpeaker.equals("Brock")) gv.ui.state = UIManager.UIState.GYM;
        else if (gv.ui.currentSpeaker.equals("Portão de Aincrad")) gv.ui.state = UIManager.UIState.AINCRAD;
        else if (gv.ui.currentSpeaker.equals("Arena Master")) {
            if (gv.player.getLevel() >= 10) gv.bossRush.start();
            else {
                gv.world.loadMap("ARENA", gv.player, gv.companion);
                gv.arena.startArena(gv.time.isTournamentDay());
            }
        }
    }
}
