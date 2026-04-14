import greenfoot.*;

public abstract class StatusEffect extends Actor {
    private static int nextId = 1;
    public int uniqueId;

    protected Hero target;
    protected int duration;
    protected int stacks;

    public StatusEffect(Hero target, int duration, int initialStacks) {
        this.uniqueId = nextId++;
        this.target = target;
        this.duration = duration;
        this.stacks = initialStacks;
        setImage(new GreenfootImage(1, 1));
    }

    public void act() {
        if (target.getWorld() != null) {
            setLocation(target.getX(), target.getY() - 150);
        } else if (getWorld() != null) {
            getWorld().removeObject(this);
        }
    }

    public void onTurnStart() {}

    public void onTurnEnd() {}

    public void onAction(Hero actor) {}

    public String getEffectName() {
        return this.getClass().getName();
    }
}