
package io.jbnu.test;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.utils.Disposable;

/**
 * 한글 픽셀 폰트 관리자.
 * assets/fonts/Galmuri11.ttf 사용.
 */
public final class GameFonts implements Disposable {

    public static final String FONT_PATH =
        "fonts/Galmuri11.ttf";

    private static final String DEATH_TEXT =
        "일어나세요!왕국을 지켜야만 해요!" +
            "여기서 쓰러질 순 없어요!" +
            "다시...다시 한 번...!포기할 수 없어요!";

    private final BitmapFont pixelFont;

    public GameFonts() {
        if (!Gdx.files.internal(FONT_PATH).exists()) {
            throw new IllegalStateException(
                "한글 픽셀 폰트가 없습니다: assets/" + FONT_PATH
            );
        }

        FreeTypeFontGenerator generator =
            new FreeTypeFontGenerator(
                Gdx.files.internal(FONT_PATH)
            );

        try {
            FreeTypeFontParameter params =
                new FreeTypeFontParameter();

            params.size = 12;

            // 필요한 한글을 폰트 아틀라스에 포함
            params.characters =
                FreeTypeFontGenerator.DEFAULT_CHARS
                    + DEATH_TEXT;

            params.minFilter =
                Texture.TextureFilter.Nearest;
            params.magFilter =
                Texture.TextureFilter.Nearest;

            params.genMipMaps = false;

            pixelFont = generator.generateFont(params);
            pixelFont.setUseIntegerPositions(true);

        } finally {
            generator.dispose();
        }
    }

    public BitmapFont getPixelFont() {
        return pixelFont;
    }

    @Override
    public void dispose() {
        pixelFont.dispose();
    }
}
