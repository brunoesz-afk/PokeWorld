package com.example.pokeworld.ui.input;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.entities.Companion;
import com.example.pokeworld.entities.Enemy;
import com.example.pokeworld.entities.NPC;
import com.example.pokeworld.ui.UIManager;
import com.example.pokeworld.ui.handlers.ExploringUI;
import com.example.pokeworld.world.Building;
import com.example.pokeworld.world.TileMap;

public class ExploringInput implements InputExtension {
    @Override
    public boolean handleEvent(android.view.MotionEvent event, GameView gv) {
        ExploringUI ui = (ExploringUI) gv.ui.getHandler(UIManager.UIState.EXPLORING);
        int action = event.getActionMasked();
        float x = event.getX();
        float y = event.getY();

        switch (action) {
            case android.view.MotionEvent.ACTION_DOWN:
                // Verifica toque na linha de troca rápida do Time (topo da tela)
                java.util.List<com.example.pokeworld.entities.CapturedCreature> party = gv.player.getParty();
                for (int i = 0; i < party.size(); i++) {
                    android.graphics.RectF slotRect = ExploringUI.partySlot(gv.getWidth(), i, party.size());
                    if (slotRect.contains(x, y)) {
                        com.example.pokeworld.entities.CapturedCreature c = party.get(i);
                        gv.companion.setSpecies(c.name);
                        gv.activity.dbHelper.updateActiveSpecies(c.name);
                        gv.ui.showMessage("🔄 Pokémon ativo alterado para: " + c.name);
                        return true;
                    }
                }

                // Verifica se há alguma ação de construção próxima
                boolean hasBuildingAction = false;
                if (gv.world != null && gv.world.buildings != null) {
                    for (Building b : gv.world.buildings) {
                        if (b.isNearby(gv.player)) {
                            hasBuildingAction = true;
                            break;
                        }
                    }
                }
                
                // Se o botão de construir estiver ativo na mesma área, ignora o clique da Pokébola
                if (ui.captureBtn.contains(x, y) && !hasBuildingAction) {
                    Integer balls = gv.player.getInventory().getOrDefault("Pokébola", 0);
                    if (balls != null && balls > 0) {
                        ui.isDraggingBall = true;
                        ui.dragBallX = x;
                        ui.dragBallY = y;
                        return true;
                    } else {
                        gv.ui.showMessage("❌ Sem Pokébolas!");
                    }
                }
                break;
            case android.view.MotionEvent.ACTION_MOVE:
                if (ui.isDraggingBall) {
                    ui.dragBallX = x;
                    ui.dragBallY = y;
                    return true;
                }
                break;
            case android.view.MotionEvent.ACTION_UP:
                if (ui.isDraggingBall) {
                    ui.isDraggingBall = false;
                    handleBallDrop(x, y, gv);
                    return true;
                }
                break;
        }
        return false;
    }

    private void handleBallDrop(float x, float y, GameView gv) {
        float worldX = getWorldX(x, gv);
        float worldY = getWorldY(y, gv);
        
        com.example.pokeworld.entities.Loot target = null;
        for (com.example.pokeworld.entities.Loot l : gv.world.loots) {
            if (l.getType() == com.example.pokeworld.entities.Loot.Type.DEFEATED) {
                double dist = Math.sqrt(Math.pow(l.getPositionX() - worldX, 2) + Math.pow(l.getPositionY() - worldY, 2));
                if (dist < 110) { // Hitbox ajustada para o tamanho moderado do corpo
                    target = l;
                    break;
                }
            }
        }
        
        if (target != null) {
            gv.combat.attemptCaptureDefeated(target);
        } else {
            gv.ui.showMessage("Arraste até um Pokémon derrotado!");
        }
    }

    @Override
    public boolean handle(float x, float y, GameView gv) {
        if (!handleButtons(x, y, gv)) {
            if (gv.fishing.isSelectingFishingSpot) {
                handleFishingSelection(x, y, gv);
            } else {
                checkSelection(x, y, gv);
            }
        }
        return true;
    }

    private boolean handleButtons(float x, float y, GameView gv) {
        ExploringUI ui = (ExploringUI) gv.ui.getHandler(UIManager.UIState.EXPLORING);
        
        if (ui.quickHealBtn.contains(x, y)) {
            if (gv.player.getInventory().containsKey("Poção")) {
                gv.player.useItem("Poção");
                gv.companion.healFull();
                gv.ui.showMessage("🧪 PET CURADO!");
                return true;
            }
        }
        if (ui.quickEatBtn.contains(x, y)) {
            if (gv.player.getInventory().containsKey("Doce de Fruta")) {
                gv.player.useItem("Doce de Fruta");
                gv.player.eat(40);
                gv.ui.showMessage("🍲 FOME RECUPERADA!");
                return true;
            }
        }

        if (ui.inventoryBtn.contains(x, y)) { gv.ui.state = UIManager.UIState.INVENTORY; return true; }
        
        // Atalho rápido para comer
        if (gv.player.getHunger() < 50) {
            if (gv.player.getInventory().containsKey("Doce de Fruta")) {
                gv.player.useItem("Doce de Fruta");
                gv.player.eat(40);
                gv.ui.showMessage("😋 Comeu Doce! +40 Fome");
                return true;
            } else if (gv.player.getInventory().containsKey("Fruta")) {
                gv.player.useItem("Fruta");
                gv.player.eat(15);
                gv.ui.showMessage("🍎 Comeu Fruta! +15 Fome");
                return true;
            }
        }

        if (ui.creaturesBtn.contains(x, y)) { gv.ui.state = UIManager.UIState.CREATURES; return true; }
        if (ui.missionsBtn.contains(x, y)) { gv.ui.state = UIManager.UIState.MISSIONS; return true; }
        if (ui.bestiaryBtn.contains(x, y)) { gv.ui.state = UIManager.UIState.BESTIARY; return true; }
        if (ui.talentsBtn.contains(x, y)) { gv.ui.state = UIManager.UIState.TALENTS; return true; }
        if (ui.achBtn.contains(x, y)) { gv.ui.state = UIManager.UIState.ACHIEVEMENTS; return true; }
        if (ui.runesBtn.contains(x, y)) { gv.ui.state = UIManager.UIState.RUNES; return true; }
        if (ui.settingsBtn.contains(x, y)) { gv.ui.state = UIManager.UIState.SETTINGS; return true; }
        if (ui.trainerSkillsBtn.contains(x, y)) { gv.ui.state = UIManager.UIState.TRAINER_SKILLS; return true; }
        
        if (ui.zoomInBtn.contains(x, y)) { gv.world.changeZoom(0.1f); return true; }
        if (ui.zoomOutBtn.contains(x, y)) { gv.world.changeZoom(-0.1f); return true; }

        if (ui.homeBtn.contains(x, y)) {
            String currentMap = gv.world.tileMap.getMapName();
            if (currentMap.equals("HOME")) {
                gv.world.loadMap("CITY", gv.player, gv.companion, "CENTER");
                gv.ui.showMessage("Voltando para a Cidade...");
            } else {
                gv.world.loadMap("HOME", gv.player, gv.companion, "CENTER");
                gv.ui.showMessage("Viajando para Minha Base...");
            }
            return true;
        }
        if (ui.buildBtn.contains(x, y) && gv.world.tileMap.getMapName().equals("HOME")) {
            if (gv.world.defense != null && gv.world.defense.isActive()) {
                gv.ui.showMessage("⚔️ Termine a invasão antes de construir!");
            } else {
                gv.ui.state = UIManager.UIState.BUILD;
            }
            return true;
        }
        if (ui.teleportBtn.contains(x, y)) {
            if (gv.tutorial != null && gv.tutorial.getCurrentState() == com.example.pokeworld.managers.TutorialManager.State.CLICK_COMPASS_TUTORIAL) {
                gv.tutorial.onCompassClicked();
                return true;
            }
            gv.ui.state = UIManager.UIState.TELEPORT;
            return true;
        }
        if (ui.towerBtn.contains(x, y)) {
            gv.ui.state = UIManager.UIState.TOWER_SELECT;
            return true;
        }
        
        // Pesca
        if (ui.fishingBtn.contains(x, y)) {
            gv.fishing.handleAction(gv.missions);
            return true;
        }

        // Troca de Comando Tático
        if (ui.tacticBtn.contains(x, y)) {
            Companion.Command next = gv.companion.getCommand() == Companion.Command.DEFENSIVE ? Companion.Command.AGGRESSIVE :
                                    gv.companion.getCommand() == Companion.Command.AGGRESSIVE ? Companion.Command.PASSIVE : Companion.Command.DEFENSIVE;
            gv.companion.setCommand(next);
            gv.ui.showMessage("TÁTICA: " + next.name());
            return true;
        }
        
        if (ui.dashBtn.contains(x, y)) {
            gv.player.dash();
            gv.vibration.dashVibrate();
            gv.particles.addExplosion(gv.player.getPositionX(), gv.player.getPositionY(), android.graphics.Color.WHITE, 5);
            return true;
        }

        if (ui.clanSkillBtn.contains(x, y)) {
            if (gv.player.useClanSkill()) {
                ClanInput.executeClanSkill(gv);
                return true;
            }
        }

        if (gv.companion.canUseUltimate() && ui.ultimateBtn.contains(x, y)) {
            gv.companion.useUltimate(gv.effects, gv.particles);
            gv.vibration.ultimateVibrate();
            gv.startShake(30, 15);
            return true;
        }

        if (ui.mountBtn.contains(x, y)) {
            String species = gv.companion.getSpecies();
            Companion.FieldAbility ability = gv.companion.getFieldAbility();
            if (ability == Companion.FieldAbility.FLY || ability == Companion.FieldAbility.SURF ||
                species.equalsIgnoreCase("Lobo") || species.equalsIgnoreCase("Lobofogo")) {
                gv.player.setMounted(!gv.player.isMounted());
                gv.ui.showMessage(gv.player.isMounted() ? "🏇 Montado!" : "👣 Desmontado");
                return true;
            }
        }

        if (ui.interactBtn.contains(x, y) && !gv.ui.isDialogueActive) {
            // Habilidade de Campo: FLASH
            if (gv.world.isMapDark() && gv.companion.getFieldAbility() == Companion.FieldAbility.FLASH) {
                gv.world.useFlash();
                gv.ui.showMessage("🔦 " + gv.companion.getSpecies().toUpperCase() + " ILUMINOU TUDO!");
                return true;
            }

            for (NPC n : gv.world.npcs) {
                if (n.isNearby(gv.player)) {
                    if (n.getName().contains("Time") || n.getName().contains("Base") || n.getName().contains("Estoque")) {
                        gv.ui.state = UIManager.UIState.STORAGE;
                        return true;
                    }
                    if (n.getName().contains("Torre")) {
                        if (gv.tower.isWaitingForNpcAdvance()) {
                            gv.tower.advanceFloor();
                        } else {
                            gv.tower.startTower();
                        }
                        return true;
                    }
                    gv.ui.isDialogueActive = true;
                    gv.ui.currentSpeaker = n.getName();
                    gv.ui.currentDialogue = n.getDialogue();
                    if (n.getType() == NPC.Type.HEALER) gv.companion.healFull();
                    gv.missions.checkRewards(n);
                    return true;
                }
            }
        }

        for (int i = 0; i < ui.skillButtons.size(); i++) {
            if (ui.skillButtons.get(i).contains(x, y)) {
                gv.companion.useSkill(i, gv.effects, gv.particles, gv.weather.getCurrentType(), gv.world);
                return true;
            }
        }

        float worldX = getWorldX(x, gv);
        float worldY = getWorldY(y, gv);
        int tile = gv.world.tileMap.getTileType(worldX, worldY);

        // Hortas da base: toque para plantar / colher
        if (gv.world.base != null) {
            com.example.pokeworld.world.BasePiece bp = gv.world.base.pieceAt((int) Math.floor(worldX / TileMap.TILE_SIZE), (int) Math.floor(worldY / TileMap.TILE_SIZE));
            if (bp != null && bp.kind == com.example.pokeworld.world.BaseKind.HORTA) {
                double dx = gv.player.getPositionX() - bp.centerX(), dy = gv.player.getPositionY() - bp.centerY();
                if (Math.sqrt(dx * dx + dy * dy) > 420) { gv.ui.showMessage("🚶 Chegue mais perto da horta"); return true; }
                com.example.pokeworld.world.BaseManager.CropResult cr = gv.world.base.tendCrop(bp, new com.example.pokeworld.world.PlayerWallet(gv.player), System.currentTimeMillis());
                if (cr != null) {
                    gv.ui.showMessage(cr.message);
                    if (cr.planted) gv.particles.addExplosion(bp.centerX(), bp.centerY(), android.graphics.Color.rgb(139, 69, 19), 14);
                    if (cr.harvested) {
                        gv.particles.addExplosion(bp.centerX(), bp.centerY(), android.graphics.Color.RED, 25);
                        gv.activity.saveCurrentGame();
                    }
                    return true;
                }
            }
        }

        if (tile == TileMap.SOIL) {
            handleFarmingInteraction(x, y, gv);
            return true;
        }
        if (tile == TileMap.BERRY_TREE) {
            handleBerryTreeInteraction(x, y, gv);
            return true;
        }
        if (tile == TileMap.BUSH) {
            gv.world.tileMap.setTileType(worldX, worldY, TileMap.GRASS);
            handleBushReward(x, y, gv);
            return true;
        }
        if (tile == TileMap.SIGN) {
            handleSignInteraction(x, y, gv);
            return true;
        }
        if (tile == TileMap.TREE && (gv.companion.getFieldAbility() == Companion.FieldAbility.CUT || gv.companion.getType1() == com.example.pokeworld.entities.ElementalType.PLANT || (gv.companion.getType2() != null && gv.companion.getType2() == com.example.pokeworld.entities.ElementalType.PLANT))) {
            gv.world.tileMap.setTileType(worldX, worldY, TileMap.GRASS);
            int axeLv = gv.player.getInventory().getOrDefault("Machado_Lv", 1);
            for (int i = 0; i < axeLv; i++) {
                gv.player.addItem("Madeira");
                gv.effects.addText("🪵", worldX + (Math.random() - 0.5) * 40, worldY - i * 20, android.graphics.Color.WHITE);
            }
            gv.ui.showMessage("🪓 Árvore cortada! +" + axeLv + " Madeira 🪵");
            gv.particles.addExplosion(worldX, worldY, android.graphics.Color.rgb(100, 70, 30), 10);
            return true;
        }
        if (tile == TileMap.ROCK && (gv.companion.getFieldAbility() == Companion.FieldAbility.SMASH || gv.companion.getType1() == com.example.pokeworld.entities.ElementalType.ROCK || (gv.companion.getType2() != null && gv.companion.getType2() == com.example.pokeworld.entities.ElementalType.ROCK))) {
            gv.world.tileMap.setTileType(worldX, worldY, TileMap.FLOOR);
            int pickaxeLv = gv.player.getInventory().getOrDefault("Picareta_Lv", 1);
            for (int i = 0; i < pickaxeLv; i++) {
                gv.player.addItem("Ferro");
                gv.effects.addText("⛓️", worldX + (Math.random() - 0.5) * 40, worldY - i * 20, android.graphics.Color.WHITE);
            }
            gv.ui.showMessage("💥 Pedra quebrada! +" + pickaxeLv + " Ferro ⛓️");
            gv.particles.addExplosion(worldX, worldY, android.graphics.Color.GRAY, 15);
            return true;
        }

        for (Building b : gv.world.buildings) {
            if (b.isTouched(x, y)) {
                handleBuildingUpgrade(b, gv);
                return true;
            }
        }

        if (ui.constructBuildingBtn.contains(x, y)) {
            // Verifica se há algo para construir antes de retornar true
            Building unconstructed = null;
            Building constructed = null;
            for (Building b : gv.world.buildings) {
                if (b.isNearby(gv.player)) {
                    if (!b.isConstructed()) {
                        unconstructed = b;
                        break;
                    } else if (b.getName().equalsIgnoreCase("Oficina")) {
                        constructed = b;
                    } else if (b.getName().equalsIgnoreCase("Fabrica de Montaria")) {
                        gv.ui.state = UIManager.UIState.MOUNT_FACTORY;
                        return true;
                    } else if (b.getName().equalsIgnoreCase("Ginásio")) {
                        gv.ui.state = UIManager.UIState.GYM;
                        return true;
                    } else if (b.getName().equalsIgnoreCase("Baú")) {
                        gv.ui.state = UIManager.UIState.ITEM_STORAGE;
                        return true;
                    } else if (b.getName().equalsIgnoreCase("Mural de Caça")) {
                        gv.ui.state = UIManager.UIState.BOUNTIES;
                        return true;
                    } else if (b.getName().equalsIgnoreCase("Academia")) {
                        gv.ui.state = UIManager.UIState.STORAGE;
                        return true;
                    }
                }
            }
            
            if (unconstructed != null) {
                handleConstructionAction(gv);
                return true;
            } else if (constructed != null) {
                gv.ui.state = UIManager.UIState.CRAFTING;
                return true;
            }
        }

        // Caso especial para pesca na floresta usando a mesma área do botão se nada mais estiver ativo
        if (ui.captureBtn.contains(x, y) && gv.world.tileMap.getMapName().equals("FOREST") && x > 900) {
            gv.fishing.handleAction(gv.missions);
            return true;
        }

        return false;
    }

    private void handleConstructionAction(GameView gv) {
        for (Building b : gv.world.buildings) {
            if (!b.isConstructed() && b.isNearby(gv.player)) {
                // Custos por tipo de construção
                int madeiraReq = 20;
                int ferroReq = 10;
                
                if (b.getName().equalsIgnoreCase("Oficina")) {
                    madeiraReq = 5; ferroReq = 2;
                } else if (b.getName().equalsIgnoreCase("Daycare")) {
                    madeiraReq = 15; ferroReq = 5;
                } else if (b.getName().equalsIgnoreCase("Laboratorio")) {
                    madeiraReq = 30; ferroReq = 15;
                }

                int madeira = gv.player.getInventory().getOrDefault("Madeira", 0);
                int ferro = gv.player.getInventory().getOrDefault("Ferro", 0);

                if (madeira >= madeiraReq && ferro >= ferroReq) {
                    for (int i = 0; i < madeiraReq; i++) gv.player.useItem("Madeira");
                    for (int i = 0; i < ferroReq; i++) gv.player.useItem("Ferro");
                    
                    long duration = 30000;
                    if (b.getName().equalsIgnoreCase("Oficina")) duration = 5000; // 5 segundos de construção
                    else if (b.getName().equalsIgnoreCase("Laboratorio")) duration = 60000;
                    
                    b.setConstructionFinishTime(System.currentTimeMillis() + duration);
                    gv.activity.dbHelper.updateBuilding(b.getName(), b.getLevel(), 0, b.getPositionX(), b.getPositionY(), b.getConstructionFinishTime());
                    gv.ui.showMessage("🏗️ CONSTRUÇÃO INICIADA!");
                    
                    gv.particles.addExplosion(b.getCenterX(), b.getCenterY(), android.graphics.Color.WHITE, 30);
                    gv.particles.addExplosion(b.getCenterX(), b.getCenterY(), android.graphics.Color.YELLOW, 40);
                    
                    // Atualiza o progresso da missão de construção
                    gv.missions.updateProgress(b.getName());
                    
                    gv.activity.saveCurrentGame();
                } else {
                    gv.ui.showMessage("Faltam recursos! (" + madeiraReq + " Madeira, " + ferroReq + " Ferro)");
                }
                break;
            }
        }
    }

    private void handleBerryTreeInteraction(float x, float y, GameView gv) {
        gv.player.addItem("Fruta");
        gv.ui.showMessage("🍎 Colheu uma Fruta da árvore!");
        
        float worldX = x + gv.world.getCameraX();
        float worldY = y + gv.world.getCameraY();
        gv.particles.addExplosion(worldX, worldY, android.graphics.Color.RED, 15);

        if (gv.world.tileMap.getMapName().equals("HOME")) {
            gv.world.tileMap.setTileType(worldX, worldY, TileMap.SOIL);
        } else {
            gv.world.tileMap.setTileType(worldX, worldY, TileMap.TREE);
        }
    }

    private void handleFarmingInteraction(float x, float y, GameView gv) {
        float worldX = x + gv.world.getCameraX();
        float worldY = y + gv.world.getCameraY();
        int col = (int) (worldX / TileMap.TILE_SIZE);
        int row = (int) (worldY / TileMap.TILE_SIZE);

        gv.farming.plant(row, col);
        gv.particles.addExplosion(worldX, worldY, android.graphics.Color.rgb(139, 69, 19), 10);
        gv.ui.showMessage("🌱 Semente plantada! Vai crescer em 60s.");
    }

    private void handleSignInteraction(float x, float y, GameView gv) {
        String mapName = gv.world.tileMap.getMapName();
        String message = "Uma placa velha e desgastada.";
        
        if (mapName.equals("CITY")) message = "BEM-VINDO À CIDADE POKEWORLD! <- Floresta | Montanha ^ | Rio ->";
        else if (mapName.equals("FOREST")) message = "CUIDADO: Pokémons selvagens à espreita. A cidade fica ao Leste.";
        
        gv.ui.isDialogueActive = true;
        gv.ui.currentSpeaker = "PLACA";
        gv.ui.currentDialogue = message;
    }

    private void handleBushReward(float x, float y, GameView gv) {
        gv.particles.addExplosion(x, y, android.graphics.Color.GREEN, 10);
        double chance = Math.random();
        if (chance < 0.3) {
            gv.player.addGold(15);
            gv.ui.showMessage("+15 🪙 (Arbusto)");
        } else if (chance < 0.45) {
            gv.player.addItem("Poção");
            gv.ui.showMessage("+1 Poção (Arbusto)");
        } else if (chance < 0.6) {
            gv.player.addItem("Fruta");
            gv.ui.showMessage("+1 Fruta 🍎 (Arbusto)");
        } else if (chance < 0.65) {
            gv.player.addGems(1);
            gv.ui.showMessage("✨ ENCONTROU 1 GEMA NO ARBUSTO! ✨");
            gv.particles.addExplosion(x, y, android.graphics.Color.YELLOW, 20);
        }
    }

    private void handleBuildingUpgrade(Building b, GameView gv) {
        if (gv.player.getGold() >= b.getUpgradeCost()) {
            gv.player.addGold(-b.getUpgradeCost());
            b.upgrade();
            gv.activity.dbHelper.updateBuilding(b.getName(), b.getLevel(), b.isConstructed() ? 1 : 0, b.getPositionX(), b.getPositionY(), b.getConstructionFinishTime());
            gv.applyKingdomBonuses();
            gv.ui.showMessage(b.getName() + " LV." + b.getLevel());
        }
    }

    private void handleFishingSelection(float x, float y, GameView gv) {
        float worldX = x + gv.world.getCameraX();
        float worldY = y + gv.world.getCameraY();
        int tile = gv.world.tileMap.getTileType(worldX, worldY);

        if (tile == TileMap.WATER || tile == TileMap.DEEP_WATER) {
            double dist = Math.sqrt(Math.pow(gv.player.getPositionX() - worldX, 2) + Math.pow(gv.player.getPositionY() - worldY, 2));
            if (dist < 300) {
                gv.fishing.startFishing();
            } else {
                gv.ui.showMessage("Muito longe da água!");
                gv.fishing.cancelSelection();
            }
        } else {
            gv.ui.showMessage("Isso não é água!");
            gv.fishing.cancelSelection();
        }
    }

    private void checkSelection(float x, float y, GameView gv) {
        float worldX = getWorldX(x, gv);
        float worldY = getWorldY(y, gv);

        // Se estiver na base, permite mover pets selecionados
        if (gv.world.tileMap.getMapName().equals("HOME")) {
            for (Enemy e : gv.world.enemies) {
                if (e.isPet && e.isSelected()) {
                    // Se clicou em outro lugar (não no pet), move ele
                    if (!e.isTouched(worldX, worldY)) {
                        e.startX = worldX;
                        e.startY = worldY;
                        gv.ui.showMessage("🏠 Pet movido!");
                        e.setSelected(false);
                        return;
                    }
                }
            }
        }

        for (Enemy e : gv.world.enemies) {
            if (e.isTouched(worldX, worldY)) {
                for (Enemy o : gv.world.enemies) o.setSelected(false);
                e.setSelected(true);
                if (e.isPet) {
                    gv.companion.setTargetEnemy(null);
                    gv.ui.showMessage("🛡️ " + e.name + " (Pokémon Guardião da Base)");
                } else {
                    gv.companion.setTargetEnemy(e);
                    gv.ui.showMessage("🎯 Alvo: " + e.name + " (Atacando!)");
                }
                return;
            }
        }
        for (Enemy e : gv.world.enemies) e.setSelected(false);
    }

    private float getWorldX(float x, GameView gv) {
        float sw = gv.getWidth();
        float zoom = gv.world.getCurrentZoom();
        return (float) gv.player.getPositionX() + (x - sw / 2f) / zoom;
    }

    private float getWorldY(float y, GameView gv) {
        float sh = gv.getHeight();
        float zoom = gv.world.getCurrentZoom();
        return (float) gv.player.getPositionY() + (y - sh / 2f) / zoom;
    }
}
