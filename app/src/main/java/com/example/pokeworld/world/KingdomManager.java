package com.example.pokeworld.world;

import com.example.pokeworld.entities.Companion;

import java.util.Map;

/**
 * KingdomManager: Gerencia os bônus passivos do Reino (Item 16).
 */
public class KingdomManager {
    
    // Oficina: Aumenta o Poder de Ataque do Companheiro
    public int getAttackBonus(int workshopLevel) {
        return (workshopLevel - 1) * 10;
    }

    // Castelo: Aumenta o Ouro ganho em combates, coletas e missões
    public int getGoldBonus(int castleLevel) {
        return (castleLevel - 1) * 5;
    }

    // Centro de Cura: Aumenta o HP Máximo do Companheiro
    public int getHPBonus(int hospitalLevel) {
        return (hospitalLevel - 1) * 20;
    }

    // Loja: Reduz o custo de itens no mercado
    public int getShopDiscount(int shopLevel) {
        return (shopLevel - 1) * 10; // 10 de desconto por nível
    }

    /**
     * Aplica todos os bônus do reino ao companheiro atual.
     * Deve ser chamado sempre que o companheiro mudar de nível, espécie ou quando um edifício for aprimorado.
     */
    public void applyBonuses(Companion companion, Map<String, Integer> buildings) {
        if (companion == null || buildings == null) return;

        // Resetamos para os valores base antes de aplicar os bônus para evitar acumulo infinito
        companion.resetToBaseStats();

        int workshopLv = buildings.getOrDefault("Oficina", 1);
        int hospitalLv = buildings.getOrDefault("Centro de Cura", 1);

        companion.setAttackPower(companion.getAttackPower() + getAttackBonus(workshopLv));
        companion.setMaxHp(companion.getMaxHp() + getHPBonus(hospitalLv));
        
        // Ajusta o HP atual se o novo máximo for maior e ele estava cheio
        if (companion.getHp() > companion.getMaxHp()) {
            companion.healFull();
        }
    }

    public int calculateGoldWithBonus(int baseGold, int castleLevel) {
        return baseGold + getGoldBonus(castleLevel);
    }

    public int calculateDiscountedPrice(int basePrice, int shopLevel) {
        int discount = getShopDiscount(shopLevel);
        return Math.max(10, basePrice - discount); // Preço mínimo de 10
    }
}
