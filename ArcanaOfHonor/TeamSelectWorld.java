import greenfoot.*;
public class TeamSelectWorld extends World {
    public TeamSelectWorld() {
        super(1280, 720, 1);
        setBackground("team_select_background.png");
        addObject(new GanMoCard(), 480, 285);
        addObject(new BianBaiQiCard(), 800, 285);
        addObject(new LockedCard("doliaHeino_locked.png"), 480, 435);
        addObject(new LockedCard("jingYao_locked.png"), 800, 435);
    }
    public void selectTeam(String teamName) {
        Greenfoot.setWorld(new BattleWorld(teamName));
    }
}