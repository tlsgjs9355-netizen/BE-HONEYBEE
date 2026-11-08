
package io.jbnu.test;

import com.badlogic.gdx.math.Rectangle;

public abstract class Enemy {

    public enum State {
        IDLE,
        TELEGRAPH,
        ATTACK,
        RECOVER
    }

    // 회복 상태에서 추가로 적용할 피해 배율
    private static final float RECOVER_DAMAGE_MULTIPLIER = 1.5f;

    public final Rectangle bounds;

    protected final int maxHp;
    protected int hp;

    protected State state;
    protected float timer;

    protected Enemy(float width, float height, int maxHp) {
        bounds = new Rectangle(0f, 0f, width, height);
        this.maxHp = maxHp;

        resetState();
    }

    protected void resetState() {
        hp = maxHp;
        state = State.IDLE;
        timer = 0f;
    }

    protected void changeState(State next) {
        state = next;
        timer = 0f;
    }

    public abstract void reset();

    public abstract void update(float dt, Player player);

    // 현재 보스가 회복 상태인지 확인
    public boolean isVulnerable() {
        return !isDead() && state == State.RECOVER;
    }

    // 일반 공격은 모든 살아 있는 보스에게 적용 가능
    public boolean takeDamage(int damage) {
        if (isDead() || damage <= 0) {
            return false;
        }

        int actualDamage = damage;

        // 회복 상태라면 1.5배 피해
        if (isVulnerable()) {
            actualDamage = Math.round(
                damage * RECOVER_DAMAGE_MULTIPLIER
            );
        }

        return applyDamage(actualDamage);
    }

    // 봉구 피해에는 회복 보너스를 중복 적용하지 않음
    public boolean takeSwarmDamage(int damage) {
        return applyDamage(damage);
    }

    private boolean applyDamage(int damage) {
        if (isDead() || damage <= 0) {
            return false;
        }

        hp = Math.max(0, hp - damage);
        return true;
    }

    public boolean isDead() {
        return hp <= 0;
    }

    public int getHp() {
        return hp;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public State getState() {
        return state;
    }

    public float centerX() {
        return bounds.x + bounds.width / 2f;
    }

    public float centerY() {
        return bounds.y + bounds.height / 2f;
    }

    // 봉구 종료 후 보스를 회복 상태로 전환
    public void forceRecover(float elapsed) {
        if (!isDead()) {
            changeState(State.RECOVER);
            timer = elapsed;
        }
    }
}
