import greenfoot.*;

public class Agony extends StatusEffect {

    private boolean processing = false;

    public Agony(Hero target, int stacks) {
        super(target, 99, Math.max(1, stacks));
    }

    @Override
    public void onAction(Hero actor) {

        if (processing || actor != target) return;
        processing = true;

        target.dealDamage(50, "Agony");

        stacks = Math.max(0, stacks - 1);
        if (stacks <= 0 && getWorld() != null) {
            getWorld().removeObject(this);
        }

        processing = false;
    }
}