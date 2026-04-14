import greenfoot.*;

public abstract class SkillCard extends Actor {
    protected Hero owner;
    private boolean isPlayed = false;

    public SkillCard(String filename, boolean isUltimate, Hero owner) {
        this.owner = owner;
        GreenfootImage image = new GreenfootImage(filename);
        if (isUltimate) {
            image.scale(82, 154);
        } else {
            image.scale(82, 122);
        }
        setImage(image);
    }

    public void act() {
        if (isPlayed) {
            return;
        }

        if (BattleWorld.getManager() != null && BattleWorld.getManager().isGameLocked()) {
            return;
        }

        if (Greenfoot.mouseClicked(this)) {

            BattleManager manager = BattleWorld.getManager();
            if (manager != null && manager.canSpendAP(1)) {
                manager.startSkillSequence(this);
            }
        }
    }

    public abstract void useSkill(BattleWorld world);

    public void markAsPlayed() {
        this.isPlayed = true;
        getImage().setTransparency(0);
    }

    public boolean isCardPlayed() {
        return isPlayed;
    }
}