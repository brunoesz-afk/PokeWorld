package com.example.pokeworld.managers;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.entities.CapturedCreature;
import com.example.pokeworld.entities.Companion;
import com.example.pokeworld.entities.Skill;
import com.example.pokeworld.entities.TeamBonus;
import com.example.pokeworld.entities.Equipment;
import com.example.pokeworld.entities.PetData;
import com.example.pokeworld.entities.Rune;

/** Troca do Pokémon ativo e sincronização com o Time/banco quando ele evolui. */
public final class PartyManager {
    private PartyManager() {}

    /** Grava o estado do companheiro ativo na tabela de pets. */
    // ---- vida de cada Pokémon do time (o ativo vive no Companion; os outros ficam guardados aqui e no banco) ----
    private static final java.util.Map<String, Integer> hpCache = new java.util.HashMap<>();
    private static boolean faintHandled = false;

    private static String hpKey(String species) { return "hp_" + species.toLowerCase(); }

    /** Vida guardada da espécie: -1 = cheia/nunca guardada, 0 = desmaiado, >0 = vida atual. */
    public static int storedHp(GameView gv, String species) {
        Integer v = hpCache.get(species.toLowerCase());
        if (v == null) { v = gv.activity.dbHelper.getKv(hpKey(species), -1); hpCache.put(species.toLowerCase(), v); }
        return v;
    }

    public static void storeHp(GameView gv, String species, int hp) {
        hpCache.put(species.toLowerCase(), hp);
        gv.activity.dbHelper.setKv(hpKey(species), hp);
    }

    private static final java.util.Set<String> dirtyHp = new java.util.HashSet<>();
    private static long lastTeamTick = 0, lastBenchTick = 0;
    /** Bônus atual do time (para a tela do Time e o cabeçalho). */
    public static TeamBonus teamBonus = new TeamBonus();

    private static int storedMax(GameView gv, String species) {
        return gv.activity.dbHelper.getKv("hpmax_" + species.toLowerCase(), 0);
    }

    /** Grava no banco as vidas que a regeneração mexeu só na memória. */
    public static void flushHp(GameView gv) {
        for (String k : dirtyHp) {
            Integer v = hpCache.get(k);
            if (v != null) gv.activity.dbHelper.setKv("hp_" + k, v);
        }
        dirtyHp.clear();
    }

    /**
     * Chamado todo quadro (barato): a cada 1s recalcula o boost do ativo e o apoio do time; a cada 2s regenera,
     * bem devagar, a vida de quem está fora de combate (o time reserva e a caixa).
     */
    public static void tickTeam(GameView gv) {
        long now = System.currentTimeMillis();
        Companion comp = gv.companion;
        if (comp == null || gv.player == null) return;
        if (now - lastTeamTick > 1000) {
            lastTeamTick = now;
            List<CapturedCreature> party = gv.player.getParty();
            int boost = 0;
            for (CapturedCreature c : party) if (isActive(gv, c)) { boost = c.boost; break; }
            TeamBonus tb = TeamBonus.compute(party, comp.getSpecies());
            teamBonus = tb;
            Skill.cooldownScale = 1f - Math.min(0.35f, tb.cdr);
            int oldMax = comp.getMaxHp();
            comp.setSupport(boost, tb);
            if (comp.getMaxHp() != oldMax) gv.applyKingdomBonuses();   // reaplica prédios/clã por cima dos stats recalculados
        }
        if (now - lastBenchTick > 2000) {
            lastBenchTick = now;
            String active = comp.getSpecies();
            java.util.Set<String> done = new java.util.HashSet<>();
            for (CapturedCreature c : gv.player.getCreatureCollection()) {
                String k = c.name.toLowerCase();
                if (c.name.equalsIgnoreCase(active) || !done.add(k)) continue;
                int st = storedHp(gv, c.name);
                if (st <= 0) continue;                              // -1 cheio, 0 desmaiado (só a cura revive)
                int max = storedMax(gv, c.name);
                if (max <= 0) { storeHp(gv, c.name, -1); continue; }
                double rate = 0.003 * (1.0 + teamBonus.regen);      // ~10 min para encher
                int nh = st + Math.max(1, (int) Math.round(max * rate));
                if (nh >= max) { storeHp(gv, c.name, -1); dirtyHp.remove(k); }
                else { hpCache.put(k, nh); dirtyHp.add(k); }
            }
        }
    }

    public static boolean isFainted(GameView gv, CapturedCreature c) {
        return isActive(gv, c) ? gv.companion.getHp() <= 0 : storedHp(gv, c.name) == 0;
    }

    /** Cura todo mundo (Enfermeira, descanso na base): time inteiro e a caixa. */
    public static void healAll(GameView gv) {
        for (CapturedCreature c : gv.player.getCreatureCollection()) storeHp(gv, c.name, -1);
        gv.companion.healFull();
        faintHandled = false;
    }

    /** Ao abrir o jogo: devolve ao companheiro ativo a vida com que ele foi guardado. */
    public static void restoreActiveHp(GameView gv) {
        int st = storedHp(gv, gv.companion.getSpecies());
        if (st >= 0) gv.companion.setHp(st);
    }

    /**
     * Chamado todo quadro. Quando o companheiro ativo desmaia: ele volta para a Pokébola e o próximo do time
     * com vida entra no lugar. Se o time inteiro desmaiou, o jogador vai para o Centro de Cura (Enfermeira).
     */
    public static void updateFaint(GameView gv) {
        Companion comp = gv.companion;
        if (comp == null || gv.player == null) return;
        if (comp.getHp() > 0) { faintHandled = false; return; }
        if (faintHandled) return;
        if (gv.tutorial != null && gv.tutorial.isTutorialActive()) { comp.healFull(); return; }   // no tutorial ninguém desmaia de verdade
        faintHandled = true;

        String fallen = comp.getSpecies();
        storeHp(gv, fallen, 0);
        gv.particles.addExplosion(comp.getPositionX(), comp.getPositionY(), android.graphics.Color.RED, 30);
        SoundManager.play("faint");
        VibrationManager.tap(220);
        gv.ui.showMessage("💀 " + fallen + " desmaiou e voltou para a Pokébola!");
        if (gv.tutorial != null) gv.tutorial.onPetFainted();   // a explicação só aparece na primeira vez

        CapturedCreature next = null;
        for (CapturedCreature c : gv.player.getParty()) {
            if (isActive(gv, c)) continue;
            if (storedHp(gv, c.name) != 0) { next = c; break; }
        }
        if (next != null) {
            if (switchActive(gv, next)) {
                faintHandled = false;
                gv.particles.addExplosion(comp.getPositionX(), comp.getPositionY(), android.graphics.Color.WHITE, 40);
                gv.ui.showMessage("🔴 Vai, " + next.name + "!");
            }
            return;
        }
        // time inteiro desmaiado
        if (gv.world.tileMap.getMapName().equals("HOME")) {
            gv.ui.showMessage("⚠️ Todo o time desmaiou! Use uma Poção ou descanse na base.");
            return;
        }
        healAll(gv);
        gv.ui.showMessage("🏥 Todo o time desmaiou! A Enfermeira Joy cuidou de todos no Centro de Cura.");
        gv.world.loadMap("CITY", gv.player, gv.companion, "CENTER");
    }

    public static void saveActive(GameView gv) {
        Companion c = gv.companion;
        storeHp(gv, c.getSpecies(), c.getHp() >= c.getMaxHp() ? -1 : c.getHp());
        gv.activity.dbHelper.setKv("hpmax_" + c.getSpecies().toLowerCase(), c.getMaxHp());
        flushHp(gv);
        gv.activity.dbHelper.savePet(
            c.getSpecies(), c.getLevel(), c.getExp(), c.getForgeAttackBonus(),
            c.getWeapon() != null ? c.getWeapon().getName() : "Nenhum",
            c.getArmor() != null ? c.getArmor().getName() : "Nenhum",
            c.isShiny(), c.getPersonality(),
            c.getRune() != null ? c.getRune().getName() : "Nenhum",
            c.getFriendship(),
            c.getType2() != null ? c.getType2().name() : "Nenhum",
            c.getRarity(), c.getGender());
    }

    public static boolean isActive(GameView gv, CapturedCreature c) {
        return c.name.equalsIgnoreCase(gv.companion.getSpecies());
    }

    /** Torna a criatura o companheiro ativo (salva o anterior e carrega nível, equipamento e runa do novo). */
    public static boolean switchActive(GameView gv, CapturedCreature sel) {
        if (isActive(gv, sel)) return false;
        if (storedHp(gv, sel.name) == 0) { gv.ui.showMessage("💀 " + sel.name + " está desmaiado! Cure-o antes de chamar."); return false; }
        saveActive(gv);
        Companion comp = gv.companion;
        comp.setSpecies(sel.name);
        PetData d = gv.activity.dbHelper.getPetData(sel.name);
        comp.loadProgress(d.level, d.exp, d.forgeBonus, sel.isShiny, d.friendship, d.type2, sel.rarity, sel.gender);
        comp.setWeapon(Equipment.create(d.weapon));
        comp.setArmor(Equipment.create(d.armor));
        comp.setRune(Rune.create(d.rune));
        gv.applyKingdomBonuses();
        int st = storedHp(gv, sel.name);
        if (st > 0) comp.setHp(st);   // volta com a vida que tinha ao ser guardado
        gv.player.setMounted(false);
        gv.activity.dbHelper.updateActiveSpecies(sel.name);
        gv.activity.saveCurrentGame();
        return true;
    }

    /** Chamado depois que o companheiro ativo virou {@code newSpecies}: renomeia a criatura do time e migra o registro do pet. */
    public static void onActiveEvolved(GameView gv, String oldSpecies, String newSpecies) {
        for (CapturedCreature c : gv.player.getCreatureCollection()) {
            if (c.name.equalsIgnoreCase(oldSpecies) && c.isInParty) { c.name = newSpecies; break; }
        }
        gv.activity.dbHelper.deletePet(oldSpecies);
        storeHp(gv, oldSpecies, -1);
        saveActive(gv);
        gv.activity.dbHelper.updateActiveSpecies(newSpecies);
        gv.bestiary.markAsCaptured(newSpecies);
        gv.activity.saveCurrentGame();
    }

    /**
     * Evolui um Pokémon qualquer do time (não só o ativo): se for o ativo, passa pelo Companion;
     * senão só renomeia a criatura e migra os dados salvos.
     */
    public static void evolveCreature(GameView gv, CapturedCreature c, String newSpecies) {
        String old = c.name;
        if (isActive(gv, c)) {
            gv.companion.evolveInto(newSpecies);
            return;
        }
        PetData d = gv.activity.dbHelper.getPetData(old);
        c.name = newSpecies;
        gv.activity.dbHelper.deletePet(old);
        gv.activity.dbHelper.savePet(newSpecies, d.level, d.exp, d.forgeBonus, d.weapon, d.armor, c.isShiny, d.personality, d.rune, d.friendship, d.type2, c.rarity, c.gender);
        gv.bestiary.markAsCaptured(newSpecies);
        gv.activity.saveCurrentGame();
    }
}
