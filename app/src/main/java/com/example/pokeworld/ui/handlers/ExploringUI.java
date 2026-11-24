package com.example.pokeworld.ui.handlers;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import com.example.pokeworld.core.GameView;
import com.example.pokeworld.entities.CapturedCreature;
import com.example.pokeworld.entities.Companion;
import com.example.pokeworld.entities.Enemy;
import com.example.pokeworld.entities.Mission;
import com.example.pokeworld.entities.NPC;
import com.example.pokeworld.entities.Player;
import com.example.pokeworld.entities.Skill;
import com.example.pokeworld.managers.SpriteManager;
import com.example.pokeworld.world.Building;
import com.example.pokeworld.world.TileMap;
import com.example.pokeworld.world.WorldManager;
import java.util.ArrayList;
import java.util.List;

public class ExploringUI extends BaseUI {
    public RectF inventoryBtn = new RectF();
    public RectF achBtn = new RectF();
    public RectF runesBtn = new RectF();
    public RectF teleportBtn = new RectF();
    public RectF friendsBtn = new RectF();
    public RectF towerBtn = new RectF();
    public RectF settingsBtn = new RectF();
    public RectF homeBtn = new RectF();
    public RectF buildBtn = new RectF();
    public RectF creaturesBtn = new RectF();
    public RectF missionsBtn = new RectF();
    public RectF bestiaryBtn = new RectF();
    public RectF talentsBtn = new RectF();
    public RectF mountBtn = new RectF();
    public RectF dashBtn = new RectF();
    public RectF clanSkillBtn = new RectF();
    public RectF ultimateBtn = new RectF();
    public RectF tacticBtn = new RectF();
    public RectF fishingBtn = new RectF();
    public RectF interactBtn = new RectF();
    public RectF constructBuildingBtn = new RectF();
    public RectF captureBtn = new RectF(); 
    public RectF trainerSkillsBtn = new RectF();
    public RectF quickHealBtn = new RectF();
    public RectF quickEatBtn = new RectF();
    public RectF zoomInBtn = new RectF();
    public RectF zoomOutBtn = new RectF();
    public RectF dialogueBox = new RectF();
    public List<RectF> skillButtons = new ArrayList<>();
    private RectF minimapRect = new RectF();

    public float dragBallX, dragBallY;
    public boolean isDraggingBall = false;
    private long lastFishingInteractionTime = 0;
    private boolean isFishingBarVisible = false;

    /** Posição de cada slot da troca rápida (linha horizontal centralizada no topo). Usada também pelo toque. */
    public final List<RectF> swapSlots = new ArrayList<>();

    /** Fator de escala da interface (a tela é em pixels reais: 1.0 = 1280x720). */
    public static float uiScale(float w, float h) { return Math.max(0.85f, Math.min(1.6f, Math.min(w / 1280f, h / 720f))); }

    /**
     * Linha da troca rápida: fica entre o painel do treinador (esquerda) e o botão do mapa + minimapa (direita),
     * centralizada nesse espaço e encolhendo se o time for grande, para nunca encostar em nada.
     */
    public static RectF partySlot(float screenW, float screenH, int index, int count) {
        float k = uiScale(screenW, screenH);
        float left = 420 * k + 16 * k, right = screenW - (240 + 130) * k;
        float gap = 8 * k;
        float size = Math.min(84 * k, (right - left - gap * Math.max(0, count - 1)) / Math.max(1, count));
        float total = count * size + Math.max(0, count - 1) * gap;
        float x = (left + right) / 2f - total / 2f + index * (size + gap);
        return new RectF(x, 8 * k, x + size, 8 * k + size);
    }

    @Override
    public void draw(Canvas canvas, GameView gv) {
        updateButtonPositions(canvas.getWidth(), canvas.getHeight());
        updateFishingBarVisibility(gv);

        // Indicador de Status Multiplayer na HUD
        boolean isOnline = com.example.pokeworld.managers.MultiplayerManager.getInstance().isConnected();
        String serverStatus = isOnline ? "🟢 Servidor Online" : "🟡 Conectando ao Servidor...";
        paint.setColor(isOnline ? Color.GREEN : Color.YELLOW);
        paint.setTextSize(14 * uiScale(canvas.getWidth(), canvas.getHeight()));
        paint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(serverStatus, canvas.getWidth() - 20, 22 * uiScale(canvas.getWidth(), canvas.getHeight()), paint);
        
        String friendNotice;
        while ((friendNotice = com.example.pokeworld.managers.FriendManager.getInstance().pollNotice()) != null) gv.ui.showMessage(friendNotice);

        drawHUD(canvas, gv.companion, gv.player, gv);
        drawMinimap(canvas, gv);
        drawBossBar(canvas, gv.world);
        if (isFishingBarVisible) drawFishingBar(canvas, gv.player);
        if (gv.ui.isDialogueActive) drawDialogueBox(canvas, gv.ui.currentSpeaker, gv.ui.currentDialogue);
        drawMessageLog(canvas, gv.ui.getMessageLog());
        
        if (isDraggingBall) {
            drawDraggingBall(canvas);
        }
    }

    private void updateButtonPositions(int w, int h) {
        final float k = uiScale(w, h);
        float padding = 20 * k;
        float btnSize = 100 * k;
        float spacing = 10 * k;
        
        tacticBtn.set(16 * k, h - 350 * k, 16 * k + 90 * k, h - 350 * k + 90 * k);   // fora do raio do joystick
        fishingBtn.set(w/2f - 60, h * 0.15f, w/2f + 60, h * 0.15f + 120);

        inventoryBtn.set(w - btnSize - padding, h - btnSize - padding, w - padding, h - padding);
        achBtn.set(w - (btnSize + spacing) * 2 + spacing - padding, h - btnSize - padding, w - btnSize - spacing - padding, h - padding);
        runesBtn.set(w - (btnSize + spacing) * 3 + spacing - padding, h - btnSize - padding, w - (btnSize + spacing) * 2 - padding, h - padding);
        // o botão do MAPA (bússola) agora é grande e fica ao lado do minimapa, no topo; a torre ocupa o lugar dele na fileira
        towerBtn.set(w - (btnSize + spacing) * 4 + spacing - padding, h - btnSize - padding, w - (btnSize + spacing) * 3 - padding, h - padding);

        creaturesBtn.set(w - btnSize - padding, h - (btnSize + spacing) * 2 + spacing - padding, w - padding, h - btnSize - spacing - padding);
        missionsBtn.set(w - (btnSize + spacing) * 2 + spacing - padding, h - (btnSize + spacing) * 2 + spacing - padding, w - btnSize - spacing - padding, h - btnSize - spacing - padding);
        bestiaryBtn.set(w - (btnSize + spacing) * 3 + spacing - padding, h - (btnSize + spacing) * 2 + spacing - padding, w - (btnSize + spacing) * 2 - padding, h - btnSize - spacing - padding);
        talentsBtn.set(w - (btnSize + spacing) * 4 + spacing - padding, h - (btnSize + spacing) * 2 + spacing - padding, w - (btnSize + spacing) * 3 - padding, h - btnSize - spacing - padding);
        settingsBtn.set(w - (btnSize + spacing) * 5 + spacing - padding, h - (btnSize + spacing) * 2 + spacing - padding, w - (btnSize + spacing) * 4 - padding, h - btnSize - spacing - padding);
        homeBtn.set(w - (btnSize + spacing) * 6 + spacing - padding, h - (btnSize + spacing) * 2 + spacing - padding, w - (btnSize + spacing) * 5 - padding, h - btnSize - spacing - padding);
        buildBtn.set(w - (btnSize + spacing) * 7 + spacing - padding, h - (btnSize + spacing) * 2 + spacing - padding, w - (btnSize + spacing) * 6 - padding, h - btnSize - spacing - padding);

        mountBtn.set(w - btnSize - padding, h - (btnSize + spacing) * 3 + spacing - padding, w - padding, h - (btnSize + spacing) * 2 - padding);
        clanSkillBtn.set(w - (btnSize + spacing) * 2 + spacing - padding, h - (btnSize + spacing) * 3 + spacing - padding, w - btnSize - spacing - padding, h - (btnSize + spacing) * 2 - padding);
        dashBtn.set(w - btnSize - padding, h - (btnSize + spacing) * 4 + spacing - padding, w - padding, h - (btnSize + spacing) * 3 - padding);
        
        ultimateBtn.set(w - 300 - padding, h - (btnSize + spacing) * 5 + spacing - padding, w - padding, h - (btnSize + spacing) * 4 - padding);
        
        trainerSkillsBtn.set(w - btnSize - padding, h - (btnSize + spacing) * 5 + spacing - padding, w - padding, h - (btnSize + spacing) * 4 - padding);

        minimapRect.set(w - 220 * k - padding, 70 * k, w - padding, 70 * k + 200 * k);
        zoomInBtn.set(minimapRect.left, minimapRect.bottom + 8 * k, minimapRect.left + 100 * k, minimapRect.bottom + 8 * k + 56 * k);
        zoomOutBtn.set(minimapRect.right - 100 * k, minimapRect.bottom + 8 * k, minimapRect.right, minimapRect.bottom + 8 * k + 56 * k);
        teleportBtn.set(minimapRect.left - 8 * k - 110 * k, minimapRect.top, minimapRect.left - 8 * k, minimapRect.top + 110 * k);
        friendsBtn.set(teleportBtn.left, teleportBtn.bottom + 34 * k, teleportBtn.right, teleportBtn.bottom + 34 * k + 90 * k);
        
        interactBtn.set(w/2f - 150, h - 120, w/2f + 150, h - 40);
        captureBtn.set(w/2f - 150, h - 220, w/2f + 150, h - 140);
        constructBuildingBtn.set(w/2f - 150, h - 220, w/2f + 150, h - 140);
        
        float healL = Math.max(405 * k, w / 2f - 150 * k - 80 * k - 16 * k);
        quickHealBtn.set(healL, h - 120 * k, healL + 80 * k, h - 40 * k);   // ao lado do botão de interagir, fora do joystick

        dialogueBox.set(100 * k, h * 0.62f, w - 100 * k, h * 0.86f);
    }

    private void updateFishingBarVisibility(GameView gv) {
        long now = System.currentTimeMillis();
        if (gv.fishing.isSelectingFishingSpot || gv.fishing.isFishing) {
            lastFishingInteractionTime = now;
            isFishingBarVisible = true;
        } else {
            if (gv.input.joystick.getIsPressed()) {
                if (now - lastFishingInteractionTime > 3000) {
                    isFishingBarVisible = false;
                }
            }
        }
    }

    private void drawHUD(Canvas canvas, Companion comp, Player p, GameView gv) {
        boolean hasBuildingAction = false;
        if (gv.world != null && gv.world.buildings != null) {
            for (Building b : gv.world.buildings) {
                if (b.isNearby(p)) {
                    hasBuildingAction = true;
                    break;
                }
            }
        }
        
        if (!hasBuildingAction) {
            drawPokeBallButton(canvas, p);
        }

        // Atalhos Rápidos
        drawIcon(canvas, quickHealBtn, "🧪");
        // pisca quando a vida está baixa e há poção: lembra da cura rápida
        if (gv.companion.getHp() > 0 && gv.companion.getHp() < gv.companion.getMaxHp() * 0.4f && gv.player.getInventory().getOrDefault("Poção", 0) > 0) {
            float pulse = 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() / 180.0);
            Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
            ring.setStyle(Paint.Style.STROKE); ring.setStrokeWidth(6);
            ring.setColor(Color.argb((int) (120 + 135 * pulse), 90, 230, 140));
            canvas.drawRoundRect(new RectF(quickHealBtn.left - 6 - 4 * pulse, quickHealBtn.top - 6 - 4 * pulse, quickHealBtn.right + 6 + 4 * pulse, quickHealBtn.bottom + 6 + 4 * pulse), 18, 18, ring);
            text(canvas, "x" + gv.player.getInventory().getOrDefault("Poção", 0), quickHealBtn.right - 8, quickHealBtn.bottom - 8, 18, Color.WHITE, Paint.Align.RIGHT, true);
        }
        drawIcon(canvas, trainerSkillsBtn, "🧙‍♂️");
        
        drawButton(canvas, zoomInBtn, "+");
        drawButton(canvas, zoomOutBtn, "-");
        
        final float ks = uiScale(canvas.getWidth(), canvas.getHeight());
        canvas.save(); canvas.scale(ks, ks);   // painel do treinador em coordenadas 1280x720
        RectF trainerPanel = new RectF(20, 20, 420, 285);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(180, 0, 0, 0));
        canvas.drawRoundRect(trainerPanel, 15, 15, paint);
        
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setColor(Color.WHITE); paint.setTextSize(22);
        canvas.drawText("👤 TREINADOR Nv: " + p.getLevel(), 40, 50, paint);
        
        drawStatBar(canvas, 40, 60, 360, 15, (float)p.getHp() / p.getMaxHp(), Color.RED, "HP: " + p.getHp());
        drawStatBar(canvas, 40, 85, 360, 15, p.getStamina() / p.getMaxStamina(), Color.rgb(255, 215, 0), "STAMINA");
        drawStatBar(canvas, 40, 110, 360, 15, (float)p.getExp() / com.example.pokeworld.entities.Progression.expToNext(p.getLevel()), Color.rgb(100, 255, 100), "XP");

        int nameColor = comp.getGender() == 0 ? Color.rgb(100, 150, 255) : Color.rgb(255, 150, 200);
        paint.setColor(nameColor);
        String genderIcon = comp.getGender() == 0 ? "♂" : "♀";
        canvas.drawText("🐾 " + comp.getSpecies().toUpperCase() + " " + genderIcon + " [Nv." + comp.getLevel() + "]", 40, 180, paint);
        
        drawStatBar(canvas, 40, 190, 360, 15, (float)comp.getHp() / comp.getMaxHp(), Color.RED, "HP: " + comp.getHp() + "/" + comp.getMaxHp());
        drawStatBar(canvas, 40, 215, 360, 15, (float)comp.getExp() / com.example.pokeworld.entities.Progression.expToNext(comp.getLevel()), Color.CYAN, "XP: " + comp.getExp());

        paint.setTextAlign(Paint.Align.LEFT);
        paint.setColor(Color.YELLOW); paint.setTextSize(20);
        canvas.drawText("🪙 " + p.getGold(), 40, 250, paint);
        paint.setColor(Color.rgb(255, 215, 0));
        canvas.drawText("💎 " + p.getGems(), 160, 250, paint);
        paint.setColor(Color.rgb(255, 100, 100));
        canvas.drawText("🔴 " + p.getInventory().getOrDefault("Pokébola", 0), 280, 250, paint);

        paint.setColor(Color.LTGRAY); paint.setTextSize(16);
        canvas.drawText(fit(comp.getPassiveDescription(), 360), 40, 275, paint);
        canvas.restore();

        canvas.save(); canvas.scale(ks, ks);   // missões ativas logo ABAIXO do painel (antes escreviam por cima dele)
        drawQuestLog(canvas, p, gv);
        canvas.restore();

        paint.setTextAlign(Paint.Align.RIGHT);
        paint.setColor(Color.argb(150, 0, 0, 0));
        paint.setColor(Color.WHITE); paint.setTextSize(20 * uiScale(canvas.getWidth(), canvas.getHeight()));
        canvas.drawText(gv.time.getTimeString() + (gv.time.isNight() ? " 🌙" : " ☀️"), canvas.getWidth() - 20, 50 * uiScale(canvas.getWidth(), canvas.getHeight()), paint);
        
        {   // evento do dia (bônus semanal), logo abaixo do relógio
            com.example.pokeworld.managers.EventCalendar.Event ev = com.example.pokeworld.managers.EventCalendar.today();
            float kk = uiScale(canvas.getWidth(), canvas.getHeight());
            paint.setColor(Color.rgb(255, 214, 120)); paint.setTextSize(15 * kk); paint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(ev.icon + " " + ev.name, canvas.getWidth() - 20, 70 * kk + 200 * kk + 56 * kk + 40 * kk, paint);
        }
        if (gv.dungeon.isInDungeon()) {
            paint.setColor(Color.WHITE); paint.setTextSize(30); paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("🏰 ANDAR: " + gv.dungeon.getCurrentFloor(), canvas.getWidth()/2f, 110, paint);
        }

        // --- TROCA RÁPIDA DE POKÉMON: linha horizontal no topo, perto do meio da tela ---
        float w = canvas.getWidth();
        List<CapturedCreature> party = p.getParty();
        swapSlots.clear();
        for (int i = 0; i < party.size(); i++) {
            CapturedCreature c = party.get(i);
            RectF slotRect = partySlot(w, canvas.getHeight(), i, party.size());
            swapSlots.add(slotRect);
            boolean isActive = c.name.equalsIgnoreCase(comp.getSpecies());

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(isActive ? Color.argb(235, 255, 200, 40) : Color.argb(170, 20, 30, 45));
            canvas.drawRoundRect(slotRect, 12, 12, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(isActive ? 3.5f : 1.5f);
            paint.setColor(isActive ? Color.WHITE : Color.argb(200, 120, 130, 150));
            canvas.drawRoundRect(slotRect, 12, 12, paint);

            Bitmap icon = SpriteManager.getInstance().getUnitSprite(c.name, 0, 0, c.isShiny);
            if (icon != null) {
                canvas.drawBitmap(icon, null, new Rect((int) slotRect.left + 4, (int) slotRect.top + 4, (int) slotRect.right - 4, (int) slotRect.bottom - 4), null);
            }
            if (com.example.pokeworld.managers.PartyManager.isFainted(gv, c)) {   // desmaiado: escurece e mostra a caveira
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(Color.argb(170, 0, 0, 0));
                canvas.drawRoundRect(slotRect, 12, 12, paint);
                paint.setColor(Color.WHITE); paint.setTextSize(36); paint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText("💀", slotRect.centerX(), slotRect.centerY() + 13, paint);
            }
            if (isActive) { // barra de vida do Pokémon ativo embaixo do slot
                float f = Math.max(0f, Math.min(1f, (float) comp.getHp() / Math.max(1, comp.getMaxHp())));
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(Color.argb(200, 0, 0, 0));
                canvas.drawRoundRect(new RectF(slotRect.left, slotRect.bottom + 3, slotRect.right, slotRect.bottom + 9), 3, 3, paint);
                paint.setColor(f > 0.5f ? Color.rgb(90, 220, 90) : f > 0.25f ? Color.rgb(240, 200, 60) : Color.rgb(230, 70, 60));
                canvas.drawRoundRect(new RectF(slotRect.left, slotRect.bottom + 3, slotRect.left + slotRect.width() * f, slotRect.bottom + 9), 3, 3, paint);
            }
        }
        paint.setStyle(Paint.Style.FILL);

        drawIcon(canvas, inventoryBtn, "🎒");
        drawIcon(canvas, achBtn, "🏆");
        drawIcon(canvas, runesBtn, "🔷");
        drawIcon(canvas, teleportBtn, "🧭");
        paint.setColor(Color.WHITE); paint.setTextSize(16 * uiScale(canvas.getWidth(), canvas.getHeight())); paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("MAPA", teleportBtn.centerX(), teleportBtn.bottom + 18 * uiScale(canvas.getWidth(), canvas.getHeight()), paint);
        drawIcon(canvas, friendsBtn, "👥");
        paint.setStyle(Paint.Style.FILL); paint.setColor(Color.WHITE); paint.setTextSize(16 * uiScale(canvas.getWidth(), canvas.getHeight())); paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("AMIGOS", friendsBtn.centerX(), friendsBtn.bottom + 18 * uiScale(canvas.getWidth(), canvas.getHeight()), paint);
        int pend = com.example.pokeworld.managers.FriendManager.getInstance().requestCount();
        if (pend > 0) { // bolinha vermelha com o número de pedidos pendentes
            float br = 16 * uiScale(canvas.getWidth(), canvas.getHeight());
            paint.setStyle(Paint.Style.FILL); paint.setColor(Color.rgb(220, 50, 50));
            canvas.drawCircle(friendsBtn.right - br * 0.3f, friendsBtn.top + br * 0.3f, br, paint);
            paint.setColor(Color.WHITE); paint.setTextSize(br * 1.2f); paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(String.valueOf(Math.min(pend, 9)), friendsBtn.right - br * 0.3f, friendsBtn.top + br * 0.3f + br * 0.42f, paint);
        }
        drawIcon(canvas, towerBtn, "🗼");
        
        drawIcon(canvas, creaturesBtn, "🐾");
        drawIcon(canvas, missionsBtn, "📜");
        drawIcon(canvas, bestiaryBtn, "📖");
        drawIcon(canvas, talentsBtn, "💡");
        
        String tacticIcon = comp.getCommand() == Companion.Command.AGGRESSIVE ? "⚔️" : 
                           comp.getCommand() == Companion.Command.DEFENSIVE ? "🛡️" : "🏃";
        drawIcon(canvas, tacticBtn, tacticIcon);
        paint.setColor(Color.WHITE); paint.setTextSize(18); paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("TÁTICA", tacticBtn.centerX(), tacticBtn.bottom + 20, paint);
        
        float dashCD = p.getDashCooldownProgress();
        drawCooldownIcon(canvas, dashBtn, "⚡", dashCD);
        
        drawIcon(canvas, fishingBtn, "🎣");
        drawIcon(canvas, settingsBtn, "⚙️");
        drawIcon(canvas, homeBtn, "🏰");
        
        if (gv.world.tileMap.getMapName().equals("HOME")) {
            drawIcon(canvas, buildBtn, "🔨");
            String raid = gv.world.defense != null ? gv.world.defense.hudText(System.currentTimeMillis()) : null;
            if (raid != null) {
                float rw = canvas.getWidth();
                paint.setColor(Color.argb(200, 150, 20, 20));
                canvas.drawRoundRect(new RectF(rw / 2f - 260, 150, rw / 2f + 260, 206), 18, 18, paint);
                paint.setColor(Color.WHITE); paint.setTextSize(28); paint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText(raid, rw / 2f, 188, paint);
            }
        }

        if (!p.getClan().equals("Nenhum")) {
            String clanIcon = p.getClan().equals("Rubi") ? "🔥" : p.getClan().equals("Safira") ? "💧" : "🌿";
            drawCooldownIcon(canvas, clanSkillBtn, clanIcon, p.getClanSkillProgress());
        }
        
        if (comp.getFieldAbility() == Companion.FieldAbility.FLY || 
            comp.getFieldAbility() == Companion.FieldAbility.SURF ||
            comp.getSpecies().equalsIgnoreCase("Lobo") || 
            comp.getSpecies().equalsIgnoreCase("Lobofogo")) {
            drawIcon(canvas, mountBtn, p.isMounted() ? "🏇" : "🐎");
        }

        if (comp.canUseUltimate()) {
            drawButton(canvas, ultimateBtn, "🌟 ULTIMATE");
        }

        boolean enemySelected = false;
        for (Enemy e : gv.world.enemies) if (e.isSelected() && !e.isPet) enemySelected = true;

        if (enemySelected) {
            drawSkills(canvas, comp);
        } else if (!gv.ui.isDialogueActive) {
            Building workInProgress = null;
            for (Building b : gv.world.buildings) {
                if (b.isNearby(p)) {
                    if (!b.isConstructed()) {
                        workInProgress = b;
                        break;
                    } 
                }
            }

            if (workInProgress != null) {
                int madeiraReq = workInProgress.getName().equalsIgnoreCase("Oficina") ? 5 : 20;
                int ferroReq = workInProgress.getName().equalsIgnoreCase("Oficina") ? 2 : 10;
                String label = "CONSTRUIR " + workInProgress.getName().toUpperCase() + 
                               "\n(" + madeiraReq + " 🪵, " + ferroReq + " ⛓️)";
                drawButtonWithMultine(canvas, constructBuildingBtn, label);
            } 

            for (NPC n : gv.world.npcs) {
                if (n.isNearby(p)) {
                    drawButton(canvas, interactBtn, "FALAR (" + n.getName() + ")");
                    break;
                }
            }
        }
    }

    private void drawPokeBallButton(Canvas canvas, Player p) {
        android.graphics.Bitmap ballIcon = SpriteManager.getInstance().getMiscSprite("pokebola_item");
        if (ballIcon != null) {
            paint.setColor(Color.argb(180, 0, 0, 0));
            canvas.drawRoundRect(captureBtn, 15, 15, paint);
            
            int r = 40;
            canvas.drawBitmap(ballIcon, null, new android.graphics.Rect((int)captureBtn.centerX() - r, (int)captureBtn.centerY() - r, (int)captureBtn.centerX() + r, (int)captureBtn.centerY() + r), null);
            
            paint.setColor(Color.WHITE); paint.setTextSize(24); paint.setTextAlign(Paint.Align.RIGHT);
            int count = p.getInventory().getOrDefault("Pokébola", 0);
            canvas.drawText("x" + count, captureBtn.right - 10, captureBtn.bottom - 10, paint);
        }
    }

    private void drawDraggingBall(Canvas canvas) {
        android.graphics.Bitmap ballIcon = SpriteManager.getInstance().getMiscSprite("pokebola_item");
        if (ballIcon != null) {
            int r = 50;
            canvas.drawBitmap(ballIcon, null, new android.graphics.Rect((int)dragBallX - r, (int)dragBallY - r, (int)dragBallX + r, (int)dragBallY + r), null);
        }
    }

    private void drawStatBar(Canvas canvas, float x, float y, float w, float h, float progress, int color, String label) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.DKGRAY);
        canvas.drawRect(x, y, x + w, y + h, paint);
        paint.setColor(color);
        canvas.drawRect(x, y, x + (w * Math.max(0, Math.min(1.0f, progress))), y + h, paint);
        
        paint.setColor(Color.WHITE);
        paint.setTextSize(h * 0.9f);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(label, x + w/2, y + h * 0.85f, paint);
    }

    private void drawQuestLog(Canvas canvas, Player p, GameView gv) {
        java.util.List<com.example.pokeworld.managers.MissionManager.Quest> qs = gv.missions.activeQuests();
        if (qs.isEmpty()) return;
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTextSize(18);
        int y = 315, shown = 0;
        for (com.example.pokeworld.managers.MissionManager.Quest q : qs) {
            if (shown++ >= 3) break;
            int prog = Math.min(q.target, gv.missions.progress(q, gv));
            paint.setColor(Color.argb(210, 0, 0, 0));
            canvas.drawText("🎯 " + q.title + " (" + prog + "/" + q.target + ")", 31, y + 1, paint);
            paint.setColor(Color.rgb(255, 235, 150));
            canvas.drawText("🎯 " + q.title + " (" + prog + "/" + q.target + ")", 30, y, paint);
            y += 25;
        }
    }

    private void drawMinimap(Canvas canvas, GameView gv) {
        paint.setColor(Color.argb(150, 0, 0, 0));
        canvas.drawRect(minimapRect, paint);
        paint.setColor(Color.WHITE); paint.setStyle(Paint.Style.STROKE);
        canvas.drawRect(minimapRect, paint); paint.setStyle(Paint.Style.FILL);
        float mapWidth = gv.world.tileMap.getCols() * TileMap.TILE_SIZE;
        float mapHeight = gv.world.tileMap.getRows() * TileMap.TILE_SIZE;
        float scaleX = minimapRect.width() / mapWidth;
        float scaleY = minimapRect.height() / mapHeight;
        paint.setColor(Color.WHITE);
        canvas.drawCircle(minimapRect.left + (float)gv.player.getPositionX() * scaleX,
                minimapRect.top + (float)gv.player.getPositionY() * scaleY, 4, paint);
        for (Enemy e : gv.world.enemies) {
            if (e.isAlive()) {
                if (e.isPet) paint.setColor(Color.BLUE);
                else paint.setColor(e.isBoss ? Color.YELLOW : Color.RED);
                
                canvas.drawCircle(minimapRect.left + (float)e.getPositionX() * scaleX,
                        minimapRect.top + (float)e.getPositionY() * scaleY, 3, paint);
            }
        }
        paint.setColor(Color.BLUE);
        for (NPC n : gv.world.npcs) {
            canvas.drawRect(minimapRect.left + (float)n.getPositionX() * scaleX - 2,
                    minimapRect.top + (float)n.getPositionY() * scaleY - 2,
                    minimapRect.left + (float)n.getPositionX() * scaleX + 2,
                    minimapRect.top + (float)n.getPositionY() * scaleY + 2, paint);
        }

        // (Removido o desenho de construções no minimap conforme pedido)
        
        paint.setColor(Color.YELLOW);
        TileMap tm = gv.world.tileMap;
        for (int r = 0; r < tm.getRows(); r++) {
            for (int c = 0; c < tm.getCols(); c++) {
                if (tm.getTileType(c * TileMap.TILE_SIZE, r * TileMap.TILE_SIZE) == TileMap.PORTAL) {
                    canvas.drawRect(minimapRect.left + c * TileMap.TILE_SIZE * scaleX - 1,
                            minimapRect.top + r * TileMap.TILE_SIZE * scaleY - 1,
                            minimapRect.left + c * TileMap.TILE_SIZE * scaleX + 1,
                            minimapRect.top + r * TileMap.TILE_SIZE * scaleY + 1, paint);
                }
            }
        }
    }

    private void drawBossBar(Canvas canvas, WorldManager world) {
        Enemy boss = null;
        for (Enemy e : world.enemies) {
            if (e.isBoss && e.isAlive()) { boss = e; break; }
        }
        if (boss != null) {
            float barW = 600;
            float x = (canvas.getWidth() - barW) / 2;
            float y = 80;
            paint.setColor(Color.BLACK);
            canvas.drawRect(x - 5, y - 5, x + barW + 5, y + 30, paint);
            paint.setColor(boss.hp < boss.maxHp * 0.2 ? Color.rgb(255, 0, 0) : Color.RED);
            canvas.drawRect(x, y, x + (barW * (float)boss.hp / boss.maxHp), y + 25, paint);
            paint.setColor(Color.WHITE); paint.setTextSize(25); paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(boss.name.toUpperCase(), canvas.getWidth() / 2f, y + 22, paint);
        }
    }

    private void drawFishingBar(Canvas canvas, Player p) {
        float w = canvas.getWidth();
        float barW = 400;
        float barH = 50;
        float x = (w - barW) / 2;
        float y = 80;

        RectF bg = new RectF(x - 10, y - 40, x + barW + 10, y + barH + 10);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(150, 0, 0, 0));
        canvas.drawRoundRect(bg, 15, 15, paint);
        
        paint.setColor(Color.WHITE);
        paint.setTextSize(20);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("PROFICIÊNCIA EM PESCA: NV." + p.getFishingLevel(), x + barW/2, y - 10, paint);

        paint.setColor(Color.DKGRAY);
        canvas.drawRect(x, y, x + barW, y + 15, paint);
        paint.setColor(Color.argb(255, 100, 200, 255));
        float progress = (float)p.getFishingExp() / (p.getFishingLevel() * 50);
        canvas.drawRect(x, y, x + (barW * progress), y + 15, paint);
    }

    private void drawDialogueBox(Canvas canvas, String speaker, String dialogue) {
        paint.setColor(Color.argb(230, 0, 0, 0));
        canvas.drawRoundRect(dialogueBox, 20, 20, paint);
        paint.setColor(Color.CYAN); paint.setTextSize(30); paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(speaker + ":", dialogueBox.left + 40, dialogueBox.top + 50, paint);
        paint.setColor(Color.WHITE); paint.setTextSize(25);
        canvas.drawText(dialogue, dialogueBox.left + 40, dialogueBox.top + 100, paint);
        paint.setTextSize(18); paint.setColor(Color.LTGRAY); paint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("Toque para continuar...", dialogueBox.right - 40, dialogueBox.bottom - 20, paint);
    }

    private void drawMessageLog(Canvas canvas, List<String> messageLog) {
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTextSize(20);
        int y = canvas.getHeight() - 150;
        for (int i = 0; i < messageLog.size(); i++) {
            paint.setColor(Color.argb(255 - (i * 40), 255, 255, 255));
            canvas.drawText("> " + messageLog.get(i), 50, y, paint);
            y -= 30;
        }
    }

    private void drawSkills(Canvas canvas, Companion comp) {
        skillButtons.clear();
        int n = comp.getSkills().size();
        if (n == 0) return;
        float xStart = 440, gap = 8;
        // com mais golpes os botões encolhem para caber na tela
        float bw = Math.min(120f, (canvas.getWidth() - xStart - 20 - gap * (n - 1)) / n);
        for (int i = 0; i < n; i++) {
            Skill s = comp.getSkills().get(i);
            RectF btn = new RectF(xStart, 470, xStart + bw, 540);
            skillButtons.add(btn);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(s.isReady() ? 200 : 100, 0, 0, 0));
            canvas.drawRoundRect(btn, 10, 10, paint);
            paint.setColor(s.getColor());
            canvas.drawRect(btn.left, btn.bottom - 5, btn.left + (btn.width() * s.getCooldownProgress()), btn.bottom, paint);
            // marca dos golpes em área
            if (s.isArea()) { paint.setTextSize(14); paint.setTextAlign(Paint.Align.RIGHT); canvas.drawText("◎", btn.right - 6, btn.top + 18, paint); }
            paint.setColor(Color.WHITE); paint.setTextSize(bw < 100 ? 15 : 18); paint.setTextAlign(Paint.Align.CENTER);
            String label = s.getName();
            while (label.length() > 3 && paint.measureText(label) > bw - 8) label = label.substring(0, label.length() - 2) + "…";
            canvas.drawText(label, btn.centerX(), btn.centerY() + 8, paint);
            xStart += bw + gap;
        }
    }
}
