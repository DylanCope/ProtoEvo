package com.protoevo.ui;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.protoevo.core.ApplicationManager;
import com.protoevo.core.Simulation;

public class GraphicsAdapter extends Game {
	private final ApplicationManager applicationManager;
	private SimulationScreen simulationScreen;
	private LoadingScreen loadingScreen;
	private SandboxScreen sandboxScreen;
	private TitleScreen titleScreen;
	private SpriteBatch batch;
	private Skin skin;

	public GraphicsAdapter(ApplicationManager applicationManager) {
		this.applicationManager = applicationManager;
	}

	@Override
	public void create() {
		batch = new SpriteBatch();

		loadingScreen = new LoadingScreen(this, applicationManager);

		skin = UIStyle.getUISkin();
		if (applicationManager.hasSimulation()) {
			Simulation simulation = applicationManager.getSimulation();
			if (simulation.isReady())
				setSimulationScreen();
			else
				loadSimulation(applicationManager.getSimulation());
		}
		else {
			titleScreen = new TitleScreen(this);
			setScreen(titleScreen);
		}
	}

	public void notifySimulationReady() {
		loadingScreen.notifySimulationReady();
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
		batch.dispose();
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

	public void moveToTitleScreen(Screen previousScreen) {
		titleScreen = new TitleScreen(this);
		setScreen(titleScreen);
//		screen.dispose();
	}

	public void loadSimulation(Simulation simulation) {
		setScreen(loadingScreen);
		applicationManager.setSimulation(simulation);
	}

	public void setSimulationScreen() {
		simulationScreen = new SimulationScreen(this, applicationManager.getSimulation());
		setScreen(simulationScreen);
	}

	public SpriteBatch getSpriteBatch() {
		return batch;
	}
}
