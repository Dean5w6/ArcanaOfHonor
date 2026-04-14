import greenfoot.*;

public class SwordQi extends StatusEffect {
    public static final int BONUS_PER_STACK = 50;

    public SwordQi(Hero target, int stacks) {
        super(target, 99, stacks);
        setImage(new GreenfootImage(1,1));
    }

    /** Consume exactly one stack and return the bonus to add to this hit. */
    public int consumeOne() {
        if (stacks > 0) {
            stacks--;
            if (stacks <= 0 && getWorld() != null) getWorld().removeObject(this);
            return BONUS_PER_STACK;
        }
        return 0;
    }
}
