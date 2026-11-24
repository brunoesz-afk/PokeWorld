package com.example.pokeworld.entities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** O que o Mercador vende e quanto paga. Uma fonte só para a tela e para o toque. */
public final class ShopCatalog {
    private ShopCatalog() {}

    public static final class Offer {
        public final String name, desc;
        public final int basePrice;
        public final boolean discountable;   // o nível da Loja (reino) baixa o preço
        Offer(String name, int basePrice, boolean discountable, String desc) {
            this.name = name; this.basePrice = basePrice; this.discountable = discountable; this.desc = desc;
        }
    }

    public static final List<Offer> BUY = new ArrayList<>();
    private static final Map<String, Integer> SELL = new HashMap<>();
    /** Materiais usados na base e na oficina: ficam de fora do "vender sucata". */
    private static final String[] KEEP = {"Madeira", "Ferro", "Pedra", "Fruta", "Pelo"};

    static {
        BUY.add(new Offer("Poção", 50, true, "Restaura toda a vida do companheiro ativo. Use na Mochila ou pelo botão de cura rápida."));
        BUY.add(new Offer("Pokébola", 100, false, "Captura Pokémon derrotados. Arraste a bola até o Pokémon no chão."));
        BUY.add(new Offer("Great Ball", 300, false, "Bola melhor: chance de captura x1,5. Usada automaticamente antes da Pokébola."));
        BUY.add(new Offer("Ultra Ball", 800, false, "A melhor bola: chance de captura x2. Tem prioridade sobre as outras."));
        BUY.add(new Offer("Fruta", 40, false, "+25% de ataque por 30s. Também serve para Doce de Fruta."));
        BUY.add(new Offer("Bomba Fumaça", 100, false, "Foge na hora para a cidade quando a coisa aperta."));

        String[] basic = {"Madeira", "Ferro", "Pedra", "Fruta", "Carvão", "Essência de Fogo", "Cristal de Água", "Coral",
                "Poeira Mística", "Presa", "Gosma", "Ferrão", "Pelo", "Núcleo de Rocha", "Essência Vital", "Doce de Fruta"};
        for (String m : basic) SELL.put(m, 15);
        SELL.put("Doce de Fruta", 25);
        SELL.put("Núcleo de Rocha", 30); SELL.put("Essência Vital", 30); SELL.put("Essência de Fogo", 30);
        SELL.put("Pérola", 50); SELL.put("Obsidiana", 50);
        SELL.put("Fragmento Shiny", 100);
        SELL.put("Relíquia Dourada", 120); SELL.put("Escama Celestial", 150);
    }

    /** Preço de compra de uma unidade (com o desconto da Loja do reino quando houver). */
    public static int buyPrice(Offer o, com.example.pokeworld.world.KingdomManager k, int shopLevel) {
        return o.discountable ? k.calculateDiscountedPrice(o.basePrice, shopLevel) : o.basePrice;
    }

    /** Quanto o Mercador paga por unidade, ou 0 se não compra esse item. */
    public static int sellPrice(String item) { Integer p = SELL.get(item); return p == null ? 0 : p; }

    public static boolean isKept(String item) {
        for (String k : KEEP) if (k.equals(item)) return true;
        return false;
    }

    /** Itens da mochila que o Mercador compra, na ordem do catálogo. */
    public static List<String> sellable(Map<String, Integer> inventory) {
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, Integer> e : inventory.entrySet()) {
            if (e.getValue() != null && e.getValue() > 0 && sellPrice(e.getKey()) > 0) out.add(e.getKey());
        }
        java.util.Collections.sort(out);
        return out;
    }
}
