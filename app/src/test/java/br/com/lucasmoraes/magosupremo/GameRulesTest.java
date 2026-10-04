package br.com.lucasmoraes.magosupremo;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GameRulesTest {
    @Test
    public void hasNineDistinctRarities() {
        Set<String> rarities = new HashSet<>();
        for (String rarity : GameDatabase.RARITIES) rarities.add(rarity);
        assertEquals(9, rarities.size());
        assertTrue(GameDatabase.CARD_COUNT >= 100);
    }

    @Test
    public void accountIdRangeReservesAdminAndFitsSixDigits() {
        assertEquals("000000", GameDatabase.ADMIN_ID);
        assertEquals("000001", GameDatabase.formatAccountId(1));
        assertEquals("999999", GameDatabase.formatAccountId(999999));
    }

    @Test
    public void accountIdGeneratorRejectsOutsideRange() {
        for (int invalid : new int[]{0, 1000000}) {
            try {
                GameDatabase.formatAccountId(invalid);
            } catch (IllegalArgumentException expected) {
                continue;
            }
            throw new AssertionError("ID fora da faixa foi aceito: " + invalid);
        }
    }
}
