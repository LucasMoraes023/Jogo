package br.com.lucasmoraes.magosupremo.battle;

public final class CombatEngine {
    public static final class Fighter { public int hp, attack, defense, energy; public Fighter(int hp,int attack,int defense,int energy){this.hp=Math.max(0,hp);this.attack=Math.max(0,attack);this.defense=Math.max(0,defense);this.energy=Math.max(0,energy);} }
    public int damage(Fighter attacker,Fighter defender,int bonus){if(attacker==null||defender==null||attacker.hp<=0||defender.hp<=0||attacker.energy<1)return 0;attacker.energy--;int amount=Math.max(1,attacker.attack+Math.max(0,bonus)-defender.defense/2);defender.hp=Math.max(0,defender.hp-amount);return amount;}
    public String outcome(Fighter player,Fighter enemy){if(enemy.hp<=0)return "VITÓRIA";if(player.hp<=0)return "DERROTA";return "EM_ANDAMENTO";}
}
