import greenfoot.*;

public class BattleResultBanner extends Actor {

    public enum ResultType {
        VICTORY,
        DEFEAT
    }

    public BattleResultBanner(ResultType type) {
        String filename = (type == ResultType.VICTORY) ? "victory.png" : "defeat.png";
        GreenfootImage img = new GreenfootImage(filename);
 
        int targetWidth = 500; 
        int targetHeight = (int)((double) img.getHeight() * targetWidth / img.getWidth());
        img.scale(targetWidth, targetHeight);

        setImage(img);
    }
}
