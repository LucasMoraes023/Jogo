package br.com.lucasmoraes.magosupremo;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Locale;

public final class MainActivity extends Activity {
    private static final int GOLD = Color.rgb(231, 199, 106);
    private static final int PURPLE = Color.rgb(112, 77, 170);
    private static final int BLUE = Color.rgb(62, 111, 177);
    private static final int PANEL = Color.rgb(22, 21, 34);

    private GameDatabase database;
    private LinearLayout content;
    private LinearLayout navigationItems;
    private TextView headerSubtitle;
    private String userId;
    private int selectedBattleCard;
    private int enemyHealth;
    private int playerHealth;
    private int battleEnemyAttack;
    private boolean campaignBattle;
    private String battleTitle;
    private boolean battleFinished;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        database = new GameDatabase(this);
        content = findViewById(R.id.content);
        navigationItems = findViewById(R.id.navigationItems);
        headerSubtitle = findViewById(R.id.headerSubtitle);
        if (Build.VERSION.SDK_INT >= 35) {
            View root = findViewById(R.id.screenRoot);
            root.setOnApplyWindowInsetsListener((view, insets) -> {
                view.setPadding(view.getPaddingLeft(), insets.getSystemWindowInsetTop(),
                        view.getPaddingRight(), insets.getSystemWindowInsetBottom());
                return insets;
            });
            root.requestApplyInsets();
        }

        SharedPreferences preferences = getSharedPreferences("mago-session", MODE_PRIVATE);
        String savedId = preferences.getString("user_id", null);
        if (savedId != null && database.getPlayer(savedId) != null) {
            userId = savedId;
            showGame("home");
        } else {
            showLogin();
        }
    }

    @Override
    protected void onDestroy() {
        database.close();
        super.onDestroy();
    }

    private void showLogin() {
        findViewById(R.id.navigation).setVisibility(View.GONE);
        headerSubtitle.setText("Desperte o poder que dorme nas sombras");
        content.removeAllViews();
        addSpace(22);
        addText("UM REINO À BEIRA DO ABISMO", 13, GOLD, true, Gravity.CENTER);
        addText("Entre na ordem arcana ou crie sua conta para iniciar a jornada.", 15,
                Color.LTGRAY, false, Gravity.CENTER);
        addSpace(14);
        EditText accountId = input("ID da conta (6 dígitos)", InputType.TYPE_CLASS_NUMBER);
        EditText password = input("Senha", InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        addView(accountId);
        addView(password);
        addSpace(10);
        addButton("ENTRAR", GOLD, () -> {
            String id = accountId.getText().toString().trim();
            GameDatabase.Player player = database.authenticate(id, password.getText().toString());
            if (player == null) {
                toast("ID ou senha incorretos.");
                return;
            }
            login(player.id);
        });
        addButton("CRIAR CONTA", PURPLE, () -> showRegistration());
        addSpace(16);
        addPanel("ADMIN MASTER", "A conta mestre protegida usa o ID reservado 000000."
                + "\nAs credenciais iniciais estão descritas no README do projeto.");
    }

    private void showRegistration() {
        content.removeAllViews();
        addSpace(20);
        addText("NOVA CONTA", 23, GOLD, true, Gravity.CENTER);
        addText("Seu ID exclusivo de seis dígitos será gerado automaticamente.", 14,
                Color.LTGRAY, false, Gravity.CENTER);
        EditText name = input("Nome do mago", InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        EditText password = input("Senha (mínimo 6 caracteres)",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        addView(name);
        addView(password);
        addButton("CRIAR E COMEÇAR", GOLD, () -> {
            try {
                String id = database.register(name.getText().toString(), password.getText().toString());
                toast("Conta criada! Seu ID é " + id);
                login(id);
            } catch (IllegalArgumentException | IllegalStateException error) {
                toast(error.getMessage());
            }
        });
        addButton("VOLTAR", BLUE, this::showLogin);
    }

    private void login(String id) {
        userId = id;
        getSharedPreferences("mago-session", MODE_PRIVATE).edit()
                .putString("user_id", id).apply();
        showGame("home");
    }

    private void showGame(String screen) {
        findViewById(R.id.navigation).setVisibility(View.VISIBLE);
        headerSubtitle.setText("Conta " + userId + "  •  " + database.getPlayer(userId).name);
        buildNavigation();
        content.removeAllViews();
        switch (screen) {
            case "cards": showCards(); break;
            case "battle": showBattleLobby(); break;
            case "progress": showProgress(); break;
            case "shop": showShop(); break;
            case "admin": showAdmin(); break;
            case "about": showAbout(); break;
            default: showHome();
        }
    }

    private void buildNavigation() {
        navigationItems.removeAllViews();
        String[][] tabs = {{"⌂ Início", "home"}, {"✧ Cartas", "cards"},
                {"⚔ Batalha", "battle"}, {"♜ Progresso", "progress"},
                {"◈ Loja", "shop"}, {"ⓘ Sobre", "about"}};
        for (String[] tab : tabs) {
            Button button = smallButton(tab[0], PURPLE, () -> showGame(tab[1]));
            navigationItems.addView(button);
        }
        GameDatabase.Player player = database.getPlayer(userId);
        if (player != null && player.admin) {
            navigationItems.addView(smallButton("⚙ Admin", BLUE, () -> showGame("admin")));
        }
        navigationItems.addView(smallButton("Sair", Color.DKGRAY, () -> {
            getSharedPreferences("mago-session", MODE_PRIVATE).edit().clear().apply();
            userId = null;
            showLogin();
        }));
    }

    private void showHome() {
        GameDatabase.Player player = database.getPlayer(userId);
        addSpace(4);
        addText("AS CRÔNICAS DO ARCANO", 22, GOLD, true, Gravity.CENTER);
        addText("A magia antiga está despertando. Escreva seu destino.", 14,
                Color.LTGRAY, false, Gravity.CENTER);
        addSpace(12);
        addPanel("RECURSOS", "🪙 " + player.coins + " moedas    🔮 " + player.crystals
                + " cristais\nNível de mago " + (1 + player.xp / 200) + "  •  XP " + player.xp
                + "\nCampanha: capítulo " + player.stage + "  •  Arena: " + player.wins
                + " vitórias / " + player.losses + " derrotas");
        addSpace(10);
        addButton("⚔  INICIAR JORNADA", GOLD, () -> showGame("battle"));
        addButton("✧  ABRIR COLEÇÃO", PURPLE, () -> showGame("cards"));
        addButton("◈  VISITAR A LOJA", BLUE, () -> showGame("shop"));
        addSpace(8);
        addPanel("SUA PRIMEIRA MISSÃO", "Reúna cartas, evolua seus campeões com duplicatas "
                + "e vença batalhas por turnos para conquistar o reino.");
    }

    private void showCards() {
        List<GameDatabase.GameCard> cards = database.getCards(userId, true);
        addText("GRIMÓRIO DE CARTAS", 21, GOLD, true, Gravity.CENTER);
        addText("Coleção: " + cards.size() + " / " + database.getCardCount()
                + " cartas descobertas", 14, Color.LTGRAY, false, Gravity.CENTER);
        addSpace(8);
        if (cards.isEmpty()) {
            addPanel("Grimório vazio", "Abra um pacote na loja para receber suas primeiras cartas.");
        }
        for (GameDatabase.GameCard card : cards) {
            addCardPanel(card);
        }
        addSpace(8);
        addText("As duplicatas são guardadas. Com 3 cópias, evolua uma carta para aumentar seu nível.",
                13, Color.LTGRAY, false, Gravity.CENTER);
    }

    private void addCardPanel(GameDatabase.GameCard card) {
        LinearLayout panel = panel();
        panel.setOrientation(LinearLayout.VERTICAL);
        TextView title = styledText("✦ " + card.name + "  •  " + card.rarity, 16, rarityColor(card.rarity), true);
        panel.addView(title);
        panel.addView(styledText(card.element + "  |  ATQ " + (card.attack + (card.level - 1) * 3)
                + "  |  DEF " + (card.defense + (card.level - 1) * 2) + "  |  Nv. " + card.level
                + "  |  ×" + card.quantity, 13, Color.WHITE, false));
        if (card.quantity >= 3) {
            Button evolve = smallButton("EVOLUIR (3 cópias)", GOLD,
                    () -> {
                        if (database.evolveCard(userId, card.id)) {
                            toast(card.name + " evoluiu!");
                            showGame("cards");
                        } else toast("Não há duplicatas suficientes.");
                    });
            panel.addView(evolve);
        }
        addView(panel);
    }

    private void showBattleLobby() {
        GameDatabase.Player player = database.getPlayer(userId);
        addText("CAMPO DE BATALHA", 22, GOLD, true, Gravity.CENTER);
        addText("Escolha uma frente. Os combates acontecem por turnos.", 14, Color.LTGRAY,
                false, Gravity.CENTER);
        addSpace(12);
        addPanel("CAMPANHA • CAPÍTULO " + player.stage,
                "Enfrente guardiões cada vez mais fortes.\nVitória: 75 moedas"
                        + (player.stage % 5 == 0 ? " + 2 cristais." : "."));
        addButton("⚔  LUTAR NA CAMPANHA", PURPLE, () -> startBattle(true));
        addSpace(4);
        addPanel("ARENA", "Desafie um rival arcano. Vitórias contam para missões e conquistas.\n"
                + "Vitória: 100 moedas.");
        addButton("⚔  ENTRAR NA ARENA", BLUE, () -> startBattle(false));
        addSpace(8);
        addPanel("COMO LUTAR", "Toque em ATACAR no seu turno. Seu campeão causa dano baseado "
                + "no ataque da carta; o inimigo contra-ataca enquanto estiver de pé.");
    }

    private void startBattle(boolean campaign) {
        List<GameDatabase.GameCard> owned = database.getCards(userId, true);
        if (owned.isEmpty()) {
            toast("Abra um pacote antes de lutar.");
            showGame("shop");
            return;
        }
        GameDatabase.GameCard strongest = owned.get(0);
        for (GameDatabase.GameCard card : owned) {
            if (card.attack + card.defense + card.level * 5
                    > strongest.attack + strongest.defense + strongest.level * 5) strongest = card;
        }
        selectedBattleCard = strongest.id;
        GameDatabase.Player player = database.getPlayer(userId);
        campaignBattle = campaign;
        battleTitle = campaign ? "Guardião do capítulo " + player.stage : "Rival da arena";
        playerHealth = 85 + strongest.defense + strongest.level * 3;
        enemyHealth = campaign ? 70 + player.stage * 12 : 110;
        battleEnemyAttack = campaign ? 9 + player.stage * 2 : 17;
        battleFinished = false;
        showBattleTurn(strongest);
    }

    private void showBattleTurn(GameDatabase.GameCard card) {
        content.removeAllViews();
        addText(campaignBattle ? "CAMPANHA" : "ARENA", 12, BLUE, true, Gravity.CENTER);
        addText(battleTitle, 22, GOLD, true, Gravity.CENTER);
        addSpace(12);
        addPanel(battleTitle.toUpperCase(Locale.ROOT), "❤ Vida inimiga: " + Math.max(0, enemyHealth)
                + "\n⚔ Ataque: " + battleEnemyAttack);
        addPanel(card.name + " • " + card.rarity, "❤ Sua vida: " + Math.max(0, playerHealth)
                + "\nATQ " + card.attack + "  •  DEF " + card.defense + "  •  Nv. " + card.level);
        if (battleFinished) {
            boolean won = enemyHealth <= 0;
            addText(won ? "VITÓRIA ARCANA!" : "DERROTA", 24, won ? GOLD : Color.LTGRAY,
                    true, Gravity.CENTER);
            addText(won ? "O reino lembrará deste feito." : "Reagrupe-se e tente novamente.",
                    14, Color.LTGRAY, false, Gravity.CENTER);
            addButton("CONTINUAR", PURPLE, () -> showGame("battle"));
            return;
        }
        addButton("⚔  ATACAR", GOLD, () -> takeTurn(card));
        addButton("RECUAR", Color.DKGRAY, () -> showGame("battle"));
    }

    private void takeTurn(GameDatabase.GameCard card) {
        int attack = card.attack + Math.max(0, card.level - 1) * 3;
        enemyHealth -= Math.max(7, attack - battleEnemyAttack / 3);
        if (enemyHealth > 0) playerHealth -= Math.max(4, battleEnemyAttack - card.defense / 4);
        if (enemyHealth <= 0 || playerHealth <= 0) {
            boolean won = enemyHealth <= 0;
            battleFinished = true;
            database.completeBattle(userId, campaignBattle, won);
            showBattleTurn(card);
        } else {
            showBattleTurn(card);
        }
    }

    private void showProgress() {
        GameDatabase.Player player = database.getPlayer(userId);
        addText("JORNADA DO MAGO", 22, GOLD, true, Gravity.CENTER);
        addPanel("CAMPANHA", "Capítulo atual: " + player.stage
                + "\nA cada 5 capítulos vencidos, ganhe cristais adicionais.");
        addPanel("ARENA", "Vitórias: " + player.wins + "  •  Derrotas: " + player.losses
                + "\nExperiência acumulada: " + player.xp + " XP");
        addText("MISSÕES", 18, GOLD, true, Gravity.CENTER);
        for (String mission : database.getMissionStatus(userId)) {
            String[] fields = mission.split("\\|");
            boolean ready = "1".equals(fields[2]);
            boolean claimed = "1".equals(fields[3]);
            addPanel(fields[1], claimed ? "Recompensa resgatada."
                    : ready ? "Concluída! Recompensa: 150 moedas + 3 cristais."
                    : "Complete a atividade para receber sua recompensa.");
            if (ready && !claimed) {
                addButton("RESGATAR RECOMPENSA", GOLD, () -> {
                    if (database.claimMission(userId, fields[0])) {
                        toast("Missão resgatada!");
                        showGame("progress");
                    } else toast("Esta missão ainda não pode ser resgatada.");
                });
            }
        }
        addText("CONQUISTAS", 18, GOLD, true, Gravity.CENTER);
        for (String achievement : database.getAchievementStatus(userId)) {
            String[] fields = achievement.split("\\|");
            addPanel((Boolean.parseBoolean(fields[1]) ? "🏆 " : "🔒 ") + fields[0],
                    Boolean.parseBoolean(fields[1]) ? "Conquista desbloqueada."
                            : "Continue sua jornada para desbloquear.");
        }
    }

    private void showShop() {
        GameDatabase.Player player = database.getPlayer(userId);
        addText("MERCADO ARCANO", 22, GOLD, true, Gravity.CENTER);
        addPanel("SEUS RECURSOS", "🪙 " + player.coins + " moedas\n🔮 " + player.crystals + " cristais");
        addPanel("PACOTE MÍSTICO", "Uma carta aleatória. A chance melhora entre as 9 raridades."
                + "\nPreço: 100 moedas.");
        addButton("ABRIR COM MOEDAS", GOLD, () -> openPack(false));
        addButton("ABRIR COM CRISTAIS • 5", PURPLE, () -> openPack(true));
        addSpace(8);
        addPanel("CÂMBIO", "Troque 300 moedas por 10 cristais.");
        addButton("TROCAR MOEDAS POR CRISTAIS", BLUE, () -> {
            if (database.exchangeCoinsForCrystals(userId)) {
                toast("Câmbio realizado.");
                showGame("shop");
            } else toast("Você precisa de 300 moedas.");
        });
        addText("RARIDADES", 17, GOLD, true, Gravity.CENTER);
        addText("Comum  •  Incomum  •  Raro  •  Épico  •  Lendário\n"
                + "Mítico  •  Ancestral  •  Celestial  •  Supremo",
                14, Color.LTGRAY, false, Gravity.CENTER);
    }

    private void openPack(boolean crystals) {
        try {
            GameDatabase.GameCard card = database.openPack(userId, crystals);
            if (card == null) {
                toast(crystals ? "Cristais insuficientes." : "Moedas insuficientes.");
                return;
            }
            String message = "Nova carta: " + card.name + " • " + card.rarity
                    + (card.quantity > 1 ? "\nDuplicata recebida. Total: " + card.quantity : "");
            new android.app.AlertDialog.Builder(this).setTitle("✧ Carta revelada ✧")
                    .setMessage(message).setPositiveButton("MAGNÍFICO", (dialog, which) -> showGame("shop"))
                    .show();
        } catch (RuntimeException error) {
            toast("Não foi possível abrir o pacote: " + error.getMessage());
        }
    }

    private void showAdmin() {
        GameDatabase.Player player = database.getPlayer(userId);
        if (player == null || !player.admin) {
            toast("Acesso administrativo negado.");
            showGame("home");
            return;
        }
        addText("PAINEL ADMIN MASTER", 21, GOLD, true, Gravity.CENTER);
        addPanel("CONTROLE LOCAL", "Contas registradas (máximo 50 recentes). "
                + "O ID 000000 da conta ADMIN MASTER é reservado e protegido.");
        for (String entry : database.getUsersForAdmin()) {
            String[] fields = entry.split("\\|");
            addPanel(fields[1] + (fields[4].equals("1") ? " • ADMIN" : ""),
                    "ID " + fields[0] + "  |  " + fields[2] + " moedas  |  "
                            + fields[3] + " cristais");
            if (fields[4].equals("0")) {
                addButton("CONCEDER 500 MOEDAS • " + fields[0], BLUE, () -> {
                    if (database.grantAdminCoins(userId, fields[0], 500)) {
                        toast("Crédito administrativo concedido.");
                        showGame("admin");
                    } else toast("Não foi possível alterar esta conta.");
                });
            }
        }
    }

    private void showAbout() {
        addSpace(26);
        addText("MAGO SUPREMO", 25, GOLD, true, Gravity.CENTER);
        addText("Um card game de fantasia sombria, criado para jogar offline.", 15,
                Color.LTGRAY, false, Gravity.CENTER);
        addSpace(22);
        addPanel("A JORNADA", "Colecione criaturas arcanas, construa seu grimório, "
                + "evolua suas cartas e enfrente os desafios da campanha e arena.");
        addPanel("MUNDO OFFLINE", "Contas, coleção e progresso ficam salvos localmente "
                + "no banco SQLite deste dispositivo.");
        addPanel("TECNOLOGIA", "Android nativo • Java • Views/XML • SQLite");
        addSpace(20);
        addText("Criado por Lucas Moraes", 16, GOLD, true, Gravity.CENTER);
    }

    private void addText(String text, int size, int color, boolean bold, int gravity) {
        TextView view = styledText(text, size, color, bold);
        view.setGravity(gravity);
        view.setPadding(8, 8, 8, 8);
        addView(view);
    }

    private TextView styledText(String text, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private void addPanel(String title, String body) {
        LinearLayout panel = panel();
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.addView(styledText(title, 15, GOLD, true));
        TextView details = styledText(body, 14, Color.rgb(219, 216, 230), false);
        details.setPadding(0, 5, 0, 0);
        panel.addView(details);
        addView(panel);
    }

    private LinearLayout panel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setPadding(14, 12, 14, 12);
        GradientDrawable background = new GradientDrawable();
        background.setColor(PANEL);
        background.setCornerRadius(dp(12));
        background.setStroke(dp(1), Color.rgb(62, 53, 87));
        panel.setBackground(background);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(5), 0, dp(5));
        panel.setLayoutParams(params);
        return panel;
    }

    private void addButton(String label, int color, Runnable action) {
        Button button = smallButton(label, color, action);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(50));
        params.setMargins(0, dp(5), 0, dp(5));
        content.addView(button, params);
    }

    private Button smallButton(String label, int color, Runnable action) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(color == GOLD ? Color.rgb(24, 19, 12) : Color.WHITE);
        button.setTextSize(12);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setAllCaps(false);
        button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
        button.setOnClickListener(view -> action.run());
        return button;
    }

    private EditText input(String hint, int inputType) {
        EditText field = new EditText(this);
        field.setSingleLine(true);
        field.setHint(hint);
        field.setHintTextColor(Color.GRAY);
        field.setTextColor(Color.WHITE);
        field.setInputType(inputType);
        field.setPadding(dp(14), dp(8), dp(14), dp(8));
        GradientDrawable background = new GradientDrawable();
        background.setColor(PANEL);
        background.setCornerRadius(dp(9));
        background.setStroke(dp(1), Color.rgb(90, 76, 113));
        field.setBackground(background);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(54));
        params.setMargins(0, dp(5), 0, dp(5));
        field.setLayoutParams(params);
        return field;
    }

    private void addView(View view) {
        content.addView(view);
    }

    private void addSpace(int height) {
        View space = new View(this);
        content.addView(space, new LinearLayout.LayoutParams(1, dp(height)));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private int rarityColor(String rarity) {
        switch (rarity) {
            case "Incomum": return Color.rgb(112, 202, 133);
            case "Raro": return BLUE;
            case "Épico": return Color.rgb(177, 118, 226);
            case "Lendário": return GOLD;
            case "Mítico": return Color.rgb(255, 128, 80);
            case "Ancestral": return Color.rgb(255, 100, 164);
            case "Celestial": return Color.rgb(136, 214, 255);
            case "Supremo": return Color.WHITE;
            default: return Color.LTGRAY;
        }
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
