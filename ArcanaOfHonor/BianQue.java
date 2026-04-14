import greenfoot.*;

public class BianQue extends Hero {
    public BianQue(boolean isEnemy) {
        super(isEnemy, 2000, "Bian Que");
        setImage(setupImage("bian_idle.png"));
    }
 
    @Override
    protected String getHeroPrefix() {
        return "bian"; 
    }
}