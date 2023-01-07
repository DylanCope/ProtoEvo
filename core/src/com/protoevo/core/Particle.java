package com.protoevo.core;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.protoevo.core.settings.Settings;
import com.protoevo.env.Environment;
import com.protoevo.env.Rock;
import com.protoevo.utils.Geometry;

import java.util.Map;
import java.util.TreeMap;


public class Particle {

    private Environment environment;
    private Body body;
    private Fixture fixture;
    private boolean dead;
    private float radius;
    private final TreeMap<String, Float> stats = new TreeMap<>();

    public Particle() {}

    public Body getBody() {
        return body;
    }

    public void applyForce(Vector2 force) {
        body.applyForceToCenter(force, true);
    }

    public void applyImpulse(Vector2 impulse) {
        body.applyLinearImpulse(impulse, body.getWorldCenter(), true);
    }

    public void onCollision(Particle other) {}
    public void onCollision(Rock rock) {}

    public float getRadius() {
        return radius;
    }

    public void setRadius(float radius) {
        this.radius = radius;
        if (fixture != null)
            fixture.getShape().setRadius(radius);
    }

    public boolean isPointInside(Vector2 point) {
        float r = getRadius();
        return point.dst2(getPos()) < r*r;
    }

    public void setPos(Vector2 pos) {
        body.setTransform(pos, 0);
    }

    public Vector2 getPos() {
        return body.getPosition();
    }

    public Vector2 getVel() {
        return body.getLinearVelocity();
    }

    public float getSpeed() {
        return getVel().len();
    }

    public float getMass() {
        return getMass(getRadius());
    }

    public float getMass(float r) {
        return getMass(r, 0);
    }

    public float getMass(float r, float extraMass) {
        return Geometry.getSphereVolume(r) * getMassDensity() + extraMass;
    }

    public float getMassDensity() {
        return 1000f;
    }

    public Color getColor() {
        return Color.LIGHT_GRAY;
    }

    public Environment getEnv() {
        return environment;
    }

    public void setEnv(Environment environment) {
        this.environment = environment;
        environment.ensureAddedToEnvironment(this);
        createBody();
    }

    public void createBody() {
        if (body != null)
            return;

        CircleShape circle = new CircleShape();
        // Create a fixture definition to apply our shape to
        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = circle;
        fixtureDef.density = 1f; // mass / (float) (Math.PI * radius * radius);
        fixtureDef.friction = 0.8f;
        fixtureDef.restitution = 0.6f;

        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;

        body = environment.getWorld().createBody(bodyDef);
        // Create our fixture and attach it to the body
        fixture = body.createFixture(fixtureDef);
        fixture.setUserData(this);
        body.setUserData(this);

        circle.dispose();
    }

    public Map<String, Float> getStats() {
        stats.clear();
        stats.put("Size", Settings.statsDistanceScalar * getRadius());
        stats.put("Speed", Settings.statsDistanceScalar * getSpeed());
        return stats;
    }

    public boolean isDead() {
        return dead;
    }


    public void handleDeath() {
    }

    public void kill() {
        dead = true;
    }

    public void dispose() {
        kill();
        environment.getWorld().destroyBody(body);
    }

    public String getPrettyName() {
        return "Particle";
    }
}
