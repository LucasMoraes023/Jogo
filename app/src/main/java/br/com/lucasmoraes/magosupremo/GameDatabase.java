package br.com.lucasmoraes.magosupremo;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Base64;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

final class GameDatabase extends SQLiteOpenHelper {
    static final String ADMIN_ID = "000000";
    static final String ADMIN_PASSWORD = "Arcano#000000";
    static final String[] RARITIES = {
            "Comum", "Incomum", "Raro", "Épico", "Lendário",
            "Mítico", "Ancestral", "Celestial", "Supremo"
    };
    static final int CARD_COUNT = 144;

    static final class GameCard {
        final int id;
        final String name;
        final String element;
        final String rarity;
        final int attack;
        final int defense;
        final int quantity;
        final int level;

        GameCard(int id, String name, String element, String rarity, int attack, int defense,
                 int quantity, int level) {
            this.id = id;
            this.name = name;
            this.element = element;
            this.rarity = rarity;
            this.attack = attack;
            this.defense = defense;
            this.quantity = quantity;
            this.level = level;
        }
    }

    static final class Player {
        final String id;
        final String name;
        final boolean admin;
        final int coins;
        final int crystals;
        final int stage;
        final int wins;
        final int losses;
        final int xp;

        Player(String id, String name, boolean admin, int coins, int crystals, int stage,
               int wins, int losses, int xp) {
            this.id = id;
            this.name = name;
            this.admin = admin;
            this.coins = coins;
            this.crystals = crystals;
            this.stage = stage;
            this.wins = wins;
            this.losses = losses;
            this.xp = xp;
        }
    }

    private static final String[] ELEMENTS = {
            "Fogo", "Gelo", "Sombra", "Luz", "Tempestade", "Arcano",
            "Natureza", "Abismo", "Astral", "Sangue", "Cristal", "Ruína"
    };
    private static final String[] CARD_NAMES = {
            "Dragão", "Oráculo", "Guardião", "Feiticeira", "Titã", "Corvo",
            "Quimera", "Cavaleiro", "Serpente", "Fênix", "Espectro", "Arquimago"
    };
    private final SecureRandom random = new SecureRandom();

    GameDatabase(Context context) {
        super(context, "mago_supremo.db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE users (id TEXT PRIMARY KEY CHECK(length(id)=6), username TEXT NOT NULL COLLATE NOCASE UNIQUE, salt TEXT NOT NULL, password_hash TEXT NOT NULL, is_admin INTEGER NOT NULL DEFAULT 0, coins INTEGER NOT NULL DEFAULT 0, crystals INTEGER NOT NULL DEFAULT 0, created_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE cards (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, element TEXT NOT NULL, rarity TEXT NOT NULL, attack INTEGER NOT NULL, defense INTEGER NOT NULL, description TEXT NOT NULL)");
        db.execSQL("CREATE TABLE inventory (user_id TEXT NOT NULL, card_id INTEGER NOT NULL, quantity INTEGER NOT NULL DEFAULT 0, level INTEGER NOT NULL DEFAULT 1, PRIMARY KEY(user_id, card_id), FOREIGN KEY(user_id) REFERENCES users(id), FOREIGN KEY(card_id) REFERENCES cards(id))");
        db.execSQL("CREATE TABLE progress (user_id TEXT PRIMARY KEY, campaign_stage INTEGER NOT NULL DEFAULT 1, arena_wins INTEGER NOT NULL DEFAULT 0, arena_losses INTEGER NOT NULL DEFAULT 0, xp INTEGER NOT NULL DEFAULT 0, FOREIGN KEY(user_id) REFERENCES users(id))");
        db.execSQL("CREATE TABLE missions (user_id TEXT NOT NULL, mission_key TEXT NOT NULL, progress INTEGER NOT NULL DEFAULT 0, claimed INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(user_id, mission_key))");
        db.execSQL("CREATE TABLE achievements (user_id TEXT NOT NULL, achievement_key TEXT NOT NULL, unlocked INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(user_id, achievement_key))");
        seedCards(db);
        createAccount(db, ADMIN_ID, "ADMIN MASTER", ADMIN_PASSWORD, true);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Database upgrades are added here when the schema version changes.
    }

    private void seedCards(SQLiteDatabase db) {
        for (int i = 0; i < CARD_COUNT; i++) {
            String element = ELEMENTS[i % ELEMENTS.length];
            String rarity = RARITIES[i % RARITIES.length];
            int tier = i % RARITIES.length;
            ContentValues card = new ContentValues();
            card.put("name", CARD_NAMES[(i / ELEMENTS.length) % CARD_NAMES.length] + " " + element);
            card.put("element", element);
            card.put("rarity", rarity);
            card.put("attack", 8 + tier * 5 + (i % 7));
            card.put("defense", 6 + tier * 4 + (i % 9));
            card.put("description", "Uma entidade " + element.toLowerCase(Locale.ROOT)
                    + " vinculada às crônicas do arcano.");
            db.insertOrThrow("cards", null, card);
        }
    }

    private void createAccount(SQLiteDatabase db, String id, String name, String password,
                               boolean admin) {
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        ContentValues values = new ContentValues();
        values.put("id", id);
        values.put("username", name);
        values.put("salt", Base64.encodeToString(salt, Base64.NO_WRAP));
        values.put("password_hash", passwordHash(password, salt));
        values.put("is_admin", admin ? 1 : 0);
        values.put("coins", admin ? 999999 : 500);
        values.put("crystals", admin ? 999999 : 25);
        values.put("created_at", System.currentTimeMillis());
        db.insertOrThrow("users", null, values);
        ContentValues progress = new ContentValues();
        progress.put("user_id", id);
        db.insertOrThrow("progress", null, progress);
        seedMissions(db, id);
        seedAchievements(db, id);
        if (!admin) {
            grantCard(db, id, 1 + random.nextInt(144));
            grantCard(db, id, 1 + random.nextInt(144));
            grantCard(db, id, 1 + random.nextInt(144));
        }
    }

    private void seedMissions(SQLiteDatabase db, String userId) {
        String[][] missions = {{"campaign", "Vença 1 batalha de campanha"},
                {"arena", "Vença 1 batalha na arena"}, {"packs", "Abra 1 pacote"}};
        for (String[] mission : missions) {
            ContentValues values = new ContentValues();
            values.put("user_id", userId);
            values.put("mission_key", mission[0]);
            db.insert("missions", null, values);
        }
    }

    private void seedAchievements(SQLiteDatabase db, String userId) {
        for (String key : new String[]{"first_battle", "first_pack", "collector_10", "arena_5"}) {
            ContentValues values = new ContentValues();
            values.put("user_id", userId);
            values.put("achievement_key", key);
            db.insert("achievements", null, values);
        }
    }

    String register(String username, String password) {
        String cleanName = username.trim();
        if (cleanName.length() < 3 || cleanName.length() > 18 || password.length() < 6) {
            throw new IllegalArgumentException("Use um nome de 3 a 18 caracteres e senha de ao menos 6.");
        }
        if (cleanName.equalsIgnoreCase("ADMIN MASTER")) {
            throw new IllegalArgumentException("Este nome é reservado para a conta ADMIN MASTER.");
        }
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            try (Cursor existingName = db.query("users", new String[]{"id"}, "username=?",
                    new String[]{cleanName}, null, null, null)) {
                if (existingName.moveToFirst()) {
                    throw new IllegalArgumentException("Este nome de mago já está em uso.");
                }
            }
            for (int attempt = 0; attempt < 100; attempt++) {
                String id = formatAccountId(1 + random.nextInt(999999));
                boolean available;
                try (Cursor existingId = db.query("users", new String[]{"id"}, "id=?",
                        new String[]{id}, null, null, null)) {
                    available = !existingId.moveToFirst();
                }
                if (available) {
                    createAccount(db, id, cleanName, password, false);
                    db.setTransactionSuccessful();
                    return id;
                }
            }
            throw new IllegalStateException("Não foi possível gerar um ID livre. Tente novamente.");
        } finally {
            db.endTransaction();
        }
    }

    static String formatAccountId(int value) {
        if (value < 1 || value > 999999) {
            throw new IllegalArgumentException("O ID deve estar entre 000001 e 999999.");
        }
        return String.format(Locale.ROOT, "%06d", value);
    }

    Player authenticate(String id, String password) {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT u.id,u.username,u.salt,u.password_hash,u.is_admin,u.coins,u.crystals,"
                        + "p.campaign_stage,p.arena_wins,p.arena_losses,p.xp "
                        + "FROM users u JOIN progress p ON p.user_id=u.id WHERE u.id=?",
                new String[]{id.trim()})) {
            if (!cursor.moveToFirst()) return null;
            byte[] salt = Base64.decode(cursor.getString(2), Base64.NO_WRAP);
            if (!passwordHash(password, salt).equals(cursor.getString(3))) return null;
            return playerFromCursor(cursor);
        }
    }

    Player getPlayer(String id) {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT u.id,u.username,u.salt,u.password_hash,u.is_admin,u.coins,u.crystals,"
                        + "p.campaign_stage,p.arena_wins,p.arena_losses,p.xp "
                        + "FROM users u JOIN progress p ON p.user_id=u.id WHERE u.id=?",
                new String[]{id})) {
            return cursor.moveToFirst() ? playerFromCursor(cursor) : null;
        }
    }

    private Player playerFromCursor(Cursor c) {
        return new Player(c.getString(0), c.getString(1), c.getInt(4) == 1, c.getInt(5),
                c.getInt(6), c.getInt(7), c.getInt(8), c.getInt(9), c.getInt(10));
    }

    List<GameCard> getCards(String userId, boolean ownedOnly) {
        List<GameCard> cards = new ArrayList<>();
        String sql = "SELECT c.id,c.name,c.element,c.rarity,c.attack,c.defense,"
                + "COALESCE(i.quantity,0),COALESCE(i.level,1) FROM cards c "
                + "LEFT JOIN inventory i ON c.id=i.card_id AND i.user_id=? "
                + (ownedOnly ? "WHERE COALESCE(i.quantity,0)>0 " : "")
                + "ORDER BY CASE c.rarity " + rarityOrderSql() + " END,c.name";
        try (Cursor cursor = getReadableDatabase().rawQuery(sql, new String[]{userId})) {
            while (cursor.moveToNext()) {
                cards.add(new GameCard(cursor.getInt(0), cursor.getString(1), cursor.getString(2),
                        cursor.getString(3), cursor.getInt(4), cursor.getInt(5), cursor.getInt(6),
                        cursor.getInt(7)));
            }
        }
        return cards;
    }

    int getCardCount() {
        try (Cursor cursor = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM cards", null)) {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        }
    }

    private String rarityOrderSql() {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < RARITIES.length; i++) {
            if (i > 0) result.append(' ');
            result.append("WHEN '").append(RARITIES[i]).append("' THEN ").append(i);
        }
        return result.toString();
    }

    GameCard openPack(String userId, boolean useCrystals) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            Player player = getPlayer(userId);
            int price = useCrystals ? 5 : 100;
            if (player == null || (useCrystals ? player.crystals : player.coins) < price) return null;
            ContentValues currency = new ContentValues();
            currency.put(useCrystals ? "crystals" : "coins",
                    (useCrystals ? player.crystals : player.coins) - price);
            db.update("users", currency, "id=?", new String[]{userId});

            int roll = random.nextInt(100);
            int tier = roll < 40 ? 0 : roll < 64 ? 1 : roll < 79 ? 2 : roll < 89 ? 3
                    : roll < 95 ? 4 : roll < 98 ? 5 : roll < 99 ? 6 : roll < 100 ? 7 : 8;
            if (random.nextInt(1000) == 0) tier = 8;
            int cardId;
            try (Cursor card = db.rawQuery("SELECT id FROM cards WHERE rarity=? ORDER BY RANDOM() LIMIT 1",
                    new String[]{RARITIES[tier]})) {
                if (!card.moveToFirst()) throw new IllegalStateException("A coleção de cartas está vazia.");
                cardId = card.getInt(0);
            }
            grantCard(db, userId, cardId);
            ContentValues mission = new ContentValues();
            mission.put("progress", 1);
            db.update("missions", mission, "user_id=? AND mission_key='packs' AND claimed=0",
                    new String[]{userId});
            unlock(db, userId, "first_pack");
            db.setTransactionSuccessful();
            try (Cursor cursor = db.rawQuery("SELECT id,name,element,rarity,attack,defense,"
                    + "quantity,level FROM cards JOIN inventory ON cards.id=inventory.card_id "
                    + "WHERE inventory.user_id=? AND cards.id=?", new String[]{userId, String.valueOf(cardId)})) {
                if (!cursor.moveToFirst()) throw new IllegalStateException("A carta recebida não foi salva.");
                return new GameCard(cursor.getInt(0), cursor.getString(1), cursor.getString(2),
                        cursor.getString(3), cursor.getInt(4), cursor.getInt(5), cursor.getInt(6),
                        cursor.getInt(7));
            }
        } finally {
            db.endTransaction();
        }
    }

    private void grantCard(SQLiteDatabase db, String userId, int cardId) {
        try (Cursor current = db.rawQuery("SELECT quantity,level FROM inventory WHERE user_id=? AND card_id=?",
                new String[]{userId, String.valueOf(cardId)})) {
            if (current.moveToFirst()) {
                int quantity = current.getInt(0) + 1;
                ContentValues update = new ContentValues();
                update.put("quantity", quantity);
                update.put("level", current.getInt(1));
                db.update("inventory", update, "user_id=? AND card_id=?",
                        new String[]{userId, String.valueOf(cardId)});
            } else {
                ContentValues insert = new ContentValues();
                insert.put("user_id", userId);
                insert.put("card_id", cardId);
                insert.put("quantity", 1);
                insert.put("level", 1);
                db.insertOrThrow("inventory", null, insert);
            }
        }
    }

    void completeBattle(String userId, boolean campaign, boolean won) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            Player player = getPlayer(userId);
            if (player == null) throw new IllegalStateException("Conta não encontrada.");
            ContentValues reward = new ContentValues();
            reward.put("coins", player.coins + (won ? (campaign ? 75 : 100) : 20));
            reward.put("crystals", player.crystals + (won && campaign && player.stage % 5 == 0 ? 2 : 0));
            db.update("users", reward, "id=?", new String[]{userId});
            ContentValues progress = new ContentValues();
            progress.put("xp", player.xp + (won ? 50 : 15));
            if (campaign) {
                progress.put("campaign_stage", player.stage + (won ? 1 : 0));
            } else {
                progress.put(won ? "arena_wins" : "arena_losses",
                        won ? player.wins + 1 : player.losses + 1);
            }
            db.update("progress", progress, "user_id=?", new String[]{userId});
            if (won) {
                String missionKey = campaign ? "campaign" : "arena";
                ContentValues mission = new ContentValues();
                mission.put("progress", 1);
                db.update("missions", mission, "user_id=? AND mission_key=? AND claimed=0",
                        new String[]{userId, missionKey});
                unlock(db, userId, "first_battle");
                if (!campaign && player.wins + 1 >= 5) unlock(db, userId, "arena_5");
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private void unlock(SQLiteDatabase db, String userId, String key) {
        ContentValues achievement = new ContentValues();
        achievement.put("unlocked", 1);
        db.update("achievements", achievement, "user_id=? AND achievement_key=?",
                new String[]{userId, key});
    }

    boolean claimMission(String userId, String key) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            try (Cursor cursor = db.rawQuery("SELECT progress,claimed FROM missions WHERE user_id=? AND mission_key=?",
                    new String[]{userId, key})) {
                if (!cursor.moveToFirst() || cursor.getInt(0) < 1 || cursor.getInt(1) == 1) return false;
            }
            ContentValues claim = new ContentValues();
            claim.put("claimed", 1);
            db.update("missions", claim, "user_id=? AND mission_key=?",
                    new String[]{userId, key});
            Player player = getPlayer(userId);
            ContentValues reward = new ContentValues();
            reward.put("coins", player.coins + 150);
            reward.put("crystals", player.crystals + 3);
            db.update("users", reward, "id=?", new String[]{userId});
            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    List<String> getMissionStatus(String userId) {
        List<String> result = new ArrayList<>();
        String[] labels = {"campaign|Vença 1 batalha de campanha", "arena|Vença 1 batalha na arena",
                "packs|Abra 1 pacote"};
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT mission_key,progress,claimed FROM missions WHERE user_id=?", new String[]{userId})) {
            while (cursor.moveToNext()) {
                String label = cursor.getString(0);
                for (String entry : labels) {
                    if (entry.startsWith(label + "|")) label = entry.substring(entry.indexOf('|') + 1);
                }
                result.add(cursor.getString(0) + "|" + label + "|" + cursor.getInt(1)
                        + "|" + cursor.getInt(2));
            }
        }
        return result;
    }

    List<String> getAchievementStatus(String userId) {
        String[] labels = {"first_battle|Primeira vitória", "first_pack|Primeiro pacote",
                "collector_10|Colecionador: 10 cartas", "arena_5|Veterano da arena"};
        List<String> result = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT achievement_key,unlocked FROM achievements WHERE user_id=?", new String[]{userId})) {
            while (cursor.moveToNext()) {
                String key = cursor.getString(0);
                String label = key;
                for (String entry : labels) if (entry.startsWith(key + "|")) label = entry.split("\\|")[1];
                boolean unlocked = cursor.getInt(1) == 1;
                if (key.equals("collector_10") && getOwnedCardCount(userId) >= 10) {
                    unlock(getWritableDatabase(), userId, key);
                    unlocked = true;
                }
                result.add(label + "|" + unlocked);
            }
        }
        return result;
    }

    int getOwnedCardCount(String userId) {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM inventory WHERE user_id=?", new String[]{userId})) {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        }
    }

    List<String> getUsersForAdmin() {
        List<String> result = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT id,username,coins,crystals,is_admin FROM users ORDER BY created_at DESC LIMIT 50",
                null)) {
            while (cursor.moveToNext()) {
                result.add(cursor.getString(0) + "|" + cursor.getString(1) + "|"
                        + cursor.getInt(2) + "|" + cursor.getInt(3) + "|" + cursor.getInt(4));
            }
        }
        return result;
    }

    boolean grantAdminCoins(String adminId, String targetId, int amount) {
        if (!ADMIN_ID.equals(adminId) || amount <= 0 || amount > 100000) return false;
        SQLiteDatabase db = getWritableDatabase();
        try (Cursor cursor = db.rawQuery("SELECT is_admin,coins FROM users WHERE id=?",
                new String[]{targetId})) {
            if (!cursor.moveToFirst() || cursor.getInt(0) == 1) return false;
            ContentValues values = new ContentValues();
            values.put("coins", cursor.getInt(1) + amount);
            db.update("users", values, "id=?", new String[]{targetId});
            return true;
        }
    }

    boolean exchangeCoinsForCrystals(String userId) {
        SQLiteDatabase db = getWritableDatabase();
        Player player = getPlayer(userId);
        if (player == null || player.coins < 300) return false;
        ContentValues values = new ContentValues();
        values.put("coins", player.coins - 300);
        values.put("crystals", player.crystals + 10);
        return db.update("users", values, "id=?", new String[]{userId}) == 1;
    }

    boolean evolveCard(String userId, int cardId) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try (Cursor cursor = db.rawQuery("SELECT quantity,level FROM inventory WHERE user_id=? AND card_id=?",
                new String[]{userId, String.valueOf(cardId)})) {
            if (!cursor.moveToFirst() || cursor.getInt(0) < 3) return false;
            ContentValues values = new ContentValues();
            values.put("quantity", cursor.getInt(0) - 2);
            values.put("level", cursor.getInt(1) + 1);
            db.update("inventory", values, "user_id=? AND card_id=?",
                    new String[]{userId, String.valueOf(cardId)});
            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    private String passwordHash(String password, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 120000, 256);
            byte[] result = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
                    .generateSecret(spec).getEncoded();
            spec.clearPassword();
            return Base64.encodeToString(result, Base64.NO_WRAP);
        } catch (Exception exception) {
            throw new IllegalStateException("Não foi possível proteger a senha local.", exception);
        }
    }
}
