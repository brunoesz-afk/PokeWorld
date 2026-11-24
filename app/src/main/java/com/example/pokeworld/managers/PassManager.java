package com.example.pokeworld.managers;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.managers.MissionManager.Metric;

import java.util.Calendar;

/**
 * Passe da Temporada (mensal): 30 níveis com trilha grátis e trilha premium.
 * O XP do passe vem das missões do passe (3 diárias + as da temporada), que o jogador reivindica;
 * depois ele reivindica a recompensa de cada nível. A trilha premium custa {@link PointsManager#PRICE_PASS} pontos.
 */
public final class PassManager {
    private PassManager() {}

    public static final int LEVELS = 30, XP_PER_LEVEL = 100, MAX_XP = LEVELS * XP_PER_LEVEL;

    // ------------------------------------------------------------------ recompensas
    public static final class Reward {
        public final RewardManager.Prize prize;   // ouro/gemas/item
        public final String skinId;               // ou uma skin exclusiva
        Reward(RewardManager.Prize p, String skinId) { this.prize = p; this.skinId = skinId; }
        public String icon() { return skinId != null ? SkinManager.byId(skinId).icon : prize.icon; }
        public String text() { return skinId != null ? "Skin " + SkinManager.byId(skinId).name : prize.text(); }
        void give(GameView gv, RewardManager.Store s) {
            if (skinId != null) SkinManager.grant(s, SkinManager.byId(skinId));
            else prize.give(gv);
        }
    }

    private static Reward prize(String icon, int gold, int gems, String item, int qty) { return new Reward(new RewardManager.Prize(icon, gold, gems, item, qty), null); }

    public static Reward freeReward(int lv) {
        switch (lv % 5) {
            case 1: return prize("🪙", 150 + lv * 30, 0, null, 0);
            case 2: return prize("🧪", 0, 0, "Poção", 2 + lv / 6);
            case 3: return prize("🔴", 0, 0, "Pokébola", 3 + lv / 5);
            case 4: return prize("🍎", 0, 0, "Fruta", 3 + lv / 5);
            default: return prize("💎", 0, 1 + lv / 10, null, 0);
        }
    }

    public static Reward premiumReward(int lv) {
        if (lv == 10) return new Reward(null, "p_aurora");
        if (lv == 20) return new Reward(null, "c_eclipse");
        if (lv == 30) return new Reward(null, "p_lendario");
        switch (lv % 5) {
            case 1: return prize("🪙", 400 + lv * 60, 0, null, 0);
            case 2: return prize("🔵", 0, 0, "Great Ball", 2 + lv / 6);
            case 3: return prize("🟡", 0, 0, "Ultra Ball", 1 + lv / 10);
            case 4: return prize("🔷", 0, 0, "Gema de Boost", 1);
            default: return prize("💎", 0, 3 + lv / 5, null, 0);
        }
    }

    // ------------------------------------------------------------------ missões
    public static final class Mission {
        public final String title; public final Metric metric; public final int target, xp;
        Mission(String t, Metric m, int target, int xp) { this.title = t; this.metric = m; this.target = target; this.xp = xp; }
    }

    private static final Mission[] DAILY_POOL = {
        new Mission("Derrote 15 Pokémon", Metric.KILLS, 15, 50),
        new Mission("Capture 2 Pokémon", Metric.CAPTURES, 2, 50),
        new Mission("Pesque 2 peixes", Metric.FISH, 2, 50),
        new Mission("Colha 1 horta da base", Metric.HARVESTS, 1, 50),
        new Mission("Conclua 1 andar da Torre", Metric.TOWER_RUNS, 1, 60),
        new Mission("Derrote 1 chefe", Metric.BOSS_KILLS, 1, 60),
        new Mission("Treine 1 vez (Academia)", Metric.TRAININGS, 1, 50),
        new Mission("Vença 1 invasão da base", Metric.RAIDS_WON, 1, 60),
    };

    public static final Mission[] SEASON = {
        new Mission("Derrote 300 Pokémon", Metric.KILLS, 300, 200),
        new Mission("Capture 25 Pokémon", Metric.CAPTURES, 25, 200),
        new Mission("Derrote 5 chefes", Metric.BOSS_KILLS, 5, 200),
        new Mission("Pesque 15 peixes", Metric.FISH, 15, 150),
        new Mission("Colha 10 hortas", Metric.HARVESTS, 10, 150),
        new Mission("Conclua 10 andares da Torre", Metric.TOWER_RUNS, 10, 250),
        new Mission("Defenda a base 8 vezes", Metric.RAIDS_WON, 8, 150),
        new Mission("Faça 10 treinos", Metric.TRAININGS, 10, 150),
        new Mission("Complete 5 expedições", Metric.EXPEDITIONS, 5, 150),
        new Mission("Faça 3 fusões", Metric.FUSIONS, 3, 100),
    };

    public static final int DAILY_SLOTS = 3;

    public static Mission daily(int slot) { return DAILY_POOL[(RewardManager.today() + slot * 3) % DAILY_POOL.length]; }

    // ------------------------------------------------------------------ temporada
    public static int season() { Calendar c = RewardManager.clock.get(); return c.get(Calendar.YEAR) * 100 + c.get(Calendar.MONTH) + 1; }

    public static int daysLeft() {
        Calendar c = RewardManager.clock.get();
        return c.getActualMaximum(Calendar.DAY_OF_MONTH) - c.get(Calendar.DAY_OF_MONTH) + 1;
    }

    /** Zera o que é da temporada/dia quando a temporada ou o dia mudam. Chamado no começo de toda leitura. */
    public static void ensure(RewardManager.Store s) {
        int se = season();
        if (s.get("pass_season", 0) != se) {
            s.set("pass_season", se);
            s.set("pass_xp", 0); s.set("pass_free", 0); s.set("pass_prem", 0);
            for (int i = 0; i < SEASON.length; i++) { s.set("pass_m" + i, 0); s.set("pass_mc" + i, 0); }
        }
        int day = RewardManager.today();
        if (s.get("pass_d_day", 0) != day) {
            s.set("pass_d_day", day);
            for (int i = 0; i < DAILY_SLOTS; i++) { s.set("pass_dp" + i, 0); s.set("pass_dc" + i, 0); }
        }
    }

    public static boolean premium(RewardManager.Store s) { ensure(s); return s.get("pass_premium", 0) == season(); }

    public static boolean buyPass(RewardManager.Store s) {
        if (premium(s)) return false;
        if (!PointsManager.spend(s, PointsManager.PRICE_PASS)) return false;
        s.set("pass_premium", season());
        return true;
    }

    public static int xp(RewardManager.Store s) { ensure(s); return s.get("pass_xp", 0); }
    public static int level(RewardManager.Store s) { return Math.min(LEVELS, xp(s) / XP_PER_LEVEL); }

    public static void addXp(GameView gv, RewardManager.Store s, int n) {
        ensure(s);
        int before = level(s);
        s.set("pass_xp", Math.min(MAX_XP, s.get("pass_xp", 0) + n));
        int after = level(s);
        if (gv != null && after > before) gv.ui.showMessage("🎟️ PASSE nível " + after + "! Reivindique a recompensa.");
    }

    // ------------------------------------------------------------------ níveis
    public static boolean freeClaimed(RewardManager.Store s, int lv) { ensure(s); return (s.get("pass_free", 0) >> (lv - 1) & 1) == 1; }
    public static boolean premClaimed(RewardManager.Store s, int lv) { ensure(s); return (s.get("pass_prem", 0) >> (lv - 1) & 1) == 1; }
    public static boolean freeReady(RewardManager.Store s, int lv) { return lv >= 1 && lv <= LEVELS && level(s) >= lv && !freeClaimed(s, lv); }
    public static boolean premReady(RewardManager.Store s, int lv) { return lv >= 1 && lv <= LEVELS && premium(s) && level(s) >= lv && !premClaimed(s, lv); }

    public static boolean claimFree(GameView gv, RewardManager.Store s, int lv) {
        if (!freeReady(s, lv)) return false;
        s.set("pass_free", s.get("pass_free", 0) | (1 << (lv - 1)));
        Reward r = freeReward(lv); r.give(gv, s);
        gv.ui.showMessage("🎟️ Nível " + lv + ": " + r.text());
        return true;
    }

    public static boolean claimPrem(GameView gv, RewardManager.Store s, int lv) {
        if (!premReady(s, lv)) return false;
        s.set("pass_prem", s.get("pass_prem", 0) | (1 << (lv - 1)));
        Reward r = premiumReward(lv); r.give(gv, s);
        gv.ui.showMessage("🎟️ Nível " + lv + " ⭐: " + r.text());
        return true;
    }

    // ------------------------------------------------------------------ progresso das missões
    /** Chamado por MissionManager.count(): soma 1 em toda missão do passe que usa essa métrica. */
    public static void onMetric(Metric m) {
        RewardManager.Store s = PointsManager.store();
        if (s == null) return;
        ensure(s);
        for (int i = 0; i < DAILY_SLOTS; i++) {
            Mission q = daily(i);
            if (q.metric == m && s.get("pass_dc" + i, 0) == 0 && s.get("pass_dp" + i, 0) < q.target) s.set("pass_dp" + i, s.get("pass_dp" + i, 0) + 1);
        }
        for (int i = 0; i < SEASON.length; i++) {
            Mission q = SEASON[i];
            if (q.metric == m && s.get("pass_mc" + i, 0) == 0 && s.get("pass_m" + i, 0) < q.target) s.set("pass_m" + i, s.get("pass_m" + i, 0) + 1);
        }
    }

    public static int dailyProgress(RewardManager.Store s, int slot) { ensure(s); return s.get("pass_dp" + slot, 0); }
    public static int seasonProgress(RewardManager.Store s, int i) { ensure(s); return s.get("pass_m" + i, 0); }
    public static boolean dailyClaimed(RewardManager.Store s, int slot) { ensure(s); return s.get("pass_dc" + slot, 0) == 1; }
    public static boolean seasonClaimed(RewardManager.Store s, int i) { ensure(s); return s.get("pass_mc" + i, 0) == 1; }
    public static boolean dailyReady(RewardManager.Store s, int slot) { return !dailyClaimed(s, slot) && dailyProgress(s, slot) >= daily(slot).target; }
    public static boolean seasonReady(RewardManager.Store s, int i) { return !seasonClaimed(s, i) && seasonProgress(s, i) >= SEASON[i].target; }

    public static boolean claimDaily(GameView gv, RewardManager.Store s, int slot) {
        if (!dailyReady(s, slot)) return false;
        s.set("pass_dc" + slot, 1);
        gv.ui.showMessage("🎟️ " + daily(slot).title + ": +" + daily(slot).xp + " XP do Passe");
        addXp(gv, s, daily(slot).xp);
        return true;
    }

    public static boolean claimSeason(GameView gv, RewardManager.Store s, int i) {
        if (!seasonReady(s, i)) return false;
        s.set("pass_mc" + i, 1);
        gv.ui.showMessage("🎟️ " + SEASON[i].title + ": +" + SEASON[i].xp + " XP do Passe");
        addXp(gv, s, SEASON[i].xp);
        return true;
    }

    // ------------------------------------------------------------------ resumo
    public static int missionsReady(RewardManager.Store s) {
        int n = 0;
        for (int i = 0; i < DAILY_SLOTS; i++) if (dailyReady(s, i)) n++;
        for (int i = 0; i < SEASON.length; i++) if (seasonReady(s, i)) n++;
        return n;
    }

    public static int levelRewardsReady(RewardManager.Store s) {
        int n = 0;
        for (int lv = 1; lv <= LEVELS; lv++) { if (freeReady(s, lv)) n++; if (premReady(s, lv)) n++; }
        return n;
    }

    public static int readyCount(RewardManager.Store s) { return missionsReady(s) + levelRewardsReady(s); }

    /** Reivindica missões primeiro (dão XP) e depois todos os níveis liberados. Retorna quantas coisas pegou. */
    public static int claimAll(GameView gv, RewardManager.Store s) {
        int n = 0;
        for (int i = 0; i < DAILY_SLOTS; i++) if (claimDaily(gv, s, i)) n++;
        for (int i = 0; i < SEASON.length; i++) if (claimSeason(gv, s, i)) n++;
        for (int lv = 1; lv <= LEVELS; lv++) { if (claimFree(gv, s, lv)) n++; if (claimPrem(gv, s, lv)) n++; }
        return n;
    }
}
