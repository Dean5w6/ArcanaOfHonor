import greenfoot.*;
import java.util.ArrayList;
import java.util.List;

public class BianQueUltimateCard extends SkillCard {
    public BianQueUltimateCard(Hero owner) {
        super("card_bian_ultimate.png", true, owner);
    }
    
    @Override
    public void useSkill(BattleWorld world) { 
        for (Hero enemy : world.getEnemiesOf(owner)) {
            int poisonStacks = 0;
            for (Poison effect : world.getObjects(Poison.class)) {
                if (effect.target == enemy) {
                    poisonStacks += effect.stacks; 
                }
            }
            int totalDamage = 300 + (30 * poisonStacks); 
            enemy.dealDamage(totalDamage, "Master of Life"); 
        }
     
        ArrayList<Cure> activeCures = new ArrayList<>(world.getObjects(Cure.class));
        for (Cure effect : activeCures) {
            if (world.getAlliesOf(owner).contains(effect.target)) {
                effect.triggerUltimateHeal();
            }
        }
    }
}