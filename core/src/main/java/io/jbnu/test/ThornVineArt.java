
package io.jbnu.test;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

/**
 * 가시덩굴의 그래픽만 담당하는 클래스.
 * 위치 및 충돌 판정은 World에서 관리한다.
 */
public final class ThornVineArt {

    private static final float STEP = 16f;
    private static final float ROOT_HEIGHT = 6f;

    private static final Color SHADOW =
        new Color(0.11f, 0.23f, 0.13f, 1f);

    private static final Color STEM =
        new Color(0.20f, 0.43f, 0.22f, 1f);

    private static final Color LEAF =
        new Color(0.38f, 0.66f, 0.30f, 1f);

    private static final Color TIP =
        new Color(0.93f, 0.82f, 0.54f, 1f);

    private ThornVineArt() {
    }

    public static void draw(
        ShapeRenderer shapes,
        Array<Rectangle> vines) {

        for (Rectangle vine : vines) {
            if (vine.width <= 0f || vine.height <= 0f) {
                continue;
            }

            drawVine(shapes, vine);
        }
    }

    private static void drawVine(
        ShapeRenderer shapes,
        Rectangle box) {

        // 천장에 설치된 덩굴은 아래로 자라도록 표시한다.
        boolean hanging = box.y >= 500f;

        float baseY = hanging
            ? box.y + box.height - ROOT_HEIGHT
            : box.y;

        shapes.setColor(SHADOW);
        shapes.rect(
            box.x, baseY,
            box.width, ROOT_HEIGHT
        );

        shapes.setColor(STEM);
        shapes.rect(
            box.x + 2f,
            baseY + 1f,
            Math.max(0f, box.width - 4f),
            4f
        );

        int segments = Math.max(
            1, (int) Math.ceil(box.width / STEP)
        );

        float unitWidth = box.width / segments;

        for (int i = 0; i < segments; i++) {

            float centerX =
                box.x + (i + 0.5f) * unitWidth;

            float tipY = hanging
                ? box.y + 3f
                : box.y + box.height - 3f;

            float rootY = hanging
                ? box.y + box.height - 4f
                : box.y + 4f;

            float lowY = Math.min(rootY, tipY);
            float highY = Math.max(rootY, tipY);

            // 두꺼운 외곽선과 중심 줄기
            shapes.setColor(SHADOW);
            shapes.rect(
                centerX - 3f,
                lowY,
                6f,
                highY - lowY
            );

            shapes.setColor(STEM);
            shapes.rect(
                centerX - 1f,
                lowY,
                3f,
                highY - lowY
            );

            // 좌우로 뻗은 잎
            float leafY = hanging
                ? box.y + box.height - 12f
                : box.y + 7f;

            shapes.setColor(LEAF);

            shapes.triangle(
                centerX - 1f, leafY,
                centerX - 9f,
                leafY + (hanging ? -3f : 3f),
                centerX - 3f,
                leafY + (hanging ? -6f : 6f)
            );

            shapes.triangle(
                centerX + 1f, leafY,
                centerX + 9f,
                leafY + (hanging ? -3f : 3f),
                centerX + 3f,
                leafY + (hanging ? -6f : 6f)
            );

            // 가시 끝은 밝은 색상으로 강조
            shapes.setColor(TIP);

            if (hanging) {
                shapes.triangle(
                    centerX - 3f, box.y + 6f,
                    centerX + 3f, box.y + 6f,
                    centerX, box.y
                );
            } else {
                shapes.triangle(
                    centerX - 3f,
                    box.y + box.height - 6f,
                    centerX + 3f,
                    box.y + box.height - 6f,
                    centerX,
                    box.y + box.height
                );
            }
        }
    }
}
