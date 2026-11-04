package com.example.pokeworld.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

import com.example.pokeworld.core.GameView;

public class GameInterfaceOverlay extends View {
    private GameView gameView;

    public GameInterfaceOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setGameView(GameView gameView) {
        this.gameView = gameView;
    }

    @Override
    public boolean onTouchEvent(android.view.MotionEvent event) {
        if (gameView != null && gameView.input != null) {
            return gameView.input.handle(event);
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (gameView != null && gameView.ui != null) {
            // Ciclo de atualização
            gameView.update();

            // Desenha o mundo (Mapa, Jogador, Inimigos, Efeitos, Partículas)
            gameView.draw(canvas);

            // Desenha a UI e filtros globais (Overlay)
            gameView.weather.draw(canvas);
            gameView.time.drawFilter(canvas);
            gameView.ui.draw(canvas, gameView);
            
            if (gameView.ui.state == UIManager.UIState.EXPLORING && 
                !gameView.ui.isDialogueActive && 
                !gameView.fishing.isFishing) {
                gameView.input.joystick.draw(canvas);
            }
            
            // Força o redesenho sincronizado com VSYNC para máximo desempenho sem travar a CPU
            postInvalidateOnAnimation();
        }
    }
}
