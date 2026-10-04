package br.com.lucasmoraes.magosupremo.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import br.com.lucasmoraes.magosupremo.cards.CardData;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public class GameDatabase extends SQLiteOpenHelper {
    private static final String DB_NAME="mago_supremo.db"; private static final int DB_VERSION=1;
    public GameDatabase(Context c) { super(c,DB_NAME,null,DB_VERSION); setWriteAheadLoggingEnabled(true); }
    @Override public void onConfigure(SQLiteDatabase db) { super.onConfigure(db); db.setForeignKeyConstraintsEnabled(true); }
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE accounts(id INTEGER PRIMARY KEY CHECK(id BETWEEN 0 AND 999999), nickname TEXT NOT NULL COLLATE NOCASE UNIQUE, level INTEGER NOT NULL DEFAULT 1, xp INTEGER NOT NULL DEFAULT 0, coins INTEGER NOT NULL DEFAULT 500, crystals INTEGER NOT NULL DEFAULT 10, created_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE used_ids(id INTEGER PRIMARY KEY CHECK(id BETWEEN 0 AND 999999))");
        db.execSQL("CREATE TABLE cards(id INTEGER PRIMARY KEY, name TEXT NOT NULL, description TEXT NOT NULL, category TEXT NOT NULL, element TEXT NOT NULL, rarity TEXT NOT NULL, attack INTEGER NOT NULL, defense INTEGER NOT NULL, energy INTEGER NOT NULL, value INTEGER NOT NULL, skill TEXT NOT NULL)");
        db.execSQL("CREATE TABLE collection(account_id INTEGER NOT NULL REFERENCES accounts(id) ON DELETE CASCADE, card_id INTEGER NOT NULL REFERENCES cards(id), quantity INTEGER NOT NULL DEFAULT 0 CHECK(quantity>=0), level INTEGER NOT NULL DEFAULT 1, xp INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(account_id,card_id))");
        db.execSQL("CREATE TABLE history(id INTEGER PRIMARY KEY AUTOINCREMENT, account_id INTEGER, event TEXT NOT NULL, detail TEXT NOT NULL, created_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE settings(key TEXT PRIMARY KEY, value TEXT NOT NULL)");
        db.execSQL("CREATE TABLE admin(salt TEXT NOT NULL, pass_hash TEXT NOT NULL)");
        db.execSQL("CREATE TABLE missions(id INTEGER PRIMARY KEY, title TEXT NOT NULL, description TEXT NOT NULL, reward INTEGER NOT NULL, progress INTEGER NOT NULL DEFAULT 0, completed INTEGER NOT NULL DEFAULT 0)");
        for(CardData c:CardData.starterCards()){ ContentValues v=new ContentValues(); v.put("id",c.id);v.put("name",c.name);v.put("description",c.description);v.put("category",c.category);v.put("element",c.element);v.put("rarity",c.rarity);v.put("attack",c.attack);v.put("defense",c.defense);v.put("energy",c.energy);v.put("value",c.value);v.put("skill",c.skill);db.insertOrThrow("cards",null,v); }
        String[] ms={"Reúna seu primeiro grimório","Vença uma batalha","Abra um pacote","Colecione cinco cartas","Alcance o nível 2"};
        for(int i=0;i<ms.length;i++){ContentValues v=new ContentValues();v.put("id",i+1);v.put("title",ms[i]);v.put("description","Complete a atividade para receber moedas.");v.put("reward",100+(i*50));db.insert("missions",null,v);}
    }
    @Override public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion) { }
    public synchronized int createAccount(String nick) {
        String n=nick==null?"":nick.trim(); if(!n.matches("[\\p{L}0-9_ ]{3,16}")) throw new IllegalArgumentException("Use um nick de 3 a 16 caracteres: letras, números, espaço ou _.");
        SQLiteDatabase db=getWritableDatabase(); db.beginTransaction();
        try {
            Cursor exists=db.rawQuery("SELECT 1 FROM accounts WHERE nickname=? COLLATE NOCASE",new String[]{n}); if(exists.moveToFirst()){exists.close();throw new IllegalArgumentException("Esse nickname já está em uso.");} exists.close();
            Cursor next=db.rawQuery("SELECT COALESCE(MIN(x.id+1),0) FROM used_ids x LEFT JOIN used_ids y ON y.id=x.id+1 WHERE y.id IS NULL AND x.id<999999",null);
            int id=0; if(next.moveToFirst()) id=next.getInt(0); next.close();
            Cursor zero=db.rawQuery("SELECT 1 FROM used_ids WHERE id=0",null); boolean zeroUsed=zero.moveToFirst();zero.close();
            if(zeroUsed){Cursor c=db.rawQuery("SELECT id FROM used_ids ORDER BY id",null);id=-1;int expected=0;while(c.moveToNext()){int got=c.getInt(0);if(got!=expected){id=expected;break;}expected=got+1;}if(id<0&&expected<=999999)id=expected;c.close();}
            if(id<0||id>999999) throw new IllegalStateException("Todos os IDs disponíveis já foram utilizados.");
            ContentValues used=new ContentValues();used.put("id",id);db.insertOrThrow("used_ids",null,used);
            ContentValues a=new ContentValues();a.put("id",id);a.put("nickname",n);a.put("created_at",System.currentTimeMillis());db.insertOrThrow("accounts",null,a);
            log(db,id,"CONTA_CRIADA","Conta criada: "+n); db.setTransactionSuccessful(); return id;
        } finally {db.endTransaction();}
    }
    public Cursor account(int id){return getReadableDatabase().rawQuery("SELECT id,nickname,level,xp,coins,crystals,created_at FROM accounts WHERE id=?",new String[]{String.valueOf(id)});}
    public Cursor accounts(){return getReadableDatabase().rawQuery("SELECT id,nickname,level,xp,coins,crystals FROM accounts ORDER BY id DESC LIMIT 100",null);}
    public Cursor cards(){return getReadableDatabase().rawQuery("SELECT c.id,c.name,c.rarity,c.element,c.attack,c.defense,COALESCE(col.quantity,0) FROM cards c LEFT JOIN collection col ON col.card_id=c.id AND col.account_id=(SELECT CAST(value AS INTEGER) FROM settings WHERE key='active_account') ORDER BY c.id",null);}
    public Cursor collection(int accountId){return getReadableDatabase().rawQuery("SELECT c.id,c.name,c.rarity,c.element,c.attack,c.defense,col.quantity,c.description,c.skill FROM collection col JOIN cards c ON c.id=col.card_id WHERE col.account_id=? AND col.quantity>0 ORDER BY c.rarity DESC,c.name",new String[]{String.valueOf(accountId)});}
    public int countCards(){Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM cards",null);int n=c.moveToFirst()?c.getInt(0):0;c.close();return n;}
    public boolean setActive(int id){Cursor c=account(id);boolean ok=c.moveToFirst();c.close();if(ok){ContentValues v=new ContentValues();v.put("value",String.valueOf(id));getWritableDatabase().insertWithOnConflict("settings",null,new ContentValues(){{put("key","active_account");put("value",String.valueOf(id));}},SQLiteDatabase.CONFLICT_REPLACE);}return ok;}
    public int activeId(){Cursor c=getReadableDatabase().rawQuery("SELECT value FROM settings WHERE key='active_account'",null);int id=-1;if(c.moveToFirst())try{id=Integer.parseInt(c.getString(0));}catch(Exception ignored){}c.close();return id;}
    public boolean addCurrency(int id,String type,long amount){if(amount<0||amount>1000000000L||(!type.equals("coins")&&!type.equals("crystals")))return false;SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{Cursor c=db.rawQuery("SELECT "+type+" FROM accounts WHERE id=?",new String[]{""+id});if(!c.moveToFirst()){c.close();return false;}long old=c.getLong(0);c.close();if(old+amount>Integer.MAX_VALUE)return false;ContentValues v=new ContentValues();v.put(type,old+amount);db.update("accounts",v,"id=?",new String[]{""+id});log(db,id,"ADMIN_CURRENCY",type+" +"+amount);db.setTransactionSuccessful();return true;}finally{db.endTransaction();}}
    public boolean spendCoins(int id,int amount){if(amount<0)return false;SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{int changed=db.compileStatement("UPDATE accounts SET coins=coins-"+amount+" WHERE id="+id+" AND coins>="+amount).executeUpdateDelete();if(changed==0)return false;db.setTransactionSuccessful();return true;}finally{db.endTransaction();}}
    public boolean grantCard(int accountId,int cardId,int qty){if(qty<1||qty>100000)return false;SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{ContentValues v=new ContentValues();v.put("account_id",accountId);v.put("card_id",cardId);v.put("quantity",qty);long r=db.insertWithOnConflict("collection",null,v,SQLiteDatabase.CONFLICT_IGNORE);if(r==-1){db.execSQL("UPDATE collection SET quantity=quantity+? WHERE account_id=? AND card_id=?",new Object[]{qty,accountId,cardId});}log(db,accountId,"CARTA_RECEBIDA","card="+cardId+" quantidade="+qty);db.setTransactionSuccessful();return true;}finally{db.endTransaction();}}
    public boolean openPack(int accountId){SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{Cursor cur=db.rawQuery("SELECT coins FROM accounts WHERE id=?",new String[]{""+accountId});if(!cur.moveToFirst()||cur.getInt(0)<100){cur.close();return false;}cur.close();db.execSQL("UPDATE accounts SET coins=coins-100 WHERE id=?",new Object[]{accountId});Cursor pick=db.rawQuery("SELECT id FROM cards ORDER BY RANDOM() LIMIT 1",null);if(!pick.moveToFirst()){pick.close();return false;}int card=pick.getInt(0);pick.close();ContentValues v=new ContentValues();v.put("account_id",accountId);v.put("card_id",card);v.put("quantity",1);if(db.insertWithOnConflict("collection",null,v,SQLiteDatabase.CONFLICT_IGNORE)==-1)db.execSQL("UPDATE collection SET quantity=quantity+1 WHERE account_id=? AND card_id=?",new Object[]{accountId,card});log(db,accountId,"PACOTE_ABERTO","Carta recebida: "+card);db.setTransactionSuccessful();return true;}finally{db.endTransaction();}}
    public String accountSummary(int id){Cursor c=account(id);if(!c.moveToFirst()){c.close();return "Conta não encontrada.";}String s="ID: "+String.format(java.util.Locale.ROOT,"%06d",c.getInt(0))+"\nNick: "+c.getString(1)+"\nNível: "+c.getInt(2)+" • XP: "+c.getInt(3)+"\nMoedas: "+c.getInt(4)+" • Cristais: "+c.getInt(5);c.close();return s;}
    public void log(int accountId,String event,String detail){log(getWritableDatabase(),accountId,event,detail);}
    private void log(SQLiteDatabase db,int id,String event,String detail){ContentValues v=new ContentValues();if(id>=0)v.put("account_id",id);v.put("event",event);v.put("detail",detail);v.put("created_at",System.currentTimeMillis());db.insert("history",null,v);}
    public Cursor logs(){return getReadableDatabase().rawQuery("SELECT event,detail,created_at FROM history ORDER BY id DESC LIMIT 100",null);}
    public boolean adminConfigured(){Cursor c=getReadableDatabase().rawQuery("SELECT 1 FROM admin LIMIT 1",null);boolean b=c.moveToFirst();c.close();return b;}
    public void configureAdmin(char[] password){if(password==null||password.length<8)throw new IllegalArgumentException("A senha do administrador precisa ter pelo menos 8 caracteres.");try{byte[] salt=new byte[16];new SecureRandom().nextBytes(salt);byte[] hash=MessageDigest.getInstance("SHA-256").digest(concat(salt,new String(password).getBytes("UTF-8")));ContentValues v=new ContentValues();v.put("salt",hex(salt));v.put("pass_hash",hex(hash));getWritableDatabase().delete("admin",null,null);getWritableDatabase().insertOrThrow("admin",null,v);log(-1,"ADMIN_SETUP","Credencial local configurada");}catch(Exception e){throw new IllegalStateException("Não foi possível configurar o administrador.",e);}}
    public boolean verifyAdmin(char[] password){try{Cursor c=getReadableDatabase().rawQuery("SELECT salt,pass_hash FROM admin LIMIT 1",null);if(!c.moveToFirst()){c.close();return false;}byte[] salt=unhex(c.getString(0));String expected=c.getString(1);c.close();byte[] actual=MessageDigest.getInstance("SHA-256").digest(concat(salt,new String(password).getBytes("UTF-8")));return MessageDigest.isEqual(unhex(expected),actual);}catch(Exception e){return false;}}
    private byte[] concat(byte[] a,byte[] b){byte[] o=new byte[a.length+b.length];System.arraycopy(a,0,o,0,a.length);System.arraycopy(b,0,o,a.length,b.length);return o;}
    private String hex(byte[] b){StringBuilder s=new StringBuilder();for(byte x:b)s.append(String.format(java.util.Locale.ROOT,"%02x",x&255));return s.toString();}
    private byte[] unhex(String s){byte[] b=new byte[s.length()/2];for(int i=0;i<b.length;i++)b[i]=(byte)Integer.parseInt(s.substring(i*2,i*2+2),16);return b;}
}
