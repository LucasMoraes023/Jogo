package br.com.lucasmoraes.magosupremo.admin;

import br.com.lucasmoraes.magosupremo.database.GameDatabase;

public class CommandProcessor {
    private final GameDatabase db;
    public CommandProcessor(GameDatabase db){this.db=db;}
    public String execute(String raw,boolean authenticated){if(!authenticated)return "Acesso negado: autentique-se como administrador.";if(raw==null)return "Comando vazio.";String[] p=raw.trim().split("\\s+");if(p.length==0)return "Comando vazio.";try{switch(p[0].toLowerCase(java.util.Locale.ROOT)){
        case "/adminhelp": return "/adminhelp, /accountinfo ID, /accountlist, /givecoins ID QTD, /givecrystals ID QTD, /givecard ID CARD_ID, /unlockallcards ID, /setlevel ID NIVEL, /backup, /adminlogs, /gameinfo";
        case "/accountinfo": if(p.length!=2)return "Uso: /accountinfo ID";return db.accountSummary(Integer.parseInt(p[1]));
        case "/accountlist": android.database.Cursor c=db.accounts();StringBuilder b=new StringBuilder();while(c.moveToNext())b.append(String.format(java.util.Locale.ROOT,"%06d",c.getInt(0))).append(" • ").append(c.getString(1)).append(" • nível ").append(c.getInt(2)).append('\n');c.close();return b.length()==0?"Nenhuma conta criada.":b.toString();
        case "/givecoins": case "/givecrystals": if(p.length!=3)return "Uso: "+p[0]+" ID QUANTIDADE";boolean ok=db.addCurrency(Integer.parseInt(p[1]),p[0].equalsIgnoreCase("/givecoins")?"coins":"crystals",Long.parseLong(p[2]));return ok?"Alteração registrada.":"Conta inexistente ou quantidade inválida.";
        case "/givecard": if(p.length!=3)return "Uso: /givecard ID CARD_ID";return db.grantCard(Integer.parseInt(p[1]),Integer.parseInt(p[2]),1)?"Carta concedida.":"Não foi possível conceder a carta.";
        case "/unlockallcards": if(p.length!=2)return "Uso: /unlockallcards ID";int id=Integer.parseInt(p[1]);for(int card=1000;card<1100;card++)db.grantCard(id,card,1);db.log(id,"ADMIN_UNLOCK_ALL","Coleção liberada");return "Coleção inicial concedida (100 cartas).";
        case "/setlevel": if(p.length!=3)return "Uso: /setlevel ID NÍVEL";int level=Integer.parseInt(p[2]);if(level<1||level>999)return "Nível deve ficar entre 1 e 999.";android.content.ContentValues v=new android.content.ContentValues();v.put("level",level);int changed=db.getWritableDatabase().update("accounts",v,"id=?",new String[]{p[1]});db.log(Integer.parseInt(p[1]),"ADMIN_SET_LEVEL","Nível definido: "+level);return changed>0?"Nível atualizado.":"Conta não encontrada.";
        case "/gameinfo":return "MAGO SUPREMO • cartas: "+db.countCards()+" • modo offline";
        case "/adminlogs": android.database.Cursor logs=db.logs();StringBuilder lb=new StringBuilder();while(logs.moveToNext())lb.append(logs.getString(0)).append(" — ").append(logs.getString(1)).append('\n');logs.close();return lb.length()==0?"Sem registros.":lb.toString();
        case "/backup": return "Backup local: use Configurações do Android para exportar dados do app. Exportação validada será adicionada na próxima etapa.";
        case "/restorebackup": return "Restauração exige um arquivo de backup validado; nenhum arquivo foi alterado.";
        default:return "Comando desconhecido. Use /adminhelp.";
    }}catch(NumberFormatException e){return "Parâmetro numérico inválido.";}catch(Exception e){return "Falha segura ao executar comando: "+e.getMessage();}}
}
