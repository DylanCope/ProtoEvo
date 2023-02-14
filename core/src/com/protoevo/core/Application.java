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
		simulation = new Simulation();
		simulationScreen = new SimulationScreen(this, simulation);
		simulation.setSimulationScreen(simulationScreen);
//		if (SimulationSettings.simulationOnSeparateThread) {
//			simulationThread = new Thread(simulation);
//			new Thread(() -> {
//				simulation.prepare();
//				simulationThread.start();
//			}).start();
//		} else {
		new Thread(() -> {
			simulation.prepare();
		}).start();
//		}
//		simulation.prepare();
	}

	@Override
	public void render() {
		ScreenUtils.clear(0, 0.1f, 0.2f, 1f);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);

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
