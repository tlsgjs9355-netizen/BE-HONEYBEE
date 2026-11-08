
package io.jbnu.test;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;

/** Moving spider trap. A new downward trip alternates its facing direction. */
public class SpiderTrap {
    public static final float WIDTH = 46f;
    public static final float HEIGHT = 40f;
    public static final float STUN_TIME = 2f;

    public final Rectangle bounds;

    private final float anchorX;
    private final float topY;
    private final float bottomY;
    private final float speed;

    private float direction = -1f;
    private float stunTimer;
    private boolean descendingFacesLeft = true;

    public SpiderTrap(float x, float topY, float bottomY, float speed) {
        this.anchorX = x;
        this.topY = topY;
        this.bottomY = bottomY;
        this.speed = speed;
        bounds = new Rectangle(x - WIDTH / 2f, topY, WIDTH, HEIGHT);
    }

    public void update(float dt) {
        if (stunTimer > 0f) {
            stunTimer = Math.max(0f, stunTimer - dt);
            return;
        }
        if (dt <= 0f) return;

        bounds.y += direction * speed * dt;

        if (bounds.y <= bottomY) {
            bounds.y = bottomY;
            direction = 1f;
        } else if (bounds.y >= topY && direction > 0f) {
            bounds.y = topY;
            direction = -1f;
            descendingFacesLeft = !descendingFacesLeft;
        }
    }

    public void stun() {
        stunTimer = STUN_TIME;
        bounds.y = topY;
        direction = -1f;
    }

    public boolean isStunned() { return stunTimer > 0f; }
    public boolean canCapture() { return !isStunned(); }
    public boolean isMovingUp() { return direction > 0f; }
    public boolean isMovingDown() { return direction < 0f; }

    /** Left, right, left, right on successive downward runs. */
    public boolean isDescendingFacingLeft() {
        return descendingFacesLeft;
    }

    public float getAnchorX() { return anchorX; }
    public float getAnchorY() { return topY + HEIGHT + 24f; }
    public float getProgress() {
        return MathUtils.clamp(
            (topY - bounds.y) / Math.max(1f, topY - bottomY), 0f, 1f
        );
    }
}
