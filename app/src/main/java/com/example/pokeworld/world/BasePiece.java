package com.example.pokeworld.world;

/** Uma peça colocada na grade da base (prédio, muralha, torre...). */
public class BasePiece {
    private static final int T = 64; // = TileMap.TILE_SIZE
    public int id = -1;
    public final BaseKind kind;
    public int gx, gy;          // canto superior esquerdo, em tiles
    public int level = 1;
    public long finishTime = 0; // >0 enquanto está em obra/melhoria
    public long cropReady = 0;  // Horta: instante em que a plantação amadurece (0 = vazia)
    public boolean upgrading = false; // true = melhoria (a peça já existia); false = obra nova

    public BasePiece(BaseKind kind, int gx, int gy, int level) {
        this.kind = kind; this.gx = gx; this.gy = gy; this.level = level;
    }

    public boolean isBusy(long now) { return finishTime > now; }
    public boolean isDone(long now) { return finishTime > 0 && now >= finishTime; }
    /** Funciona normalmente (não está em obra). Uma melhoria não desliga o prédio. */
    public boolean isUsable() { return finishTime == 0 || upgrading; }

    /** Finaliza a obra/melhoria. Retorna true se mudou algo. */
    public boolean complete() {
        if (finishTime == 0) return false;
        if (upgrading) level++;
        finishTime = 0; upgrading = false;
        return true;
    }

    public float px() { return gx * T; }
    public float py() { return gy * T; }
    public float pw() { return kind.w * T; }
    public float ph() { return kind.h * T; }
    public double centerX() { return px() + pw() / 2.0; }
    public double centerY() { return py() + ph() / 2.0; }
    public int remainingSeconds(long now) { return (int) Math.max(0, (finishTime - now + 999) / 1000); }
}
