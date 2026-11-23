package com.example.pokeworld.entities;

import java.util.ArrayList;
import java.util.List;

/**
 * Árvore de talentos: 3 ramos (Domador, Guerreiro, Explorador). Cada nó exige um nível mínimo do nó pai
 * para ser liberado. As chaves "Domador", "Guerreiro" e "Explorador" são as mesmas dos saves antigos.
 */
public final class TalentTree {
    private TalentTree() {}

    public static final class Node {
        public final String key, name, icon, parent, effect;
        public final int branch, row, side;   // side: -1 esquerda, 0 centro, +1 direita dentro do ramo
        public final int max, parentReq;
        public final double perLevel;         // efeito por nível (fração: 0.10 = 10%)
        Node(int branch, int row, int side, String key, String name, String icon, int max, String parent, int parentReq, double perLevel, String effect) {
            this.branch = branch; this.row = row; this.side = side; this.key = key; this.name = name; this.icon = icon;
            this.max = max; this.parent = parent; this.parentReq = parentReq; this.perLevel = perLevel; this.effect = effect;
        }
        public int percent() { return (int) Math.round(perLevel * 100); }
        /** Texto do efeito com o valor por nível, ex.: "+10% de chance de captura". */
        public String effectText() { return effect.replace("{p}", String.valueOf(percent())); }
    }

    public static final String[] BRANCH_NAMES = {"DOMADOR", "GUERREIRO", "EXPLORADOR"};
    public static final String[] BRANCH_ICONS = {"🔴", "⚔️", "🧭"};

    public static final List<Node> NODES = new ArrayList<>();

    static {
        // Ramo 0: Domador (captura e evolução do time)
        n(0, 0, 0, "Domador", "Domador", "🔴", 5, null, 0, 0.10, "+{p}% de chance de captura por nível");
        n(0, 1, -1, "Coletor", "Coletor", "🎒", 3, "Domador", 2, 0.10, "{p}% de chance de não gastar a Pokébola ao capturar");
        n(0, 1, 1, "Estudioso", "Estudioso", "📚", 5, "Domador", 3, 0.10, "+{p}% de EXP para o companheiro");
        n(0, 2, 1, "Mentor", "Mentor", "🎓", 3, "Estudioso", 3, 0.10, "+{p}% de EXP para o treinador");
        // Ramo 1: Guerreiro (combate do companheiro)
        n(1, 0, 0, "Guerreiro", "Guerreiro", "⚔️", 5, null, 0, 0.04, "+{p}% de ataque do companheiro por nível");
        n(1, 1, -1, "Critico", "Golpe Crítico", "🎯", 5, "Guerreiro", 2, 0.03, "+{p}% de chance de golpe crítico nas habilidades");
        n(1, 1, 1, "Resistencia", "Pele Dura", "🛡️", 5, "Guerreiro", 3, 0.04, "-{p}% de dano recebido pelo companheiro");
        n(1, 2, -1, "Furia", "Fúria", "😡", 3, "Critico", 3, 0.15, "+{p}% de ganho de Fúria ao lutar");
        n(1, 2, 1, "Vitalidade", "Vitalidade", "❤️", 5, "Resistencia", 3, 0.05, "+{p}% de vida máxima do companheiro");
        // Ramo 2: Explorador (mobilidade e economia)
        n(2, 0, 0, "Explorador", "Explorador", "🧭", 5, null, 0, 0.10, "+{p}% de velocidade por nível");
        n(2, 1, -1, "Folego", "Fôlego", "🫁", 5, "Explorador", 2, 0.10, "+{p}% de stamina máxima");
        n(2, 1, 1, "Garimpeiro", "Garimpeiro", "💰", 5, "Explorador", 2, 0.08, "+{p}% de ouro dos inimigos");
        n(2, 2, -1, "Dash", "Pés Ligeiros", "👟", 3, "Folego", 3, 0.15, "-{p}% de recarga do dash");
        n(2, 2, 1, "Achado", "Achado Raro", "🍀", 3, "Garimpeiro", 3, 0.10, "+{p}% de chance de drops raros (runas e pedras)");
    }

    private static void n(int b, int r, int s, String key, String name, String icon, int max, String parent, int req, double per, String effect) {
        NODES.add(new Node(b, r, s, key, name, icon, max, parent, req, per, effect));
    }

    public static Node get(String key) {
        for (Node n : NODES) if (n.key.equals(key)) return n;
        return null;
    }

    public static int level(Player p, String key) {
        Integer v = p.getTalents().get(key);
        return v == null ? 0 : v;
    }

    /** Bônus total (fração) do talento: nível × efeito por nível. */
    public static double bonus(Player p, String key) {
        Node n = get(key);
        return n == null ? 0 : level(p, key) * n.perLevel;
    }

    public static boolean isUnlocked(Player p, Node n) {
        return n.parent == null || level(p, n.parent) >= n.parentReq;
    }

    public static boolean canUpgrade(Player p, Node n) {
        return p.getTalentPoints() > 0 && level(p, n.key) < n.max && isUnlocked(p, n);
    }

    public static String requirementText(Node n) {
        if (n.parent == null) return "Nó inicial do ramo";
        Node par = get(n.parent);
        return "Requer " + par.name + " nível " + n.parentReq;
    }

    public static int spentPoints(Player p) {
        int t = 0;
        for (Node n : NODES) t += level(p, n.key);
        return t;
    }
}
