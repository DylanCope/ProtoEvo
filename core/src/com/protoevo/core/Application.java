package com.protoevo.core;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.utils.ScreenUtils;
import com.protoevo.settings.SimulationSettings;
import com.protoevo.env.ChemicalSolution;
import com.protoevo.ui.SimulationScreen;

public class Application extends ApplicationAdapter {
	public static final float refreshDelay = 1000 / 60f;

	private Simulation simulation;
	private SimulationScreen simulationScreen;
	private Thread simulationThread;

	@Override
	public void create() {
		System.out.println("Current JVM version: " + System.getProperty("java.version"));

		simulation = new Simulation(0, "chaos-nidoking-iure");
//		simulation = new Simulation();
		simulationScreen = new SimulationScreen(this, simulation);
		simulation.setSimulationScreen(simulationScreen);
		new Thread(() -> {
			simulation.prepare();
		}).start();
	}

	@Override
	public void render() {
		ScreenUtils.clear(0, 0.1f, 0.2f, 1f);

		if (simulationThread == null && simulation.isReady())
			simulation.update(SimulationSettings.simulationUpdateDelta);

		float deltaTime = Gdx.graphics.getDeltaTime();
		simulationScreen.draw(deltaTime);
	}

	public void toggleSeparateThread() {
		if (simulationThread == null) {
			simulationThread = new Thread(simulation);
			simulationThread.start();
		} else {
			simulation.interruptSimulationLoop();
			try {
				simulationThread.join();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}

			simulationThread = null;
		}
	}

	private void relocateCUDAContext() {
		// reinitialise chemical solution to move cuda context to main thread
		ChemicalSolution chemicalSolution = simulation.getEnv().getChemicalSolution();
		if (chemicalSolution != null)
			chemicalSolution.initialise();
	}

	@Override
	public void dispose() {
		simulation.dispose();
		simulationScreen.dispose();
	}

}
