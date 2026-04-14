import greenfoot.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public abstract class Hero extends Actor {
    private int initialFrameHold = 1;
    public String name;
    protected boolean isEnemy;
    protected int maxHp;
    protected int currentHp;
    protected int currentUltimateCharge;
    public static final int MAX_ULTIMATE_CHARGE = 5;
    private HealthBar healthBar;
    private UltimateBar ultimateBar;

    private GreenfootImage idleSprite;
    private ArrayList < GreenfootImage > animationFrames;
    private int currentFrame = 0;
    private int animationTimer = 0;
    private int animationSpeed = 9;
    private boolean holdLastFrame = false;
    private Map < Integer, Runnable > frameTriggers;
    private boolean deathScheduled = false;
    private int deathTimer = 0;

    public Hero(boolean isEnemy, int maxHp, String name) {
        this.isEnemy = isEnemy;
        this.maxHp = maxHp;
        this.name = name;
        this.currentHp = maxHp;
        this.currentUltimateCharge = 0;
    }

    protected abstract String getHeroPrefix();

    @Override
    public void act() {
        if (animationFrames == null) return;

        animationTimer++;
        if (animationTimer < animationSpeed) return;
        animationTimer = 0;

        if (currentFrame == -1) {
            currentFrame = 0;
            setImage(animationFrames.get(0));
            if (initialFrameHold > 1) {
                initialFrameHold--;
                return;
            }
        } else {

            currentFrame++;
        }

        if (frameTriggers != null) {
            Runnable r = frameTriggers.remove(currentFrame);
            if (r != null) r.run();
        }

        if (currentFrame >= animationFrames.size()) {
            if (holdLastFrame) {
                currentFrame = animationFrames.size() - 1;
                setImage(animationFrames.get(currentFrame));
            } else {
                animationFrames = null;
                GreenfootImage rest = getRestPoseImage();
                if (rest == null) rest = getIdleSprite();
                setImage(rest);
                show();
            }
            return;
        }

        setImage(animationFrames.get(currentFrame));

        if (deathScheduled && animationFrames == null) {
            BattleWorld w = (BattleWorld) getWorld();
            if (w != null) {

                w.onHeroDeath(this);
            }
        }

        if (deathScheduled) {
            if (deathTimer > 0) deathTimer--;

            if (!isAnimating() || deathTimer <= 0) {
                BattleWorld w = (BattleWorld) getWorld();
                if (w != null) w.onHeroDeath(this);

                return;
            }
        }
    }

    protected GreenfootImage getRestPoseImage() {
        return getIdleSprite();
    }

    public void takeDamage(int amount) {
        dealDamage(amount, "Direct Hit");
    }

    public void dealDamage(int amount, String source) {
        if (this.currentHp <= 0) return;

        int previousHp = this.currentHp;
        int swordQiBonus = 0;
        if (getWorld() != null) {
            for (SwordQi sq: getWorld().getObjects(SwordQi.class)) {
                if (sq.target == this) {
                    swordQiBonus = sq.consumeOne();
                    break;
                }
            }
        }
        amount += swordQiBonus;
        this.currentHp -= amount;
        if (this.currentHp < 0) this.currentHp = 0;
        if (healthBar != null) healthBar.updateBar(this.currentHp, this.maxHp);

        Color dmgColor = "Agony".equals(source) ? new Color(152, 79, 176) : Color.RED;
        showFloatingText("-" + amount, dmgColor);

        BattleManager mgr = BattleWorld.getManager();
        if (mgr != null) {
            String tag = mgr.getCurrentActionTag();
            mgr.logEvent("DMG [" + source + "] " + name + " (" + previousHp + " -> " + this.currentHp + ")  " + tag);
        }

        if (this.currentHp == 0) {
            requestDeath();
        }
    }

    public void applyHeal(int amount, String source) {
        int previousHp = this.currentHp;
        this.currentHp += amount;
        if (this.currentHp > this.maxHp) this.currentHp = this.maxHp;
        if (healthBar != null) healthBar.updateBar(this.currentHp, this.maxHp);
        showFloatingText("+" + amount, Color.GREEN);

        BattleManager mgr = BattleWorld.getManager();
        if (mgr != null) {
            String tag = mgr.getCurrentActionTag();
            mgr.logEvent("HEAL [" + source + "] " + name + " (" + previousHp + " -> " + this.currentHp + ")  " + tag);
        }
    }

    public void heal(int amount) {

        applyHeal(amount, "Direct Heal");
    }

    public void showFloatingText(String text, Color color) {
        List < FloatingText > existingTexts = getWorld().getObjects(FloatingText.class);
        int yOffset = 0;

        for (FloatingText ft: existingTexts) {
            if (ft.getX() == this.getX()) {

                yOffset += 30;
            }
        }

        FloatingText fText = new FloatingText(text, color);
        getWorld().addObject(fText, getX(), getY() - 80 - yOffset);
    }

    public void setBars(HealthBar healthBar, UltimateBar ultimateBar) {
        this.healthBar = healthBar;
        this.ultimateBar = ultimateBar;
    }

    public void increaseUltimateCharge() {
        if (currentUltimateCharge < MAX_ULTIMATE_CHARGE) {
            currentUltimateCharge++;
            if (ultimateBar != null) {
                ultimateBar.updateBar(this.currentUltimateCharge, MAX_ULTIMATE_CHARGE);
            }
        }
    }

    public void applyStatus(StatusEffect effectToApply) {
        if (effectToApply instanceof Cure) {
            List < Cure > existingCures = getWorld().getObjects(Cure.class);
            for (Cure existingEffect: existingCures) {
                if (existingEffect.target == this) {
                    existingEffect.stacks += effectToApply.stacks;
                    existingEffect.duration = 2;

                    if (BattleWorld.getManager() != null) {
                        BattleWorld.getManager().logEvent(this.name + "'s [Cure] stacks increased to " + existingEffect.stacks);
                    }

                    return;
                }
            }
        }
        getWorld().addObject(effectToApply, getX(), getY() - 150);
    }

    protected GreenfootImage setupImage(String filename) {
        this.idleSprite = new GreenfootImage(filename);
        this.idleSprite.scale(494, 502);
        if (isEnemy) {
            this.idleSprite.mirrorHorizontally();
        }
        return this.idleSprite;
    }

    public void playAnimation(String[] frameFiles, int speed, int initialHold, boolean holdLastFrame, Map < Integer, Runnable > frameTriggers) {

        if (frameFiles == null || frameFiles.length == 0) {
            this.animationFrames = null;
            this.frameTriggers = null;
            GreenfootImage rest = getRestPoseImage();
            if (rest == null) rest = getIdleSprite();
            setImage(rest);
            return;
        }

        this.holdLastFrame = holdLastFrame;
        animationFrames = new ArrayList < > ();
        for (String file: frameFiles) {
            if ("blank".equals(file)) {
                animationFrames.add(new GreenfootImage(1, 1));
            } else {
                GreenfootImage frame = new GreenfootImage(file + ".png");
                frame.scale(494, 502);
                if (isEnemy) frame.mirrorHorizontally();
                animationFrames.add(frame);
            }
        }
        this.frameTriggers = frameTriggers;
        this.animationSpeed = speed;
        this.initialFrameHold = initialHold > 1 ? initialHold : 1;
        this.currentFrame = -1;
        this.animationTimer = speed;
    }

    public void playAnimation(String[] frameFiles, int speed, boolean hold) {
        playAnimation(frameFiles, speed, 1, hold, new HashMap < > ());
    }

    public void playAnimation(String[] frameFiles, int speed) {
        playAnimation(frameFiles, speed, 1, false, new HashMap < > ());
    }

    public void playAnimation(String[] frameFiles) {
        playAnimation(frameFiles, 9, 1, false, new HashMap < > ());
    }

    public void playHurtAnimation() {
        show();

        String heroPrefix = getHeroPrefix();

        String[] frames = new String[] {
            heroPrefix + "_hurt_gray",
                "blank",
                heroPrefix + "_hurt",
                heroPrefix + "_hurt_gray",
                "blank"
        };

        playAnimation(frames, 5, 1, false, new java.util.HashMap < Integer, Runnable > ());
    }

    public boolean isAnimating() {
        return animationFrames != null;
    }

    public void hide() {
        getImage().setTransparency(0);
        if (healthBar != null) healthBar.hide();
        if (ultimateBar != null) ultimateBar.hide();
    }

    public void show() {
        getImage().setTransparency(255);
        if (healthBar != null) healthBar.show();
        if (ultimateBar != null) ultimateBar.show();
    }

    public boolean isUltimateReady() {
        return currentUltimateCharge >= MAX_ULTIMATE_CHARGE;
    }

    public void resetUltimate() {
        this.currentUltimateCharge = 0;
        if (ultimateBar != null) {
            ultimateBar.updateBar(this.currentUltimateCharge, MAX_ULTIMATE_CHARGE);
        }
    }

    public String getCurrentSpriteName() {
        GreenfootImage currentImage = getImage();
        if (currentImage == null) {
            return "";
        }

        GreenfootImage tempIdle = new GreenfootImage(getHeroPrefix() + "_idle.png");
        tempIdle.scale(494, 502);
        if (isEnemy) tempIdle.mirrorHorizontally();

        GreenfootImage tempChannel = new GreenfootImage("baiQi_skill1_anim4.png");
        tempChannel.scale(494, 502);
        if (isEnemy) tempChannel.mirrorHorizontally();

        if (currentImage.toString().equals(tempChannel.toString())) {
            return "baiQi_skill1_anim4.png";
        }
        if (currentImage.toString().equals(tempIdle.toString())) {
            return getHeroPrefix() + "_idle.png";
        }

        return "unknown";
    }

    protected GreenfootImage getIdleSprite() {
        return idleSprite;
    }

    public void setToRestPose() {
        GreenfootImage rest = getRestPoseImage();
        if (rest == null) rest = getIdleSprite();
        setImage(new GreenfootImage(rest));
        show();
    }

    public void requestDeath() {
        if (deathScheduled) return;
        deathScheduled = true;
        deathTimer = 18;
        playHurtAnimation();
    }

    public int getCurrentHp() {
        return currentHp;
    }
    public HealthBar getHealthBar() {
        return healthBar;
    }
    public UltimateBar getUltimateBar() {
        return ultimateBar;
    }
    public boolean isAlive() {
        return getWorld() != null && currentHp > 0;
    }

    public void decreaseUltimateCharge(int amount) {
        if (amount <= 0) return;
        this.currentUltimateCharge -= amount;
        if (this.currentUltimateCharge < 0) this.currentUltimateCharge = 0;
        if (ultimateBar != null) {
            ultimateBar.updateBar(this.currentUltimateCharge, MAX_ULTIMATE_CHARGE);
        }
    }

    public void setUltimateToMax() {
        this.currentUltimateCharge = MAX_ULTIMATE_CHARGE;
        if (ultimateBar != null) {
            ultimateBar.updateBar(this.currentUltimateCharge, MAX_ULTIMATE_CHARGE);
        }
    }
}