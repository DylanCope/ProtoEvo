package com.protoevo.env;

import com.badlogic.gdx.math.Vector2;
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
        public Particle particleA, particleB;
        public float anchorAngleA, anchorAngleB;
        private final Vector2 anchorA = new Vector2(), anchorB = new Vector2();


        public JoinedParticles(
                Particle particleA, Particle particleB,
                float anchorAngleA, float anchorAngleB) {
            this.particleA = particleA;
            this.particleB = particleB;
            this.anchorAngleA = anchorAngleA;
            this.anchorAngleB = anchorAngleB;
        }

        public Vector2 getAnchorA() {
            anchorA.set(particleA.getRadius(), 0)
                    .rotateRad(anchorAngleA + particleA.getAngle())
                    .add(particleA.getPos());
            return anchorA;
        }

        public Vector2 getAnchorB() {
            anchorB.set(particleB.getRadius(), 0)
                    .rotateRad(anchorAngleB + particleB.getAngle())
                    .add(particleB.getPos());
            return anchorB;
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

        public Particle getOther(Particle particle) {
            if (particle == particleA)
                return particleB;
            else if (particle == particleB)
                return particleA;

            throw new IllegalArgumentException("Particle is not part of this binding");
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

    private void destroyJoint(Joint joint) {
        environment.getWorld().destroyJoint(joint);
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
                destroyJoint(joint);
                continue;
            }

            if (bodyA.getUserData() instanceof Particle
                    && bodyB.getUserData() instanceof Particle) {
                Particle p1 = (Particle) joint.getBodyA().getUserData();
                Particle p2 = (Particle) joint.getBodyB().getUserData();

                if (p1.isDead() || p2.isDead()) {
                    destroyJoint(joint);
                }

                handleGrowingParticle(joint, p1, p2);
            }
        }

        for (JoinedParticles joining : jointRemovalRequests) {
            particleBindings.remove(joining);
            if (joining.particleA.getJoints() == null)
                continue;
            for (JointEdge jointEdge : joining.particleA.getJoints()) {
                if ((jointEdge.joint.getBodyA() == joining.particleB.getBody()
                        || jointEdge.joint.getBodyB() == joining.particleB.getBody())
                        && !(jointEdge.joint.getBodyA() == null || jointEdge.joint.getBodyB() == null)) {
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
                Vector2 anchorA = ropeJoint.getLocalAnchorA();
                Vector2 anchorB = ropeJoint.getLocalAnchorB();
                jointsToAdd.add(makeJointDef(p1, p2, anchorA, anchorB));
            }
        }
    }

    public static float idealJointLength(Particle p1, Particle p2) {
        return (p1.getRadius() + p2.getRadius()) * .2f;
    }

    private JointDef makeJointDef(Particle particleA, Particle particleB, Vector2 anchorA, Vector2 anchorB) {
        RopeJointDef defJoint = new RopeJointDef();
        defJoint.maxLength = idealJointLength(particleA, particleB);
        defJoint.bodyA = particleA.getBody();
        defJoint.bodyB = particleB.getBody();
        defJoint.localAnchorA.set(anchorA.setLength(particleA.getRadius()));
        defJoint.localAnchorB.set(anchorB.setLength(particleB.getRadius()));
        defJoint.collideConnected = true;
        return defJoint;
    }

    public boolean joiningExists(JoinedParticles joining) {
        Body bodyA = joining.particleA.getBody();
        Body bodyB = joining.particleB.getBody();
        return particleBindings.contains(joining) || jointsToAdd.stream().anyMatch(jointDef ->
                (jointDef.bodyA == bodyA && jointDef.bodyB == bodyB)
                        || (jointDef.bodyA == bodyB && jointDef.bodyB == bodyA));
    }

    public void createJoint(JoinedParticles joining) {
        if (!joiningExists(joining)) {
            jointsToAdd.add(makeJointDef(joining.particleA, joining.particleB,
                    joining.getAnchorA(), joining.getAnchorB()));
        }
    }

    public void createJoint(CollisionHandler.FixtureCollision contact, Body bodyA, Body bodyB) {
        if (jointsToAdd.stream().anyMatch(jointDef ->
                (jointDef.bodyA == bodyA && jointDef.bodyB == bodyB)
                        || (jointDef.bodyA == bodyB && jointDef.bodyB == bodyA)))
            return;

        if (bodyA.getUserData() instanceof Particle
                && bodyB.getUserData() instanceof Particle) {
            Particle particleA = (Particle) bodyA.getUserData();
            Particle particleB = (Particle) bodyB.getUserData();

            Vector2 anchorA, anchorB;
            if (contact.anchorA != null && contact.anchorB != null) {
                anchorA = bodyA.getLocalPoint(contact.anchorA);
                anchorB = bodyB.getLocalPoint(contact.anchorB);
            } else {
                anchorA = bodyA.getLocalPoint(contact.point);
                anchorB = bodyB.getLocalPoint(contact.point);
            }
            jointsToAdd.add(makeJointDef(particleA, particleB, anchorA, anchorB));
        }
    }

    public void requestJointRemoval(Particle particleA, Particle particleB) {
        jointRemovalRequests.add(new JoinedParticles(particleA, particleB));
    }

    public void requestJointRemoval(JoinedParticles joining) {
        jointRemovalRequests.add(joining);
    }
}
