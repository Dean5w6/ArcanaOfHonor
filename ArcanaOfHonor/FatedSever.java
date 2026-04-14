import greenfoot.*;
import java.util.ArrayList;

public class FatedSever extends StatusEffect {

    public boolean pending = true;

    public FatedSever(Hero victim) {
        super(victim, 2, 1);
        setImage(new GreenfootImage(1, 1));
    }

    public static void handleAfterPlayerRefill(BattleWorld w) {
        java.util.ArrayList < FatedSever > toRemove = new java.util.ArrayList < > ();
        for (FatedSever fs: new java.util.ArrayList < > (w.getObjects(FatedSever.class))) {
            if (!fs.pending) {
                toRemove.add(fs);
                continue;
            }
            if (w.getPlayerTeam().contains(fs.target)) {
                java.util.ArrayList < SkillCard > rm = new java.util.ArrayList < > ();
                for (SkillCard c: w.getObjects(SkillCard.class))
                    if (c.owner == fs.target) rm.add(c);
                if (!rm.isEmpty()) w.removeObjects(rm);

                w.reflowSkilldockAll();

                fs.pending = false;
                toRemove.add(fs);
                if (BattleWorld.getManager() != null)
                    BattleWorld.getManager().logEvent("Fated Sever purged player's cards of " + fs.target.name);
            }
        }
        for (FatedSever fs: toRemove)
            if (fs.getWorld() != null) w.removeObject(fs);
    }

    public static void handleAfterAIGenerate(BattleManager mgr) {
        BattleWorld w = (BattleWorld) mgr.getWorld();
        ArrayList < FatedSever > toRemove = new ArrayList < > ();
        for (FatedSever fs: new ArrayList < > (w.getObjects(FatedSever.class))) {
            if (!fs.pending) {
                toRemove.add(fs);
                continue;
            }
            if (w.getEnemyTeam().contains(fs.target)) {
                int n = mgr.purgeAIHandFor(fs.target);
                fs.pending = false;
                toRemove.add(fs);
                mgr.logEvent("Fated Sever purged " + n + " AI cards of " + fs.target.name);
            }
        }
        for (FatedSever fs: toRemove)
            if (fs.getWorld() != null) w.removeObject(fs);
    }
}