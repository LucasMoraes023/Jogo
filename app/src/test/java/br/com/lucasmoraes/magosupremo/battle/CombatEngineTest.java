package br.com.lucasmoraes.magosupremo.battle;
import org.junit.Test;
import static org.junit.Assert.*;
public class CombatEngineTest {
 @Test public void damageConsumesEnergyAndRespectsDefense(){CombatEngine e=new CombatEngine();CombatEngine.Fighter a=new CombatEngine.Fighter(100,30,5,2),d=new CombatEngine.Fighter(100,10,20,2);int damage=e.damage(a,d,0);assertEquals(20,damage);assertEquals(80,d.hp);assertEquals(1,a.energy);}
 @Test public void deadEnemyMeansVictory(){CombatEngine e=new CombatEngine();assertEquals("VITÓRIA",e.outcome(new CombatEngine.Fighter(10,1,1,1),new CombatEngine.Fighter(0,1,1,1)));}
 @Test public void noEnergyMeansNoDamage(){CombatEngine e=new CombatEngine();CombatEngine.Fighter a=new CombatEngine.Fighter(10,20,0,0),d=new CombatEngine.Fighter(10,1,0,1);assertEquals(0,e.damage(a,d,0));assertEquals(10,d.hp);}
}
