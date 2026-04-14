import greenfoot.*;

public class Cure extends StatusEffect {
    
    public Cure(Hero target, int stacks) {
        super(target, 2, stacks); 
    }
    
    public void triggerUltimateHeal() {
        if (stacks > 0) {
            int totalHeal = 100 * this.stacks;
            target.applyHeal(totalHeal, "Cure (Ultimate)");
             
            if (getWorld() != null) {
                Animation healBurst = new Animation("bian_skill2_effect", 3, 6); // Play it a bit faster
                getWorld().addObject(healBurst, target.getX(), target.getY());
            }
            
            this.stacks = 0;
            this.duration = 0;
        }
    }
 
    @Override
    public void onTurnEnd() { 
        if (stacks > 0) {
            target.applyHeal(50, "Cure");
            stacks--;
        }
         
        duration--;
         
        if (duration < 0 || stacks <= 0) {
            if (getWorld() != null) {
                getWorld().removeObject(this);
            }
        }
    }
}