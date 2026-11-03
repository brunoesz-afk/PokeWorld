# Reverter para 2D no PokeWorld

O objetivo é remover todas as dependências e lógicas 3D (SceneView, ModelNode, GLB) e voltar a usar o sistema de desenho 2D via Canvas, que já possui fundamentos no projeto (TileMap, SpriteManager).

## Mudanças Propostas

### UI e Atividades

#### [MODIFY] [activity_game.xml](file:///C:/Users/Bruno/Desktop/AndroidStudioProjects/PokeWorld/app/src/main/res/layout/activity_game.xml)
- Confirmar que não há elementos de `SceneView`.
- Ajustar `GameInterfaceOverlay` para ocupar a tela inteira se necessário.

#### [MODIFY] [MainActivity.java](file:///C:/Users/Bruno/Desktop/AndroidStudioProjects/PokeWorld/app/src/main/java/com/example/pokeworld/MainActivity.java)
- Remover referências a `io.github.sceneview.SceneView`.
- Atualizar `startWorld()` para inicializar o `GameView` sem o parâmetro `SceneView`.
- Remover chamadas de ciclo de vida para o `sceneView`.

### Lógica do Jogo

#### [MODIFY] [GameView.java](file:///C:/Users/Bruno/Desktop/AndroidStudioProjects/PokeWorld/app/src/main/java/com/example/pokeworld/GameView.java)
- Remover todos os imports de `sceneview`, `kotlin.math`, `ModelNode`, `CameraNode`.
- Remover o campo `sceneView` e o parâmetro do construtor.
- Remover `setup3DScene()`.
- Mudar o sistema de `update()` para ser disparado pelo `GameInterfaceOverlay` ou por um loop 2D próprio.
- Adicionar um método `draw(Canvas canvas)` que coordena o desenho de todo o mundo.

#### [MODIFY] [WorldManager.java](file:///C:/Users/Bruno/Desktop/AndroidStudioProjects/PokeWorld/app/src/main/java/com/example/pokeworld/WorldManager.java)
- Remover lógica de `ModelNode` e billboarding (`updateSpriteNode`).
- Implementar `draw(Canvas canvas)` para desenhar o mapa e todas as entidades.
- Limpar `loadMap` de referências 3D.

#### [MODIFY] [GameInterfaceOverlay.java](file:///C:/Users/Bruno/Desktop/AndroidStudioProjects/PokeWorld/app/src/main/java/com/example/pokeworld/GameInterfaceOverlay.java)
- Atualizar `onDraw` para chamar `gameView.draw(canvas)` antes de desenhar a UI.
- Garantir que o loop de atualização (`gameView.update()`) seja chamado.

### Entidades (Sprites e Desenho)

#### [MODIFY] [Player.java](file:///C:/Users/Bruno/Desktop/AndroidStudioProjects/PokeWorld/app/src/main/java/com/example/pokeworld/Player.java), [Companion.java](file:///C:/Users/Bruno/Desktop/AndroidStudioProjects/PokeWorld/app/src/main/java/com/example/pokeworld/Companion.java), [Enemy.java](file:///C:/Users/Bruno/Desktop/AndroidStudioProjects/PokeWorld/app/src/main/java/com/example/pokeworld/Enemy.java), [NPC.java](file:///C:/Users/Bruno/Desktop/AndroidStudioProjects/PokeWorld/app/src/main/java/com/example/pokeworld/NPC.java), [Building.java](file:///C:/Users/Bruno/Desktop/AndroidStudioProjects/PokeWorld/app/src/main/java/com/example/pokeworld/Building.java), [Loot.java](file:///C:/Users/Bruno/Desktop/AndroidStudioProjects/PokeWorld/app/src/main/java/com/example/pokeworld/Loot.java)
- Remover métodos `init3D`, `remove3D`, `updateSpriteNode`.
- Adicionar método `draw(Canvas canvas)` utilizando o `SpriteManager` para renderizar o bitmap correto com base na posição e animação.

## Plano de Verificação

### Testes Manuais
1. Compilar o projeto e verificar se não há erros de símbolo (SceneView removido).
2. Abrir o jogo e verificar se o mapa 2D (TileMap) é renderizado.
3. Verificar se o jogador e o companheiro aparecem como sprites 2D.
4. Testar o movimento com o joystick e a transição entre mapas (Cidade/Floresta).
