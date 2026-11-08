
package io.jbnu.test;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

/**
 * Stage 3: Asian hornet boss. M6.29.
 * DASH = 2 HP, BITE = 2.5 HP, STING = 3 HP.
 * Target is fixed before each warning.
 */
public class Hornet extends Enemy {

    public static final int MAX_HP = 150;

    public enum Pattern {
        DASH, BITE, STING
    }

    private static final float LEFT = 2200f;
    private static final float RIGHT = 3000f;
    private static final float BOTTOM = 40f;
    private static final float TOP = 600f;
    private static final float NORMAL_IDLE = 0.62f;
    private static final float FAST_IDLE = 0.48f;

    private final Vector2 attackStart = new Vector2();
    private final Vector2 target = new Vector2();
    private final Vector2 direction = new Vector2(-1f, 0f);
    private final Rectangle attackBounds = new Rectangle();

    private Pattern pattern = Pattern.DASH;
    private int patternIndex;
    private int chargesLeft;
    private int facing = -1;

    private boolean attackHit;
    private boolean phaseTwo;

    private float warningDuration;
    private float attackDuration;
    private float recoveryDuration;

    public Hornet() {
        super(64f, 40f, MAX_HP);
        reset();
    }

    @Override
    public void reset() {
        resetState();
        bounds.setPosition(2748f, 300f);

        pattern = Pattern.DASH;
        patternIndex = 0;
        chargesLeft = 0;
        facing = -1;

        attackHit = false;
        phaseTwo = false;

        warningDuration = 0.92f;
        attackDuration = 0.60f;
        recoveryDuration = 0.82f;

        attackStart.set(centerX(), centerY());
        target.set(attackStart);
        direction.set(-1f, 0f);
        attackBounds.set(0f, 0f, 0f, 0f);
    }

    @Override
    public void update(float dt, Player player) {
        if (isDead() || dt <= 0f) return;

        timer += dt;

        switch (state) {
            case IDLE:
                updateIdle(dt, player);
                break;

            case TELEGRAPH:
                if (timer >= warningDuration) {
                    attackHit = false;
                    changeState(State.ATTACK);
                }
                break;

            case ATTACK:
                updateAttack(player);
                break;

            case RECOVER:
                if (timer >= recoveryDuration) {
                    changeState(State.IDLE);
                }
                break;
        }
    }

    private void updateIdle(float dt, Player player) {
        phaseTwo = hp <= MAX_HP / 2;

        Pattern next = Pattern.values()[
            patternIndex % Pattern.values().length
            ];

        float px = player.bounds.x
            + player.bounds.width / 2f;

        float py = player.bounds.y
            + player.bounds.height / 2f;

        facing = px < centerX() ? -1 : 1;

        float desiredY = MathUtils.clamp(
            py, BOTTOM + 80f, TOP - 75f
        );

        float verticalSpeed = next == Pattern.BITE
            ? 210f : 155f;

        setCenter(
            centerX(),
            centerY() + MathUtils.clamp(
                desiredY - centerY(),
                -verticalSpeed * dt,
                verticalSpeed * dt
            )
        );

        // Bite approaches during IDLE only.
        // During TELEGRAPH, its target remains fixed.
        if (next == Pattern.BITE) {
            float dx = px - centerX();

            float approach = Math.max(
                0f,
                Math.abs(dx) - 115f
            );

            float step = Math.min(
                approach,
                (phaseTwo ? 330f : 285f) * dt
            );

            setCenter(
                centerX() + Math.signum(dx) * step,
                centerY()
            );
        }

        if (timer >= (
            phaseTwo ? FAST_IDLE : NORMAL_IDLE
        )) {
            beginPattern(player);
        }
    }

    private void beginPattern(Player player) {
        phaseTwo = hp <= MAX_HP / 2;

        pattern = Pattern.values()[
            patternIndex % Pattern.values().length
            ];

        chargesLeft = pattern == Pattern.DASH
            && phaseTwo ? 2 : 1;

        prepareAttack(player);
    }

    private void prepareAttack(Player player) {
        float px = player.bounds.x
            + player.bounds.width / 2f;

        float py = player.bounds.y
            + player.bounds.height / 2f;

        attackStart.set(centerX(), centerY());
        facing = px < centerX() ? -1 : 1;

        float dx = px - centerX();
        float dy = py - centerY();

        float distance = (float) Math.sqrt(
            dx * dx + dy * dy
        );

        if (distance < 0.01f) {
            dx = facing;
            dy = 0f;
            distance = 1f;
        }

        float ux = dx / distance;
        float uy = dy / distance;

        float move;
        float speed;

        if (pattern == Pattern.BITE) {
            // Stop near the bee, attack with a short jaw hitbox.
            move = MathUtils.clamp(
                distance - 37f,
                0f, 140f
            );

            warningDuration = phaseTwo
                ? 0.76f : 0.91f;

            speed = phaseTwo
                ? 455f : 395f;

        } else if (pattern == Pattern.STING) {
            // Thrust slightly beyond the snapshotted bee position.
            move = Math.min(
                590f,
                distance + 74f
            );

            warningDuration = phaseTwo
                ? 0.81f : 1.02f;

            speed = phaseTwo
                ? 625f : 535f;

        } else {
            move = Math.min(
                610f,
                distance + 50f
            );

            warningDuration = phaseTwo
                ? 0.77f : 0.93f;

            speed = phaseTwo
                ? 635f : 550f;
        }

        target.set(
            MathUtils.clamp(
                attackStart.x + ux * move,
                LEFT + bounds.width / 2f,
                RIGHT - bounds.width / 2f
            ),
            MathUtils.clamp(
                attackStart.y + uy * move,
                BOTTOM + bounds.height / 2f,
                TOP - bounds.height / 2f
            )
        );

        direction.set(
            target.x - attackStart.x,
            target.y - attackStart.y
        );

        float length = direction.len();

        if (length > 0.001f) {
            direction.scl(1f / length);
        } else {
            direction.set(facing, 0f);
        }

        if (pattern == Pattern.BITE) {
            attackDuration = 0.61f;
        } else {
            attackDuration = MathUtils.clamp(
                length / speed,
                0.30f, 0.98f
            );
        }

        attackBounds.set(0f, 0f, 0f, 0f);
        changeState(State.TELEGRAPH);
    }

    private void updateAttack(Player player) {
        float p = MathUtils.clamp(
            timer / attackDuration,
            0f, 1f
        );

        if (pattern == Pattern.BITE) {
            float travel = MathUtils.clamp(
                p / 0.52f,
                0f, 1f
            );

            setCenter(
                MathUtils.lerp(
                    attackStart.x,
                    target.x,
                    travel
                ),
                MathUtils.lerp(
                    attackStart.y,
                    target.y,
                    travel
                )
            );

        } else {
            setCenter(
                MathUtils.lerp(
                    attackStart.x,
                    target.x,
                    p
                ),
                MathUtils.lerp(
                    attackStart.y,
                    target.y,
                    p
                )
            );
        }

        updateAttackBounds();

        if (p >= 1f) {
            if (pattern == Pattern.DASH
                && chargesLeft > 1) {

                chargesLeft--;
                prepareAttack(player);

            } else {
                patternIndex++;

                recoveryDuration = phaseTwo
                    ? 0.72f : 0.90f;

                attackBounds.set(
                    0f, 0f, 0f, 0f
                );

                changeState(State.RECOVER);
            }
        }
    }

    private void updateAttackBounds() {
        if (pattern == Pattern.BITE) {
            attackBounds.set(
                facing > 0
                    ? bounds.x + bounds.width - 3f
                    : bounds.x - 37f,
                bounds.y + 6f,
                40f, 29f
            );

        } else if (pattern == Pattern.STING) {
            float bx = centerX()
                + direction.x * 26f;

            float by = centerY()
                + direction.y * 16f;

            attackBounds.set(
                bx - 21f,
                by - 17f,
                42f, 34f
            );

        } else {
            attackBounds.set(
                bounds.x + 4f,
                bounds.y + 4f,
                bounds.width - 8f,
                bounds.height - 8f
            );
        }
    }

    private void setCenter(float x, float y) {
        bounds.x = MathUtils.clamp(
            x - bounds.width / 2f,
            LEFT,
            RIGHT - bounds.width
        );

        bounds.y = MathUtils.clamp(
            y - bounds.height / 2f,
            BOTTOM,
            TOP - bounds.height
        );
    }

    public boolean isAttackDangerous() {
        if (isDead()
            || state != State.ATTACK
            || attackHit) {
            return false;
        }

        float p = attackDuration > 0f
            ? timer / attackDuration
            : 1f;

        if (pattern == Pattern.BITE) {
            return p >= 0.50f
                && p <= 0.84f;
        }

        return p >= 0.08f
            && p <= 0.96f;
    }

    public void markAttackHit() {
        attackHit = true;
    }

    public int getAttackDamageHalfHearts() {
        if (pattern == Pattern.BITE) {
            return 5;
        }

        if (pattern == Pattern.STING) {
            return 6;
        }

        return 4;
    }

    public Rectangle getAttackBounds() {
        return attackBounds;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public Vector2 getTarget() {
        return target;
    }

    public Vector2 getAttackStart() {
        return attackStart;
    }

    public Vector2 getDirection() {
        return direction;
    }

    public int getFacing() {
        return facing;
    }

    public float getAttackProgress() {
        return state == State.ATTACK
            && attackDuration > 0f
            ? MathUtils.clamp(
            timer / attackDuration,
            0f, 1f
        ) : 0f;
    }

    public float getWarningProgress() {
        return state == State.TELEGRAPH
            && warningDuration > 0f
            ? MathUtils.clamp(
            timer / warningDuration,
            0f, 1f
        ) : 0f;
    }

    public boolean isPhaseTwo() {
        return hp <= MAX_HP / 2;
    }
}
