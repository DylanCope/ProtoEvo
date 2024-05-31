package com.protoevo.ui;

import com.badlogic.gdx.*;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.ScreenUtils;
import com.protoevo.core.ApplicationManager;
import com.protoevo.core.Simulation;
import com.protoevo.settings.RenderingSettings;
import com.protoevo.ui.rendering.EnvironmentRenderer;
import com.protoevo.ui.screens.LoadSaveScreen;
import com.protoevo.ui.screens.LoadingScreen;
import com.protoevo.ui.screens.SimulationScreen;
import com.protoevo.ui.screens.TitleScreen;
import com.protoevo.utils.CursorUtils;

public class GraphicsAdapter extends Game {
	public static RenderingSettings settings = RenderingSettings.createDefault();
	private final ApplicationManager applicationManager;
	private SimulationScreen simulationScreen;
	private LoadingScreen loadingScreen;
	private TitleScreen titleScreen;
	private ScreenAdapter currentScreen, previousScreen;
	private SpriteBatch batch;
	private Skin skin;
	private static DefaultBackgroundRenderer defaultBackgroundRenderer;

	public GraphicsAdapter(ApplicationManager applicationManager) {
		this.applicationManager = applicationManager;
	}

	@Override
	public void create() {
		boolean windowed = ApplicationManager.settings.displayMode.get().equals("Windowed");
		boolean borderlessWindowed = ApplicationManager.settings.displayMode.get().equals("Borderless Window");

		if (windowed) {
			Gdx.graphics.setWindowedMode(ApplicationManager.settings.windowWidth.get(),
										 ApplicationManager.settings.windowHeight.get());
		} else if (borderlessWindowed) {
			Graphics.DisplayMode displayMode = Gdx.graphics.getDisplayMode();
			Gdx.graphics.setUndecorated(true);
			Gdx.graphics.setWindowedMode(displayMode.width, displayMode.height);
		} else {
			Gdx.graphics.setFullscreenMode(Lwjgl3ApplicationConfiguration.getDisplayMode());
		}

		CursorUtils.setDefaultCursor();
		batch = new SpriteBatch();
		ShaderProgram.pedantic = false;

		defaultBackgroundRenderer = DefaultBackgroundRenderer.getInstance();
		skin = UIStyle.getUISkin();

		loadingScreen = new LoadingScreen(this, applicationManager);

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

	@Override
	public void setScreen(Screen screen) {
		previousScreen = currentScreen;
		super.setScreen(screen);
		currentScreen = (ScreenAdapter) screen;
	}

	public static void renderBackground(float delta) {
		defaultBackgroundRenderer.render(delta);
	}

	public void notifySimulationReady() {
		if (loadingScreen != null)
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

	public ApplicationManager getManager() {
		return applicationManager;
	}

	@Override
	public void dispose() {
		super.dispose();
		batch.dispose();
		if (skin != null)
			skin.dispose();
		if (simulationScreen != null)
			simulationScreen.dispose();
		if (titleScreen != null)
			titleScreen.dispose();
		FrameBufferManager.dispose();
		CursorUtils.dispose();
		DefaultBackgroundRenderer.disposeInstance();
	}

	@Override
	public void render() {
		ScreenUtils.clear(EnvironmentRenderer.backgroundColor);
		super.render();
	}

	public Skin getSkin() {
		return skin;
	}

	public void moveToSandbox() {}

	public void moveToTitleScreen(ScreenAdapter previousScreen) {
		this.previousScreen = previousScreen;
		applicationManager.disposeSimulationIfPresent();
		titleScreen = new TitleScreen(this);
		setScreen(titleScreen);
//		screen.dispose();
	}

	public void loadSimulation(Simulation simulation) {
		loadingScreen.setUpdatesBeforeRendering(50);
		setScreen(loadingScreen);
		applicationManager.setSimulation(simulation);
	}

	public void loadPreexistingSimulation(Simulation simulation) {
		loadingScreen.setUpdatesBeforeRendering(0);
		setScreen(loadingScreen);
		applicationManager.setSimulation(simulation);
	}

	public void moveToLoadSaveScreen(String simulationName) {
		LoadSaveScreen loadSaveScreen = new LoadSaveScreen(this, simulationName, titleScreen);
		setScreen(loadSaveScreen);
	}

	public void setSimulationScreen() {
		simulationScreen = new SimulationScreen(this, applicationManager.getSimulation());
		setScreen(simulationScreen);
	}

	public boolean hasPreviousScreen() {
		return previousScreen != null;
	}

	public void moveToPreviousScreen() {
		if (hasPreviousScreen())
			setScreen(previousScreen);
	}

	public void returnToPreviousScreenAndDispose() {
		if (hasPreviousScreen()) {
			currentScreen.dispose();
			setScreen(previousScreen);
		}
	}

	public SpriteBatch getSpriteBatch() {
		return batch;
	}
}
