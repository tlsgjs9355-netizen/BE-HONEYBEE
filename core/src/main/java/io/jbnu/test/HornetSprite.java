
package io.jbnu.test;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Disposable;

/**
 * M6.52 - Stage 3/4 hornet sprite renderer.
 * Giant hornet forelegs follow the locked attack direction.
 * Combat and capture hitboxes remain in GiantHornet.
 */
public final class HornetSprite implements Disposable {

    private static final String HORNET_PATH =
        "bosses/hornets/hornet_black.png";

    private static final String GIANT_PATH =
        "bosses/hornets/giant_hornet_sheet.png";

    private static final String STING_PATH =
        "effects/hornet_sting_curl.png";

    private static final int HORNET_CELL = 32;
    private static final int GIANT_CELL = 96;
    private static final int HORNET_FRAMES = 4;
    private static final int GIANT_FRAMES = 11;
    private static final int STING_FRAMES = 6;

    private static final float HORNET_SIZE = 96f;
    private static final float GIANT_SIZE = 144f;

    private static final Color DARK_LEG =
        new Color(.22f, .15f, .11f, 1f);

    private static final Color GOLD_LEG =
        new Color(.96f, .69f, .28f, 1f);

    private static final Color LIGHT_LEG =
        new Color(1f, .89f, .54f, 1f);

    private final Texture hornetTexture;
    private final Texture giantTexture;
    private final Texture stingTexture;
    private final Texture solidPixel;

    private final TextureRegion[] hornetRight;
    private final TextureRegion[] hornetLeft;
    private final TextureRegion[] stingRight;
    private final TextureRegion[] stingLeft;
    private final TextureRegion[] giantRight;
    private final TextureRegion[] giantLeft;

    public HornetSprite() {
        checkExists(HORNET_PATH);
        checkExists(GIANT_PATH);
        checkExists(STING_PATH);

        Texture normal = null;
        Texture giant = null;
        Texture sting = null;
        Texture pixel = null;

        try {
            normal = new Texture(
                Gdx.files.internal(HORNET_PATH)
            );
            giant = new Texture(
                Gdx.files.internal(GIANT_PATH)
            );
            sting = new Texture(
                Gdx.files.internal(STING_PATH)
            );

            verifySize(
                normal,
                HORNET_CELL * HORNET_FRAMES,
                HORNET_CELL,
                HORNET_PATH
            );
            verifySize(
                giant,
                GIANT_CELL * GIANT_FRAMES,
                GIANT_CELL,
                GIANT_PATH
            );
            verifySize(
                sting,
                HORNET_CELL * STING_FRAMES,
                HORNET_CELL,
                STING_PATH
            );

            useNearest(normal);
            useNearest(giant);
            useNearest(sting);

            Pixmap dot = new Pixmap(
                1, 1, Pixmap.Format.RGBA8888
            );

            try {
                dot.setColor(Color.WHITE);
                dot.fill();

                pixel = new Texture(dot);
                useNearest(pixel);

            } finally {
                dot.dispose();
            }

            hornetRight = frames(
                normal,
                HORNET_CELL, HORNET_FRAMES, false
            );
            hornetLeft = frames(
                normal,
                HORNET_CELL, HORNET_FRAMES, true
            );

            giantRight = frames(
                giant,
                GIANT_CELL, GIANT_FRAMES, false
            );
            giantLeft = frames(
                giant,
                GIANT_CELL, GIANT_FRAMES, true
            );

            stingRight = frames(
                sting,
                HORNET_CELL, STING_FRAMES, false
            );
            stingLeft = frames(
                sting,
                HORNET_CELL, STING_FRAMES, true
            );

            hornetTexture = normal;
            giantTexture = giant;
            stingTexture = sting;
            solidPixel = pixel;

        } catch (RuntimeException ex) {
            if (normal != null) normal.dispose();
            if (giant != null) giant.dispose();
            if (sting != null) sting.dispose();
            if (pixel != null) pixel.dispose();
            throw ex;
        }
    }

    private static void checkExists(String path) {
        if (!Gdx.files.internal(path).exists()) {
            throw new IllegalStateException(
                "말벌 에셋이 없습니다: assets/" + path
            );
        }
    }

    private static void verifySize(
        Texture texture,
        int width,
        int height,
        String path
    ) {
        if (texture.getWidth() != width
            || texture.getHeight() != height) {

            throw new IllegalStateException(
                "스프라이트 크기 오류: " + path
                    + " (현재 "
                    + texture.getWidth()
                    + "x" + texture.getHeight()
                    + ", 필요 "
                    + width + "x" + height + ")"
            );
        }
    }

    private static void useNearest(Texture texture) {
        texture.setFilter(
            Texture.TextureFilter.Nearest,
            Texture.TextureFilter.Nearest
        );
    }

    private static TextureRegion[] frames(
        Texture texture,
        int cell,
        int count,
        boolean flipped
    ) {
        TextureRegion[] out = new TextureRegion[count];

        for (int i = 0; i < count; i++) {
            out[i] = new TextureRegion(
                texture,
                i * cell, 0, cell, cell
            );

            if (flipped) {
                out[i].flip(true, false);
            }
        }

        return out;
    }

    public void draw(
        SpriteBatch batch,
        Enemy boss,
        Rectangle beeBounds,
        float time
    ) {
        if (boss == null || boss.isDead()) return;

        boolean isGiant = boss instanceof GiantHornet;

        if (!isGiant && !(boss instanceof Hornet)) {
            return;
        }

        boolean lookRight = isGiant
            ? ((GiantHornet) boss).getFacing() > 0
            : ((Hornet) boss).getFacing() > 0;

        Enemy.State state = boss.getState();

        float frameDuration;

        switch (state) {
            case TELEGRAPH:
                frameDuration = isGiant ? .14f : .18f;
                break;

            case ATTACK:
                frameDuration = isGiant ? .065f : .075f;
                break;

            case RECOVER:
                frameDuration = isGiant ? .17f : .22f;
                break;

            default:
                frameDuration = isGiant ? .10f : .13f;
                break;
        }

        TextureRegion[] selected;
        int index;

        boolean stingAnimation =
            !isGiant
                && state == Enemy.State.ATTACK
                && ((Hornet) boss).getPattern()
                == Hornet.Pattern.STING;

        if (stingAnimation) {
            selected = lookRight
                ? stingRight : stingLeft;

            float p =
                ((Hornet) boss).getAttackProgress();

            if (p < .12f) index = 0;
            else if (p < .28f) index = 1;
            else if (p < .46f) index = 2;
            else if (p < .68f) index = 3;
            else if (p < .85f) index = 4;
            else index = 5;

        } else {
            selected = isGiant
                ? (lookRight ? giantRight : giantLeft)
                : (lookRight ? hornetRight : hornetLeft);

            index = (int)(
                Math.max(0f, time) / frameDuration
            ) % selected.length;
        }

        float size =
            isGiant ? GIANT_SIZE : HORNET_SIZE;

        float x = boss.centerX() - size * .5f;
        float y = boss.centerY() - size * .5f;

        if (state == Enemy.State.ATTACK) {
            x += lookRight ? 5f : -5f;

        } else {
            y += MathUtils.sin(time * 9f) * 2f;
        }

        if (state == Enemy.State.TELEGRAPH) {
            float glow =
                .67f + .15f * MathUtils.sin(time * 25f);

            batch.setColor(1f, glow, glow, 1f);

        } else if (state == Enemy.State.RECOVER) {
            batch.setColor(.75f, .83f, 1f, 1f);

        } else {
            batch.setColor(Color.WHITE);
        }

        batch.draw(
            selected[index],
            Math.round(x),
            Math.round(y),
            size,
            size
        );

        batch.setColor(Color.WHITE);

        if (isGiant) {
            drawGiantGrabArms(
                batch,
                (GiantHornet) boss,
                time
            );
        }
    }

    /**
     * Draw both forelegs along the actual grab direction.
     * No damage or capture logic is changed here.
     */
    private void drawGiantGrabArms(
        SpriteBatch batch,
        GiantHornet boss,
        float time
    ) {
        if (boss.getPattern()
            != GiantHornet.Pattern.GRAB) {
            return;
        }

        Enemy.State state = boss.getState();

        if (state != Enemy.State.TELEGRAPH
            && state != Enemy.State.ATTACK) {
            return;
        }

        Vector2 direction = boss.getDirection();

        float ux = direction.x;
        float uy = direction.y;

        float length = (float)Math.sqrt(
            ux * ux + uy * uy
        );

        if (length < .001f) {
            ux = boss.getFacing() > 0 ? 1f : -1f;
            uy = 0f;

        } else {
            ux /= length;
            uy /= length;
        }

        // A perpendicular vector allows upward,
        // downward and diagonal grab animations.
        float nx = -uy;
        float ny = ux;

        float p =
            state == Enemy.State.ATTACK
                ? boss.getAttackProgress()
                : 0f;

        float reach;
        float gap;

        if (state == Enemy.State.TELEGRAPH) {
            reach = 29f;
            gap = 24f
                + MathUtils.sin(time * 16f) * 2f;

        } else if (p < .28f) {
            reach = MathUtils.lerp(
                29f, 53f, p / .28f
            );
            gap = 26f;

        } else if (p < .66f) {
            float close = (p - .28f) / .38f;

            reach = MathUtils.lerp(
                53f, 65f, close
            );
            gap = MathUtils.lerp(
                26f, 4.5f, close
            );

        } else if (p < .79f) {
            reach = 65f;
            gap = 4.5f;

        } else {
            float retract = MathUtils.clamp(
                (p - .79f) / .21f,
                0f, 1f
            );

            reach = MathUtils.lerp(
                65f, 29f, retract
            );
            gap = MathUtils.lerp(
                4.5f, 24f, retract
            );
        }

        float cx = boss.centerX();
        float cy = boss.centerY();

        for (int side = -1; side <= 1; side += 2) {

            // Shoulder
            float sx =
                cx + ux * 12f
                    + nx * (side * 15f);

            float sy =
                cy + uy * 12f
                    + ny * (side * 15f);

            // Elbow
            float ex =
                cx + ux * (reach * .64f)
                    + nx * (side * (gap + 10f));

            float ey =
                cy + uy * (reach * .64f)
                    + ny * (side * (gap + 10f));

            // Claw tip
            float tx =
                cx + ux * reach
                    + nx * (side * gap);

            float ty =
                cy + uy * reach
                    + ny * (side * gap);

            // Dark outlines
            segment(
                batch,
                sx, sy, ex, ey,
                8f, DARK_LEG
            );

            segment(
                batch,
                ex, ey, tx, ty,
                7f, DARK_LEG
            );

            // Golden foreleg details
            segment(
                batch,
                sx + ux * 2f,
                sy + uy * 2f,
                ex, ey,
                4.5f, GOLD_LEG
            );

            segment(
                batch,
                ex, ey, tx, ty,
                3.5f, GOLD_LEG
            );

            // Sharp inward-pointing claw
            float hookX =
                tx - ux * 8f
                    - nx * (side * 7f);

            float hookY =
                ty - uy * 8f
                    - ny * (side * 7f);

            segment(
                batch,
                tx, ty, hookX, hookY,
                3f, DARK_LEG
            );

            segment(
                batch,
                tx, ty, hookX, hookY,
                1.5f, LIGHT_LEG
            );
        }

        batch.setColor(Color.WHITE);
    }

    /**
     * Draw a rotated, pixel-sharp segment using SpriteBatch.
     */
    private void segment(
        SpriteBatch batch,
        float ax,
        float ay,
        float bx,
        float by,
        float thickness,
        Color tint
    ) {
        float dx = bx - ax;
        float dy = by - ay;

        float length = (float)Math.sqrt(
            dx * dx + dy * dy
        );

        if (length < .001f) return;

        float degrees = (float)Math.toDegrees(
            Math.atan2(dy, dx)
        );

        batch.setColor(tint);

        batch.draw(
            solidPixel,
            Math.round(ax),
            Math.round(ay - thickness * .5f),
            0f,
            thickness * .5f,
            length,
            thickness,
            1f,
            1f,
            degrees,
            0, 0, 1, 1,
            false, false
        );
    }

    @Override
    public void dispose() {
        hornetTexture.dispose();
        giantTexture.dispose();
        stingTexture.dispose();
        solidPixel.dispose();
    }
}
