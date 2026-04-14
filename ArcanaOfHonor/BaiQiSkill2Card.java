import greenfoot.*;

public class BaiQiSkill2Card extends SkillCard {
    public BaiQiSkill2Card(Hero owner) {
        super("card_baiQi_skill2.png", false, owner);
    }

    @Override
    public void useSkill(BattleWorld world) {
        if (!(owner instanceof BaiQi)) return;

        BattleManager mgr = BattleWorld.getManager();
        if (mgr != null) mgr.logEvent("♦ Agonizing Strike by " + owner.name + " (cancel channel first)");

        BaiQi bqOwner = (BaiQi) owner;

        bqOwner.leaveExecutionStance();

        for (Hero enemy: world.getEnemiesOf(owner)) {
            enemy.dealDamage(145, "Agonizing Strike");
            owner.applyHeal(25, "Agonizing Strike");
            enemy.applyStatus(new Agony(enemy, 2));
            enemy.playHurtAnimation();

            Animation effect = new Animation("baiQi_skill2_effect", 3, 8);
            world.addObject(effect, enemy.getX(), enemy.getY());
        }

        for (StatusEffect effect: new java.util.ArrayList < > (world.getObjects(StatusEffect.class))) {
            if (effect instanceof ExecutionStance && effect.target == owner) {
                world.removeObject(effect);
            }
        }
    }

}