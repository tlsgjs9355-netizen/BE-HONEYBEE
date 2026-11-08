
package io.jbnu.test;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;

/** Shared Korean pixel-font renderer. A single font generator serves all UI screens. */
public final class GalmuriFonts implements Disposable {
    private static GalmuriFonts instance;

    private final FreeTypeFontGenerator generator;
    private final BitmapFont small;
    private final BitmapFont normal;
    private final BitmapFont large;
    private final GlyphLayout layout = new GlyphLayout();

    private GalmuriFonts() {
        FileHandle file = Gdx.files.internal("fonts/Galmuri11.ttf");
        if (!file.exists()) file = Gdx.files.internal("fonts/Galmuri14.ttf");
        if (!file.exists()) {
            throw new GdxRuntimeException(
                "Galmuri font missing: put Galmuri11.ttf in assets/fonts/"
            );
        }

        generator = new FreeTypeFontGenerator(file);
        small = generate(16);
        normal = generate(22);
        large = generate(32);
    }

    public static GalmuriFonts shared() {
        if (instance == null) instance = new GalmuriFonts();
        return instance;
    }

    private BitmapFont generate(int size) {
        FreeTypeFontParameter p = new FreeTypeFontParameter();
        p.size = size;
        p.incremental = true;
        p.minFilter = Texture.TextureFilter.Nearest;
        p.magFilter = Texture.TextureFilter.Nearest;
        p.characters = FreeTypeFontGenerator.DEFAULT_CHARS
            + "가나다라마바사아자차카타파하꿀벌이습니델";
        return generator.generateFont(p);
    }

    private BitmapFont choose(int size) {
        if (size <= 17) return small;
        if (size <= 25) return normal;
        return large;
    }

    public void draw(
        SpriteBatch batch,
        String text,
        float x,
        float baseline,
        int size,
        Color color
    ) {
        BitmapFont f = choose(size);
        f.setColor(color);
        f.draw(batch, text, Math.round(x), Math.round(baseline));
        f.setColor(Color.WHITE);
    }

    public void center(
        SpriteBatch batch,
        String text,
        float centerX,
        float baseline,
        int size,
        Color color
    ) {
        BitmapFont f = choose(size);
        layout.setText(f, text);
        draw(batch, text, centerX - layout.width / 2f,
            baseline, size, color);
    }

    public void wrap(
        SpriteBatch batch,
        String text,
        float x,
        float baseline,
        float width,
        int size,
        Color color
    ) {
        BitmapFont f = choose(size);
        layout.setText(f, text, color, width, Align.center, true);
        f.setColor(Color.WHITE);
        f.draw(batch, layout, x, baseline);
    }

    @Override
    public void dispose() {
        small.dispose();
        normal.dispose();
        large.dispose();
        generator.dispose();
    }

    public static void disposeShared() {
        if (instance != null) {
            instance.dispose();
            instance = null;
        }
    }
}
