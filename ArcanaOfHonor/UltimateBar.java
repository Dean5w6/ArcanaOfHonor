import greenfoot.*;

public class UltimateBar extends Bar {
    private GreenfootImage frame;
    private GreenfootImage fill;
    private GreenfootImage fullBarImage;

    public UltimateBar() {
        frame = new GreenfootImage("bar_frame.png");
        fill = new GreenfootImage("ultimate_bar_fill.png");
        fullBarImage = new GreenfootImage("ultimate_bar_full.png");

        frame.scale(94, 10);
        fill.scale(94, 10);
        fullBarImage.scale(94, 10);

        updateBar(0, Hero.MAX_ULTIMATE_CHARGE);
    }

    public void updateBar(int current, int max) {

        if (current >= max) {
            setImage(fullBarImage);
            return;
        }

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