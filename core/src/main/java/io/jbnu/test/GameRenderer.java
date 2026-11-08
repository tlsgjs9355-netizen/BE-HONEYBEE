
package io.jbnu.test;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ScreenUtils;

public class GameRenderer implements Disposable {
    public static final float WIDTH = 800f;
    public static final float HEIGHT = 600f;

    private final GameAssets assets;
    private final TerrainArt terrain;
    private final PixelHud hud;
    private final ShapeRenderer shapes;
    private final SpriteBatch batch;
    private final MantisSprite mantisSprite;
    private final SpiderSprite spiderSprite;
    private final BeeSprite beeSprite;
    private final HornetSprite hornetSprite;

    /** Visual-only correction; physics and warning coordinates stay intact. */
    private static final float MANTIS_GROUND_IMAGE_DROP = 12f;
    private static final float MANTIS_GROUND_LEVEL = 40f;
    private final Matrix4 mantisOriginalTransform = new Matrix4();
    private final Matrix4 mantisShiftedTransform = new Matrix4();

    private final Texture mantisSlashTexture;
    private static final int FROG_CELL = 64;
    private static final int FROG_FRAME_COUNT = 5;
    private static final int FROG_JUMP_FRAME_COUNT = 10;
    private static final float FROG_SIZE = 96f;
    private static final float FROG_IDLE_FRAME_TIME = 0.16f;

    private final Texture frogIdleTexture;
    private final Texture frogAttackTexture;
    private final Texture frogJumpTexture;
    private final TextureRegion[] frogIdleRight;
    private final TextureRegion[] frogIdleLeft;
    private final TextureRegion[] frogAttackRight;
    private final TextureRegion[] frogAttackLeft;
    private final TextureRegion[] frogJumpRight;
    private final TextureRegion[] frogJumpLeft;

    // Stage 3 hornet bite: pixel-art jaw impact, four frames.
    private static final int HORNET_JAW_CELL = 128;
    private static final int HORNET_JAW_COUNT = 4;
    private static final String HORNET_JAW_PATH =
        "effects/hornet_jaw_scissor.png";

    private final Texture hornetJawTexture;
    private final TextureRegion[] hornetJawRight;
    private final TextureRegion[] hornetJawLeft;

    private final Texture mantisClawTexture;
    private final TextureRegion[] mantisClawFrames;

    public GameRenderer(GameAssets assets) {
        this.assets = assets;
        terrain = new TerrainArt();
        hud = new PixelHud(assets);
        shapes = new ShapeRenderer();
        batch = new SpriteBatch();
        mantisSprite = new MantisSprite();
        spiderSprite = new SpiderSprite();
        beeSprite = new BeeSprite();
        hornetSprite = new HornetSprite();

        mantisSlashTexture = loadSlashTexture();

        FileHandle frogIdleFile = Gdx.files.internal(
            "sprites/frogs/frog_green_idle.png"
        );
        FileHandle frogAttackFile = Gdx.files.internal(
            "sprites/frogs/frog_green_attack.png"
        );
        FileHandle frogJumpFile = Gdx.files.internal(
            "sprites/frogs/frog_green_jump.png"
        );

        if (!frogIdleFile.exists() || !frogAttackFile.exists()
            || !frogJumpFile.exists()) {
            throw new IllegalStateException(
                "초록색 개구리 PNG 3개가 필요합니다: "
                    + "assets/sprites/frogs/frog_green_idle.png, "
                    + "frog_green_attack.png, frog_green_jump.png"
            );
        }

        frogIdleTexture = new Texture(frogIdleFile);
        frogAttackTexture = new Texture(frogAttackFile);
        frogJumpTexture = new Texture(frogJumpFile);

        if (frogIdleTexture.getWidth() != 320
            || frogIdleTexture.getHeight() != 64
            || frogAttackTexture.getWidth() != 320
            || frogAttackTexture.getHeight() != 64
            || frogJumpTexture.getWidth() != 640
            || frogJumpTexture.getHeight() != 64) {
            throw new IllegalStateException(
                "개구리 대기/공격은 320x64, 점프는 640x64여야 합니다."
            );
        }

        frogIdleTexture.setFilter(
            Texture.TextureFilter.Nearest,
            Texture.TextureFilter.Nearest
        );
        frogAttackTexture.setFilter(
            Texture.TextureFilter.Nearest,
            Texture.TextureFilter.Nearest
        );
        frogJumpTexture.setFilter(
            Texture.TextureFilter.Nearest,
            Texture.TextureFilter.Nearest
        );

        frogIdleRight = frogFrames(frogIdleTexture, false);
        frogIdleLeft = frogFrames(frogIdleTexture, true);
        frogAttackRight = frogFrames(frogAttackTexture, false);
        frogAttackLeft = frogFrames(frogAttackTexture, true);
        frogJumpRight = frogFrames(
            frogJumpTexture, false, FROG_JUMP_FRAME_COUNT
        );
        frogJumpLeft = frogFrames(
            frogJumpTexture, true, FROG_JUMP_FRAME_COUNT
        );

        // 새 이미지가 없으면 기존 선 기반 물기 이펙트를 사용한다.
        FileHandle jawFile = Gdx.files.internal(HORNET_JAW_PATH);

        if (jawFile.exists()) {
            hornetJawTexture = new Texture(jawFile);

            if (hornetJawTexture.getWidth()
                != HORNET_JAW_CELL * HORNET_JAW_COUNT
                || hornetJawTexture.getHeight() != HORNET_JAW_CELL) {
                throw new IllegalStateException(
                    "말벌 큰 턱 이미지 크기는 512x128이어야 합니다: "
                        + HORNET_JAW_PATH
                );
            }

            hornetJawTexture.setFilter(
                Texture.TextureFilter.Nearest,
                Texture.TextureFilter.Nearest
            );

            // 원본은 왼쪽을 향함. 오른쪽 공격에 좌우 반전.
            hornetJawRight = jawRegions(hornetJawTexture, true);
            hornetJawLeft = jawRegions(hornetJawTexture, false);
        } else {
            hornetJawTexture = null;
            hornetJawRight = null;
            hornetJawLeft = null;
        }

        FileHandle clawFile = Gdx.files.internal(
            "effects/mantis_claw_sweep.png"
        );

        mantisClawTexture =
            clawFile.exists() ? new Texture(clawFile) : null;

        if (mantisClawTexture != null) {
            mantisClawTexture.setFilter(
                Texture.TextureFilter.Nearest,
                Texture.TextureFilter.Nearest
            );

            mantisClawFrames = new TextureRegion[8];

            for (int i = 0; i < mantisClawFrames.length; i++) {
                mantisClawFrames[i] = new TextureRegion(
                    mantisClawTexture,
                    i * 96, 0, 96, 112
                );
            }
        } else {
            mantisClawFrames = null;
        }
    }

    private static TextureRegion[] frogFrames(
        Texture texture, boolean flipX
    ) {
        return frogFrames(texture, flipX, FROG_FRAME_COUNT);
    }

    private static TextureRegion[] frogFrames(
        Texture texture, boolean flipX, int count
    ) {
        TextureRegion[] frames = new TextureRegion[count];

        for (int i = 0; i < frames.length; i++) {
            frames[i] = new TextureRegion(
                texture,
                i * FROG_CELL, 0,
                FROG_CELL, FROG_CELL
            );

            if (flipX) frames[i].flip(true, false);
        }

        return frames;
    }

    private static TextureRegion[] jawRegions(
        Texture texture, boolean flipX
    ) {
        TextureRegion[] regions = new TextureRegion[HORNET_JAW_COUNT];

        for (int i = 0; i < HORNET_JAW_COUNT; i++) {
            regions[i] = new TextureRegion(
                texture,
                i * HORNET_JAW_CELL, 0,
                HORNET_JAW_CELL, HORNET_JAW_CELL
            );

            if (flipX) regions[i].flip(true, false);
        }

        return regions;
    }

    public void render(
        World world,
        OrthographicCamera camera,
        float animationTime,
        float endingTime,
        boolean paused,
        boolean ending
    ) {
        ScreenUtils.clear(Color.BLACK);

        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        terrain.drawBackground(shapes, camera, world.level);
        shapes.end();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        drawDecor(world);
        terrain.drawBlocks(batch, world);
        drawHoney(world);
        drawSpiders(world, animationTime);
        drawFrogSprites(world, animationTime);
        drawExit(world);

        batch.end();

        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        drawSpikes(world);
        drawFrogEffects(world);
        drawGate(world);
        drawBoss(world, animationTime);
        drawProjectiles(world);
        drawSwarm(world, animationTime);

        shapes.end();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        Enemy activeBoss = world.getActiveBoss();

        if (activeBoss instanceof Mantis && !activeBoss.isDead()) {
            Mantis mantis = (Mantis) activeBoss;
            drawMantisSlashSprites(mantis);
            drawGroundAlignedMantis(mantis, animationTime);
            drawMantisAttackEffects(mantis, animationTime);

        } else if (activeBoss instanceof Hornet
            || activeBoss instanceof GiantHornet) {

            hornetSprite.draw(
                batch,
                activeBoss,
                world.player.bounds,
                animationTime
            );
        }

        drawBee(world, animationTime);

        // 턱 스프라이트를 보스 및 플레이어 위에 그린다.
        if (activeBoss instanceof Hornet
            && !activeBoss.isDead()
            && hornetJawTexture != null) {
            drawHornetScissorSprite(
                (Hornet) activeBoss, animationTime
            );
        }

        batch.end();

        // 새 PNG가 없을 때만 이전 이펙트 사용
        if (activeBoss instanceof Hornet
            && !activeBoss.isDead()
            && hornetJawTexture == null) {
            shapes.setProjectionMatrix(camera.combined);
            shapes.begin(ShapeRenderer.ShapeType.Filled);
            drawHornetBiteChomp((Hornet) activeBoss);
            shapes.end();
        }

        if (world.isCaptured()) {
            shapes.setProjectionMatrix(camera.combined);
            shapes.begin(ShapeRenderer.ShapeType.Filled);
            drawCaptureOverlay(world, animationTime);

            if (world.isGiantHornetCaptured()) {
                drawGiantGrabOverlay(world, animationTime);
            }

            shapes.end();
        }

        hud.render(world, endingTime, paused, ending);
    }

    /**
     * 사마귀의 몸체 이미지만 아래로 보정한다.
     * 실제 물리/충돌 위치는 변경하지 않는다.
     */
    private void drawGroundAlignedMantis(
        Mantis mantis, float time
    ) {
        float airborne = MathUtils.clamp(
            (mantis.bounds.y - MANTIS_GROUND_LEVEL) / 40f,
            0f, 1f
        );

        float drop = Math.round(
            MANTIS_GROUND_IMAGE_DROP * (1f - airborne)
        );

        if (drop <= 0f) {
            mantisSprite.draw(batch, mantis, time);
            return;
        }

        mantisOriginalTransform.set(batch.getTransformMatrix());
        mantisShiftedTransform.set(mantisOriginalTransform);
        mantisShiftedTransform.translate(0f, -drop, 0f);

        batch.setTransformMatrix(mantisShiftedTransform);
        try {
            mantisSprite.draw(batch, mantis, time);
        } finally {
            batch.setTransformMatrix(mantisOriginalTransform);
        }
    }

    private void drawDecor(World world) {
        for (
            float x = 92f;
            x < world.worldWidth - 70f;
            x += 151f
        ) {
            if (world.level == 4 && x < 1810f) continue;

            boolean supported = false;

            for (Rectangle ground : world.blocks) {
                if (
                    ground.y <= 0.1f
                        && ground.y + ground.height >= 39f
                        && x >= ground.x + 16f
                        && x <= ground.x + ground.width - 16f
                ) {
                    supported = true;
                    break;
                }
            }

            if (!supported) continue;

            boolean danger = false;

            for (Rectangle spike : world.spikes) {
                if (
                    x >= spike.x - 55f
                        && x <= spike.x + spike.width + 55f
                        && spike.y <= 105f
                ) {
                    danger = true;
                    break;
                }
            }

            for (SpiderTrap spider : world.spiders) {
                if (Math.abs(spider.getAnchorX() - x) < 48f) {
                    danger = true;
                }
            }

            if (danger) continue;

            int index = (int)(x / 151f);
            float size = 27f + (index % 4) * 4f;

            if (world.level == 3) {
                batch.setColor(.79f, .81f, .91f, .83f);
            } else if (world.level == 4) {
                batch.setColor(.85f, .74f, .62f, .68f);
            } else if (index % 3 == 0) {
                batch.setColor(1f, .76f, .84f, .94f);
            } else if (index % 3 == 1) {
                batch.setColor(.90f, .93f, .70f, .95f);
            } else {
                batch.setColor(.78f, .89f, 1f, .95f);
            }

            batch.draw(assets.flower, x, 39f, size, size);

            if (index % 4 == 1) {
                batch.setColor(1f, .86f, .69f, .86f);
                batch.draw(
                    assets.flower,
                    x + 33f, 39f,
                    size * .65f, size * .65f
                );
            }
        }

        batch.setColor(Color.WHITE);

        if (world.level >= 3) {
            for (
                float x = 440f;
                x < world.worldWidth - 140f;
                x += 680f
            ) {
                batch.setColor(1f, 1f, 1f, .50f);
                batch.draw(
                    assets.honeycomb,
                    x, 405f, 38f, 38f
                );
            }
        }

        batch.setColor(Color.WHITE);
    }

    private void drawHoney(World world) {
        batch.setColor(Color.WHITE);

        for (Rectangle r : world.honeys) {
            batch.draw(
                assets.honey,
                r.x - 5f,
                r.y - 5f,
                r.width + 10f,
                r.height + 10f
            );
        }
    }

    private void drawSpiders(World world, float time) {
        SpiderTrap capturedSpider = capturingSpider(world);

        for (SpiderTrap spider : world.spiders) {
            Rectangle r = spider.bounds;
            float cx = r.x + r.width / 2f;

            batch.setColor(1f, 1f, 1f, .68f);
            batch.draw(
                assets.spiderWeb,
                cx - 24f,
                spider.getAnchorY() - 20f,
                48f, 48f
            );

            batch.setColor(Color.WHITE);

            spiderSprite.draw(
                batch,
                spider,
                time,
                spider == capturedSpider
            );
        }

        batch.setColor(Color.WHITE);
    }

    // 거미줄은 SpiderSprite에서 렌더링한다.

    private void drawFrogSprites(World world, float time) {
        batch.setColor(Color.WHITE);

        for (GreenFrog frog : world.frogs) {
            boolean right = frog.isFacingRight();
            TextureRegion[] frames;
            int index;

            if (frog.getState() == GreenFrog.State.HOP) {
                frames = right ? frogJumpRight : frogJumpLeft;
                float progress = frog.getHopProgress();

                if (progress < 0.18f) {
                    index = MathUtils.clamp(
                        (int)(progress / .18f * 5f), 0, 4
                    );
                } else if (progress < .50f) {
                    index = 5;
                } else if (progress < .82f) {
                    index = 6;
                } else {
                    index = 7 + MathUtils.clamp(
                        (int)((progress - .82f) / .18f * 3f),
                        0, 2
                    );
                }
            } else if (frog.getState() == GreenFrog.State.WARNING) {
                frames = right ? frogAttackRight : frogAttackLeft;
                index = MathUtils.clamp(
                    (int)(frog.getWarningProgress() * FROG_FRAME_COUNT),
                    0, FROG_FRAME_COUNT - 1
                );
            } else if (frog.getState() == GreenFrog.State.STRIKE) {
                frames = right ? frogAttackRight : frogAttackLeft;
                index = FROG_FRAME_COUNT - 1;
            } else {
                frames = right ? frogIdleRight : frogIdleLeft;
                index = Math.max(
                    0, (int)(time / FROG_IDLE_FRAME_TIME)
                ) % FROG_FRAME_COUNT;
            }

            batch.draw(
                frames[index],
                Math.round(frog.bounds.x - 24f),
                Math.round(frog.bounds.y - 1f),
                FROG_SIZE,
                FROG_SIZE
            );
        }

        batch.setColor(Color.WHITE);
    }

    private void drawFrogEffects(World world) {
        for (GreenFrog frog : world.frogs) {
            if (frog.getState() == GreenFrog.State.WARNING) {
                float alpha = .32f + .5f * frog.getWarningProgress();

                shapes.setColor(.98f, .76f, .55f, alpha);

                float x1 = frog.mouthX();
                float y1 = frog.mouthY();
                float x2 = frog.warningTipX();
                float y2 = frog.warningTipY();
                float dx = x2 - x1;
                float dy = y2 - y1;

                for (int i = 1; i <= 12; i += 2) {
                    float a = i / 12f;
                    float b = (i + 1f) / 12f;

                    shapes.rectLine(
                        x1 + dx * a,
                        y1 + dy * a,
                        x1 + dx * b,
                        y1 + dy * b,
                        2.5f
                    );
                }

            } else if (frog.getState() == GreenFrog.State.STRIKE) {
                float x1 = frog.mouthX();
                float y1 = frog.mouthY();
                float x2 = frog.tipX();
                float y2 = frog.tipY();

                shapes.setColor(.19f, .10f, .12f, 1f);
                shapes.rectLine(x1, y1, x2, y2, 7f);

                shapes.setColor(.74f, .37f, .39f, 1f);
                shapes.rectLine(x1, y1, x2, y2, 4.5f);

                shapes.setColor(.87f, .51f, .49f, 1f);
                shapes.circle(x2, y2, 4.5f, 10);
            }
        }
    }

    private void drawExit(World world) {
        Rectangle r = world.exit;

        batch.setColor(
            world.isExitUnlocked()
                ? Color.WHITE
                : Color.DARK_GRAY
        );

        batch.draw(
            assets.beehive,
            r.x - 16f,
            r.y - 16f,
            r.width + 32f,
            r.height + 32f
        );

        batch.setColor(Color.WHITE);
    }

    private SpiderTrap capturingSpider(World world) {
        if (!world.isCaptured()) return null;

        for (SpiderTrap spider : world.spiders) {
            if (spider.bounds.overlaps(world.player.bounds)) {
                return spider;
            }
        }

        return null;
    }

    private void drawBee(World world, float time) {
        beeSprite.draw(
            batch,
            world,
            time,
            capturingSpider(world)
        );
    }

    private void drawCaptureOverlay(World world, float time) {
        SpiderTrap spider = capturingSpider(world);
        if (spider == null) return;

        float cx = spider.bounds.x + spider.bounds.width / 2f;
        float cy = spider.bounds.y + spider.bounds.height / 2f;

        float looseness = MathUtils.clamp(
            world.getEscapeHits() / 3f,
            0f,
            1f
        );

        float twitch = MathUtils.sin(time * 13f)
            * (4f - looseness * 2f);
        float reach = 24f - looseness * 8f;

        shapes.setColor(.13f, .09f, .17f, 1f);

        shapes.rectLine(
            cx - 12f, cy - 5f,
            cx - reach + twitch, cy - 24f,
            5f
        );
        shapes.rectLine(
            cx + 12f, cy - 5f,
            cx + reach - twitch, cy - 24f,
            5f
        );

        shapes.rectLine(
            cx - reach + twitch, cy - 24f,
            cx - 12f - looseness * 6f, cy - 34f,
            4f
        );
        shapes.rectLine(
            cx + reach - twitch, cy - 24f,
            cx + 12f + looseness * 6f, cy - 34f,
            4f
        );

        shapes.setColor(.85f, .90f, .95f, 1f);
        float webY = cy - 18f;

        for (int i = 0; i < 3; i++) {
            float sway = MathUtils.sin(time * 8f + i) * 2f;
            float half = 21f - looseness * 9f;

            shapes.rectLine(
                cx - half + sway,
                webY - i * 9f,
                cx + half + sway,
                webY - i * 9f + 5f,
                2f
            );
        }
    }

    private void drawGiantGrabOverlay(World world, float time) {
        Rectangle bee = world.player.bounds;
        float x = bee.x + bee.width * .5f;
        float y = bee.y + bee.height * .5f;
        float twitch = MathUtils.sin(time * 14f) * 2f;

        shapes.setColor(.20f, .14f, .10f, .98f);
        shapes.rectLine(x - 44f, y + 25f, x - 18f, y + 13f, 9f);
        shapes.rectLine(x - 18f, y + 13f, x - 8f, y + twitch, 8f);
        shapes.rectLine(x + 44f, y - 25f, x + 18f, y - 13f, 9f);
        shapes.rectLine(x + 18f, y - 13f, x + 8f, y - twitch, 8f);

        shapes.setColor(1f, .69f, .26f, 1f);
        shapes.rectLine(x - 43f, y + 25f, x - 18f, y + 13f, 5f);
        shapes.rectLine(x - 18f, y + 13f, x - 8f, y + twitch, 5f);
        shapes.rectLine(x + 43f, y - 25f, x + 18f, y - 13f, 5f);
        shapes.rectLine(x + 18f, y - 13f, x + 8f, y - twitch, 5f);

        shapes.setColor(.25f, .19f, .12f, 1f);
        shapes.rectLine(x - 8f, y + twitch, x + 2f, y + 6f, 3f);
        shapes.rectLine(x + 8f, y - twitch, x - 2f, y - 6f, 3f);
    }

    private void drawSpikes(World world) {
        ThornVineArt.draw(shapes, world.spikes);
    }

    private void drawGate(World world) {
        if (world.level == 1) return;
        if (world.isArenaEntered() && world.isArenaCleared()) return;

        if (world.isArenaEntered() || !world.isArenaGateOpen()) {
            float x = world.level == 2 ? 2394f
                : world.level == 3 ? 2194f : 1794f;

            shapes.setColor(Color.FIREBRICK);
            shapes.rect(x, 40f, 6f, 560f);
        }
    }

    private void drawBoss(World world, float animationTime) {
        Enemy boss = world.getActiveBoss();
        if (boss == null || boss.isDead()) return;

        Rectangle r = boss.bounds;

        if (boss.getState() == Enemy.State.TELEGRAPH) {
            if (boss instanceof Mantis) {
                Mantis m = (Mantis) boss;

                if (m.isClawWarningVisible()) {
                    drawMantisClawWarning(m);
                }
                if (m.isLeapWarningVisible()) {
                    drawMantisLeapWarning(m);
                }
                if (m.isSlashWarningVisible(false)) {
                    drawMantisSlashWarning(m, false);
                }
                if (m.isSlashWarningVisible(true)) {
                    drawMantisSlashWarning(m, true);
                }

            } else if (boss instanceof Hornet) {
                Hornet h = (Hornet) boss;

                float x1 = h.getAttackStart().x;
                float y1 = h.getAttackStart().y;
                float x2 = h.getTarget().x;
                float y2 = h.getTarget().y;
                float alpha = .45f + .45f * h.getWarningProgress();

                if (h.getPattern() == Hornet.Pattern.DASH) {
                    drawBossDashWarning(
                        x1, y1, x2, y2,
                        1f, .30f, .27f, alpha,
                        2200f, 3000f
                    );
                } else if (h.getPattern() == Hornet.Pattern.BITE) {
                    drawBossDashWarning(
                        x1, y1, x2, y2,
                        1f, .70f, .25f, alpha,
                        2200f, 3000f
                    );
                } else {
                    drawBossDashWarning(
                        x1, y1, x2, y2,
                        1f, .40f, .46f, alpha,
                        2200f, 3000f
                    );
                }

            } else if (boss instanceof GiantHornet) {
                GiantHornet g = (GiantHornet) boss;
                float alpha = .47f + .45f * g.getWarningProgress();

                if (g.getPattern() == GiantHornet.Pattern.CHARGE) {
                    drawBossDashWarning(
                        g.getAttackStart().x,
                        g.getAttackStart().y,
                        g.getTarget().x,
                        g.getTarget().y,
                        1f, .36f, .30f, alpha,
                        1800f, 2600f
                    );
                } else if (g.getPattern() == GiantHornet.Pattern.GRAB) {
                    drawBossDashWarning(
                        g.getAttackStart().x,
                        g.getAttackStart().y,
                        g.getTarget().x,
                        g.getTarget().y,
                        1f, .67f, .27f, alpha,
                        1800f, 2600f
                    );
                } else {
                    drawGiantRadialWarning(g, alpha);
                }
            }
        }

        if (boss instanceof Mantis) {
            Mantis m = (Mantis) boss;

            if (boss.getState() != Enemy.State.TELEGRAPH) {
                if (m.isSlashWarningVisible(false)) {
                    drawMantisSlashWarning(m, false);
                }
                if (m.isSlashWarningVisible(true)) {
                    drawMantisSlashWarning(m, true);
                }
            }

            if (m.isClawDashing()) {
                drawMantisDashTrail(m);
            }
        }

        if (boss instanceof Hornet) {
            drawHornetAttackEffect((Hornet) boss);
        }

        if (boss instanceof GiantHornet) {
            drawGiantAttackEffect((GiantHornet) boss);
        }

        shapes.setColor(Color.DARK_GRAY);
        shapes.rect(
            r.x,
            r.y + r.height + 10f,
            r.width,
            8f
        );

        shapes.setColor(Color.RED);
        shapes.rect(
            r.x,
            r.y + r.height + 10f,
            r.width * boss.getHp() / boss.getMaxHp(),
            8f
        );
    }

    private void drawGiantRadialWarning(
        GiantHornet g, float alpha
    ) {
        int count = g.getStingCount();
        float cx = g.centerX();
        float cy = g.centerY();

        shapes.setColor(1f, .38f, .62f, alpha);

        for (int i = 0; i < count; i++) {
            float angle = g.getVolleyAngle()
                + MathUtils.PI2 * i / count;
            float dx = MathUtils.cos(angle);
            float dy = MathUtils.sin(angle);

            for (int radius = 60; radius < 340; radius += 27) {
                float x1 = cx + dx * radius;
                float y1 = cy + dy * radius;
                float x2 = cx + dx * (radius + 11);
                float y2 = cy + dy * (radius + 11);

                if (
                    x1 >= 1800f && x1 <= 2600f
                        && y1 >= 40f && y1 <= 600f
                ) {
                    shapes.rectLine(x1, y1, x2, y2, 2.4f);
                }
            }
        }
    }

    private void drawHornetAttackEffect(Hornet h) {
        if (h.getState() != Enemy.State.ATTACK
            || h.getPattern() != Hornet.Pattern.DASH) {
            return;
        }

        float x = h.centerX();
        float y = h.centerY();
        float ux = h.getDirection().x;
        float uy = h.getDirection().y;

        shapes.setColor(1f, .86f, .54f, .43f);

        for (int i = 1; i <= 3; i++) {
            shapes.rect(
                x - ux * (27f + i * 16f) - 3f,
                y - uy * (27f + i * 16f) - 2f,
                10f - i,
                4f
            );
        }
    }

    /**
     * 큰 턱 벌리기 → 닫힘 → 충격 → 파편 애니메이션.
     * 실제 피해 판정은 Hornet과 World에서 처리한다.
     */
    private void drawHornetScissorSprite(
        Hornet hornet, float time
    ) {
        if (hornet.getPattern() != Hornet.Pattern.BITE) return;

        Enemy.State state = hornet.getState();
        if (state != Enemy.State.ATTACK
            && state != Enemy.State.TELEGRAPH) {
            return;
        }

        boolean right = hornet.getFacing() > 0;
        TextureRegion[] frames =
            right ? hornetJawRight : hornetJawLeft;

        int index;
        float opacity = 1f;
        float p = hornet.getAttackProgress();

        if (state == Enemy.State.TELEGRAPH) {
            index = 0;
            opacity = .40f + .18f
                * (1f + MathUtils.sin(time * 19f)) * .5f;

        } else if (p < .15f) {
            index = 0;
        } else if (p < .50f) {
            index = 1;
        } else if (p < .76f) {
            index = 2;
        } else {
            index = 3;
            opacity = MathUtils.clamp(
                (1f - p) / .24f, 0f, 1f
            );
        }

        float size = 82f;

        float headX = right
            ? hornet.bounds.x + hornet.bounds.width - 8f
            : hornet.bounds.x + 8f;

        float centerX = headX + (right ? 24f : -24f);
        float centerY = hornet.centerY() + 1f;

        batch.setColor(1f, 1f, 1f, opacity);

        batch.draw(
            frames[index],
            Math.round(centerX - size * .5f),
            Math.round(centerY - size * .5f),
            size, size
        );

        batch.setColor(Color.WHITE);
    }

    /**
     * PNG 미설치 시 기존 턱 공격 연출.
     */
    private void drawHornetBiteChomp(Hornet h) {
        if (h.getState() != Enemy.State.ATTACK
            || h.getPattern() != Hornet.Pattern.BITE) {
            return;
        }

        float p = h.getAttackProgress();
        if (p < 0.26f || p > 0.74f) return;

        int side = h.getFacing();
        float headX = side > 0
            ? h.bounds.x + h.bounds.width - 8f
            : h.bounds.x + 8f;

        float centerY = h.centerY() + 2f;
        float open;
        float reach;

        if (p < 0.42f) {
            float t = (p - 0.26f) / 0.16f;
            open = MathUtils.lerp(4f, 12f, t);
            reach = MathUtils.lerp(8f, 18f, t);
        } else if (p < 0.55f) {
            float t = (p - 0.42f) / 0.13f;
            open = MathUtils.lerp(12f, 1.8f, t);
            reach = MathUtils.lerp(18f, 22f, t);
        } else {
            float t = (p - 0.55f) / 0.19f;
            open = MathUtils.lerp(1.8f, 5f, t);
            reach = MathUtils.lerp(22f, 11f, t);
        }

        float midX = headX + side * (reach * 0.55f);
        float tipX = headX + side * reach;
        float upperY = centerY + open;
        float lowerY = centerY - open;

        shapes.setColor(.23f, .16f, .10f, 1f);
        shapes.rectLine(headX, centerY + 4f, midX, upperY, 5f);
        shapes.rectLine(headX, centerY - 4f, midX, lowerY, 5f);

        shapes.setColor(1f, .73f, .25f, 1f);
        shapes.rectLine(
            headX + side * 2f, centerY + 4f,
            tipX, upperY, 2.4f
        );
        shapes.rectLine(
            headX + side * 2f, centerY - 4f,
            tipX, lowerY, 2.4f
        );

        shapes.setColor(1f, .95f, .78f, 1f);
        shapes.rect(tipX - 1.5f, upperY - 2f, 3f, 3f);
        shapes.rect(tipX - 1.5f, lowerY - 1f, 3f, 3f);

        if (p >= 0.47f && p <= 0.58f) {
            float flash = p <= 0.52f
                ? (p - 0.47f) / 0.05f
                : (0.58f - p) / 0.06f;

            flash = MathUtils.clamp(flash, 0f, 1f);
            float fx = tipX + side * 5f;

            shapes.setColor(1f, .97f, .82f, flash);
            shapes.rect(fx - 1.5f, centerY - 1.5f, 3f, 3f);
            shapes.rect(
                fx + side * 5f - 1.5f,
                centerY + 4f, 3f, 3f
            );
            shapes.rect(
                fx + side * 5f - 1.5f,
                centerY - 7f, 3f, 3f
            );
        }
    }

    private void drawGiantAttackEffect(GiantHornet g) {
        if (g.getState() == Enemy.State.ATTACK) {
            float x = g.centerX();
            float y = g.centerY();
            float p = g.getAttackProgress();

            if (g.getPattern() == GiantHornet.Pattern.CHARGE) {
                float dx = g.getDirection().x;
                float dy = g.getDirection().y;

                for (int i = 1; i <= 4; i++) {
                    shapes.setColor(
                        1f, .56f, .25f,
                        .55f - i * .09f
                    );

                    shapes.rect(
                        x - dx * (40f + i * 16f) - 8f,
                        y - dy * (40f + i * 16f) - 5f,
                        15f - i * 2f,
                        8f
                    );
                }

            } else if (g.getPattern() == GiantHornet.Pattern.GRAB) {
                float t = MathUtils.clamp(
                    (p - .13f) / .52f, 0f, 1f
                );
                float closing = MathUtils.clamp(
                    (t - .37f) / .40f, 0f, 1f
                );
                float open = MathUtils.lerp(28f, 6f, closing);
                float extension = MathUtils.lerp(12f, 52f, t);
                int side = g.getFacing();

                float anchorX = x + side * 23f;
                float elbowX = anchorX + side * (extension * .65f);
                float tipX = anchorX + side * extension;

                shapes.setColor(.23f, .16f, .10f, 1f);
                shapes.rectLine(
                    anchorX, y + 19f,
                    elbowX, y + open + 11f, 10f
                );
                shapes.rectLine(
                    elbowX, y + open + 11f,
                    tipX, y + open, 8f
                );
                shapes.rectLine(
                    anchorX, y - 19f,
                    elbowX, y - open - 11f, 10f
                );
                shapes.rectLine(
                    elbowX, y - open - 11f,
                    tipX, y - open, 8f
                );

                shapes.setColor(.96f, .67f, .26f, 1f);
                shapes.rectLine(
                    anchorX, y + 19f,
                    elbowX, y + open + 11f, 6f
                );
                shapes.rectLine(
                    elbowX, y + open + 11f,
                    tipX, y + open, 4.5f
                );
                shapes.rectLine(
                    anchorX, y - 19f,
                    elbowX, y - open - 11f, 6f
                );
                shapes.rectLine(
                    elbowX, y - open - 11f,
                    tipX, y - open, 4.5f
                );

                shapes.setColor(.20f, .15f, .11f, 1f);
                shapes.rectLine(
                    tipX, y + open,
                    tipX - side * 9f, y + open - 8f, 3f
                );
                shapes.rectLine(
                    tipX, y - open,
                    tipX - side * 9f, y - open + 8f, 3f
                );

                if (closing >= .82f) {
                    shapes.setColor(1f, .93f, .70f, .72f);
                    shapes.rectLine(
                        tipX, y + 7f,
                        tipX + side * 4f, y, 2.3f
                    );
                    shapes.rectLine(
                        tipX, y - 7f,
                        tipX + side * 4f, y, 2.3f
                    );
                }

            } else if (g.getPattern() == GiantHornet.Pattern.STING) {
                shapes.setColor(.98f, .48f, .25f, .76f);

                float radius = 40f
                    + MathUtils.sin(p * MathUtils.PI) * 12f;

                shapes.circle(x, y, radius, 30);
            }
        }

        for (GiantHornet.Sting sting : g.getStings()) {
            Rectangle r = sting.bounds;
            float cx = r.x + r.width / 2f;
            float cy = r.y + r.height / 2f;

            float length = (float)Math.sqrt(
                sting.vx * sting.vx + sting.vy * sting.vy
            );
            if (length < .001f) continue;

            float dx = sting.vx / length;
            float dy = sting.vy / length;

            shapes.setColor(1f, .84f, .43f, 1f);
            shapes.rectLine(
                cx - dx * 8f, cy - dy * 8f,
                cx + dx * 9f, cy + dy * 9f,
                4f
            );

            shapes.setColor(.30f, .19f, .13f, 1f);
            shapes.rectLine(
                cx + dx * 8f, cy + dy * 8f,
                cx + dx * 13f, cy + dy * 13f,
                2f
            );
        }
    }

    private void drawBossDashWarning(
        float x1, float y1,
        float x2, float y2,
        float red, float green, float blue,
        float alpha,
        float minX, float maxX
    ) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float)Math.sqrt(dx * dx + dy * dy);

        if (length < .01f) return;

        float ux = dx / length;
        float uy = dy / length;

        shapes.setColor(
            red, green, blue,
            Math.min(.72f, alpha)
        );

        for (float d = 8f; d < length; d += 20f) {
            float to = Math.min(length, d + 8f);

            float ax = x1 + ux * d;
            float ay = y1 + uy * d;
            float bx = x1 + ux * to;
            float by = y1 + uy * to;

            if (
                ax < minX || ax > maxX
                    || ay < 40f || ay > 598f
            ) {
                continue;
            }

            shapes.rectLine(ax, ay, bx, by, 2f);
        }
    }

    private void drawMantisLeapWarning(Mantis mantis) {
        float progress = mantis.getLeapWarningProgress();

        shapes.setColor(
            .82f, .63f, 1f,
            .40f + .43f * progress
        );

        for (int i = 0; i < 28; i += 2) {
            float p1 = (i + .20f) / 28f;
            float p2 = (i + 1.23f) / 28f;

            shapes.rectLine(
                mantis.getLeapWarningX(p1),
                mantis.getLeapWarningY(p1),
                mantis.getLeapWarningX(p2),
                mantis.getLeapWarningY(p2),
                3f
            );
        }

        float x = mantis.getLeapWarningX(1f);
        float y = mantis.getLeapWarningY(1f);
        float radius = 8f + progress * 4f;

        shapes.rectLine(
            x - radius, y,
            x + radius, y,
            2.5f
        );
        shapes.rectLine(
            x, y - radius,
            x, y + radius,
            2.5f
        );
    }

    private void drawMantisClawWarning(Mantis mantis) {
        float startX = mantis.getClawWarningStartX();
        float startY = mantis.getClawWarningStartY();
        float endX = mantis.getClawWarningEndX();
        float endY = mantis.getClawWarningEndY();

        float dx = endX - startX;
        float dy = endY - startY;
        float length = (float)Math.sqrt(dx * dx + dy * dy);

        if (length < .01f) return;

        float ux = dx / length;
        float uy = dy / length;
        float progress = mantis.getClawWarningProgress();

        shapes.setColor(
            1f, .68f, .22f,
            .38f + .42f * progress
        );

        for (float d = 18f; d < length; d += 23f) {
            float far = Math.min(d + 9f, length);

            shapes.rectLine(
                startX + ux * d,
                startY + uy * d,
                startX + ux * far,
                startY + uy * far,
                3f
            );
        }

        float radius = 7f + 4f * progress;

        shapes.rectLine(
            endX - radius, endY,
            endX + radius, endY,
            2.5f
        );
        shapes.rectLine(
            endX, endY - radius,
            endX, endY + radius,
            2.5f
        );
    }

    private void drawMantisSlashWarning(
        Mantis mantis,
        boolean second
    ) {
        float vx = mantis.getSlashWarningVelocityX(second);
        float vy = mantis.getSlashWarningVelocityY(second);

        float speed = (float)Math.sqrt(vx * vx + vy * vy);
        if (speed < .001f) return;

        float ux = vx / speed;
        float uy = vy / speed;

        float startX = mantis.getSlashWarningX(second);
        float startY = mantis.getSlashWarningY(second);
        float progress = mantis.getSlashWarningProgress();

        float alpha = .52f + .34f * progress;

        if (second) {
            shapes.setColor(.99f, .68f, .40f, alpha);
        } else {
            shapes.setColor(.55f, .86f, 1f, alpha);
        }

        for (int i = 0; i < 38; i++) {
            float distance = 10f + i * 21f;

            float x1 = startX + ux * distance;
            float y1 = startY + uy * distance;
            float x2 = startX + ux * (distance + 9f);
            float y2 = startY + uy * (distance + 9f);

            if (x1 < 2400f || x1 > 3200f
                || y1 < 40f || y1 > 590f) {
                continue;
            }

            shapes.rectLine(x1, y1, x2, y2, 2.6f);
        }

        shapes.rect(
            startX - 3f, startY - 3f,
            6f, 6f
        );
    }

    private void drawMantisDashTrail(Mantis mantis) {
        Rectangle r = mantis.bounds;

        float ux = mantis.getClawDashHeadingX();
        float uy = mantis.getClawDashHeadingY();

        float cx = r.x + r.width / 2f;
        float cy = r.y + r.height / 2f;

        for (int i = 0; i < 5; i++) {
            float distance = 36f + i * 18f;
            float x = cx - ux * distance;
            float y = cy - uy * distance;

            shapes.setColor(
                .90f, .91f, .80f,
                .53f - i * .085f
            );

            shapes.rect(
                x - 4f, y - 2f,
                i % 2 == 0 ? 8f : 5f,
                4f
            );
        }
    }

    private void drawMantisSlashSprites(Mantis mantis) {
        if (mantis.isSlashVisible()) {
            drawWindBlade(
                mantis.getSlashBounds(),
                mantis.getSlashTravelDirection(false),
                mantis.getSlashRotation(false),
                mantis.getSlashWarningVelocityX(false),
                mantis.getSlashWarningVelocityY(false)
            );
        }

        if (mantis.isSecondSlashVisible()) {
            drawWindBlade(
                mantis.getSecondSlashBounds(),
                mantis.getSlashTravelDirection(true),
                mantis.getSlashRotation(true),
                mantis.getSlashWarningVelocityX(true),
                mantis.getSlashWarningVelocityY(true)
            );
        }
    }

    private void drawWindBlade(
        Rectangle r,
        int direction,
        float rotation,
        float vx,
        float vy
    ) {
        boolean flipX = direction < 0;

        float x = r.x - 8f;
        float y = r.y - 7f;
        float w = r.width + 16f;
        float h = r.height + 14f;

        float velocityLength = (float)Math.sqrt(
            vx * vx + vy * vy
        );

        float trailX = velocityLength > .001f
            ? vx / velocityLength
            : direction;

        float trailY = velocityLength > .001f
            ? vy / velocityLength
            : 0f;

        for (int i = 2; i >= 1; i--) {
            batch.setColor(
                .70f, .88f, 1f,
                i == 1 ? .28f : .13f
            );

            drawRotatedWind(
                x - trailX * i * 13f,
                y - trailY * i * 13f,
                w, h, rotation, flipX
            );
        }

        batch.setColor(Color.WHITE);
        drawRotatedWind(
            x, y, w, h, rotation, flipX
        );
        batch.setColor(Color.WHITE);
    }

    private void drawRotatedWind(
        float x, float y,
        float w, float h,
        float angle,
        boolean flipX
    ) {
        batch.draw(
            mantisSlashTexture,
            x, y,
            w / 2f, h / 2f,
            w, h,
            1f, 1f, angle,
            0, 0,
            mantisSlashTexture.getWidth(),
            mantisSlashTexture.getHeight(),
            flipX, false
        );
    }

    private void drawMantisAttackEffects(
        Mantis mantis, float time
    ) {
        if (mantis.getState() == Enemy.State.TELEGRAPH) {
            drawMantisWindup(mantis, time);
        }

        if (!mantis.isClawSwingVisible()) return;

        float elapsed = mantis.getClawSwingElapsed();

        int frame = MathUtils.clamp(
            (int)((elapsed - .04f) / .045f),
            0, 7
        );

        Rectangle area = mantis.getClawBounds();

        boolean flipX = mantis.getDirection() < 0;
        boolean intro = mantis.isIntroSwing();

        float extra = intro ? 1.12f : 1f;
        float w = (area.width + 23f) * extra;
        float h = (area.height + 18f) * extra;

        float x = area.x + area.width / 2f - w / 2f;
        float y = area.y + area.height / 2f - h / 2f;

        float tilt = (float)Math.toDegrees(
            Math.atan2(
                mantis.getClawDashHeadingY(),
                Math.max(
                    .12f,
                    Math.abs(mantis.getClawDashHeadingX())
                )
            )
        );

        tilt = MathUtils.clamp(
            tilt * .62f, -56f, 56f
        );

        if (flipX) tilt = -tilt;

        if (mantisClawFrames != null) {
            if (intro && frame >= 3 && frame <= 5) {
                batch.setColor(.72f, .86f, 1f, .24f);

                drawClawFrame(
                    Math.max(0, frame - 2),
                    x - mantis.getDirection() * 7f,
                    y + 4f, w, h,
                    flipX, tilt
                );
            }

            batch.setColor(Color.WHITE);
            drawClawFrame(
                frame, x, y, w, h,
                flipX, tilt
            );

        } else {
            batch.setColor(.82f, .94f, 1f, .92f);

            batch.draw(
                mantisSlashTexture,
                x, y, w, h,
                0, 0,
                mantisSlashTexture.getWidth(),
                mantisSlashTexture.getHeight(),
                flipX, false
            );
        }

        batch.setColor(Color.WHITE);
    }

    private void drawMantisWindup(
        Mantis mantis, float time
    ) {
        Rectangle r = mantis.bounds;
        boolean flipX = mantis.getDirection() < 0;

        float pulse = .20f + .12f
            * (1f + MathUtils.sin(time * 16f)) * .5f;

        if (mantis.getPattern() == Mantis.Pattern.CLAW) {
            if (mantisClawFrames != null) {
                float x = mantis.getDirection() > 0
                    ? r.x + r.width - 13f
                    : r.x - 93f;

                batch.setColor(1f, .75f, .56f, pulse);

                drawClawFrame(
                    1, x, r.y + 34f,
                    100f, 126f, flipX
                );
            }

        } else if (mantis.getPattern() == Mantis.Pattern.SLASH) {
            float x = mantis.getDirection() > 0
                ? r.x + r.width - 7f
                : r.x - 57f;

            batch.setColor(
                .78f, .92f, 1f,
                pulse + .10f
            );

            batch.draw(
                mantisSlashTexture,
                x, r.y + 45f,
                64f, 64f,
                0, 0,
                mantisSlashTexture.getWidth(),
                mantisSlashTexture.getHeight(),
                flipX, false
            );

        } else if (mantisClawFrames != null) {
            batch.setColor(1f, .81f, .57f, pulse);

            drawClawFrame(
                1, r.x + 2f, r.y - 3f,
                67f, 35f, false
            );
        }

        batch.setColor(Color.WHITE);
    }

    private void drawClawFrame(
        int frame,
        float x, float y,
        float w, float h,
        boolean flipX
    ) {
        drawClawFrame(
            frame, x, y, w, h,
            flipX, 0f
        );
    }

    private void drawClawFrame(
        int frame,
        float x, float y,
        float w, float h,
        boolean flipX,
        float rotation
    ) {
        batch.draw(
            mantisClawFrames[frame],
            x, y,
            w / 2f, h / 2f,
            w, h,
            flipX ? -1f : 1f,
            1f, rotation
        );
    }

    private Texture loadSlashTexture() {
        FileHandle file =
            Gdx.files.internal("effects/mantis_slash.png");

        Texture texture;

        if (file.exists()) {
            texture = new Texture(file);

        } else {
            Pixmap pixels = new Pixmap(
                64, 64, Pixmap.Format.RGBA8888
            );

            pixels.setColor(0f, 0f, 0f, 0f);
            pixels.fill();

            for (int i = 0; i <= 38; i++) {
                float angle = -2.10f
                    + i * 4.20f / 38f;

                int x = Math.round(
                    22f + 30f * MathUtils.cos(angle)
                );
                int y = Math.round(
                    32f + 26f * MathUtils.sin(angle)
                );

                pixels.setColor(
                    .46f, .76f, .94f, .95f
                );
                pixels.fillCircle(x, y, 6);

                pixels.setColor(Color.WHITE);
                pixels.fillCircle(x - 2, y, 3);
            }

            texture = new Texture(pixels);
            pixels.dispose();
        }

        texture.setFilter(
            Texture.TextureFilter.Nearest,
            Texture.TextureFilter.Nearest
        );

        return texture;
    }

    private void drawTargetLine(
        float x1, float y1,
        float x2, float y2
    ) {
        shapes.rectLine(x1, y1, x2, y2, 5f);
        shapes.rectLine(
            x2 - 15f, y2,
            x2 + 15f, y2,
            3f
        );
        shapes.rectLine(
            x2, y2 - 15f,
            x2, y2 + 15f,
            3f
        );
    }

    private void drawProjectiles(World world) {
        for (Projectile p : world.projectiles) {
            Rectangle r = p.bounds;

            shapes.setColor(Color.GOLD);
            shapes.rect(
                r.x, r.y,
                r.width, r.height
            );

            shapes.setColor(Color.BLACK);

            float tip = p.direction > 0
                ? r.x + r.width - 4f
                : r.x;

            shapes.rect(tip, r.y + 1f, 4f, 4f);
        }
    }

    private void drawSwarm(World world, float time) {
        if (!world.isSwarmActive()) return;

        Enemy boss = world.getActiveBoss();
        if (boss == null) return;

        float elapsed = 2.5f - world.getSwarmTimer();
        float cx = boss.centerX();
        float cy = boss.centerY();

        if (elapsed < .30f) {
            float sourceX = world.player.bounds.x
                + world.player.bounds.width / 2f;

            float sourceY = world.player.bounds.y
                + world.player.bounds.height / 2f;

            shapes.setColor(1f, .85f, .38f, .70f);
            shapes.circle(
                sourceX,
                sourceY,
                16f + elapsed * 140f,
                32
            );
        }

        for (int i = 0; i < 6; i++) {
            float theta = time * 5.6f
                + i * MathUtils.PI2 / 6f;

            float orbitX = cx
                + MathUtils.cos(theta) * 65f;
            float orbitY = cy
                + MathUtils.sin(theta) * 47f;

            float entryX = world.player.bounds.x
                + world.player.bounds.width / 2f
                + MathUtils.cos(
                i * MathUtils.PI2 / 6f
            ) * 90f;

            float entryY = world.player.bounds.y
                + world.player.bounds.height / 2f
                + MathUtils.sin(
                i * MathUtils.PI2 / 6f
            ) * 62f;

            float t = MathUtils.clamp(
                (elapsed - .20f) / .55f,
                0f, 1f
            );

            float x = MathUtils.lerp(
                entryX, orbitX, t
            );
            float y = MathUtils.lerp(
                entryY, orbitY, t
            );

            if (elapsed > 2.10f) {
                float exit = (elapsed - 2.10f) / .40f;

                x += MathUtils.cos(theta)
                    * exit * 140f;
                y += MathUtils.sin(theta)
                    * exit * 110f;
            }

            shapes.setColor(1f, .76f, .25f, 1f);
            shapes.rect(x - 7f, y - 4f, 14f, 9f);

            shapes.setColor(.24f, .20f, .16f, 1f);
            shapes.rect(x - 2f, y - 4f, 3f, 9f);
            shapes.rect(x + 4f, y - 4f, 2f, 9f);

            shapes.setColor(.88f, .95f, 1f, .83f);

            float wing = MathUtils.sin(
                time * 20f + i
            ) * 2f;

            shapes.rect(
                x - 4f, y + 6f + wing,
                5f, 4f
            );
            shapes.rect(
                x + 2f, y + 6f - wing,
                5f, 4f
            );

            shapes.setColor(.97f, .97f, .81f, .9f);
            shapes.rect(
                x + 8f, y - 1f,
                4f, 2f
            );
        }
    }

    @Override
    public void dispose() {
        hud.dispose();
        terrain.dispose();
        mantisSprite.dispose();

        if (mantisClawTexture != null) {
            mantisClawTexture.dispose();
        }

        spiderSprite.dispose();
        beeSprite.dispose();
        hornetSprite.dispose();
        mantisSlashTexture.dispose();
        frogIdleTexture.dispose();
        frogAttackTexture.dispose();
        frogJumpTexture.dispose();

        if (hornetJawTexture != null) {
            hornetJawTexture.dispose();
        }

        shapes.dispose();
        batch.dispose();
    }
}
