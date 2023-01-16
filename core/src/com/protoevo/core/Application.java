package com.protoevo.core;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.protoevo.core.settings.SimulationSettings;
import com.protoevo.ui.SimulationScreen;

public class Application extends ApplicationAdapter {
	public static final float refreshDelay = 1000 / 120f;

	private Simulation simulation;
	private SimulationScreen simulationScreen;


	@Override
	public void create () {
		simulation = new Simulation();
		simulation.setupEnvironment();
//		new Thread(simulation).start();
		simulationScreen = new SimulationScreen(simulation);
	}


	@Override
	public void render () {
		ScreenUtils.clear(0, 0f, 0f, 1);

		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);

		simulation.update(SimulationSettings.simulationUpdateDelta);

		float deltaTime = Gdx.graphics.getDeltaTime();
		simulationScreen.draw(deltaTime);
	}


	@Override
	public void dispose () {
		simulation.dispose();
		simulationScreen.dispose();
	}

}
