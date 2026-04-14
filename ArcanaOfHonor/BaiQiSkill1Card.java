import greenfoot.*;

public class BaiQiSkill1Card extends SkillCard {
    public BaiQiSkill1Card(Hero owner) {
        super("card_baiQi_skill1.png", false, owner);
    }

    @Override
    public void useSkill(BattleWorld world) { 
        if (owner instanceof BaiQi) {
            ((BaiQi) owner).enterExecutionStance();
        }
    }
}
