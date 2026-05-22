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
        keyFunctions.put(Input.Keys.F4, screen::toggleLineageOverlay);
        keyFunctions.put(Input.Keys.ESCAPE, screen::moveToPauseScreen);

        // Speed controls: ] faster, [ slower, \ reset to 1x
        keyFunctions.put(Input.Keys.RIGHT_BRACKET, () -> bumpSpeed(true));
        keyFunctions.put(Input.Keys.LEFT_BRACKET, () -> bumpSpeed(false));
        keyFunctions.put(Input.Keys.BACKSLASH, () -> {
            simulation.setTimeDilation(1f);
            System.out.println("Speed: 1.0x");
        });

        // Perf toggles: F10 low-detail (both), F11 chemicals only.
        keyFunctions.put(Input.Keys.F10, screen::toggleLowDetailMode);
        keyFunctions.put(Input.Keys.F11, () -> {
            var r = screen.getBaseEnvironmentRenderer();
            r.setRenderChemicals(!r.isRenderChemicals());
            System.out.println("Render chemicals: " + r.isRenderChemicals());
        });
        // Turbo: black screen + line chart + max sim throughput. Good for
        // leaving the sim running overnight.
        keyFunctions.put(Input.Keys.F8, screen::toggleTurboMode);

        // Homeostat: dynamically adjust food density / decay rate to track a
        // target population so evolution always has selection pressure.
        keyFunctions.put(Input.Keys.F7, () ->
                simulation.setHomeostasisEnabled(!simulation.isHomeostasisEnabled()));
    }

    private void bumpSpeed(boolean faster) {
        float td = simulation.getTimeDilation();
        // Speed steps: ..., 0.25, 0.5, 1, 2, 4, 8, 16, 32, 64, 128, 256
        if (faster) td = (td < 0.25f) ? 0.25f : Math.min(td * 2f, 256f);
        else        td = (td > 32f)   ? 32f   : Math.max(td * 0.5f, 0.0625f);
        simulation.setTimeDilation(td);
        System.out.printf("Speed: %.3fx%n", td);
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
