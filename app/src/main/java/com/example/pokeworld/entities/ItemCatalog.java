package com.example.pokeworld.entities;

import java.util.LinkedHashMap;
import java.util.Map;

/** Catálogo de itens: categoria, ícone, descrição e o que o botão de uso faz. */
public final class ItemCatalog {
    private ItemCatalog() {}

    public enum Category {
        CONSUMIVEL("Consumíveis", 0xFF5AD282),
        CAPTURA("Captura", 0xFFE85A5A),
        EVOLUCAO("Evolução", 0xFFFFC440),
        MATERIAL("Materiais", 0xFF7FB2E5),
        RUNA("Runas", 0xFFC080FF);

        public final String label;
        public final int color;
        Category(String label, int color) { this.label = label; this.color = color; }
    }

    public static final class Info {
        public final String icon, desc, useLabel; // useLabel == null: sem ação direta
        public final Category category;
        Info(Category c, String icon, String desc, String useLabel) {
            this.category = c; this.icon = icon; this.desc = desc; this.useLabel = useLabel;
        }
    }

    private static final Map<String, Info> CATALOG = new LinkedHashMap<>();
    private static final Info UNKNOWN = new Info(Category.MATERIAL, "📦", "Item diverso. Pode ser vendido na loja junto com os outros materiais.", null);

    static {
        put("Poção", Category.CONSUMIVEL, "🧪", "Restaura toda a vida do companheiro ativo.", "USAR");
        put("Peixe", Category.CONSUMIVEL, "🐟", "Pescado fresco. Restaura toda a vida do companheiro ativo.", "USAR");
        put("Fruta", Category.CONSUMIVEL, "🍎", "Dá +25% de ataque ao companheiro por 30s. Também é ingrediente de Doce de Fruta e colhida na Horta da base.", "USAR");
        put("Doce de Fruta", Category.CONSUMIVEL, "🍬", "Feito no fogão. Recupera fome; é usado automaticamente quando você está com fome.", "COMER");
        put("Bomba Fumaça", Category.CONSUMIVEL, "🌫️", "Foge instantaneamente para a cidade.", "USAR");
        put("Gema de Boost", Category.EVOLUCAO, "💎", "Sobe o BOOST (+1, +2...) de um Pokémon: cada nível dá +8% de ataque e vida. Use na ficha do Pokémon, na tela do Time. Quanto maior o boost, menor a chance.", null);
        put("Mega Pedra", Category.EVOLUCAO, "🔮", "Ativada no golpe especial do companheiro: faz a Mega Evolução temporária.", null);
        put("Pokébola", Category.CAPTURA, "🔴", "Usada para capturar Pokémon selvagens. Cai de chefes, e a Horta da base produz mais.", null);
        put("Pedra Fogo", Category.EVOLUCAO, "🔥", "Evolui Eevee (Flareon), Growlithe (Arcanine) e Vulpix (Ninetales). Cai de Pokémon de fogo e chefes.", "EVOLUIR");
        put("Pedra Água", Category.EVOLUCAO, "🌊", "Evolui Eevee (Vaporeon), Poliwhirl, Staryu e mais. Cai de Pokémon de água e gelo e chefes.", "EVOLUIR");
        put("Pedra Trovão", Category.EVOLUCAO, "⚡", "Evolui Pikachu (Raichu), Eevee (Jolteon) e Magneton. Cai de Pokémon elétricos e chefes.", "EVOLUIR");
        put("Pedra Folha", Category.EVOLUCAO, "🍃", "Evolui Eevee (Leafeon), Gloom e Exeggcute. Cai de Pokémon de planta e chefes.", "EVOLUIR");
        put("Madeira", Category.MATERIAL, "🪵", "Material de construção: muralhas, torres e casas da base.", null);
        put("Ferro", Category.MATERIAL, "⛓️", "Material de construção e de melhoria das muralhas da base.", null);
        put("Pedra", Category.MATERIAL, "🪨", "Pedra bruta de Pokémon de rocha. Vale ouro na loja.", null);
        put("Pelo", Category.MATERIAL, "🧶", "10 Pelos viram uma Capa Térmica na oficina. Também serve na Forja.", null);
        put("Presa", Category.MATERIAL, "🦷", "Material de Forja: aumenta o ataque do companheiro.", null);
        put("Gosma", Category.MATERIAL, "🫧", "Material de Forja: aumenta o ataque do companheiro.", null);
        put("Ferrão", Category.MATERIAL, "🪡", "Material de Forja: aumenta o ataque do companheiro.", null);
        put("Núcleo de Rocha", Category.MATERIAL, "💠", "Na Forja vira a Armadura de Rocha.", null);
        put("Essência Vital", Category.MATERIAL, "💚", "Na Forja vira a Lâmina Vital.", null);
        put("Relíquia Dourada", Category.MATERIAL, "🏺", "Na Forja vira o Cajado Solar.", null);
        put("Escama Celestial", Category.MATERIAL, "🐉", "Na Forja vira a Couraça de Dragão.", null);
        put("Fogão", Category.MATERIAL, "🍳", "Equipamento de cozinha: permite fazer Doce de Fruta.", null);
        put("Capa Térmica", Category.MATERIAL, "🧣", "Protege do frio das montanhas nevadas enquanto estiver na mochila.", null);
    }

    private static void put(String n, Category c, String icon, String desc, String use) {
        CATALOG.put(n, new Info(c, icon, desc, use));
    }

    public static Info get(String name) {
        Info i = CATALOG.get(name);
        if (i != null) return i;
        if (name.startsWith("Runa")) {
            Rune r = Rune.create(name);
            return new Info(Category.RUNA, "🔷", r != null ? r.getDescription() : "Runa misteriosa.", "EQUIPAR");
        }
        return UNKNOWN;
    }

    public static boolean isStone(String name) { return name.startsWith("Pedra ") ; }
}
