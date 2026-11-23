package com.example.pokeworld.ui.input;

import android.view.MotionEvent;
import com.example.pokeworld.core.GameView;

/**
 * Base para telas com lista rolável: arrastar rola, toque curto (soltar sem arrastar) chama onTap.
 * Assim o toque num item não dispara no começo de um arrasto.
 */
public abstract class TapScrollInput implements InputExtension {
    private float downX, downY, lastY;
    private boolean moved;

    protected abstract boolean onTap(float x, float y, GameView gv);
    /** dy > 0: o dedo desceu. */
    protected void onScroll(float x, float y, float dy, GameView gv) {}

    @Override
    public boolean handle(float x, float y, GameView gv) { return onTap(x, y, gv); }

    @Override
    public boolean handleEvent(MotionEvent e, GameView gv) {
        float x = e.getX(), y = e.getY();
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = x; downY = y; lastY = y; moved = false;
                return true;
            case MotionEvent.ACTION_MOVE:
                if (!moved && (Math.abs(y - downY) > 16 || Math.abs(x - downX) > 16)) moved = true;
                if (moved) { onScroll(downX, downY, y - lastY, gv); lastY = y; }
                return true;
            case MotionEvent.ACTION_UP:
                if (!moved) onTap(x, y, gv);
                return true;
            default:
                return true;
        }
    }
}
