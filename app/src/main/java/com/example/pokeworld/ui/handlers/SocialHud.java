package com.example.pokeworld.ui.handlers;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.entities.RemotePlayer;
import com.example.pokeworld.managers.FriendManager;
import com.example.pokeworld.managers.GroupManager;
import com.example.pokeworld.managers.MultiplayerManager;
import com.example.pokeworld.managers.SpriteManager;
import com.example.pokeworld.world.ArenaLayout;
import com.example.pokeworld.world.MapVariants;
import com.example.pokeworld.world.WorldManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Parte social do mundo: tocar em outro jogador abre um menu (pedir amizade / convidar para o grupo);
 * o grupo aparece num painel na lateral com nome e sprite de cada colega e, se ele estiver longe, o botão TELEPORTAR.
 */
public class SocialHud {
    private final Paint paint = new Paint();
    private float k = 1f;

    // menu do jogador tocado
    private String menuCode = null, menuName = "";
    private final RectF menuCard = new RectF(), menuFriend = new RectF(), menuGroup = new RectF(), menuClose = new RectF();

    // convite recebido
    private final RectF inviteCard = new RectF(), inviteYes = new RectF(), inviteNo = new RectF();

    // painel do grupo
    private final RectF leaveBtn = new RectF();
    private final List<RectF> slotRects = new ArrayList<>(), tpRects = new ArrayList<>();
    private final List<String> slotCodes = new ArrayList<>();
    private String expandedCode = null;       // colega cujo botão TELEPORTAR está aberto

    public SocialHud() { paint.setAntiAlias(true); }

    // ------------------------------------------------------------------ utilidades
    public static RemotePlayer remoteByCode(String code) {
        if (code == null || code.isEmpty()) return null;
        for (RemotePlayer rp : MultiplayerManager.getInstance().getRemotePlayers()) if (code.equals(rp.code)) return rp;
        return null;
    }

    /** Colega longe: em outro mapa ou a mais de FAR_PX de você. */
    public static boolean isFar(GameView gv, RemotePlayer rp) {
        if (rp == null) return true;
        if (rp.mapName == null || !rp.mapName.equals(gv.world.getMapName())) return true;
        double dx = rp.targetX - gv.player.getPositionX(), dy = rp.targetY - gv.player.getPositionY();
        return Math.sqrt(dx * dx + dy * dy) > GroupManager.FAR_PX;
    }

    /** Só dá para ir a mapas públicos (cidade e mapas selvagens): base, torre e arena são privadas. */
    public static boolean publicMap(String map) {
        return "CITY".equals(map) || Arrays.asList(WorldManager.WILD_MAPS).contains(map);
    }

    public static boolean teleportTo(GameView gv, RemotePlayer rp) {
        if (rp == null) { gv.ui.showMessage("📡 Sem sinal desse colega."); return false; }
        String cur = gv.world.getMapName();
        if (ArenaLayout.MAP.equals(cur) || "TOWER".equals(cur) || (gv.world.defense != null && gv.world.defense.isActive())) {
            gv.ui.showMessage("🚫 Termine o que está fazendo aqui antes de se teleportar.");
            return false;
        }
        if (!publicMap(rp.mapName)) { gv.ui.showMessage("🔒 " + rp.name + " está num lugar privado."); return false; }
        if (!rp.mapName.equals(cur)) gv.world.loadMap(rp.mapName, gv.player, gv.companion, "CENTER");
        double[][] offs = {{-80, 0}, {80, 0}, {0, -80}, {0, 80}, {-80, -80}, {80, 80}, {0, 0}};
        double px = rp.targetX, py = rp.targetY;
        for (double[] o : offs) {
            if (o[0] == 0 && o[1] == 0 || gv.world.tileMap.isWalkable(rp.targetX + o[0], rp.targetY + o[1], false, false, false, false)) { px = rp.targetX + o[0]; py = rp.targetY + o[1]; break; }
        }
        gv.player.setPosition(px, py);
        gv.companion.setPosition(px - 50, py);
        gv.ui.showMessage("⚡ Teleportado para perto de " + rp.name + "!");
        return true;
    }

    // ------------------------------------------------------------------ toque no mundo
    /** Toque em coordenadas do mundo: se acertou outro jogador, abre o menu dele. */
    public boolean tapWorld(float wx, float wy, GameView gv) {
        String map = gv.world.getMapName();
        for (RemotePlayer rp : MultiplayerManager.getInstance().getRemotePlayers()) {
            if (rp.mapName == null || !rp.mapName.equals(map)) continue;
            double dx = rp.posX - wx, dy = (rp.posY - 20) - wy;
            if (dx * dx + dy * dy > 80.0 * 80.0) continue;
            if (rp.code == null || rp.code.isEmpty()) { gv.ui.showMessage("👤 " + rp.name + " (jogador anônimo)"); return true; }
            menuCode = rp.code; menuName = rp.name; expandedCode = null;
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ toque na tela
    public boolean handle(float x, float y, GameView gv) {
        GroupManager gm = GroupManager.getInstance();
        FriendManager fm = FriendManager.getInstance();

        GroupManager.Invite inv = gm.pendingInvite();
        if (inv != null) {
            if (inviteYes.contains(x, y)) { gm.respond(true); return true; }
            if (inviteNo.contains(x, y)) { gm.respond(false); return true; }
            if (inviteCard.contains(x, y)) return true;
        }

        if (menuCode != null) {
            if (menuFriend.contains(x, y)) {
                if (fm.isFriend(menuCode)) gv.ui.showMessage("✓ Vocês já são amigos.");
                else { fm.sendRequest(menuCode); menuCode = null; }
                return true;
            }
            if (menuGroup.contains(x, y)) {
                if (gm.isMember(menuCode)) gv.ui.showMessage("👥 " + menuName + " já está no seu grupo.");
                else if (gm.inGroup() && !gm.isLeader()) gv.ui.showMessage("Só o líder do grupo pode convidar.");
                else if (gm.isFull()) gv.ui.showMessage("Grupo cheio (máx. " + GroupManager.MAX_MEMBERS + ").");
                else { gm.invite(menuCode, menuName); menuCode = null; }
                return true;
            }
            if (menuClose.contains(x, y) || !menuCard.contains(x, y)) { menuCode = null; return true; }
            return true;
        }

        if (gm.inGroup()) {
            if (leaveBtn.contains(x, y)) { gm.leave(); expandedCode = null; return true; }
            for (int i = 0; i < tpRects.size(); i++) {
                if (slotCodes.get(i).equals(expandedCode) && tpRects.get(i).contains(x, y)) {
                    teleportTo(gv, remoteByCode(expandedCode));
                    expandedCode = null;
                    return true;
                }
            }
            for (int i = 0; i < slotRects.size(); i++) {
                if (!slotRects.get(i).contains(x, y)) continue;
                String code = slotCodes.get(i);
                RemotePlayer rp = remoteByCode(code);
                if (isFar(gv, rp)) expandedCode = code.equals(expandedCode) ? null : code;
                else gv.ui.showMessage("👥 " + (rp != null ? rp.name : "O colega") + " está perto de você.");
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ desenho
    public void draw(Canvas canvas, GameView gv) {
        k = ExploringUI.uiScale(canvas.getWidth(), canvas.getHeight());
        GroupManager gm = GroupManager.getInstance();
        slotRects.clear(); tpRects.clear(); slotCodes.clear();
        if (gm.pendingInvite() == null) { inviteCard.setEmpty(); inviteYes.setEmpty(); inviteNo.setEmpty(); }
        if (!gm.inGroup()) { leaveBtn.setEmpty(); expandedCode = null; }
        if (gm.inGroup()) drawParty(canvas, gv, gm);
        drawInvite(canvas, gm);
        if (menuCode != null) drawMenu(canvas, gv, gm);
    }

    private void drawParty(Canvas canvas, GameView gv, GroupManager gm) {
        String me = FriendManager.getInstance().getMyCode();
        float x = 20 * k, y = 232 * k, w = 252 * k, hdr = 20 * k, sh = 44 * k, gap = 3 * k;
        List<GroupManager.Member> others = new ArrayList<>();
        for (GroupManager.Member m : gm.getMembers()) if (!m.code.equals(me)) others.add(m);

        // cabeçalho: 👥 GRUPO  n/4   [SAIR]
        RectF head = new RectF(x, y, x + w, y + hdr);
        paint.setStyle(Paint.Style.FILL); paint.setColor(Color.argb(190, 20, 40, 70));
        canvas.drawRoundRect(head, 8 * k, 8 * k, paint);
        text(canvas, "👥 GRUPO " + gm.getMembers().size() + "/" + GroupManager.MAX_MEMBERS + (gm.isLeader() ? "  ★ líder" : ""), x + 8 * k, y + 15 * k, 13 * k, Color.WHITE, Paint.Align.LEFT, true);
        leaveBtn.set(x + w - 56 * k, y + 2 * k, x + w - 3 * k, y + hdr - 2 * k);
        paint.setColor(Color.argb(220, 150, 50, 50));
        canvas.drawRoundRect(leaveBtn, 6 * k, 6 * k, paint);
        text(canvas, "SAIR", leaveBtn.centerX(), leaveBtn.centerY() + 5 * k, 12 * k, Color.WHITE, Paint.Align.CENTER, true);

        float cy = y + hdr + gap;
        for (GroupManager.Member m : others) {
            RemotePlayer rp = remoteByCode(m.code);
            boolean far = isFar(gv, rp);
            RectF r = new RectF(x, cy, x + w, cy + sh);
            boolean leader = m.code.equals(gm.leaderCode());
            paint.setStyle(Paint.Style.FILL); paint.setColor(Color.argb(far ? 175 : 210, 0, 0, 0));
            canvas.drawRoundRect(r, 10 * k, 10 * k, paint);
            paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(2 * k);
            paint.setColor(leader ? Color.rgb(255, 205, 80) : far ? Color.rgb(120, 130, 150) : Color.rgb(120, 235, 150));
            canvas.drawRoundRect(r, 10 * k, 10 * k, paint);
            paint.setStyle(Paint.Style.FILL);

            Bitmap bmp = SpriteManager.getInstance().getUnitSprite("personagem", 0, 1, false);
            if (bmp != null) {
                Paint sp = null;
                if (rp != null && rp.skinColor != Color.WHITE) { sp = new Paint(); sp.setColorFilter(new PorterDuffColorFilter(rp.skinColor, PorterDuff.Mode.MULTIPLY)); }
                int s = (int) (sh - 6 * k);
                canvas.drawBitmap(bmp, null, new Rect((int) (r.left + 4 * k), (int) (r.top + 3 * k), (int) (r.left + 4 * k + s), (int) (r.top + 3 * k + s)), sp);
            }
            float tx = r.left + sh + 4 * k;
            paint.setTextSize(15 * k);
            text(canvas, (leader ? "★ " : "") + fit(m.name, r.right - tx - 34 * k), tx, r.top + 19 * k, 15 * k, Color.WHITE, Paint.Align.LEFT, true);
            String sub = rp == null ? "sem sinal" : far ? (rp.mapName.equals(gv.world.getMapName()) ? "longe" : "em " + WorldManager.mapLabel(MapVariants.base(rp.mapName))) : "perto";
            text(canvas, sub, tx, r.top + 36 * k, 12 * k, far ? Color.rgb(255, 190, 120) : Color.rgb(140, 235, 170), Paint.Align.LEFT, false);
            if (far) text(canvas, "⚡", r.right - 18 * k, r.top + 28 * k, 20 * k, Color.rgb(255, 220, 90), Paint.Align.CENTER, true);

            slotRects.add(r); slotCodes.add(m.code);
            RectF tp = new RectF(r.right + 6 * k, r.top, r.right + 6 * k + 128 * k, r.bottom);
            tpRects.add(tp);
            if (far && m.code.equals(expandedCode)) {
                paint.setStyle(Paint.Style.FILL); paint.setColor(Color.rgb(255, 205, 70));
                canvas.drawRoundRect(tp, 10 * k, 10 * k, paint);
                paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(2 * k); paint.setColor(Color.WHITE);
                canvas.drawRoundRect(tp, 10 * k, 10 * k, paint);
                paint.setStyle(Paint.Style.FILL);
                text(canvas, "⚡ TELEPORTAR", tp.centerX(), tp.centerY() + 5 * k, 14 * k, Color.BLACK, Paint.Align.CENTER, true);
            } else tp.setEmpty();
            cy += sh + gap;
        }
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawInvite(Canvas canvas, GroupManager gm) {
        GroupManager.Invite inv = gm.pendingInvite();
        if (inv == null) return;
        float w = canvas.getWidth();
        float cw = Math.min(460 * k, w - 40 * k), ch = 92 * k;
        inviteCard.set(w / 2f - cw / 2f, 112 * k, w / 2f + cw / 2f, 112 * k + ch);
        panel(canvas, inviteCard, Color.argb(240, 20, 36, 62), Color.rgb(120, 200, 255));
        int left = (int) Math.max(0, (GroupManager.INVITE_TTL_MS - (System.currentTimeMillis() - inv.at)) / 1000);
        text(canvas, "👥 " + fitTo(inv.fromName + " convidou você para um grupo", cw - 24 * k, 17 * k), inviteCard.centerX(), inviteCard.top + 28 * k, 17 * k, Color.WHITE, Paint.Align.CENTER, true);
        text(canvas, "Caçam juntos e dividem o XP  •  " + left + "s", inviteCard.centerX(), inviteCard.top + 48 * k, 13 * k, Color.rgb(150, 162, 182), Paint.Align.CENTER, false);
        inviteYes.set(inviteCard.left + 14 * k, inviteCard.bottom - 38 * k, inviteCard.centerX() - 6 * k, inviteCard.bottom - 8 * k);
        inviteNo.set(inviteCard.centerX() + 6 * k, inviteCard.bottom - 38 * k, inviteCard.right - 14 * k, inviteCard.bottom - 8 * k);
        button(canvas, inviteYes, "ACEITAR", Color.rgb(90, 210, 130), true);
        button(canvas, inviteNo, "RECUSAR", Color.rgb(200, 90, 90), true);
    }

    private void drawMenu(Canvas canvas, GameView gv, GroupManager gm) {
        FriendManager fm = FriendManager.getInstance();
        float w = canvas.getWidth(), h = canvas.getHeight();
        float cw = Math.min(380 * k, w - 40 * k), ch = 232 * k;
        menuCard.set(w / 2f - cw / 2f, h / 2f - ch / 2f - 30 * k, w / 2f + cw / 2f, h / 2f + ch / 2f - 30 * k);
        paint.setStyle(Paint.Style.FILL); paint.setColor(Color.argb(90, 0, 0, 0));
        canvas.drawRect(0, 0, w, h, paint);
        panel(canvas, menuCard, Color.argb(245, 20, 30, 50), Color.rgb(255, 196, 64));
        text(canvas, "👤 " + fitTo(menuName, cw - 40 * k, 22 * k), menuCard.centerX(), menuCard.top + 38 * k, 22 * k, Color.WHITE, Paint.Align.CENTER, true);
        RemotePlayer rp = remoteByCode(menuCode);
        if (rp != null && rp.petSpecies != null && !rp.petSpecies.isEmpty())
            text(canvas, "com " + rp.petSpecies, menuCard.centerX(), menuCard.top + 58 * k, 14 * k, Color.rgb(150, 162, 182), Paint.Align.CENTER, false);

        boolean friend = fm.isFriend(menuCode);
        boolean member = gm.isMember(menuCode);
        boolean canInvite = !member && !(gm.inGroup() && !gm.isLeader()) && !gm.isFull();
        float bx = menuCard.left + 20 * k, bxr = menuCard.right - 20 * k, bh = 46 * k;
        menuFriend.set(bx, menuCard.top + 72 * k, bxr, menuCard.top + 72 * k + bh);
        menuGroup.set(bx, menuFriend.bottom + 10 * k, bxr, menuFriend.bottom + 10 * k + bh);
        menuClose.set(bx, menuGroup.bottom + 10 * k, bxr, menuGroup.bottom + 10 * k + 36 * k);
        button(canvas, menuFriend, friend ? "✓ JÁ SÃO AMIGOS" : "🤝 PEDIR AMIZADE", Color.rgb(120, 200, 255), !friend);
        button(canvas, menuGroup, member ? "👥 JÁ ESTÁ NO GRUPO" : "👥 CONVIDAR PARA O GRUPO", Color.rgb(255, 205, 80), canInvite);
        button(canvas, menuClose, "FECHAR", Color.rgb(160, 168, 184), true);
    }

    // ------------------------------------------------------------------ helpers de desenho
    private void panel(Canvas canvas, RectF r, int fill, int stroke) {
        paint.setStyle(Paint.Style.FILL); paint.setColor(fill);
        canvas.drawRoundRect(r, 14 * k, 14 * k, paint);
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(3 * k); paint.setColor(stroke);
        canvas.drawRoundRect(r, 14 * k, 14 * k, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void button(Canvas canvas, RectF r, String label, int color, boolean enabled) {
        paint.setStyle(Paint.Style.FILL); paint.setColor(enabled ? color : Color.rgb(52, 58, 70));
        canvas.drawRoundRect(r, 10 * k, 10 * k, paint);
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(2 * k); paint.setColor(enabled ? Color.WHITE : Color.rgb(84, 92, 108));
        canvas.drawRoundRect(r, 10 * k, 10 * k, paint);
        paint.setStyle(Paint.Style.FILL);
        float size = Math.min(18 * k, r.height() * 0.46f);
        text(canvas, fitTo(label, r.width() - 12 * k, size), r.centerX(), r.centerY() + size * 0.34f, size, enabled ? Color.BLACK : Color.rgb(130, 138, 152), Paint.Align.CENTER, true);
    }

    private void text(Canvas canvas, String s, float x, float y, float size, int color, Paint.Align align, boolean bold) {
        paint.setStyle(Paint.Style.FILL); paint.setColor(color); paint.setTextSize(size); paint.setTextAlign(align); paint.setFakeBoldText(bold);
        canvas.drawText(s, x, y, paint);
        paint.setFakeBoldText(false);
    }

    private String fit(String s, float maxW) { return fitTo(s, maxW, paint.getTextSize()); }

    private String fitTo(String s, float maxW, float size) {
        paint.setTextSize(size);
        if (s == null) return "";
        if (paint.measureText(s) <= maxW) return s;
        int n = s.length();
        while (n > 1 && paint.measureText(s, 0, n) + paint.measureText("…") > maxW) n--;
        return s.substring(0, n) + "…";
    }
}
