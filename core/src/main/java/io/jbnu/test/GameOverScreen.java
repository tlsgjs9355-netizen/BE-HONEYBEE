package io.jbnu.test;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.math.RandomXS128;

public class GameOverScreen implements Disposable {

    private static final float WIDTH = 800f;
    private static final float HEIGHT = 600f;

    private static final String[] MESSAGES = {
        "일어나세요!",
        "왕국을 지켜야만 해요!",
        "여기서 쓰러질 순 없어요!",
        "다시...다시 한 번...!",
        "포기할 수 없어요!"
    };

    private static final Rectangle RESTART_BUTTON =
        new Rectangle(230f, 210f, 340f, 62f);

    private final SpriteBatch batch = new SpriteBatch();
    private final OrthographicCamera camera = new OrthographicCamera();
    private final GalmuriFonts font = GalmuriFonts.shared();
    private final Texture pixel;
    private final RandomXS128 random = new RandomXS128();

    private String currentMessage = MESSAGES[0];

    public GameOverScreen() {
        camera.setToOrtho(false, WIDTH, HEIGHT);
        camera.update();

        Pixmap p = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        p.setColor(Color.WHITE);
        p.fill();

        pixel = new Texture(p);
        pixel.setFilter(
            Texture.TextureFilter.Nearest,
            Texture.TextureFilter.Nearest
        );
        p.dispose();
    }

    public void showRandomMessage() {
        currentMessage = MESSAGES[random.nextInt(MESSAGES.length)];
    }

    public boolean isRestartButtonPressed(float x, float y) {
        return RESTART_BUTTON.contains(x, y);
    }

    public void render() {
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        // 배경
        fillRect(0f, 0f, WIDTH, HEIGHT, Color.BLACK);

        // 게임 오버 제목은 제거
        font.center(
            batch,
            currentMessage,
            WIDTH / 2f,
            345f,
            20,
            Color.WHITE
        );

        // 버튼: 흰 테두리 + 흰 글씨만
        drawOutlineRect(
            RESTART_BUTTON.x,
            RESTART_BUTTON.y,
            RESTART_BUTTON.width,
            RESTART_BUTTON.height,
            3f,
            Color.WHITE
        );

        font.center(
            batch,
            "ENTER 다시 도전",
            WIDTH / 2f,
            RESTART_BUTTON.y + 39f,
            20,
            Color.WHITE
        );

        batch.end();
    }

    private void fillRect(
        float x,
        float y,
        float w,
        float h,
        Color color
    ) {
        batch.setColor(color);
        batch.draw(pixel, x, y, w, h);
        batch.setColor(Color.WHITE);
    }

    private void drawOutlineRect(
        float x,
        float y,
        float w,
        float h,
        float thickness,
        Color color
    ) {
        batch.setColor(color);

        // 아래
        batch.draw(pixel, x, y, w, thickness);
        // 위
        batch.draw(pixel, x, y + h - thickness, w, thickness);
        // 왼쪽
        batch.draw(pixel, x, y, thickness, h);
        // 오른쪽
        batch.draw(pixel, x + w - thickness, y, thickness, h);

        batch.setColor(Color.WHITE);
    }

    @Override
    public void dispose() {
        batch.dispose();
        pixel.dispose();
    }
}
