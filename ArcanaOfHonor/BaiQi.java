import greenfoot.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class BaiQi extends Hero {
    public enum Stance {
        IDLE,
        CHANNELING,
        ATTACKING
    }

    private Stance currentStance = Stance.IDLE;
    private GreenfootImage channelPoseSprite;

    private boolean hasExecutionStance = false;
    private boolean channelReleaseInProgress = false;

    public BaiQi(boolean isEnemy) {
        super(isEnemy, 2000, "Bai Qi");
        setImage(setupImage("baiQi_idle.png"));
    }

    @Override
    protected String getHeroPrefix() {
        return "baiQi";
    }

    public boolean isChannelReleaseInProgress() {
        return channelReleaseInProgress;
    }
    public void setChannelReleaseInProgress(boolean inProgress) {
        this.channelReleaseInProgress = inProgress;
    }

    public void setStance(Stance s) {
        this.currentStance = s;
    }
    public Stance getStance() {
        return currentStance;
    }
    public boolean isStanceActive() {
        return currentStance != Stance.IDLE;
    }

    public void enterExecutionStance() {
        if (hasExecutionStance) return;
        hasExecutionStance = true;
        setStance(Stance.CHANNELING);

        BattleWorld w = (BattleWorld) getWorld();
        if (w != null) {

            for (ExecutionStance e: new ArrayList < > (w.getObjects(ExecutionStance.class))) {
                if (e != null && e.target == this) w.removeObject(e);
            }

            w.addObject(new ExecutionStance(this), getX(), getY());
        }
    }

    public void leaveExecutionStance() {
        hasExecutionStance = false;
        setStance(Stance.IDLE);
        channelReleaseInProgress = false;

        BattleWorld w = (BattleWorld) getWorld();
        if (w != null) {
            for (ExecutionStance e: new ArrayList < > (w.getObjects(ExecutionStance.class))) {
                if (e != null && e.target == this) w.removeObject(e);
            }
        }
    }

    public void continueChannelAnimation(Runnable onComplete) {
        this.currentStance = Stance.ATTACKING;

        Map < Integer, Runnable > triggers = new HashMap < > ();

        triggers.put(1, onComplete);

        String[] attackFrames = {
            "baiQi_skill1_anim5",
            "baiQi_skill1_anim6"
        };

        playAnimation(attackFrames, 12, 1, false, triggers);
    }

    public GreenfootImage getChannelingPoseImage() {
        if (channelPoseSprite == null) {
            channelPoseSprite = new GreenfootImage("baiQi_skill1_anim4.png");
            channelPoseSprite.scale(494, 502);
            if (isEnemy) channelPoseSprite.mirrorHorizontally();
        }

        return new GreenfootImage(channelPoseSprite);
    }

    @Override
    protected GreenfootImage getRestPoseImage() {

        if (getStance() == Stance.CHANNELING) {
            return getChannelingPoseImage();
        }
        return getIdleSprite();
    }

}