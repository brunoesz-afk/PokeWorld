package com.example.pokeworld.managers;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.entities.Enemy;
import com.example.pokeworld.entities.NPC;
import com.example.pokeworld.world.TileMap;
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
        FAINT_INFO_DIALOGUE,
        GO_TO_RANGER,               // seguir a seta até o Guarda-Florestal
        RANGER_BRIEFING_DIALOGUE,   // ele explica a missão
        HUNT_CATERPIE,              // derrotar 10 Caterpie espalhados
        RETURN_TO_RANGER,           // voltar para entregar
        RANGER_REWARD_DIALOGUE,     // recompensa
        TOWER_INTRO_DIALOGUE,       // e a Torre
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
            gv.ui.isDialogueActive = false;
            startRangerQuest();
        } else if (currentState == State.RANGER_BRIEFING_DIALOGUE) {
            gv.ui.isDialogueActive = false;
            startHunt();
        } else if (currentState == State.RANGER_REWARD_DIALOGUE) {
            currentState = State.TOWER_INTRO_DIALOGUE;
            gv.ui.isDialogueActive = true;
            gv.ui.currentSpeaker = "Prof. Invocador";
            gv.ui.currentDialogue = "Ótimo começo! Agora vamos direto para a Torre de Desafios testar a sua força!";
        } else if (currentState == State.TOWER_INTRO_DIALOGUE) {
            currentState = State.FINISHED;
            gv.ui.isDialogueActive = false;
            gv.activity.dbHelper.setKv("intro_step", 0);
            gv.activity.dbHelper.setTutorialCompleted(true);
            gv.tower.startTower();
            gv.ui.showMessage("🏰 Bem-vindo à Torre de Desafios!");
        } else if (currentState == State.NURSE_JOY_FAINT_DIALOGUE) {
            PartyManager.healAll(gv);
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
        } else if (currentState == State.FAINT_INFO_DIALOGUE) {
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

    /** Durante a captura guiada do primeiro Rattata a chance é sempre 100%, para o tutorial não quebrar. */
    public boolean isGuidedCapture() {
        return currentState == State.DRAG_POKEBALL_TUTORIAL;
    }

    public void onTutorialRattataCaptured() {
        if (currentState == State.DRAG_POKEBALL_TUTORIAL) {
            currentState = State.CAPTURED_RATTATA_DIALOGUE;
            gv.ui.isDialogueActive = true;
            gv.ui.currentSpeaker = "Prof. Invocador";
            gv.ui.currentDialogue = "Incrível! Você capturou seu primeiro Pokémon com sucesso!\n" +
                    "Agora conheça a floresta: fale com o Guarda-Florestal Ivo.\nSiga a SETA AMARELA até ele!";
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
        } else if (currentState == State.HUNT_CATERPIE) {
            keepCaterpiesAlive();
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

    }

    // ====================================================================== missão guiada (Guarda-Florestal)
    private static final int RANGER_TX = 82, RANGER_TY = 12, CATERPIE_GOAL = 10;
    private NPC ranger;
    private long lastCaterpieCheck = 0;
    private final java.util.Random rnd = new java.util.Random();

    private int caterpiesDefeated() { return gv.activity.dbHelper.getKv("intro_cat", 0); }

    /** Chamado pelo jogo ao carregar: retoma a missão guiada de onde parou, ou começa o tutorial do zero. */
    public void beginOrResume() {
        int step = gv.activity.dbHelper.getKv("intro_step", 0);
        if (step <= 0) { startTutorial(); return; }
        spawnRanger();
        if (step == 1) currentState = State.GO_TO_RANGER;
        else if (step == 2) { currentState = State.HUNT_CATERPIE; spawnCaterpies(Math.max(2, CATERPIE_GOAL - caterpiesDefeated()) + 2); }
        else { currentState = State.RETURN_TO_RANGER; ranger.setQuestIcon(NPC.QuestIcon.MISSION_READY); }
    }

    private void spawnRanger() {
        if (ranger != null) gv.world.npcs.remove(ranger);
        gv.world.tileMap.carveClearing(RANGER_TY, RANGER_TX, 3);
        ranger = new NPC("Guarda-Florestal Ivo", "", (RANGER_TX + 0.5) * TileMap.TILE_SIZE, (RANGER_TY + 0.5) * TileMap.TILE_SIZE, NPC.Type.GUARD);
        ranger.setAction("INTRO_RANGER");
        ranger.setSprite("treinador_montaria");
        ranger.setRole("🌲 FALE COMIGO", Color.rgb(255, 224, 120));
        ranger.setQuestIcon(NPC.QuestIcon.MISSION_AVAILABLE);
        gv.world.npcs.add(ranger);
    }

    private void startRangerQuest() {
        currentState = State.GO_TO_RANGER;
        gv.activity.dbHelper.setKv("intro_step", 1);
        spawnRanger();
        gv.ui.showMessage("🧭 Nova missão: fale com o Guarda-Florestal Ivo");
    }

    /** Toque no NPC (botão INTERAGIR perto dele). */
    public void onRangerTalk() {
        if (currentState == State.GO_TO_RANGER) {
            currentState = State.RANGER_BRIEFING_DIALOGUE;
            gv.ui.isDialogueActive = true;
            gv.ui.currentSpeaker = "Guarda-Florestal Ivo";
            gv.ui.currentDialogue = "Ah, você deve ser o novo Treinador! O Professor me avisou.\n" +
                    "Estes bosques estão cheios de Caterpie comendo todas as folhas! Derrote " + CATERPIE_GOAL + " deles, espalhados pela floresta, e volte aqui.\n" +
                    "Dica: toque no inimigo para atacar e arraste a Pokébola para capturar.";
        } else if (currentState == State.HUNT_CATERPIE) {
            gv.ui.showMessage("🐛 Ainda faltam " + (CATERPIE_GOAL - caterpiesDefeated()) + " Caterpie. Siga a seta!");
        } else if (currentState == State.RETURN_TO_RANGER) {
            gv.player.addItem("Poção", 3);
            gv.player.addItem("Pokébola", 5);
            gv.player.addGold(Economy.scale(300, Economy.REWARD));
            ranger.setQuestIcon(NPC.QuestIcon.NONE);
            currentState = State.RANGER_REWARD_DIALOGUE;
            gv.ui.isDialogueActive = true;
            gv.ui.currentSpeaker = "Guarda-Florestal Ivo";
            gv.ui.currentDialogue = "Excelente trabalho! A floresta agradece.\nTome: 3 Poções, 5 Pokébolas e " + Economy.scale(300, Economy.REWARD) + " de ouro.";
        }
    }

    private void startHunt() {
        currentState = State.HUNT_CATERPIE;
        gv.activity.dbHelper.setKv("intro_step", 2);
        gv.activity.dbHelper.setKv("intro_cat", 0);
        ranger.setQuestIcon(NPC.QuestIcon.NONE);
        spawnCaterpies(CATERPIE_GOAL + 2);
        gv.ui.showMessage("🐛 Missão: derrote " + CATERPIE_GOAL + " Caterpie. Siga a seta!");
    }

    /** Espalha Caterpie pela floresta, longe do jogador e uns dos outros. */
    private void spawnCaterpies(int n) {
        TileMap tm = gv.world.tileMap;
        int rows = tm.getRows(), cols = tm.getCols(), spawned = 0, tries = 0;
        double T = TileMap.TILE_SIZE, px = gv.player.getPositionX(), py = gv.player.getPositionY();
        while (spawned < n && tries++ < 600) {
            int r = 4 + rnd.nextInt(rows - 8), c = 4 + rnd.nextInt(cols - 8);
            double x = (c + 0.5) * T, y = (r + 0.5) * T;
            if (Math.hypot(x - px, y - py) < 9 * T) continue;
            if (Math.hypot(x - (RANGER_TX + 0.5) * T, y - (RANGER_TY + 0.5) * T) < 6 * T) continue;
            boolean close = false;
            for (Enemy e : gv.world.enemies) if (e.name.startsWith("Caterpie") && Math.hypot(x - e.getPositionX(), y - e.getPositionY()) < 5 * T) { close = true; break; }
            if (close) continue;
            int before = gv.world.enemies.size();
            gv.world.addEnemy("Caterpie", x, y, 32, Color.rgb(120, 200, 80), false, false, 1 + rnd.nextInt(2), 0);
            if (gv.world.enemies.size() > before) spawned++;
        }
    }

    /** Chamado pelo mundo quando um selvagem é derrotado. */
    public void onEnemyDefeated(String name) {
        if (currentState != State.HUNT_CATERPIE || name == null || !name.toLowerCase().startsWith("caterpie")) return;
        int n = caterpiesDefeated() + 1;
        gv.activity.dbHelper.setKv("intro_cat", n);
        if (n >= CATERPIE_GOAL) {
            currentState = State.RETURN_TO_RANGER;
            gv.activity.dbHelper.setKv("intro_step", 3);
            if (ranger != null) ranger.setQuestIcon(NPC.QuestIcon.MISSION_READY);
            gv.ui.showMessage("✅ " + CATERPIE_GOAL + " Caterpie derrotados! Volte ao Guarda-Florestal Ivo.");
        } else {
            gv.ui.showMessage("🐛 Caterpie derrotados: " + n + "/" + CATERPIE_GOAL);
        }
    }

    /** Linha da missão para o quadro de missões da tela (null = nada a mostrar). */
    public String questLine() {
        switch (currentState) {
            case GO_TO_RANGER: return "Fale com o Guarda-Florestal Ivo";
            case HUNT_CATERPIE: return "Derrote " + CATERPIE_GOAL + " Caterpie (" + caterpiesDefeated() + "/" + CATERPIE_GOAL + ")";
            case RETURN_TO_RANGER: return "Volte ao Guarda-Florestal Ivo";
            default: return null;
        }
    }

    private void keepCaterpiesAlive() {
        long now = System.currentTimeMillis();
        if (now - lastCaterpieCheck < 1500) return;
        lastCaterpieCheck = now;
        int alive = 0;
        for (Enemy e : gv.world.enemies) if (e.isAlive() && e.name.startsWith("Caterpie")) alive++;
        int need = CATERPIE_GOAL - caterpiesDefeated();
        if (alive < need) spawnCaterpies(need - alive);   // nunca deixa a missão impossível
    }

    /**
     * Primeiro desmaio de um Pokémon: explica uma única vez (salvo no banco) como funciona.
     * Nas próximas vezes não aparece nada — o jogo só troca o Pokémon sozinho.
     */
    public void onPetFainted() {
        if (hasExperiencedFaint || gv.activity.dbHelper.getKv("tut_faint", 0) == 1) { hasExperiencedFaint = true; return; }
        if (isTutorialActive()) return;
        hasExperiencedFaint = true;
        gv.activity.dbHelper.setKv("tut_faint", 1);
        currentState = State.FAINT_INFO_DIALOGUE;
        gv.ui.isDialogueActive = true;
        gv.ui.currentSpeaker = "Prof. Invocador";
        gv.ui.currentDialogue = "Seu Pokémon ficou sem forças e voltou para a Pokébola!\n" +
                "Se ainda houver outro Pokémon com vida no seu time, ele entra na luta na hora. " +
                "Se o time inteiro desmaiar, você é levado ao Centro de Cura e a Enfermeira Joy cuida de todos. " +
                "Por isso, leve até 6 Pokémon no time!";
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
            com.example.pokeworld.ui.handlers.ExploringUI eui = (com.example.pokeworld.ui.handlers.ExploringUI) gv.ui.getHandler(UIManager.UIState.EXPLORING);
            if (eui != null) drawArrow(canvas, eui.teleportBtn.centerX(), eui.teleportBtn.top - 6, "TOQUE AQUI!");
        } else if (currentState == State.GO_TO_RANGER || currentState == State.RETURN_TO_RANGER) {
            boolean back = currentState == State.RETURN_TO_RANGER;
            drawQuestBanner(canvas, back ? "✅ MISSÃO CUMPRIDA!" : "🧭 MISSÃO: FALE COM O GUARDA-FLORESTAL IVO",
                    back ? "Volte ao Guarda-Florestal Ivo para receber a recompensa." : "Ande pela floresta seguindo a seta amarela até encontrá-lo.");
            if (ranger != null) drawGuide(canvas, ranger.getPositionX(), ranger.getPositionY(), "IVO");
        } else if (currentState == State.HUNT_CATERPIE) {
            drawQuestBanner(canvas, "🐛 MISSÃO: DERROTE " + CATERPIE_GOAL + " CATERPIE (" + caterpiesDefeated() + "/" + CATERPIE_GOAL + ")",
                    "Toque no Caterpie para atacar. Pontos vermelhos no minimapa = onde estão.");
            Enemy best = null; double bd = Double.MAX_VALUE;
            for (Enemy e : gv.world.enemies) {
                if (!e.isAlive() || !e.name.startsWith("Caterpie")) continue;
                double d = Math.hypot(e.getPositionX() - gv.player.getPositionX(), e.getPositionY() - gv.player.getPositionY());
                if (d < bd) { bd = d; best = e; }
            }
            if (best != null) drawGuide(canvas, best.getPositionX(), best.getPositionY(), "CATERPIE");
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

    /** Faixa da missão guiada, logo abaixo da barra de Pokémon do topo. */
    private void drawQuestBanner(Canvas canvas, String title, String subtitle) {
        float sw = canvas.getWidth();
        float k = com.example.pokeworld.ui.handlers.ExploringUI.uiScale(sw, canvas.getHeight());
        float top = 100 * k;
        float bl = com.example.pokeworld.ui.handlers.ExploringUI.hudLeft(k), br = com.example.pokeworld.ui.handlers.ExploringUI.hudRight(sw, k), half = (br - bl) / 2f;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(215, 18, 28, 22));
        RectF b = new RectF(bl, top, br, top + 62 * k);
        canvas.drawRoundRect(b, 14 * k, 14 * k, paint);
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(2.5f * k);
        paint.setColor(Color.rgb(255, 214, 90));
        canvas.drawRoundRect(b, 14 * k, 14 * k, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(Color.WHITE); paint.setTextSize(18 * k); paint.setFakeBoldText(true);
        canvas.drawText(title, (bl + br) / 2f, top + 25 * k, paint);
        paint.setFakeBoldText(false);
        paint.setColor(Color.rgb(170, 235, 255)); paint.setTextSize(14 * k);
        String sub = subtitle;
        while (sub.length() > 8 && paint.measureText(sub) > 2 * half - 24 * k) sub = sub.substring(0, sub.length() - 2);
        canvas.drawText(sub.equals(subtitle) ? sub : sub + "…", (bl + br) / 2f, top + 49 * k, paint);
    }

    /**
     * Guia na tela: se o alvo está visível, uma seta pula em cima dele; se está fora da tela,
     * uma seta na borda aponta para a direção e mostra a distância em passos.
     */
    private void drawGuide(Canvas canvas, double tx, double ty, String label) {
        float sw = canvas.getWidth(), sh = canvas.getHeight();
        float k = com.example.pokeworld.ui.handlers.ExploringUI.uiScale(sw, sh), z = gv.world.getCurrentZoom();
        float sx = (float) ((tx - gv.world.getCameraX()) * z), sy = (float) ((ty - gv.world.getCameraY()) * z);
        int steps = (int) Math.round(Math.hypot(tx - gv.player.getPositionX(), ty - gv.player.getPositionY()) / TileMap.TILE_SIZE);
        float left = 50 * k, right = sw - 50 * k, top = 190 * k, bottom = sh - 160 * k;
        if (sx >= left && sx <= right && sy >= top + 40 && sy <= bottom) {
            drawArrow(canvas, sx, sy - 70 * z, label);
            return;
        }
        float cx = sw / 2f, cy = sh / 2f, dx = sx - cx, dy = sy - cy;
        float len = (float) Math.max(1.0, Math.hypot(dx, dy));
        float ux = dx / len, uy = dy / len;
        float limX = ux > 0 ? right - cx : cx - left, limY = uy > 0 ? bottom - cy : cy - top;
        float s = Math.min(Math.abs(ux) < 1e-4f ? 1e9f : limX / Math.abs(ux), Math.abs(uy) < 1e-4f ? 1e9f : limY / Math.abs(uy));
        float px = cx + ux * s, py = cy + uy * s, vx = -uy, vy = ux, a = 26 * k;
        float pulse = 1f + 0.15f * (float) Math.sin(System.currentTimeMillis() / 180.0);
        Path tri = new Path();
        tri.moveTo(px + ux * a * 1.4f * pulse, py + uy * a * 1.4f * pulse);
        tri.lineTo(px - ux * a * 0.6f + vx * a, py - uy * a * 0.6f + vy * a);
        tri.lineTo(px - ux * a * 0.6f - vx * a, py - uy * a * 0.6f - vy * a);
        tri.close();
        paint.setStyle(Paint.Style.FILL); paint.setColor(Color.YELLOW);
        canvas.drawPath(tri, paint);
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(3 * k); paint.setColor(Color.argb(220, 0, 0, 0));
        canvas.drawPath(tri, paint);
        paint.setStyle(Paint.Style.FILL);
        float lx = Math.max(100 * k, Math.min(sw - 100 * k, px - ux * 84 * k)), ly = py - uy * 40 * k;
        paint.setTextAlign(Paint.Align.CENTER); paint.setFakeBoldText(true); paint.setTextSize(16 * k);
        paint.setColor(Color.argb(230, 0, 0, 0)); canvas.drawText(label + " · " + steps + " passos", lx + 1.5f, ly + 1.5f, paint);
        paint.setColor(Color.WHITE); canvas.drawText(label + " · " + steps + " passos", lx, ly, paint);
        paint.setFakeBoldText(false);
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
