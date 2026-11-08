
package io.jbnu.test;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

public class World {
    public static final float WORLD_HEIGHT = 600f;
    public static final int ESCAPE_HITS = CaptureTiming.REQUIRED_HITS;
    public static final int MAX_MISSES = CaptureTiming.MAX_MISSES;
    public static final float STINGER_COOLDOWN = 0.5f;
    public static final float SWARM_COOLDOWN = 7f;

    private static final float MOVE_STEP = 8f;
    private static final float FALL_Y = -80f;
    private static final float INVINCIBLE_TIME = 1f;
    private static final float ESCAPE_IMMUNITY = 1.5f;
    private static final float SWARM_DURATION = 2.5f;
    private static final float SWARM_INTERVAL = 0.5f;
    private static final float SWARM_RANGE = 220f;
    private static final int DASH_DAMAGE = 10;
    private static final int ALLY_DAMAGE_PER_HIT = 4;

    private static final float STAGE4_SPAWN_X = 2110f;
    private static final float STAGE4_TRIGGER_X = 2010f;
    private static final float STAGE4_PUSHBACK_X = 1880f;
    private static final float STAGE4_AMBUSH_DURATION = 1.7f;

    private static final float[] ARENA_START = {
        0f, 0f, 2400f, 2200f, 1800f
    };

    public final Array<Rectangle> blocks = new Array<>();
    public final Array<Rectangle> honeys = new Array<>();
    public final Array<Rectangle> webs = new Array<>();
    public final Array<Rectangle> spikes = new Array<>();
    public final Array<Projectile> projectiles = new Array<>();
    public final Array<SpiderTrap> spiders = new Array<>();
    public final Array<GreenFrog> frogs = new Array<>();

    public final Player player = new Player();
    public final Mantis mantis = new Mantis();
    public final Hornet hornet = new Hornet();
    public final GiantHornet giantHornet = new GiantHornet();
    public final Rectangle exit = new Rectangle();

    public int level = 1;
    public float worldWidth;

    private int totalHoney;
    private int requiredHoney;
    private int collectedHoney;
    private int playerHealthHalf = 10;
    private int spiderDeathCount;

    private boolean arenaEntered;
    private boolean arenaCleared;
    private boolean stingUnlocked;
    private boolean swarmUnlocked;
    private boolean gameCleared;
    private boolean gameOver;
    private boolean stageFourAmbushTriggered;

    private float invincibleTimer;
    private float stingerCooldown;
    private float swarmCooldown;
    private float swarmTimer;
    private float swarmDamageTimer;
    private float captureImmunity;
    private float stageFourAmbushRemaining;
    private float messageTimer;

    private String message = "";
    private SpiderTrap captor;
    private boolean mantisCaptured;
    private boolean giantCaptured;
    private boolean tutorialSpiderEscaped;

    private final CaptureTiming captureTiming = new CaptureTiming();
    private final Rectangle dashHitbox = new Rectangle();

    public World() {
        loadLevel(1);
    }

    private void block(float x, float y, float w, float h) {
        blocks.add(new Rectangle(x, y, w, h));
    }

    private void honey(float x, float y) {
        honeys.add(new Rectangle(x - 12f, y - 12f, 24f, 24f));
    }

    private void spider(float x, float top, float bottom, float speed) {
        spiders.add(new SpiderTrap(x, top, bottom, speed));
    }

    private void frog(float x, float y, float minX, float maxX) {
        frogs.add(new GreenFrog(x, y, minX, maxX));
    }

    private void spike(float x, float y, float w, float h) {
        spikes.add(new Rectangle(x, y, w, h));
    }

    public void loadLevel(int next) {
        if (next < 1 || next > 4) return;

        level = next;
        blocks.clear();
        honeys.clear();
        webs.clear();
        spikes.clear();
        projectiles.clear();
        spiders.clear();
        frogs.clear();

        collectedHoney = 0;
        playerHealthHalf = 10;
        invincibleTimer = 0f;
        stingerCooldown = 0f;
        swarmCooldown = 0f;
        swarmTimer = 0f;
        swarmDamageTimer = 0f;
        captureImmunity = 0f;

        arenaEntered = false;
        arenaCleared = false;
        gameCleared = false;
        gameOver = false;
        stageFourAmbushTriggered = false;
        stageFourAmbushRemaining = 0f;
        tutorialSpiderEscaped = false;

        clearCapture();
        player.reset(100f, 100f);
        mantis.reset();
        hornet.reset();
        giantHornet.reset();

        switch (level) {
            case 1:
                createOne();
                showMessage("FIRST DAY AT WORK");
                break;
            case 2:
                createTwo();
                showMessage("FLOWER FIELD AMBUSH");
                break;
            case 3:
                createThree();
                showMessage("HIVE FRONTIER");
                break;
            case 4:
                createFour();
                player.reset(STAGE4_SPAWN_X, 100f);
                swarmUnlocked = true;
                showMessage("FINAL DEFENSE!");
                break;
            default:
                break;
        }

        if (honeys.size != requiredHoney) {
            throw new IllegalStateException(
                "Stage " + level + ": honey count "
                    + honeys.size + " != required " + requiredHoney
            );
        }

        clearArenaHazards();
        validateLevelLayout();
    }

    // Check authored maps for buried honey and unsafe frog corridors.
    private void validateLevelLayout() {
        if (honeys.size != requiredHoney) {
            throw new IllegalStateException(
                "Stage " + level + " honey count mismatch"
            );
        }

        for (Rectangle drop : honeys) {
            for (Rectangle wall : blocks) {
                if (drop.overlaps(wall)) {
                    throw new IllegalStateException(
                        "Stage " + level
                            + " honey buried in wall at x=" + drop.x
                    );
                }
            }

            for (Rectangle thorn : spikes) {
                if (drop.overlaps(thorn)) {
                    throw new IllegalStateException(
                        "Stage " + level
                            + " honey overlaps thorn at x=" + drop.x
                    );
                }
            }
        }

        for (GreenFrog enemy : frogs) {
            Rectangle path = new Rectangle(
                enemy.getPatrolMinX(), enemy.bounds.y,
                enemy.getPatrolMaxX() - enemy.getPatrolMinX()
                    + enemy.bounds.width,
                enemy.bounds.height
            );

            for (Rectangle thorn : spikes) {
                if (path.overlaps(thorn)) {
                    throw new IllegalStateException(
                        "Stage " + level
                            + " frog patrol overlaps thorn at x=" + thorn.x
                    );
                }
            }
        }
    }

    private void createOne() {
        worldWidth = 2400f;
        requiredHoney = 6;
        totalHoney = requiredHoney;

        block(0, 0, worldWidth, 40);
        block(400, 100, 220, 24);
        block(875, 190, 130, 22);
        block(1050, 145, 220, 24);
        block(1470, 200, 210, 24);
        block(1820, 160, 190, 24);
        block(2100, 190, 125, 22);

        spider(780, 255, 45, 78);
        spider(1290, 360, 135, 88);
        spider(2010, 380, 100, 95);

        spike(560, 40, 54, 17);
        spike(1095, 169, 57, 17);
        spike(1640, 224, 31, 16);
        spike(950, 40, 44, 16);
        spike(1745, 40, 47, 16);

        honey(300, 95);
        honey(500, 155);
        honey(1080, 200);
        honey(1250, 110);
        honey(1510, 255);
        honey(2150, 98);

        exit.set(2256, 75, 48, 50);
    }

    private void createTwo() {
        worldWidth = 3200f;
        requiredHoney = 10;
        totalHoney = requiredHoney;

        block(0, 0, worldWidth, 40);
        block(400, 120, 240, 24);
        block(1050, 180, 240, 24);
        block(1750, 120, 220, 24);
        block(700, 240, 300, 360);
        block(1450, 260, 250, 340);
        block(2270, 170, 150, 22);

        spider(500, 420, 160, 75);
        spider(1120, 420, 130, 108);
        spider(1840, 460, 190, 105);
        spider(2070, 415, 105, 120);

        // Safe ground corridors for patrolling frogs.
        frog(1225, 40, 1108, 1298);
        frog(1830, 40, 1755, 1910);

        spike(570, 144, 60, 18);
        spike(1195, 204, 60, 18);
        spike(1030, 40, 65, 18);
        spike(1350, 40, 75, 18);
        spike(1975, 40, 80, 18);
        spike(2140, 40, 70, 18);
        spike(905, 40, 49, 17);
        spike(1680, 40, 56, 17);

        honey(300, 90);
        honey(600, 200);
        honey(680, 110);
        honey(850, 130);
        honey(1050, 235);
        honey(1230, 235);
        honey(1450, 130);
        honey(1590, 140);
        honey(1780, 185);
        honey(2170, 110);

        exit.set(3066, 75, 48, 50);
    }

    private void createThree() {
        worldWidth = 3000f;
        requiredHoney = 15;
        totalHoney = requiredHoney;

        block(0, 0, worldWidth, 40);
        block(350, 160, 220, 24);
        block(750, 300, 220, 24);
        block(1150, 190, 220, 24);
        block(1550, 330, 220, 24);
        block(1900, 140, 180, 24);
        block(580, 240, 120, 360);
        block(1400, 280, 140, 320);
        block(2140, 300, 135, 22);
        block(2310, 160, 140, 24);
        block(2750, 160, 140, 24);

        spider(450, 440, 195, 95);
        spider(1010, 465, 140, 125);
        spider(1200, 460, 255, 120);
        spider(1765, 475, 195, 130);
        spider(1940, 445, 175, 110);

        frog(970, 40, 755, 1050);
        frog(1790, 40, 1635, 1920);

        spike(520, 40, 58, 18);
        spike(1120, 40, 65, 18);
        spike(1305, 214, 56, 18);
        spike(1540, 40, 70, 18);
        spike(2010, 40, 66, 18);
        spike(665, 40, 42, 16);

        honey(220, 98);
        honey(370, 235);
        honey(545, 216);
        honey(740, 100);
        honey(850, 355);
        honey(990, 120);
        honey(1090, 280);
        honey(1250, 245);
        honey(1320, 260);
        honey(1600, 120);
        honey(1650, 385);
        honey(1775, 140);
        honey(1880, 210);
        honey(2010, 260);
        honey(2100, 100);

        exit.set(2866, 75, 48, 50);
    }

    private void createFour() {
        worldWidth = 2600f;
        requiredHoney = 4;
        totalHoney = requiredHoney;

        block(0, 0, worldWidth, 40);
        block(1900, 150, 140, 24);
        block(2360, 150, 140, 24);

        // The honey quest ends when the boss ambush begins.
        honey(2045, 103);
        honey(2085, 148);
        honey(2140, 124);
        honey(2220, 188);

        exit.set(2500, 75, 48, 50);
    }

    private void clearArenaHazards() {
        if (level < 2 || level > 4) return;
        float start = ARENA_START[level];

        for (int i = spiders.size - 1; i >= 0; i--) {
            if (spiders.get(i).getAnchorX() >= start - 20f) {
                spiders.removeIndex(i);
            }
        }

        for (int i = frogs.size - 1; i >= 0; i--) {
            GreenFrog frog = frogs.get(i);
            if (frog.bounds.x + frog.bounds.width >= start) {
                frogs.removeIndex(i);
            }
        }

        for (int i = spikes.size - 1; i >= 0; i--) {
            Rectangle thorn = spikes.get(i);
            if (thorn.x + thorn.width > start) {
                spikes.removeIndex(i);
            }
        }
    }

    private void showMessage(String value) {
        message = value;
        messageTimer = 2f;
    }

    public String getMessage() {
        return messageTimer > 0f ? message : "";
    }

    public int getHoneyCollected() { return collectedHoney; }
    public int getTotalHoney() { return totalHoney; }
    public int getRequiredHoney() { return requiredHoney; }
    public int getPlayerHp() { return (playerHealthHalf + 1) / 2; }
    public int getPlayerHalfHearts() { return playerHealthHalf; }
    public boolean isTutorialSpiderEscaped() { return tutorialSpiderEscaped; }
    public boolean isMantisCaptured() { return mantisCaptured; }
    public boolean isGiantHornetCaptured() { return giantCaptured; }
    public int getSpiderDeathCount() { return spiderDeathCount; }
    public float getStingerCooldown() { return stingerCooldown; }
    public float getSwarmCooldown() { return swarmCooldown; }
    public float getSwarmTimer() { return swarmTimer; }

    public float getSwarmGauge() {
        return 100f * (1f - swarmCooldown / SWARM_COOLDOWN);
    }

    public boolean isStingUnlocked() { return stingUnlocked; }
    public boolean isSwarmUnlocked() { return swarmUnlocked; }
    public boolean isSwarmActive() { return swarmTimer > 0f; }
    public boolean isPlayerInvincible() { return invincibleTimer > 0f; }
    public boolean isArenaEntered() { return arenaEntered; }

    public boolean isMantisIntroLocked() {
        return level == 2 && arenaEntered && !arenaCleared
            && !gameOver && mantis.isIntroSequenceActive();
    }

    public boolean isArenaCleared() { return arenaCleared; }
    public boolean isGameCleared() { return gameCleared; }
    public boolean isGameOver() { return gameOver; }

    public boolean isCaptured() {
        return captor != null || mantisCaptured || giantCaptured;
    }

    public boolean isStuck() { return isCaptured(); }

    public boolean hasStageFourAmbushOccurred() {
        return stageFourAmbushTriggered;
    }

    public boolean isStageFourAmbushActive() {
        return stageFourAmbushRemaining > 0f;
    }

    public float getStageFourAmbushRemaining() {
        return stageFourAmbushRemaining;
    }

    public int getEscapeHits() { return captureTiming.getHits(); }
    public int getEscapeMisses() { return captureTiming.getMisses(); }
    public float getGaugePosition() { return captureTiming.getPosition(); }
    public float getEscapeZoneStart() { return captureTiming.getZoneStart(); }
    public float getEscapeZoneEnd() { return captureTiming.getZoneEnd(); }

    public float getEscapeFeedbackTime() {
        return captureTiming.getFeedbackTime();
    }

    public CaptureTiming.Feedback getEscapeFeedback() {
        return captureTiming.getFeedback();
    }

    public float getCaptureTimeRemaining() {
        return isCaptured() ? captureTiming.getRemaining() : 0f;
    }

    public boolean isArenaGateOpen() {
        return (level != 2 && level != 3)
            || collectedHoney >= requiredHoney;
    }

    public boolean isExitUnlocked() {
        return level == 1 ? collectedHoney >= requiredHoney : arenaCleared;
    }

    public Enemy getActiveBoss() {
        if (!arenaEntered || arenaCleared) return null;

        switch (level) {
            case 2: return mantis;
            case 3: return hornet;
            case 4: return giantHornet;
            default: return null;
        }
    }

    private void clearCapture() {
        captor = null;
        mantisCaptured = false;
        giantCaptured = false;
        captureTiming.clear();
    }

    private void beginCapture(SpiderTrap trap) {
        captor = trap;
        captureTiming.start(level, 0);
        player.stopDash();
        player.velocityX = 0f;
        player.velocityY = 0f;
    }

    private void beginMantisCapture() {
        mantisCaptured = true;
        captureTiming.start(2, 2);
        player.stopDash();
        player.setMovement(false, false);
        player.velocityX = 0f;
        player.velocityY = 0f;
        mantis.onCapture();

        player.bounds.setPosition(
            mantis.centerX() - player.bounds.width / 2f,
            mantis.bounds.y + mantis.bounds.height + 5f
        );
    }

    private void beginGiantCapture() {
        giantCaptured = true;
        captureTiming.start(4, 2);
        giantHornet.onCapture();

        player.stopDash();
        player.setMovement(false, false);
        player.velocityX = 0f;
        player.velocityY = 0f;

        player.bounds.setPosition(
            giantHornet.centerX() - player.bounds.width / 2f,
            giantHornet.centerY() - player.bounds.height / 2f
        );
    }

    private void finishCapture(boolean escaped) {
        if (giantCaptured) {
            int misses = captureTiming.getMisses();
            clearCapture();

            captureImmunity = ESCAPE_IMMUNITY;
            invincibleTimer = Math.max(
                invincibleTimer, ESCAPE_IMMUNITY
            );

            player.bounds.x = MathUtils.clamp(
                giantHornet.centerX() - 125f,
                ARENA_START[4],
                worldWidth - player.bounds.width
            );

            player.bounds.y = MathUtils.clamp(
                giantHornet.centerY() - 50f,
                40f,
                WORLD_HEIGHT - player.bounds.height
            );

            player.velocityY = 0f;

            if (!escaped && misses < CaptureTiming.MAX_MISSES) {
                damagePlayerHalf(4, true);
            }
            return;
        }

        if (mantisCaptured) {
            clearCapture();
            captureImmunity = ESCAPE_IMMUNITY;

            if (!escaped) {
                damagePlayerHalf(5, true);
            }
            return;
        }

        SpiderTrap trap = captor;
        if (trap == null) return;

        if (!escaped) {
            spiderDeathCount++;
            killPlayer();
            return;
        }

        clearCapture();
        captureImmunity = ESCAPE_IMMUNITY;

        if (level == 1) {
            tutorialSpiderEscaped = true;
        }

        trap.stun();
    }

    private void updateCapture(float dt) {
        if (!isCaptured()) return;

        CaptureTiming.Result result = captureTiming.update(dt);

        if (result == CaptureTiming.Result.CAUGHT) {
            finishCapture(false);
        } else if (result == CaptureTiming.Result.ESCAPED) {
            finishCapture(true);
        }
    }

    private void killPlayer() {
        playerHealthHalf = 0;
        gameOver = true;
        clearCapture();
        player.stopDash();
        player.setMovement(false, false);
        player.velocityY = 0f;
        swarmTimer = 0f;
        projectiles.clear();
    }

    public void restartCurrentStage() {
        int retryLevel = level;
        boolean retryArena =
            arenaEntered && !arenaCleared && retryLevel >= 2;

        loadLevel(retryLevel);

        if (!retryArena) return;

        // Restart directly inside the boss arena.
        arenaEntered = true;
        arenaCleared = false;
        honeys.clear();
        collectedHoney = requiredHoney;
        player.reset(ARENA_START[retryLevel] + 70f, 100f);
        invincibleTimer = 1.25f;
        captureImmunity = 1.25f;
        messageTimer = 0f;

        if (retryLevel == 4) {
            stageFourAmbushTriggered = true;
            stageFourAmbushRemaining = 0f;
            requiredHoney = 0;
            collectedHoney = 0;
            player.reset(STAGE4_PUSHBACK_X + 35f, 100f);
        }
    }

    public void restart() {
        restartCurrentStage();
    }

    public void restartEntireGame() {
        player.dashUnlocked = false;
        stingUnlocked = false;
        swarmUnlocked = false;
        spiderDeathCount = 0;
        loadLevel(1);
    }

    public void pressFlap() {
        if (gameOver || isStageFourAmbushActive()
            || isMantisIntroLocked()) {
            return;
        }

        if (!isCaptured()) {
            player.flap();
            return;
        }

        int oldMisses = captureTiming.getMisses();
        CaptureTiming.Result result = captureTiming.press();

        if (giantCaptured
            && captureTiming.getMisses() > oldMisses) {
            damagePlayerHalf(4, true);
            if (gameOver) return;
        }

        if (result == CaptureTiming.Result.ESCAPED) {
            finishCapture(true);
        } else if (result == CaptureTiming.Result.CAUGHT) {
            finishCapture(false);
        }
    }

    public void pressDash() {
        if (!gameOver && !isCaptured()
            && !isStageFourAmbushActive()
            && !isMantisIntroLocked()) {
            player.startDash();
        }
    }

    public void pressStinger() {
        if (gameOver || isCaptured()
            || isStageFourAmbushActive()
            || isMantisIntroLocked()
            || !stingUnlocked
            || stingerCooldown > 0f
            || player.isDashing()
            || projectiles.size >= Projectile.MAX_COUNT) {
            return;
        }

        float x = player.facingRight
            ? player.bounds.x + player.bounds.width
            : player.bounds.x - Projectile.WIDTH;

        float y = player.bounds.y + player.bounds.height / 2f
            - Projectile.HEIGHT / 2f;

        projectiles.add(
            new Projectile(x, y, player.facingRight)
        );

        stingerCooldown = STINGER_COOLDOWN;
    }

    public void pressSwarm() {
        if (gameOver || isCaptured()
            || isStageFourAmbushActive()
            || isMantisIntroLocked()
            || !swarmUnlocked
            || swarmCooldown > 0f
            || isSwarmActive()
            || player.isDashing()) {
            return;
        }

        Enemy boss = getActiveBoss();
        if (boss == null || boss.isDead()) return;

        float px = player.bounds.x + player.bounds.width / 2f;
        float py = player.bounds.y + player.bounds.height / 2f;
        float dx = boss.centerX() - px;
        float dy = boss.centerY() - py;

        if (dx * dx + dy * dy > SWARM_RANGE * SWARM_RANGE) {
            showMessage("적에게 가까이 다가가야 합니다!");
            return;
        }

        swarmCooldown = SWARM_COOLDOWN;
        swarmTimer = SWARM_DURATION;
        swarmDamageTimer = 0f;

        if (boss instanceof GiantHornet) {
            giantHornet.interruptCombo();
        } else {
            boss.forceRecover(0f);
        }
    }

    private void beginStageFourAmbush() {
        if (level != 4 || stageFourAmbushTriggered) return;

        stageFourAmbushTriggered = true;
        stageFourAmbushRemaining = STAGE4_AMBUSH_DURATION;
        arenaEntered = true;

        // End the honey quest immediately.
        honeys.clear();
        requiredHoney = 0;
        clearArenaHazards();

        player.stopDash();
        player.setMovement(false, false);
        player.bounds.setPosition(STAGE4_PUSHBACK_X, 100f);
        player.velocityX = 0f;
        player.velocityY = 0f;

        captureImmunity = ESCAPE_IMMUNITY;
        invincibleTimer = STAGE4_AMBUSH_DURATION + 0.35f;
        messageTimer = 0f;
    }

    private void updateStageFourAmbush(float dt) {
        if (stageFourAmbushRemaining <= 0f) return;

        stageFourAmbushRemaining = Math.max(
            0f, stageFourAmbushRemaining - dt
        );

        player.setMovement(false, false);
        player.velocityX = 0f;
        player.velocityY = 0f;
    }

    public void update(float dt) {
        if (gameCleared || gameOver) return;

        invincibleTimer = Math.max(0f, invincibleTimer - dt);
        stingerCooldown = Math.max(0f, stingerCooldown - dt);
        swarmCooldown = Math.max(0f, swarmCooldown - dt);
        captureImmunity = Math.max(0f, captureImmunity - dt);
        messageTimer = Math.max(0f, messageTimer - dt);

        if (isStageFourAmbushActive()) {
            updateStageFourAmbush(dt);
            return;
        }

        if (isCaptured()) {
            updateCapture(dt);
            return;
        }

        if (isMantisIntroLocked()) {
            player.stopDash();
            player.setMovement(false, false);
            player.velocityX = 0f;
            player.velocityY = 0f;
            mantis.update(dt, player);
            return;
        }

        for (SpiderTrap trap : spiders) {
            trap.update(dt);
        }

        for (GreenFrog enemy : frogs) {
            enemy.update(dt, player, blocks);
        }

        if (arenaEntered) {
            clearArenaHazards();
        }

        player.update(dt);
        moveX(player.velocityX * dt);

        if (!player.isDashing()) {
            moveY(player.velocityY * dt);
        }

        if (level == 1
            && !tutorialSpiderEscaped
            && !isCaptured()
            && captureImmunity <= 0f
            && player.bounds.x >= 740f
            && spiders.size > 0) {

            SpiderTrap tutorialSpider = spiders.get(0);

            tutorialSpider.bounds.setPosition(
                player.bounds.x + player.bounds.width / 2f
                    - tutorialSpider.bounds.width / 2f,
                player.bounds.y + player.bounds.height / 2f
                    - tutorialSpider.bounds.height / 2f
            );

            beginCapture(tutorialSpider);
        }

        if (!isCaptured()) {
            checkSpiderCollision();
        }
        if (isCaptured() || gameOver) return;

        checkBoundary();
        if (gameOver) return;

        if (player.bounds.y + player.bounds.height < FALL_Y) {
            killPlayer();
            return;
        }

        for (GreenFrog enemy : frogs) {
            if (enemy.tryHit(player.bounds)) {
                damagePlayerHalf(2, false);
                if (gameOver) return;
            }
        }

        collectHoney();

        if (level == 1) {
            if (isExitUnlocked() && player.bounds.overlaps(exit)) {
                player.dashUnlocked = true;
                loadLevel(2);
            }
            return;
        }

        if (level == 4
            && !stageFourAmbushTriggered
            && player.bounds.x <= STAGE4_TRIGGER_X) {
            beginStageFourAmbush();
            return;
        }

        if (level != 4 && !arenaEntered
            && isArenaGateOpen()
            && player.bounds.x + player.bounds.width
            >= ARENA_START[level]) {

            clearArenaHazards();
            arenaEntered = true;

            if (level == 2) {
                messageTimer = 0f;
            } else {
                showMessage("BOSS INCOMING!");
            }
        }

        Enemy boss = getActiveBoss();

        if (boss != null) {
            if (isSwarmActive()) {
                updateSwarm(dt, boss);
            } else {
                boss.update(dt, player);
            }
        }

        updateProjectiles(dt);

        if (!isCaptured() && !gameOver) {
            resolveBossContact();
        }

        if (boss instanceof Mantis
            && !isSwarmActive()
            && !isCaptured() && !gameOver) {
            resolveMantisAttacks();
        }

        if (boss instanceof Hornet
            && !isSwarmActive()
            && !isCaptured() && !gameOver) {
            resolveHornetAttacks();
        }

        if (boss instanceof GiantHornet
            && !isSwarmActive()
            && !isCaptured() && !gameOver) {
            resolveGiantAttacks();
        }

        if (gameOver || isCaptured()) return;

        checkBossDeath();

        if (arenaCleared && player.bounds.overlaps(exit)) {
            if (level == 2) {
                stingUnlocked = true;
                loadLevel(3);
            } else if (level == 3) {
                swarmUnlocked = true;
                loadLevel(4);
            }
        }
    }

    private void checkSpiderCollision() {
        if (captureImmunity > 0f) return;

        for (SpiderTrap trap : spiders) {
            if (!trap.canCapture()
                || !player.bounds.overlaps(trap.bounds)) {
                continue;
            }

            if (player.isDashing()) {
                trap.stun();
                player.stopDash();
                return;
            }

            beginCapture(trap);
            return;
        }
    }

    private void updateSwarm(float dt, Enemy boss) {
        float oldTimer = swarmTimer;
        swarmTimer = Math.max(0f, swarmTimer - dt);
        swarmDamageTimer += dt;

        while (swarmDamageTimer + 0.0001f >= SWARM_INTERVAL
            && !boss.isDead()) {

            swarmDamageTimer -= SWARM_INTERVAL;
            boss.takeSwarmDamage(ALLY_DAMAGE_PER_HIT);
        }

        if (oldTimer > 0f
            && swarmTimer <= 0f
            && !boss.isDead()) {

            if (boss instanceof GiantHornet) {
                ((GiantHornet) boss).interruptCombo();
            } else {
                boss.forceRecover(0f);
            }
        }
    }

    private void updateProjectiles(float dt) {
        for (int i = projectiles.size - 1; i >= 0; i--) {
            Projectile p = projectiles.get(i);
            float distance = p.direction * Projectile.SPEED * dt;

            int steps = Math.max(
                1, (int)Math.ceil(Math.abs(distance) / MOVE_STEP)
            );
            float step = distance / steps;

            for (int j = 0; j < steps && !p.isExpired(); j++) {
                p.bounds.x += step;

                for (Rectangle b : blocks) {
                    if (p.bounds.overlaps(b)) {
                        p.destroy();
                        break;
                    }
                }

                Enemy boss = getActiveBoss();

                if (!p.isExpired() && boss != null
                    && p.bounds.overlaps(boss.bounds)) {

                    if (isSwarmActive()) {
                        boss.takeSwarmDamage(Projectile.DAMAGE);
                    } else {
                        boss.takeDamage(Projectile.DAMAGE);
                    }
                    p.destroy();
                }

                if (p.bounds.x < 0f || p.bounds.x > worldWidth) {
                    p.destroy();
                }
            }

            if (!p.isExpired()) {
                float currentX = p.bounds.x;
                p.update(dt);
                p.bounds.x = currentX;
            }

            if (p.isExpired()) {
                projectiles.removeIndex(i);
            }
        }
    }

    private void resolveMantisAttacks() {
        if (mantis.isDead() || captureImmunity > 0f) return;
        if (player.isDashing()) return;

        if (mantis.isClawDangerous()
            && player.bounds.overlaps(mantis.getClawBounds())) {
            mantis.markAttackHit();
            damagePlayerHalf(3, false);
        }

        if (mantis.isSlashDangerous()
            && player.bounds.overlaps(mantis.getSlashBounds())) {
            mantis.markAttackHit();
            damagePlayerHalf(3, false);
        }

        if (mantis.isSecondSlashDangerous()
            && player.bounds.overlaps(mantis.getSecondSlashBounds())) {
            mantis.markSecondSlashHit();
            damagePlayerHalf(3, false);
        }

        if (mantis.isLeapDangerous()
            && player.bounds.overlaps(mantis.bounds)
            && invincibleTimer <= 0f) {
            mantis.markAttackHit();
            beginMantisCapture();
        }
    }

    private void resolveHornetAttacks() {
        if (invincibleTimer > 0f
            || captureImmunity > 0f
            || player.isDashing()) {
            return;
        }

        if (hornet.isAttackDangerous()
            && player.bounds.overlaps(hornet.getAttackBounds())) {
            hornet.markAttackHit();
            damagePlayerHalf(
                hornet.getAttackDamageHalfHearts(), false
            );
        }
    }

    private void resolveGiantAttacks() {
        if (captureImmunity > 0f || player.isDashing()) return;

        if (giantHornet.isGrabDangerous()
            && player.bounds.overlaps(giantHornet.getGrabBounds())
            && invincibleTimer <= 0f) {

            giantHornet.markHit();
            beginGiantCapture();
            return;
        }

        if (giantHornet.isChargeDangerous()
            && player.bounds.overlaps(giantHornet.bounds)
            && invincibleTimer <= 0f) {

            giantHornet.markHit();
            damagePlayerHalf(8, false);
        }

        for (int i = giantHornet.getStings().size - 1;
             i >= 0; i--) {

            GiantHornet.Sting sting =
                giantHornet.getStings().get(i);

            if (player.bounds.overlaps(sting.bounds)
                && invincibleTimer <= 0f) {

                giantHornet.getStings().removeIndex(i);
                damagePlayerHalf(6, false);
                break;
            }
        }
    }

    private void resolveBossContact() {
        Enemy boss = getActiveBoss();

        if (boss == null || boss.isDead()
            || !player.bounds.overlaps(boss.bounds)) {
            return;
        }

        if (player.isDashing()) {
            if (isSwarmActive()) {
                boss.takeSwarmDamage(DASH_DAMAGE);
            } else {
                boss.takeDamage(DASH_DAMAGE);
            }

            if (boss instanceof GiantHornet) {
                ((GiantHornet)boss).interruptCombo();
            } else if (boss instanceof Hornet) {
                boss.forceRecover(0f);
            }

            player.stopDash();
            moveX(player.facingRight ? -40f : 40f);
            return;
        }

        if (boss instanceof Mantis
            || boss instanceof Hornet
            || boss instanceof GiantHornet) {
            return;
        }

        if (!isSwarmActive()
            && boss.getState() == Enemy.State.ATTACK) {

            int damage = boss instanceof GiantHornet ? 8
                : boss instanceof Hornet ? 4 : 2;
            damagePlayerHalf(damage, false);
        }
    }

    private void checkBossDeath() {
        Enemy boss = getActiveBoss();
        if (boss == null || !boss.isDead()) return;

        arenaCleared = true;
        swarmTimer = 0f;
        projectiles.clear();

        if (level == 4) {
            gameCleared = true;
            messageTimer = 0f;
        } else {
            showMessage("BOSS DEFEATED - EXIT OPEN!");
        }
    }

    private void damagePlayer() {
        damagePlayerHalf(2, false);
    }

    private void damagePlayerHalf(
        int halfHearts, boolean ignoreInvincibility
    ) {
        if (gameOver || halfHearts <= 0) return;

        if (!ignoreInvincibility
            && (invincibleTimer > 0f || player.isDashing())) {
            return;
        }

        playerHealthHalf = Math.max(
            0, playerHealthHalf - halfHearts
        );
        invincibleTimer = INVINCIBLE_TIME;

        if (playerHealthHalf == 0) {
            killPlayer();
        }
    }

    private void collectHoney() {
        for (int i = honeys.size - 1; i >= 0; i--) {
            if (player.bounds.overlaps(honeys.get(i))) {
                honeys.removeIndex(i);
                collectedHoney++;

                if (collectedHoney % 3 == 0) {
                    playerHealthHalf = Math.min(
                        10, playerHealthHalf + 2
                    );
                }
            }
        }
    }

    private void checkBoundary() {
        if (player.bounds.y + player.bounds.height
            > WORLD_HEIGHT) {

            player.bounds.y =
                WORLD_HEIGHT - player.bounds.height;

            if (player.velocityY > 0f) {
                player.velocityY = 0f;
            }
        }

        for (Rectangle vine : spikes) {
            if (player.bounds.overlaps(vine)) {
                damagePlayer();
                break;
            }
        }
    }

    private void moveX(float distance) {
        int steps = Math.max(
            1, (int)Math.ceil(Math.abs(distance) / MOVE_STEP)
        );
        float step = distance / steps;

        for (int i = 0; i < steps; i++) {
            player.bounds.x += step;

            float minX = 0f;
            float maxX = worldWidth - player.bounds.width;

            if (level > 1) {
                float start = ARENA_START[level];

                if (arenaEntered && !arenaCleared) {
                    minX = start;
                } else if (!isArenaGateOpen()) {
                    maxX = Math.min(
                        maxX, start - player.bounds.width
                    );
                }
            }

            float attemptedX = player.bounds.x;
            player.bounds.x = MathUtils.clamp(
                player.bounds.x, minX, maxX
            );

            if (attemptedX != player.bounds.x) {
                if (player.isDashing()) player.stopDash();
                return;
            }

            for (Rectangle b : blocks) {
                if (player.bounds.overlaps(b)) {
                    player.bounds.x = step > 0f
                        ? b.x - player.bounds.width
                        : b.x + b.width;

                    if (player.isDashing()) player.stopDash();
                    return;
                }
            }

            if (player.isDashing()) {
                Enemy boss = getActiveBoss();

                dashHitbox.set(
                    player.bounds.x - 16f,
                    player.bounds.y - 22f,
                    player.bounds.width + 32f,
                    player.bounds.height + 44f
                );

                if (boss != null
                    && boss.bounds.overlaps(dashHitbox)) {

                    if (isSwarmActive()) {
                        boss.takeSwarmDamage(DASH_DAMAGE);
                    } else {
                        boss.takeDamage(DASH_DAMAGE);
                    }

                    if (boss instanceof GiantHornet) {
                        ((GiantHornet)boss).interruptCombo();
                    } else if (boss instanceof Hornet) {
                        boss.forceRecover(0f);
                    }

                    player.stopDash();
                    moveX(player.facingRight ? -40f : 40f);
                    checkBossDeath();
                    return;
                }
            }
        }
    }

    private void moveY(float distance) {
        int steps = Math.max(
            1, (int)Math.ceil(Math.abs(distance) / MOVE_STEP)
        );
        float step = distance / steps;

        player.grounded = false;

        for (int i = 0; i < steps; i++) {
            player.bounds.y += step;

            for (Rectangle b : blocks) {
                if (player.bounds.overlaps(b)) {
                    if (step < 0f) {
                        player.bounds.y = b.y + b.height;
                        player.grounded = true;
                    } else if (step > 0f) {
                        player.bounds.y =
                            b.y - player.bounds.height;
                    }

                    player.velocityY = 0f;
                    return;
                }
            }
        }
    }
}
