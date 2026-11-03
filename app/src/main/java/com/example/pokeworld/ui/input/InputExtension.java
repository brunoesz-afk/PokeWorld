package com.example.pokeworld.ui.input;

import android.view.MotionEvent;
import com.example.pokeworld.core.GameView;

public interface InputExtension {
    boolean handle(float x, float y, GameView gv);
    default boolean handleEvent(MotionEvent event, GameView gv) { return false; }
}
