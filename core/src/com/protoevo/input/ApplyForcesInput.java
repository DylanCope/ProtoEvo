package com.protoevo.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.protoevo.env.Environment;
import com.protoevo.physics.Particle;
import com.protoevo.ui.SimulationScreen;
import com.protoevo.ui.shaders.ShockWaveLayer;

import java.util.Collection;

public class ApplyForcesInput extends InputAdapter {

    private final SimulationScreen simulationScreen;
    private final OrthographicCamera camera;

    public ApplyForcesInput(SimulationScreen simulationScreen) {
        this.camera = simulationScreen.getCamera();
        this.simulationScreen = simulationScreen;
    }

    public void applyForce(float explosionX, float explosionY, float power) {

        Environment env = simulationScreen.getEnvironment();
        if (env == null)
            return;

        if (ShockWaveLayer.getInstance() != null)
            ShockWaveLayer.getInstance().start(explosionX, explosionY);
        Vector2 tmp = new Vector2();

        for (Particle particle : env.getParticles()) {
            Vector2 bodyPos = particle.getPos();
            tmp.set(bodyPos.x - explosionX, bodyPos.y - explosionY);
            float dist2 = tmp.len2();
            if (power / dist2 > 1) {
                float explosionFallout = 10f;
                tmp.setLength((float) (power * Math.exp(-explosionFallout * dist2)));
                particle.applyImpulse(tmp);
            }
        }
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (simulationScreen.hasSimulationNotLoaded())
            return false;

        if (Gdx.input.isButtonPressed(Input.Buttons.RIGHT)) {
            Vector3 worldSpace = camera.unproject(new Vector3(screenX, screenY, 0));
            applyForce(worldSpace.x, worldSpace.y, .1f);
        }
        return false;
    }
}
