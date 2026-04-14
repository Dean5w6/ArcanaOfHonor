import greenfoot.*;

public class DimEffect extends Actor {
    public DimEffect(int width, int height) { 
        GreenfootImage img = new GreenfootImage(width, height);
        img.setColor(new Color(0, 0, 0, 77)); 
        img.fill();
        setImage(img);
    }
}