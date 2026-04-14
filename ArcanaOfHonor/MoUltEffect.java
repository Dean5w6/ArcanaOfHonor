import greenfoot.*;

public class MoUltEffect extends Actor {
    private int[] yOffsets;
    private int baseX;
    private int baseY;
    private int ticksPerFrame;
    private int tick = 0;
    private int frameIndex = 0;

    private int totalIntroFrames;
    private int totalMainFrames = 5;
    private boolean mirror;

    private Runnable strike;
    private boolean strikeDone = false;
    private int damageFrameIndex;

    public MoUltEffect(boolean mirror,
        int baseX, int baseY,
        int[] yOffsets,
        int ticksPerFrame,
        Runnable strike,
        int damageFrameIndex) {

        this.mirror = mirror;
        this.baseX = baseX;
        this.baseY = baseY;
        this.yOffsets = (yOffsets != null) ? yOffsets : new int[0];
        this.ticksPerFrame = Math.max(1, ticksPerFrame);
        this.totalIntroFrames = this.yOffsets.length;
        this.strike = strike;

        int totalFrames = totalIntroFrames + totalMainFrames;
        if (damageFrameIndex < 0 || damageFrameIndex >= totalFrames) {

            this.damageFrameIndex = totalIntroFrames + 1;
        } else {
            this.damageFrameIndex = damageFrameIndex;
        }

        GreenfootImage img = new GreenfootImage("mo_ultimate_effect1.png");
        img.scale(494, 502);
        if (mirror) img.mirrorHorizontally();
        setImage(img);
    }

    public void initialize() {
        tick = 0;
        frameIndex = 0;
        renderFrame();
    }

    public void act() {
        tick++;
        if (tick < ticksPerFrame) return;
        tick = 0;

        frameIndex++;
        int totalFrames = totalIntroFrames + totalMainFrames;
        if (frameIndex >= totalFrames) {
            if (getWorld() != null) getWorld().removeObject(this);
            return;
        }

        renderFrame();
    }

    private void renderFrame() {
        int totalFrames = totalIntroFrames + totalMainFrames;

        if (!strikeDone && strike != null && frameIndex == damageFrameIndex) {
            strike.run();
            strikeDone = true;
        }

        GreenfootImage img;
        int x = baseX;
        int y = baseY;

        if (frameIndex < totalIntroFrames) {

            img = new GreenfootImage("mo_ultimate_effect1.png");
            img.scale(494, 502);
            if (mirror) img.mirrorHorizontally();

            int off = yOffsets[frameIndex];
            y = baseY + off;

        } else {

            int mainIndex = frameIndex - totalIntroFrames;
            int spriteNum = 2 + mainIndex;
            img = new GreenfootImage("mo_ultimate_effect" + spriteNum + ".png");
            img.scale(494, 502);
            if (mirror) img.mirrorHorizontally();
        }

        setImage(img);
        setLocation(x, y);
    }
}