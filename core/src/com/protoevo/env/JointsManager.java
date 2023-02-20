package com.protoevo.env;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.physics.box2d.joints.RopeJoint;
import com.badlogic.gdx.physics.box2d.joints.RopeJointDef;
import com.badlogic.gdx.utils.Array;
import com.protoevo.biology.cells.Cell;
import com.protoevo.core.Particle;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class JointsManager implements Serializable {
    public static long serialVersionUID = 1L;

    public static class Joining implements Serializable {
        public static long serialVersionUID = 1L;
        public Particle particleA, particleB;
        public float anchorAngleA, anchorAngleB;
        public boolean anchoredA, anchoredB;
        private final Vector2 anchorA = new Vector2(), anchorB = new Vector2();

        public Joining(Particle particleA, Particle particleB) {
            this.particleA = particleA;
            this.particleB = particleB;
            anchoredB = anchoredA = false;
        }

        public Joining(
                Particle particleA, Particle particleB,
                float anchorAngleA, float anchorAngleB) {
            this.particleA = particleA;
            this.particleB = particleB;
            this.anchorAngleA = anchorAngleA;
            this.anchorAngleB = anchorAngleB;
            anchoredB = anchoredA = true;
        }

        public Vector2 getAnchorA() {
            float t = anchorAngleA + particleA.getAngle();
            float r = particleA.getRadius();
            anchorA.set((float) (r * Math.cos(t)), (float) (r * Math.sin(t)))
                    .add(particleA.getPos());
            return anchorA;
        }

        public Vector2 getAnchorB() {
            float t = anchorAngleB + particleB.getAngle();
            float r = particleB.getRadius();
            anchorB.set((float) (r * Math.cos(t)), (float) (r * Math.sin(t)))
                    .add(particleB.getPos());
            return anchorB;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) return false;
            if (o == this) return true;
            if (!(o instanceof Joining)) return false;
            Joining jp = (Joining) o;
            return (particleA == jp.particleA && particleB == jp.particleB)
                    | (particleA == jp.particleB && particleB == jp.particleA);
        }

        @Override
        public int hashCode() {
            return particleA.hashCode() + particleB.hashCode();
        }

        public Particle getOther(Particle particle) {
            if (particle == particleA)
                return particleB;
            else if (particle == particleB)
                return particleA;

            throw new IllegalArgumentException("Particle is not part of this binding");
        }

        public Joint getJoint() {
            Array<JointEdge> joints = particleA.getJoints();
            Body otherBody = particleB.getBody();
            if (joints == null) {
                joints = particleB.getJoints();
                otherBody = particleA.getBody();
            }
            if (joints == null)
                return null;

            for (JointEdge jointEdge : joints) {
                if ((jointEdge.joint.getBodyA() == otherBody
                        || jointEdge.joint.getBodyB() == otherBody)
                        && !(jointEdge.joint.getBodyA() == null || jointEdge.joint.getBodyB() == null)) {
                    return jointEdge.joint;
                }
            }
            return null;
        }

        public void destroy() {
            Environment env = particleA.getEnv();
            Joint joint = getJoint();
            while (joint != null) {
                env.getWorld().destroyJoint(joint);
                joint = getJoint();
            }
        }

        public float getIdealLength() {
            float len = JointsManager.idealJointLength(particleA, particleB);
            if (!anchoredA)
                len += particleA.getRadius();
            if (!anchoredB)
                len += particleB.getRadius();
            return len;
        }

        public float getMaxLength() {
            return getIdealLength() * 1.2f;
        }
    }

    private final Environment environment;
    private final Collection<Joining> jointsToAdd = new ConcurrentLinkedQueue<>();
    private final Collection<Joining> jointRemovalRequests = new ConcurrentLinkedQueue<>();
    private final Collection<Joining> particleBindings = new HashSet<>();

    public JointsManager(Environment environment) {
        this.environment = environment;
    }

    public Collection<Joining> getParticleBindings() {
        return particleBindings;
    }

    private boolean jointDefIsStale(JointDef jointDef) {

        if (jointDef.bodyA == null || jointDef.bodyB == null)
            return true;
        if (jointDef.bodyA.getUserData() == null || jointDef.bodyB.getUserData() == null)
            return true;
        if (jointDef.bodyA.getUserData() instanceof Particle) {
            Particle particleA = (Particle) jointDef.bodyA.getUserData();
            if (particleA.isDead())
                return true;
        }
        if (jointDef.bodyB.getUserData() instanceof Particle) {
            Particle particleB = (Particle) jointDef.bodyB.getUserData();
            return particleB.isDead();
        }

        return false;

    }

    public void flushJoints() {
        handleStaleJoints();

        for (Joining joining : jointsToAdd) {
            if (joining.particleA.isDead() || joining.particleB.isDead()) {
                deregisterJoining(joining);
                continue;
            }
            JointDef jointDef = makeJointDef(joining);
            if (jointDefIsStale(jointDef)) {
                deregisterJoining(joining);
            } else {
                environment.getWorld().createJoint(jointDef);
                particleBindings.add(joining);
            }
        }
        jointsToAdd.clear();
    }

    public void handleStaleJoints() {
        for (Joining joining : particleBindings) {

            if (joining.particleA.isDead() || joining.particleB.isDead()) {
                requestJointRemoval(joining);
                continue;
            }

            Joint joint = joining.getJoint();
            if (joint == null)
                continue;

            Body bodyA = joint.getBodyA();
            Body bodyB = joint.getBodyB();

            if (bodyA == null || bodyB == null) {
                requestJointRemoval(joining);
                continue;
            }

            if (bodyA.getUserData() instanceof Particle
                    && bodyB.getUserData() instanceof Particle) {
                handleGrowingParticle(joining);
            }
        }

        for (Joining joining : jointRemovalRequests) {
            if (!particleBindings.contains(joining))
                continue;

            deregisterJoining(joining);
            particleBindings.remove(joining);
            joining.destroy();
        }
        jointRemovalRequests.clear();
    }

    private void handleGrowingParticle(Joining joining) {
        Joint joint = joining.getJoint();

        if (joint instanceof RopeJoint) {
            float idealJointLength = joining.getIdealLength();
            RopeJoint ropeJoint = (RopeJoint) joint;
            if (1.05f * ropeJoint.getMaxLength() < idealJointLength) {
                environment.getWorld().destroyJoint(joint);
                jointsToAdd.add(joining);
            }
        }
    }

    public static float idealJointLength(Particle p1, Particle p2) {
        return (p1.getRadius() + p2.getRadius()) * .2f;
    }


    private JointDef makeJointDef(Joining joining) {
        Particle particleA = joining.particleA;
        Particle particleB = joining.particleB;
        Vector2 anchorA = joining.anchorA;
        Vector2 anchorB = joining.anchorB;

        RopeJointDef defJoint = new RopeJointDef();
        defJoint.maxLength = joining.getIdealLength();
        defJoint.bodyA = particleA.getBody();
        defJoint.bodyB = particleB.getBody();

        if (joining.anchoredA) {
            anchorA = defJoint.bodyA.getLocalPoint(anchorA);
            defJoint.localAnchorA.set(anchorA.setLength(particleA.getRadius()));
        }
        else
            defJoint.localAnchorA.set(0, 0);

        if (joining.anchoredB) {
            anchorB = defJoint.bodyB.getLocalPoint(anchorB);
            defJoint.localAnchorB.set(anchorB.setLength(particleB.getRadius()));
        }
        else
            defJoint.localAnchorB.set(0, 0);

        defJoint.collideConnected = true;
        return defJoint;
    }

    public boolean joiningExists(Joining joining) {
        return jointsToAdd.contains(joining)
                || particleBindings.contains(joining)
                || jointRemovalRequests.contains(joining);
    }

    public void createJoint(Joining joining) {
        if (!joiningExists(joining)) {
            jointsToAdd.add(joining);
        }
    }

    private void deregisterJoining(Joining joining) {
        if (joining.particleA instanceof Cell)
            ((Cell) joining.particleA).deregisterJoining(joining);
        if (joining.particleB instanceof Cell)
            ((Cell) joining.particleB).deregisterJoining(joining);
    }

    public void requestJointRemoval(Joining joining) {
        if (!jointRemovalRequests.contains(joining)) {
            deregisterJoining(joining);
            jointRemovalRequests.add(joining);
        }
    }
}
