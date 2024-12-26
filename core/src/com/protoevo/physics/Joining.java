package com.protoevo.physics;

import com.badlogic.gdx.math.Vector2;
import com.protoevo.physics.box2d.Box2DParticle;

import java.io.Serializable;
import java.util.Optional;

public class Joining implements Serializable {
    public static long serialVersionUID = 1L;
    public long id;

    public enum Type {
        DISTANCE, ROPE
    }

    public static abstract class MetaData implements Serializable {
        public static long serialVersionUID = 1L;

        public final Type type;

        public MetaData(Type type) {
            this.type = type;
        }

        public Type getType() {
            return type;
        }
    }

    public static class RopeMetaData extends MetaData {
        public static long serialVersionUID = 1L;
        public RopeMetaData() {
            super(Type.ROPE);
        }
    }

    public static class DistanceMetaData extends MetaData {
        public static long serialVersionUID = 1L;
        public float dampingRatio = 0f;
        public float frequencyHz = 0f;

        public DistanceMetaData() {
            super(Type.DISTANCE);
        }

        public DistanceMetaData(float dampingRatio, float frequencyHz) {
            super(Type.DISTANCE);
            this.dampingRatio = dampingRatio;
            this.frequencyHz = frequencyHz;
        }
    }

    public long particleAId, particleBId;
    public float anchorAngleA, anchorAngleB;
    public boolean anchoredA, anchoredB;
    private final Vector2 anchorA = new Vector2();
    private final Vector2 anchorB = new Vector2();
    private Physics physics;
    private MetaData metaData;

    public static long getId(long particleAId, long particleBId) {
        return particleAId ^ particleBId;
    }

    public Joining() {}

    public Joining(Particle particleA, Particle particleB) {
        particleAId = particleA.getId();
        particleBId = particleB.getId();
        physics = particleA.getPhysics();
        id = getId(particleAId, particleBId);
        anchoredB = anchoredA = false;
    }

    public Joining(
            Particle particleA, Particle particleB,
            float anchorAngleA, float anchorAngleB) {
        this(particleA, particleB);
        this.anchorAngleA = anchorAngleA;
        this.anchorAngleB = anchorAngleB;
        anchoredB = anchoredA = true;
    }

    public void setMetaData(MetaData metaData) {
        this.metaData = metaData;
    }

    public MetaData getMetaData() {
        if (metaData == null)
            metaData = new RopeMetaData();
        return metaData;
    }

    public Optional<Particle> getParticleA() {
        return physics.getParticle(particleAId);
    }

    public Optional<Particle> getParticleB() {
        return physics.getParticle(particleBId);
    }

    public boolean anyDied() {
        Optional<Particle> a = getParticleA();
        Optional<Particle> b = getParticleB();
        return a.map(Particle::isDead).orElse(true)
                || b.map(Particle::isDead).orElse(true);
    }

    private Vector2 getAnchor(
            Vector2 anchor, Particle particle, float anchorAngle, boolean anchored) {
        if (!anchored)
            return particle.getPos();

        float t = anchorAngle + particle.getAngle();
        float r = particle.getRadius();
        return anchor.set((float) (r * Math.cos(t)), (float) (r * Math.sin(t)))
                .add(particle.getPos());
    }

    public Optional<Vector2> getAnchorA() {
        return getParticleA().map(particle -> getAnchor(anchorA, particle, anchorAngleA, anchoredA));
    }

    public Optional<Vector2> getAnchorB() {
        return getParticleB().map(particle -> getAnchor(anchorB, particle, anchorAngleB, anchoredB));
    }

    public Optional<Vector2> getParticleAnchor(Particle particle) {
        if (particle.getId() == particleAId)
            return getAnchorA();
        else if (particle.getId() == particleBId)
            return getAnchorB();

        throw new IllegalArgumentException("Particle is not part of this binding");
    }

    public Optional<Float> getAnchorDist2() {
        Optional<Vector2> anchorA = getAnchorA();
        Optional<Vector2> anchorB = getAnchorB();
        if (!anchorA.isPresent() || !anchorB.isPresent())
            return Optional.empty();
        return Optional.of(anchorA.get().dst2(anchorB.get()));
    }

    public boolean maxLengthExceeded() {
        Optional<Float> anchorDist2 = getAnchorDist2();
        if (!anchorDist2.isPresent())
            return true;
        float maxLen = getMaxLength();
        return anchorDist2.get() > maxLen * maxLen;
    }

    public boolean notAnchored() {
        return !anchoredA && !anchoredB;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if (o == this) return true;
        if (!(o instanceof Joining)) return false;
        Joining jp = (Joining) o;
        return (particleAId == jp.particleAId && particleBId == jp.particleBId)
                || (particleAId == jp.particleBId && particleBId == jp.particleAId);
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }

    public Optional<Particle> getOther(Particle particle) {
        if (particle.getId() == particleAId)
            return getParticleB();
        else if (particle.getId() == particleBId)
            return getParticleA();

        throw new IllegalArgumentException("Particle is not part of this binding");
    }

    public long getOtherId(long id) {
        if (id == particleAId)
            return particleBId;
        else if (id == particleBId)
            return particleAId;

        throw new IllegalArgumentException("Particle is not part of this binding");
    }

    public float getIdealLength() {
        Optional<Particle> maybeA = getParticleA();
        Optional<Particle> maybeB = getParticleB();
        if (!maybeA.isPresent() || !maybeB.isPresent())
            return 0;
        Box2DParticle particleA = (Box2DParticle) maybeA.get();
        Box2DParticle particleB = (Box2DParticle) maybeB.get();

        float len = JointsManager.idealJoinedParticleDistance(particleA, particleB);
        if (!anchoredA)
            len += particleA.getRadius();
        if (!anchoredB)
            len += particleB.getRadius();
        return len;
    }

    public float getMaxLength() {
        return getIdealLength() * 1.5f;
    }
}
