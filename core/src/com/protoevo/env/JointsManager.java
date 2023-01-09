package com.protoevo.env;

import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.physics.box2d.joints.RopeJoint;
import com.badlogic.gdx.physics.box2d.joints.RopeJointDef;
import com.badlogic.gdx.utils.Array;
import com.protoevo.core.Particle;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class JointsManager implements Serializable {
    public static long serialVersionUID = 1L;

    public static class JoinedParticles implements Serializable {
        public static long serialVersionUID = 1L;
        public Particle particleA;
        public Particle particleB;

        public JoinedParticles(Particle particleA, Particle particleB) {
            this.particleA = particleA;
            this.particleB = particleB;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) return false;
            if (o == this) return true;
            if (!(o instanceof JoinedParticles)) return false;
            JoinedParticles jp = (JoinedParticles) o;
            return (particleA == jp.particleA && particleB == jp.particleB)
                    | (particleA == jp.particleB && particleB == jp.particleA);
        }

        @Override
        public int hashCode() {
            return particleA.hashCode() + particleB.hashCode();
        }
    }

    private final Environment environment;
    private final List<JointDef> jointsToAdd = new ArrayList<>();
    private final ConcurrentLinkedQueue<JoinedParticles> jointRemovalRequests = new ConcurrentLinkedQueue<>();

    private final Set<JoinedParticles> particleBindings = new HashSet<>();

    public JointsManager(Environment environment) {
        this.environment = environment;
    }

    public Collection<JoinedParticles> getParticleBindings() {
        return particleBindings;
    }

    public void flushJoints() {
        handleStaleJoints();

        for (JointDef jointDef : jointsToAdd) {
            if (jointDef.bodyA == null || jointDef.bodyB == null)
                continue;
            if (jointDef.bodyA.getUserData() == null || jointDef.bodyB.getUserData() == null)
                continue;
            if (jointDef.bodyA.getUserData() instanceof Particle) {
                Particle particleA = (Particle) jointDef.bodyA.getUserData();
                if (particleA.isDead())
                    continue;
            }
            if (jointDef.bodyB.getUserData() instanceof Particle) {
                Particle particleB = (Particle) jointDef.bodyB.getUserData();
                if (particleB.isDead())
                    continue;
            }
            environment.getWorld().createJoint(jointDef);
            JoinedParticles joining = new JoinedParticles(
                    (Particle) jointDef.bodyA.getUserData(),
                    (Particle) jointDef.bodyB.getUserData());
            particleBindings.add(joining);
        }
        jointsToAdd.clear();
    }

    private boolean removalRequested(Joint joint) {
        Body bodyA = joint.getBodyA();
        Body bodyB = joint.getBodyB();
        Particle p1 = (Particle) joint.getBodyA().getUserData();
        Particle p2 = (Particle) joint.getBodyB().getUserData();

        return jointRemovalRequests.contains(new JoinedParticles(p1, p2));
    }

    private void destroyJointAndUpdateJoinedParticles(Joint joint) {
        environment.getWorld().destroyJoint(joint);
        Particle p1 = (Particle) joint.getBodyA().getUserData();
        Particle p2 = (Particle) joint.getBodyB().getUserData();
    }

    public void handleStaleJoints() {
        Array<Joint> joints = new Array<>();
        environment.getWorld().getJoints(joints);

        particleBindings.removeIf(joinedParticles ->
            joinedParticles.particleA.isDead() || joinedParticles.particleB.isDead()
        );

        for (Joint joint : joints) {
            Body bodyA = joint.getBodyA();
            Body bodyB = joint.getBodyB();

            if (bodyA == null || bodyB == null) {
                destroyJointAndUpdateJoinedParticles(joint);
                continue;
            }

            if (bodyA.getUserData() instanceof Particle
                    && bodyB.getUserData() instanceof Particle) {
                Particle p1 = (Particle) joint.getBodyA().getUserData();
                Particle p2 = (Particle) joint.getBodyB().getUserData();

                if (p1.isDead() || p2.isDead()) {
                    destroyJointAndUpdateJoinedParticles(joint);
                }

                handleGrowingParticle(joint, p1, p2);
            }
        }

        for (JoinedParticles joining : jointRemovalRequests) {
            particleBindings.remove(joining);
            for (JointEdge jointEdge : joining.particleA.getJoints()) {
                if (jointEdge.joint.getBodyA() == joining.particleB.getBody()
                        || jointEdge.joint.getBodyB() == joining.particleB.getBody()) {
                    environment.getWorld().destroyJoint(jointEdge.joint);
                }
            }
        }
        jointRemovalRequests.clear();
    }

    private void handleGrowingParticle(Joint joint, Particle p1, Particle p2) {
        if (joint instanceof RopeJoint) {
            float idealJointLength = (p1.getRadius() + p2.getRadius()) * 1.1f;
            RopeJoint ropeJoint = (RopeJoint) joint;
            if (1.05f * ropeJoint.getMaxLength() < idealJointLength) {
                environment.getWorld().destroyJoint(joint);
                jointsToAdd.add(makeJointDef(p1, p2));
            }
        }
    }

    public float idealJointLength(Particle p1, Particle p2) {
        return (p1.getRadius() + p2.getRadius()) * 1.2f;
    }

    private JointDef makeJointDef(Particle particleA, Particle particleB) {
        RopeJointDef defJoint = new RopeJointDef();
        defJoint.maxLength = idealJointLength(particleA, particleB);
        defJoint.bodyA = particleA.getBody();
        defJoint.bodyB = particleB.getBody();
        defJoint.localAnchorA.set(0, 0);
        defJoint.localAnchorB.set(0, 0);
        defJoint.collideConnected = true;
        return defJoint;
    }

    public void createJoint(Body bodyA, Body bodyB) {
        int maxJoints = 2;
        if (bodyA.getJointList().size >= maxJoints || bodyB.getJointList().size >= maxJoints)
            return;

        if (bodyA.getUserData() instanceof Particle
                && bodyB.getUserData() instanceof Particle) {
            Particle particleA = (Particle) bodyA.getUserData();
            Particle particleB = (Particle) bodyB.getUserData();
            jointsToAdd.add(makeJointDef(particleA, particleB));
        }
    }

    public void requestJointRemoval(Particle particleA, Particle particleB) {
        jointRemovalRequests.add(new JoinedParticles(particleA, particleB));
    }
}
