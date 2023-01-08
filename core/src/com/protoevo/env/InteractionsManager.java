package com.protoevo.env;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.JointEdge;
import com.badlogic.gdx.physics.box2d.QueryCallback;
import com.badlogic.gdx.utils.Array;
import com.protoevo.core.Particle;
import com.protoevo.core.settings.Settings;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class InteractionsManager {

	private static class QueuedInteraction {
		public final Particle particle;
		public final List<Object> interaction;

		public QueuedInteraction(Particle particle, List<Object> interaction) {
			this.particle = particle;
			this.interaction = interaction;
		}

		public void call() {
			particle.interact(interaction);
		}

		public boolean hasInteractions() {
			return interaction.size() > 0;
		}
	}

    private final Environment environment;
    private float timeSinceLastInteract = 0;
	private final float timeToInteract = Settings.simulationUpdateDelta * 10;
	private final Queue<QueuedInteraction> interactionQueue = new LinkedList<>();

    public InteractionsManager(Environment environment) {
        this.environment = environment;
    }

    public void update(float deltaTime) {
		timeSinceLastInteract += deltaTime;
		if (timeSinceLastInteract >= timeToInteract) {
			timeSinceLastInteract = 0;
			for (Particle particle : environment.getParticles()) {
				if (particle.doesInteract()) {
					QueuedInteraction interaction = getInteractions(particle);
					if (interaction.hasInteractions())
						interactionQueue.add(interaction);
				}
			}
			interactionQueue.parallelStream().forEach(QueuedInteraction::call);
		}
    }

	public QueuedInteraction getInteractions(Particle particle) {
		List<Object> interactions = new ArrayList<>();

		Vector2 particlePos = particle.getPos();
		float maxDist = particle.getInteractionRange();

		Array<JointEdge> jointEdgeArray = particle.getBody().getJointList();

		QueryCallback query = fixture -> {
			for (JointEdge joint : jointEdgeArray) {
				Body bodyA = joint.joint.getBodyA();
				Body bodyB = joint.joint.getBodyB();
				if ((bodyA.equals(particle.getBody()) && bodyB.equals(fixture.getBody())) ||
						(bodyB.equals(particle.getBody()) && bodyA.equals(fixture.getBody())))
					return true;
			}

			Object userData = fixture.getBody().getUserData();
			interactions.add(userData);
			return true;
		};

		environment.getWorld().QueryAABB(query,
				particlePos.x - maxDist, particlePos.y - maxDist,
				particlePos.x + maxDist, particlePos.y + maxDist);

		return new QueuedInteraction(particle, interactions);
	}


	public float getTimeSinceLastInteract() {
		return timeSinceLastInteract;
	}

	public float getTimeToInteract() {
		return timeToInteract;
	}
}
