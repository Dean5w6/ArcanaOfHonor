import greenfoot.*;
import java.util.ArrayList;

public class BianQueSkill2Card extends SkillCard {
    public BianQueSkill2Card(Hero owner) {
        super("card_bian_skill2.png", false, owner);
    }
    
   @Override
    public void useSkill(BattleWorld world) { 
        ArrayList<Hero> allies = world.getAlliesOf(owner);
        
        for (Hero ally : allies) { 
            ally.applyHeal(100, "Fatal Diagnosis");
             
            ally.applyStatus(new Cure(ally, 2));
        }
    }
}