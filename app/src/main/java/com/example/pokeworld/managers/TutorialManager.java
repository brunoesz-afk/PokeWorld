package com.example.pokeworld.managers;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.entities.Enemy;
import com.example.pokeworld.ui.UIManager;

public class TutorialManager {
    public enum State {
        NONE,
        INTRO_DIALOGUE,
        STARTER_SELECTION,
        GIVE_BALLS_DIALOGUE,
        SPAWN_RATTATA,
        CAPTURING_RATTATA,
        TUTORIAL_COMPLETED_DIALOGUE,
        FINISHED
    }

    private GameView gv;
    private State currentState = State.NONE;
    private Enemy tutorialRattata;
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
            currentState = State.FINISHED;
            gv.ui.isDialogueActive = false;
            finishTutorial();
        }
    }

    public void onStarterSelected(String starterSpecies) {
        // Altera o companheiro ativo para o inicial escolhido
        gv.activity.dbHelper.updateActiveSpecies(starterSpecies);
        gv.companion.setSpecies(starterSpecies);
        gv.player.addCreatureToCollection(starterSpecies, false, com.example.pokeworld.entities.Rarity.COMMON, 0);

        gv.ui.state = UIManager.UIState.EXPLORING;
        currentState = State.GIVE_BALLS_DIALOGUE;

        // Dá 5 Pokébolas de presente do Professor
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

    public void update() {
        if (currentState == State.CAPTURING_RATTATA) {
            // Verifica se o Rattata foi capturado ou derrotado
            if (tutorialRattata != null && (!tutorialRattata.isAlive || !gv.world.enemies.contains(tutorialRattata))) {
                currentState = State.TUTORIAL_COMPLETED_DIALOGUE;
                gv.ui.isDialogueActive = true;
                gv.ui.currentSpeaker = "Prof. Invocador";
                gv.ui.currentDialogue = "Parabéns! Você aprendeu os passos básicos de um verdadeiro Treinador!\n" +
                        "Agora o mundo aberto de PokéWorld está livre para você explorar!";
            }
        }
    }

    private void finishTutorial() {
        gv.activity.dbHelper.setTutorialCompleted(true);
        gv.ui.showMessage("🎉 TUTORIAL CONCLUÍDO! BEM-VINDO À SUA BASE!");
        gv.missions.giveInitialMission();
        gv.world.loadMap("HOME", gv.player, gv.companion, "CENTER");
    }

    public void drawOverlay(Canvas canvas) {
        if (currentState == State.CAPTURING_RATTATA) {
            // Desenha banner com dica piscante de como capturar
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(220, 20, 20, 40));
            RectF banner = new RectF(canvas.getWidth() / 2f - 340, 20, canvas.getWidth() / 2f + 340, 100);
            canvas.drawRoundRect(banner, 15, 15, paint);

            paint.setColor(Color.YELLOW);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(3);
            canvas.drawRoundRect(banner, 15, 15, paint);

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.WHITE);
            paint.setTextSize(20);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("💡 DICA DO PROFESSOR:", canvas.getWidth() / 2f, 50, paint);
            paint.setColor(Color.CYAN);
            paint.setTextSize(17);
            canvas.drawText("Arraste a Pokébola do botão inferior até o Rattata para capturá-lo!", canvas.getWidth() / 2f, 80, paint);
        }
    }

    public boolean isTutorialActive() {
        return currentState != State.NONE && currentState != State.FINISHED;
    }

    public State getCurrentState() {
        return currentState;
    }
}
