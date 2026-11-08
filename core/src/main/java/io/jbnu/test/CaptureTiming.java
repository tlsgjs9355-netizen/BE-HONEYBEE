
package io.jbnu.test;

import com.badlogic.gdx.math.MathUtils;

/** Difficulty-scaled timing challenge for the existing capture HUD. */
public final class CaptureTiming {
    public static final int REQUIRED_HITS = 3;
    public static final int MAX_MISSES = 3;

    public enum Result { NONE, ESCAPED, CAUGHT }
    public enum Feedback { NONE, HIT, MISS }

    private static final float[] STAGE_SPEED = {0f, 0.82f, 1.12f, 1.43f, 1.76f};
    private static final float[] STAGE_WIDTH = {0f, 0.22f, 0.19f, 0.15f, 0.12f};
    private static final float[] STAGE_TIME = {0f, 13.5f, 12f, 10.5f, 9.5f};
    private static final float FEEDBACK_DURATION = 0.26f;
    private static final float MIN_ZONE_WIDTH = 0.10f;
    private static final float TIMING_GRACE = 0.038f;

    private float position;
    private float direction;
    private float zoneStart;
    private float zoneEnd;
    private float speed;
    private float remaining;
    private float feedbackRemaining;
    private int stage;
    private int enemyTier;
    private int hits;
    private int misses;
    private boolean active;
    private Feedback feedback = Feedback.NONE;

    public void start(int stage) {
        start(stage, 0);
    }

    /** enemyTier: 0 = normal spider, 1 = elite, 2 = boss. */
    public void start(int stage, int enemyTier) {
        this.stage = MathUtils.clamp(stage, 1, 4);
        this.enemyTier = MathUtils.clamp(enemyTier, 0, 2);
        this.hits = 0;
        this.misses = 0;
        this.position = 0f;
        this.direction = 1f;
        this.feedbackRemaining = 0f;
        this.feedback = Feedback.NONE;
        this.remaining = Math.max(5.2f,
            STAGE_TIME[this.stage] - this.enemyTier * 0.45f);
        this.active = true;
        updateDifficulty();
        chooseZone();
    }

    private void updateDifficulty() {
        speed = STAGE_SPEED[stage] * (1f + enemyTier * 0.13f)
            * (1f + hits * 0.09f);
        float width = STAGE_WIDTH[stage] - enemyTier * 0.008f
            - hits * 0.004f;
        width = Math.max(MIN_ZONE_WIDTH, width);
        zoneEnd = width; // temporary width; chooseZone establishes actual bounds
    }

    private void chooseZone() {
        float width = zoneEnd;
        // Keep the target away from the extreme ends, where direction reverses.
        float margin = 0.10f;
        zoneStart = MathUtils.random(margin, 1f - margin - width);
        zoneEnd = zoneStart + width;
    }

    public Result update(float delta) {
        if (!active) return Result.NONE;
        float dt = MathUtils.clamp(delta, 0f, 1f / 15f);
        remaining = Math.max(0f, remaining - dt);
        feedbackRemaining = Math.max(0f, feedbackRemaining - dt);
        if (feedbackRemaining <= 0f) feedback = Feedback.NONE;
        if (remaining <= 0f) {
            active = false;
            return Result.CAUGHT;
        }
        float next = position + direction * speed * dt;
        // Reflection rather than clamp avoids sticky edge behavior.
        while (next < 0f || next > 1f) {
            if (next > 1f) { next = 2f - next; direction = -1f; }
            if (next < 0f) { next = -next; direction = 1f; }
        }
        position = next;
        return Result.NONE;
    }

    public Result press() {
        if (!active) return Result.NONE;
        // Small forgiving margin accounts for reflexes and input polling.
        boolean success = position >= zoneStart - TIMING_GRACE
            && position <= zoneEnd + TIMING_GRACE;
        if (success) {
            hits++;
            feedback = Feedback.HIT;
            if (hits >= REQUIRED_HITS) {
                active = false;
                return Result.ESCAPED;
            }
            updateDifficulty();
            chooseZone();
            position = 0f;
            direction = 1f;
        } else {
            misses++;
            feedback = Feedback.MISS;
            if (misses >= MAX_MISSES) {
                active = false;
                return Result.CAUGHT;
            }
        }
        feedbackRemaining = FEEDBACK_DURATION;
        return Result.NONE;
    }

    public void clear() {
        active = false;
        hits = misses = 0;
        position = zoneStart = zoneEnd = remaining = feedbackRemaining = 0f;
        direction = 1f;
        feedback = Feedback.NONE;
    }

    public int getHits() { return hits; }
    public int getMisses() { return misses; }
    public float getPosition() { return position; }
    public float getZoneStart() { return zoneStart; }
    public float getZoneEnd() { return zoneEnd; }
    public float getFeedbackTime() { return feedbackRemaining; }
    public Feedback getFeedback() { return feedback; }
    public float getRemaining() { return remaining; }
}
