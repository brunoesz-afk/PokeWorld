# PokeWorld: Registro Completo de Funcionalidades e Notas de Atualização (Release Log)

Este documento centraliza todas as configurações, mecânicas implementadas e notas detalhadas de cada versão do **PokeWorld**, servindo como guia definitivo de funcionalidades para publicação e leitura.

---

## 📌 Sumário de Versões

| Versão | Código | Principais Adições e Mudanças |
| :--- | :--- | :--- |
| **v1.62** | 62 | Integração e fatiamento automático do `tileset_volcano_4x4` (piso de basalto, solo de cinzas, obsidiana rachada, ponte de magma e cristais) para o mapa `MOLTEN_SANCTUM`. |
| **v1.61** | 61 | Criação do mapa `MOLTEN_SANCTUM_BEGINNER` (Santuário de Magma Iniciante - Nível 10+) com Boss Charmeleon (Nv. 15), mantendo o layout do mapa de Nível 50+. |
| **v1.60** | 60 | Posicionamento do Seletor Rápido de Equipe do Time no lado direito da tela, exatamente à esquerda do Minimapa. |
| **v1.58 - v1.59** | 58-59 | Sincronização e download automático de save da nuvem Supabase na abertura do app + Escolha clara de modo Online x Offline na tela inicial. |
| **v1.55 - v1.57** | 55-57 | Redesign do Menu de Criaturas (`🐾`) com animação lenta em movimento, novo mapa `MOLTEN_SANCTUM` (Santuário de Magma) com Arena de Boss Mega Charizard X e portas de lava animadas. |
| **v1.53 - v1.54** | 53-54 | Sistema de 3 Hordas/Rounds por andar na Torre, Portal `aincrad_portal` no chão ao vencer o andar e Seleção de Andares da Torre com Zoom e Paginação (1-10 e 11-20). |
| **v1.44 - v1.47** | 44-47 | Conexão com Supabase Auth e alinhamento com as tabelas `player_profiles` e `pokemon_party` do site em tempo real + Botão de Login/Vínculo na tela inicial. |
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

### 1. Sistema de Tilesets Vulcânicos e Novo Mapa `MOLTEN_SANCTUM`
* **Fatiamento 4x4 do Tileset Vulcânico (`SpriteManager.java`)**:
  - `BASALT_FLOOR`: Piso escuro de basalto para travessia.
  - `ASH_GROUND`: Solo de cinzas vulcânicas para spawn de Pokémons de fogo.
  - `CRACKED_OBSIDIAN`: Paredes de obsidiana rachada com veias de lava.
  - `MAGMA_BRIDGE`: Pontes de pedra sobre rios de lava.
  - `SULFUR_CRYSTAL`: Cristais de enxofre decorativos com colisão.
  - `MAGMA_VENT`: Fenda de magma animada 3x1.
* **Duas Versões do Santuário de Magma**:
  - `MOLTEN_SANCTUM_BEGINNER` (Nível 10+): Acessível via portal na Caverna de Brasa (`FIRE_CAVE`) ou Bússola Pág 1.
  - `MOLTEN_SANCTUM` (Nível 50+): Acessível via portal no Vulcão (`VOLCANO`) ou Bússola Pág 3 com Boss Mega Charizard X.

### 2. Sistema de Progressão, Enredo e Seleção de Time
* **Atalho Rápido de Equipe no Minimapa**: Os ícones dos até 6 Pokémons do seu time ficam dispostos verticalmente logo à esquerda do Minimapa (no lado direito da tela). Tocar diretamente no ícone de qualquer Pokémon altera instantaneamente seu companheiro ativo de batalha.
* **Torre de Desafios (`TOWER`)**: Mapa compacto de 20 andares com 3 hordas progressivas por andar. Ao vencer as hordas, o portal `aincrad_portal` aparece no chão para subir ao próximo nível.
* **Ato I ao Ato III**: Missões guiadas divididas por níveis cobrindo desde a captura do primeiro Pokémon na floresta, construção da Oficina (5 segundos), até exploração de cavernas, montanhas nevadas, vulcões e o Santuário de Magma (`MOLTEN_SANCTUM`).

### 3. Integração Supabase & Site
* **Sincronização em Tempo Real**: Conexão nativa com `player_profiles` e `pokemon_party` do Supabase do seu site.
* **Download de Cloud Save**: Ao logar no app, o jogo baixa automaticamente o save mais recente da nuvem antes de carregar o mundo.
* **Modo Offline x Online**: O jogador pode alternar livremente entre jogar online (sincronizado na nuvem) e jogar offline (save local no celular).

### 4. Mecânicas de Combate, IA e Colisão
* **Colisão Sólida e Repulsão**: Pokémons selvagens e aliados ocupam espaço físico real no mapa (*tile*), impedindo que o jogador atravesse por dentro deles. Sistema de *Separation Steering* evita que criaturas fiquem sobrepostas.
* **Guarda da Base**: Pokémons deixados no Depósito ganham o status de `🛡️ Guarda`, patrulhando e defendendo a base automaticamente contra invasores. O limite de guardas escala com o nível do Treinador.
