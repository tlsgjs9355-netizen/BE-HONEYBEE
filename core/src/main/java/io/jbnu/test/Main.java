
package io.jbnu.test;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.FitViewport;

/** Main loop, screen flow, inputs and camera feedback. */
public class Main extends ApplicationAdapter {

    private static final float WIDTH = 800f;
    private static final float HEIGHT = 600f;
    private static final float MAX_DT = 1f / 30f;
    private static final float STORY_INTRO_DURATION = 10.2f;
    private static final float OTHER_INTRO_DURATION = 3.6f;

    private static final String[] TUTORIAL_LINES = {
        "방향키 ← → 로 좌우로 움직여 보세요.",
        "SPACE를 눌러 점프해 보세요.",
        "앞에 있는 꿀을 하나 모아 보세요.",
        "앗! 앞에 거미가 있습니다. 조심해서 다가가세요!",
        "거미에게 붙잡혔어요! 초록색에 맞춰 SPACE를 3번 누르세요.",
        "잘했어요! 거미에게서 탈출했습니다.",
        "앞으로 Z 대시, X 독침, C 동료 벌 스킬을 배울 수 있어요."
    };

    private enum GameState {
        TITLE, STAGE_INTRO, RUNNING, PAUSED, GAME_OVER, ENDING
    }

    private GameState state = GameState.TITLE;
    private GameState previousState = GameState.RUNNING;

    private World world;
    private GameAssets assets;
    private GameRenderer renderer;
    private GameMenu menu;
    private GameOverScreen gameOverScreen;

    private OrthographicCamera camera;
    private FitViewport viewport;

    private final CameraManager cameraManager =
        new CameraManager();

    private final Vector3 mousePosition = new Vector3();

    private float animationTime;
    private float endingTime;
    private float introElapsed;
    private float introDuration;
    private int tutorialStep;
    private float tutorialStepTime;
    private float tutorialStartX;
    private boolean tutorialSpacePressed;
    private int previousLevel;

    private boolean observedAmbush;
    private boolean observedArenaCleared;
    private boolean observedMantisOpeningSwing;
    private Enemy observedBoss;
    private int observedBossHp;

    @Override
    public void create() {
        world = new World();
        assets = new GameAssets();

        renderer = new GameRenderer(assets);
        menu = new GameMenu(assets);
        gameOverScreen = new GameOverScreen();

        camera = new OrthographicCamera();
        viewport = new FitViewport(WIDTH, HEIGHT, camera);

        viewport.update(
            Gdx.graphics.getWidth(),
            Gdx.graphics.getHeight(),
            true
        );

        animationTime = 0f;
        endingTime = 0f;
        introElapsed = 0f;
        introDuration = STORY_INTRO_DURATION;

        resetCameraForStage();
    }

    private float getCameraTargetX() {
        float desired = world.player.bounds.x
            + world.player.bounds.width / 2f;

        if (world.isArenaEntered()
            && !world.isArenaCleared()) {

            switch (world.level) {
                case 2:
                    return world.isMantisIntroLocked()
                        ? 2610f : 2800f;

                case 3:
                    return 2600f;

                case 4:
                    return 2200f;

                default:
                    break;
            }
        }

        return MathUtils.clamp(
            desired,
            WIDTH / 2f,
            Math.max(
                WIDTH / 2f,
                world.worldWidth - WIDTH / 2f
            )
        );
    }

    private void resetCameraForStage() {
        float x = getCameraTargetX();

        camera.position.set(x, HEIGHT / 2f, 0f);
        camera.update();

        cameraManager.reset(x);

        previousLevel = world.level;
        observedAmbush = world.hasStageFourAmbushOccurred();
        observedArenaCleared = world.isArenaCleared();
        observedMantisOpeningSwing = false;

        observedBoss = world.getActiveBoss();
        observedBossHp = observedBoss != null
            ? observedBoss.getHp() : 0;
    }

    private void beginStageIntro() {
        state = GameState.STAGE_INTRO;
        introElapsed = 0f;
        introDuration = world.level == 1
            ? STORY_INTRO_DURATION : OTHER_INTRO_DURATION;
        if (world.level == 1) {
            tutorialStep = 0;
            tutorialStepTime = 0f;
            tutorialSpacePressed = false;
            tutorialStartX = world.player.bounds.x;
        } else {
            tutorialStep = TUTORIAL_LINES.length;
        }
        previousState = GameState.RUNNING;

        world.player.setMovement(false, false);
        resetCameraForStage();
    }

    private void startNewGame() {
        world.restartEntireGame();

        animationTime = 0f;
        endingTime = 0f;

        beginStageIntro();
    }

    @Override
    public void render() {
        float dt = Math.min(
            Gdx.graphics.getDeltaTime(),
            MAX_DT
        );

        handleInput();
        menu.update(dt);

        switch (state) {
            case TITLE:
                break;

            case STAGE_INTRO:
                introElapsed += dt;
                if (introElapsed >= introDuration) {
                    introElapsed = introDuration;
                    state = GameState.RUNNING;
                }
                break;

            case RUNNING:
                checkGameOver();

                if (state != GameState.RUNNING) break;

                world.update(dt);
                animationTime += dt;
                updateTutorial(dt);

                checkGameOver();

                if (state != GameState.RUNNING) break;

                if (previousLevel != world.level) {
                    beginStageIntro();
                    break;
                }

                observeCameraEvents();

                cameraManager.update(
                    dt,
                    getCameraTargetX(),
                    world.worldWidth,
                    camera
                );

                if (world.isGameCleared()) {
                    state = GameState.ENDING;
                    endingTime = 0f;

                    world.player.setMovement(false, false);
                }
                break;

            case GAME_OVER:
                break;

            case ENDING:
                endingTime += dt;

                cameraManager.update(
                    dt,
                    getCameraTargetX(),
                    world.worldWidth,
                    camera
                );
                break;

            case PAUSED:
                break;
        }

        updateWingSound();
        viewport.apply();

        if (state == GameState.TITLE) {
            menu.renderTitle();
            return;
        }

        if (state == GameState.STAGE_INTRO) {
            menu.renderStageIntro(world.level, introElapsed);
            return;
        }

        if (state == GameState.GAME_OVER) {
            gameOverScreen.render();
            return;
        }

        boolean paused = state == GameState.PAUSED;

        boolean ending = state == GameState.ENDING
            || (paused && previousState == GameState.ENDING);

        renderer.render(
            world, camera,
            animationTime, endingTime,
            paused, ending
        );

        if (!paused && state == GameState.RUNNING
            && world.level == 1 && tutorialStep < TUTORIAL_LINES.length
            && !world.isCaptured()) {
            menu.renderTutorial(TUTORIAL_LINES[tutorialStep], tutorialStep);
        }
    }

    private void nextTutorialStep() {
        tutorialStep = Math.min(TUTORIAL_LINES.length, tutorialStep + 1);
        tutorialStepTime = 0f;
    }

    private void updateTutorial(float dt) {
        if (world.level != 1 || tutorialStep >= TUTORIAL_LINES.length) return;
        tutorialStepTime += dt;
        switch (tutorialStep) {
            case 0:
                if (Math.abs(world.player.bounds.x - tutorialStartX) >= 55f)
                    nextTutorialStep();
                break;
            case 1:
                if (tutorialSpacePressed) nextTutorialStep();
                break;
            case 2:
                if (world.getHoneyCollected() >= 1) nextTutorialStep();
                break;
            case 3:
                if (world.isCaptured()) nextTutorialStep();
                break;
            case 4:
                if (world.isTutorialSpiderEscaped()) nextTutorialStep();
                break;
            case 5:
                if (tutorialStepTime >= 3.2f) nextTutorialStep();
                break;
            case 6:
                if (tutorialStepTime >= 4.5f) nextTutorialStep();
                break;
            default: break;
        }
    }

    private void observeCameraEvents() {
        boolean ambush = world.hasStageFourAmbushOccurred();

        if (world.level == 4
            && ambush && !observedAmbush) {

            cameraManager.startShake(0.85f, 18f);
        }

        observedAmbush = ambush;

        Enemy boss = world.getActiveBoss();

        if (boss != observedBoss) {
            if (boss != null && world.level != 4) {
                cameraManager.startShake(0.40f, 8f);
            }

            observedBoss = boss;
            observedBossHp = boss != null
                ? boss.getHp() : 0;

        } else if (boss != null
            && boss.getHp() < observedBossHp) {

            cameraManager.startShake(0.10f, 2.8f);
            observedBossHp = boss.getHp();
        }

        if (world.level == 2
            && world.mantis.isIntroSwing()
            && !observedMantisOpeningSwing) {

            cameraManager.startShake(0.42f, 15f);
            observedMantisOpeningSwing = true;
        }

        boolean cleared = world.isArenaCleared();

        if (cleared && !observedArenaCleared) {
            cameraManager.startShake(
                0.8f,
                world.level == 4 ? 17f : 12f
            );
        }

        observedArenaCleared = cleared;
    }

    private void checkGameOver() {
        if (state == GameState.RUNNING
            && world.isGameOver()) {

            world.player.setMovement(false, false);
            state = GameState.GAME_OVER;

            gameOverScreen.showRandomMessage();
        }
    }

    private void retryCurrentStage() {
        world.restartCurrentStage();
        if (world.level == 1) {
            tutorialStep = 0;
            tutorialStepTime = 0f;
            tutorialSpacePressed = false;
            tutorialStartX = world.player.bounds.x;
        }

        animationTime = 0f;
        endingTime = 0f;
        previousState = GameState.RUNNING;
        state = GameState.RUNNING;

        resetCameraForStage();
    }

    private boolean pressedStart() {
        return Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
            || Gdx.input.isKeyJustPressed(Input.Keys.SPACE);
    }

    private void handleInput() {
        if (state == GameState.TITLE) {
            if (pressedStart()) {
                startNewGame();
            }
            return;
        }

        if (state == GameState.STAGE_INTRO) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                if (world.level == 1) {
                    introElapsed = Math.min(
                        STORY_INTRO_DURATION,
                        ((int)(introElapsed / 3.4f) + 1) * 3.4f);
                } else {
                    introElapsed = introDuration;
                }
                if (introElapsed >= introDuration) state = GameState.RUNNING;
            }
            return;
        }

        if (state == GameState.GAME_OVER) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                retryCurrentStage();

            } else if (Gdx.input.justTouched()) {
                mousePosition.set(
                    Gdx.input.getX(),
                    Gdx.input.getY(),
                    0f
                );

                viewport.unproject(mousePosition);

                float uiX = mousePosition.x
                    - (camera.position.x - WIDTH / 2f);

                float uiY = mousePosition.y
                    - (camera.position.y - HEIGHT / 2f);

                if (gameOverScreen.isRestartButtonPressed(
                    uiX, uiY
                )) {
                    retryCurrentStage();
                }
            }
            return;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.P)
            || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {

            if (state == GameState.PAUSED) {
                state = previousState;

            } else {
                previousState = state;
                state = GameState.PAUSED;

                world.player.setMovement(false, false);
            }
            return;
        }

        if (state == GameState.PAUSED) return;

        if (state == GameState.ENDING) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
                && endingTime >= 21f) {
                world.restartEntireGame();

                animationTime = 0f;
                endingTime = 0f;
                previousState = GameState.RUNNING;
                state = GameState.TITLE;

                resetCameraForStage();
            }
            return;
        }

        if (world.isCaptured()) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
                world.pressFlap();
            }
            return;
        }

        // Pause still works, but no movement, flap or skills during the intro.
        if (world.isMantisIntroLocked()) {
            world.player.setMovement(false, false);
            return;
        }

        boolean left =
            Gdx.input.isKeyPressed(Input.Keys.A)
                || Gdx.input.isKeyPressed(Input.Keys.LEFT);

        boolean right =
            Gdx.input.isKeyPressed(Input.Keys.D)
                || Gdx.input.isKeyPressed(Input.Keys.RIGHT);

        world.player.setMovement(left, right);

        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            if (world.level == 1 && tutorialStep == 1) {
                tutorialSpacePressed = true;
            }
            world.pressFlap();
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.Z)) {
            world.pressDash();
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.X)) {
            world.pressStinger();
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.C)) {
            world.pressSwarm();
        }
    }

    private void updateWingSound() {
        Music buzz = assets.wingBuzz;
        if (buzz == null) return;

        boolean play = state == GameState.RUNNING
            && !world.isMantisIntroLocked()
            && !world.isCaptured()
            && !world.player.grounded
            && !world.player.isDashing();

        if (play && !buzz.isPlaying()) {
            buzz.play();
        } else if (!play && buzz.isPlaying()) {
            buzz.pause();
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        gameOverScreen.dispose();
        menu.dispose();
        renderer.dispose();
        assets.dispose();
        GalmuriFonts.disposeShared();
    }
}
