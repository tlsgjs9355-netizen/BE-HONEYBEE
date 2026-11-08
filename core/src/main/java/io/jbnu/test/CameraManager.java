
package io.jbnu.test;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;

/**
 * 카메라 추적과 시간 기반 화면 흔들림을 관리한다.
 * World의 물리 좌표와는 독립적으로 동작한다.
 */
public final class CameraManager {
    private static final float VIEW_WIDTH = 800f;
    private static final float VIEW_HEIGHT = 600f;
    private static final float FOLLOW_SPEED = 6f;

    private float baseX = VIEW_WIDTH / 2f;
    private float shakeDuration;
    private float shakeRemaining;
    private float shakeStrength;
    private float shakeClock;

    public void reset(float cameraX) {
        baseX = cameraX;
        shakeDuration = 0f;
        shakeRemaining = 0f;
        shakeStrength = 0f;
        shakeClock = 0f;
    }

    /** 더 강한 새 충격이 오면 기존 효과를 교체한다. */
    public void startShake(float duration, float intensity) {
        if (duration <= 0f || intensity <= 0f) return;
        if (shakeRemaining > 0f && intensity < shakeStrength) return;

        shakeDuration = duration;
        shakeRemaining = duration;
        shakeStrength = intensity;
        shakeClock = 0f;
    }

    public void update(
        float dt,
        float desiredX,
        float worldWidth,
        OrthographicCamera camera
    ) {
        float half = VIEW_WIDTH / 2f;
        float rightLimit = Math.max(half, worldWidth - half);
        float target = MathUtils.clamp(desiredX, half, rightLimit);

        // 프레임 시간에 따라 보간하여 카메라 이동을 부드럽게 한다.
        float alpha = 1f - (float) Math.exp(-FOLLOW_SPEED * Math.max(0f, dt));
        baseX = MathUtils.lerp(baseX, target, alpha);

        float offsetX = 0f;
        float offsetY = 0f;

        if (shakeRemaining > 0f) {
            shakeRemaining = Math.max(0f, shakeRemaining - dt);
            shakeClock += dt;

            float fade = shakeDuration > 0f
                ? shakeRemaining / shakeDuration : 0f;

            float strength = shakeStrength * fade;

            offsetX = MathUtils.sin(shakeClock * 69f) * strength;
            offsetY = MathUtils.sin(shakeClock * 91f + 0.8f)
                * strength * 0.65f;
        }

        camera.position.set(
            baseX + offsetX,
            VIEW_HEIGHT / 2f + offsetY,
            0f
        );
        camera.update();
    }
}
