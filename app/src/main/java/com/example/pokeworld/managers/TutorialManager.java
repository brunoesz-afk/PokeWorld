package com.example.pokeworld.managers;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.entities.Enemy;
import com.example.pokeworld.ui.UIManager;
import com.example.pokeworld.world.Building;

public class TutorialManager {
    public enum State {
        NONE,
        INTRO_DIALOGUE,
        STARTER_SELECTION,
        GIVE_BALLS_DIALOGUE,
        SPAWN_RATTATA,
        ATTACKING_RATTATA,
        DRAG_POKEBALL_TUTORIAL,
        CAPTURED_RATTATA_DIALOGUE,
        ARRIVED_AT_BASE_DIALOGUE,
        BUILDING_WORKSHOP,
        WORKSHOP_BUILT_DIALOGUE,
        CLICK_COMPASS_TUTORIAL,
        NURSE_JOY_FAINT_DIALOGUE,
        NURSE_JOY_HEALED_DIALOGUE,
        FINISHED
    }

    private GameView gv;
    private State currentState = State.NONE;
    private Enemy tutorialRattata;
    private boolean hasExperiencedFaint = false;
    private Paint paint = new Paint();

    public TutorialManager(GameView gv) {
        this.gv = gv;
    }

    public void startTutorial() {
        currentState = State.INTRO_DIALOGUE;
        gv.ui.isDialogueActive = true;
        gv.ui.currentSpeaker = "Prof. Invocador";
        gv.ui.currentDialogue = "Olá, jovem Treinador! Bem-vindo ao mundo de PokéWorld!\nEu sou o Professor. Hoje começa a sua grande jornada como Treinador!";
    }

    public void onDialogueAdvanced() {
        if (currentState == State.INTRO_DIALOGUE) {
            currentState = State.STARTER_SELECTION;
            gv.ui.isDialogueActive = false;
            gv.ui.state = UIManager.UIState.STARTER_SELECTION;
        } else if (currentState == State.GIVE_BALLS_DIALOGUE) {
            currentState = State.SPAWN_RATTATA;
            gv.ui.isDialogueActive = false;
            spawnTutorialRattata();
        } else if (currentState == State.CAPTURED_RATTATA_DIALOGUE) {
            currentState = State.FINISHED;
            gv.ui.isDialogueActive = false;
            gv.activity.dbHelper.setTutorialCompleted(true);
            gv.tower.startTower();
            gv.ui.showMessage("🏰 Bem-vindo à Torre de Desafios!");
        } else if (currentState == State.NURSE_JOY_FAINT_DIALOGUE) {
            gv.companion.healFull();
            gv.world.loadMap("CITY", gv.player, gv.companion, "CENTER");
            currentState = State.NURSE_JOY_HEALED_DIALOGUE;
            gv.ui.isDialogueActive = true;
            gv.ui.currentSpeaker = "Enfermeira Joy";
            gv.ui.currentDialogue = "Olá! Cuidei do seu parceiro com muito carinho no Centro de Cura da Cidade.\nSua saúde está 100% restaurada!";
        } else if (currentState == State.NURSE_JOY_HEALED_DIALOGUE) {
            currentState = State.CLICK_COMPASS_TUTORIAL;
            gv.ui.isDialogueActive = true;
            gv.ui.currentSpeaker = "Prof. Invocador";
            gv.ui.currentDialogue = "Excelente! Agora que seus Pokémons estão curados, toque no ícone da Bússola (🧭) no menu inferior para escolher os mapas de exploração do mundo aberto!";
        } else if (currentState == State.CLICK_COMPASS_TUTORIAL) {
            currentState = State.FINISHED;
            gv.ui.isDialogueActive = false;
        }
    }

    public void onStarterSelected(String starterSpecies) {
        gv.activity.dbHelper.updateActiveSpecies(starterSpecies);
        gv.companion.setSpecies(starterSpecies);
        gv.player.addCreatureToCollection(starterSpecies, false, com.example.pokeworld.entities.Rarity.COMMON, 0);

        gv.ui.state = UIManager.UIState.EXPLORING;
        currentState = State.GIVE_BALLS_DIALOGUE;

        gv.player.addItem("Pokébola", 5);

        gv.ui.isDialogueActive = true;
        gv.ui.currentSpeaker = "Prof. Invocador";
        gv.ui.currentDialogue = "Excelente escolha! " + starterSpecies + " é um parceiro incrível!\n" +
                "Tome 5 Pokébolas de presente! Olhe, um Rattata selvagem apareceu! Vamos aprender a batalhar!";
    }

    private void spawnTutorialRattata() {
        double px = gv.player.getPositionX();
        double py = gv.player.getPositionY();
        gv.world.addEnemy("Rattata", px + 160, py, 32, Color.GRAY, false, false, 1, 0);

        for (Enemy e : gv.world.enemies) {
            if (e.name.equalsIgnoreCase("Rattata")) {
                tutorialRattata = e;
                break;
            }
        }
        currentState = State.ATTACKING_RATTATA;
    }

    public void onTutorialRattataCaptured() {
        if (currentState == State.DRAG_POKEBALL_TUTORIAL) {
            currentState = State.CAPTURED_RATTATA_DIALOGUE;
            gv.ui.isDialogueActive = true;
            gv.ui.currentSpeaker = "Prof. Invocador";
            gv.ui.currentDialogue = "Incrível! Você capturou seu primeiro Pokémon com sucesso!\n" +
                    "Agora vamos direto para a Torre de Desafios testar sua força!";
        }
    }

    public void onTowerChallengeCompleted() {
        currentState = State.NURSE_JOY_FAINT_DIALOGUE;
        gv.ui.isDialogueActive = true;
        gv.ui.currentSpeaker = "Prof. Invocador";
        gv.ui.currentDialogue = "Parabéns por vencer o desafio da Torre com louvor!\nAgora vá até a Cidade falar com a Enfermeira Joy para curar os seus Pokémons!";
    }

    public void update() {
        if (currentState == State.ATTACKING_RATTATA) {
            if (tutorialRattata != null && (!tutorialRattata.isAlive || !gv.world.enemies.contains(tutorialRattata))) {
                currentState = State.DRAG_POKEBALL_TUTORIAL;
                gv.ui.isDialogueActive = true;
                gv.ui.currentSpeaker = "Prof. Invocador";
                gv.ui.currentDialogue = "Perfeito! O Rattata foi derrotado e caiu no chão!\n" +
                        "Agora segure o botão de Pokébola no menu inferior e arraste até o corpo do Rattata para capturá-lo!";
            }
        } else if (currentState == State.BUILDING_WORKSHOP) {
            com.example.pokeworld.world.BasePiece shop = gv.world.base != null ? gv.world.base.layout.find(com.example.pokeworld.world.BaseKind.OFICINA) : null;
            if (shop != null && shop.isUsable()) {
                currentState = State.WORKSHOP_BUILT_DIALOGUE;
                gv.ui.isDialogueActive = true;
                gv.ui.currentSpeaker = "Mestre da Base";
                gv.ui.currentDialogue = "Excelente trabalho! A Oficina está pronta!\n" +
                        "Agora toque na Bússola no menu inferior para se teleportar para a Floresta e explorar!";
            }
        }

        // Checagem de primeiro desmaio do Pokémon em batalha
        if (gv.companion != null && gv.companion.getHp() <= 0 && !hasExperiencedFaint) {
            onPetFainted();
        }
    }

    public void onPetFainted() {
        if (!hasExperiencedFaint) {
            hasExperiencedFaint = true;
            currentState = State.NURSE_JOY_FAINT_DIALOGUE;
            gv.ui.isDialogueActive = true;
            gv.ui.currentSpeaker = "Prof. Invocador";
            gv.ui.currentDialogue = "Oh não! O seu Pokémon ficou sem forças e foi derrotado em batalha!\n" +
                    "Quando isso acontecer, você deve ir imediatamente até o Centro de Cura na Cidade falar com a Enfermeira Joy!";
        }
    }

    public void onCompassClicked() {
        if (currentState == State.CLICK_COMPASS_TUTORIAL) {
            currentState = State.FINISHED;
            gv.world.loadMap("FOREST", gv.player, gv.companion, "BOTTOM");
            gv.ui.showMessage("🌲 BEM-VINDO À FLORESTA DE EXPLORAÇÃO!");
        }
    }

    public void drawOverlay(Canvas canvas) {
        float sw = canvas.getWidth();
        float sh = canvas.getHeight();

        if (currentState == State.ATTACKING_RATTATA) {
            drawGuideBanner(canvas, "⚔️ PASSO 1 - BATALHA:", "Toque no Rattata para que seu Pokémon o ataque até derrotá-lo!");
        } else if (currentState == State.DRAG_POKEBALL_TUTORIAL) {
            drawGuideBanner(canvas, "🔴 PASSO 2 - CAPTURA:", "Arraste a Pokébola do botão inferior até o corpo do Rattata!");
            if (tutorialRattata != null) {
                float rx = (float) (tutorialRattata.getPositionX() - gv.world.getCameraX());
                float ry = (float) (tutorialRattata.getPositionY() - gv.world.getCameraY());
                drawArrow(canvas, rx, ry - 30, "ARRASTE A POKÉBOLA AQUI!");
            }
        } else if (currentState == State.BUILDING_WORKSHOP) {
            drawGuideBanner(canvas, "🏗️ PRIMEIRA CONSTRUÇÃO:", "Toque em 🔨, escolha a Oficina, posicione onde quiser e confirme (5s)!");
            drawArrow(canvas, sw * 0.70f, sh * 0.40f, "CONSTRUA AQUI!");
        } else if (currentState == State.CLICK_COMPASS_TUTORIAL) {
            drawGuideBanner(canvas, "🧭 TELEPORTE PARA A FLORESTA:", "Toque no ícone de Bússola/Teleporte no menu inferior para viajar!");
            drawArrow(canvas, sw * 0.15f, sh - 110, "TOQUE AQUI!");
        }
    }

    private void drawGuideBanner(Canvas canvas, String title, String subtitle) {
        float sw = canvas.getWidth();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(220, 20, 20, 40));
        RectF banner = new RectF(sw / 2f - 360, 20, sw / 2f + 360, 105);
        canvas.drawRoundRect(banner, 15, 15, paint);

        paint.setColor(Color.YELLOW);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3);
        canvas.drawRoundRect(banner, 15, 15, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        paint.setTextSize(20);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(title, sw / 2f, 50, paint);
        paint.setColor(Color.CYAN);
        paint.setTextSize(17);
        canvas.drawText(subtitle, sw / 2f, 85, paint);
    }

    private void drawArrow(Canvas canvas, float x, float y, String label) {
        float animOffset = (float) Math.sin(System.currentTimeMillis() / 150.0) * 15f;
        float arrowY = y + animOffset;

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.YELLOW);

        Path arrowPath = new Path();
        arrowPath.moveTo(x, arrowY);
        arrowPath.lineTo(x - 20, arrowY - 30);
        arrowPath.lineTo(x + 20, arrowY - 30);
        arrowPath.close();
        canvas.drawPath(arrowPath, paint);

        paint.setColor(Color.WHITE);
        paint.setTextSize(18);
        paint.setFakeBoldText(true);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(label, x, arrowY - 40, paint);
        paint.setFakeBoldText(false);
    }

    public boolean isTutorialActive() {
        return currentState != State.NONE && currentState != State.FINISHED;
    }

    public State getCurrentState() {
        return currentState;
    }
}
