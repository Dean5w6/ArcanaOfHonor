import greenfoot.*;
public abstract class Bar extends Actor {
    public void hide() {
        getImage().setTransparency(0);
    }
    
    public void show() {
        getImage().setTransparency(255);
    }
}