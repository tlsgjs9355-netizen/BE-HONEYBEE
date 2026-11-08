
package io.jbnu.test;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Disposable;

/**
 * BE HONEYBEE - M6.15
 *
 * Ponk 사마귀 스프라이트 렌더링.
 *
 * 대기, 걷기, 공격 예고, 돌진, 회복 모션 지원.
 *
 * M6.14에서 정상 작동한 방향 매핑을 유지한다.
 */
public final class MantisSprite implements Disposable {

    private static final String ROOT =
        "bosses/mantis/";

    private static final int TILE_SIZE = 32;
    private static final int SOURCE_LEFT_ROW = 1;

    private static final float DRAW_SIZE = 96f;

    // M6.14에서 확인된 방향 매핑
    private static final boolean
        REVERSE_VISUAL_DIRECTION = true;

    private static final float WALK_FRAME_TIME = 0.13f;
    private static final float ATTACK_FRAME_TIME = 0.08f;

    private final Texture[] textures =
        new Texture[3];

    private final TextureRegion[] idle =
        new TextureRegion[2];

    private final TextureRegion[][] movement =
        new TextureRegion[2][4];

    private final TextureRegion[][] attack =
        new TextureRegion[2][3];

    private Enemy.State previousState;
    private boolean previousWalking;

    private float previousAnimationTime = -1f;
    private float stateTime;

    public MantisSprite() {
        try {
            textures[0] = load(
                "Mantis.png", 64, 64
            );

            textures[1] = load(
                "MantisMove.png", 128, 128
            );

            textures[2] = load(
                "MantisAttack.png", 224, 128
            );

            // 기존에 정상 작동한 원본 방향 유지
            idle[0] = frame(
                textures[0], 0, false
            );

            idle[1] = frame(
                textures[0], 0, true
            );

            // 이동 4프레임
            for (int i = 0; i < 4; i++) {
                movement[0][i] = frame(
                    textures[1], i, false
                );

                movement[1][i] = frame(
                    textures[1], i, true
                );
            }

            // 공격 준비 및 시작 3프레임
            for (int i = 0; i < 3; i++) {
                attack[0][i] = frame(
                    textures[2], i, false
                );

                attack[1][i] = frame(
                    textures[2], i, true
                );
            }

            Gdx.app.log(
                "MANTIS",
                "M6.15 WALK ANIMATION LOADED"
            );

        } catch (RuntimeException exception) {
            dispose();
            throw exception;
        }
    }

    private Texture load(
        String name,
        int expectedWidth,
        int expectedHeight
    ) {
        String path = ROOT + name;

        if (!Gdx.files.internal(path).exists()) {
            throw new IllegalStateException(
                "Missing mantis asset: assets/" + path
            );
        }

        Texture texture = new Texture(
            Gdx.files.internal(path)
        );

        if (
            texture.getWidth() != expectedWidth
                || texture.getHeight() != expectedHeight
        ) {
            String error =
                path
                    + " expected "
                    + expectedWidth
                    + "x"
                    + expectedHeight
                    + ", got "
                    + texture.getWidth()
                    + "x"
                    + texture.getHeight();

            texture.dispose();

            throw new IllegalStateException(error);
        }

        texture.setFilter(
            Texture.TextureFilter.Nearest,
            Texture.TextureFilter.Nearest
        );

        return texture;
    }

    /**
     * 원본 스프라이트에서 프레임 선택.
     * 오른쪽 방향은 동일 프레임을 수평 반전.
     */
    private TextureRegion frame(
        Texture texture,
        int column,
        boolean mirrored
    ) {
        TextureRegion region = new TextureRegion(
            texture,
            column * TILE_SIZE,
            SOURCE_LEFT_ROW * TILE_SIZE,
            TILE_SIZE,
            TILE_SIZE
        );

        if (mirrored) {
            region.flip(true, false);
        }

        return region;
    }

    public void draw(
        SpriteBatch batch,
        Mantis boss,
        float animationTime
    ) {
        if (
            boss == null
                || boss.isDead()
        ) {
            return;
        }

        Enemy.State state = boss.getState();
        boolean walking = boss.isWalking();

        // 상태 전환 또는 이동 시작·정지 시
        // 해당 애니메이션을 처음부터 재생
        if (
            state != previousState
                || walking != previousWalking
                || animationTime < previousAnimationTime
        ) {
            stateTime = 0f;

        } else if (previousAnimationTime >= 0f) {
            stateTime += MathUtils.clamp(
                animationTime - previousAnimationTime,
                0f,
                0.05f
            );
        }

        previousState = state;
        previousWalking = walking;
        previousAnimationTime = animationTime;

        // M6.14의 방향 매핑 유지
        boolean movingRight =
            boss.getDirection() > 0;

        boolean visualRight =
            REVERSE_VISUAL_DIRECTION
                ? !movingRight
                : movingRight;

        int index = visualRight ? 1 : 0;

        TextureRegion region;

        switch (state) {

            case IDLE:

                if (walking) {
                    int walkFrame =
                        ((int) (
                            stateTime / WALK_FRAME_TIME
                        )) % 4;

                    region = movement[index][walkFrame];

                } else {
                    region = idle[index];
                }

                break;

            case TELEGRAPH:

                int preparationFrame = Math.min(
                    2,
                    (int) (stateTime / 0.17f)
                );

                region =
                    attack[index][preparationFrame];

                break;

            case ATTACK:

                if (stateTime < 0.24f) {

                    int attackFrame = Math.min(
                        2,
                        (int) (
                            stateTime / ATTACK_FRAME_TIME
                        )
                    );

                    region =
                        attack[index][attackFrame];

                } else {

                    int chargeFrame =
                        ((int) (
                            (stateTime - 0.24f) / 0.10f
                        )) % 4;

                    region =
                        movement[index][chargeFrame];
                }

                break;

            case RECOVER:
            default:

                region = idle[index];
                break;
        }

        // 공격 예고 시 색상 효과
        if (state == Enemy.State.TELEGRAPH) {

            float pulse =
                0.80f
                    + 0.12f * MathUtils.sin(
                    animationTime * 19f
                );

            batch.setColor(
                1f,
                pulse,
                pulse,
                1f
            );

        } else if (
            state == Enemy.State.RECOVER
        ) {
            // 공격 후 회복 상태를 파란빛으로 표시
            batch.setColor(
                0.78f,
                0.88f,
                1f,
                1f
            );

        } else {
            batch.setColor(Color.WHITE);
        }

        float drawX = Math.round(
            boss.centerX() - DRAW_SIZE / 2f
        );

        float drawY = Math.round(
            boss.bounds.y + 4f
        );

        batch.draw(
            region,
            drawX,
            drawY,
            DRAW_SIZE,
            DRAW_SIZE
        );

        batch.setColor(Color.WHITE);
    }

    @Override
    public void dispose() {
        for (Texture texture : textures) {
            if (texture != null) {
                texture.dispose();
            }
        }
    }
}
