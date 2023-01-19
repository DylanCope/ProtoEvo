package com.protoevo.input;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.protoevo.core.Simulation;
import com.protoevo.ui.SimulationScreen;

public class SimulationKeyboardControls extends InputAdapter {

    private final Simulation simulation;
    private final SimulationScreen screen;

    public SimulationKeyboardControls(SimulationScreen simulationScreen) {
        this.simulation = simulationScreen.getSimulation();
        this.screen = simulationScreen;
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.SPACE) {
            simulation.togglePause();
        }
        if (keycode == Input.Keys.F12) {
            screen.toggleUI();
        }

        return false;
    }
}
