package com.protoevo.ui.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector3;
import com.protoevo.env.Environment;
import com.protoevo.env.Spawnable;
import com.protoevo.ui.screens.SimulationScreen;
import com.protoevo.utils.Utils;

public class UserSpawnInput extends InputAdapter {

    private final OrthographicCamera camera;
    private final Environment environment;
    private final SimulationScreen simulationScreen;

    private final float rate = 0.1f;
    private float timeSinceSpawn = 0;
    private final Vector3 lastMousePos = new Vector3(), mousePos = new Vector3();

    public UserSpawnInput(SimulationScreen simulationScreen) {
        this.simulationScreen = simulationScreen;
        this.camera = simulationScreen.getCamera();
        this.environment = simulationScreen.getEnvironment();
    }

    public void spawn(float x, float y) {
        synchronized (environment) {
            Spawnable spawnable = simulationScreen.getInputManager().createSelectedSpawnable();
            spawnable.spawn(environment, x, y);
        }
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        timeSinceSpawn = 0f;
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {

        if (simulationScreen.hasSimulationNotLoaded())
            return false;

        if (Gdx.input.isButtonPressed(Input.Buttons.MIDDLE)) {
            lastMousePos.set(screenX, screenY, 0);
            Vector3 worldSpace = camera.unproject(new Vector3(screenX, screenY, 0));
            spawn(worldSpace.x, worldSpace.y);
            return true;
        }
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        mousePos.set(screenX, screenY, 0);

        if (simulationScreen.hasSimulationNotLoaded())
            return false;

        if (Gdx.input.isButtonPressed(Input.Buttons.MIDDLE)) {
            timeSinceSpawn += Gdx.graphics.getDeltaTime();
            float speed = mousePos.dst(lastMousePos);
            float dynamicRate = Utils.clampedLinearRemap(speed, 0f, Gdx.graphics.getWidth() / 4f, rate, 0f);
            if (timeSinceSpawn > dynamicRate) {
                Vector3 worldSpace = camera.unproject(new Vector3(screenX, screenY, 0));
                spawn(worldSpace.x, worldSpace.y);
                timeSinceSpawn = 0f;
            }
            return true;
        }
        return false;
    }

}
