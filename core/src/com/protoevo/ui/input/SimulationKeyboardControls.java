package com.protoevo.ui.input;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.protoevo.core.Simulation;
import com.protoevo.ui.screens.SimulationScreen;

import java.util.HashMap;
import java.util.Map;

public class SimulationKeyboardControls extends InputAdapter {

    private final Simulation simulation;
    private final SimulationScreen screen;
    private final Map<Integer, Runnable> keyFunctions = new HashMap<>();

    public SimulationKeyboardControls(SimulationScreen simulationScreen) {
        this.simulation = simulationScreen.getSimulation();
        this.screen = simulationScreen;

        keyFunctions.put(Input.Keys.SPACE, simulation::togglePause);
        keyFunctions.put(Input.Keys.F12, screen::toggleUI);
        keyFunctions.put(Input.Keys.ESCAPE, screen::moveToPauseScreen);
    }

    @Override
    public boolean keyDown(int keycode) {
        Runnable function = keyFunctions.get(keycode);
        if (function != null) {
            function.run();
            return true;
        }
        return false;
    }
}
