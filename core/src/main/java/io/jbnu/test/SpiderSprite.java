
package io.jbnu.test;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;

/**
 * Spider rendering.
 * Thread is drawn behind the spider and connects to its body.
 * Movement, collision and capture remain unchanged.
 */
public class SpiderSprite implements Disposable {

    private static final int CELL = 32;
    private static final int FRAME_COUNT = 4;
    private static final float FRAME_TIME = 0.16f;
    private static final float SIZE = 106f;

    // 거미 이미지 내부의 투명 여백을 고려한 연결 위치
    private static final float BODY_ATTACHMENT_OFFSET =
        SIZE * 0.44f;

    // 천장 거미줄 장식과 겹치게 연결
    private static final float WEB_OVERLAP = 15f;

    private final Texture texture;
    private final Texture stringPixel;

    private final TextureRegion[] walkingFrames;
    private final TextureRegion climbingFrame;

    public SpiderSprite() {
        String[] candidates = {
            "sprites/spider_sheet.png",
            "sprites/Spider Sprite Sheet.png",
            "Spider Sprite Sheet.png",
            "sprites/spider_black.png"
        };

        FileHandle selected = null;

        for (String path : candidates) {
            FileHandle file = Gdx.files.internal(path);

            if (file.exists()) {
                selected = file;
                break;
            }
        }

        if (selected == null) {
            throw new IllegalStateException(
                "Spider texture not found in assets/sprites/"
            );
        }

        texture = new Texture(selected);

        texture.setFilter(
            Texture.TextureFilter.Nearest,
            Texture.TextureFilter.Nearest
        );

        if (
            texture.getWidth() >= CELL * FRAME_COUNT
                && texture.getHeight() >= CELL
        ) {
            walkingFrames = new TextureRegion[FRAME_COUNT];

            for (int i = 0; i < FRAME_COUNT; i++) {
                walkingFrames[i] = new TextureRegion(
                    texture,
                    i * CELL,
                    0,
                    CELL,
                    CELL
                );
            }

        } else {
            walkingFrames = new TextureRegion[] {
                new TextureRegion(texture)
            };
        }

        // 기존 상승 애니메이션 유지
        climbingFrame =
            texture.getWidth() >= CELL
                && texture.getHeight() >= CELL * 4
                ? new TextureRegion(
                texture,
                0,
                CELL * 3,
                CELL,
                CELL
            )
                : null;

        Pixmap p = new Pixmap(
            1,
            1,
            Pixmap.Format.RGBA8888
        );

        p.setColor(Color.WHITE);
        p.fill();

        stringPixel = new Texture(p);

        stringPixel.setFilter(
            Texture.TextureFilter.Nearest,
            Texture.TextureFilter.Nearest
        );

        p.dispose();
    }

    public void draw(
        SpriteBatch batch,
        SpiderTrap spider,
        float animationTime,
        boolean captured
    ) {
        boolean ascending = spider.isMovingUp();

        int tick = Math.max(
            0,
            (int) (animationTime / FRAME_TIME)
        );

        TextureRegion region;

        if (ascending && climbingFrame != null) {
            region = climbingFrame;

        } else {
            int frame = spider.isStunned()
                ? 0
                : tick % walkingFrames.length;

            region = walkingFrames[frame];
        }

        float cx = spider.bounds.x
            + spider.bounds.width / 2f;

        float cy = spider.bounds.y
            + spider.bounds.height / 2f;

        // =========================================
        // 거미줄 연결
        // =========================================
        // 기존 cy + 3f에서 끊기던 줄을
        // 실제 거미 이미지의 몸체 안쪽까지 연장한다.
        //
        // 거미줄을 먼저 그리고, 그 위에 거미 이미지를
        // 그리므로 끝부분이 자연스럽게 가려진다.

        float threadBottom = Math.round(
            cy - BODY_ATTACHMENT_OFFSET
        );

        float threadTop = Math.round(
            spider.getAnchorY() + WEB_OVERLAP
        );

        float height = threadTop - threadBottom;

        if (height > 0f) {
            batch.setColor(
                0.82f,
                0.89f,
                0.96f,
                0.95f
            );

            batch.draw(
                stringPixel,
                Math.round(cx) - 1f,
                threadBottom,
                2f,
                height
            );
        }

        // =========================================
        // 거미 상태별 색상
        // =========================================
        if (spider.isStunned()) {
            batch.setColor(
                0.65f, 0.75f, 0.95f, 0.70f
            );

        } else if (captured) {
            batch.setColor(
                1f, 0.55f, 0.55f, 1f
            );

        } else {
            batch.setColor(Color.WHITE);
        }

        // =========================================
        // 기존 이동 방향 및 애니메이션 유지
        // =========================================
        float facing = ascending
            ? -1f
            : spider.isDescendingFacingLeft()
            ? -1f
            : 1f;

        // 거미줄 위에 거미 몸체 출력
        batch.draw(
            region,
            Math.round(cx - SIZE / 2f),
            Math.round(cy - SIZE / 2f),
            SIZE / 2f,
            SIZE / 2f,
            SIZE,
            SIZE,
            facing,
            1f,
            0f
        );

        batch.setColor(Color.WHITE);
    }

    @Override
    public void dispose() {
        stringPixel.dispose();
        texture.dispose();
    }
}
