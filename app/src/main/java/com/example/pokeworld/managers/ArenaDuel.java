package com.example.pokeworld.managers;

import com.example.pokeworld.entities.Companion;
import com.example.pokeworld.entities.ElementalType;
import com.example.pokeworld.entities.Rarity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Motor do duelo da Arena (sem nada de tela, para poder ser testado): dois times de até 3 Pokémon,
 * um ativo de cada lado, golpes automáticos em intervalos, vantagem de tipo, e troca do Pokémon ativo
 * com recarga de 5 segundos. Quem derrubar o time inteiro do outro vence; no tempo limite vence quem tem mais vida.
 */
public final class ArenaDuel {
    public static final int MAX_TEAM = 3;
    public static final long SWAP_CD_MS = 5000;
    public static final long COUNTDOWN_MS = 3000;
    public static final long HIT_MS = 1300;
    public static final long TIME_LIMIT_MS = 120000;
    public static final long PICK_MS = 2000;       // tempo para escolher o próximo quando o ativo cai

    public enum Phase { PREP, COUNTDOWN, FIGHT, RESULT }

    /** Um lutador do duelo. Os números vêm de um Companion de verdade, então seguem o mesmo balanceamento do jogo. */
    public static final class Unit {
        public String species;
        public int level, maxHp, hp, atk;
        public ElementalType t1, t2;
        public boolean shiny;
        public Rarity rarity = Rarity.COMMON;
        public int hits;                 // golpes dados (a cada 4º é um golpe especial)

        public boolean alive() { return hp > 0; }
        public float hpFrac() { return maxHp <= 0 ? 0 : Math.max(0f, hp / (float) maxHp); }
        public int power() { return atk + maxHp / 5; }

        public static Unit from(Companion c) {
            Unit u = new Unit();
            u.species = c.getSpecies(); u.level = c.getLevel();
            u.maxHp = c.getMaxHp(); u.hp = u.maxHp; u.atk = c.getAttackPower();
            u.t1 = c.getType1(); u.t2 = c.getType2();
            u.shiny = c.isShiny(); u.rarity = c.getRarity();
            return u;
        }
    }

    /** Texto flutuante para a tela mostrar (dano, crítico, efetividade). */
    public static final class Pop {
        public boolean onFoe;
        public String text;
        public int color;
        public Pop(boolean onFoe, String text, int color) { this.onFoe = onFoe; this.text = text; this.color = color; }
    }

    public static final class Side {
        public String name;
        public final List<Unit> team = new ArrayList<>();
        public int active = 0;                // -1 = precisa escolher o próximo
        public long swapCd = 0;               // ms até poder trocar de novo
        public long hitIn = HIT_MS;
        public long pickIn = 0;
        public long hurtFlash = 0;            // ms restantes do "piscar" de dano
        public Unit cur() { return active >= 0 && active < team.size() ? team.get(active) : null; }
        public int aliveCount() { int n = 0; for (Unit u : team) if (u.alive()) n++; return n; }
        public int totalHp() { int s = 0; for (Unit u : team) s += Math.max(0, u.hp); return s; }
        public int totalMax() { int s = 0; for (Unit u : team) s += u.maxHp; return s; }
        public int power() { int s = 0; for (Unit u : team) s += u.power(); return s; }
    }

    public final Side me = new Side(), foe = new Side();
    public Phase phase = Phase.PREP;
    public long countdown = COUNTDOWN_MS;
    public long elapsed = 0;
    public boolean won = false;
    public boolean timeUp = false;
    public final List<Pop> pops = new ArrayList<>();
    private final Random rnd;
    private long aiThink = 1500;

    public ArenaDuel(Random rnd) { this.rnd = rnd == null ? new Random() : rnd; }

    /** Pronto: começa a contagem regressiva. */
    public void ready() {
        if (phase != Phase.PREP || me.cur() == null || foe.team.isEmpty()) return;
        int best = 0; double bs = -1;                  // o oponente escolhe o inicial que melhor encaixa contra o seu
        for (int i = 0; i < foe.team.size(); i++) { double sc = score(foe.team.get(i), me.cur()); if (sc > bs) { bs = sc; best = i; } }
        foe.active = best;
        phase = Phase.COUNTDOWN; countdown = COUNTDOWN_MS;
    }

    /** Desistir no meio da luta conta como derrota. */
    public void forfeit() { if (phase == Phase.FIGHT || phase == Phase.COUNTDOWN) finish(false); }

    /** Troca o Pokémon ativo do jogador. Vale na preparação (livre) e na luta (recarga de 5 s). */
    public boolean swap(int idx) {
        if (idx < 0 || idx >= me.team.size() || !me.team.get(idx).alive()) return false;
        if (phase == Phase.PREP || phase == Phase.COUNTDOWN) { me.active = idx; return true; }
        if (phase != Phase.FIGHT) return false;
        if (me.active == idx) return false;
        if (me.active == -1) { me.active = idx; me.pickIn = 0; me.hitIn = 700; return true; }   // escolha após cair: sem recarga
        if (me.swapCd > 0) return false;
        me.active = idx; me.swapCd = SWAP_CD_MS; me.hitIn = 700;
        pops.add(new Pop(false, "🔄 " + me.cur().species, 0xFF9AD4FF));
        return true;
    }

    public void update(long dt) {
        if (phase == Phase.COUNTDOWN) {
            countdown -= dt;
            if (countdown <= 0) { phase = Phase.FIGHT; me.hitIn = HIT_MS; foe.hitIn = HIT_MS + 250; }
            return;
        }
        if (phase != Phase.FIGHT) return;
        elapsed += dt;
        me.swapCd = Math.max(0, me.swapCd - dt);
        foe.swapCd = Math.max(0, foe.swapCd - dt);
        me.hurtFlash = Math.max(0, me.hurtFlash - dt);
        foe.hurtFlash = Math.max(0, foe.hurtFlash - dt);

        if (me.active == -1) {                      // jogador escolhe o próximo; depois do prazo o jogo escolhe
            me.pickIn -= dt;
            if (me.pickIn <= 0) autoPick(me, foe);
        }
        if (foe.active == -1) {
            foe.pickIn -= dt;
            if (foe.pickIn <= 0) autoPick(foe, me);
        }
        aiThink -= dt;
        if (aiThink <= 0) { aiThink = 1000; aiSwap(); }

        if (me.cur() != null && foe.cur() != null) {
            me.hitIn -= dt;
            foe.hitIn -= dt;
            if (me.hitIn <= 0) { me.hitIn += HIT_MS; strike(me, foe, true); }
            if (phase == Phase.FIGHT && foe.hitIn <= 0 && foe.cur() != null && me.cur() != null) { foe.hitIn += HIT_MS; strike(foe, me, false); }
        }
        if (phase == Phase.FIGHT && elapsed >= TIME_LIMIT_MS) {
            timeUp = true;
            float a = me.totalHp() / (float) Math.max(1, me.totalMax()), b = foe.totalHp() / (float) Math.max(1, foe.totalMax());
            finish(a > b);
        }
    }

    private void strike(Side att, Side def, boolean byMe) {
        Unit a = att.cur(), d = def.cur();
        if (a == null || d == null) return;
        double mult = Math.max(ElementalType.getMultiplier(a.t1, d.t1, d.t2), a.t2 == null ? 0 : ElementalType.getMultiplier(a.t2, d.t1, d.t2));
        a.hits++;
        boolean special = a.hits % 4 == 0;
        boolean crit = rnd.nextDouble() < 0.10;
        double v = a.atk * mult * (0.9 + rnd.nextDouble() * 0.2) * (special ? 1.8 : 1.0) * (crit ? 1.5 : 1.0);
        int dmg = Balance.dmg((int) Math.round(v));
        d.hp = Math.max(0, d.hp - dmg);
        def.hurtFlash = 260;
        boolean onFoe = byMe;
        String t = "-" + dmg;
        if (special) t = "⭐ " + t;
        else if (crit) t = "💥 " + t;
        pops.add(new Pop(onFoe, t, special ? 0xFFFFD84A : crit ? 0xFFFF8A4A : 0xFFFFFFFF));
        if (mult > 1.0) pops.add(new Pop(onFoe, "SUPER EFETIVO!", 0xFFFFE066));
        else if (mult < 1.0) pops.add(new Pop(onFoe, "Pouco efetivo", 0xFFB0B8C8));
        if (d.hp <= 0) {
            pops.add(new Pop(onFoe, "💀 " + d.species, 0xFFFF6A6A));
            if (def.aliveCount() == 0) { finish(byMe); return; }
            def.active = -1; def.pickIn = (def == foe) ? 900 : PICK_MS;
        }
    }

    private void finish(boolean iWon) { won = iWon; phase = Phase.RESULT; }

    /** Escolhe o próximo vivo com melhor encaixe de tipo contra o ativo do outro lado. */
    private void autoPick(Side s, Side other) {
        int best = -1; double bs = -1;
        Unit o = other.cur();
        for (int i = 0; i < s.team.size(); i++) {
            Unit u = s.team.get(i);
            if (!u.alive()) continue;
            double sc = o == null ? u.power() : score(u, o);
            if (sc > bs) { bs = sc; best = i; }
        }
        s.active = best; s.pickIn = 0; s.hitIn = 700;
        if (s == foe && best >= 0) pops.add(new Pop(true, "🔄 " + s.cur().species, 0xFF9AD4FF));
    }

    private double score(Unit u, Unit o) {
        double atk = ElementalType.getMultiplier(u.t1, o.t1, o.t2);
        double def = ElementalType.getMultiplier(o.t1, u.t1, u.t2);
        return u.power() * atk / Math.max(0.25, def);
    }

    /** O oponente também troca (respeitando os 5 s): sai de uma desvantagem de tipo ou quando está muito ferido. */
    private void aiSwap() {
        if (foe.swapCd > 0 || foe.cur() == null || me.cur() == null || foe.active < 0) return;
        Unit cur = foe.cur();
        double curScore = score(cur, me.cur());
        int best = -1; double bs = curScore;
        for (int i = 0; i < foe.team.size(); i++) {
            if (i == foe.active || !foe.team.get(i).alive()) continue;
            double sc = score(foe.team.get(i), me.cur());
            if (sc > bs) { bs = sc; best = i; }
        }
        if (best < 0) return;
        boolean worse = ElementalType.getMultiplier(me.cur().t1, cur.t1, cur.t2) > 1.0 && bs > curScore * 1.4;
        boolean hurt = cur.hpFrac() < 0.25 && foe.team.get(best).hpFrac() > 0.6;
        if ((worse || hurt) && rnd.nextDouble() < 0.6) {
            foe.active = best; foe.swapCd = SWAP_CD_MS; foe.hitIn = 700;
            pops.add(new Pop(true, "🔄 " + foe.cur().species, 0xFF9AD4FF));
        }
    }

    public long timeLeft() { return Math.max(0, TIME_LIMIT_MS - elapsed); }
}
