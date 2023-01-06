package com.protoevo.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.protoevo.core.Particle;
import com.protoevo.utils.Geometry;

import java.util.Collection;

public class ParticleTracker extends InputAdapter {

    Collection<Particle> particles;
    OrthographicCamera camera;
    PanZoomCameraInput panZoomCameraInput;
    Particle trackedParticle;

    public ParticleTracker(Collection<Particle> particles,
                           OrthographicCamera camera,
                           PanZoomCameraInput panZoomCameraInput) {
        this.particles = particles;
        this.camera = camera;
        this.panZoomCameraInput = panZoomCameraInput;
    }

    public boolean isTracking() {
        return trackedParticle != null;
    }

    public Vector3 getTrackedParticlePosition() {
        return new Vector3(trackedParticle.getPos(), 0);
    }

    public boolean track(Vector2 touchPos) {
        for (Particle particle : particles) {
            if (Geometry.isPointInsideCircle(particle.getPos(), particle.getRadius(), touchPos)) {
                trackedParticle = particle;
                panZoomCameraInput.setPanningDisabled(true);
                return true;
            }
        }
        return false;
    }

    public boolean untrack(Vector2 touchPos) {
        if (!Geometry.isPointInsideCircle(trackedParticle.getPos(), trackedParticle.getRadius(), touchPos)) {
            trackedParticle = null;
            panZoomCameraInput.setPanningDisabled(false);
        }

        return track(touchPos);
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
            Vector3 worldSpace = camera.unproject(new Vector3(screenX, screenY, 0));
            if (trackedParticle == null)
                return track(new Vector2(worldSpace.x, worldSpace.y));
            else
                return untrack(new Vector2(worldSpace.x, worldSpace.y));
        }
        return false;
    }

    public Particle getTrackedParticle() {
        return trackedParticle;
    }

    public boolean canTrack() {
        return true;
    }
}
