
package io.jbnu.test;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Disposable;

/** Korean Galmuri title, story, transition and gameplay tutorial UI. */
public final class GameMenu implements Disposable {
    private static final float WIDTH = 800f;
    private static final float HEIGHT = 600f;
    private static final Color GOLD = new Color(.98f, .79f, .35f, 1f);
    private static final Color INK = new Color(.20f, .17f, .15f, 1f);
    private static final Color CREAM = new Color(.99f, .95f, .82f, 1f);
    private static final String[] FIRST_STORY = {
        "당신은 이제 갓 외역봉이 된 꿀벌입니다.",
        "이제 벌통에서 나와 직접 꽃을 찾아다녀 꿀을 모아야 합니다.",
        "다만...곳곳에 위험이 도사리고 있다는 걸 명심하세요."
    };

    private final GameAssets assets;
    private final SpriteBatch batch = new SpriteBatch();
    private final OrthographicCamera camera = new OrthographicCamera();
    private final GalmuriFonts fonts = GalmuriFonts.shared();
    private final Texture pixel;
    private float animationTime;

    public GameMenu(GameAssets assets) {
        this.assets = assets;
        camera.setToOrtho(false, WIDTH, HEIGHT);
        camera.update();
        Pixmap p = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        p.setColor(Color.WHITE);
        p.fill();
        pixel = new Texture(p);
        pixel.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        p.dispose();
    }

    public void update(float delta) {
        animationTime += Math.min(delta, .05f);
    }

    private void rect(float x, float y, float w, float h, Color color) {
        batch.setColor(color);
        batch.draw(pixel, x, y, w, h);
        batch.setColor(Color.WHITE);
    }

    private void scenery() {
        rect(0f, 0f, WIDTH, HEIGHT, new Color(.69f, .84f, .89f, 1f));
        rect(0f, 0f, WIDTH, 180f, new Color(.53f, .71f, .39f, 1f));
        rect(0f, 0f, WIDTH, 63f, new Color(.32f, .50f, .28f, 1f));
        for (int i = 0; i < 7; i++) {
            float x = i * 132f + 12f;
            rect(x + 28f, 115f, 25f, 210f, new Color(.45f, .33f, .21f, 1f));
            rect(x + 2f, 260f, 76f, 50f, new Color(.32f, .57f, .39f, 1f));
            rect(x + 17f, 302f, 53f, 43f, new Color(.39f, .66f, .42f, 1f));
            rect(x + 28f, 338f, 30f, 30f, new Color(.47f, .72f, .43f, 1f));
        }
        for (int i = 0; i < 8; i++) {
            batch.draw(assets.flower, 12f + i * 107f, 66f + (i % 3) * 15f, 42f, 42f);
        }
    }

    public void renderTitle() {
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        scenery();
        rect(85f, 135f, 630f, 378f, new Color(.18f, .27f, .21f, .90f));
        rect(91f, 143f, 618f, 364f, CREAM);
        rect(91f, 491f, 618f, 16f, GOLD);
        fonts.center(batch, "BE HONEYBEE", 400f, 461f, 32, INK);
        fonts.center(batch, "작은 꿀벌의 위대한 모험", 400f, 421f, 22, INK);

        float bob = MathUtils.sin(animationTime * 3.5f) * 7f;
        batch.setColor(Color.WHITE);
        batch.draw(assets.beeYellow, 265f, 288f + bob, 112f, 112f);
        batch.draw(assets.beehive, 460f, 288f, 106f, 106f);
        rect(376f, 336f, 17f, 5f, new Color(.93f, .78f, .34f, 1f));
        rect(407f, 338f, 12f, 5f, new Color(.93f, .78f, .34f, 1f));
        rect(430f, 339f, 8f, 4f, new Color(.93f, .78f, .34f, 1f));

        rect(252f, 207f, 296f, 57f, new Color(.55f, .33f, .15f, 1f));
        rect(259f, 214f, 282f, 43f, GOLD);
        fonts.center(batch, "ENTER  모험 시작", 400f, 244f, 22, INK);
        fonts.center(batch, "이동 ← →   점프 SPACE   스킬 Z / X / C", 400f, 164f, 16, INK);
        batch.end();
    }

    /** elapsed is time since the beginning of the intro (seconds). */
    public void renderStageIntro(int level, float elapsed) {
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        rect(0f, 0f, WIDTH, HEIGHT, Color.BLACK);
        if (level == 1) {
            int line = MathUtils.clamp((int)(elapsed / 3.4f), 0, FIRST_STORY.length - 1);
            float local = elapsed - line * 3.4f;
            float alpha = MathUtils.clamp(local / .38f, 0f, 1f)
                * MathUtils.clamp((3.4f - local) / .45f, 0f, 1f);
            Color color = new Color(1f, 1f, 1f, alpha);
            fonts.wrap(batch, FIRST_STORY[line], 70f, 326f, 660f, 22, color);
        } else {
            String title = level == 2 ? "Stage 2. 꿀 모으기"
                : level == 3 ? "Stage 3. 말벌 무찌르기"
                : "Stage 4. 장수말벌이다...!";
            String description = level == 2 ? "위험한 꽃밭에서 꿀 10개를 모으세요."
                : level == 3 ? "거미를 피해 꿀 15개를 모으세요."
                : "꽃밭으로 출발하려던 순간, 거대한 위협이 다가옵니다.";
            float fade = MathUtils.clamp(elapsed / .5f, 0f, 1f);
            Color white = new Color(1f, 1f, 1f, fade);
            fonts.center(batch, title, 400f, 364f, 32, white);
            fonts.wrap(batch, description, 70f, 314f, 660f, 22, white);
            if (level == 2) {
                fonts.center(batch, "새로운 스킬: 대시 Z", 400f, 245f, 22, GOLD);
                fonts.wrap(batch, "작은 고추가 매운 법이죠! 적에게 당신의 따끔함을 보여주세요!",
                    105f, 215f, 590f, 16, GOLD);
            } else if (level == 3) {
                fonts.center(batch, "새로운 스킬: 독침 발사 X", 400f, 245f, 22, GOLD);
                fonts.center(batch, "당신을 무시하는 적들의 코를 퉁퉁 붓게 만듭시다!",
                    400f, 207f, 16, GOLD);
            } else {
                fonts.center(batch, "새로운 스킬: 도와줘! C", 400f, 245f, 22, GOLD);
                fonts.center(batch, "동료 벌들을 불러 적을 함께 공격하세요!",
                    400f, 207f, 16, GOLD);
            }
            fonts.center(batch, "ENTER  시작", 400f, 107f, 16, Color.GRAY);
        }
        batch.end();
    }

    public void renderTutorial(String message, int step) {
        if (message == null || message.isEmpty()) return;
        batch.setProjectionMatrix(camera.combined); batch.begin();
        // A brief situational line, never an opaque box.
        fonts.wrap(batch, message, 71f, 398f, 658f, 16,
            new Color(.08f, .10f, .10f, .85f));
        fonts.wrap(batch, message, 70f, 400f, 658f, 16, Color.WHITE);
        batch.end();
    }

    @Override
    public void dispose() {
        batch.dispose();
        pixel.dispose();
    }
}
