package com.protoevo.ui;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.protoevo.core.ApplicationManager;
import com.protoevo.core.Simulation;

public class GraphicsAdapter extends Game {
	private final ApplicationManager applicationManager;
	private SimulationScreen simulationScreen;
	private SandboxScreen sandboxScreen;
	private TitleScreen titleScreen;
	private Skin skin;

	public GraphicsAdapter(ApplicationManager applicationManager) {
		this.applicationManager = applicationManager;
	}

	@Override
	public void create() {
		skin = UIStyle.getUISkin();
		if (applicationManager.hasSimulation()) {
			simulationScreen = new SimulationScreen(this, applicationManager.getSimulation());
			if (applicationManager.getSimulation().isReady())
				simulationScreen.notifySimulationLoaded();
			setScreen(simulationScreen);
		}
		else {
			titleScreen = new TitleScreen(this);
			setScreen(titleScreen);
		}
	}

	public void notifySimulationReady() {
		if (simulationScreen != null)
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
		super.dispose();
		if (skin != null)
			skin.dispose();
		if (simulationScreen != null)
			simulationScreen.dispose();
		if (sandboxScreen != null)
			sandboxScreen.dispose();
		if (titleScreen != null)
			titleScreen.dispose();
		FrameBufferManager.dispose();
	}

	public Skin getSkin() {
		return skin;
	}

	public void moveToSandbox() {
		sandboxScreen = new SandboxScreen(this);
		setScreen(sandboxScreen);
	}

	public void moveToTitleScreen() {
		titleScreen = new TitleScreen(this);
		setScreen(titleScreen);
	}

	public void moveToSimulationScreen(Simulation simulation) {
		applicationManager.setSimulation(simulation);
		simulationScreen = new SimulationScreen(this, applicationManager.getSimulation());
		if (applicationManager.getSimulation().isReady())
			simulationScreen.notifySimulationLoaded();
		setScreen(simulationScreen);
	}
}
