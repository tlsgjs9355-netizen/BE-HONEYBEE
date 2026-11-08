
package io.jbnu.test;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

public class GiantHornet extends Enemy {

    public static final int MAX_HP = 200;

    public enum Pattern {
        CHARGE, GRAB, STING;

        @Deprecated
        public static final Pattern BITE = GRAB;
    }

    private static final float LEFT = 1800f;
    private static final float RIGHT = 2600f;
    private static final float BOTTOM = 40f;
    private static final float TOP = 600f;

    private static final float GRAB_APPROACH_RANGE = 260f;
    private static final float GRAB_APPROACH_SPEED = 310f;
    private static final float GRAB_CONTACT_PROGRESS = .66f;

    public static final class Sting {
        public final Rectangle bounds;
        public final float vx, vy;
        public float age;

        private Sting(
            float x, float y, float vx, float vy
        ) {
            bounds = new Rectangle(
                x - 8f, y - 4f, 16f, 8f
            );
            this.vx = vx;
            this.vy = vy;
        }
    }

    private final Array<Sting> stings = new Array<>();
    private final Rectangle grabBounds = new Rectangle();
    private final Vector2 attackStart = new Vector2();
    private final Vector2 target = new Vector2();
    private final Vector2 heading = new Vector2(-1f, 0f);

    private Pattern pattern = Pattern.CHARGE;

    private int patternIndex;
    private int remainingCharges;
    private int facing = -1;

    private boolean phaseTwo;
    private boolean hitRegistered;
    private boolean volleyFired;

    private float volleyAngle;
    private float warningTime;
    private float attackTime;
    private float recoveryTime;

    public GiantHornet() {
        super(88f, 56f, MAX_HP);
        reset();
    }

    @Override
    public void reset() {
        resetState();

        bounds.setPosition(
            2390f - bounds.width / 2f,
            292f
        );

        pattern = Pattern.CHARGE;
        patternIndex = 0;
        remainingCharges = 0;
        facing = -1;

        phaseTwo = false;
        hitRegistered = false;
        volleyFired = false;

        volleyAngle = 0f;
        warningTime = .95f;
        attackTime = .6f;
        recoveryTime = .75f;

        stings.clear();
        grabBounds.set(0f, 0f, 0f, 0f);

        attackStart.set(centerX(), centerY());
        target.set(attackStart);
        heading.set(-1f, 0f);
    }

    @Override
    public void update(float dt, Player player) {
        if (isDead() || dt <= 0f) return;

        timer += dt;
        updateStings(dt);

        switch (state) {
            case IDLE:
                phaseTwo = hp <= MAX_HP / 2;

                float desiredY = MathUtils.clamp(
                    player.bounds.y
                        + player.bounds.height / 2f,
                    105f,
                    TOP - 80f
                );

                float beeX = player.bounds.x
                    + player.bounds.width * .5f;

                float beeY = player.bounds.y
                    + player.bounds.height * .5f;

                // 붙잡기 패턴 직전에는 거리를 좁힌다.
                // 이 접근 이동에는 포획 피해 판정이 없다.
                if (Pattern.values()[
                    patternIndex % Pattern.values().length
                    ] == Pattern.GRAB) {

                    float vx = beeX - centerX();
                    float vy = beeY - centerY();

                    float gap = (float)Math.sqrt(
                        vx * vx + vy * vy
                    );

                    if (gap > GRAB_APPROACH_RANGE) {
                        float step = Math.min(
                            gap - GRAB_APPROACH_RANGE,
                            GRAB_APPROACH_SPEED * dt
                        );

                        setCenter(
                            centerX() + vx / gap * step,
                            centerY() + vy / gap * step
                        );
                    }

                } else {
                    setCenter(
                        centerX(),
                        centerY() + MathUtils.clamp(
                            desiredY - centerY(),
                            -160f * dt,
                            160f * dt
                        )
                    );
                }

                facing = beeX < centerX() ? -1 : 1;

                if (timer >= (phaseTwo ? .44f : .60f)) {
                    startPattern(player);
                }
                break;

            case TELEGRAPH:
                // 예고가 시작되면 공격 경로는 고정된다.
                if (timer >= warningTime) {
                    hitRegistered = false;
                    volleyFired = false;
                    changeState(State.ATTACK);
                }
                break;

            case ATTACK:
                float p = MathUtils.clamp(
                    timer / attackTime,
                    0f, 1f
                );

                if (pattern == Pattern.STING) {
                    if (!volleyFired && timer >= .30f) {
                        volleyFired = true;
                        fireVolley();
                    }

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

                    if (pattern == Pattern.GRAB) {
                        setGrabBounds();
                    }
                }

                if (p >= 1f) {
                    finishAttack(player);
                }
                break;

            case RECOVER:
                if (timer >= recoveryTime) {
                    changeState(State.IDLE);
                }
                break;
        }
    }

    private void startPattern(Player player) {
        phaseTwo = hp <= MAX_HP / 2;

        pattern = Pattern.values()[
            patternIndex % Pattern.values().length
            ];

        remainingCharges =
            pattern == Pattern.CHARGE
                ? (phaseTwo ? 2 : 1)
                : 0;

        aim(player);
    }

    private void aim(Player player) {
        attackStart.set(centerX(), centerY());

        float px = player.bounds.x
            + player.bounds.width / 2f;

        float py = player.bounds.y
            + player.bounds.height / 2f;

        facing = px < centerX() ? -1 : 1;

        grabBounds.set(0f, 0f, 0f, 0f);
        hitRegistered = false;
        volleyFired = false;

        if (pattern == Pattern.STING) {
            volleyAngle =
                patternIndex % 2 == 0 ? 0f : .22f;

            target.set(attackStart);
            heading.set(facing, 0f);

            warningTime = phaseTwo ? .90f : 1.06f;
            attackTime = phaseTwo ? .83f : .95f;

        } else {
            float dx = px - attackStart.x;
            float dy = py - attackStart.y;

            float length = (float)Math.sqrt(
                dx * dx + dy * dy
            );

            if (length < .001f) {
                dx = facing;
                dy = 0f;
                length = 1f;
            }

            // 꿀벌이 공격 시간의 약 66% 시점에
            // 포획 영역과 만나도록 목표 거리를 설정한다.
            float distance =
                pattern == Pattern.GRAB
                    ? MathUtils.clamp(
                    length / GRAB_CONTACT_PROGRESS,
                    100f, 820f
                )
                    : MathUtils.clamp(
                    length + 185f,
                    245f, 820f
                );

            target.set(
                MathUtils.clamp(
                    attackStart.x
                        + dx / length * distance,
                    LEFT + bounds.width / 2f,
                    RIGHT - bounds.width / 2f
                ),
                MathUtils.clamp(
                    attackStart.y
                        + dy / length * distance,
                    BOTTOM + bounds.height / 2f,
                    TOP - bounds.height / 2f
                )
            );

            heading.set(
                target.x - attackStart.x,
                target.y - attackStart.y
            );

            float travel = heading.len();

            if (travel > .001f) {
                heading.scl(1f / travel);
            } else {
                heading.set(facing, 0f);
            }

            if (pattern == Pattern.GRAB) {
                warningTime = phaseTwo ? .53f : .70f;

                attackTime = MathUtils.clamp(
                    travel / (phaseTwo ? 1260f : 990f),
                    .23f, .84f
                );

            } else {
                boolean followUp =
                    phaseTwo && remainingCharges == 1;

                warningTime = followUp
                    ? .46f
                    : (phaseTwo ? .73f : .92f);

                attackTime = MathUtils.clamp(
                    travel / (phaseTwo ? 1030f : 830f),
                    .27f, 1.02f
                );
            }
        }

        changeState(State.TELEGRAPH);
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

    private void setGrabBounds() {
        // 포획 영역을 실제 돌진 방향에 맞춰 배치한다.
        // 위아래로 날아 있는 꿀벌도 붙잡을 수 있다.
        float armCenterX =
            centerX() + heading.x * 34f;

        float armCenterY =
            centerY() + heading.y * 34f;

        grabBounds.set(
            armCenterX - 34f,
            armCenterY - 29f,
            68f,
            58f
        );
    }

    private void fireVolley() {
        int count = getStingCount();
        float speed = phaseTwo ? 385f : 315f;

        for (int i = 0; i < count; i++) {
            float angle = volleyAngle
                + MathUtils.PI2 * i / count;

            float dx = MathUtils.cos(angle);
            float dy = MathUtils.sin(angle);

            stings.add(
                new Sting(
                    centerX() + dx * 56f,
                    centerY() + dy * 56f,
                    dx * speed,
                    dy * speed
                )
            );
        }
    }

    private void updateStings(float dt) {
        for (int i = stings.size - 1; i >= 0; i--) {
            Sting s = stings.get(i);

            s.age += dt;
            s.bounds.x += s.vx * dt;
            s.bounds.y += s.vy * dt;

            if (s.age > 1.75f
                || s.bounds.x < LEFT - 24f
                || s.bounds.x > RIGHT + 24f
                || s.bounds.y < BOTTOM - 24f
                || s.bounds.y > TOP + 24f) {

                stings.removeIndex(i);
            }
        }
    }

    private void finishAttack(Player player) {
        grabBounds.set(0f, 0f, 0f, 0f);

        if (pattern == Pattern.CHARGE
            && remainingCharges > 1) {

            remainingCharges--;

            // 2페이즈 연속 돌진:
            // 두 번째 공격에 앞서 위치 재조준 + 새 경고
            aim(player);
            return;
        }

        remainingCharges = 0;
        patternIndex++;

        recoveryTime =
            pattern == Pattern.GRAB
                ? (phaseTwo ? .70f : .85f)
                : pattern == Pattern.STING
                ? (phaseTwo ? .68f : .80f)
                : (phaseTwo ? .65f : .78f);

        changeState(State.RECOVER);
    }

    public void onCapture() {
        hitRegistered = true;
        remainingCharges = 0;

        grabBounds.set(0f, 0f, 0f, 0f);

        patternIndex++;
        recoveryTime = .85f;

        changeState(State.RECOVER);
    }

    public void interruptCombo() {
        remainingCharges = 0;
        stings.clear();

        grabBounds.set(0f, 0f, 0f, 0f);

        if (state == State.ATTACK
            || state == State.TELEGRAPH) {
            patternIndex++;
        }

        recoveryTime = .85f;
        changeState(State.RECOVER);
    }

    @Override
    public void forceRecover(float elapsed) {
        interruptCombo();

        timer = MathUtils.clamp(
            elapsed, 0f, recoveryTime
        );
    }

    public boolean isChargeDangerous() {
        return state == State.ATTACK
            && pattern == Pattern.CHARGE
            && !hitRegistered
            && timer >= .06f
            && timer <= attackTime;
    }

    public boolean isGrabDangerous() {
        // 양팔을 벌리는 준비 구간에는 포획하지 않는다.
        // 닫히는 구간에만 판정이 활성화된다.
        return state == State.ATTACK
            && pattern == Pattern.GRAB
            && !hitRegistered
            && timer >= attackTime * .45f
            && timer <= attackTime * .94f;
    }

    @Deprecated
    public boolean isBiteDangerous() {
        return isGrabDangerous();
    }

    public void markHit() {
        hitRegistered = true;
    }

    public Rectangle getGrabBounds() {
        return grabBounds;
    }

    @Deprecated
    public Rectangle getBiteBounds() {
        return getGrabBounds();
    }

    public Array<Sting> getStings() {
        return stings;
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
        return heading;
    }

    public int getFacing() {
        return facing;
    }

    public int getStingCount() {
        return phaseTwo ? 14 : 10;
    }

    public float getVolleyAngle() {
        return volleyAngle;
    }

    public float getAttackProgress() {
        return state == State.ATTACK && attackTime > 0f
            ? MathUtils.clamp(
            timer / attackTime,
            0f, 1f
        )
            : 0f;
    }

    public float getWarningProgress() {
        return state == State.TELEGRAPH && warningTime > 0f
            ? MathUtils.clamp(
            timer / warningTime,
            0f, 1f
        )
            : 0f;
    }

    public boolean isPhaseTwo() {
        return hp <= MAX_HP / 2;
    }

    public int getRemainingCharges() {
        return remainingCharges;
    }
}
