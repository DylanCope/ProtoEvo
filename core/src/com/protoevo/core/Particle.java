package com.protoevo.core;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.protoevo.env.Environment;
import com.protoevo.utils.Geometry;


public class Particle {

    private Environment environment;
    private Body body;
    private Fixture fixture;
    private boolean dead, hasHandledDeath;

    public Particle() {}

    public Body getBody() {
        return body;
    }

    public float getRadius() {
        if (fixture == null)
            return 0;
        return fixture.getShape().getRadius();
    }

    public void setRadius(float radius) {
        if (fixture != null)
            fixture.getShape().setRadius(radius);
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

        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        body = environment.getWorld().createBody(bodyDef);

        CircleShape circle = new CircleShape();
        // Create a fixture definition to apply our shape to
        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = circle;
        fixtureDef.density = 1f; // mass / (float) (Math.PI * radius * radius);
        fixtureDef.friction = 0.8f;
        fixtureDef.restitution = 0.6f;

        // Create our fixture and attach it to the body
        fixture = body.createFixture(fixtureDef);
        fixture.setUserData(this);
        body.setUserData(this);

        circle.dispose();
    }

    public boolean isDead() {
        return dead;
    }


    public void handleDeath() {
    }

    public void kill() {
        if (hasHandledDeath)
            return;

        hasHandledDeath = true;
        dead = true;
        environment.getWorld().destroyBody(body);
    }
}
