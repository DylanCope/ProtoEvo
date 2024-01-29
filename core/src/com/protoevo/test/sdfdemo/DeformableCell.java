package com.protoevo.test.sdfdemo;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.protoevo.physics.Collision;
import com.protoevo.physics.Particle;
import com.protoevo.physics.Physics;
import com.protoevo.maths.Geometry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DeformableCell {

    private final List<Particle> particles;
    private final Vector2 pos = new Vector2();
    private final Physics physics;

    public DeformableCell(Physics physics) {
        particles = new ArrayList<>();
        this.physics = physics;

        Particle particle = physics.createNewParticle();
        float particleR = 0.07f;

        particle.setRadius(particleR);
        particle.setPos(new Vector2(0, 0));
        particles.add(particle);

//        Particle particle2 = physics.createNewParticle();
//        particle2.setRadius(particleR);
//        particle2.setPos(new Vector2(2*particleR, 0));
//        particles.add(particle2);
//
//        Particle particle3 = physics.createNewParticle();
//        particle3.setRadius(particleR*0.5);
//        particle3.setPos(new Vector2(0, 1.5f*particleR));
//        particles.add(particle3);
//
//        Particle particle4 = physics.createNewParticle();
//        particle4.setRadius(particleR * 0.25);
//        particle4.setPos(new Vector2(2*particleR, 2*particleR));
//        particles.add(particle4);

        float angle = (float) Math.random() * 2 * (float) Math.PI;
        for (int i = 0; i < 8; i++) {
            Particle nextParticle = physics.createNewParticle();
            nextParticle.setRadius(MathUtils.random(particleR * 0.25f, particleR));
            Vector2 dir = Geometry.fromAngle(angle).setLength(particle.getRadius() + nextParticle.getRadius());
            nextParticle.setPos(particle.getPos().cpy().add(dir));
            angle += 2f * (MathUtils.random() * Math.PI) / 3f;
            physics.joinParticles(particle, nextParticle);
            particle = nextParticle;
            particles.add(particle);
        }
    }

    public void handleInternalForces() {
        particles.removeIf(Particle::isDead);
        Vector2 cellPos = getPos();

        for (Particle particle : particles) {
            Collection<Collision> collisions = particle.getContacts();
            if (!collisions.isEmpty())
                for (Collision collision : collisions) {
                    if (collision.getOther(particle) instanceof Particle
                        && !physics.areJoined(particle, (Particle) collision.getOther(particle))) {
                        Particle other = (Particle) collision.getOther(particle);
                        physics.joinParticlesFromCentres(particle, other);
                    }
                }
        }

        float internalForceStrength = 1e-3f;
        for (Particle particle : particles) {
            particle.applyImpulse(cellPos.cpy().sub(particle.getPos()).scl(internalForceStrength));
        }

        Vector2 excessImpulse = new Vector2(0, 0);
        for (Particle particle : particles) {
            excessImpulse.add(particle.getImpulse());
        }

        for (Particle particle : particles) {
            particle.applyImpulse(excessImpulse.cpy().scl(-1f / particles.size()));
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
        for (Particle particle : particles) {
            particle.update(delta);
        }
    }

    public List<Particle> getParticles() {
        return particles;
    }
}
