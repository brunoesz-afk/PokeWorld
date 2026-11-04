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
        CAPTURING_RATTATA,
        TUTORIAL_COMPLETED_DIALOGUE,
        ARRIVED_AT_BASE_DIALOGUE,
        BUILDING_WORKSHOP,
        WORKSHOP_BUILT_DIALOGUE,
        CLICK_COMPASS_TUTORIAL,
        NURSE_JOY_FAINT_DIALOGUE,
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
        } else if (currentState == State.TUTORIAL_COMPLETED_DIALOGUE) {
            currentState = State.ARRIVED_AT_BASE_DIALOGUE;
            gv.activity.dbHelper.setTutorialCompleted(true);
            gv.world.loadMap("HOME", gv.player, gv.companion, "CENTER");
            gv.ui.isDialogueActive = true;
            gv.ui.currentSpeaker = "Mestre da Base";
            gv.ui.currentDialogue = "Bem-vindo à sua nova Base Central!\nSua primeira tarefa é construir a Oficina para fabricar Pokébolas e suprimentos!";
        } else if (currentState == State.ARRIVED_AT_BASE_DIALOGUE) {
            currentState = State.BUILDING_WORKSHOP;
            gv.ui.isDialogueActive = false;
            // Garante que o jogador possui recursos para a primeira Oficina do tutorial
            if (gv.player.getInventory().getOrDefault("Madeira", 0) < 5) {
                gv.player.addItem("Madeira", 5);
            }
            if (gv.player.getInventory().getOrDefault("Ferro", 0) < 2) {
                gv.player.addItem("Ferro", 2);
            }
        } else if (currentState == State.WORKSHOP_BUILT_DIALOGUE) {
            currentState = State.CLICK_COMPASS_TUTORIAL;
            gv.ui.isDialogueActive = false;
        } else if (currentState == State.NURSE_JOY_FAINT_DIALOGUE) {
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
                "Tome 5 Pokébolas de presente! Olhe, um Rattata selvagem apareceu! Vamos aprender a capturar!";
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
        currentState = State.CAPTURING_RATTATA;
    }

    public void onBuildingStarted(String name) {
        if (name.equalsIgnoreCase("Oficina") && currentState == State.BUILDING_WORKSHOP) {
            // A construção de 5s no ExploringInput.java terminará logo em breve
        }
    }

    public void update() {
        if (currentState == State.CAPTURING_RATTATA) {
            if (tutorialRattata != null && (!tutorialRattata.isAlive || !gv.world.enemies.contains(tutorialRattata))) {
                currentState = State.TUTORIAL_COMPLETED_DIALOGUE;
                gv.ui.isDialogueActive = true;
                gv.ui.currentSpeaker = "Prof. Invocador";
                gv.ui.currentDialogue = "Parabéns! Você aprendeu os passos básicos de um verdadeiro Treinador!\n" +
                        "Agora vamos viajar para a sua Base Central!";
            }
        } else if (currentState == State.BUILDING_WORKSHOP) {
            // Verifica se a oficina foi construída na base
            for (Building b : gv.world.buildings) {
                if (b.getName().equalsIgnoreCase("Oficina") && b.isConstructed()) {
                    currentState = State.WORKSHOP_BUILT_DIALOGUE;
                    gv.ui.isDialogueActive = true;
                    gv.ui.currentSpeaker = "Mestre da Base";
                    gv.ui.currentDialogue = "Excelente trabalho! A Oficina está pronta!\n" +
                            "Agora toque na Bússola no menu inferior para se teleportar para a Floresta e explorar!";
                    break;
                }
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
            gv.companion.healFull();
            gv.world.loadMap("CITY", gv.player, gv.companion, "CENTER");
            currentState = State.NURSE_JOY_FAINT_DIALOGUE;
            gv.ui.isDialogueActive = true;
            gv.ui.currentSpeaker = "Enfermeira Joy";
            gv.ui.currentDialogue = "Oh não! Seu Pokémon desmaiou em batalha por exaustão!\n" +
                    "Trouxe vocês imediatamente ao Centro de Cura! Seu parceiro foi totalmente restaurado!";
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

        if (currentState == State.CAPTURING_RATTATA) {
            drawGuideBanner(canvas, "💡 DICA DO PROFESSOR:", "Ataque o Rattata para derrotá-lo e depois arraste a Pokébola até ele!");
        } else if (currentState == State.BUILDING_WORKSHOP) {
            drawGuideBanner(canvas, "🏗️ PRIMEIRA CONSTRUÇÃO:", "Aproxime-se e toque para construir a Oficina (Leva apenas 5s)!");
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
