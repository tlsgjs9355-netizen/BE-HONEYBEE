
package io.jbnu.test;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;

public class GameAssets implements Disposable {

    public final Texture beeFlight;
    public final Texture beeYellow;
    public final Texture spider;
    public final Texture spiderWeb;
    public final Texture flower;
    public final Texture honey;
    public final Texture honeycomb;
    public final Texture beehive;

    public final Animation<TextureRegion> beeAnimation;
    public final Music wingBuzz;

    private static final float BEE_FRAME_TIME = 0.035f;

    public GameAssets() {
        beeFlight = load("bee_flight.png");

        beeYellow = load("sprites/bee_yellow.png");
        spider = load("sprites/spider_black.png");
        spiderWeb = load("sprites/spider_web.png");
        flower = load("sprites/flower.png");
        honey = load("sprites/honey_jar.png");
        honeycomb = load("sprites/honeycomb.png");
        beehive = load("sprites/beehive.png");

        TextureRegion[][] frames =
            TextureRegion.split(beeFlight, 48, 40);

        beeAnimation = new Animation<>(
            BEE_FRAME_TIME,
            frames[0]
        );

        beeAnimation.setPlayMode(
            Animation.PlayMode.LOOP
        );

        if (Gdx.files.internal("wing_buzz.wav").exists()) {
            wingBuzz = Gdx.audio.newMusic(
                Gdx.files.internal("wing_buzz.wav")
            );

            wingBuzz.setLooping(true);
            wingBuzz.setVolume(0.18f);
        } else {
            wingBuzz = null;
        }
    }

    private Texture load(String path) {
        Texture texture = new Texture(
            Gdx.files.internal(path)
        );

        texture.setFilter(
            Texture.TextureFilter.Nearest,
            Texture.TextureFilter.Nearest
        );

        return texture;
    }

    @Override
    public void dispose() {
        if (wingBuzz != null) {
            wingBuzz.stop();
            wingBuzz.dispose();
        }

        beeFlight.dispose();
        beeYellow.dispose();
        spider.dispose();
        spiderWeb.dispose();
        flower.dispose();
        honey.dispose();
        honeycomb.dispose();
        beehive.dispose();
    }
}
