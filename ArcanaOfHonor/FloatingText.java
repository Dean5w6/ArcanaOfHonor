import greenfoot.*;

public class FloatingText extends Actor {
    private int life = 60;
    private int initialY;

    public FloatingText(String text, Color color) {
        GreenfootImage image = new GreenfootImage(text, 48, color, new Color(0,0,0,0));
        setImage(image);
    }

    @Override
    protected void addedToWorld(World world) {
        initialY = getY();
    }

    public void act() {
        life--;
        
        setLocation(getX(), getY() - 1);
        
        if (life < 30) {
            getImage().setTransparency((life * 255) / 30);
        }
        
        if (life <= 0) {
            getWorld().removeObject(this);
        }
    }
}