import greenfoot.*;
public class APCrystal extends Actor {
    private GreenfootImage fullImg = new GreenfootImage("ap_crystal_full.png");
    private GreenfootImage emptyImg = new GreenfootImage("ap_crystal_empty.png");
    public APCrystal() {
        fullImg.scale(28, 28);
        emptyImg.scale(28, 28);
        setImage(fullImg);
    }
    public void setFull(boolean isFull) {
        setImage(isFull ? fullImg : emptyImg);
    }
}