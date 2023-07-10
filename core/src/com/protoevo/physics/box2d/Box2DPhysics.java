package com.protoevo.physics.box2d;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.protoevo.core.Statistics;
import com.protoevo.env.Environment;
import com.protoevo.env.Rock;
import com.protoevo.physics.FixtureCategories;
import com.protoevo.physics.JointsManager;
import com.protoevo.physics.Particle;
import com.protoevo.physics.Physics;

import java.util.Collection;

public class Box2DPhysics extends Physics {

    private transient World world;
    private final JointsManager jointsManager;

    public Box2DPhysics() {
        createWorld();
        jointsManager = new Box2DJointsManager(this);
    }

    private void createWorld() {
        world = new World(new Vector2(0, 0), true);
        world.setContinuousPhysics(false);
        world.setAutoClearForces(true);
        world.setContactListener(new Box2DCollisionHandler());
    }

    public void createRockFixtures(Environment environment) {
        for (Rock rock : environment.getRocks()) {
            BodyDef rockBodyDef = new BodyDef();
            Body rockBody = world.createBody(rockBodyDef);
            PolygonShape rockShape = new PolygonShape();
            rockShape.set(rock.getPoints());
            rockBody.setUserData(rock);

            FixtureDef rockFixtureDef = new FixtureDef();
            rockFixtureDef.shape = rockShape;
            rockFixtureDef.density = 0.0f;
            rockFixtureDef.friction = 0.7f;
            rockFixtureDef.filter.categoryBits = ~FixtureCategories.SENSOR;

            rockBody.createFixture(rockFixtureDef);
        }
    }

    @Override
    public void registerStaticBodies(Environment environment) {
        createRockFixtures(environment);
    }

    @Override
    public void rebuildTransientFields(Environment environment) {
        createWorld();
        registerStaticBodies(environment);
        for (Particle particle : getParticles())
            particle.rebuildTransientFields();
        super.rebuildTransientFields(environment);
    }

    @Override
    public void dispose() {
        world.dispose();
    }

    @Override
    public void stepPhysics(float delta) {
        world.step(
                delta,
                Environment.settings.misc.physicsVelocityIterations.get(),
                Environment.settings.misc.physicsPositionIterations.get());
    }

    @Override
    public JointsManager getJointsManager() {
        return jointsManager;
    }

    @Override
    public Particle newParticle() {
        return new Box2DParticle(this);
    }

    @Override
    public Statistics getDebugStats() {
        Statistics debugStats = super.getDebugStats();

        debugStats.putCount("Bodies", world.getBodyCount());
        debugStats.putCount("Contacts", world.getContactCount());
        debugStats.putCount("Joints", world.getJointCount());
        debugStats.putCount("Fixtures", world.getFixtureCount());
        debugStats.putCount("Proxies", world.getProxyCount());

        int sleepCount = 0;
        Collection<Particle> particles = getParticles();
        int totalCells = particles.size();
        for (Particle particle : particles) {
            Box2DParticle box2DParticle = (Box2DParticle) particle;
            Body body = box2DParticle.getBody();
            if (body != null && !body.isAwake())
                sleepCount++;
        }

        debugStats.putPercentage("Sleeping",  100f * sleepCount / totalCells);

        return debugStats;
    }

    public World getWorld() {
        return world;
    }
}
