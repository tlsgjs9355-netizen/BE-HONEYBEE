
package io.jbnu.test;

import com.badlogic.gdx.math.Rectangle;

public class Player {
    public static final float MOVE_SPEED = 220f;
    public static final float FLAP_SPEED = 280f;
    public static final float GRAVITY = -650f;
    public static final float MAX_FALL_SPEED = -420f;
    public static final float WIDTH = 28f;
    public static final float HEIGHT = 24f;

    public static final float DASH_SPEED = 700f;
    public static final float DASH_DURATION = 0.22f;
    public static final float DASH_COOLDOWN = 1.5f;

    public final Rectangle bounds =
        new Rectangle(0, 0, WIDTH, HEIGHT);

    public float velocityX;
    public float velocityY;
    public boolean grounded;
    public boolean facingRight = true;
    public boolean dashUnlocked;

    private boolean dashing;
    private float dashTimer;
    private float dashCooldownTimer;

    public void reset(float x, float y) {
        bounds.setPosition(x, y);
        velocityX = 0f;
        velocityY = 0f;
        grounded = false;
        facingRight = true;
        dashing = false;
        dashTimer = 0f;
        dashCooldownTimer = 0f;
    }

    public void setMovement(boolean left, boolean right) {
        if (dashing) return;

        velocityX = left == right
            ? 0f
            : left ? -MOVE_SPEED : MOVE_SPEED;

        if (velocityX != 0f) {
            facingRight = velocityX > 0f;
        }
    }

    public void flap() {
        if (dashing) return;

        velocityY = FLAP_SPEED;
        grounded = false;
    }

    public boolean startDash() {
        if (!dashUnlocked || dashing
            || dashCooldownTimer > 0f) {
            return false;
        }

        dashing = true;
        dashTimer = DASH_DURATION;
        dashCooldownTimer = DASH_COOLDOWN;

        velocityX = facingRight
            ? DASH_SPEED : -DASH_SPEED;
        velocityY = 0f;
        grounded = false;
        return true;
    }

    public void update(float dt) {
        dashCooldownTimer = Math.max(
            0f, dashCooldownTimer - dt
        );

        if (dashing) {
            dashTimer -= dt;

            if (dashTimer <= 0f) {
                stopDash();
            }
        } else {
            velocityY = Math.max(
                MAX_FALL_SPEED,
                velocityY + GRAVITY * dt
            );
        }
    }

    public void stopDash() {
        dashing = false;
        dashTimer = 0f;
        velocityX = 0f;
    }

    public boolean isDashing() {
        return dashing;
    }

    public float getDashCooldown() {
        return dashCooldownTimer;
    }
}
