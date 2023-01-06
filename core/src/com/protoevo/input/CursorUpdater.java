package com.protoevo.input;

import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.protoevo.core.Particle;
import com.protoevo.utils.CursorUtils;
import com.protoevo.utils.Geometry;

import java.util.Collection;

public class CursorUpdater extends InputAdapter {
    private final OrthographicCamera camera;
    private final Collection<Particle> particles;
    private final MoveParticleButton moveParticleButton;
    private final ParticleTracker particleTracker;
    private final Vector2 touchPos = new Vector2(0, 0);

    public CursorUpdater(OrthographicCamera camera,
                         Collection<Particle> particles,
                         MoveParticleButton moveParticleButton,
                         ParticleTracker particleTracker) {
        this.camera = camera;
        this.particles = particles;
        this.moveParticleButton = moveParticleButton;
        this.particleTracker = particleTracker;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {

        Vector3 worldSpace = camera.unproject(new Vector3(screenX, screenY, 0));
        touchPos.set(worldSpace.x, worldSpace.y);
        for (Particle particle : particles) {
            if (Geometry.isPointInsideCircle(particle.getPos(), particle.getRadius(), touchPos)) {
                if (moveParticleButton.getState() == MoveParticleButton.State.HOLDING)
                    CursorUtils.setClosedHandCursor();
                else if (moveParticleButton.couldHold())
                        CursorUtils.setOpenHandCursor();
                else if (particleTracker.canTrack())
                    CursorUtils.setMagnifyingGlassCursor();
                else
                    CursorUtils.setDefaultCursor();

                return true;
            }
            CursorUtils.setDefaultCursor();
        }
        return false;
    }
}
