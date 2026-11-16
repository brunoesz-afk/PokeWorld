# PokeWorld: Registro Completo de Funcionalidades e Notas de Atualização (Release Log)

Este documento centraliza todas as configurações, mecânicas implementadas e notas detalhadas de cada versão do **PokeWorld**, servindo como guia definitivo de funcionalidades para publicação e leitura.

---

## 📌 Sumário de Versões

| Versão | Código | Principais Adições e Mudanças |
| :--- | :--- | :--- |
| **v1.40** | 40 | Correção do tutorial da Enfermeira Joy (ocorre apenas 1x) + Adição do mapa `TOWER` (Torre de Desafios em Ondas) e NPC Mestre da Torre. |
| **v1.38** | 38 | Colisão física sólida contra inimigos/Pokémons (impedindo o jogador de atravessá-los). |
| **v1.36** | 36 | Sistema de separação e repulsão física entre criaturas (*Separation Steering* para evitar sobreposição de tiles). |
| **v1.33 - v1.35** | 33-35 | Conexão multiplayer 100% assíncrona, indicador de status online/conectando na HUD e otimização de thread safety na inicialização. |
| **v1.26 - v1.32** | 26-32 | Conexão inteligente por mapa (Exploração offline padrão, multiplayer social em `CITY` e `HOME`) e suporte a modo offline com botão de reconexão na Splash Screen. |
| **v1.15 - v1.25** | 15-25 | Gerenciador de download de conteúdo OTA silencioso (`ContentDownloader`), otimização de RAM com `RGB_565` e carregamento progressivo em lotes. |
| **v1.10 - v1.14** | 10-14 | Floresta do tutorial inicial (`STARTER_FOREST`), transição automática para a Base (`HOME`), NPC "Prof. de Time" para gerenciamento de Party vs. Depósito e limite de guardas baseado no nível do Treinador. |
| **v1.01 - v1.09** | 1-9 | Sistema de Atualização Automática de APK via FileProvider, enredo épico, missões do Ato I e II, e tela de carregamento (`carregamento.jpg`) com barra de progresso. |

---

## 🛠️ Detalhamento Completo das Funcionalidades Atuais

### 1. Sistema de Progressão e Enredo
* **Ato I ao Ato III**: Missões guiadas divididas por níveis cobrindo desde a captura do primeiro Pokémon na floresta, construção da Oficina (5 segundos), até exploração de cavernas, montanhas nevadas e vulcões.
* **Torre de Desafios (`TOWER`)**: Mapa fechado com ondas contínuas de Pokémons selvagens. A cada onda superada, o jogador sobe de andar e recebe recompensas em Ouro. NPC **Mestre da Torre** localizado na Base Central (`HOME`).

### 2. Mecânicas de Combate, IA e Colisão
* **Colisão Sólida e Repulsão**: Pokémons selvagens e aliados ocupam espaço físico real no mapa (*tile*), impedindo que o jogador atravesse por dentro deles. Sistema de *Separation Steering* evita que criaturas fiquem sobrepostas.
* **Guarda da Base**: Pokémons deixados no Depósito ganham o status de `🛡️ Guarda`, patrulhando e defendendo a base automaticamente contra invasores. O limite de guardas escala com o nível do Treinador (Nível 1-10: 6, Nível 11-20: 10, Nível 21-30: 15, Nível 30+: +1/nível).
* **Tutorial Guiado & Enfermeira Joy**: Batalha inicial facilitada contra o Rattata do tutorial (dano de 2 HP). O evento de resgate da Enfermeira Joy ocorre **apenas uma vez** no primeiro desmaio do Pokémon, restaurando o time em 100% e teleportando para a Cidade.

### 3. Multiplayer e Otimizações Técnicas
* **Multiplayer Inteligente por Mapa**: Exploração nos mapas de campo e dungeons roda totalmente local e offline para máxima fluidez. A conexão com o Socket.IO (`https://pokeworld-1-nmva.onrender.com`) ativa automaticamente em segundo plano ao entrar na Cidade (`CITY`) ou Base (`HOME`).
* **Desempenho Extremo (60+ FPS)**: Culling de visão (*Viewport Culling* desenha apenas os ~200 tiles visíveis em vez de 12.800), reutilização de instâncias `Rect`, renderização em `RGB_565` (50% menos consumo de RAM) e carregamento progressivo em lotes para evitar OOM em celulares intermediários.
* **Atualizações OTA Silenciosas**: Sincronização automática de dados e status na inicialização com indicador visual na tela de carregamento (*Splash Screen*) e botão de reconexão offline.
