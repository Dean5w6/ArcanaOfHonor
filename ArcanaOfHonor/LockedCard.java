import greenfoot.*;

public class LockedCard extends Actor {
    public LockedCard(String imageFile) {
        GreenfootImage image = new GreenfootImage(imageFile);
         
        int newWidth = 300;
        int newHeight = 132;
        image.scale(newWidth, newHeight); 
        
        setImage(image);
    }
}