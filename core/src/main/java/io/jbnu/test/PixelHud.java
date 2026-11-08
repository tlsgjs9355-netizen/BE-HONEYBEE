
package io.jbnu.test;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Disposable;
import java.util.Locale;

/**
 * M6.49 - Korean pixel HUD.
 * Quest is placed below the skill list.
 */
public final class PixelHud implements Disposable {

    private static final Color WHITE = Color.WHITE;
    private static final Color GOLD =
        new Color(.98f, .78f, .34f, 1f);
    private static final Color RED =
        new Color(.96f, .33f, .34f, 1f);
    private static final Color GREEN =
        new Color(.25f, .88f, .46f, 1f);
    private static final Color SHADE =
        new Color(.08f, .11f, .12f, .7f);
    private static final Color EMPTY =
        new Color(.35f, .35f, .38f, 1f);

    private static final String[] HEART = {
        "01100110", "11111111", "11111111",
        "01111110", "00111100", "00011000"
    };

    private static final String[] ENDING = {
        "당신은 모든 위협으로부터 왕국을 지켜냈습니다.",
        "사마귀도, 장수말벌도 당신의 위대한 비행을 막을 순 없었습니다!",
        "이제는 베테랑 일벌이 된 당신은 보초병으로서 마지막 일생을 보내고 있습니다.",
        "어린 꿀벌들 사이에선 당신의 이야기가 늘 오르내립니다.",
        "다시 평화로운 일상이 시작되었지만, 아직 방심할 수는 없습니다.",
        "알 수 없는 또다른 위험이 찾아오고 있으니까요."
    };

    private final GameAssets assets;
    private final SpriteBatch batch = new SpriteBatch();
    private final OrthographicCamera camera =
        new OrthographicCamera();
    private final GalmuriFonts font =
        GalmuriFonts.shared();

    private final Texture pixel;

    public PixelHud(GameAssets assets) {
        this.assets = assets;

        camera.setToOrtho(false, 800f, 600f);
        camera.update();

        Pixmap p = new Pixmap(
            1, 1, Pixmap.Format.RGBA8888
        );

        p.setColor(WHITE);
        p.fill();

        pixel = new Texture(p);
        pixel.setFilter(
            Texture.TextureFilter.Nearest,
            Texture.TextureFilter.Nearest
        );

        p.dispose();
        batch.enableBlending();
    }

    private void rect(
        float x, float y,
        float w, float h,
        Color c
    ) {
        batch.setColor(c);
        batch.draw(pixel, x, y, w, h);
        batch.setColor(WHITE);
    }

    private void txt(
        String s, float x, float y,
        int size, Color c
    ) {
        font.draw(
            batch, s,
            x + 1f, y - 1f,
            size, SHADE
        );

        font.draw(batch, s, x, y, size, c);
    }

    private void centered(
        String s, float x, float y,
        int size, Color c
    ) {
        font.center(
            batch, s,
            x + 1f, y - 1f,
            size, SHADE
        );

        font.center(batch, s, x, y, size, c);
    }

    private void heart(
        float x, float y,
        int halves
    ) {
        for (int row = 0; row < HEART.length; row++) {
            for (int col = 0; col < 8; col++) {
                if (HEART[row].charAt(col) != '1') {
                    continue;
                }

                Color c =
                    (col < 4 && halves >= 1)
                        || (col >= 4 && halves >= 2)
                        ? RED : EMPTY;

                rect(
                    x + col * 2.4f,
                    y + (5 - row) * 2.4f,
                    2.4f,
                    2.4f,
                    c
                );
            }
        }
    }

    private void meter(
        float x, float y,
        float width, float ratio
    ) {
        rect(
            x, y, width, 3f,
            new Color(.15f, .19f, .20f, .55f)
        );

        rect(
            x, y,
            width * MathUtils.clamp(ratio, 0f, 1f),
            3f,
            GOLD
        );
    }

    private String quest(World w) {
        if (w.level == 1) {
            return w.isExitUnlocked()
                ? "벌통으로 돌아가세요"
                : "꿀 6개 모으기";
        }

        if (w.level == 2) {
            return w.isArenaCleared()
                ? "벌통으로 돌아가세요"
                : w.isArenaGateOpen()
                ? "사마귀 물리치기"
                : "꿀 10개 모으기";
        }

        if (w.level == 3) {
            return w.isArenaCleared()
                ? "벌통으로 돌아가세요"
                : w.isArenaGateOpen()
                ? "등검은말벌 물리치기"
                : "꿀 15개 모으기";
        }

        if (w.isGameCleared()) {
            return "왕국을 지켜냈습니다";
        }

        return w.hasStageFourAmbushOccurred()
            ? "장수말벌 물리치기"
            : "장수말벌의 습격을 막으세요";
    }

    private String message(String s) {
        switch (s) {
            case "FIRST DAY AT WORK":
                return "첫 외역을 시작합니다";

            case "FLOWER FIELD AMBUSH":
                return "위험한 꽃밭";

            case "HIVE FRONTIER":
                return "벌통으로 돌아가는 길";

            case "FINAL DEFENSE!":
                return "최후의 방어!";

            case "GET CLOSER TO THE BOSS!":
                return "적에게 가까이 다가가야 합니다!";

            case "BOSS INCOMING!":
                return "보스 등장!";

            case "BOSS DEFEATED - EXIT OPEN!":
                return "보스 격파! 벌통으로 이동하세요";

            default:
                return s;
        }
    }

    private void skill(
        String key,
        String name,
        float y,
        boolean unlocked,
        float ratio
    ) {
        txt(
            key, 625f, y, 16,
            unlocked ? GOLD : Color.GRAY
        );

        txt(
            name, 656f, y, 16,
            unlocked ? WHITE : Color.GRAY
        );

        if (unlocked) {
            meter(655f, y - 25f, 120f, ratio);
        }
    }

    public void render(
        World w,
        float endingTime,
        boolean paused,
        boolean ending
    ) {
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        txt(
            "STAGE " + w.level,
            14f, 576f, 22, GOLD
        );

        for (int i = 0; i < 5; i++) {
            heart(
                17f + i * 27f,
                542f,
                MathUtils.clamp(
                    w.getPlayerHalfHearts() - i * 2,
                    0, 2
                )
            );
        }

        // No honey goal in Stage 4 or active boss arenas.
        boolean showHoney =
            w.level <= 3
                && (w.level == 1 || !w.isArenaEntered());

        if (showHoney) {
            batch.setColor(WHITE);

            batch.draw(
                assets.honey,
                15f, 500f,
                23f, 23f
            );

            txt(
                w.getHoneyCollected() + " / "
                    + w.getRequiredHoney(),
                47f, 519f, 16, WHITE
            );
        }

        skill(
            "Z", "대시", 575f,
            w.player.dashUnlocked,
            w.player.isDashing()
                ? 0f
                : 1f - w.player.getDashCooldown()
                / Player.DASH_COOLDOWN
        );

        skill(
            "X", "독침", 536f,
            w.isStingUnlocked(),
            1f - w.getStingerCooldown()
                / World.STINGER_COOLDOWN
        );

        skill(
            "C", "도와줘!", 497f,
            w.isSwarmUnlocked(),
            w.isSwarmActive()
                ? 0f
                : w.getSwarmGauge() / 100f
        );

        // M6.49: objective below the skill list.
        txt(
            "현재 목표",
            625f, 444f, 14, GOLD
        );

        font.wrap(
            batch,
            quest(w),
            625f, 419f,
            166f, 14, WHITE
        );

        Enemy boss = w.getActiveBoss();

        if (boss != null) {
            String name =
                boss instanceof GiantHornet
                    ? "장수말벌"
                    : boss instanceof Hornet
                    ? "등검은말벌"
                    : "사마귀";

            centered(
                name,
                400f, 575f, 22, GOLD
            );

            rect(
                287f, 530f,
                226f, 7f,
                new Color(.10f, .13f, .15f, .65f)
            );

            rect(
                287f, 530f,
                226f * (float)boss.getHp()
                    / boss.getMaxHp(),
                7f, RED
            );
        }

        if (w.isCaptured() && !ending) {
            drawCapture(w);

        } else if (
            !ending
                && !w.getMessage().isEmpty()
        ) {
            centered(
                message(w.getMessage()),
                400f, 430f, 16, GOLD
            );
        }

        if (ending) {
            drawEnding(endingTime);
        }

        if (paused) {
            drawPause();
        }

        batch.end();
    }

    private void drawCapture(World w) {
        rect(
            0f, 0f, 800f, 600f,
            new Color(0f, 0f, 0f, .55f)
        );

        rect(
            154f, 207f,
            492f, 197f,
            new Color(.06f, .09f, .10f, .92f)
        );

        centered(
            w.isGiantHornetCaptured()
                ? "장수말벌에게 붙잡혔습니다!"
                : w.isMantisCaptured()
                ? "사마귀에게 붙잡혔습니다!"
                : "거미에게 붙잡혔습니다!",
            400f, 375f, 22, RED
        );

        centered(
            "초록색에 맞춰 SPACE를 누르세요",
            400f, 347f, 16, WHITE
        );

        // Three progress slots, without success/failure text.
        for (int i = 0; i < 3; i++) {
            float sx = 347f + i * 42f;
            boolean complete = i < w.getEscapeHits();

            rect(
                sx - 2f, 299f,
                34f, 29f, EMPTY
            );

            rect(
                sx, 301f,
                30f, 25f,
                complete ? GREEN : SHADE
            );

            if (complete) {
                rect(
                    sx + 8f, 308f,
                    14f, 10f, GOLD
                );
            }
        }

        for (
            int i = 0;
            i < w.getEscapeMisses() && i < 3;
            i++
        ) {
            rect(
                494f + i * 12f,
                317f, 8f, 7f, RED
            );
        }

        float x = 220f;
        float y = 264f;
        float width = 360f;

        rect(
            x, y, width, 27f,
            new Color(.23f, .25f, .28f, 1f)
        );

        float st = w.getEscapeZoneStart();
        float ed = w.getEscapeZoneEnd();

        rect(
            x + st * width,
            y,
            (ed - st) * width,
            27f,
            GREEN
        );

        rect(
            x + width * w.getGaugePosition() - 3f,
            y - 6f,
            6f, 39f, WHITE
        );

        if (w.getEscapeFeedbackTime() > 0f) {
            boolean good =
                w.getEscapeFeedback()
                    == CaptureTiming.Feedback.HIT;

            Color flash = good ? GREEN : RED;

            rect(
                x - 4f, y - 5f,
                width + 8f, 3f, flash
            );

            rect(
                x - 4f, y + 30f,
                width + 8f, 3f, flash
            );

            rect(
                x - 4f, y - 2f,
                3f, 32f, flash
            );

            rect(
                x + width + 1f, y - 2f,
                3f, 32f, flash
            );
        }

        centered(
            String.format(
                Locale.US,
                "남은 시간 %.1f초",
                w.getCaptureTimeRemaining()
            ),
            400f, 235f, 16, WHITE
        );

        if (w.isGiantHornetCaptured()) {
            centered(
                "타이밍을 놓치면 체력 2칸 감소",
                400f, 215f, 16, RED
            );

        } else if (w.isMantisCaptured()) {
            centered(
                "탈출하지 못하면 체력 2.5칸 감소",
                400f, 215f, 16, RED
            );

        } else {
            centered(
                "탈출하지 못하면 사망합니다",
                400f, 215f, 16, RED
            );
        }
    }

    private void drawPause() {
        rect(
            0f, 0f, 800f, 600f,
            new Color(0f, 0f, 0f, .65f)
        );

        centered(
            "일시정지",
            400f, 370f, 32, GOLD
        );

        centered(
            "이동 ← →   점프 SPACE   대시 Z",
            400f, 315f, 16, WHITE
        );

        centered(
            "독침 X   동료 벌 C   P / ESC 계속하기",
            400f, 279f, 16, WHITE
        );
    }

    private void drawEnding(float elapsed) {
        rect(
            0f, 0f, 800f, 600f,
            Color.BLACK
        );

        int index = Math.min(
            ENDING.length - 1,
            (int)(elapsed / 3.5f)
        );

        if (elapsed < ENDING.length * 3.5f) {
            float local = elapsed - index * 3.5f;

            float alpha =
                MathUtils.clamp(
                    local / .6f, 0f, 1f
                ) * MathUtils.clamp(
                    (3.5f - local) / .5f, 0f, 1f
                );

            font.wrap(
                batch,
                ENDING[index],
                60f, 341f,
                680f, 22,
                new Color(1f, 1f, 1f, alpha)
            );

        } else {
            batch.setColor(WHITE);

            batch.draw(
                assets.beeYellow,
                275f, 237f,
                108f, 108f
            );

            batch.draw(
                assets.beehive,
                444f, 230f,
                119f, 119f
            );

            centered(
                "BE HONEYBEE",
                400f, 435f, 32, GOLD
            );

            centered(
                "왕국에 다시 평화가 찾아왔습니다",
                400f, 190f, 22, WHITE
            );

            centered(
                "ENTER  처음부터 다시 시작",
                400f, 115f, 16,
                Color.LIGHT_GRAY
            );
        }
    }

    @Override
    public void dispose() {
        batch.dispose();
        pixel.dispose();
    }
}
