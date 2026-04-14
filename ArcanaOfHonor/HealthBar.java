import greenfoot.*;

public class HealthBar extends Bar {
    private GreenfootImage frame;
    private GreenfootImage fill;

    public HealthBar(boolean isEnemy) {
        frame = new GreenfootImage("bar_frame.png");
        frame.scale(94, 10);

        if (isEnemy) {
            fill = new GreenfootImage("enemy_health_bar_fill.png");
        } else {
            fill = new GreenfootImage("health_bar_fill.png");
        }
        fill.scale(94, 10);

        updateBar(100, 100);
    }

    public void updateBar(int current, int max) {
        GreenfootImage newBar = new GreenfootImage(frame);
        int newWidth = (int)((double) current / max * 94);

        if (newWidth > 0) {
            GreenfootImage fillCopy = new GreenfootImage(fill);
            fillCopy.scale(newWidth, 10);
            newBar.drawImage(fillCopy, 0, 0);
        }

        setImage(newBar);
    }
}