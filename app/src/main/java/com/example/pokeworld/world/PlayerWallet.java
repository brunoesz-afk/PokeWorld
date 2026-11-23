package com.example.pokeworld.world;

import com.example.pokeworld.entities.Player;

/** Liga as regras da base ao inventário e ao ouro do jogador. */
public class PlayerWallet implements BaseManager.Wallet {
    private final Player p;
    public PlayerWallet(Player p) { this.p = p; }

    @Override public int count(String item) { Integer v = p.getInventory().get(item); return v == null ? 0 : v; }
    @Override public void take(String item, int n) {
        int left = count(item) - n;
        if (left > 0) p.getInventory().put(item, left); else p.getInventory().remove(item);
    }
    @Override public int gold() { return p.getGold(); }
    @Override public void addGold(int g) { p.addGold(g); }
    @Override public void give(String item, int n) { p.addItem(item, n); }
}
