# BE HONEYBEE — M1 flight prototype

Based on the supplied `Project2(1).zip` (libGDX 1.13.1, Gradle project); original course project is not overwritten.

## Open in Android Studio (Windows)
1. Extract this ZIP to a short local path, e.g. `C:\Projects\BE_HONEYBEE_M1`.
2. Android Studio → Open → select the extracted project folder containing `settings.gradle`.
3. Allow Gradle to sync. Internet access may be required for the Gradle distribution and dependencies.
4. Open `lwjgl3/src/main/java/io/jbnu/test/lwjgl3/Lwjgl3Launcher.java` and click Run beside `main`, or run Gradle task `lwjgl3:run`.

## Controls
- A/D or Left/Right: horizontal movement (220 world units/second)
- Space **new press**: flap, vertical velocity set to +280 even in the air
- P/Escape: pause/resume

## Implemented (M1)
- FitViewport 800×600; camera smooth-follow X axis; HUD fixed on screen
- Gravity -650 u/s²; fall speed clamped to -420
- Player collision 28×24; movement separated X then Y in <=8-unit steps
- Stage 1 terrain rectangles from the supplied BE HONEYBEE v1.1 document
- Upper bound at y=600; fall reset when player's top falls below -80
- Shape-rendered prototype bee with moving wings (no PNG required)
- Placeholder exit region highlights green; M1 does **not** yet implement collecting honey, level clear or next level
- P/Escape pause freezes world and wing animation

## Next milestones
M2: honey pickup, spikes, stage progression, checkpoint logic. Replace temporary art with final assets later.

## Verification
Source and assets packaged; full Gradle build was **not** verified here because the runtime cannot download `gradle-8.14.3-bin.zip` (network/DNS unavailable). Run in your Android Studio installation and share exact Gradle errors, if any.
