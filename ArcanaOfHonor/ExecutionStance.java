import greenfoot.*;
import java.util.ArrayList;

public class ExecutionStance extends StatusEffect {
    private boolean hasTriggered = false;

    public ExecutionStance(Hero target) {
        super(target, 1, 1);
        setImage(new GreenfootImage(1, 1));
    }

    @Override
    public void onTurnStart() {

        if (hasTriggered || getWorld() == null) return;
        if (!(target instanceof BaiQi)) {
            hasTriggered = true;
            if (getWorld() != null) getWorld().removeObject(this);
            return;
        }

        BaiQi bqOwner = (BaiQi) target;
        BattleWorld world = (BattleWorld) getWorld();
        BattleManager mgr = BattleWorld.getManager();

        if (mgr != null) mgr.logEvent("⏱ ExecutionStance turn-start for " + bqOwner.name + " (stance=" + bqOwner.getStance() + ")");

        if (bqOwner.getStance() != BaiQi.Stance.CHANNELING) {
            hasTriggered = true;
            if (getWorld() != null) getWorld().removeObject(this);
            return;
        }

        if (bqOwner.isChannelReleaseInProgress()) {
            hasTriggered = true;
            if (getWorld() != null) getWorld().removeObject(this);
            return;
        }

        bqOwner.setChannelReleaseInProgress(true);
        hasTriggered = true;
        this.duration = 0;

        if (mgr != null) mgr.lockGame();

        java.util.ArrayList < Hero > enemies = world.getEnemiesOf(bqOwner);

        for (Hero h: world.getObjects(Hero.class)) {
            if (h != bqOwner && !enemies.contains(h)) h.hide();
            else if (enemies.contains(h)) h.show();
        }

        Runnable onAnimationComplete = () -> {
            if (mgr != null) mgr.logEvent("✦ Blood Echo release by " + bqOwner.name + " (targets=" + enemies.size() + ")");

            for (Hero enemy: enemies) {
                enemy.dealDamage(115, "Blood Echo");
                bqOwner.applyHeal(15, "Blood Echo");
                enemy.playHurtAnimation();
                Animation fx = new Animation("baiQi_skill1_effect", 3, 8);
                world.addObject(fx, enemy.getX(), enemy.getY());
            }

            bqOwner.leaveExecutionStance();
            world.updateSkilldockForStance(bqOwner, false);

            BattleManager m = BattleWorld.getManager();
            if (m != null) m.updateAIHandForStance(bqOwner, false);

            for (Hero ally: world.getAlliesOf(bqOwner))
                if (ally != bqOwner) ally.show();

            bqOwner.setChannelReleaseInProgress(false);
            if (mgr != null) mgr.unlockGame();

            if (getWorld() != null) getWorld().removeObject(this);
        };

        bqOwner.continueChannelAnimation(onAnimationComplete);
    }

    private void safeRemove() {
        if (getWorld() != null) getWorld().removeObject(this);
    }
}