package br.com.lucasmoraes.magosupremo.cards;

import java.util.ArrayList;
import java.util.List;

public final class CardData {
    public final int id, attack, defense, energy, value;
    public final String name, description, category, element, rarity, skill;
    public CardData(int id, String name, String description, String category, String element, String rarity, int attack, int defense, int energy, int value, String skill) {
        this.id=id; this.name=name; this.description=description; this.category=category; this.element=element; this.rarity=rarity; this.attack=attack; this.defense=defense; this.energy=energy; this.value=value; this.skill=skill;
    }
    public static List<CardData> starterCards() {
        List<CardData> out = new ArrayList<>();
        String[] names={"Aprendiz do Véu","Maga das Cinzas","Guardião de Ônix","Dragão do Alvorecer","Espada do Eclipse","Cajado das Marés","Amuleto Lunar","Coroa Estelar","Grimório Perdido","Cristal Primordial","Fênix Rubra","Lobo Astral","Sentinela do Norte","Oráculo Violeta","Titã de Basalto","Serpente Celeste","Cavaleiro Espectral","Druida Antigo","Arcanista Solar","Soberano Abissal"};
        String[] elements={"Arcano","Fogo","Trevas","Luz","Metal","Água","Luz","Arcano","Trevas","Terra","Fogo","Vento","Gelo","Arcano","Terra","Vento","Trevas","Natureza","Luz","Trevas"};
        String[] rarities={"Comum","Incomum","Rara","Épica","Lendária","Mítica","Divina","Suprema","Transcendente"};
        String[] categories={"Mago","Feiticeiro","Guardião","Dragão","Arma","Cajado","Relíquia","Artefato","Grimório","Cristal"};
        for (int i=0;i<100;i++) {
            String base=names[i%names.length];
            String name=(i<names.length)?base:base+" "+roman(i/names.length+1);
            String rarity=rarities[Math.min(8, (i%100)/12)];
            int tier=1+(i%9);
            out.add(new CardData(1000+i,name,"Uma carta única nascida das lendas do Véu. Sua energia responde ao destino de quem a domina.",categories[i%categories.length],elements[i%elements.length],rarity,8+tier*5+(i%7),6+tier*4+(i%5),1+(i%5),20+tier*15,"Pulso Arcano: causa dano adicional de "+(tier*2)+" pontos."));
        }
        return out;
    }
    private static String roman(int n) { String[] r={"II","III","IV","V","VI","VII"}; return n>=2&&n<=7?r[n-2]:""+n; }
    public String toString() { return "#"+id+" • "+name+"\n"+rarity+" | "+element+" | ATQ "+attack+" DEF "+defense+" EN "+energy+"\n"+description+"\nHabilidade: "+skill; }
}
