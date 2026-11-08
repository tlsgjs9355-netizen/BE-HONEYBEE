
package io.jbnu.test;

import com.badlogic.gdx.math.Rectangle;

public class Projectile {

    public static final float SPEED = 560f;
    public static final float WIDTH = 12f;
    public static final float HEIGHT = 6f;
    public static final float LIFETIME = 1.2f;
    public static final int DAMAGE = 5;
    public static final int MAX_COUNT = 6;

    public final Rectangle bounds;
    public final int direction;

    private float lifetimeRemaining;
    private boolean expired;

    public Projectile(float x, float y, boolean facingRight) {
        bounds = new Rectangle(x, y, WIDTH, HEIGHT);
        direction = facingRight ? 1 : -1;
        lifetimeRemaining = LIFETIME;
        expired = false;
    }

    public void update(float dt) {
        if (expired) return;

        bounds.x += direction * SPEED * dt;
        lifetimeRemaining -= dt;

        if (lifetimeRemaining <= 0f) {
            expired = true;
        }
    }

    public boolean isExpired() {
        return expired;
    }

    public void destroy() {
        expired = true;
    }
}
