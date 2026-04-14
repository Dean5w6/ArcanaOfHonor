import greenfoot.*;

public class TargetSelector extends Actor {
    private Hero leftTarget;
    private Hero rightTarget;
    private BattleManager manager;

    private int portraitSize = 80;
    private int leftX = 15;
    private int leftY = 15;
    private int rightX = 15 + 80 + 10;
    private int rightY = 15;

    public TargetSelector(Hero left, Hero right, BattleManager manager) {
        this.leftTarget = left;
        this.rightTarget = right;
        this.manager = manager;

        int width = 360;
        int height = 110;
        GreenfootImage img = new GreenfootImage(width, height);

        img.setColor(new Color(0, 0, 0, 180));
        img.fill();

        if (leftTarget != null) {
            GreenfootImage lp = loadPortrait(leftTarget);
            lp.scale(portraitSize, portraitSize);
            img.drawImage(lp, leftX, leftY);
        }

        if (rightTarget != null) {
            GreenfootImage rp = loadPortrait(rightTarget);
            rp.scale(portraitSize, portraitSize);
            img.drawImage(rp, rightX, rightY);
        }

        img.setFont(new Font("Times New Roman", false, false, 20));
        img.setColor(Color.WHITE);
        int textX = rightX + portraitSize + 30;
        int textY = leftY + portraitSize / 2 + 8;
        img.drawString("Select a target", textX, textY);

        setImage(img);
    }

    private GreenfootImage loadPortrait(Hero h) {
        String file;
        if (h instanceof BaiQi) file = "baiQi_portrait.png";
        else if (h instanceof BianQue) file = "bian_portrait.png";
        else if (h instanceof GanJiang) file = "gan_portrait.png";
        else if (h instanceof MoYe) file = "mo_portrait.png";
        else file = "default_portrait.png";

        return new GreenfootImage(file);
    }

    public void act() {
        if (Greenfoot.mouseClicked(this)) {
            MouseInfo mi = Greenfoot.getMouseInfo();
            if (mi == null) return;

            GreenfootImage img = getImage();
            int imgW = img.getWidth();
            int imgH = img.getHeight();

            int localX = mi.getX() - (getX() - imgW / 2);
            int localY = mi.getY() - (getY() - imgH / 2);

            Hero chosen = null;

            if (localX >= leftX && localX <= leftX + portraitSize &&
                localY >= leftY && localY <= leftY + portraitSize) {
                chosen = leftTarget;
            } else if (rightTarget != null &&
                localX >= rightX && localX <= rightX + portraitSize &&
                localY >= rightY && localY <= rightY + portraitSize) {
                chosen = rightTarget;
            }

            if (chosen != null && manager != null) {
                manager.onPlayerTargetChosen(chosen);
                if (getWorld() != null) getWorld().removeObject(this);
            }
        }
    }
}