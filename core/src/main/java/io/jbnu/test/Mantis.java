
package io.jbnu.test;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;

/**
 * Stage 2 Mantis Boss - M6.22
 * Faster combat tempo with fair attack warnings.
 */
public class Mantis extends Enemy {

    public static final int MAX_HP = 100;

    public enum Pattern {
        CLAW, LEAP_CAPTURE, SLASH
    }

    private static final float LEFT = 2400f;
    private static final float RIGHT = 3200f;
    private static final float GROUND = 40f;

    private static final float STOP_DISTANCE = 182f;
    private static final float INTRO_STOP_DISTANCE = 255f;
    private static final float INTRO_APPROACH_SECONDS = 0.42f;
    private static final float INTRO_APPROACH_SPEED = 650f;
    private static final float INTRO_CLAW_WINDUP = 0.55f;
    private static final float INTRO_RECOVERY = 0.38f;

    // Attack warning durations
    private static final float CLAW_WINDUP = 0.88f;
    private static final float SECOND_CLAW_WINDUP = 0.72f;
    private static final float CLAW_WINDUP_PHASE_TWO = 0.79f;

    private static final float LEAP_WINDUP = 0.92f;
    private static final float LEAP_WINDUP_PHASE_TWO = 0.82f;

    private static final float SLASH_WINDUP = 0.90f;
    private static final float SLASH_WINDUP_PHASE_TWO = 0.82f;

    private static final float SLASH_SPEED = 460f;
    private static final float SLASH_SPEED_PHASE_TWO = 510f;
    private static final float SLASH_RELEASE_TIME = 0.18f;
    private static final float SECOND_SLASH_START = 0.62f;

    // Claw dash
    private static final float FIRST_DASH_DURATION = 0.34f;
    private static final float FIRST_SWING_START = 0.21f;
    private static final float FIRST_SWING_END = 0.59f;
    private static final float CLAW_SINGLE_DURATION = 0.68f;
    private static final float INTRO_CLAW_DURATION = 0.70f;

    private static final float CLAW_STOP_GAP = 75f;
    private static final float CLAW_MIN_DASH = 28f;
    private static final float CLAW_MAX_DASH = 540f;
    private static final float CLAW_DASH_ARC = 24f;
    private static final float CLAW_LANDING_SPEED = 1080f;

    private final Rectangle clawBounds = new Rectangle();
    private final Rectangle slashBounds = new Rectangle();
    private final Rectangle secondSlashBounds = new Rectangle();

    private Pattern pattern = Pattern.CLAW;
    private int patternIndex;
    private int direction = -1;

    private boolean walking;
    private boolean phaseTwo;
    private boolean firstAttack = true;
    private boolean introRecovering;
    private boolean attackHit;
    private boolean secondClawStarted;
    private boolean slashLaunched;
    private boolean secondSlashLaunched;
    private boolean secondSlashHit;

    private float leapStartX;
    private float leapStartY;
    private float leapTargetX;
    private float leapTargetY;

    private float clawDashStartX;
    private float clawDashTargetX;
    private float clawDashStartY;
    private float clawDashTargetY;
    private float clawDashHeadingX;
    private float clawDashHeadingY;

    private float slashStartX;
    private float slashStartY;
    private float slashVelocityX;
    private float slashVelocityY;

    private float secondSlashStartX;
    private float secondSlashStartY;
    private float secondSlashVelocityX;
    private float secondSlashVelocityY;

    public Mantis() {
        super(72f, 100f, MAX_HP);
        reset();
    }

    @Override
    public void reset() {
        resetState();
        bounds.setPosition(2884f, GROUND);

        pattern = Pattern.CLAW;
        patternIndex = 0;
        direction = -1;

        walking = false;
        phaseTwo = false;
        firstAttack = true;
        introRecovering = false;
        attackHit = false;
        secondClawStarted = false;
        slashLaunched = false;
        secondSlashLaunched = false;
        secondSlashHit = false;

        clawDashStartX = bounds.x;
        clawDashTargetX = bounds.x;
        clawDashStartY = bounds.y;
        clawDashTargetY = bounds.y;
        clawDashHeadingX = -1f;
        clawDashHeadingY = 0f;

        leapStartX = bounds.x;
        leapStartY = bounds.y;
        leapTargetX = bounds.x;
        leapTargetY = bounds.y;

        slashStartX = bounds.x;
        slashStartY = 82f;
        slashVelocityX = -SLASH_SPEED;
        slashVelocityY = 0f;

        secondSlashStartX = bounds.x;
        secondSlashStartY = 174f;
        secondSlashVelocityX = -SLASH_SPEED_PHASE_TWO;
        secondSlashVelocityY = 0f;

        clawBounds.set(0f, 0f, 0f, 0f);
        slashBounds.set(0f, 0f, 0f, 0f);
        secondSlashBounds.set(0f, 0f, 0f, 0f);
    }

    private void face(Player player) {
        float playerMid = player.bounds.x
            + player.bounds.width / 2f;

        direction = playerMid < centerX() ? -1 : 1;
    }

    @Override
    public void update(float dt, Player player) {
        if (isDead() || dt <= 0f) {
            walking = false;
            return;
        }

        timer += dt;
        phaseTwo = hp <= MAX_HP / 2;

        switch (state) {

            case IDLE:
                face(player);

                float distance = Math.abs(
                    player.bounds.x
                        + player.bounds.width / 2f
                        - centerX()
                );

                float wantedGap = firstAttack
                    ? INTRO_STOP_DISTANCE
                    : STOP_DISTANCE;

                float speed = firstAttack
                    ? INTRO_APPROACH_SPEED
                    : (phaseTwo ? 235f : 180f);

                float step = Math.min(
                    Math.max(0f, distance - wantedGap),
                    speed * dt
                );

                float oldX = bounds.x;

                bounds.x = MathUtils.clamp(
                    bounds.x + direction * step,
                    LEFT,
                    RIGHT - bounds.width
                );

                bounds.y = GROUND;

                walking = Math.abs(
                    bounds.x - oldX
                ) > 0.01f;

                float approachTime = firstAttack
                    ? INTRO_APPROACH_SECONDS
                    : (phaseTwo ? 0.55f : 0.70f);

                if (timer >= approachTime) {
                    walking = false;

                    pattern = firstAttack
                        ? Pattern.CLAW
                        : Pattern.values()[patternIndex % 3];

                    attackHit = false;
                    secondSlashHit = false;
                    secondClawStarted = false;
                    slashLaunched = false;
                    secondSlashLaunched = false;

                    // Fix the target before the warning.
                    if (pattern == Pattern.SLASH) {
                        prepareSlashAim(player);
                    }

                    if (pattern == Pattern.CLAW) {
                        startClawDash(player);
                    }

                    if (pattern == Pattern.LEAP_CAPTURE) {
                        prepareLeapAim(player);
                    }

                    changeState(State.TELEGRAPH);
                }
                break;

            case TELEGRAPH:
                walking = false;

                float windup;

                if (firstAttack) {
                    windup = INTRO_CLAW_WINDUP;

                } else if (pattern == Pattern.CLAW) {
                    windup = secondClawStarted
                        ? SECOND_CLAW_WINDUP
                        : phaseTwo
                        ? CLAW_WINDUP_PHASE_TWO
                        : CLAW_WINDUP;

                } else if (pattern == Pattern.LEAP_CAPTURE) {
                    windup = phaseTwo
                        ? LEAP_WINDUP_PHASE_TWO
                        : LEAP_WINDUP;

                } else {
                    windup = phaseTwo
                        ? SLASH_WINDUP_PHASE_TWO
                        : SLASH_WINDUP;
                }

                if (timer >= windup) {
                    beginAttack(player);
                }
                break;

            case ATTACK:
                walking = false;

                if (pattern == Pattern.CLAW) {

                    float dashP = MathUtils.clamp(
                        timer / FIRST_DASH_DURATION,
                        0f, 1f
                    );

                    float eased =
                        1f - (1f - dashP) * (1f - dashP);

                    bounds.x = MathUtils.clamp(
                        MathUtils.lerp(
                            clawDashStartX,
                            clawDashTargetX,
                            eased
                        ),
                        LEFT,
                        RIGHT - bounds.width
                    );

                    bounds.y = MathUtils.clamp(
                        MathUtils.lerp(
                            clawDashStartY,
                            clawDashTargetY,
                            eased
                        )
                            + CLAW_DASH_ARC
                            * MathUtils.sin(MathUtils.PI * dashP),
                        GROUND,
                        600f - bounds.height
                    );

                    setClawBounds();

                    float duration = firstAttack
                        ? INTRO_CLAW_DURATION
                        : CLAW_SINGLE_DURATION;

                    if (timer >= duration) {

                        if (phaseTwo
                            && !firstAttack
                            && !secondClawStarted) {

                            // Second attack has a separate warning.
                            secondClawStarted = true;
                            attackHit = false;

                            startClawDash(player);
                            changeState(State.TELEGRAPH);

                        } else {
                            finishAttack();
                        }
                    }
                    break;

                } else if (pattern == Pattern.LEAP_CAPTURE) {

                    float leapDuration = phaseTwo
                        ? 0.66f : 0.83f;

                    float p = MathUtils.clamp(
                        timer / leapDuration,
                        0f, 1f
                    );

                    bounds.x = MathUtils.clamp(
                        MathUtils.lerp(
                            leapStartX,
                            leapTargetX,
                            p
                        ),
                        LEFT,
                        RIGHT - bounds.width
                    );

                    bounds.y = MathUtils.clamp(
                        MathUtils.lerp(
                            leapStartY,
                            leapTargetY,
                            p
                        )
                            + (phaseTwo ? 230f : 205f)
                            * MathUtils.sin(MathUtils.PI * p),
                        GROUND,
                        600f - bounds.height
                    );

                    if (p >= 1f) {
                        finishAttack();
                    }

                } else {

                    if (!slashLaunched
                        && timer >= SLASH_RELEASE_TIME) {

                        slashLaunched = true;

                        slashBounds.set(
                            slashStartX,
                            slashStartY,
                            68f, 48f
                        );
                    }

                    if (phaseTwo
                        && !secondSlashLaunched
                        && timer >= SECOND_SLASH_START) {

                        secondSlashLaunched = true;
                        secondSlashHit = false;

                        secondSlashBounds.set(
                            secondSlashStartX,
                            secondSlashStartY,
                            68f, 48f
                        );
                    }

                    if (slashLaunched) {
                        slashBounds.x += slashVelocityX * dt;
                        slashBounds.y += slashVelocityY * dt;
                    }

                    if (secondSlashLaunched) {
                        secondSlashBounds.x +=
                            secondSlashVelocityX * dt;

                        secondSlashBounds.y +=
                            secondSlashVelocityY * dt;
                    }

                    if (timer >= (
                        phaseTwo ? 1.46f : 1.22f
                    )) {
                        finishAttack();
                    }
                }
                break;

            case RECOVER:
                walking = false;

                if (pattern == Pattern.CLAW
                    && bounds.y > GROUND) {

                    bounds.y = Math.max(
                        GROUND,
                        bounds.y - CLAW_LANDING_SPEED * dt
                    );
                }

                float recovery = introRecovering
                    ? INTRO_RECOVERY
                    : (phaseTwo ? 0.52f : 0.67f);

                if (timer >= recovery
                    && bounds.y <= GROUND + 0.01f) {

                    introRecovering = false;
                    face(player);
                    changeState(State.IDLE);
                }
                break;
        }
    }

    // ======================================================
    // Claw dash targeting
    // ======================================================

    private void startClawDash(Player player) {
        face(player);

        clawDashStartX = bounds.x;
        clawDashStartY = bounds.y;

        float dx = player.bounds.x
            + player.bounds.width / 2f
            - centerX();

        float dy = player.bounds.y
            + player.bounds.height / 2f
            - centerY();

        float distance = (float) Math.sqrt(
            dx * dx + dy * dy
        );

        if (distance < 0.01f) {
            dx = direction;
            dy = 0f;
            distance = 1f;
        }

        clawDashHeadingX = dx / distance;
        clawDashHeadingY = dy / distance;

        float travel = MathUtils.clamp(
            distance - CLAW_STOP_GAP,
            CLAW_MIN_DASH,
            CLAW_MAX_DASH
        );

        clawDashTargetX = MathUtils.clamp(
            clawDashStartX + clawDashHeadingX * travel,
            LEFT,
            RIGHT - bounds.width
        );

        clawDashTargetY = MathUtils.clamp(
            clawDashStartY + clawDashHeadingY * travel,
            GROUND,
            600f - bounds.height
        );
    }

    // ======================================================
    // Leap capture targeting
    // ======================================================

    private void prepareLeapAim(Player player) {
        face(player);

        leapStartX = bounds.x;
        leapStartY = bounds.y;

        leapTargetX = MathUtils.clamp(
            player.bounds.x
                + player.bounds.width / 2f
                - bounds.width / 2f,
            LEFT,
            RIGHT - bounds.width
        );

        leapTargetY = MathUtils.clamp(
            player.bounds.y
                + player.bounds.height / 2f
                - bounds.height / 2f,
            GROUND,
            600f - bounds.height
        );
    }

    // ======================================================
    // Slash targeting
    // ======================================================

    private void prepareSlashAim(Player player) {
        face(player);

        float targetX = player.bounds.x
            + player.bounds.width / 2f;

        float targetY = player.bounds.y
            + player.bounds.height / 2f;

        int fireDir = direction;

        float sourceX = fireDir > 0
            ? bounds.x + bounds.width - 3f
            : bounds.x - 65f;

        slashStartX = sourceX;
        slashStartY = 82f - 24f;

        secondSlashStartX = sourceX;
        secondSlashStartY = 174f - 24f;

        float speed = phaseTwo
            ? SLASH_SPEED_PHASE_TWO
            : SLASH_SPEED;

        float dx = targetX - (slashStartX + 34f);
        float dy = targetY - (slashStartY + 24f);

        float length = (float) Math.sqrt(
            dx * dx + dy * dy
        );

        if (length < 1f) {
            dx = fireDir;
            dy = 0f;
            length = 1f;
        }

        slashVelocityX = dx / length * speed;
        slashVelocityY = dy / length * speed;

        dx = targetX - (secondSlashStartX + 34f);
        dy = targetY - (secondSlashStartY + 24f);

        length = (float) Math.sqrt(
            dx * dx + dy * dy
        );

        if (length < 1f) {
            dx = fireDir;
            dy = 0f;
            length = 1f;
        }

        secondSlashVelocityX =
            dx / length * SLASH_SPEED_PHASE_TWO;

        secondSlashVelocityY =
            dy / length * SLASH_SPEED_PHASE_TWO;
    }

    // ======================================================
    // Attack state management
    // ======================================================

    private void beginAttack(Player player) {
        attackHit = false;

        // Preserve secondClawStarted across the second warning.
        slashLaunched = false;
        secondSlashLaunched = false;
        secondSlashHit = false;

        if (pattern == Pattern.CLAW) {
            setClawBounds();
        }

        changeState(State.ATTACK);
    }

    private void setClawBounds() {
        float width = phaseTwo && !firstAttack
            ? 136f : 124f;

        float x = direction > 0
            ? bounds.x + bounds.width - 8f
            : bounds.x - width + 8f;

        clawBounds.set(
            x,
            bounds.y + 4f,
            width,
            phaseTwo && !firstAttack ? 166f : 154f
        );
    }

    private void finishAttack() {
        if (pattern == Pattern.LEAP_CAPTURE) {
            bounds.y = GROUND;
        }

        slashLaunched = false;
        secondSlashLaunched = false;

        if (firstAttack) {
            introRecovering = true;
        } else {
            patternIndex++;
        }

        firstAttack = false;
        changeState(State.RECOVER);
    }

    public void onCapture() {
        bounds.y = GROUND;
        attackHit = true;
        firstAttack = false;
        patternIndex++;
        changeState(State.RECOVER);
    }

    // ======================================================
    // Collision checks
    // ======================================================

    public boolean isClawDangerous() {
        if (state != State.ATTACK
            || pattern != Pattern.CLAW
            || firstAttack
            || attackHit) {
            return false;
        }

        return timer >= 0.25f && timer <= 0.55f;
    }

    public boolean isLeapDangerous() {
        return state == State.ATTACK
            && pattern == Pattern.LEAP_CAPTURE
            && !attackHit
            && timer >= 0.08f
            && timer <= (phaseTwo ? 0.64f : 0.80f);
    }

    public boolean isSlashDangerous() {
        return state == State.ATTACK
            && pattern == Pattern.SLASH
            && slashLaunched
            && !attackHit;
    }

    public boolean isSecondSlashDangerous() {
        return state == State.ATTACK
            && pattern == Pattern.SLASH
            && secondSlashLaunched
            && !secondSlashHit;
    }

    public void markAttackHit() {
        attackHit = true;
    }

    public void markSecondSlashHit() {
        secondSlashHit = true;
    }

    public Rectangle getClawBounds() {
        return clawBounds;
    }

    public Rectangle getSlashBounds() {
        return slashBounds;
    }

    public Rectangle getSecondSlashBounds() {
        return secondSlashBounds;
    }

    // ======================================================
    // Slash rendering information
    // ======================================================

    public boolean isSlashVisible() {
        return state == State.ATTACK
            && pattern == Pattern.SLASH
            && slashLaunched;
    }

    public boolean isSecondSlashVisible() {
        return state == State.ATTACK
            && pattern == Pattern.SLASH
            && secondSlashLaunched;
    }

    public boolean isSlashWarningVisible(boolean second) {
        if (pattern != Pattern.SLASH || isDead()) {
            return false;
        }

        if (state == State.TELEGRAPH) {
            return !second || phaseTwo;
        }

        if (state != State.ATTACK) {
            return false;
        }

        return second
            ? phaseTwo && !secondSlashLaunched
            : !slashLaunched;
    }

    public float getSlashWarningX(boolean second) {
        return (second
            ? secondSlashStartX
            : slashStartX) + 34f;
    }

    public float getSlashWarningY(boolean second) {
        return (second
            ? secondSlashStartY
            : slashStartY) + 24f;
    }

    public float getSlashWarningVelocityX(boolean second) {
        return second
            ? secondSlashVelocityX
            : slashVelocityX;
    }

    public float getSlashWarningVelocityY(boolean second) {
        return second
            ? secondSlashVelocityY
            : slashVelocityY;
    }

    public float getSlashWarningProgress() {
        if (state == State.ATTACK) {
            return 1f;
        }

        float duration = phaseTwo
            ? SLASH_WINDUP_PHASE_TWO
            : SLASH_WINDUP;

        return MathUtils.clamp(
            timer / duration, 0f, 1f
        );
    }

    // ======================================================
    // Leap warning information
    // ======================================================

    public boolean isLeapWarningVisible() {
        return !isDead()
            && state == State.TELEGRAPH
            && pattern == Pattern.LEAP_CAPTURE;
    }

    public float getLeapWarningX(float progress) {
        float p = MathUtils.clamp(
            progress, 0f, 1f
        );

        return MathUtils.clamp(
            MathUtils.lerp(leapStartX, leapTargetX, p),
            LEFT,
            RIGHT - bounds.width
        ) + bounds.width / 2f;
    }

    public float getLeapWarningY(float progress) {
        float p = MathUtils.clamp(
            progress, 0f, 1f
        );

        return MathUtils.clamp(
            MathUtils.lerp(leapStartY, leapTargetY, p)
                + (phaseTwo ? 230f : 205f)
                * MathUtils.sin(MathUtils.PI * p),
            GROUND,
            600f - bounds.height
        ) + bounds.height / 2f;
    }

    public float getLeapWarningProgress() {
        float duration = phaseTwo
            ? LEAP_WINDUP_PHASE_TWO
            : LEAP_WINDUP;

        return MathUtils.clamp(
            timer / duration, 0f, 1f
        );
    }

    // ======================================================
    // Claw warning information
    // ======================================================

    public boolean isClawWarningVisible() {
        return !isDead()
            && state == State.TELEGRAPH
            && pattern == Pattern.CLAW;
    }

    public float getClawWarningStartX() {
        return clawDashStartX + bounds.width / 2f;
    }

    public float getClawWarningStartY() {
        return clawDashStartY + bounds.height / 2f;
    }

    public float getClawWarningEndX() {
        return clawDashTargetX
            + bounds.width / 2f
            + clawDashHeadingX * 64f;
    }

    public float getClawWarningEndY() {
        return clawDashTargetY
            + bounds.height / 2f
            + clawDashHeadingY * 64f;
    }

    public float getClawWarningProgress() {
        float windup = firstAttack
            ? INTRO_CLAW_WINDUP
            : secondClawStarted
            ? SECOND_CLAW_WINDUP
            : phaseTwo
            ? CLAW_WINDUP_PHASE_TWO
            : CLAW_WINDUP;

        return MathUtils.clamp(
            timer / windup, 0f, 1f
        );
    }

    // ======================================================
    // Claw animation information
    // ======================================================

    public float getClawDashHeadingX() {
        return clawDashHeadingX;
    }

    public float getClawDashHeadingY() {
        return clawDashHeadingY;
    }

    public boolean isClawDashing() {
        return state == State.ATTACK
            && pattern == Pattern.CLAW
            && timer < FIRST_DASH_DURATION;
    }

    public boolean isClawSwingVisible() {
        return state == State.ATTACK
            && pattern == Pattern.CLAW
            && timer >= FIRST_SWING_START
            && timer <= FIRST_SWING_END;
    }

    public float getClawSwingElapsed() {
        return Math.max(
            0f, timer - FIRST_SWING_START
        );
    }

    public boolean isSecondClawSwing() {
        return phaseTwo
            && !firstAttack
            && state == State.ATTACK
            && pattern == Pattern.CLAW
            && secondClawStarted;
    }

    // ======================================================
    // Slash direction and rotation
    // ======================================================

    public int getSlashTravelDirection(boolean second) {
        float vx = second
            ? secondSlashVelocityX
            : slashVelocityX;

        return vx < 0f ? -1 : 1;
    }

    public float getSlashRotation(boolean second) {
        float vx = second
            ? secondSlashVelocityX
            : slashVelocityX;

        float vy = second
            ? secondSlashVelocityY
            : slashVelocityY;

        float angle = (float) Math.toDegrees(
            Math.atan2(vy, Math.abs(vx))
        );

        return vx < 0f ? -angle : angle;
    }

    // ======================================================
    // General getters
    // ======================================================

    public Pattern getPattern() {
        return pattern;
    }

    public int getDirection() {
        return direction;
    }

    public boolean isWalking() {
        return state == State.IDLE && walking;
    }

    public boolean isPhaseTwo() {
        return hp <= MAX_HP / 2;
    }

    public boolean isFirstAttackWhiff() {
        return firstAttack;
    }

    public boolean isIntroSequenceActive() {
        return !isDead()
            && (firstAttack || introRecovering);
    }

    public boolean isIntroApproaching() {
        return firstAttack && state == State.IDLE;
    }

    public boolean isIntroSwing() {
        return firstAttack
            && state == State.ATTACK
            && pattern == Pattern.CLAW
            && timer >= FIRST_SWING_START + 0.04f
            && timer <= FIRST_SWING_END;
    }

    public float getAttackElapsed() {
        return timer;
    }
}
