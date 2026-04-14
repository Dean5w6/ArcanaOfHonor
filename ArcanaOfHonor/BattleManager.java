import greenfoot.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class BattleManager extends Actor {

    private enum Turn {
        PLAYER,
        ENEMY_THINKING,
        ENEMY_ACTING,
        ROUND_END
    }
    private Turn currentTurn;

    private int actionPoints;
    private ArrayList < APCrystal > apCrystals = new ArrayList < > ();
    private boolean isLocked = false;
    private SkillCard activeSkillCard = null;
    private int sequenceStep = 0;
    private int sequenceTimer = 0;
    private DebugDisplay debugDisplay;
    private int turnNumber = 1;
    private ArrayList < SkillCard > aiHand = new ArrayList < > ();
    private Hero aiHero1;
    private Hero aiHero2;

    private int actionSeq = 0;
    private String currentActionTag = "-";
    private boolean debugToggleLatch = false;
    private boolean debugF5Latch = false;

    private ArrayList < Hero > deathQueue = new ArrayList < > ();
    private int deathWait = 0;

    private Hero seqTarget = null;
    private boolean seqHitApplied = false;
    private int moUltIntroTimer = 0;
    private int seqDamage = 0;
    private String seqSource = "";
    private int seqSwordQiStacks = 0;

    private boolean awaitingTargetSelection = false;

    public String getCurrentActionTag() {
        return currentActionTag;
    }
    private void setActionTag(String tag) {
        currentActionTag = tag;
    }

    public BattleManager(DebugDisplay display, Hero aiHero1, Hero aiHero2) {
        this.debugDisplay = display;
        this.aiHero1 = aiHero1;
        this.aiHero2 = aiHero2;
        setImage(new GreenfootImage(1, 1));

    }

    public void act() {
        if (Greenfoot.isKeyDown("f1")) {
            if (!debugToggleLatch) {
                debugToggleLatch = true;
                ((BattleWorld) getWorld()).toggleDebug();
            }
        } else {
            debugToggleLatch = false;
        }

        if (Greenfoot.isKeyDown("f5")) {
            if (!debugF5Latch) {
                debugF5Latch = true;
                applyDebugBuffF5();
            }
        } else {
            debugF5Latch = false;
        }

        if (activeSkillCard != null) {
            runSkillSequence();
            return;
        }

        switch (currentTurn) {
        case ENEMY_THINKING:

            sequenceTimer++;
            if (sequenceTimer > 45) {
                executeAITurn();
            }
            break;
        case ENEMY_ACTING:

            if (activeSkillCard == null) {
                currentTurn = Turn.ENEMY_THINKING;
                sequenceTimer = 0;
            }
            break;

        case ROUND_END:
            if (!deathQueue.isEmpty()) {
                if (deathWait > 0) {
                    deathWait--;
                } else {

                    BattleWorld w = (BattleWorld) getWorld();
                    for (Hero h: new ArrayList < > (deathQueue)) {
                        if (h != null && h.getWorld() != null) w.onHeroDeath(h);
                    }
                    deathQueue.clear();

                    turnNumber++;
                    startPlayerTurn();
                }
            }
            break;
        }
    }

    public void initializeFirstTurn() {
        currentTurn = Turn.PLAYER;
        logEvent("--- Turn 1 Start ---");
        actionPoints = ((BattleWorld) getWorld()).getPlayerTeam().size();
        updateAPCrystals();
        unlockGame();
    }

    public void startPlayerTurn() {
        currentTurn = Turn.PLAYER;
        BattleWorld world = (BattleWorld) getWorld();

        world.refillSkilldock();

        logEvent("--- Turn " + turnNumber + " Start (Player) ---");
        triggerTurnStartForTeam(livingTeam(world.getPlayerTeam()));

        actionPoints = livingTeam(world.getPlayerTeam()).size();
        updateAPCrystals();
        unlockGame();
    }

    public int getTurnNumber() {
        return this.turnNumber;
    }

    public void endPlayerTurn() {
        if (isLocked) return;
        lockGame();

        BattleWorld world = (BattleWorld) getWorld();
        world.cleanupPlayedCards();
        logEvent("Player turn ended. Entering enemy phase...");

        generateAIHand();

        actionPoints = livingTeam(world.getEnemyTeam()).size();
        updateAPCrystals();
        triggerTurnStartForTeam(livingTeam(world.getEnemyTeam()));

        currentTurn = Turn.ENEMY_THINKING;
        sequenceTimer = 0;
    }

    public void updateAIHandForStance(BaiQi baiQi, boolean isEnteringStance) {
        for (int i = 0; i < aiHand.size(); i++) {
            SkillCard c = aiHand.get(i);
            if (c == null || c.owner != baiQi) continue;
            if (isEnteringStance) {
                if (c instanceof BaiQiSkill1Card) aiHand.set(i, new BaiQiSkill2Card(baiQi));
            } else {
                if (c instanceof BaiQiSkill2Card) aiHand.set(i, new BaiQiSkill1Card(baiQi));
            }
        }
    }

    private void triggerOnActionFor(Hero actor) {
        BattleWorld world = (BattleWorld) getWorld();
        if (world == null || actor == null) return;

        for (StatusEffect effect: new java.util.ArrayList < > (world.getObjects(StatusEffect.class))) {
            if (effect != null && effect.target == actor) {
                logEvent("… onAction -> " + effect.getEffectName() + " on " + actor.name);
                effect.onAction(actor);
            }
        }
    }

    private void executeAITurn() {
        BattleWorld world = (BattleWorld) getWorld();

        ArrayList < Hero > playerAlive = livingTeam(world.getPlayerTeam());
        if (playerAlive.isEmpty()) {
            logEvent("No player targets alive. Ending round.");
            endRound();
            return;
        }

        pruneAIHand();
        ArrayList < Hero > aiAlive = livingTeam(world.getEnemyTeam());
        if (aiAlive.isEmpty()) {
            endRound();
            return;
        }

        if (aiHand == null || aiHand.isEmpty()) {
            logEvent("AI has no cards in hand; ending enemy turn.");
            endRound();
            return;
        }

        if (actionPoints > aiAlive.size()) {
            actionPoints = aiAlive.size();
            updateAPCrystals();
        }

        if (actionPoints <= 0) {
            endRound();
            return;
        }

        if (activeSkillCard != null) {
            currentTurn = Turn.ENEMY_ACTING;
            return;
        }

        SkillCard cardToPlay = null;

        for (SkillCard c: aiHand) {
            if (c instanceof BaiQiUltimateCard || c instanceof BianQueUltimateCard ||
                c instanceof GanJiangUltimateCard || c instanceof MoYeUltimateCard) {
                cardToPlay = c;
                break;
            }
        }

        if (cardToPlay == null) {
            for (SkillCard c: aiHand) {
                if (c.owner != null && c.owner.getWorld() != null && c.owner.getCurrentHp() > 0 &&
                    c instanceof BaiQiSkill2Card && ((BaiQi) c.owner).isStanceActive()) {
                    cardToPlay = c;
                    break;
                }
            }
            if (cardToPlay == null) {
                for (SkillCard c: aiHand) {
                    if (c.owner != null && c.owner.getWorld() != null && c.owner.getCurrentHp() > 0 &&
                        c instanceof BaiQiSkill1Card) {
                        cardToPlay = c;
                        break;
                    }
                }
            }
            if (cardToPlay == null) {
                for (SkillCard c: aiHand) {
                    if (c.owner != null && c.owner.getWorld() != null && c.owner.getCurrentHp() > 0 &&
                        c instanceof BianQueSkill1Card) {
                        cardToPlay = c;
                        break;
                    }
                }
            }
            if (cardToPlay == null) {
                for (SkillCard c: aiHand) {
                    if (c.owner != null && c.owner.getWorld() != null && c.owner.getCurrentHp() > 0 &&
                        c instanceof BianQueSkill2Card) {
                        cardToPlay = c;
                        break;
                    }
                }
            }
        }

        if (cardToPlay == null) {
            for (SkillCard c: aiHand) {
                if (c.owner != null && c.owner.getWorld() != null && c.owner.getCurrentHp() > 0) {
                    cardToPlay = c;
                    break;
                }
            }
        }

        if (cardToPlay == null) {
            logEvent("AI has no valid card to play; ending enemy turn.");
            endRound();
            return;
        }

        logEvent("Enemy used " + cardToPlay.getClass().getSimpleName() + "!");
        aiHand.remove(cardToPlay);

        startSkillSequence(cardToPlay);
        currentTurn = Turn.ENEMY_ACTING;
    }

    private void endRound() {
        currentTurn = Turn.ROUND_END;
        BattleWorld world = (BattleWorld) getWorld();
        logEvent("--- End of Turn " + turnNumber + " ---");

        for (StatusEffect effect: new ArrayList < > (world.getObjects(StatusEffect.class))) {
            effect.onTurnEnd();
        }

        prepareEndOfTurnDeaths();

        if (deathQueue.isEmpty()) {
            turnNumber++;
            startPlayerTurn();
        }

    }

    private void safeReflowForPlayerCard(SkillCard c, BattleWorld world) {

        if (c != null && c.getWorld() != null) {
            world.reorganizeSkilldock(c);
        }
    }

    private void generateAIHand() {
        aiHand.clear();
        BattleWorld world = (BattleWorld) getWorld();

        java.util.ArrayList < Hero > alive = new java.util.ArrayList < > ();
        for (Hero h: world.getEnemyTeam()) {
            if (h != null && h.getWorld() != null) alive.add(h);
        }

        if (turnNumber == 1) {
            Hero bai = null, bian = null, gan = null, mo = null;
            for (Hero h: alive) {
                if (h instanceof BaiQi) bai = h;
                else if (h instanceof BianQue) bian = h;
                else if (h instanceof GanJiang) gan = h;
                else if (h instanceof MoYe) mo = h;
            }

            if (bai != null && bian != null) {
                aiHand.add(new BaiQiSkill1Card(bai));
                aiHand.add(new BaiQiSkill1Card(bai));
                aiHand.add(new BianQueSkill1Card(bian));
                aiHand.add(new BianQueSkill2Card(bian));
            } else if (gan != null && mo != null) {
                aiHand.add(new GanJiangSkill1Card(gan));
                aiHand.add(new GanJiangSkill2Card(gan));
                aiHand.add(new MoYeSkill1Card(mo));
                aiHand.add(new MoYeSkill2Card(mo));
            }

            ArrayList < SkillCard > pool = getAICardPool();
            while (aiHand.size() < 5 && !pool.isEmpty()) {
                SkillCard t = pool.get(Greenfoot.getRandomNumber(pool.size()));
                aiHand.add(world.createNewCardInstance(t));
            }
            java.util.Collections.shuffle(aiHand);

            FatedSever.handleAfterAIGenerate(this);
            return;
        }

        for (Hero h: alive) {
            if (h.isUltimateReady() && !containsUltimateFor(h)) {
                SkillCard ult = createUltimateFor(h);
                if (ult != null && aiHand.size() < 5) aiHand.add(ult);
            }
        }

        ArrayList < SkillCard > pool = getAICardPool();
        while (aiHand.size() < 5 && !pool.isEmpty()) {
            SkillCard template = pool.get(Greenfoot.getRandomNumber(pool.size()));
            SkillCard fresh = world.createNewCardInstance(template);
            aiHand.add(fresh);
        }

        FatedSever.handleAfterAIGenerate(this);
    }

    private SkillCard weightedAICardTemplate() {
        BattleWorld w = (BattleWorld) getWorld();
        Hero bq = findAIHero(BaiQi.class);
        Hero bn = findAIHero(BianQue.class);

        ArrayList < Integer > tickets = new ArrayList < > ();

        if (bq != null) {
            tickets.add(0);
            tickets.add(1);
        }
        if (bn != null) {
            tickets.add(2);
            tickets.add(3);
        }

        if (tickets.isEmpty()) return null;

        int pick = tickets.get(Greenfoot.getRandomNumber(tickets.size()));
        switch (pick) {
        case 0:
        case 1:
            if (bq != null) return new BaiQiSkill1Card(bq);
            break;
        case 2:
            if (bn != null) return new BianQueSkill1Card(bn);
            break;
        case 3:
            if (bn != null) return new BianQueSkill2Card(bn);
            break;
        }
        return null;
    }

    public void startSkillSequence(SkillCard card) {
        if (card == null || card.owner == null || card.owner.getWorld() == null || card.owner.getCurrentHp() <= 0) {
            unlockGame();
            return;
        }

        if (isLocked && currentTurn == Turn.PLAYER) return;

        lockGame();

        int apBefore = (currentTurn == Turn.PLAYER) ? actionPoints : actionPoints;
        if (currentTurn == Turn.PLAYER) {
            spendAP(1);
        } else {
            actionPoints--;
            updateAPCrystals();
        }
        int apAfter = actionPoints;

        triggerOnActionFor(card.owner);

        if (card instanceof BaiQiUltimateCard || card instanceof BianQueUltimateCard || card instanceof GanJiangUltimateCard || card instanceof MoYeUltimateCard) {

            card.owner.resetUltimate();
        } else {

            card.owner.increaseUltimateCharge();
        }

        actionSeq++;
        String side = (currentTurn == Turn.PLAYER) ? "PLAYER" : "ENEMY";
        String tag = "AID#" + actionSeq + " [" + side + "] " + card.owner.name + " -> " + card.getClass().getSimpleName() +
            " | Turn " + turnNumber + " | AP " + apBefore + "->" + apAfter;
        setActionTag(tag);
        logEvent("▶ " + tag + " (sequence start)");

        this.activeSkillCard = card;
        this.sequenceStep = 0;
        this.sequenceTimer = 0;
    }

    private void runSkillSequence() {

        sequenceTimer++;
        BattleWorld world = (BattleWorld) getWorld();
        Hero caster = activeSkillCard.owner;

        if (isSingleTargetSkill(activeSkillCard)) {

            if (seqTarget == null) {
                java.util.ArrayList < Hero > options =
                    new java.util.ArrayList < > (world.getEnemiesOf(caster));
                options.removeIf(h -> h == null || h.getWorld() == null || h.getCurrentHp() <= 0);

                if (options.isEmpty()) {
                    endSkillSequence();
                    return;
                }

                if (currentTurn == Turn.PLAYER) {

                    if (options.size() == 1) {
                        seqTarget = options.get(0);
                    } else {
                        if (!awaitingTargetSelection) {
                            requestPlayerTargetSelection(options);
                        }

                        return;
                    }
                } else {

                    seqTarget = pickLowestHp(options);
                    if (seqTarget == null) {
                        endSkillSequence();
                        return;
                    }
                }
            }
        }

        if (activeSkillCard instanceof BaiQiSkill1Card) {
            if (sequenceStep == 0) {
                activeSkillCard.markAsPlayed();
                safeReflowForPlayerCard(activeSkillCard, world);
                for (Hero hero: world.getObjects(Hero.class)) {
                    if (hero != caster) {
                        hero.hide();
                    }
                }
                activeSkillCard.useSkill(world);
                world.updateSkilldockForStance((BaiQi) caster, true);

                if (caster.isEnemy && caster instanceof BaiQi) {
                    updateAIHandForStance((BaiQi) caster, true);
                }
                sequenceStep++;
                sequenceTimer = 0;
            } else if (sequenceStep == 1 && sequenceTimer > 10) {
                String[] frames = {
                    "baiQi_skill1_anim1",
                    "baiQi_skill1_anim2",
                    "baiQi_skill1_anim3",
                    "baiQi_skill1_anim4"
                };
                caster.playAnimation(frames, 8, true);
                sequenceStep++;
            } else if (sequenceStep == 2) {
                if (sequenceTimer > 40) {
                    for (Hero hero: world.getObjects(Hero.class)) {
                        if (hero != caster) hero.show();
                    }

                    if (caster instanceof BaiQi) {
                        caster.setImage(((BaiQi) caster).getChannelingPoseImage());
                    }
                    endSkillSequence();
                }
            }
        } else if (activeSkillCard instanceof BaiQiSkill2Card) {
            ArrayList < Hero > targets = world.getEnemiesOf(caster);
            if (sequenceStep == 0) {
                activeSkillCard.markAsPlayed();
                safeReflowForPlayerCard(activeSkillCard, world);

                for (Hero hero: world.getAlliesOf(caster)) {
                    if (hero != caster) hero.hide();
                }

                Runnable hitAction = () -> {

                    activeSkillCard.useSkill(world);

                    for (Hero target: targets) {
                        target.playHurtAnimation();
                        Animation effect = new Animation("baiQi_skill2_effect", 3, 8);
                        world.addObject(effect, target.getX(), target.getY());
                    }
                };

                Map < Integer, Runnable > triggers = new HashMap < > ();
                triggers.put(1, hitAction);

                String[] frames = {
                    "baiQi_ultimate_anim5",
                    "baiQi_ultimate_anim6",
                    "baiQi_skill1_anim6"
                };

                caster.playAnimation(frames, 8, 1, false, triggers);
                sequenceStep = 1;

            } else if (sequenceStep == 1) {
                if (!caster.isAnimating() && world.getObjects(Animation.class).isEmpty()) {

                    world.updateSkilldockForStance((BaiQi) caster, false);

                    if (caster.isEnemy && caster instanceof BaiQi) {
                        updateAIHandForStance((BaiQi) caster, false);
                    }

                    for (Hero hero: world.getAlliesOf(caster)) {
                        if (hero != caster) hero.show();
                    }
                    endSkillSequence();
                }
            }
        } else if (activeSkillCard instanceof BaiQiUltimateCard) {
            ArrayList < Hero > targets = world.getEnemiesOf(caster);
            if (sequenceStep == 0) {
                activeSkillCard.markAsPlayed();
                safeReflowForPlayerCard(activeSkillCard, world);
                world.addObject(new DimEffect(world.getWidth(), world.getHeight()), world.getWidth() / 2, world.getHeight() / 2);
                for (Hero hero: world.getObjects(Hero.class)) {
                    if (hero != caster && !targets.contains(hero)) {
                        hero.hide();
                    }
                }
                Runnable firstHitAction = () -> {
                    for (Hero target: targets) {
                        target.dealDamage(155, "Reaper's Judgment");
                        caster.applyHeal(35, "Reaper's Judgment");
                        target.applyStatus(new Agony(target, 2));
                        target.playHurtAnimation();
                        Animation effect1 = new Animation("baiQi_ultimate_effect", 4, 1, 9, -1, null, 0, 999);
                        world.addObject(effect1, target.getX(), target.getY());
                    }
                };
                Runnable secondHitAction = () -> {
                    for (Hero target: targets) {
                        target.dealDamage(155, "Reaper's Judgment");
                        caster.applyHeal(35, "Reaper's Judgment");
                        target.applyStatus(new Agony(target, 2));
                        target.playHurtAnimation();
                    }
                    world.removeObjects(world.getObjects(Animation.class));
                    for (Hero target: targets) {
                        Animation effect2 = new Animation("baiQi_ultimate_effect", 4, 2, 9, -1, null, -1, 0);
                        world.addObject(effect2, target.getX(), target.getY());
                    }
                };
                Map < Integer, Runnable > triggers = new HashMap < > ();
                triggers.put(5, firstHitAction);
                triggers.put(9, secondHitAction);
                String[] ultFrames = {
                    "baiQi_ultimate_anim1",
                    "baiQi_ultimate_anim2",
                    "baiQi_ultimate_anim3",
                    "baiQi_ultimate_anim4",
                    "baiQi_ultimate_anim5",
                    "baiQi_ultimate_anim6",
                    "baiQi_skill1_anim6",
                    "baiQi_ultimate_anim2",
                    "baiQi_ultimate_anim3",
                    "baiQi_ultimate_anim6",
                    "baiQi_skill1_anim6"
                };
                caster.playAnimation(ultFrames, 7, 3, false, triggers);
                sequenceStep = 1;
            } else if (sequenceStep == 1) {
                if (caster.isAnimating()) {
                    sequenceStep = 2;
                }
            } else if (sequenceStep == 2) {
                if (!caster.isAnimating() && world.getObjects(Animation.class).isEmpty()) {
                    world.removeObjects(world.getObjects(DimEffect.class));
                    for (Hero hero: world.getObjects(Hero.class)) {
                        hero.show();
                        if (hero instanceof BaiQi) {
                            BaiQi baiQiCaster = (BaiQi) hero;
                            if (baiQiCaster.getStance() == BaiQi.Stance.CHANNELING) {
                                baiQiCaster.setImage(baiQiCaster.getChannelingPoseImage());
                            }
                        }
                    }
                    endSkillSequence();
                }
            }
        } else if (activeSkillCard instanceof BianQueSkill1Card) {
            ArrayList < Hero > targets = world.getEnemiesOf(caster);
            if (sequenceStep == 0) {
                activeSkillCard.markAsPlayed();
                safeReflowForPlayerCard(activeSkillCard, world);
                for (Hero hero: world.getObjects(Hero.class)) {
                    if (hero != caster && !targets.contains(hero)) {
                        hero.hide();
                    }
                }
                GreenfootImage castImage = new GreenfootImage("bian_skill1_anim1.png");
                castImage.scale(494, 502);
                if (caster.isEnemy) castImage.mirrorHorizontally();
                caster.setImage(castImage);
                sequenceStep++;
                sequenceTimer = 0;

            } else if (sequenceStep == 1 && sequenceTimer > 15) {
                for (Hero specificTarget: targets) {
                    Runnable singleTargetDamageAction = () -> {
                        specificTarget.dealDamage(80, "Deadly Panacea");
                        specificTarget.applyStatus(new Poison(specificTarget, 2));
                        specificTarget.playHurtAnimation();
                    };
                    Animation effect = new Animation("bian_skill1_effect", 4, 9, 2, singleTargetDamageAction);
                    world.addObject(effect, specificTarget.getX(), specificTarget.getY());
                }
                sequenceStep++;

            } else if (sequenceStep == 2) {
                if (world.getObjects(Animation.class).isEmpty()) {

                    caster.setToRestPose();
                    for (Hero hero: world.getObjects(Hero.class)) hero.show();
                    endSkillSequence();
                }
            }

        } else if (activeSkillCard instanceof BianQueSkill2Card) {
            ArrayList < Hero > targets = world.getAlliesOf(caster);
            if (sequenceStep == 0) {
                activeSkillCard.markAsPlayed();
                safeReflowForPlayerCard(activeSkillCard, world);
                for (Hero hero: world.getObjects(Hero.class)) {
                    if (!targets.contains(hero)) hero.hide();
                }
                GreenfootImage castImage = new GreenfootImage("bian_skill2_anim1.png");
                castImage.scale(494, 502);
                if (caster.isEnemy) castImage.mirrorHorizontally();
                caster.setImage(castImage);
                sequenceStep++;
                sequenceTimer = 0;

            } else if (sequenceStep == 1 && sequenceTimer > 15) {
                for (Hero specificTarget: targets) {
                    Runnable singleTargetHealAction = () -> {
                        specificTarget.applyHeal(70, "Fatal Diagnosis");
                        specificTarget.applyStatus(new Cure(specificTarget, 2));
                    };
                    Animation effect = new Animation("bian_skill2_effect", 3, 9, 1, singleTargetHealAction);
                    world.addObject(effect, specificTarget.getX(), specificTarget.getY());
                }
                sequenceStep++;

            } else if (sequenceStep == 2) {
                if (world.getObjects(Animation.class).isEmpty()) {

                    caster.setToRestPose();
                    for (Hero hero: world.getObjects(Hero.class)) hero.show();
                    endSkillSequence();
                }
            }

        } else if (activeSkillCard instanceof BianQueUltimateCard) {
            ArrayList < Hero > targets = world.getEnemiesOf(caster);
            if (sequenceStep == 0) {
                activeSkillCard.markAsPlayed();
                safeReflowForPlayerCard(activeSkillCard, world);
                world.addObject(new DimEffect(world.getWidth(), world.getHeight()),
                    world.getWidth() / 2, world.getHeight() / 2);

                ArrayList < Hero > involvedHeroes = new ArrayList < > ();
                involvedHeroes.add(caster);
                involvedHeroes.addAll(targets);
                for (Cure cureEffect: world.getObjects(Cure.class)) {
                    if (world.getAlliesOf(caster).contains(cureEffect.target)) {
                        if (!involvedHeroes.contains(cureEffect.target)) involvedHeroes.add(cureEffect.target);
                    }
                }
                for (Hero hero: world.getObjects(Hero.class)) {
                    if (!involvedHeroes.contains(hero)) hero.hide();
                }
                sequenceStep++;
                sequenceTimer = 0;

            } else if (sequenceStep == 1 && sequenceTimer > 15) {
                GreenfootImage castImage = new GreenfootImage("bian_skill1_anim1.png");
                castImage.scale(494, 502);
                if (caster.isEnemy) castImage.mirrorHorizontally();
                caster.setImage(castImage);

                Runnable damageAction = () -> {
                    activeSkillCard.useSkill(world);
                    for (Hero enemy: targets) enemy.playHurtAnimation();
                };
                for (Hero target: targets) {
                    Animation ultimateEffect = new Animation("bian_ultimate_effect", 8, 6, 5, damageAction);
                    world.addObject(ultimateEffect, target.getX(), target.getY());
                    damageAction = null;
                }
                sequenceStep++;

            } else if (sequenceStep == 2) {
                if (world.getObjects(Animation.class).isEmpty()) {

                    caster.setToRestPose();
                    world.removeObjects(world.getObjects(DimEffect.class));
                    for (Hero hero: world.getObjects(Hero.class)) hero.show();
                    endSkillSequence();
                }
            }
        } else if (activeSkillCard instanceof GanJiangSkill1Card) {
            BattleWorld w = (BattleWorld) getWorld();
            if (sequenceStep == 0) {
                activeSkillCard.markAsPlayed();
                safeReflowForPlayerCard(activeSkillCard, w);

                for (Hero h: w.getObjects(Hero.class)) {
                    if (h != caster && !w.getEnemiesOf(caster).contains(h)) h.hide();
                }

                caster.playAnimation(new String[] {
                    "gan_skill1_anim1",
                    "gan_skill1_anim2",
                    "gan_skill1_anim3"
                }, 8, false);

                sequenceStep = 1;
                sequenceTimer = 0;

            } else if (sequenceStep == 1 && sequenceTimer > 10) {
                if (seqTarget == null || seqTarget.getWorld() == null || seqTarget.getCurrentHp() <= 0) {
                    endSkillSequence();
                    return;
                }

                seqDamage = 200;
                seqSource = "Temper Strike";
                seqHitApplied = false;

                Animation fx = new Animation("gan_skill1_effect", 4, 9);
                w.addObject(fx, seqTarget.getX(), seqTarget.getY());

                sequenceStep = 2;

            } else if (sequenceStep == 2) {
                if (!seqHitApplied && w.getObjects(Animation.class).isEmpty()) {
                    if (seqTarget != null && seqTarget.getWorld() != null) {
                        seqTarget.dealDamage(seqDamage, seqSource);
                        seqTarget.playHurtAnimation();
                    }
                    seqHitApplied = true;
                }
                if (seqHitApplied && !caster.isAnimating()) {
                    caster.playAnimation(new String[] {});
                    for (Hero h: w.getObjects(Hero.class)) h.show();
                    endSkillSequence();
                }
            }
        } else if (activeSkillCard instanceof GanJiangSkill2Card) {
            if (sequenceStep == 0) {
                activeSkillCard.markAsPlayed();
                safeReflowForPlayerCard(activeSkillCard, world);
                for (Hero h: world.getAlliesOf(caster))
                    if (h != caster) h.hide();

                Runnable boom = () -> {
                    for (Hero e: living(world.getEnemiesOf(caster))) {
                        e.dealDamage(130, "Molten Rebellion");
                        e.playHurtAnimation();
                        Animation fx = new Animation("gan_skill2_effect", 3, 9);
                        world.addObject(fx, e.getX(), e.getY());
                    }
                };
                java.util.Map < Integer, Runnable > trig = new java.util.HashMap < > ();
                trig.put(1, boom);

                caster.playAnimation(new String[] {
                    "gan_skill2_anim1",
                    "gan_skill2_anim2",
                    "gan_skill2_anim3"
                }, 8, 1, false, trig);
                sequenceStep = 1;
            } else if (sequenceStep == 1) {
                if (!caster.isAnimating() && world.getObjects(Animation.class).isEmpty()) {
                    for (Hero h: world.getObjects(Hero.class)) h.show();
                    endSkillSequence();
                }
            }
        } else if (activeSkillCard instanceof GanJiangUltimateCard) {
            if (sequenceStep == 0) {
                activeSkillCard.markAsPlayed();
                safeReflowForPlayerCard(activeSkillCard, world);

                world.addObject(new DimEffect(world.getWidth(), world.getHeight()),
                    world.getWidth() / 2, world.getHeight() / 2);

                Hero tgt = seqTarget;
                if (tgt == null || tgt.getWorld() == null || tgt.getCurrentHp() <= 0) {
                    world.removeObjects(world.getObjects(DimEffect.class));
                    endSkillSequence();
                    return;
                }

                for (Hero h: world.getObjects(Hero.class)) {
                    if (h != caster && h != tgt) h.hide();
                    else h.show();
                }

                seqSwordQiStacks = 0;
                for (SwordQi sq: world.getObjects(SwordQi.class)) {
                    if (sq.target == tgt && sq.stacks > 0) {
                        seqSwordQiStacks = 1;
                        break;
                    }
                }

                caster.playAnimation(new String[] {
                    "gan_ultimate_anim1",
                    "gan_ultimate_anim2",
                    "gan_ultimate_anim3",
                    "gan_ultimate_anim4",
                    "gan_ultimate_anim5",
                    "gan_ultimate_anim6",
                    "gan_ultimate_anim7"
                }, 7, 1, false, new java.util.HashMap < Integer, Runnable > ());

                sequenceStep = 1;

            } else if (sequenceStep == 1) {

                if (caster.isAnimating()) return;

                Hero tgt = seqTarget;
                if (tgt != null && tgt.getWorld() != null) {

                    tgt.dealDamage(500, "Severing Fate");

                    tgt.decreaseUltimateCharge(1);

                    Animation fx = new Animation(
                        "gan_ultimate_effect",
                        4,
                        1,
                        9,
                        caster.isEnemy,
                        -1,
                        null,
                        -1,
                        0
                    );
                    world.addObject(fx, tgt.getX(), tgt.getY());

                    if (seqSwordQiStacks > 0) {
                        world.addObject(new FatedSever(tgt), tgt.getX(), tgt.getY());
                    }
                }

                sequenceStep = 2;

            } else if (sequenceStep == 2) {
                if (!world.getObjects(Animation.class).isEmpty()) return;

                Hero tgt = seqTarget;
                if (tgt != null && tgt.getWorld() != null) {
                    tgt.playHurtAnimation();
                }

                world.removeObjects(world.getObjects(DimEffect.class));
                for (Hero h: world.getObjects(Hero.class)) h.show();

                endSkillSequence();
            }
        } else if (activeSkillCard instanceof MoYeSkill1Card) {
            BattleWorld w = (BattleWorld) getWorld();
            if (sequenceStep == 0) {
                activeSkillCard.markAsPlayed();
                safeReflowForPlayerCard(activeSkillCard, w);

                for (Hero h: w.getObjects(Hero.class)) {
                    if (h != caster && !w.getEnemiesOf(caster).contains(h)) h.hide();
                }

                GreenfootImage pose = new GreenfootImage("mo_skill_anim.png");
                pose.scale(494, 502);
                if (caster.isEnemy) pose.mirrorHorizontally();
                caster.setImage(pose);

                sequenceStep = 1;
                sequenceTimer = 0;

            } else if (sequenceStep == 1 && sequenceTimer > 5) {
                if (seqTarget == null || seqTarget.getWorld() == null || seqTarget.getCurrentHp() <= 0) {
                    endSkillSequence();
                    return;
                }

                seqDamage = 220;
                seqSource = "Coupled Blades";
                seqHitApplied = false;

                Animation fx = new Animation("mo_skill1_effect", 4, 9);
                w.addObject(fx, seqTarget.getX(), seqTarget.getY());

                sequenceStep = 2;

            } else if (sequenceStep == 2) {
                if (!seqHitApplied && w.getObjects(Animation.class).isEmpty()) {
                    if (seqTarget != null && seqTarget.getWorld() != null) {
                        seqTarget.dealDamage(seqDamage, seqSource);
                        seqTarget.playHurtAnimation();
                    }
                    seqHitApplied = true;
                }
                if (seqHitApplied) {
                    caster.playAnimation(new String[] {});
                    for (Hero h: w.getObjects(Hero.class)) h.show();
                    endSkillSequence();
                }
            }
        } else if (activeSkillCard instanceof MoYeSkill2Card) {
            BattleWorld w = (BattleWorld) getWorld();
            if (sequenceStep == 0) {
                activeSkillCard.markAsPlayed();
                safeReflowForPlayerCard(activeSkillCard, w);

                for (Hero h: w.getObjects(Hero.class)) {
                    if (h != caster && !w.getEnemiesOf(caster).contains(h)) h.hide();
                }

                GreenfootImage pose = new GreenfootImage("mo_skill_anim.png");
                pose.scale(494, 502);
                if (caster.isEnemy) pose.mirrorHorizontally();
                caster.setImage(pose);

                sequenceStep = 1;
                sequenceTimer = 0;

            } else if (sequenceStep == 1 && sequenceTimer > 5) {
                if (seqTarget == null || seqTarget.getWorld() == null || seqTarget.getCurrentHp() <= 0) {
                    endSkillSequence();
                    return;
                }

                seqDamage = 150;
                seqSource = "Ember Oath";
                seqSwordQiStacks = 1;
                seqHitApplied = false;

                Animation fx = new Animation(
                    "mo_skill2_effect",
                    5,
                    1,
                    9,
                    caster.isEnemy,
                    -1,
                    null,
                    -1,
                    0
                );
                w.addObject(fx, seqTarget.getX(), seqTarget.getY());

                sequenceStep = 2;
            } else if (sequenceStep == 2) {
                if (!seqHitApplied && w.getObjects(Animation.class).isEmpty()) {
                    if (seqTarget != null && seqTarget.getWorld() != null) {
                        seqTarget.dealDamage(seqDamage, seqSource);
                        if (seqSwordQiStacks > 0) {
                            try {
                                seqTarget.applyStatus(new SwordQi(seqTarget, seqSwordQiStacks));
                            } catch (Throwable t) {}
                        }
                        seqTarget.playHurtAnimation();
                    }
                    seqHitApplied = true;
                }
                if (seqHitApplied) {
                    caster.playAnimation(new String[] {});
                    for (Hero h: w.getObjects(Hero.class)) h.show();
                    endSkillSequence();
                }
            }
        } else if (activeSkillCard instanceof MoYeUltimateCard) {
            if (sequenceStep == 0) {
                activeSkillCard.markAsPlayed();
                safeReflowForPlayerCard(activeSkillCard, world);

                world.addObject(new DimEffect(world.getWidth(), world.getHeight()),
                    world.getWidth() / 2, world.getHeight() / 2);

                Hero tgt = seqTarget;
                if (tgt == null || tgt.getWorld() == null || tgt.getCurrentHp() <= 0) {
                    world.removeObjects(world.getObjects(DimEffect.class));
                    endSkillSequence();
                    return;
                }

                for (Hero h: world.getObjects(Hero.class)) {
                    if (h != caster && h != tgt) h.hide();
                    else h.show();
                }

                int baseX = tgt.getX();
                int baseY = tgt.getY();
                int[] offs = MoYe.MO_ULT_INTRO_Y_OFFSETS;

                Runnable strike = () -> {
                    Hero target = seqTarget;
                    if (target != null && target.getWorld() != null) {
                        target.dealDamage(700, "Oathfall");
                        target.applyStatus(new SwordQi(target, 2));
                    }
                };

                MoUltEffect effect = new MoUltEffect(
                    caster.isEnemy,
                    baseX,
                    baseY,
                    offs,
                    MoYe.MO_ULT_INTRO_TICKS_PER_FRAME,
                    strike,
                    -1
                );
                world.addObject(effect, baseX, baseY);
                effect.initialize();

                GreenfootImage pose = new GreenfootImage("mo_skill_anim.png");
                pose.scale(494, 502);
                if (caster.isEnemy) pose.mirrorHorizontally();
                caster.setImage(pose);

                sequenceStep = 1;

            } else if (sequenceStep == 1) {

                if (!world.getObjects(MoUltEffect.class).isEmpty()) return;

                Hero tgt = seqTarget;
                if (tgt != null && tgt.getWorld() != null) {
                    tgt.playHurtAnimation();
                }

                caster.setToRestPose();

                world.removeObjects(world.getObjects(DimEffect.class));
                for (Hero h: world.getObjects(Hero.class)) h.show();

                endSkillSequence();
            }
        } else {
            if (sequenceStep == 0) {
                activeSkillCard.markAsPlayed();
                safeReflowForPlayerCard(activeSkillCard, world);
                activeSkillCard.useSkill(world);
                if (activeSkillCard instanceof BaiQiSkill2Card) {
                    world.updateSkilldockForStance((BaiQi) caster, false);
                    if (caster.isEnemy && caster instanceof BaiQi) {
                        updateAIHandForStance((BaiQi) caster, false);
                    }
                }
                endSkillSequence();
            }
        }
    }

    private boolean containsUltimateFor(Hero h) {
        for (SkillCard c: aiHand) {
            if (c.owner == h &&
                (c instanceof BaiQiUltimateCard || c instanceof BianQueUltimateCard ||
                    c instanceof GanJiangUltimateCard || c instanceof MoYeUltimateCard)) {
                return true;
            }
        }
        return false;
    }

    private SkillCard createUltimateFor(Hero h) {
        if (h instanceof BaiQi) return new BaiQiUltimateCard(h);
        if (h instanceof BianQue) return new BianQueUltimateCard(h);
        if (h instanceof GanJiang) return new GanJiangUltimateCard(h);
        if (h instanceof MoYe) return new MoYeUltimateCard(h);
        return null;
    }

    private ArrayList < SkillCard > getAICardPool() {
        BattleWorld world = (BattleWorld) getWorld();
        ArrayList < SkillCard > pool = new ArrayList < > ();
        if (world == null) return pool;

        ArrayList < Hero > enemies = world.getEnemyTeam();

        Hero bai = null, bian = null, gan = null, mo = null;
        for (Hero h: enemies) {
            if (h == null || !h.isAlive()) continue;
            if (h instanceof BaiQi) bai = h;
            else if (h instanceof BianQue) bian = h;
            else if (h instanceof GanJiang) gan = h;
            else if (h instanceof MoYe) mo = h;
        }

        if (bai != null && bian != null) {
            BaiQi bq = (BaiQi) bai;

            if (bq.isStanceActive()) {

                pool.add(new BaiQiSkill1Card(bai));
                pool.add(new BaiQiSkill2Card(bai));
            } else {

                pool.add(new BaiQiSkill1Card(bai));
                pool.add(new BaiQiSkill1Card(bai));
            }

            pool.add(new BianQueSkill1Card(bian));
            pool.add(new BianQueSkill2Card(bian));
        } else if (gan != null && mo != null) {
            pool.add(new GanJiangSkill1Card(gan));
            pool.add(new GanJiangSkill2Card(gan));
            pool.add(new MoYeSkill1Card(mo));
            pool.add(new MoYeSkill2Card(mo));
        }

        if (pool.isEmpty()) {
            for (Hero h: enemies) {
                if (h == null || !h.isAlive()) continue;
                if (h instanceof BaiQi) {
                    pool.add(new BaiQiSkill1Card(h));
                    pool.add(new BaiQiSkill1Card(h));
                    break;
                } else if (h instanceof BianQue) {
                    pool.add(new BianQueSkill1Card(h));
                    pool.add(new BianQueSkill2Card(h));
                    break;
                } else if (h instanceof GanJiang) {
                    pool.add(new GanJiangSkill1Card(h));
                    pool.add(new GanJiangSkill2Card(h));
                    break;
                } else if (h instanceof MoYe) {
                    pool.add(new MoYeSkill1Card(h));
                    pool.add(new MoYeSkill2Card(h));
                    break;
                }
            }
        }

        return pool;
    }

    private void triggerTurnStartForTeam(ArrayList < Hero > team) {
        BattleWorld world = (BattleWorld) getWorld();
        for (StatusEffect effect: new ArrayList < > (world.getObjects(StatusEffect.class))) {

            if (effect != null && effect.target != null && team.contains(effect.target)) {
                effect.onTurnStart();
            }
        }
    }

    private void endSkillSequence() {
        this.activeSkillCard = null;
        this.sequenceStep = 0;

        seqTarget = null;
        seqHitApplied = false;
        seqDamage = 0;
        seqSource = "";
        seqSwordQiStacks = 0;
        unlockGame();
    }

    public void logEvent(String message) {
        if (debugDisplay != null) {
            debugDisplay.addLogMessage(message);

        }
    }

    public void lockGame() {
        this.isLocked = true;
    }

    public void unlockGame() {
        this.isLocked = false;
    }

    public boolean isGameLocked() {
        return isLocked;
    }

    public boolean canSpendAP(int amount) {
        return actionPoints >= amount;
    }

    public void spendAP(int amount) {
        if (canSpendAP(amount)) {
            actionPoints -= amount;
            updateAPCrystals();
        }
    }

    public void registerAPCrystals(APCrystal c1, APCrystal c2) {
        apCrystals.add(c1);
        apCrystals.add(c2);
    }

    private void updateAPCrystals() {
        if (!apCrystals.isEmpty()) {
            apCrystals.get(0).setFull(actionPoints >= 1);
            apCrystals.get(1).setFull(actionPoints >= 2);
        }
    }

    public void onHeroDeath(Hero dead) {
        if (dead == null || aiHand == null || aiHand.isEmpty()) return;
        ArrayList < SkillCard > rm = new ArrayList < > ();
        for (SkillCard c: aiHand)
            if (c.owner == dead) rm.add(c);
        aiHand.removeAll(rm);
        logEvent("AI purged " + rm.size() + " cards for fallen unit: " + dead.name);
    }

    private void prepareEndOfTurnDeaths() {
        BattleWorld w = (BattleWorld) getWorld();
        deathQueue.clear();

        for (Hero h: new ArrayList < > (w.getObjects(Hero.class))) {
            if (h != null && h.getWorld() != null && h.getCurrentHp() <= 0) {
                deathQueue.add(h);
            }
        }

        if (!deathQueue.isEmpty()) {

            for (Hero h: deathQueue) h.playHurtAnimation();
            deathWait = 18;
        }
    }

    private ArrayList < Hero > livingTeam(ArrayList < Hero > team) {
        ArrayList < Hero > out = new ArrayList < > ();
        if (team == null) return out;
        for (Hero h: team) {
            if (h != null && h.getWorld() != null && h.getCurrentHp() > 0) out.add(h);
        }
        return out;
    }

    private void pruneAIHand() {
        if (aiHand == null) return;
        ArrayList < SkillCard > rm = new ArrayList < > ();
        for (SkillCard c: aiHand) {
            if (c == null || c.owner == null || c.owner.getWorld() == null || c.owner.getCurrentHp() <= 0) {
                rm.add(c);
            }
        }
        if (!rm.isEmpty()) {
            aiHand.removeAll(rm);
            logEvent("AI pruned " + rm.size() + " invalid cards.");
        }
    }

    private Hero findAIHero(Class < ? extends Hero > clazz) {
        BattleWorld w = (BattleWorld) getWorld();
        for (Hero h: livingTeam(w.getEnemyTeam())) {
            if (clazz.isInstance(h)) return h;
        }
        return null;
    }

    private boolean aiHandContainsUltFor(Hero h) {
        for (SkillCard c: aiHand) {
            if (c.owner == h && (c instanceof BaiQiUltimateCard || c instanceof BianQueUltimateCard)) return true;
        }
        return false;
    }

    public int purgeAIHandFor(Hero h) {
        if (aiHand == null || h == null) return 0;
        ArrayList < SkillCard > rm = new ArrayList < > ();
        for (SkillCard c: aiHand)
            if (c.owner == h) rm.add(c);
        aiHand.removeAll(rm);
        return rm.size();
    }

    private ArrayList < Hero > living(ArrayList < Hero > raw) {
        ArrayList < Hero > out = new ArrayList < > ();
        for (Hero h: raw)
            if (h != null && h.getWorld() != null && h.getCurrentHp() > 0) out.add(h);
        return out;
    }

    private Hero pickSingleTarget(Hero caster, boolean preferFirst) {
        BattleWorld w = (BattleWorld) getWorld();
        ArrayList < Hero > candidates = living(w.getEnemiesOf(caster));
        if (candidates.isEmpty()) return null;
        return preferFirst ? candidates.get(0) : candidates.get(Greenfoot.getRandomNumber(candidates.size()));
    }

    private Hero pickRandomAliveTarget(java.util.List < Hero > candidates) {
        java.util.ArrayList < Hero > alive = new java.util.ArrayList < > ();
        for (Hero h: candidates) {
            if (h != null && h.getWorld() != null && hIsAlive(h)) alive.add(h);
        }
        if (alive.isEmpty()) return null;
        return alive.get(Greenfoot.getRandomNumber(alive.size()));
    }

    private boolean hIsAlive(Hero h) {

        return h.getWorld() != null;
    }

    private java.util.List < Hero > enemiesOf(Hero caster) {
        return ((BattleWorld) getWorld()).getEnemiesOf(caster);
    }

    private java.util.List < Hero > alliesOf(Hero caster) {
        return ((BattleWorld) getWorld()).getAlliesOf(caster);
    }

    private boolean isSingleTargetSkill(SkillCard card) {
        return card instanceof GanJiangSkill1Card ||
            card instanceof GanJiangUltimateCard ||
            card instanceof MoYeSkill1Card ||
            card instanceof MoYeSkill2Card ||
            card instanceof MoYeUltimateCard;
    }

    private Hero pickLowestHp(java.util.List < Hero > candidates) {
        Hero best = null;
        for (Hero h: candidates) {
            if (h == null || h.getWorld() == null || h.getCurrentHp() <= 0) continue;
            if (best == null || h.getCurrentHp() < best.getCurrentHp()) best = h;
        }
        return best;
    }

    public void requestPlayerTargetSelection(java.util.List < Hero > candidates) {
        BattleWorld w = (BattleWorld) getWorld();
        if (w == null || candidates == null || candidates.isEmpty()) return;

        awaitingTargetSelection = true;
        seqTarget = null;

        Hero left = candidates.get(0);
        Hero right = (candidates.size() > 1) ? candidates.get(1) : null;

        TargetSelector ui = new TargetSelector(left, right, this);
        w.addObject(ui, 200, 525);
    }

    public void onPlayerTargetChosen(Hero target) {
        if (!awaitingTargetSelection) return;
        this.seqTarget = target;
        awaitingTargetSelection = false;
    }

    private void applyDebugBuffF5() {
        BattleWorld w = (BattleWorld) getWorld();
        if (w == null) return;

        for (Hero h: new java.util.ArrayList < > (w.getObjects(Hero.class))) {
            if (h == null || h.getWorld() == null) continue;

            h.applyHeal(500, "Debug F5");

            h.setUltimateToMax();

            try {
                h.applyStatus(new SwordQi(h, 1));
            } catch (Throwable t) {

            }
        }

        logEvent("[DEBUG] F5: +500 HP, full ult, +1 Sword Qi applied to all heroes.");
    }
}