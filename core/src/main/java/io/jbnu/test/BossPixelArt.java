
package io.jbnu.test;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;

/**
 * 신규 이미지 에셋 없이 표시하는 보스 임시 도트 아트.
 * 공격 상태에 따라 앞발, 날개, 독침의 자세가 달라진다.
 * 피격 판정은 여전히 각 보스의 bounds를 따른다.
 */
public final class BossPixelArt {
    private static final Color OUTLINE =
        new Color(0.12f, 0.13f, 0.14f, 1f);
    private static final Color GREEN =
        new Color(0.30f, 0.66f, 0.33f, 1f);
    private static final Color GREEN_LIGHT =
        new Color(0.55f, 0.83f, 0.43f, 1f);
    private static final Color GREEN_DARK =
        new Color(0.16f, 0.40f, 0.22f, 1f);
    private static final Color AMBER =
        new Color(0.94f, 0.63f, 0.20f, 1f);
    private static final Color AMBER_LIGHT =
        new Color(1f, 0.84f, 0.36f, 1f);
    private static final Color RED_BROWN =
        new Color(0.48f, 0.23f, 0.16f, 1f);
    private static final Color WING =
        new Color(0.69f, 0.86f, 0.90f, 1f);
    private static final Color WHITE =
        new Color(0.97f, 0.96f, 0.88f, 1f);
    private static final Color EYE =
        new Color(0.13f, 0.09f, 0.10f, 1f);

    private BossPixelArt() {}

    public static void draw(
        ShapeRenderer s,
        Enemy boss,
        float time
    ) {
        if (boss == null || boss.isDead()) return;

        if (boss instanceof Mantis) {
            drawMantis(s, boss.bounds, boss.getState(), time);
        } else if (boss instanceof GiantHornet) {
            drawHornet(s, boss.bounds, boss.getState(), time, true);
        } else if (boss instanceof Hornet) {
            drawHornet(s, boss.bounds, boss.getState(), time, false);
        }
    }

    private static void block(
        ShapeRenderer s,
        float x, float y,
        float w, float h,
        Color color
    ) {
        s.setColor(color);
        s.rect(
            Math.round(x),
            Math.round(y),
            Math.max(1f, Math.round(w)),
            Math.max(1f, Math.round(h))
        );
    }

    private static void drawMantis(
        ShapeRenderer s,
        Rectangle r,
        Enemy.State state,
        float time
    ) {
        float x = r.x;
        float y = r.y;

        boolean striking = state == Enemy.State.ATTACK;
        boolean warning = state == Enemy.State.TELEGRAPH;
        boolean resting = state == Enemy.State.RECOVER;

        float bob = MathUtils.sin(time * 5f) * 2f;
        Color body = resting ? GREEN_LIGHT : GREEN;

        // 뒷다리와 긴 가슴
        block(s, x + 17f, y + 4f, 7f, 26f, OUTLINE);
        block(s, x + 18f, y + 7f, 5f, 21f, GREEN_DARK);
        block(s, x + 46f, y + 4f, 7f, 26f, OUTLINE);
        block(s, x + 47f, y + 7f, 5f, 21f, GREEN_DARK);

        block(s, x + 27f, y + 17f, 21f, 48f, OUTLINE);
        block(s, x + 30f, y + 20f, 15f, 44f, body);
        block(s, x + 33f, y + 36f, 7f, 27f, GREEN_LIGHT);

        // 머리와 눈
        block(s, x + 25f, y + 60f + bob, 26f, 20f, OUTLINE);
        block(s, x + 28f, y + 63f + bob, 20f, 14f, body);
        block(s, x + 31f, y + 70f + bob, 5f, 5f, EYE);
        block(s, x + 42f, y + 70f + bob, 5f, 5f, EYE);

        // 더듬이
        block(s, x + 31f, y + 80f + bob, 3f, 14f, GREEN_DARK);
        block(s, x + 45f, y + 80f + bob, 3f, 14f, GREEN_DARK);
        block(s, x + 27f, y + 93f + bob, 6f, 3f, GREEN_LIGHT);
        block(s, x + 46f, y + 93f + bob, 6f, 3f, GREEN_LIGHT);

        // 공격 상태에 따라 낫 모양 앞발을 펼친다.
        float clawY = striking ? 48f : warning ? 54f : 39f;
        float extension = striking ? 13f : 0f;

        block(s, x + 11f - extension, y + clawY,
            17f, 7f, OUTLINE);
        block(s, x + 12f - extension, y + clawY + 1f,
            14f, 5f, warning ? AMBER_LIGHT : GREEN_LIGHT);
        block(s, x + 4f - extension, y + clawY - 13f,
            9f, 18f, GREEN_DARK);
        block(s, x + 7f - extension, y + clawY - 16f,
            4f, 6f, WHITE);

        block(s, x + 47f, y + clawY,
            17f + extension, 7f, OUTLINE);
        block(s, x + 48f, y + clawY + 1f,
            14f + extension, 5f,
            warning ? AMBER_LIGHT : GREEN_LIGHT);
        block(s, x + 60f + extension, y + clawY - 13f,
            9f, 18f, GREEN_DARK);
        block(s, x + 62f + extension, y + clawY - 16f,
            4f, 6f, WHITE);
    }

    private static void drawHornet(
        ShapeRenderer s,
        Rectangle r,
        Enemy.State state,
        float time,
        boolean giant
    ) {
        float k = giant ? 1.25f : 1f;
        float x = r.x;
        float y = r.y;

        boolean attack = state == Enemy.State.ATTACK;
        boolean warning = state == Enemy.State.TELEGRAPH;

        boolean wingsUp =
            ((int) (time * (attack ? 22f : 12f)) & 1) == 0;

        // 날개가 두 가지 자세를 반복한다.
        float wingY = wingsUp ? 26f * k : 22f * k;

        block(s, x + 21f * k, y + wingY,
            17f * k, 5f * k, OUTLINE);
        block(s, x + 23f * k, y + wingY + 2f,
            14f * k, 4f * k, WING);
        block(s, x + 38f * k, y + wingY - 2f,
            12f * k, 5f * k, OUTLINE);
        block(s, x + 39f * k, y + wingY,
            10f * k, 5f * k, WING);

        // 배와 복부 줄무늬
        block(s, x + 9f * k, y + 10f * k,
            42f * k, 17f * k, OUTLINE);
        block(s, x + 12f * k, y + 13f * k,
            36f * k, 11f * k,
            giant ? RED_BROWN : AMBER);

        for (int i = 0; i < 3; i++) {
            block(s, x + (18f + i * 9f) * k,
                y + 13f * k,
                4f * k, 11f * k,
                giant ? AMBER_LIGHT : OUTLINE);
        }

        // 머리, 눈, 턱
        block(s, x + 47f * k, y + 14f * k,
            13f * k, 14f * k, OUTLINE);
        block(s, x + 49f * k, y + 16f * k,
            9f * k, 10f * k,
            giant ? AMBER : AMBER_LIGHT);
        block(s, x + 56f * k, y + 22f * k,
            3f * k, 3f * k,
            warning ? Color.RED : EYE);
        block(s, x + 59f * k, y + 16f * k,
            4f * k, 3f * k, OUTLINE);

        // 더듬이와 다리
        block(s, x + 53f * k, y + 27f * k,
            3f * k, 7f * k, OUTLINE);
        block(s, x + 59f * k, y + 27f * k,
            3f * k, 6f * k, OUTLINE);
        block(s, x + 23f * k, y + 6f * k,
            4f * k, 7f * k, OUTLINE);
        block(s, x + 39f * k, y + 6f * k,
            4f * k, 7f * k, OUTLINE);

        // 공격 상태에서 독침 자세 변화
        block(s, x + (attack ? 3f : 6f) * k,
            y + 17f * k,
            7f * k, 4f * k,
            giant ? RED_BROWN : OUTLINE);
        block(s, x + (attack ? 0f : 3f) * k,
            y + 18f * k,
            4f * k, 2f * k, WHITE);
    }
}
