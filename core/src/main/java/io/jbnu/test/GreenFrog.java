
package io.jbnu.test;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

public final class GreenFrog {

    public enum State {
        IDLE, HOP, WARNING, STRIKE, RECOVER
    }

    public static final float WIDTH = 44f;
    public static final float HEIGHT = 31f;

    private static final float SIGHT_X = 360f;
    private static final float SIGHT_Y = 190f;
    private static final float ATTACK_X = 170f;
    private static final float ATTACK_Y = 135f;

    private static final float PATROL_RADIUS = 105f;
    private static final float CHASE_RADIUS = 145f;
    private static final float PATROL_HOP = 78f;
    private static final float CHASE_HOP = 112f;

    private static final float HOP_HEIGHT = 45f;
    private static final float HOP_TIME = 0.55f;
    private static final float IDLE_DELAY = 0.35f;
    private static final float PATROL_WAIT = 0.80f;
    private static final float WARNING_TIME = 0.38f;
    private static final float STRIKE_TIME = 0.25f;
    private static final float RECOVERY_TIME = 0.85f;

    public final Rectangle bounds;

    private final float homeX;
    private final float groundY;
    private final float patrolMinX;
    private final float patrolMaxX;

    private State state = State.IDLE;
    private float timer;
    private float hopStartX;
    private float hopEndX;
    private float hopProgress;
    private float targetX;
    private float targetY;
    private float dirX = 1f;
    private float dirY;
    private float reach;
    private int patrolDirection = 1;
    private boolean hitRegistered;
    private boolean facingRight = true;

    // Compatibility with older two-argument calls.
    public GreenFrog(float x, float y) {
        this(x, y, x - CHASE_RADIUS, x + CHASE_RADIUS);
    }

    // minX/maxX describe the frog's permitted LEFT-edge positions.
    public GreenFrog(
        float x, float y, float minX, float maxX
    ) {
        float lo = Math.min(minX, maxX);
        float hi = Math.max(minX, maxX);

        patrolMinX = lo;
        patrolMaxX = hi;
        bounds = new Rectangle(
            MathUtils.clamp(x, lo, hi),
            y, WIDTH, HEIGHT
        );

        homeX = bounds.x;
        groundY = y;
        timer = 0f;
    }

    public void update(float dt, Player bee) {
        update(dt, bee, null);
    }

    public void update(
        float dt,
        Player bee,
        Array<Rectangle> solidBlocks
    ) {
        if (dt <= 0f) return;

        float elapsed = Math.min(dt, 1f / 20f);
        timer += elapsed;

        float px = bee.bounds.x + bee.bounds.width * .5f;
        float py = bee.bounds.y + bee.bounds.height * .5f;

        float dx = px - mouthX();
        float dy = py - mouthY();

        boolean spotted =
            Math.abs(dx) <= SIGHT_X
                && Math.abs(dy) <= SIGHT_Y;

        boolean inTongueRange =
            Math.abs(dx) <= ATTACK_X
                && Math.abs(dy) <= ATTACK_Y;

        switch (state) {
            case IDLE:
                bounds.y = groundY;

                if (spotted) {
                    facingRight = dx >= 0f;
                }

                if (timer < IDLE_DELAY) break;

                if (spotted && inTongueRange
                    && !lineBlocked(px, py, solidBlocks)) {

                    beginWarning(px, py);

                } else if (spotted) {
                    beginHop(
                        px >= centerX() ? 1 : -1,
                        true
                    );

                } else if (timer >= PATROL_WAIT) {
                    beginHop(patrolDirection, false);
                }
                break;

            case HOP:
                hopProgress = MathUtils.clamp(
                    timer / HOP_TIME, 0f, 1f
                );

                bounds.x = MathUtils.lerp(
                    hopStartX, hopEndX, hopProgress
                );

                bounds.y = groundY
                    + HOP_HEIGHT * 4f
                    * hopProgress * (1f - hopProgress);

                if (hopProgress >= 1f) {
                    bounds.x = hopEndX;
                    bounds.y = groundY;
                    state = State.IDLE;
                    timer = 0f;
                    hopProgress = 0f;
                }
                break;

            case WARNING:
                // Target direction was fixed at the start of warning.
                if (timer >= WARNING_TIME) {
                    state = State.STRIKE;
                    timer = 0f;
                }
                break;

            case STRIKE:
                if (timer >= STRIKE_TIME) {
                    state = State.RECOVER;
                    timer = 0f;
                }
                break;

            case RECOVER:
                if (timer >= RECOVERY_TIME) {
                    state = State.IDLE;
                    timer = 0f;
                }
                break;
        }
    }

    // Check whether a solid platform blocks the tongue path.
    private boolean lineBlocked(
        float px,
        float py,
        Array<Rectangle> solids
    ) {
        if (solids == null) return false;

        float x1 = mouthX();
        float y1 = mouthY();
        float dx = px - x1;
        float dy = py - y1;

        int samples = Math.max(
            1, (int)(Math.sqrt(dx * dx + dy * dy) / 6f)
        );

        for (int i = 1; i < samples; i++) {
            float t = (float)i / samples;
            float x = x1 + dx * t;
            float y = y1 + dy * t;

            for (Rectangle wall : solids) {
                if (wall.contains(x, y)) {
                    return true;
                }
            }
        }

        return false;
    }

    private void beginHop(int direction, boolean chase) {
        float limit = chase ? CHASE_RADIUS : PATROL_RADIUS;

        float left = Math.max(
            homeX - limit, patrolMinX
        );

        float right = Math.min(
            homeX + limit, patrolMaxX
        );

        float step = chase ? CHASE_HOP : PATROL_HOP;
        int chosen = direction >= 0 ? 1 : -1;

        float next = MathUtils.clamp(
            bounds.x + chosen * step,
            left, right
        );

        if (Math.abs(next - bounds.x) < 20f) {
            chosen = -chosen;

            next = MathUtils.clamp(
                bounds.x + chosen * step,
                left, right
            );
        }

        if (Math.abs(next - bounds.x) < 5f) {
            timer = 0f;
            return;
        }

        facingRight = chosen > 0;
        patrolDirection = -chosen;
        hopStartX = bounds.x;
        hopEndX = next;
        hopProgress = 0f;
        state = State.HOP;
        timer = 0f;
    }

    private void beginWarning(float px, float py) {
        float dx = px - mouthX();
        float dy = py - mouthY();

        float length = (float)Math.sqrt(
            dx * dx + dy * dy
        );

        facingRight = dx >= 0f;
        targetX = px;
        targetY = py;

        reach = MathUtils.clamp(
            length + 12f, 18f, 292f
        );

        dirX = length < .01f
            ? (facingRight ? 1f : -1f)
            : dx / length;

        dirY = length < .01f ? 0f : dy / length;

        hitRegistered = false;
        state = State.WARNING;
        timer = 0f;
    }

    // A single tongue strike can register only one hit.
    public boolean tryHit(Rectangle bee) {
        if (state != State.STRIKE || hitRegistered) {
            return false;
        }

        float length = currentTongueLength();
        float ox = mouthX();
        float oy = mouthY();

        int segments = Math.max(
            1, (int)(length / 6f)
        );

        for (int i = 1; i <= segments; i++) {
            float t = (float)i / segments;
            float x = ox + dirX * length * t;
            float y = oy + dirY * length * t;

            if (bee.contains(x, y)
                || bee.contains(x - 5f, y)
                || bee.contains(x + 5f, y)
                || bee.contains(x, y - 5f)
                || bee.contains(x, y + 5f)) {

                hitRegistered = true;
                return true;
            }
        }

        return false;
    }

    public float centerX() {
        return bounds.x + WIDTH * .5f;
    }

    public float getPatrolMinX() {
        return patrolMinX;
    }

    public float getPatrolMaxX() {
        return patrolMaxX;
    }

    public float mouthX() {
        return facingRight
            ? bounds.x + WIDTH - 5f
            : bounds.x + 5f;
    }

    public float mouthY() {
        return bounds.y + HEIGHT - 11f;
    }

    public float tipX() {
        return mouthX() + dirX * currentTongueLength();
    }

    public float tipY() {
        return mouthY() + dirY * currentTongueLength();
    }

    public float warningTipX() {
        return mouthX() + dirX * reach;
    }

    public float warningTipY() {
        return mouthY() + dirY * reach;
    }

    public float getHopProgress() {
        return hopProgress;
    }

    public boolean isFacingRight() {
        return facingRight;
    }

    public State getState() {
        return state;
    }

    public float getWarningProgress() {
        return state == State.WARNING
            ? MathUtils.clamp(
            timer / WARNING_TIME, 0f, 1f
        )
            : 0f;
    }

    private float currentTongueLength() {
        if (state != State.STRIKE) return 0f;

        float t = MathUtils.clamp(
            timer / STRIKE_TIME, 0f, 1f
        );

        return reach * (
            t < .55f
                ? t / .55f
                : (1f - t) / .45f
        );
    }
}
