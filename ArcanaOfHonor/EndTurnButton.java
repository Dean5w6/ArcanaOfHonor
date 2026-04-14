import greenfoot.*;

public class EndTurnButton extends Actor {
 
    public static final int BIAN_WIDTH  = 302;
    public static final int BIAN_HEIGHT = 85;
    
    public static final int GANMO_WIDTH  = 250;
    public static final int GANMO_HEIGHT = 177;

    private boolean isGanMoStyle = false;
 
    public EndTurnButton() {
        this(false);
    }
 
    public EndTurnButton(boolean isGanMoStyle) {
        this.isGanMoStyle = isGanMoStyle;
        GreenfootImage img;

        if (isGanMoStyle) {
            img = new GreenfootImage("ganMo_end_turn.png");
            img.scale(GANMO_WIDTH, GANMO_HEIGHT);
        } else {
            img = new GreenfootImage("bianBaiQi_end_turn.png");
            img.scale(BIAN_WIDTH, BIAN_HEIGHT);
        }

        setImage(img);
    }

    public void act() {
        if (BattleWorld.getManager() == null || BattleWorld.getManager().isGameLocked()) {
            return;
        }
        if (Greenfoot.mouseClicked(this)) {
            BattleWorld.getManager().endPlayerTurn();
        }
    }
}