package com.protoevo.ui.input;

import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.protoevo.physics.Particle;
import com.protoevo.ui.SimulationInputManager;
import com.protoevo.ui.screens.SimulationScreen;
import com.protoevo.utils.CursorUtils;
import com.protoevo.utils.Geometry;

import java.util.Optional;

public class CursorUpdater extends InputAdapter {
    private final SimulationScreen simulationScreen;
    private final SimulationInputManager inputManager;
    private final Vector2 touchPos = new Vector2(0, 0);

    public CursorUpdater(SimulationScreen simulationScreen, SimulationInputManager inputManager) {
        this.simulationScreen = simulationScreen;
        this.inputManager = inputManager;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        OrthographicCamera camera = simulationScreen.getCamera();
        if (simulationScreen.overOnScreenControls(screenX, screenY)) {
            CursorUtils.setDefaultCursor();
            return false;
        }

        if (simulationScreen.hasSimulationNotLoaded())
            return false;

        Vector3 worldSpace = camera.unproject(new Vector3(screenX, screenY, 0));
        touchPos.set(worldSpace.x, worldSpace.y);

        float cameraScaling = camera.project(new Vector3(1, 0, 0)).len();
        synchronized (simulationScreen.getEnvironment()) {
            Optional<Particle> particleOptional = simulationScreen.getEnvironment().getParticles()
                    .filter(p -> p.getRadius() * cameraScaling > 30 && Geometry.isPointInsideCircle(p.getPos(), p.getRadius(), touchPos))
                    .findFirst();

            if (particleOptional.isPresent()) {
                Particle particle = particleOptional.get();
                if (inputManager.getLightningButton().canStrike()) {
                    CursorUtils.setLightningCursor();
                } else if (inputManager.getMoveParticleButton().getState() == MoveParticleButton.State.HOLDING)
                    CursorUtils.setClosedHandCursor();
                else if (inputManager.getMoveParticleButton().couldHold())
                    CursorUtils.setOpenHandCursor();
                else if (inputManager.getParticleTracker().canTrack() &&
                        inputManager.getParticleTracker().getTrackedParticle() != particle)
                    CursorUtils.setMagnifyingGlassCursor();
                else
                    CursorUtils.setDefaultCursor();

                return true;
            }
        }

        CursorUtils.setDefaultCursor();
        return false;
    }
}
