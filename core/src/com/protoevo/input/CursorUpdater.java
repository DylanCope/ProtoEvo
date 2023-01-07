package com.protoevo.input;

import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.protoevo.core.Particle;
import com.protoevo.ui.InputManager;
import com.protoevo.ui.UI;
import com.protoevo.utils.CursorUtils;
import com.protoevo.utils.Geometry;

public class CursorUpdater extends InputAdapter {
    private final UI ui;
    private final InputManager inputManager;
    private final Vector2 touchPos = new Vector2(0, 0);

    public CursorUpdater(UI ui, InputManager inputManager) {
        this.ui = ui;
        this.inputManager = inputManager;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        OrthographicCamera camera = ui.getCamera();
        if (ui.overOnScreenControls(screenX, screenY)) {
            CursorUtils.setDefaultCursor();
            return false;
        }

        Vector3 worldSpace = camera.unproject(new Vector3(screenX, screenY, 0));
        touchPos.set(worldSpace.x, worldSpace.y);

        float cameraScaling = camera.project(new Vector3(1, 0, 0)).len();
        for (Particle particle : ui.getEnvironment().getParticles()) {
            float screenR = particle.getRadius() * cameraScaling;
            if (screenR > 30 && Geometry.isPointInsideCircle(particle.getPos(), particle.getRadius(), touchPos)) {
                if (inputManager.getLightningButton().canStrike()) {
                    CursorUtils.setLightningCursor();
                }
                else if (inputManager.getMoveParticleButton().getState() == MoveParticleButton.State.HOLDING)
                    CursorUtils.setClosedHandCursor();
                else if (inputManager.getMoveParticleButton().couldHold())
                        CursorUtils.setOpenHandCursor();
                else if (inputManager.getParticleTracker().canTrack())
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
