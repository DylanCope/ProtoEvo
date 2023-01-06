package com.protoevo.core;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.JointEdge;
import com.badlogic.gdx.physics.box2d.QueryCallback;
import com.protoevo.ui.UI;

import java.util.List;

public class Application extends ApplicationAdapter {
	public static final float refreshDelay = 1000 / 120f;

	private Simulation simulation;
	private UI ui;


	@Override
	public void create () {
		simulation = new Simulation();
		simulation.getEnv().initialise();
		ui = new UI(simulation);
	}


	@Override
	public void render () {
		float deltaTime = Gdx.graphics.getDeltaTime();
		ui.draw(deltaTime);
		simulation.update(deltaTime);


//		if (paused)
//			return;

//		timeToAttractionTracker += deltaTime;
//		particles.forEach(this::updateParticle);
//		if (timeToAttractionTracker >= .25f)
//			timeToAttractionTracker = 0;

	}

//	private float timeToAttractionTracker = 0;
//	public void updateParticle(Particle particle) {
//		Vector2 vel = particle.getVel();
//		Vector2 f = vel.cpy().scl(-0.1f);
//
//		float gr = 0.01f;
//		float delta = Gdx.graphics.getDeltaTime();
//		particle.setRadius(particle.getRadius() * (1 + gr * delta));
//
//		if (timeToAttractionTracker >= 0.25f) {
//			Vector2 particlePos = particle.getPos();
//			float maxDist2 = particle.getRadius() * particle.getRadius() * 9f;
//			float minDist2 = particle.getRadius() * particle.getRadius() * 4f;
//			float maxDist = (float) Math.sqrt(maxDist2);
//
//			QueryCallback query = fixture -> {
//				for (JointEdge joint : particle.getBody().getJointList())
//					if (joint.joint.getBodyA().equals(particle.getBody())
//							|| joint.joint.getBodyB().equals(particle.getBody()))
//						return true;
//
//				Vector2 pos = fixture.getBody().getPosition();
//				float dist2 = pos.dst2(particlePos);
//				if (dist2 < maxDist2 && dist2 > minDist2) {
//					Vector2 attractiveF = pos.cpy().sub(particlePos).nor().scl(10000f / dist2);
//					f.add(attractiveF);
//				}
//				return true;
//			};
//
//			environment.getWorld().QueryAABB(query,
//					particlePos.x - maxDist, particlePos.y - maxDist,
//					particlePos.x + maxDist, particlePos.y + maxDist);
//		}
//
//		particle.getBody().applyForce(f, particle.getBody().getWorldCenter(), true);
//	}

	@Override
	public void dispose () {
		simulation.dispose();
		ui.dispose();
	}

}
