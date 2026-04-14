import greenfoot.*;

public class Poison extends StatusEffect {

    public Poison(Hero target, int initialStacks) {
        super(target, 99, initialStacks);
    }

    @Override
    public void onTurnEnd() {

        if (this.stacks > 0) {
            target.dealDamage(50, "Poison");
            target.playHurtAnimation();
            this.stacks--;
        }

        if (this.stacks <= 0) {
            if (getWorld() != null) {
                getWorld().removeObject(this);
            }
        }
    }
}