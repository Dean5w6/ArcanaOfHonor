import greenfoot.*;
import java.util.ArrayList;

public class Animation extends Actor {
    private ArrayList < GreenfootImage > frames;
    private int currentFrame = 0;
    private int animationTimer = 0;
    private int animationSpeed;

    private int triggerFrame;
    private Runnable triggerAction;
    private boolean hasTriggered = false;

    private int holdFrame = -1;
    private int holdDuration = 0;
    private int holdTimer = 0;

    public Animation(String filePrefix,
        int frameCount,
        int startFrame,
        int speed,
        boolean mirror,
        int triggerFrame,
        Runnable triggerAction,
        int holdFrame,
        int holdDuration) {
        this.animationSpeed = speed;
        this.triggerFrame = triggerFrame;
        this.triggerAction = triggerAction;
        this.holdFrame = holdFrame;
        this.holdDuration = holdDuration;

        frames = new ArrayList < > ();
        for (int i = startFrame; i <= frameCount; i++) {
            GreenfootImage frame = new GreenfootImage(filePrefix + i + ".png");
            frame.scale(494, 502);
            if (mirror) frame.mirrorHorizontally();
            frames.add(frame);
        }

        if (!frames.isEmpty()) {
            setImage(frames.get(0));
        }
    }

    public Animation(String filePrefix,
        int frameCount,
        int startFrame,
        int speed,
        int triggerFrame,
        Runnable triggerAction,
        int holdFrame,
        int holdDuration) {
        this(filePrefix, frameCount, startFrame, speed, false, triggerFrame, triggerAction, holdFrame, holdDuration);
    }

    public Animation(String filePrefix, int frameCount, int speed) {
        this(filePrefix, frameCount, 1, speed, false, -1, null, -1, 0);
    }

    public Animation(String filePrefix, int frameCount, int speed,
        int triggerFrame, Runnable triggerAction) {
        this(filePrefix, frameCount, 1, speed, false, triggerFrame, triggerAction, -1, 0);
    }

    public void act() {

        if (currentFrame == holdFrame && holdTimer < holdDuration) {
            holdTimer++;
            return;
        }

        animationTimer++;
        if (animationTimer >= animationSpeed) {
            animationTimer = 0;
            currentFrame++;

            if (!hasTriggered && triggerAction != null && currentFrame == triggerFrame) {
                triggerAction.run();
                hasTriggered = true;
            }

            if (currentFrame >= frames.size()) {
                if (getWorld() != null) {
                    getWorld().removeObject(this);
                }
            } else {
                setImage(frames.get(currentFrame));
            }
        }
    }

    public void mirrorFramesHorizontally() {
        if (frames == null) return;

        for (GreenfootImage frame: frames) {
            frame.mirrorHorizontally();
        }

        GreenfootImage img = getImage();
        if (img != null) img.mirrorHorizontally();
    }
}