package com.protoevo.physics;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.protoevo.biology.CauseOfDeath;
import com.protoevo.core.Statistics;
import com.protoevo.utils.Colour;
import com.protoevo.utils.Geometry;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public abstract class Particle implements Shape, Coloured {

    protected final Physics physics;
    protected boolean rangedInteractionsEnabled = false;

    public Particle(Physics physics) {
        this.physics = physics;
    }

    public Physics getPhysics() {
        return physics;
    }

    public abstract boolean isDead();
    public abstract void update(float delta);
    public abstract void physicsUpdate();
    public abstract float getRadius();
    public abstract void setRadius(double r);
    public abstract double getMassIfRadius(double r);
    public abstract float getMass();
    public abstract float getMassDensity();
    public abstract Vector2 getPos();
    public abstract Vector2 getVel();
    public abstract float getAngle();
    public abstract long getId();
    public abstract void setPos(Vector2 pos);
    public abstract void setAngle(float angle);
    public abstract void applyImpulse(Vector2 impulse);
    public abstract void applyForce(Vector2 force);
    public abstract void applyTorque(float torque);

    public abstract Map<Long, Long> getJoiningIds();
    public abstract Optional<Joining> getJoining(long joiningId);
    public abstract void requestJointRemoval(Joining joining);
    public abstract Collection<Collision> getContacts();
    public abstract Collection<Object> getInteractionQueue();
    public abstract Object getUserData();
    public abstract <T> T getUserData(Class<T> type);
    public abstract void setUserData(Object userData);

    public abstract Statistics getStats();
    public abstract Statistics getDebugStats();

    public abstract void kill(CauseOfDeath causeOfDeath);
    public abstract CauseOfDeath getCauseOfDeath();
    public abstract void dispose();

    public float getSpeed() {
        return getVel().len();
    }

    public float getArea() {
        return Geometry.getCircleArea(getRadius());
    }

    public abstract void setVel(float vx, float vy);
    public abstract void setVel(Vector2 vel);
    public abstract void setAngularVel(float angularVel);

    public abstract void setRangedInteractionRadius(float radius);

    public void setCanInteractAtRange() {
        rangedInteractionsEnabled = true;
    }

    @Override
    public Colour getColour() {
        if (getUserData() instanceof Coloured)
            return ((Coloured) getUserData()).getColour();
        return Colour.WHITE;
    }
}
