package com.protoevo.ui;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.ScreenUtils;
import com.protoevo.core.ApplicationManager;
import com.protoevo.ui.SimulationScreen;

public class GraphicsAdapter extends ApplicationAdapter {
	public static final float refreshDelay = 1000 / 60f;

	private final ApplicationManager applicationManager;
	private SimulationScreen simulationScreen;
	private int updatesPerFrame = 1;
	private boolean reducedRendering = false;

	public GraphicsAdapter(ApplicationManager applicationManager) {
		this.applicationManager = applicationManager;
	}

	@Override
	public void create() {
		if (applicationManager.hasSimulation()) {
			simulationScreen = new SimulationScreen(this, applicationManager.getSimulation());
			if (applicationManager.getSimulation().isReady())
				simulationScreen.notifySimulationLoaded();
		}
	}

	@Override
	public void render() {
		ScreenUtils.clear(0, 0.1f, 0.2f, 1f);

		if (reducedRendering) {
			int fps = Gdx.graphics.getFramesPerSecond();
			if (fps > 5)
				updatesPerFrame = fps / 5;
		} else {
			updatesPerFrame = 1;
		}

		for (int i = 0; i < updatesPerFrame; i++)
			applicationManager.update();

		if (applicationManager.hasSimulation()) {
			float deltaTime = Gdx.graphics.getDeltaTime();
			simulationScreen.draw(deltaTime);
		}
	}

	public void notifySimulationReady() {
		simulationScreen.notifySimulationLoaded();
	}

	public SimulationScreen getSimulationScreen() {
		return simulationScreen;
	}

	public void exitApplication() {
		applicationManager.exit();
	}

	public void switchToHeadlessMode() {
		applicationManager.switchToHeadlessMode();
	}

	@Override
	public void dispose() {
		simulationScreen.dispose();
		FrameBufferManager.dispose();
	}

	public void toggleReducedRendering() {
		reducedRendering = !reducedRendering;
	}
}
