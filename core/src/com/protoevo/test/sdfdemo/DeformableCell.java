package com.protoevo.test.sdfdemo;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.protoevo.physics.Collision;
import com.protoevo.physics.Joining;
import com.protoevo.physics.Particle;
import com.protoevo.physics.Physics;
import com.protoevo.maths.Geometry;

import java.util.*;

public class DeformableCell {

    private final List<Particle> particles;
    private final Set<Long> particleIDs;
    private final Vector2 pos = new Vector2();
    private final Physics physics;

    public DeformableCell(Physics physics, Vector2 pos) {
        particles = new ArrayList<>();
        this.physics = physics;

        particleIDs = new HashSet<>();
        Particle particle = physics.createNewParticle();
        float particleR = 0.07f;

        particle.setRadius(particleR);
        particle.setPos(pos);
        particles.add(particle);

        float angle = (float) Math.random() * 2 * (float) Math.PI;
        for (int i = 0; i < 3; i++) {
            Particle nextParticle = physics.createNewParticle();
            nextParticle.setRadius(MathUtils.random(particleR * 0.25f, particleR));
            Vector2 dir = Geometry.fromAngle(angle).setLength(particle.getRadius() + nextParticle.getRadius());
            nextParticle.setPos(particle.getPos().cpy().add(dir));
            angle += 2f * (MathUtils.random() * Math.PI) / 3f;
            joinParticles(particle, nextParticle);
            particle = nextParticle;
            particles.add(particle);
        }
    }

    private void joinParticles(Particle p1, Particle p2) {
        Joining joining = physics.joinParticlesFromCentres(p1, p2);
        joining.setMetaData(new Joining.DistanceMetaData(.1f, 4f));
    }

    public void handleInternalForces() {

        for (Particle particle : particles) {
            if (particle.isDead())
                continue;

            Collection<Collision> collisions = particle.getContacts();
            if (!collisions.isEmpty()) {
                for (Collision collision : collisions) {
                    if (collision.getOther(particle) instanceof Particle
                            && !((Particle) collision.getOther(particle)).isDead()
                            && particleIDs.contains(((Particle) collision.getOther(particle)).getId())
                            && !physics.areJoined(particle, (Particle) collision.getOther(particle))) {
                        Particle other = (Particle) collision.getOther(particle);
                        joinParticles(particle, other);
                    }
                }
            }
            else {
                Particle closestParticle = null;
                float closestDist2 = Float.MAX_VALUE;
                for (Particle other : particles) {
                    if (other == particle)
                        continue;
                    float dist2 = particle.getPos().dst2(other.getPos());
                    if (dist2 < closestDist2) {
                        closestDist2 = dist2;
                        closestParticle = other;
                    }
                }
                if (closestParticle != null && !physics.areJoined(particle, closestParticle)) {
                    joinParticles(particle, closestParticle);
                }
            }
        }

        Vector2 cellPos = getPos();
        float internalAttractiveForceStrength = 5e-4f;
        Vector2 excessImpulse = new Vector2(0, 0);
        for (Particle particle : particles) {
            Vector2 impulse = cellPos.cpy().sub(particle.getPos()).scl(internalAttractiveForceStrength);
            excessImpulse.add(impulse);
            particle.applyImpulse(impulse);
        }
        excessImpulse.scl(-1f / particles.size());
        for (Particle particle : particles) {
            particle.applyImpulse(excessImpulse);
        }
    }

    public void physicsUpdate() {
        handleInternalForces();
        for (Particle particle : getParticles()) {
            particle.physicsUpdate();
        }
    }

    public Vector2 getPos() {
        pos.set(0, 0);
        for (Particle particle : particles) {
            pos.add(particle.getPos());
        }
        pos.scl(1f / particles.size());
        return pos;
    }

    public void update(float delta) {
        particles.removeIf(Particle::isDead);
        particleIDs.clear();
        for (Particle particle : particles) {
            particleIDs.add(particle.getId());
            particle.update(delta);
        }
    }

    public List<Particle> getParticles() {
        return particles;
    }
}
