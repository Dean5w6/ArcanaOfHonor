import greenfoot.*;

public abstract class TeamCard extends Actor {
    
    protected String teamName;
    protected GreenfootImage idleImage;
    protected GreenfootImage hoverImage;

    public TeamCard(String teamName, String idleImgFile, String hoverImgFile) {
        this.teamName = teamName;
        this.idleImage = new GreenfootImage(idleImgFile);
        this.hoverImage = new GreenfootImage(hoverImgFile);

        int newWidth = 300;
        int newHeight = 132;
        this.idleImage.scale(newWidth, newHeight);
        this.hoverImage.scale(newWidth, newHeight);
        
        setImage(idleImage);
    }

    public void act() {
        if (Greenfoot.mouseMoved(this)) {
            setImage(hoverImage);
        }
        if (Greenfoot.mouseMoved(null) && !Greenfoot.mouseMoved(this)) {
            setImage(idleImage);
        }
        
        if (Greenfoot.mouseClicked(this)) {
            TeamSelectWorld world = (TeamSelectWorld) getWorld();
            world.selectTeam(this.teamName);
        }
    }
}