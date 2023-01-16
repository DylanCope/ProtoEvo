package com.protoevo.input;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.protoevo.core.Simulation;

public class SimulationKeyboardControls extends InputAdapter {

    private final Simulation simulation;

    public SimulationKeyboardControls(Simulation simulation) {
        this.simulation = simulation;
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.SPACE) {
            simulation.togglePause();
        }

        return false;
    }
}
