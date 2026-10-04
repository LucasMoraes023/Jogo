package br.com.lucasmoraes.magosupremo.battle;
import org.junit.Test;
import static org.junit.Assert.*;
import br.com.lucasmoraes.magosupremo.cards.CardData;
public class CardDataTest {
 @Test public void startsWithOneHundredDistinctIds(){java.util.List<CardData> cards=CardData.starterCards();assertEquals(100,cards.size());java.util.HashSet<Integer> ids=new java.util.HashSet<>();for(CardData c:cards)ids.add(c.id);assertEquals(100,ids.size());}
}
