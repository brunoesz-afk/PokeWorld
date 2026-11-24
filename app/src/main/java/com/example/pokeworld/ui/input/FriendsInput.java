package com.example.pokeworld.ui.input;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.text.InputFilter;
import android.text.InputType;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.managers.FriendManager;
import com.example.pokeworld.ui.UIManager;
import com.example.pokeworld.ui.handlers.FriendsUI;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FriendsInput implements InputExtension {
    @Override
    public boolean handle(float x, float y, GameView gv) {
        FriendsUI ui = (FriendsUI) gv.ui.getHandler(UIManager.UIState.FRIENDS);
        if (ui == null) return false;
        FriendManager fm = FriendManager.getInstance();

        if (ui.exitBtn.contains(x, y)) { ui.confirmRemove = ""; gv.ui.state = UIManager.UIState.EXPLORING; return true; }
        if (!ui.prevBtn.isEmpty() && ui.prevBtn.contains(x, y)) { ui.page = Math.max(0, ui.page - 1); ui.confirmRemove = ""; return true; }
        if (!ui.nextBtn.isEmpty() && ui.nextBtn.contains(x, y)) { ui.page++; ui.confirmRemove = ""; return true; }

        if (ui.addBtn.contains(x, y)) { askAddFriend(gv); return true; }
        if (ui.nameBtn.contains(x, y)) { askName(gv); return true; }
        if (ui.copyBtn.contains(x, y)) { copyCode(gv); return true; }
        if (ui.shareBtn.contains(x, y)) { share(gv); return true; }

        for (FriendsUI.Hit hit : ui.hits) {
            if (!hit.r.contains(x, y)) continue;
            switch (hit.action) {
                case "accept": fm.respond(hit.code, true); break;
                case "decline": fm.respond(hit.code, false); break;
                case "remove":
                    if (hit.code.equals(ui.confirmRemove)) { ui.confirmRemove = ""; fm.remove(hit.code); }
                    else { ui.confirmRemove = hit.code; gv.ui.showMessage("Toque de novo em CONFIRMAR? para remover."); }
                    break;
                default: break;
            }
            return true;
        }
        ui.confirmRemove = "";
        return true;
    }

    private void copyCode(GameView gv) {
        try {
            ClipboardManager cm = (ClipboardManager) gv.activity.getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("codigo", FriendManager.getInstance().shareText()));
            gv.ui.showMessage("Código copiado!");
        } catch (Exception e) { gv.ui.showMessage("Não foi possível copiar."); }
    }

    private void share(GameView gv) {
        final String msg = FriendManager.getInstance().shareText();
        gv.activity.runOnUiThread(() -> {
            try {
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("text/plain");
                i.putExtra(Intent.EXTRA_TEXT, msg);
                gv.activity.startActivity(Intent.createChooser(i, "Enviar código de amigo"));
            } catch (Exception e) { FriendManager.getInstance().notice("Não foi possível abrir o compartilhamento."); }
        });
    }

    private void askAddFriend(GameView gv) {
        gv.activity.runOnUiThread(() -> {
            final EditText in = new EditText(gv.activity);
            in.setSingleLine(true);
            in.setHint("ABCD-EFGH");
            in.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
            in.setFilters(new InputFilter[]{new InputFilter.LengthFilter(14)});
            try { // se o jogador copiou a mensagem de convite, já preenche
                ClipboardManager cm = (ClipboardManager) gv.activity.getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null && cm.hasPrimaryClip() && cm.getPrimaryClip().getItemCount() > 0) {
                    CharSequence cs = cm.getPrimaryClip().getItemAt(0).getText();
                    if (cs != null) {
                        Matcher m = Pattern.compile("\\b([A-Z0-9]{4})-([A-Z0-9]{4})\\b").matcher(cs.toString().toUpperCase());
                        if (m.find()) in.setText(m.group(1) + "-" + m.group(2));
                    }
                }
            } catch (Exception ignored) {}
            new AlertDialog.Builder(gv.activity)
                    .setTitle("Adicionar amigo")
                    .setMessage("Digite o código de amigo (8 letras/números):")
                    .setView(in)
                    .setPositiveButton("ENVIAR PEDIDO", (d, w) -> {
                        FriendManager.getInstance().sendRequest(in.getText().toString());
                        gv.activity.hideSystemUI();
                    })
                    .setNegativeButton("Cancelar", (d, w) -> gv.activity.hideSystemUI())
                    .setOnCancelListener(d -> gv.activity.hideSystemUI())
                    .show();
        });
    }

    private void askName(GameView gv) {
        gv.activity.runOnUiThread(() -> {
            final EditText in = new EditText(gv.activity);
            in.setSingleLine(true);
            in.setText(FriendManager.getInstance().getMyName());
            in.setSelection(in.getText().length());
            in.setFilters(new InputFilter[]{new InputFilter.LengthFilter(16)});
            new AlertDialog.Builder(gv.activity)
                    .setTitle("Seu nome de treinador")
                    .setMessage("É o nome que seus amigos e outros jogadores veem.")
                    .setView(in)
                    .setPositiveButton("SALVAR", (d, w) -> {
                        FriendManager.getInstance().setName(in.getText().toString());
                        FriendManager.getInstance().notice("Nome alterado para " + FriendManager.getInstance().getMyName() + ".");
                        gv.activity.hideSystemUI();
                    })
                    .setNegativeButton("Cancelar", (d, w) -> gv.activity.hideSystemUI())
                    .setOnCancelListener(d -> gv.activity.hideSystemUI())
                    .show();
        });
    }
}
