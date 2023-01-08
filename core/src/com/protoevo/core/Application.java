package com.protoevo.core;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.protoevo.core.settings.Settings;
import com.protoevo.ui.UI;

public class Application extends ApplicationAdapter {
	public static final float refreshDelay = 1000 / 120f;

	private Simulation simulation;
	private UI ui;


	@Override
	public void create () {
		simulation = new Simulation();
		Vector2[] populationCentres = simulation.getEnv().initialise();
		ui = new UI(simulation);

		Vector2 pos = populationCentres[0];
		ui.getCamera().position.set(pos.x, pos.y, 0);
		ui.getCamera().zoom = 0.5f;
	}


	@Override
	public void render () {
		float deltaTime = Gdx.graphics.getDeltaTime();
		ui.draw(deltaTime);
		simulation.update(Settings.simulationUpdateDelta);


//		if (paused)
//			return;

//		timeToAttractionTracker += deltaTime;
//		particles.forEach(this::updateParticle);
//		if (timeToAttractionTracker >= .25f)
//			timeToAttractionTracker = 0;

	}


	@Override
	public void dispose () {
		simulation.dispose();
		ui.dispose();
	}

}
