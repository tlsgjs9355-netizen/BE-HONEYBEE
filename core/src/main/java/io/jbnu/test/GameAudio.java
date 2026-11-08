
package io.jbnu.test;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Disposable;

import java.util.IdentityHashMap;

/**
 * BE HONEYBEE M6.57 - stage music and event-driven insect sound effects.
 * No changes to World or boss combat logic are required.
 */
public final class GameAudio implements Disposable {

    private final Music titleBgm;
    private final Music endingBgm;
    private final Music[] stageBgm = new Music[5];
    private final Music[] bossBgm = new Music[5];
    private final Music giantPhaseTwoBgm;
    private final Music blackFly;
    private final Music giantFly;
    private Music currentBgm;

    private final Sound bossEntrance;
    private final Sound honey;
    private final Sound beeDash;
    private final Sound beeSting;
    private final Sound beeStingHit;
    private final Sound beeHurt;
    private final Sound capture;
    private final Sound escapeHit;
    private final Sound escapeMiss;
    private final Sound gameOver;
    private final Sound bossClear;
    private final Sound menuStart;
    private final Sound mantisClaw;
    private final Sound mantisClawHeavy;
    private final Sound mantisSlash;
    private final Sound mantisSlashHeavy;
    private final Sound mantisHit;
    private final Sound blackDash;
    private final Sound blackBite;
    private final Sound blackHit;
    private final Sound giantDash;
    private final Sound giantGrab;
    private final Sound giantHit;
    private final Sound frogCroak;
    private final Sound frogTongue;
    private final Sound spiderDescend;

    private final IdentityHashMap<GreenFrog, GreenFrog.State> frogStates =
        new IdentityHashMap<GreenFrog, GreenFrog.State>();
    private final IdentityHashMap<SpiderTrap, Boolean> spiderDescending =
        new IdentityHashMap<SpiderTrap, Boolean>();
    private final IdentityHashMap<Projectile, Boolean> previousShots =
        new IdentityHashMap<Projectile, Boolean>();

    private boolean initialized;
    private boolean previousArena;
    private boolean previousClear;
    private boolean previousDead;
    private boolean previousEnding;
    private boolean previousCaptured;
    private boolean previousClawSwing;
    private boolean previousSlash;
    private boolean previousSecondSlash;
    private boolean previousGrabContact;
    private boolean shotThisFrame;

    private int previousLevel;
    private int previousHoney;
    private int previousHealth;
    private int previousHits;
    private int previousMisses;
    private int previousBossHp;
    private int previousGiantStings;

    private Enemy.State previousHornetState;
    private Enemy.State previousGiantState;
    private float bossIntroTime;
    private float frogCroakTime = 3f;
    private float hitSoundDelay;
    private String previousMode = "";

    public GameAudio() {
        titleBgm = loadMusic("audio/bgm/title.ogg", 0.19f, null);
        endingBgm = loadMusic("audio/bgm/ending.ogg", 0.22f, null);

        for (int level = 1; level <= 4; level++) {
            stageBgm[level] = loadMusic(
                "audio/bgm/stage" + level + ".ogg", 0.19f,
                "audio/field_loop.ogg"
            );
        }
        bossBgm[2] = loadMusic(
            "audio/bgm/boss_mantis.ogg", 0.25f, "audio/boss_loop.ogg"
        );
        bossBgm[3] = loadMusic(
            "audio/bgm/boss_black_hornet.ogg", 0.25f, "audio/boss_loop.ogg"
        );
        bossBgm[4] = loadMusic(
            "audio/bgm/boss_giant_hornet.ogg", 0.26f, "audio/boss_loop.ogg"
        );
        giantPhaseTwoBgm = loadMusic(
            "audio/bgm/boss_giant_hornet_phase2.ogg", 0.27f,
            "audio/bgm/boss_giant_hornet.ogg"
        );
        blackFly = loadMusic("audio/black_hornet_fly.wav", 0.14f, null);
        giantFly = loadMusic("audio/giant_hornet_fly.wav", 0.16f, null);

        bossEntrance = loadSound("boss_entrance.wav");
        honey = loadSound("honey.wav");
        beeDash = loadSound("dash.wav");
        beeSting = loadSound("sting.wav");
        beeStingHit = loadSound("bee_sting_hit.wav");
        beeHurt = loadSound("bee_hurt.wav", "damage.wav");
        capture = loadSound("capture.wav");
        escapeHit = loadSound("escape_hit.wav");
        escapeMiss = loadSound("escape_miss.wav");
        gameOver = loadSound("game_over.wav");
        bossClear = loadSound("boss_clear.wav");
        menuStart = loadSound("menu_start.wav");
        mantisClaw = loadSound("mantis_claw.wav");
        mantisClawHeavy = loadSound("mantis_claw_heavy.wav");
        mantisSlash = loadSound("mantis_slash.wav");
        mantisSlashHeavy = loadSound("mantis_slash_heavy.wav");
        mantisHit = loadSound("mantis_hit.wav", "boss_hit.wav");
        blackDash = loadSound("black_hornet_dash.wav");
        blackBite = loadSound("black_hornet_bite.wav");
        blackHit = loadSound("black_hornet_hit.wav", "boss_hit.wav");
        giantDash = loadSound("giant_hornet_dash.wav");
        giantGrab = loadSound("giant_hornet_grab.wav");
        giantHit = loadSound("giant_hornet_hit.wav", "boss_hit.wav");
        frogCroak = loadSound("frog_croak.wav");
        frogTongue = loadSound("frog_tongue.wav");
        spiderDescend = loadSound("spider_descend.wav");
    }

    private static Music loadMusic(String path, float volume, String fallback) {
        FileHandle file = Gdx.files.internal(path);
        if (!file.exists() && fallback != null) file = Gdx.files.internal(fallback);
        if (!file.exists()) return null;
        Music m = Gdx.audio.newMusic(file);
        m.setLooping(true);
        m.setVolume(volume);
        return m;
    }

    private static Sound loadSound(String name) {
        return loadSound(name, null);
    }

    private static Sound loadSound(String name, String fallback) {
        FileHandle file = Gdx.files.internal("audio/" + name);
        if (!file.exists() && fallback != null) {
            file = Gdx.files.internal("audio/" + fallback);
        }
        return file.exists() ? Gdx.audio.newSound(file) : null;
    }

    private static void play(Sound sound, float volume) {
        if (sound != null) sound.play(MathUtils.clamp(volume, 0f, 1f));
    }

    private void selectBgm(Music target) {
        if (currentBgm != target) {
            if (currentBgm != null) currentBgm.pause();
            currentBgm = target;
            if (currentBgm != null) currentBgm.setPosition(0f);
        }
        if (currentBgm != null && !currentBgm.isPlaying()) currentBgm.play();
    }

    private void pauseAll() {
        if (currentBgm != null && currentBgm.isPlaying()) currentBgm.pause();
        if (blackFly != null && blackFly.isPlaying()) blackFly.pause();
        if (giantFly != null && giantFly.isPlaying()) giantFly.pause();
    }

    private void pauseFlights() {
        if (blackFly != null && blackFly.isPlaying()) blackFly.pause();
        if (giantFly != null && giantFly.isPlaying()) giantFly.pause();
    }

    /** Called when starting a new run or retrying after death. */
    public void reset() {
        selectBgm(null);
        pauseFlights();
        initialized = false;
        previousArena = false;
        previousClear = false;
        previousDead = false;
        previousEnding = false;
        shotThisFrame = false;
        bossIntroTime = 0f;
        hitSoundDelay = 0f;
        previousMode = "";
        frogStates.clear();
        spiderDescending.clear();
        previousShots.clear();
    }

    /** Direct input event: avoids missing a shot that hits in the same frame. */
    public void onBeeSting() {
        shotThisFrame = true;
        play(beeSting, .39f);
    }

    public void onBeeDash() {
        play(beeDash, .46f);
    }

    public void onMenuStart() {
        play(menuStart, .43f);
    }

    public void onEscapeSuccessHit() {
        play(escapeHit, .48f);
    }

    public void onEscapeMiss() {
        play(escapeMiss, .46f);
    }

    private void snapshot(World w) {
        previousLevel = w.level;
        previousHoney = w.getHoneyCollected();
        previousHealth = w.getPlayerHalfHearts();
        previousHits = w.getEscapeHits();
        previousMisses = w.getEscapeMisses();
        previousCaptured = w.isCaptured();
        previousClear = w.isArenaCleared();
        previousDead = w.isGameOver();
        previousEnding = w.isGameCleared();
        previousBossHp = bossForLevel(w).getHp();
        previousClawSwing = w.mantis.isClawSwingVisible();
        previousSlash = w.mantis.isSlashVisible();
        previousSecondSlash = w.mantis.isSecondSlashVisible();
        previousGrabContact = w.giantHornet.isGrabDangerous();
        previousHornetState = w.hornet.getState();
        previousGiantState = w.giantHornet.getState();
        previousGiantStings = w.giantHornet.getStings().size;
        previousArena = w.isArenaEntered() && !w.isArenaCleared() && w.level > 1;

        previousShots.clear();
        for (Projectile p : w.projectiles) previousShots.put(p, Boolean.TRUE);

        for (GreenFrog f : w.frogs) frogStates.put(f, f.getState());
        for (SpiderTrap s : w.spiders) {
            spiderDescending.put(s, Boolean.valueOf(s.isMovingDown()));
        }
    }

    private Enemy bossForLevel(World w) {
        if (w.level == 2) return w.mantis;
        if (w.level == 3) return w.hornet;
        return w.giantHornet;
    }

    private void resetStageObservations(World w) {
        frogStates.clear();
        spiderDescending.clear();
        previousShots.clear();
        snapshot(w);
        // On an arena retry, play the boss introduction again.
        previousArena = false;
    }

    private static float distance(float x1, float y1, float x2, float y2) {
        float dx = x1 - x2;
        float dy = y1 - y2;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private static float playerDistance(World w, float x, float y) {
        return distance(
            w.player.bounds.x + w.player.bounds.width / 2f,
            w.player.bounds.y + w.player.bounds.height / 2f,
            x, y
        );
    }

    private void updateWaspFlights(World w, boolean arenaActive) {
        if (!arenaActive || (w.level != 3 && w.level != 4)) {
            pauseFlights();
            return;
        }
        Music playing = w.level == 3 ? blackFly : giantFly;
        Music other = w.level == 3 ? giantFly : blackFly;
        if (other != null && other.isPlaying()) other.pause();
        if (playing == null) return;
        Enemy boss = bossForLevel(w);
        if (boss.isDead()) {
            playing.pause();
            return;
        }
        float d = playerDistance(w, boss.centerX(), boss.centerY());
        float closeness = MathUtils.clamp(1f - d / 700f, 0f, 1f);
        playing.setVolume((w.level == 3 ? .12f : .15f) * closeness);
        if (closeness > 0.03f) {
            if (!playing.isPlaying()) playing.play();
        } else if (playing.isPlaying()) playing.pause();
    }

    private void updateFrogsAndSpiders(World w, float dt) {
        frogCroakTime = Math.max(0f, frogCroakTime - dt);
        for (GreenFrog f : w.frogs) {
            GreenFrog.State now = f.getState();
            GreenFrog.State before = frogStates.get(f);
            float d = playerDistance(w, f.centerX(), f.bounds.y + f.bounds.height / 2f);
            if (d < 550f) {
                if (before != null && before != GreenFrog.State.STRIKE
                    && now == GreenFrog.State.STRIKE) play(frogTongue, .43f);
                if (frogCroakTime == 0f && now == GreenFrog.State.IDLE) {
                    play(frogCroak, .22f);
                    frogCroakTime = MathUtils.random(4.4f, 7.8f);
                }
            }
            frogStates.put(f, now);
        }

        for (SpiderTrap s : w.spiders) {
            boolean down = s.isMovingDown() && !s.isStunned();
            Boolean before = spiderDescending.get(s);
            if (down && (before == null || !before.booleanValue())
                && s.getProgress() < .15f
                && playerDistance(w, s.bounds.x + s.bounds.width / 2f,
                s.bounds.y + s.bounds.height / 2f) < 490f) {
                play(spiderDescend, .20f);
            }
            spiderDescending.put(s, Boolean.valueOf(down));
        }
    }

    private void updateBossAttacks(World w) {
        if (w.level == 2) {
            Mantis m = w.mantis;
            boolean swing = m.isClawSwingVisible();
            boolean slash = m.isSlashVisible();
            boolean second = m.isSecondSlashVisible();
            if (swing && !previousClawSwing) {
                play(m.isPhaseTwo() ? mantisClawHeavy : mantisClaw, .54f);
            }
            if (slash && !previousSlash) play(mantisSlash, .55f);
            if (second && !previousSecondSlash) play(mantisSlashHeavy, .57f);
        } else if (w.level == 3) {
            Hornet h = w.hornet;
            if (h.getState() == Enemy.State.ATTACK
                && previousHornetState != Enemy.State.ATTACK) {
                switch (h.getPattern()) {
                    case DASH: play(blackDash, .57f); break;
                    case BITE: play(blackBite, .54f); break;
                    case STING: play(blackDash, .38f); break;
                    default: break;
                }
            }
        } else if (w.level == 4) {
            GiantHornet g = w.giantHornet;
            if (g.getState() == Enemy.State.ATTACK
                && previousGiantState != Enemy.State.ATTACK) {
                if (g.getPattern() == GiantHornet.Pattern.CHARGE) {
                    play(giantDash, .63f);
                } else if (g.getPattern() == GiantHornet.Pattern.GRAB) {
                    play(giantDash, .44f);
                }
            }
            if (g.isGrabDangerous() && !previousGrabContact) {
                play(giantGrab, .56f);
            }
            if (g.getPattern() == GiantHornet.Pattern.STING
                && g.getStings().size > previousGiantStings) {
                play(beeSting, .45f);
            }
        }
    }

    private void updateBossDamage(World w, boolean wasBossActive) {
        Enemy boss = bossForLevel(w);
        boolean damaged = wasBossActive && boss.getHp() < previousBossHp;
        if (!damaged || hitSoundDelay > 0f) return;

        if (w.level == 2) play(mantisHit, .39f);
        else if (w.level == 3) play(blackHit, .44f);
        else if (w.level == 4) play(giantHit, .51f);

        // Only play the sting impact when a projectile was consumed on hit.
        boolean projectileGone = shotThisFrame;
        if (!projectileGone) {
            for (Projectile old : previousShots.keySet()) {
                boolean stillExists = false;
                for (Projectile now : w.projectiles) {
                    if (old == now) { stillExists = true; break; }
                }
                if (!stillExists) { projectileGone = true; break; }
            }
        }
        if (projectileGone) play(beeStingHit, .28f);
        hitSoundDelay = .065f;
    }

    /** Called once per frame by Main after World.update(). */
    public void update(World w, String mode, float dt) {
        dt = Math.max(0f, dt);
        if (!initialized) {
            snapshot(w);
            previousArena = false;
            previousDead = false;
            previousEnding = false;
            initialized = true;
        }
        if ("PAUSED".equals(mode)) {
            pauseAll();
            previousMode = mode;
            shotThisFrame = false;
            return;
        }
        hitSoundDelay = Math.max(0f, hitSoundDelay - dt);
        if (w.level != previousLevel) resetStageObservations(w);

        boolean dead = "GAME_OVER".equals(mode) || w.isGameOver();
        boolean ending = "ENDING".equals(mode) || w.isGameCleared();
        boolean battleArena = w.isArenaEntered() && !w.isArenaCleared()
            && w.level > 1;

        if (w.getPlayerHalfHearts() < previousHealth) play(beeHurt, .54f);
        if (dead) {
            if (!previousDead) play(gameOver, .63f);
            selectBgm(null);
            pauseFlights();
        } else if (ending) {
            if (!previousEnding) play(bossClear, .68f);
            selectBgm(endingBgm);
            pauseFlights();
        } else if ("TITLE".equals(mode)) {
            selectBgm(titleBgm);
            pauseFlights();
        } else if ("STAGE_INTRO".equals(mode)) {
            if ("TITLE".equals(previousMode)) play(menuStart, .4f);
            selectBgm(stageBgm[MathUtils.clamp(w.level, 1, 4)]);
            pauseFlights();
        } else if ("RUNNING".equals(mode)) {
            if (battleArena && !previousArena) {
                play(bossEntrance, .62f);
                bossIntroTime = 2.25f;
                selectBgm(null);
            }
            if (!battleArena && previousArena && w.isArenaCleared()) {
                play(bossClear, .66f);
                bossIntroTime = 0f;
            }
            if (battleArena) {
                bossIntroTime = Math.max(0f, bossIntroTime - dt);
                if (bossIntroTime > 0f) {
                    selectBgm(null);
                } else {
                    Music track = (w.level == 4 && w.giantHornet.isPhaseTwo())
                        ? giantPhaseTwoBgm : bossBgm[w.level];
                    selectBgm(track);
                }
                updateWaspFlights(w, true);
                updateBossAttacks(w);
                updateBossDamage(w, previousArena);
            } else {
                selectBgm(stageBgm[MathUtils.clamp(w.level, 1, 4)]);
                pauseFlights();
                // Final attack can kill the boss in the same frame as clearance.
                if (previousArena && w.isArenaCleared()) updateBossDamage(w, true);
            }
            if (w.getHoneyCollected() > previousHoney) play(honey, .43f);
            if (w.isCaptured() && !previousCaptured) play(capture, .48f);
            if (w.isCaptured() && w.getEscapeHits() > previousHits) {
                play(escapeHit, .4f);
            }
            if (w.isCaptured() && w.getEscapeMisses() > previousMisses) {
                play(escapeMiss, .4f);
            }
            updateFrogsAndSpiders(w, dt);
        }

        snapshot(w);
        previousMode = mode;
        shotThisFrame = false;
    }

    private static void close(Sound sound) {
        if (sound != null) sound.dispose();
    }

    private static void close(Music music) {
        if (music != null) music.dispose();
    }

    @Override
    public void dispose() {
        close(titleBgm);
        close(endingBgm);
        for (int i = 1; i <= 4; i++) close(stageBgm[i]);
        for (int i = 2; i <= 4; i++) close(bossBgm[i]);
        close(giantPhaseTwoBgm);
        close(blackFly);
        close(giantFly);
        close(bossEntrance);
        close(honey);
        close(beeDash);
        close(beeSting);
        close(beeStingHit);
        close(beeHurt);
        close(capture);
        close(escapeHit);
        close(escapeMiss);
        close(gameOver);
        close(bossClear);
        close(menuStart);
        close(mantisClaw);
        close(mantisClawHeavy);
        close(mantisSlash);
        close(mantisSlashHeavy);
        close(mantisHit);
        close(blackDash);
        close(blackBite);
        close(blackHit);
        close(giantDash);
        close(giantGrab);
        close(giantHit);
        close(frogCroak);
        close(frogTongue);
        close(spiderDescend);
    }
}
