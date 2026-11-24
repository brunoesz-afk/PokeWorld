package com.example.pokeworld.managers;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.ui.UIManager;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

/**
 * Dicas de uso: cada uma aparece uma única vez (salva no banco), na primeira vez que o jogador
 * abre uma tela, entra num mapa ou atinge um marco. São mostradas só em exploração, fora de diálogos e do tutorial.
 */
public class TipManager {
    private static final Map<String, String> TEXT = new HashMap<>();
    static {
        // telas
        TEXT.put("screen_INVENTORY", "Mochila: toque num item para ver o que ele faz. Pedras de Evolução e Runas têm botões próprios, e itens sem uso direto servem para construir e forjar.");
        TEXT.put("screen_CREATURES", "Time: veja nível, amizade e evoluções de cada Pokémon. Toque em TORNAR ATIVO para escolher quem luta ao seu lado. Quando um Pokémon atinge o nível de evoluir (ou você tem a pedra certa) aparece o botão EVOLUIR na ficha dele: a evolução nunca é automática, você decide.");
        TEXT.put("screen_EXPEDITION", "Expedições: seus Pokémon da Caixa/Defesa viajam e voltam com recursos (mesmo com o jogo fechado). Bases rivais: 3 novas por dia, a chance depende do poder do time.");
        TEXT.put("screen_TRAINING", "Academia: Pokémon da Caixa e da Defesa treinam por 5 min e voltam com XP, mesmo com o jogo fechado. Quanto maior o nível da Academia, mais vagas e mais XP.");
        TEXT.put("screen_SHOP", "Mercado: compre Poções e bolas; venda sucata (drops de inimigos). Materiais da base ficam guardados. O ouro é raro, então compre só o que precisa.");
        TEXT.put("screen_TRADE", "Mercado de Trocas: só Pokémon repetidos (mesma espécie, raridade e brilho) fora do time e sem boost. Sempre fica 1 de cada.");
        TEXT.put("screen_FRIENDS", "Amigos: passe seu código (ENVIAR) ou digite o de um amigo. Quando ele aceitar, vocês veem quem está online e aparecem com ★ verde no mapa.");
        TEXT.put("screen_BOOSTLAB", "Laboratório de Boost: mova o boost de um Pokémon para outro (perde 1 nível) ou recicle em Gemas de Boost.");
        TEXT.put("screen_TALENTS", "Talentos: ganhe 1 ponto por nível de treinador. Cada nó só libera quando o nó de cima chega ao nível pedido, então planeje o ramo. Dá para resetar por ouro.");
        TEXT.put("screen_RUNES", "Runas: equipe uma por Pokémon ativo. Sangue cura, Mercúrio acelera, Sorte aumenta ouro e drops. Elas caem de chefes e dos andares 5, 10, 15 e 20 da Torre.");
        TEXT.put("screen_BESTIARY", "Pokédex: chegue perto de um Pokémon para registrá-lo. A ficha mostra onde cada espécie aparece e como ela evolui.");
        TEXT.put("screen_ACHIEVEMENTS", "Conquistas: ao cumprir uma, toque em REIVINDICAR para receber o ouro ou as gemas.");
        TEXT.put("screen_TOWER_SELECT", "Torre: cada andar tem 3 hordas e dá prêmios. Você precisa concluir um andar para liberar o próximo.");
        TEXT.put("screen_STORAGE", "Gerenciador: Pokémon na CAIXA ficam guardados e não aparecem na base. Os da DEFESA andam pelo reino e protegem contra invasões. A defesa cresce com o Quartel e com cada construção.");
        TEXT.put("screen_BUILD", "Construção: escolha uma peça, posicione e confirme. Obras levam tempo, e melhorar o Quartel libera mais peças.");
        TEXT.put("screen_EVOLUTION", "Pedras: escolha quem vai evoluir. A pedra é consumida e o Pokémon mantém nível, equipamento e runa.");
        TEXT.put("screen_MISSIONS", "Jornada: as missões do ato atual aparecem aqui. Quando uma fica pronta, toque em REIVINDICAR (ou REIVINDICAR TUDO). O próximo ato abre quando você reivindica o último deste.");
        // mapas
        TEXT.put("map_HOME", "Esta é a sua BASE. Toque em 🔨 para construir. Pokémon da defesa andam por aqui, e invasões de tempos em tempos testam suas muralhas e torres.");
        TEXT.put("map_CITY", "Cidade em distritos: Praça dos Eventos (centro), Cura e Mercado (norte), Coliseu (nordeste), Guilda de Missões (sudeste) e Mestres (sudoeste). Um \"!\" sobre o NPC indica novidade.");
        TEXT.put("screen_REWARDS", "Central de Recompensas: pegue o Presente Diário (7 dias seguidos, o 7º é o melhor), o Baú do Evento e a Recarga grátis de suprimentos. A barra de ícones no topo da cidade e da base avisa quando há algo para reivindicar.");
        TEXT.put("screen_COLISEUM", "Coliseu: reúne os desafios de combate (Torre, Torre Infinita, Ginásio e Bases Rivais) e mostra o bônus do dia.");
        TEXT.put("map_wild", "Mapa selvagem: os Pokémon só aparecem quando você chega perto, em pontos fixos. Depois que um cai, o ponto demora um pouco para renascer.");
        TEXT.put("map_SMALL_LAKE", "Um lago! Chegue perto da água e use a ação de pescar.");
        TEXT.put("map_VOLCANO", "Vulcão: há um chefe Charizard brilhante aqui. Leve Poções e uma runa antes de enfrentá-lo.");
        TEXT.put("map_SNOW_MOUNTAIN", "Montanha Nevada: o frio é intenso. Uma Capa Térmica na mochila ajuda, e o Articuno guarda o topo.");
        // marcos
        TEXT.put("talentpoints", "Você tem pontos de talento para gastar! Abra o menu de talentos e escolha um ramo.");
        TEXT.put("noballs", "Suas Pokébolas acabaram. Elas caem de chefes e invasões e são produzidas na Horta 🌱 e na Oficina da base.");
        TEXT.put("evolve_close", "Seu Pokémon está perto de evoluir! Veja os níveis de evolução na tela do Time.");
        // guia de início (uma vez só, depois do tutorial)
        TEXT.put("intro_potion", "🧪 CURA RÁPIDA: o botão da poção (canto inferior esquerdo) cura o seu Pokémon ativo na hora e gasta 1 Poção. Ele pisca quando a vida está baixa. Compre Poções no Mercado da cidade.");
        TEXT.put("intro_swap", "🔴 TROCA: os ícones do seu time ficam no topo da tela. Toque em um deles para trocar de Pokémon na hora. Se um desmaiar, o próximo do time entra sozinho. Todos regeneram vida devagar com o tempo.");
        TEXT.put("intro_skills", "⚔ GOLPES: os botões de golpes ficam na lateral. Alguns atingem uma área (marcados com ◎) e podem causar estados: queimadura, veneno, sono, paralisia e gelo. Pokémon reserva dão bônus ao ativo!");
        // dicas de dinheiro (cada uma aparece uma vez)
        TEXT.put("money_intro", "💰 OURO É RARO! Inimigos soltam pouco. As melhores fontes: Missões diárias 📅, Expedições 🎒 (rendem mesmo com o jogo fechado), Torre 🗼, bases rivais 🏴 e vender sucata no Mercador. Gaste com cuidado!");
        TEXT.put("money_low", "🪙 Ouro baixo! Ideias: faça as missões diárias, mande Pokémon da Caixa em expedição, pesque, colha a Horta/Estufa e venda sucata no Mercador (Madeira, Ferro, Pedra, Fruta e Pelo ficam guardados para a base).");
        TEXT.put("money_potion", "🧪 Poção custa 50 🪙. Para economizar: deixe o regen lento curar fora de combate, troque de Pokémon pela barra do topo e use Frutas/Peixes (que também curam) em vez de comprar.");
        TEXT.put("money_rich", "💎 Muito ouro guardado! Bons investimentos: Gema de Boost (1500 🪙), melhorar o Quartel e a Academia, resetar talentos ou comprar Ultra Balls para capturar chefes.");
        TEXT.put("money_saturday", "🏰 Hoje o evento paga mais pela defesa da base e as expedições levam metade do tempo: ótimo dia para juntar ouro!");
        TEXT.put("lowhp", "Vida baixa! Use uma Poção na Mochila ou volte à cidade para a Enfermeira.");
    }

    private final GameView gv;
    private final ArrayDeque<String> queue = new ArrayDeque<>();
    private UIManager.UIState lastState = null;
    private String lastMap = "";
    private long lastCheck = 0;

    public TipManager(GameView gv) { this.gv = gv; }

    private boolean seen(String key) { return gv.activity.dbHelper.getKv("tip_" + key, 0) == 1; }

    private void enqueue(String key) {
        if (TEXT.containsKey(key) && !queue.contains(key) && !seen(key)) queue.add(key);
    }

    /** Chamado a cada quadro de exploração; o trabalho pesado roda no máximo 2x por segundo. */
    public void update() {
        UIManager.UIState st = gv.ui.state;
        if (st != lastState) {
            lastState = st;
            if (st != UIManager.UIState.EXPLORING) enqueue("screen_" + st.name());
        }
        long now = System.currentTimeMillis();
        if (now - lastCheck < 500) return;
        lastCheck = now;

        String map = gv.world.tileMap.getMapName();
        if (!map.equals(lastMap)) {
            lastMap = map;
            if (TEXT.containsKey("map_" + map)) enqueue("map_" + map);
            else if (!map.equals("HOME") && !map.equals("CITY") && !map.equals("TOWER")) enqueue("map_wild");
        }
        boolean wild = !map.equals("HOME") && !map.equals("CITY");
        if (gv.player.getTalentPoints() > 0 && gv.player.getLevel() >= 2) enqueue("talentpoints");
        if (!gv.tutorial.isTutorialActive()) {
            int gold = gv.player.getGold();
            if (gv.player.getLevel() >= 2) enqueue("money_intro");
            if (gold < 120 && gv.player.getLevel() >= 3) enqueue("money_low");
            if (gv.player.getInventory().getOrDefault("Poção", 0) <= 0 && gv.player.getLevel() >= 3) enqueue("money_potion");
            if (gold >= 5000) enqueue("money_rich");
            if (com.example.pokeworld.managers.EventCalendar.dayIndex() == 6) enqueue("money_saturday");
        }
        if (!gv.tutorial.isTutorialActive() && !map.equals("TOWER")) { enqueue("intro_potion"); enqueue("intro_swap"); enqueue("intro_skills"); }
        if (wild && gv.player.getInventory().getOrDefault("Pokébola", 0) <= 0 && !gv.tutorial.isTutorialActive()) enqueue("noballs");
        if (gv.companion.getHp() > 0 && gv.companion.getHp() < gv.companion.getMaxHp() * 0.25f) enqueue("lowhp");

        if (st != UIManager.UIState.EXPLORING || gv.ui.isDialogueActive || gv.tutorial.isTutorialActive() || queue.isEmpty()) return;
        String key = queue.poll();
        gv.activity.dbHelper.setKv("tip_" + key, 1);
        gv.ui.isDialogueActive = true;
        gv.ui.currentSpeaker = "💡 Dica";
        gv.ui.currentDialogue = TEXT.get(key);
    }
}
