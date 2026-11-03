package com.example.pokeworld.core;

import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.pokeworld.R;
import com.example.pokeworld.entities.CapturedCreature;
import com.example.pokeworld.entities.Companion;
import com.example.pokeworld.entities.PetData;
import com.example.pokeworld.entities.Player;
import com.example.pokeworld.managers.SpriteManager;
import com.example.pokeworld.ui.UIManager;

import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    public GameView gameView;
    public DatabaseHelper dbHelper;
    private boolean isInGame = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            dbHelper = new DatabaseHelper(this);
            showHomeScreen();
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Erro no onCreate: " + e.getMessage(), e);
        }
    }

    private final android.os.Handler animHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable animRunnable;
    private int homeAnimFrame = 0;
    private ImageView ivPlayerAvatar;
    private final List<ImageView> partyPokemonImageViews = new java.util.ArrayList<>();
    private final List<CapturedCreature> partyCreaturesList = new java.util.ArrayList<>();

    public void showHomeScreen() {
        setContentView(R.layout.activity_main);
        isInGame = false;
        stopHomeAnimation();

        SpriteManager.getInstance().loadSprites(this);

        int[] pStats = dbHelper.getPlayerStats();
        int gold = pStats[0];
        int level = pStats[1];
        int exp = pStats[2];
        int gems = pStats[4];

        TextView tvLevel = findViewById(R.id.tv_level);
        if (tvLevel != null) tvLevel.setText("⭐ NÍVEL " + level + " | EXP: " + exp);

        TextView tvGold = findViewById(R.id.tv_gold);
        if (tvGold != null) tvGold.setText("🪙 " + gold + "   💎 " + gems);

        ivPlayerAvatar = findViewById(R.id.iv_player_avatar);
        updatePlayerHomeSprite();

        LinearLayout pokemonContainer = findViewById(R.id.layout_pokemon_list);
        partyPokemonImageViews.clear();
        partyCreaturesList.clear();

        if (pokemonContainer != null) {
            pokemonContainer.removeAllViews();
            List<CapturedCreature> collection = dbHelper.getCollection();
            for (CapturedCreature c : collection) {
                if (c.isInParty) {
                    partyCreaturesList.add(c);

                    LinearLayout pokeItem = new LinearLayout(this);
                    pokeItem.setOrientation(LinearLayout.VERTICAL);
                    pokeItem.setGravity(android.view.Gravity.CENTER);
                    pokeItem.setPadding(15, 10, 15, 10);

                    ImageView pokeImg = new ImageView(this);
                    pokeImg.setLayoutParams(new LinearLayout.LayoutParams(110, 110));
                    partyPokemonImageViews.add(pokeImg);

                    android.graphics.Bitmap pSprite = SpriteManager.getInstance().getUnitSprite(c.name, 0, 0, c.isShiny);
                    if (pSprite != null) {
                        pokeImg.setImageBitmap(pSprite);
                    } else {
                        pokeImg.setImageResource(android.R.drawable.ic_menu_help);
                    }
                    pokeItem.addView(pokeImg);

                    TextView pokeTxt = new TextView(this);
                    pokeTxt.setText(c.name + (c.isShiny ? " ✨" : ""));
                    pokeTxt.setTextColor(Color.WHITE);
                    pokeTxt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
                    pokeItem.addView(pokeTxt);

                    pokemonContainer.addView(pokeItem);
                }
            }
        }

        Button btnMapa = findViewById(R.id.btn_mapa);
        if (btnMapa != null) btnMapa.setOnClickListener(v -> {
            stopHomeAnimation();
            startWorld();
        });

        View btnInv = findViewById(R.id.btn_home_inventory);
        if (btnInv != null) btnInv.setOnClickListener(v -> {
            stopHomeAnimation();
            startWorld();
            if (gameView != null && gameView.ui != null) gameView.ui.state = UIManager.UIState.INVENTORY;
        });

        View btnCreatures = findViewById(R.id.btn_home_creatures);
        if (btnCreatures != null) btnCreatures.setOnClickListener(v -> {
            stopHomeAnimation();
            startWorld();
            if (gameView != null && gameView.ui != null) gameView.ui.state = UIManager.UIState.CREATURES;
        });

        View btnConfig = findViewById(R.id.btn_home_config);
        if (btnConfig != null) btnConfig.setOnClickListener(v -> {
            stopHomeAnimation();
            startWorld();
            if (gameView != null && gameView.ui != null) gameView.ui.state = UIManager.UIState.SETTINGS;
        });

        startHomeAnimation();
    }

    private void updatePlayerHomeSprite() {
        if (ivPlayerAvatar != null) {
            android.graphics.Bitmap playerSprite = SpriteManager.getInstance().getUnitSprite("personagem", 0, homeAnimFrame, false);
            if (playerSprite != null) {
                ivPlayerAvatar.setImageBitmap(playerSprite);
            }
        }
    }

    private void startHomeAnimation() {
        animRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    homeAnimFrame = (homeAnimFrame + 1) % 3;
                    updatePlayerHomeSprite();

                    for (int i = 0; i < partyPokemonImageViews.size(); i++) {
                        if (i < partyCreaturesList.size()) {
                            ImageView img = partyPokemonImageViews.get(i);
                            CapturedCreature c = partyCreaturesList.get(i);
                            if (img != null && c != null) {
                                android.graphics.Bitmap pSprite = SpriteManager.getInstance().getUnitSprite(c.name, 0, homeAnimFrame, c.isShiny);
                                if (pSprite != null) {
                                    img.setImageBitmap(pSprite);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    android.util.Log.e("MainActivity", "Erro na animacao da home: " + e.getMessage());
                }

                animHandler.postDelayed(this, 300);
            }
        };
        animHandler.postDelayed(animRunnable, 300);
    }

    private void stopHomeAnimation() {
        if (animRunnable != null) {
            animHandler.removeCallbacks(animRunnable);
            animRunnable = null;
        }
    }

    private void startWorld() {
        setContentView(R.layout.activity_game);

        int[] pStats = dbHelper.getPlayerStats();
        List<CapturedCreature> collection = dbHelper.getCollection();
        Map<String, Integer> inventory = dbHelper.getInventory();
        String species = dbHelper.getActiveSpecies();
        String clan = dbHelper.getClan();

        PetData petData = dbHelper.getPetData(species);

        gameView = new GameView(this, pStats[0], collection, inventory, species, petData);
        gameView.player.loadProgress(pStats[1], pStats[2], pStats[3], pStats[4], clan, pStats[10]);
        gameView.player.setFishingStats(pStats[7], pStats[8]);
        gameView.aincrad.setMaxFloorReached(pStats[9]);

        isInGame = true;
        hideSystemUI();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopHomeAnimation();
        if (isInGame && gameView != null) {
            saveCurrentGame();
        }
    }

    public void saveCurrentGame() {
        if (gameView == null || gameView.player == null) return;
        
        Player p = gameView.player;
        Companion c = gameView.companion;
        
        // Salva o estado global do jogador (Gold, Lvl, Exp, Inventário, etc.)
        saveGame(p.getGold(), p.getLevel(), p.getExp(), p.getTalentPoints(), p.getGems(), 
                p.getClan(), p.getNgPlusLevel(), p.getSkinColor(), p.getTalents(), p.getCreatureCollection(), p.getInventory(),
                p.getFishingLevel(), p.getFishingExp(), gameView.aincrad.getMaxFloorReached(), p.hasSeenDaycareIntro() ? 1 : 0);
        
        if (c != null) {
            // Salva os dados específicos do pet ativo
            dbHelper.savePet(
                c.getSpecies(),
                c.getLevel(),
                c.getExp(),
                c.getForgeAttackBonus(),
                c.getWeapon() != null ? c.getWeapon().getName() : "Nenhum",
                c.getArmor() != null ? c.getArmor().getName() : "Nenhum",
                c.isShiny(),
                c.getPersonality(),
                c.getRune() != null ? c.getRune().getName() : "Nenhum",
                c.getFriendship(),
                c.getType2() != null ? c.getType2().name() : "Nenhum",
                c.getRarity(),
                c.getGender()
            );
            dbHelper.updateActiveSpecies(c.getSpecies());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void hideSystemUI() {
        if (getSupportActionBar() != null) getSupportActionBar().hide();
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                        View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    public void saveGame(int gold, int level, int exp, int talentPoints, int gems, String clan, int ngPlus, int skinColor, Map<String, Integer> talents, List<CapturedCreature> collection, Map<String, Integer> inventory, int fishingLevel, int fishingExp, int aincrad, int daycareIntro) {
        dbHelper.savePlayerState(gold, level, exp, talentPoints, gems, clan, ngPlus, skinColor, talents, collection, inventory, fishingLevel, fishingExp, aincrad, daycareIntro);
    }
}
