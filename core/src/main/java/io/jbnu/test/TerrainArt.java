
package io.jbnu.test;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Disposable;

/**
 * M6.55 - Final forest background alignment.
 * Uses forest_complete_trees.png without transparent padding.
 */
public final class TerrainArt implements Disposable {

    private static final float TILE_SIZE = 36f;
    private static final int TILE_SOURCE = 18;
    private static final float GROUND_TOP = 40f;
    private static final float HILL_PIXEL_STEP = 8f;

    // Original atlas: 448x332, four frames of 112x332.
    // Visible tree pixels occupy rows 6 through 318.
    private static final int TREE_SOURCE_W = 112;
    private static final int TREE_SOURCE_Y = 6;
    private static final int TREE_SOURCE_H = 313;

    private final Texture grassTop;
    private final Texture dirt;
    private final Texture woodTop;
    private final Texture woodInside;
    private final Texture skyTexture;
    private final Texture treeTexture;

    private final TextureRegion[] treeRight =
        new TextureRegion[4];
    private final TextureRegion[] treeLeft =
        new TextureRegion[4];

    private final SpriteBatch backgroundBatch =
        new SpriteBatch();

    public TerrainArt() {
        grassTop = required("terrain/tile_0000.png");
        dirt = required("terrain/tile_0004.png");
        woodTop = required("terrain/tile_0040.png");
        woodInside = required("terrain/tile_0049.png");

        skyTexture = optional(
            "forest/forest_sky.png"
        );

        treeTexture = optional(
            "forest/forest_complete_trees.png"
        );

        if (treeTexture != null
            && treeTexture.getWidth() >= 448
            && treeTexture.getHeight()
            >= TREE_SOURCE_Y + TREE_SOURCE_H) {

            for (int i = 0; i < 4; i++) {
                int x = i * TREE_SOURCE_W;

                treeRight[i] = new TextureRegion(
                    treeTexture,
                    x, TREE_SOURCE_Y,
                    TREE_SOURCE_W, TREE_SOURCE_H
                );

                treeLeft[i] = new TextureRegion(
                    treeTexture,
                    x, TREE_SOURCE_Y,
                    TREE_SOURCE_W, TREE_SOURCE_H
                );

                treeLeft[i].flip(true, false);
            }
        }
    }

    private static Texture required(String path) {
        Texture texture = new Texture(
            Gdx.files.internal(path)
        );

        nearest(texture);
        return texture;
    }

    private static Texture optional(String path) {
        FileHandle file = Gdx.files.internal(path);

        if (!file.exists()) {
            return null;
        }

        Texture texture = new Texture(file);
        nearest(texture);
        return texture;
    }

    private static void nearest(Texture texture) {
        texture.setFilter(
            Texture.TextureFilter.Nearest,
            Texture.TextureFilter.Nearest
        );
    }

    public void drawBackground(
        ShapeRenderer shapes,
        OrthographicCamera camera,
        int level
    ) {
        if (skyTexture == null || treeRight[0] == null) {
            fallback(shapes, camera, level);
            return;
        }

        // GameRenderer begins the ShapeRenderer first.
        shapes.end();

        backgroundBatch.setProjectionMatrix(
            camera.combined
        );

        // Sky
        backgroundBatch.begin();
        sky(camera, level);
        backgroundBatch.end();

        // Hills are always behind all trees.
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        hills(shapes, camera, level, false);
        hills(shapes, camera, level, true);
        shapes.end();

        // Draw the three tree depth layers.
        backgroundBatch.begin();

        trees(camera, level, 0);
        trees(camera, level, 1);
        trees(camera, level, 2);

        backgroundBatch.setColor(Color.WHITE);
        backgroundBatch.end();

        // Restore the rendering state.
        shapes.begin(ShapeRenderer.ShapeType.Filled);
    }

    private void sky(
        OrthographicCamera camera,
        int level
    ) {
        float w = camera.viewportWidth * camera.zoom;
        float h = camera.viewportHeight * camera.zoom;

        float left = camera.position.x - w / 2f;
        float bottom = camera.position.y - h / 2f;

        if (level == 2) {
            backgroundBatch.setColor(.97f, 1f, .91f, 1f);
        } else if (level == 3) {
            backgroundBatch.setColor(.82f, .86f, .99f, 1f);
        } else if (level == 4) {
            backgroundBatch.setColor(1f, .83f, .77f, 1f);
        } else {
            backgroundBatch.setColor(Color.WHITE);
        }

        backgroundBatch.draw(
            skyTexture,
            Math.round(left - 45f),
            Math.round(bottom - 20f),
            w + 90f,
            h + 40f
        );

        backgroundBatch.setColor(Color.WHITE);
    }

    private void hills(
        ShapeRenderer shapes,
        OrthographicCamera camera,
        int level,
        boolean near
    ) {
        float w = camera.viewportWidth * camera.zoom;
        float h = camera.viewportHeight * camera.zoom;

        float left = camera.position.x - w / 2f;
        float right = left + w;
        float base = camera.position.y - h / 2f - 8f;

        float parallax = near ? .23f : .08f;

        hillColor(shapes, level, near);

        float start =
            (float)Math.floor(left / HILL_PIXEL_STEP)
                * HILL_PIXEL_STEP;

        for (
            float x = start;
            x < right + HILL_PIXEL_STEP;
            x += HILL_PIXEL_STEP
        ) {
            float phase =
                x - camera.position.x * (1f - parallax);

            float ridge;

            if (near) {
                ridge = 224f
                    + 27f * MathUtils.sin(
                    phase * .014f + .8f
                )
                    + 13f * MathUtils.sin(
                    phase * .029f
                )
                    + 6f * MathUtils.sin(
                    phase * .061f
                );
            } else {
                ridge = 314f
                    + 25f * MathUtils.sin(
                    phase * .011f
                )
                    + 15f * MathUtils.sin(
                    phase * .024f + 1.3f
                )
                    + 7f * MathUtils.sin(
                    phase * .037f
                );
            }

            ridge = Math.round(ridge / 4f) * 4f;

            if (ridge > base) {
                shapes.rect(
                    x, base,
                    HILL_PIXEL_STEP + .5f,
                    ridge - base
                );
            }
        }
    }

    private void hillColor(
        ShapeRenderer shapes,
        int level,
        boolean near
    ) {
        if (level == 1) {
            shapes.setColor(
                near ? .486f : .651f,
                near ? .733f : .843f,
                near ? .706f : .824f,
                1f
            );

        } else if (level == 2) {
            shapes.setColor(
                near ? .55f : .72f,
                near ? .74f : .86f,
                near ? .61f : .76f,
                1f
            );

        } else if (level == 3) {
            shapes.setColor(
                near ? .35f : .53f,
                near ? .47f : .63f,
                near ? .53f : .70f,
                1f
            );

        } else {
            shapes.setColor(
                near ? .50f : .65f,
                near ? .43f : .62f,
                near ? .46f : .66f,
                1f
            );
        }
    }

    private void trees(
        OrthographicCamera camera,
        int level,
        int layer
    ) {
        float w = camera.viewportWidth * camera.zoom;
        float left = camera.position.x - w / 2f;
        float right = left + w;

        float parallax =
            layer == 0 ? .17f :
                layer == 1 ? .42f : .70f;

        // Slightly wider spacing prevents excessive crowding.
        float spacing =
            layer == 0 ? 155f :
                layer == 1 ? 215f : 385f;

        float minHeight =
            layer == 0 ? 159f :
                layer == 1 ? 190f : 211f;

        float varyHeight =
            layer == 0 ? 56f :
                layer == 1 ? 61f : 49f;

        float alpha =
            layer == 0 ? .31f :
                layer == 1 ? .56f : .91f;

        float origin =
            camera.position.x * (1f - parallax);

        int first = (int)Math.floor(
            (left - origin) / spacing
        ) - 3;

        int last = (int)Math.ceil(
            (right - origin) / spacing
        ) + 3;

        for (int i = first; i <= last; i++) {
            float a = noise01(i, layer + 1);
            float b = noise01(i, layer + 17);
            float c = noise01(i, layer + 33);

            float cx = origin + i * spacing
                + (a - .5f) * spacing * .58f;

            float height = minHeight + varyHeight * b;

            // Maintain the actual cropped frame ratio.
            float width =
                height * TREE_SOURCE_W / TREE_SOURCE_H;

            float x = Math.round(cx - width / 2f);

            if (x + width < left - 20f
                || x > right + 20f) {
                continue;
            }

            int color = treeColor(level, layer, c);

            if (level == 3) {
                backgroundBatch.setColor(
                    .78f, .83f, .96f, alpha
                );

            } else if (level == 4) {
                backgroundBatch.setColor(
                    1f, .83f, .76f, alpha
                );

            } else if (layer == 0) {
                backgroundBatch.setColor(
                    .78f, 1f, 1f, alpha
                );

            } else if (layer == 1) {
                backgroundBatch.setColor(
                    .88f, .98f, .93f, alpha
                );

            } else {
                backgroundBatch.setColor(
                    .97f, 1f, .96f, alpha
                );
            }

            TextureRegion tree =
                noise01(i, layer + 51) > .5f
                    ? treeRight[color]
                    : treeLeft[color];

            // The actual trunk ends one pixel inside the grass.
            // Do not introduce random vertical offsets.
            backgroundBatch.draw(
                tree,
                x,
                GROUND_TOP - 1f,
                Math.round(width),
                Math.round(height)
            );
        }

        backgroundBatch.setColor(Color.WHITE);
    }

    private int treeColor(
        int level,
        int layer,
        float value
    ) {
        if (level == 1) {
            if (layer == 0) {
                return value < .75f ? 2 : 0;
            }

            if (layer == 1) {
                return value < .32f ? 2 : 0;
            }

            return value < .79f ? 0 : 1;
        }

        if (level == 2) {
            return value < (layer == 2 ? .44f : .31f)
                ? 1 : 0;
        }

        if (level == 3) {
            return value < .20f ? 3 : 2;
        }

        return value < .42f ? 3 : 2;
    }

    private static float noise01(int i, int seed) {
        int n = i * 1103515245 + seed * 12345;

        n ^= n >>> 16;
        n *= 0x85ebca77;
        n ^= n >>> 13;

        return (n & 0x7fffffff)
            / (float)Integer.MAX_VALUE;
    }

    private void fallback(
        ShapeRenderer shapes,
        OrthographicCamera camera,
        int level
    ) {
        float w = camera.viewportWidth * camera.zoom;
        float h = camera.viewportHeight * camera.zoom;

        float left = camera.position.x - w / 2f;
        float bottom = camera.position.y - h / 2f;

        if (level == 1) {
            shapes.setColor(.72f, .91f, .95f, 1f);
        } else if (level == 2) {
            shapes.setColor(.73f, .88f, .81f, 1f);
        } else if (level == 3) {
            shapes.setColor(.46f, .44f, .62f, 1f);
        } else {
            shapes.setColor(.55f, .41f, .45f, 1f);
        }

        shapes.rect(left, bottom, w, h);
    }

    public void drawBlocks(
        SpriteBatch batch,
        World world
    ) {
        Texture top;
        Texture inside;

        if (world.level == 4) {
            top = woodTop;
            inside = woodInside;

            batch.setColor(.82f, .68f, .65f, 1f);

        } else {
            top = grassTop;
            inside = dirt;

            if (world.level == 3) {
                batch.setColor(.73f, .75f, .84f, 1f);
            } else if (world.level == 2) {
                batch.setColor(.94f, 1f, .87f, 1f);
            } else {
                batch.setColor(Color.WHITE);
            }
        }

        for (Rectangle rect : world.blocks) {
            drawBlock(batch, rect, top, inside);
        }

        batch.setColor(Color.WHITE);
    }

    private void drawBlock(
        SpriteBatch batch,
        Rectangle block,
        Texture top,
        Texture inside
    ) {
        float cap = Math.min(TILE_SIZE, block.height);
        float body = block.height - cap;

        if (body > 0f) {
            tiled(
                batch,
                inside,
                block.x, block.y,
                block.width, body
            );
        }

        tiled(
            batch,
            top,
            block.x, block.y + body,
            block.width, cap
        );
    }

    private void tiled(
        SpriteBatch batch,
        Texture texture,
        float x,
        float y,
        float width,
        float height
    ) {
        for (float dx = 0f; dx < width; dx += TILE_SIZE) {
            float dw = Math.min(TILE_SIZE, width - dx);

            for (float dy = 0f; dy < height; dy += TILE_SIZE) {
                float dh = Math.min(TILE_SIZE, height - dy);

                int sw = Math.max(
                    1,
                    Math.min(
                        TILE_SOURCE,
                        Math.round(
                            dw / TILE_SIZE * TILE_SOURCE
                        )
                    )
                );

                int sh = Math.max(
                    1,
                    Math.min(
                        TILE_SOURCE,
                        Math.round(
                            dh / TILE_SIZE * TILE_SOURCE
                        )
                    )
                );

                TextureRegion tile = new TextureRegion(
                    texture,
                    0,
                    TILE_SOURCE - sh,
                    sw, sh
                );

                batch.draw(
                    tile,
                    x + dx,
                    y + dy,
                    dw, dh
                );
            }
        }
    }

    @Override
    public void dispose() {
        grassTop.dispose();
        dirt.dispose();
        woodTop.dispose();
        woodInside.dispose();

        if (skyTexture != null) {
            skyTexture.dispose();
        }

        if (treeTexture != null) {
            treeTexture.dispose();
        }

        backgroundBatch.dispose();
    }
}
