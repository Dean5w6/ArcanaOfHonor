import greenfoot.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BattleWorld extends World {
    private BattleManager battleManager;
    private ArrayList < Hero > playerTeam = new ArrayList < > ();
    private ArrayList < Hero > enemyTeam = new ArrayList < > ();
    private Hero playerHero1, playerHero2;
    private static BattleManager instance_BattleManager;
    private DebugDisplay debugDisplay;
    private static final int BIAN_END_X = 1080;
    private static final int BIAN_END_Y = 660;
    private static final int BIAN_AP1_X = 1105 - 35;
    private static final int BIAN_AP1_Y = 590;
    private static final int BIAN_AP2_X = 1105;
    private static final int BIAN_AP2_Y = 590;

    private static final int GANMO_END_X = 1088;
    private static final int GANMO_END_Y = 620;
    private static final int GANMO_AP1_X = 1105 - 35;
    private static final int GANMO_AP1_Y = 550;
    private static final int GANMO_AP2_X = 1105;
    private static final int GANMO_AP2_Y = 550;
    private boolean battleResultShown = false;

    public BattleWorld(String selectedTeam) {
        super(1280, 720, 1);
        setBackground("battle_background1.png");
        setupBattle(selectedTeam);
    }

    public static BattleManager getManager() {
        return instance_BattleManager;
    }

    private void setupBattle(String selectedTeam) {
        DebugDisplay debugDisplay = new DebugDisplay();
        addObject(debugDisplay, getWidth() / 2, 150);

        Hero enemyHero1 = null, enemyHero2 = null;

        if ("BianBaiQi".equals(selectedTeam)) {

            playerHero1 = new BaiQi(false);
            playerHero2 = new BianQue(false);

            enemyHero1 = new GanJiang(true);
            enemyHero2 = new MoYe(true);

        } else if ("GanMo".equals(selectedTeam)) {

            playerHero1 = new GanJiang(false);
            playerHero2 = new MoYe(false);

            enemyHero1 = new BaiQi(true);
            enemyHero2 = new BianQue(true);

        } else {

            playerHero1 = new BaiQi(false);
            playerHero2 = new BianQue(false);
            enemyHero1 = new GanJiang(true);
            enemyHero2 = new MoYe(true);
        }

        enemyTeam.add(enemyHero1);
        enemyTeam.add(enemyHero2);
        playerTeam.add(playerHero1);
        playerTeam.add(playerHero2);

        battleManager = new BattleManager(debugDisplay, enemyHero1, enemyHero2);
        instance_BattleManager = battleManager;
        addObject(battleManager, 0, 0);

        addObject(playerHero1, 420, 260);
        addObject(playerHero2, 220, 260);
        addObject(enemyTeam.get(0), 860, 260);
        addObject(enemyTeam.get(1), 1060, 260);

        setupUI(playerHero1, playerHero2, enemyTeam.get(0), enemyTeam.get(1));
        setupInitialSkilldock();
        battleManager.startPlayerTurn();
    }

    private void showBattleResult(boolean playerWon) {
        if (battleResultShown) return;
        battleResultShown = true;

        BattleResultBanner.ResultType type = playerWon ?
            BattleResultBanner.ResultType.VICTORY :
            BattleResultBanner.ResultType.DEFEAT;

        BattleResultBanner banner = new BattleResultBanner(type);

        int x = getWidth() / 2;
        int y = 100;
        addObject(banner, x, y);

        if (BattleWorld.getManager() != null) {
            BattleWorld.getManager().lockGame();
        }
    }

    public void checkForBattleEnd() {
        if (battleResultShown) return;

        boolean anyPlayerAlive = false;
        for (Hero h: playerTeam) {
            if (h != null && h.getWorld() != null && h.isAlive()) {
                anyPlayerAlive = true;
                break;
            }
        }

        boolean anyEnemyAlive = false;
        for (Hero h: enemyTeam) {
            if (h != null && h.getWorld() != null && h.isAlive()) {
                anyEnemyAlive = true;
                break;
            }
        }

        if (!anyPlayerAlive) {
            showBattleResult(false);
        } else if (!anyEnemyAlive) {
            showBattleResult(true);
        }
    }

    public void toggleDebug() {
        if (debugDisplay == null) return;
        if (debugDisplay.getWorld() != null) {
            removeObject(debugDisplay);
        } else {
            addObject(debugDisplay, getWidth() / 2, 150);
        }
    }

    public SkillCard createNewCardInstance(SkillCard template) {
        if (template instanceof BaiQiSkill1Card) return new BaiQiSkill1Card(template.owner);
        if (template instanceof BaiQiSkill2Card) return new BaiQiSkill2Card(template.owner);
        if (template instanceof BianQueSkill1Card) return new BianQueSkill1Card(template.owner);
        if (template instanceof BianQueSkill2Card) return new BianQueSkill2Card(template.owner);

        if (template instanceof GanJiangSkill1Card) return new GanJiangSkill1Card(template.owner);
        if (template instanceof GanJiangSkill2Card) return new GanJiangSkill2Card(template.owner);
        if (template instanceof GanJiangUltimateCard) return new GanJiangUltimateCard(template.owner);
        if (template instanceof MoYeSkill1Card) return new MoYeSkill1Card(template.owner);
        if (template instanceof MoYeSkill2Card) return new MoYeSkill2Card(template.owner);
        if (template instanceof MoYeUltimateCard) return new MoYeUltimateCard(template.owner);
        return null;
    }

    public void reorganizeSkilldock(SkillCard playedCard) {
        int playedCardX = playedCard.getX();
        int cardSpacing = 90;
        for (SkillCard card: getObjects(SkillCard.class)) {
            if (card.isCardPlayed()) continue;
            if (card.getX() > playedCardX) {
                card.setLocation(card.getX() - cardSpacing, card.getY());
            }
        }
    }

    public void cleanupPlayedCards() {
        List < SkillCard > cardsToRemove = new ArrayList < > ();
        for (SkillCard card: getObjects(SkillCard.class)) {
            if (card.isCardPlayed()) {
                cardsToRemove.add(card);
            }
        }
        removeObjects(cardsToRemove);

    }

    public void refillSkilldock() {
        List < SkillCard > remainingCards = getObjects(SkillCard.class);
        int cardsNeeded = 5 - remainingCards.size();
        if (cardsNeeded <= 0) {
            FatedSever.handleAfterPlayerRefill(this);
            return;
        }

        ArrayList < SkillCard > newCards = new ArrayList < > ();

        if (battleManager.getTurnNumber() == 1) {

            if ((playerHero1 instanceof BaiQi && playerHero2 instanceof BianQue) ||
                (playerHero1 instanceof BianQue && playerHero2 instanceof BaiQi)) {

                Hero bai = (playerHero1 instanceof BaiQi) ? playerHero1 : playerHero2;
                Hero bian = (playerHero1 instanceof BianQue) ? playerHero1 : playerHero2;

                newCards.add(new BaiQiSkill1Card(bai));
                newCards.add(new BaiQiSkill1Card(bai));
                newCards.add(new BianQueSkill1Card(bian));
                newCards.add(new BianQueSkill2Card(bian));

                ArrayList < SkillCard > pool = getCardGenerationPool();
                if (!pool.isEmpty()) {
                    SkillCard template = pool.get(Greenfoot.getRandomNumber(pool.size()));
                    newCards.add(createNewCardInstance(template));
                }
            } else if ((playerHero1 instanceof GanJiang && playerHero2 instanceof MoYe) ||
                (playerHero1 instanceof MoYe && playerHero2 instanceof GanJiang)) {

                Hero gan = (playerHero1 instanceof GanJiang) ? playerHero1 : playerHero2;
                Hero mo = (playerHero1 instanceof MoYe) ? playerHero1 : playerHero2;

                newCards.add(new GanJiangSkill1Card(gan));
                newCards.add(new GanJiangSkill2Card(gan));
                newCards.add(new MoYeSkill1Card(mo));
                newCards.add(new MoYeSkill2Card(mo));

                ArrayList < SkillCard > pool = getCardGenerationPool();
                if (!pool.isEmpty()) {
                    SkillCard template = pool.get(Greenfoot.getRandomNumber(pool.size()));
                    newCards.add(createNewCardInstance(template));
                }
            } else {
                ArrayList < SkillCard > pool = getCardGenerationPool();
                while (newCards.size() < 5 && !pool.isEmpty()) {
                    SkillCard template = pool.get(Greenfoot.getRandomNumber(pool.size()));
                    newCards.add(createNewCardInstance(template));
                }
            }

            java.util.Collections.shuffle(newCards);
        } else {

            for (Hero hero: playerTeam) {
                if (hero == null || hero.getWorld() == null || !hero.isAlive()) continue;
                if (hero.isUltimateReady() && newCards.size() < cardsNeeded) {
                    boolean ultInHand = false;
                    for (SkillCard c: remainingCards) {
                        if (c.owner == hero &&
                            (c instanceof BaiQiUltimateCard || c instanceof BianQueUltimateCard ||
                                c instanceof GanJiangUltimateCard || c instanceof MoYeUltimateCard)) {
                            ultInHand = true;
                            break;
                        }
                    }
                    if (!ultInHand) {
                        if (hero instanceof BaiQi) newCards.add(new BaiQiUltimateCard(hero));
                        else if (hero instanceof BianQue) newCards.add(new BianQueUltimateCard(hero));
                        else if (hero instanceof GanJiang) newCards.add(new GanJiangUltimateCard(hero));
                        else if (hero instanceof MoYe) newCards.add(new MoYeUltimateCard(hero));
                    }
                }
            }

            ArrayList < SkillCard > pool = getCardGenerationPool();
            while (newCards.size() < cardsNeeded && !pool.isEmpty()) {
                SkillCard template = pool.get(Greenfoot.getRandomNumber(pool.size()));
                newCards.add(createNewCardInstance(template));
            }
        }

        int cardStartX = 60, cardSpacing = 90, cardNormalY = 650;
        int firstEmptySlotIndex = remainingCards.size();
        for (int i = 0; i < newCards.size(); i++) {
            SkillCard newCard = newCards.get(i);
            int cardX = cardStartX + ((firstEmptySlotIndex + i) * cardSpacing);
            int cardY = cardNormalY;
            if (newCard instanceof BaiQiUltimateCard || newCard instanceof BianQueUltimateCard ||
                newCard instanceof GanJiangUltimateCard || newCard instanceof MoYeUltimateCard) {
                cardY -= 16;
            }
            addObject(newCard, cardX, cardY);
        }

        FatedSever.handleAfterPlayerRefill(this);
    }

    private void setupInitialSkilldock() {

    }

    private ArrayList < SkillCard > getCardGenerationPool() {
        ArrayList < SkillCard > pool = new ArrayList < > ();

        for (Hero hero: playerTeam) {
            if (hero == null) continue;
            if (hero.getWorld() == null || !hero.isAlive()) continue;

            if (hero instanceof BaiQi) {
                BaiQi bq = (BaiQi) hero;

                pool.add(bq.isStanceActive() ? new BaiQiSkill2Card(hero) : new BaiQiSkill1Card(hero));
                pool.add(new BaiQiSkill1Card(hero));
            } else if (hero instanceof BianQue) {

                pool.add(new BianQueSkill1Card(hero));
                pool.add(new BianQueSkill2Card(hero));
            } else if (hero instanceof GanJiang) {

                pool.add(new GanJiangSkill1Card(hero));
                pool.add(new GanJiangSkill2Card(hero));
            } else if (hero instanceof MoYe) {

                pool.add(new MoYeSkill1Card(hero));
                pool.add(new MoYeSkill2Card(hero));
            }
        }

        return pool;
    }

    public void updateSkilldockForStance(BaiQi baiQi, boolean isEnteringStance) {
        ArrayList < SkillCard > toSwap = new ArrayList < > ();

        for (SkillCard card: getObjects(SkillCard.class)) {
            if (card == null) continue;
            if (card.isCardPlayed()) continue;
            if (card.getWorld() == null) continue;
            if (card.owner != baiQi) continue;

            if (isEnteringStance && card instanceof BaiQiSkill1Card) {
                toSwap.add(card);
            } else if (!isEnteringStance && card instanceof BaiQiSkill2Card) {
                toSwap.add(card);
            }
        }

        for (SkillCard oldCard: toSwap) {
            int x = oldCard.getX();
            int y = oldCard.getY();

            SkillCard newCard = isEnteringStance ?
                new BaiQiSkill2Card(baiQi) :
                new BaiQiSkill1Card(baiQi);

            addObject(newCard, x, y);
            removeObject(oldCard);
        }
    }

    private void setupUI(Hero p1, Hero p2, Hero e1, Hero e2) {
        HealthBar p1_hp = new HealthBar(false), p2_hp = new HealthBar(false);
        HealthBar e1_hp = new HealthBar(true), e2_hp = new HealthBar(true);
        UltimateBar p1_ult = new UltimateBar(), p2_ult = new UltimateBar();
        UltimateBar e1_ult = new UltimateBar(), e2_ult = new UltimateBar();

        addObject(p1_hp, 420, 230);
        addObject(p1_ult, 420, 245);
        addObject(p2_hp, 220, 230);
        addObject(p2_ult, 220, 245);
        addObject(e1_hp, 860, 230);
        addObject(e1_ult, 860, 245);
        addObject(e2_hp, 1060, 230);
        addObject(e2_ult, 1060, 245);

        p1.setBars(p1_hp, p1_ult);
        p2.setBars(p2_hp, p2_ult);
        e1.setBars(e1_hp, e1_ult);
        e2.setBars(e2_hp, e2_ult);

        APCrystal ap1 = new APCrystal();
        APCrystal ap2 = new APCrystal();

        boolean isGanMoTeam =
            (playerHero1 instanceof GanJiang && playerHero2 instanceof MoYe) ||
            (playerHero1 instanceof MoYe && playerHero2 instanceof GanJiang);

        if (isGanMoTeam) {

            addObject(new EndTurnButton(true), GANMO_END_X, GANMO_END_Y);
            addObject(ap1, GANMO_AP1_X, GANMO_AP1_Y);
            addObject(ap2, GANMO_AP2_X, GANMO_AP2_Y);
        } else {

            addObject(new EndTurnButton(false), BIAN_END_X, BIAN_END_Y);
            addObject(ap1, BIAN_AP1_X, BIAN_AP1_Y);
            addObject(ap2, BIAN_AP2_X, BIAN_AP2_Y);
        }

        battleManager.registerAPCrystals(ap1, ap2);
    }

    public ArrayList < Hero > getPlayerTeam() {
        return this.playerTeam;
    }
    public ArrayList < Hero > getEnemyTeam() {
        return this.enemyTeam;
    }
    public ArrayList < Hero > getAlliesOf(Hero self) {
        ArrayList < Hero > out = new ArrayList < > ();
        ArrayList < Hero > src = playerTeam.contains(self) ? playerTeam : enemyTeam;
        for (Hero h: src)
            if (h != null && h.isAlive()) out.add(h);
        return out;
    }
    public ArrayList < Hero > getEnemiesOf(Hero self) {
        ArrayList < Hero > out = new ArrayList < > ();
        ArrayList < Hero > src = playerTeam.contains(self) ? enemyTeam : playerTeam;
        for (Hero h: src)
            if (h != null && h.isAlive()) out.add(h);
        return out;
    }

    public void onHeroDeath(Hero dead) {
        if (dead == null || dead.getWorld() == null) return;

        if (getManager() != null) getManager().logEvent("✖ " + dead.name + " has fallen.");

        ArrayList < StatusEffect > effs = new ArrayList < > ();
        for (StatusEffect se: getObjects(StatusEffect.class)) {
            if (se.target == dead) effs.add(se);
        }
        removeObjects(effs);

        ArrayList < SkillCard > cards = new ArrayList < > ();
        for (SkillCard c: getObjects(SkillCard.class)) {
            if (c.owner == dead) cards.add(c);
        }
        removeObjects(cards);

        if (getManager() != null) getManager().onHeroDeath(dead);

        if (dead.getHealthBar() != null && dead.getHealthBar().getWorld() != null) {
            removeObject(dead.getHealthBar());
        }
        if (dead.getUltimateBar() != null && dead.getUltimateBar().getWorld() != null) {
            removeObject(dead.getUltimateBar());
        }

        removeObject(dead);

        reflowPlayerSkilldock();

        checkForBattleEnd();
    }

    private void reflowPlayerSkilldock() {
        final int cardStartX = 60, cardSpacing = 90, cardNormalY = 650;

        ArrayList < SkillCard > hand = new ArrayList < > (getObjects(SkillCard.class));
        hand.removeIf(SkillCard::isCardPlayed);
        hand.sort((a, b) -> Integer.compare(a.getX(), b.getX()));

        for (int i = 0; i < hand.size(); i++) {
            SkillCard sc = hand.get(i);
            int x = cardStartX + (i * cardSpacing);
            int y = (sc.getImage().getHeight() > 122) ? cardNormalY - 16 : cardNormalY;
            sc.setLocation(x, y);
        }
    }

    public void reflowSkilldockAll() {
        int cardStartX = 60, cardSpacing = 90, cardNormalY = 650;

        java.util.ArrayList < SkillCard > cards = new java.util.ArrayList < > (getObjects(SkillCard.class));

        cards.sort(java.util.Comparator.comparingInt(Actor::getX));

        int i = 0;
        for (SkillCard c: cards) {
            if (c.isCardPlayed()) continue;
            int y = cardNormalY;
            if (c instanceof BaiQiUltimateCard || c instanceof BianQueUltimateCard ||
                c instanceof GanJiangUltimateCard || c instanceof MoYeUltimateCard) {
                y -= 16;
            }
            c.setLocation(cardStartX + i * cardSpacing, y);
            i++;
        }
    }
}