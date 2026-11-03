package com.example.pokeworld.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class SpriteInspectorView extends View {

    private Bitmap sheet;
    private String pokemonName = "charizard_shiny"; // <--- MUDE O NOME AQUI PARA TESTAR OUTROS

    private final int rows = 3;
    private final int cols = 3;

    // ====================================================================================================
    // AJUSTES TÉCNICOS (Altere os valores abaixo e veja a caixa vermelha mudar em tempo real)
    // ====================================================================================================

    // Deslocamento vertical e altura extra por linha [linha 0, 1, 2]
    public int[] vShift = new int[]{0, -30, 0};
    public int[] vExtra = new int[]{-40, 0, 0};

    // hShift[linha][coluna]: Deslocamento da posição X inicial (Positivo: Direita | Negativo: Esquerda)
    public int[][] hShift = new int[][]{
            {0, 0, 0},
            {0, 0, 0},
            {0, 0, 0}
    };

    // hExtra[linha][coluna]: Largura extra aplicada (Positivo: Alarga | Negativo: Encolhe)
    public int[][] hExtra = new int[][]{
            {-40, 0, 0},
            {-30, 0, 0},
            {10, 0, 0}
    };

    private final Paint gridPaint = new Paint();
    private final Paint cropPaint = new Paint();
    private final Paint textPaint = new Paint();

    public SpriteInspectorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        setPokemon(pokemonName);

        gridPaint.setColor(Color.CYAN);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(2f);

        cropPaint.setColor(Color.RED);
        cropPaint.setStyle(Paint.Style.STROKE);
        cropPaint.setStrokeWidth(5f);

        textPaint.setColor(Color.YELLOW);
        textPaint.setTextSize(30f);
        textPaint.setFakeBoldText(true);
    }

    /**
     * Carrega um pokemon pelo nome do arquivo na pasta drawable
     */
    public void setPokemon(String name) {
        this.pokemonName = name.toLowerCase();
        int resId = getContext().getResources().getIdentifier(pokemonName, "drawable", getContext().getPackageName());
        if (resId != 0) {
            sheet = BitmapFactory.decodeResource(getContext().getResources(), resId);
        }
        invalidate(); // Atualiza a tela
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (sheet == null) {
            canvas.drawText("Erro: Drawable '" + pokemonName + "' não encontrado!", 50, 100, textPaint);
            return;
        }

        // Desenha a imagem base preenchendo a View
        RectF dstRect = new RectF(0, 0, getWidth(), getHeight());
        canvas.drawBitmap(sheet, null, dstRect, null);

        float scaleX = (float) getWidth() / sheet.getWidth();
        float scaleY = (float) getHeight() / sheet.getHeight();

        int fw = sheet.getWidth() / cols;
        int fh = sheet.getHeight() / rows;

        // 1. Grade Matemática Perfeita (Ciano)
        for (int r = 0; r <= rows; r++) {
            canvas.drawLine(0, r * fh * scaleY, getWidth(), r * fh * scaleY, gridPaint);
        }
        for (int c = 0; c <= cols; c++) {
            canvas.drawLine(c * fw * scaleX, 0, c * fw * scaleX, getHeight(), gridPaint);
        }

        // 2. Caixas de Corte Customizadas (Vermelho)
        for (int r = 0; r < rows; r++) {
            for (int f = 0; f < cols; f++) {
                // Lógica de cálculo conforme SpriteManager
                int startY = Math.max(0, r * fh + (vShift.length > r ? vShift[r] : 0));
                int height = fh + (vExtra.length > r ? vExtra[r] : 0) - (vShift.length > r ? vShift[r] : 0);
                
                int sX = (hShift.length > r && hShift[r].length > f) ? hShift[r][f] : 0;
                int eW = (hExtra.length > r && hExtra[r].length > f) ? hExtra[r][f] : 0;

                int startX = Math.max(0, f * fw + sX);
                int width = fw + eW;

                float left = startX * scaleX;
                float top = startY * scaleY;
                float right = (startX + width) * scaleX;
                float bottom = (startY + height) * scaleY;

                canvas.drawRect(left, top, right, bottom, cropPaint);
                
                // Desenha a coordenada da célula (Linha, Coluna) para facilitar o ajuste no código
                canvas.drawText("L" + r + " C" + f, left + 10, top + 40, textPaint);
            }
        }
    }
}
