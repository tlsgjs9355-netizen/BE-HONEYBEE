
package io.jbnu.test;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Disposable;

/**
 * High Forest 'Small Bee' player graphics.
 * Physics, health, attacks, and capture logic stay in Player / World.
 */
public final class BeeSprite implements Disposable {

    private static final String ROOT = "sprites/SmallBee/";
    private static final int CELL = 64;
    private static final int FRAME_COUNT = 4;

    // 원본 64x64에서 실제 픽셀이 있는 영역을 사용
    private static final int CROP_X = 16;
    private static final int CROP_Y = 0;
    private static final int CROP_SIZE = 48;
    private static final float DRAW_SIZE = 48f;

    private static final float FLY_FRAME_TIME = 0.105f;
    private static final float WALK_FRAME_TIME = 0.145f;
    private static final float HIT_FRAME_TIME = 0.12f;
    private static final float ATTACK_DURATION = 0.28f;

    private final Texture[] textures = new Texture[3];

    private final TextureRegion[] flyRight;
    private final TextureRegion[] flyLeft;
    private final TextureRegion[] attackRight;
    private final TextureRegion[] attackLeft;
    private final TextureRegion[] hitRight;
    private final TextureRegion[] hitLeft;

    private Projectile lastObservedShot;
    private float attackRemaining;
    private float previousAnimationTime = -1f;

    public BeeSprite() {
        try {
            textures[0] = load("Fly-Sheet.png");
            textures[1] = load("Attack-Sheet.png");
            textures[2] = load("Hit-Sheet.png");

            // 원본 이미지는 왼쪽을 바라봄
            flyLeft = makeFrames(textures[0], false);
            flyRight = makeFrames(textures[0], true);

            attackLeft = makeFrames(textures[1], false);
            attackRight = makeFrames(textures[1], true);

            hitLeft = makeFrames(textures[2], false);
            hitRight = makeFrames(textures[2], true);

        } catch (RuntimeException error) {
            dispose();
            throw error;
        }
    }

    private static Texture load(String filename) {
        String path = ROOT + filename;
        FileHandle file = Gdx.files.internal(path);

        if (!file.exists()) {
            throw new IllegalStateException(
                "Small Bee 이미지가 없습니다: assets/" + path
            );
        }

        Texture texture = new Texture(file);

        if (texture.getWidth() != CELL * FRAME_COUNT
            || texture.getHeight() != CELL) {

            int width = texture.getWidth();
            int height = texture.getHeight();
            texture.dispose();

            throw new IllegalStateException(
                "Small Bee 이미지 크기 오류: " + path
                    + " (" + width + "x" + height
                    + ", 필요한 크기 256x64)"
            );
        }

        texture.setFilter(
            Texture.TextureFilter.Nearest,
            Texture.TextureFilter.Nearest
        );

        return texture;
    }

    private static TextureRegion[] makeFrames(
        Texture texture,
        boolean flipped
    ) {
        TextureRegion[] frames =
            new TextureRegion[FRAME_COUNT];

        for (int i = 0; i < FRAME_COUNT; i++) {
            TextureRegion region = new TextureRegion(
                texture,
                i * CELL + CROP_X,
                CROP_Y,
                CROP_SIZE,
                CROP_SIZE
            );

            if (flipped) {
                region.flip(true, false);
            }

            frames[i] = region;
        }

        return frames;
    }

    /**
     * 기존 독침 발사체 생성 여부를 감지한다.
     * 실제 공격과 피해 계산은 World에서 수행한다.
     */
    private void updateAnimation(
        World world,
        float time
    ) {
        float dt = 0f;

        if (previousAnimationTime >= 0f) {
            if (time < previousAnimationTime) {
                lastObservedShot = null;
                attackRemaining = 0f;

            } else {
                dt = Math.min(
                    1f / 20f,
                    time - previousAnimationTime
                );
            }
        }

        previousAnimationTime = time;

        attackRemaining = Math.max(
            0f,
            attackRemaining - dt
        );

        if (world.isCaptured()
            || world.player.isDashing()) {
            return;
        }

        Rectangle player = world.player.bounds;

        float px = player.x + player.width * 0.5f;
        float py = player.y + player.height * 0.5f;

        for (Projectile shot : world.projectiles) {
            if (shot == lastObservedShot) {
                continue;
            }

            float sx = shot.bounds.x
                + shot.bounds.width * 0.5f;

            float sy = shot.bounds.y
                + shot.bounds.height * 0.5f;

            if (Math.abs(sx - px) < 90f
                && Math.abs(sy - py) < 42f) {

                lastObservedShot = shot;
                attackRemaining = ATTACK_DURATION;
                break;
            }
        }
    }

    private static TextureRegion select(
        TextureRegion[] frames,
        float time,
        float frameTime
    ) {
        int index = (
            (int) (Math.max(0f, time) / frameTime)
        ) % frames.length;

        return frames[index];
    }

    /**
     * GameRenderer에서 호출하는 기존 인터페이스 유지
     */
    public void draw(
        SpriteBatch batch,
        World world,
        float animationTime,
        SpiderTrap capturingSpider
    ) {
        updateAnimation(world, animationTime);

        if (
            world.isPlayerInvincible()
                && !world.isCaptured()
                && ((int) (animationTime * 12f) % 2 == 0)
        ) {
            return;
        }

        boolean right = world.player.facingRight;
        boolean captured = world.isCaptured();
        boolean dashing = world.player.isDashing();

        TextureRegion region;

        // 거미에게 붙잡히면 피격 애니메이션
        if (captured) {
            region = select(
                right ? hitRight : hitLeft,
                animationTime,
                HIT_FRAME_TIME
            );

            // 독침 발사 모션
        } else if (
            attackRemaining > 0f && !dashing
        ) {
            TextureRegion[] frames =
                right ? attackRight : attackLeft;

            float progress = 1f
                - attackRemaining / ATTACK_DURATION;

            int frame = Math.min(
                frames.length - 1,
                Math.max(
                    0,
                    (int) (progress * frames.length)
                )
            );

            region = frames[frame];

            // 대시 또는 공중 비행
        } else if (
            dashing || !world.player.grounded
        ) {
            region = select(
                right ? flyRight : flyLeft,
                animationTime,
                FLY_FRAME_TIME
            );

            // 지상 이동
        } else if (
            Math.abs(world.player.velocityX) > 1f
        ) {
            region = select(
                right ? flyRight : flyLeft,
                animationTime,
                WALK_FRAME_TIME
            );

            // 대기
        } else {
            region = right
                ? flyRight[0]
                : flyLeft[0];
        }

        Rectangle bounds = world.player.bounds;

        float cx = bounds.x
            + bounds.width * 0.5f;

        float cy = bounds.y
            + bounds.height * 0.5f + 10f;

        // 기존 거미 포획 시 꿀벌 위치 유지
        if (
            captured && capturingSpider != null
        ) {
            Rectangle spider = capturingSpider.bounds;

            cx = spider.x
                + spider.width * 0.5f
                + (float) Math.sin(
                animationTime * 22f
            ) * 4f;

            cy = spider.y
                + spider.height * 0.5f - 11f
                + (float) Math.sin(
                animationTime * 16f
            ) * 2f;
        }

        // 상태별 색상
        if (captured) {
            batch.setColor(
                1f, 0.88f, 0.91f, 1f
            );

        } else if (dashing) {
            batch.setColor(
                1f, 0.88f, 0.45f, 1f
            );

        } else {
            batch.setColor(Color.WHITE);
        }

        // 픽셀 단위 정렬
        batch.draw(
            region,
            Math.round(cx - DRAW_SIZE * 0.5f),
            Math.round(cy - DRAW_SIZE * 0.5f),
            DRAW_SIZE,
            DRAW_SIZE
        );

        batch.setColor(Color.WHITE);
    }

    @Override
    public void dispose() {
        for (int i = 0; i < textures.length; i++) {
            if (textures[i] != null) {
                textures[i].dispose();
                textures[i] = null;
            }
        }
    }
}
