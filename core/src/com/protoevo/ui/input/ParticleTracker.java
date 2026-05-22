package com.protoevo.ui.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.protoevo.physics.Particle;
import com.protoevo.ui.screens.SimulationScreen;
import com.protoevo.maths.Geometry;

import java.util.Optional;

public class ParticleTracker extends InputAdapter {

    private final SimulationScreen simulationScreen;
    private final OrthographicCamera camera;
    private final PanZoomCameraInput panZoomCameraInput;
    private Particle trackedParticle;
    // Secondary particle for BLAST-style signature comparison. Set by
    // shift-click while a primary is already tracked; cleared whenever
    // the primary is cleared.
    private Particle comparedParticle;
    private boolean canTrack = true;

    public Particle getComparedParticle() { return comparedParticle; }
    public void setComparedParticle(Particle p) { this.comparedParticle = p; }

    public ParticleTracker(SimulationScreen screen,
                           PanZoomCameraInput panZoomCameraInput) {
        this.simulationScreen = screen;
        this.camera = screen.getCamera();
        this.panZoomCameraInput = panZoomCameraInput;
    }

    public boolean isTracking() {
        return trackedParticle != null;
    }

    public Vector3 getTrackedParticlePosition() {
        return new Vector3(trackedParticle.getPos(), 0);
    }

    public boolean track(Vector2 touchPos) {
        if (!canTrack)
            return false;

        Optional<Particle> particle = simulationScreen.getEnvironment().getParticles()
                .filter(p -> Geometry.isPointInsideCircle(p.getPos(), p.getRadius(), touchPos))
                .findFirst();

        if (particle.isPresent()) {
                trackedParticle = particle.get();
                panZoomCameraInput.setPanningDisabled(true);
                simulationScreen.pollStats();
                return true;
        }
        return false;
    }

    public void untrack() {
        trackedParticle = null;
        comparedParticle = null;
        panZoomCameraInput.setPanningDisabled(false);
        simulationScreen.pollStats();
    }

    /** Shift-click variant: leave the primary tracked particle in place,
     *  set the secondary so the signature HUD can BLAST them side-by-side. */
    public boolean trackComparison(Vector2 touchPos) {
        if (!canTrack || trackedParticle == null) return false;
        Optional<Particle> particle = simulationScreen.getEnvironment().getParticles()
                .filter(p -> Geometry.isPointInsideCircle(p.getPos(), p.getRadius(), touchPos))
                .findFirst();
        if (particle.isPresent() && particle.get() != trackedParticle) {
            comparedParticle = particle.get();
            return true;
        }
        return false;
    }

    private boolean untrack(Vector2 touchPos) {
        if (!Geometry.isPointInsideCircle(trackedParticle.getPos(), trackedParticle.getRadius(), touchPos))
            untrack();

        return track(touchPos);
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (!canTrack)
            return false;

        if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
            Vector3 worldSpace = camera.unproject(new Vector3(screenX, screenY, 0));
            Vector2 world = new Vector2(worldSpace.x, worldSpace.y);
            // Shift+left-click while already tracking → set comparison
            // (for the signature HUD's BLAST-style alignment view). Doesn't
            // change the primary tracked particle.
            boolean shift = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)
                    || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);
            if (shift && trackedParticle != null)
                return trackComparison(world);
            if (trackedParticle == null)
                return track(world);
            else
                return untrack(world);
        }
        return false;
    }

    public Particle getTrackedParticle() {
        return trackedParticle;
    }

    public boolean canTrack() {
        return canTrack;
    }

    public void setCanTrack(boolean canTrack) {
        // Note: does not untrack the particle if it is currently being tracked
        this.canTrack = canTrack;
    }
}
