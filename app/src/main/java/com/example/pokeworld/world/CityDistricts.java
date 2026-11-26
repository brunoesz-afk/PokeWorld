package com.example.pokeworld.world;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.entities.NPC;
import com.example.pokeworld.entities.Player;
import com.example.pokeworld.managers.EventCalendar;

import java.util.Calendar;
import java.util.List;

/**
 * Distritos da cidade: cada um tem sua área (em tiles), uma placa flutuante e os NPCs que moram nele.
 * A mesma tabela alimenta o terreno (MapBuilder), os NPCs, as placas desenhadas no mundo e o aviso de entrada.
 * Coordenadas em tiles (1 tile = 64 px). Mapa da cidade: 100 x 50.
 */
public final class CityDistricts {
    private CityDistricts() {}

    private static final int T = 64;

    public static final class District {
        public final String id, icon, name, desc;
        public final int color, r0, c0, r1, c1;
        District(String id, String icon, String name, String desc, int color, int r0, int c0, int r1, int c1) {
            this.id = id; this.icon = icon; this.name = name; this.desc = desc; this.color = color;
            this.r0 = r0; this.c0 = c0; this.r1 = r1; this.c1 = c1;
        }
        boolean contains(double px, double py) {
            return px >= c0 * T && px < (c1 + 1) * T && py >= r0 * T && py < (r1 + 1) * T;
        }
        float centerX() { return (c0 + c1 + 1) * T / 2f; }
    }

    /** Um NPC da cidade: onde mora, o que faz e como aparece. */
    public static final class Spot {
        public final String name, dialogue, action, sprite, role;
        public final NPC.Type type;
        public final double tx, ty;
        public final int color;
        Spot(String name, String dialogue, NPC.Type type, String action, String sprite, String role, int color, double tx, double ty) {
            this.name = name; this.dialogue = dialogue; this.type = type; this.action = action;
            this.sprite = sprite; this.role = role; this.color = color; this.tx = tx; this.ty = ty;
        }
    }

    // A ordem importa: o primeiro distrito que contém o ponto vence (a Praça sobrepõe as bordas dos vizinhos).
    public static final District PRACA = new District("praca", "🎉", "Praça dos Eventos", "Eventos do dia e avisos do reino", Color.rgb(255, 200, 90), 19, 42, 33, 58);
    public static final District COLISEU = new District("coliseu", "🏟️", "Coliseu", "Torre, Ginásio e bases rivais", Color.rgb(235, 90, 90), 9, 70, 22, 85);
    public static final District GUILDA = new District("guilda", "📜", "Guilda de Missões", "Jornada, diárias e recompensas", Color.rgb(110, 190, 255), 29, 70, 41, 85);
    public static final District CURA = new District("cura", "🏥", "Quarteirão da Cura", "Centro de Cura e jardim", Color.rgb(255, 150, 190), 9, 14, 22, 47);
    public static final District MERCADO = new District("mercado", "🛒", "Mercado", "Loja e troca de Pokémon", Color.rgb(120, 220, 140), 9, 53, 22, 69);
    public static final District MESTRES = new District("mestres", "⚒️", "Distrito dos Mestres", "Forja, Clãs e Ginásio", Color.rgb(190, 140, 255), 29, 14, 41, 47);
    public static final District[] ALL = {PRACA, COLISEU, GUILDA, CURA, MERCADO, MESTRES};

    /** Retângulos dos distritos em tiles {r0, c0, r1, c1} (para os treinadores simulados passearem). */
    public static int[][] areas() {
        int[][] a = new int[ALL.length][];
        for (int i = 0; i < ALL.length; i++) a[i] = new int[]{ALL[i].r0, ALL[i].c0, ALL[i].r1, ALL[i].c1};
        return a;
    }

    /** Onde o Mercador Viajante aparece (Quarteirão da Cura). */
    public static final double MERCHANT_TX = 24.5, MERCHANT_TY = 16.5;

    public static final Spot[] SPOTS = {
        // Praça dos Eventos
        new Spot("Arauto de Eventos", "", NPC.Type.GUARD, "EVENTS", "treinador_pokebola", "🎉 EVENTOS", Color.rgb(255, 200, 90), 50.5, 21.5),
        new Spot("Guarda Real", "Bem-vindo ao Reino! Cura e Mercado ficam ao norte, o Coliseu a nordeste,\na Guilda de Missões a sudeste e os Mestres a sudoeste.", NPC.Type.GUARD, "", null, "🛡 INFORMAÇÕES", Color.rgb(200, 210, 230), 50.5, 31.5),
        // Quarteirão da Cura
        new Spot("Enfermeira", "Deseja curar seus Pokémons?", NPC.Type.HEALER, "", null, "🏥 CURA GRÁTIS", Color.rgb(255, 150, 190), 36.5, 21.8),
        // Mercado
        new Spot("Mercador", "Ouro por itens?", NPC.Type.SHOPKEEPER, "SHOP", null, "🛒 LOJA", Color.rgb(120, 220, 140), 60.5, 21.8),
        new Spot("Comerciante de Trocas", "Troco seus Pokémon repetidos por itens!", NPC.Type.SHOPKEEPER, "TRADE", "personagem", "🔁 TROCAS", Color.rgb(120, 220, 140), 66.5, 21.8),
        // Coliseu
        new Spot("Mestre do Coliseu", "", NPC.Type.GUARD, "COLISEUM", "prof_ginasio", "🏟️ DESAFIOS", Color.rgb(235, 90, 90), 77.5, 15.5),
        new Spot("Mestre da Torre", "", NPC.Type.GUARD, "TOWER", "treinador_montaria", "🗼 TORRE", Color.rgb(235, 90, 90), 73.5, 19.5),
        new Spot("Capitão Rival", "", NPC.Type.GUARD, "RIVALS", "treinador_academia", "⚔️ BASES RIVAIS", Color.rgb(235, 90, 90), 81.5, 19.5),
        // Guilda de Missões
        new Spot("Mestre da Jornada", "", NPC.Type.GUARD, "JOURNEY", "prof_mural", "📖 JORNADA", Color.rgb(110, 190, 255), 73, 33),
        new Spot("Chefe das Diárias", "", NPC.Type.GUARD, "DAILY", "treinador_laboratorio", "📅 DIÁRIAS", Color.rgb(110, 190, 255), 73, 38),
        new Spot("Caçador", "", NPC.Type.GUARD, "BOUNTIES", "prof_mural", "🎯 CAÇADAS", Color.rgb(110, 190, 255), 79, 38.5),
        // Distrito dos Mestres
        new Spot("Ferreiro", "", NPC.Type.BLACKSMITH, "FORGE", null, "🔨 FORJA", Color.rgb(190, 140, 255), 20.5, 33.5),
        new Spot("Mestre dos Clãs", "", NPC.Type.GUARD, "CLAN", "treinador_laboratorio", "🔥 CLÃS", Color.rgb(190, 140, 255), 25.5, 38.5),
        new Spot("Líder Brock", "", NPC.Type.GUARD, "GYM", "prof_ginasio", "🥋 GINÁSIO", Color.rgb(190, 140, 255), 36.5, 39.5),
    };

    private static String lastDistrict = "";
    private static long lastRefresh = 0;
    private static final Paint paint = new Paint();
    static { paint.setAntiAlias(true); }

    /** Chamado ao carregar a cidade. */
    public static void spawn(List<NPC> npcs) {
        lastDistrict = ""; lastRefresh = 0;
        for (Spot s : SPOTS) {
            NPC n = new NPC(s.name, s.dialogue, s.tx * T, s.ty * T, s.type);
            n.setAction(s.action);
            if (s.sprite != null) n.setSprite(s.sprite);
            n.setRole(s.role, s.color);
            npcs.add(n);
        }
    }

    public static District districtAt(double px, double py) {
        for (District d : ALL) if (d.contains(px, py)) return d;
        return null;
    }

    /** Avisa a entrada em cada distrito e mantém os ícones "!" dos NPCs de missão (1x por segundo). */
    public static void update(Player p, GameView gv) {
        District d = districtAt(p.getPositionX(), p.getPositionY());
        String id = d == null ? "" : d.id;
        if (!id.equals(lastDistrict)) {
            lastDistrict = id;
            if (d != null) gv.ui.showMessage("📍 " + d.icon + " " + d.name.toUpperCase() + " — " + d.desc);
        }
        long now = System.currentTimeMillis();
        if (now - lastRefresh < 1000) return;
        lastRefresh = now;
        for (NPC n : gv.world.npcs) {
            switch (n.getAction()) {
                case "EVENTS": n.setQuestIcon(eventSeen(gv) ? NPC.QuestIcon.NONE : NPC.QuestIcon.MISSION_AVAILABLE); break;
                case "JOURNEY": n.setQuestIcon(gv.missions.currentAct() != 0 ? NPC.QuestIcon.MISSION_AVAILABLE : NPC.QuestIcon.NONE); break;
                case "DAILY": n.setQuestIcon(gv.missions.dailiesDone() < 3 ? NPC.QuestIcon.MISSION_AVAILABLE : NPC.QuestIcon.NONE); break;
                default: break;
            }
        }
    }

    private static int todayKey() {
        Calendar c = Calendar.getInstance();
        return c.get(Calendar.YEAR) * 400 + c.get(Calendar.DAY_OF_YEAR);
    }

    public static boolean eventSeen(GameView gv) { return gv.activity.dbHelper.getKv("city_event_seen", 0) == todayKey(); }
    public static void markEventSeen(GameView gv) { gv.activity.dbHelper.setKv("city_event_seen", todayKey()); }

    /** Tinta suave no chão de cada distrito (desenhada logo depois do mapa, antes de prédios e NPCs). */
    public static void drawGround(Canvas canvas, float vl, float vt, float vr, float vb) {
        for (District d : ALL) {
            RectF r = new RectF(d.c0 * T, d.r0 * T, (d.c1 + 1) * T, (d.r1 + 1) * T);
            if (r.right < vl || r.left > vr || r.bottom < vt || r.top > vb) continue;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(22, Color.red(d.color), Color.green(d.color), Color.blue(d.color)));
            canvas.drawRoundRect(r, 24, 24, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(4);
            paint.setColor(Color.argb(90, Color.red(d.color), Color.green(d.color), Color.blue(d.color)));
            canvas.drawRoundRect(new RectF(r.left + 6, r.top + 6, r.right - 6, r.bottom - 6), 20, 20, paint);
        }
        paint.setStyle(Paint.Style.FILL);
    }

    /** Placas dos distritos por cima de tudo (nome + descrição; a Praça mostra o evento de hoje). */
    public static void drawLabels(Canvas canvas, float vl, float vt, float vr, float vb) {
        paint.setTextAlign(Paint.Align.CENTER);
        for (District d : ALL) {
            float cx = d.centerX(), top = d.r0 * T;
            if (cx < vl - 400 || cx > vr + 400 || top < vt - 200 || top > vb + 100) continue;
            String title = d.icon + "  " + d.name.toUpperCase();
            String sub = d.desc;
            if (d == PRACA) {
                EventCalendar.Event e = EventCalendar.today();
                sub = "HOJE: " + e.icon + " " + e.name + " — " + e.desc;
            }
            paint.setFakeBoldText(true);
            paint.setTextSize(34);
            float w1 = paint.measureText(title);
            paint.setFakeBoldText(false);
            paint.setTextSize(20);
            float w2 = paint.measureText(sub);
            float bw = Math.max(w1, w2) + 44, bh = 84, by = top + 22;
            RectF bg = new RectF(cx - bw / 2, by, cx + bw / 2, by + bh);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(190, 14, 20, 34));
            canvas.drawRoundRect(bg, 18, 18, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(4);
            paint.setColor(d.color);
            canvas.drawRoundRect(bg, 18, 18, paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setFakeBoldText(true);
            paint.setTextSize(34);
            paint.setColor(Color.WHITE);
            canvas.drawText(title, cx, by + 40, paint);
            paint.setFakeBoldText(false);
            paint.setTextSize(20);
            paint.setColor(Color.rgb(205, 215, 235));
            canvas.drawText(sub, cx, by + 68, paint);
        }
        paint.setFakeBoldText(false);
    }
}
