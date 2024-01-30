package com.protoevo.physics;

import com.badlogic.gdx.math.Vector2;
import com.protoevo.biology.CauseOfDeath;
import com.protoevo.core.Statistics;
import com.protoevo.maths.Geometry;
import com.protoevo.maths.Shape;
import com.protoevo.utils.Colour;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public abstract class Particle implements Serializable, Shape, Coloured {
    private static final long serialVersionUID = 1L;

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
    public abstract Vector2 getImpulse();
    public abstract void applyForce(Vector2 force);
    public abstract Vector2 getForce();
    public abstract void applyTorque(float torque);
    public abstract float getTorque();

    /**
     * @return A map from particle ids to joining ids
     */
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

    public boolean canInteractAtRange() {
        return rangedInteractionsEnabled;
    }

    public abstract void rebuildTransientFields();

    @Override
    public Colour getColour() {
        if (getUserData() instanceof Coloured)
            return ((Coloured) getUserData()).getColour();
        return Colour.WHITE;
    }
}
