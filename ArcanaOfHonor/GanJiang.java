// GanJiang.java
import greenfoot.*;

public class GanJiang extends Hero {
    public GanJiang(boolean isEnemy) {
        super(isEnemy, 2500, "Gan Jiang");
        setImage(setupImage("gan_idle.png"));
    }
    @Override protected String getHeroPrefix() { return "gan"; }
}
