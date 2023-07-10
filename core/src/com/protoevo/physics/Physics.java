package com.protoevo.physics;

import com.protoevo.core.Statistics;
import com.protoevo.env.Environment;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public abstract class Physics implements Serializable {
    private static final long serialVersionUID = 1L;
    private final Statistics debugStats = new Statistics();
    protected float physicsStepTime;
    private final Map<Long, Particle> particles = new ConcurrentHashMap<>();

    public abstract void registerStaticBodies(Environment environment);

    public abstract void dispose();

    public void step(float delta) {
        long startTime = System.nanoTime();
        stepPhysics(delta);
        physicsStepTime = TimeUnit.SECONDS.convert(
                System.nanoTime() - startTime, TimeUnit.NANOSECONDS);

        particles.entrySet().removeIf(entry -> {
            boolean dead = entry.getValue().isDead();
            if (dead)
                entry.getValue().dispose();
            return dead;
        });
    }

    protected abstract void stepPhysics(float delta);

    public abstract JointsManager getJointsManager();

    public Collection<Particle> getParticles() {
        return particles.values();
    }

    public Particle createNewParticle() {
        Particle particle = newParticle();
        particles.put(particle.getId(), particle);
        return particle;
    }

    protected abstract Particle newParticle();

    public Optional<Particle> getParticle(long id) {
        return Optional.ofNullable(particles.get(id));
    }

    public Statistics getDebugStats() {
        debugStats.clear();
        debugStats.put("Physics Step Time", physicsStepTime, Statistics.ComplexUnit.TIME);
        return debugStats;
    }

    public void rebuildTransientFields(Environment environment) {
        getJointsManager().rebuild(this);
    }
}
