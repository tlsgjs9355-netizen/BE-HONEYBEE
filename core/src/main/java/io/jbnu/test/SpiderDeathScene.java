
package io.jbnu.test;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Disposable;

public class SpiderDeathScene implements Disposable {

    private static final float WIDTH = 800f;
    private static final float HEIGHT = 600f;

    // 컷신 전체 시간
    private static final float TOTAL_DURATION = 2.8f;

    // 애니메이션 구간
    private static final float GRAB_DURATION = 0.45f;
    private static final float PULL_END = 1.85f;
    private static final float FADE_START = 1.65f;
    private static final float FADE_END = 2.55f;

    private final GameAssets assets;
    private final SpriteBatch batch;
    private final BitmapFont font;
    private final OrthographicCamera camera;
    private final Texture pixel;

    private float elapsed;
    private boolean active;

    public SpiderDeathScene(GameAssets assets) {
        this.assets = assets;

        batch = new SpriteBatch();
        font = new BitmapFont();

        camera = new OrthographicCamera();
        camera.setToOrtho(false, WIDTH, HEIGHT);
        camera.update();

        // 단색 배경과 암전에 사용하는 1x1 텍스처
        Pixmap pixmap = new Pixmap(
            1, 1, Pixmap.Format.RGBA8888
        );

        pixmap.setColor(Color.WHITE);
        pixmap.fill();

        pixel = new Texture(pixmap);
        pixmap.dispose();

        elapsed = 0f;
        active = false;
    }

    public void start() {
        elapsed = 0f;
        active = true;
    }

    public void update(float dt) {
        if (!active) return;

        elapsed += dt;

        if (elapsed >= TOTAL_DURATION) {
            elapsed = TOTAL_DURATION;
            active = false;
        }
    }

    public boolean isActive() {
        return active;
    }

    public void render() {
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        // 컷신 중 원래 게임 월드를 완전히 가린다.
        batch.setColor(0.12f, 0.11f, 0.19f, 1f);
        batch.draw(pixel, 0f, 0f, WIDTH, HEIGHT);

        drawWebBackground();
        drawSpiderAndBee();
        drawText();
        drawFade();

        batch.setColor(Color.WHITE);
        batch.end();
    }

    private void drawWebBackground() {
        batch.setColor(0.33f, 0.30f, 0.43f, 1f);

        // 천장
        batch.draw(pixel, 0f, 555f, WIDTH, 45f);

        // 거미줄 세로줄
        for (int i = 0; i < 9; i++) {
            float x = 80f + i * 80f;

            batch.setColor(
                0.43f, 0.45f, 0.56f, 0.55f
            );

            batch.draw(
                pixel,
                x,
                180f,
                2f,
                375f
            );
        }

        // 아래쪽 장식
        batch.setColor(0.23f, 0.21f, 0.31f, 1f);
        batch.draw(pixel, 0f, 0f, WIDTH, 100f);

        batch.setColor(Color.WHITE);
    }

    private void drawSpiderAndBee() {
        float progress = MathUtils.clamp(
            (elapsed - GRAB_DURATION)
                / (PULL_END - GRAB_DURATION),
            0f,
            1f
        );

        // 처음에는 붙잡힌 모습을 보여주고
        // 이후 천천히 천장으로 끌어올린다.
        float eased = progress * progress
            * (3f - 2f * progress);

        float pullY = eased * 245f;

        float spiderCenterX = WIDTH / 2f;
        float spiderCenterY = 280f + pullY;

        float spiderW = 104f;
        float spiderH = 94f;

        float spiderX = spiderCenterX - spiderW / 2f;
        float spiderY = spiderCenterY - spiderH / 2f;

        // 천장부터 거미까지 이어지는 실
        batch.setColor(0.77f, 0.82f, 0.91f, 1f);

        float silkTop = 555f;
        float silkBottom = spiderY + spiderH * 0.7f;

        if (silkBottom < silkTop) {
            batch.draw(
                pixel,
                spiderCenterX - 1.5f,
                silkBottom,
                3f,
                silkTop - silkBottom
            );
        }

        // 붙잡힌 꿀벌을 먼저 그린다.
        float beeW = 54f;
        float beeH = 45f;

        float struggle = MathUtils.sin(
            elapsed * 24f
        ) * (1f - eased) * 5f;

        float beeX = spiderCenterX
            - beeW / 2f + struggle;

        float beeY = spiderCenterY
            - 60f
            + MathUtils.sin(elapsed * 18f)
            * (1f - eased) * 2f;

        TextureRegion beeFrame =
            assets.beeAnimation.getKeyFrame(elapsed);

        batch.setColor(Color.WHITE);

        batch.draw(
            beeFrame,
            beeX,
            beeY,
            beeW,
            beeH
        );

        // 꿀벌을 감싸는 거미줄
        float silkAlpha = MathUtils.clamp(
            0.50f + elapsed * 0.15f,
            0.50f,
            0.95f
        );

        batch.setColor(
            0.86f, 0.91f, 0.97f, silkAlpha
        );

        for (int i = 0; i < 4; i++) {
            float y = beeY + 8f + i * 9f;

            batch.draw(
                pixel,
                beeX - 4f,
                y,
                beeW + 8f,
                2f
            );
        }

        // 포식자 몸체는 가장 앞쪽에 표시
        batch.setColor(Color.WHITE);

        batch.draw(
            assets.spider,
            spiderX,
            spiderY,
            spiderW,
            spiderH
        );

        // 포획이 강화되는 연출
        if (elapsed > GRAB_DURATION) {
            float clampWidth = 8f + eased * 10f;

            batch.setColor(0.16f, 0.12f, 0.22f, 1f);

            batch.draw(
                pixel,
                beeX - 7f,
                beeY + 10f,
                clampWidth,
                7f
            );

            batch.draw(
                pixel,
                beeX + beeW - clampWidth + 7f,
                beeY + 10f,
                clampWidth,
                7f
            );
        }

        batch.setColor(Color.WHITE);
    }

    private void drawText() {
        font.setColor(Color.WHITE);

        font.getData().setScale(1.6f);

        if (elapsed < 1.7f) {
            font.draw(
                batch,
                "CAUGHT BY THE SPIDER!",
                255f,
                500f
            );
        } else {
            font.draw(
                batch,
                "GAME OVER",
                325f,
                440f
            );
        }

        font.getData().setScale(1f);

        if (elapsed > 1.3f) {
            font.draw(
                batch,
                "The worker bee could not escape...",
                284f,
                145f
            );
        }

        font.setColor(Color.WHITE);
    }

    private void drawFade() {
        if (elapsed < FADE_START) return;

        float alpha = MathUtils.clamp(
            (elapsed - FADE_START)
                / (FADE_END - FADE_START),
            0f,
            1f
        );

        batch.setColor(0f, 0f, 0f, alpha);
        batch.draw(pixel, 0f, 0f, WIDTH, HEIGHT);
        batch.setColor(Color.WHITE);
    }

    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
        pixel.dispose();
    }
}
