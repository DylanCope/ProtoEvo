package com.protoevo.physics.box2d;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.physics.box2d.joints.DistanceJointDef;
import com.badlogic.gdx.physics.box2d.joints.RopeJoint;
import com.badlogic.gdx.physics.box2d.joints.RopeJointDef;
import com.badlogic.gdx.utils.Array;
import com.protoevo.physics.Joining;
import com.protoevo.physics.JointsManager;
import com.protoevo.physics.Particle;
import com.protoevo.physics.Physics;

import java.util.Optional;


public class Box2DJointsManager extends JointsManager {
    public static long serialVersionUID = 1L;

    public Box2DJointsManager(Box2DPhysics physics) {
        super(physics);
    }

    public void rebuild(Physics physics) {
        this.physics = physics;
        joinings.entrySet().removeIf(
                entry -> !entry.getValue().getParticleA().isPresent()
                        || !entry.getValue().getParticleB().isPresent()
        );
        jointsToAdd.clear();
        jointsToAdd.addAll(joinings.values());
        flushJoints();
    }

    private boolean jointDefIsStale(JointDef jointDef) {

        if (jointDef.bodyA == null || jointDef.bodyB == null)
            return true;
        if (jointDef.bodyA.getUserData() == null || jointDef.bodyB.getUserData() == null)
            return true;
        if (jointDef.bodyA.getUserData() instanceof Box2DParticle) {
            Box2DParticle particleA = (Box2DParticle) jointDef.bodyA.getUserData();
            if (particleA.isDead())
                return true;
        }
        if (jointDef.bodyB.getUserData() instanceof Box2DParticle) {
            Box2DParticle particleB = (Box2DParticle) jointDef.bodyB.getUserData();
            return particleB.isDead();
        }

        return false;
    }

    private World getWorld() {
        return ((Box2DPhysics) physics).getWorld();
    }

    public void flushJoints() {
        handleStaleJoints();

        for (Joining joining : jointsToAdd) {
            if (joining.anyDied()) {
                deregisterJoining(joining);
                continue;
            }
            JointDef jointDef = makeJointDef(joining);
            if (jointDefIsStale(jointDef)) {
                deregisterJoining(joining);
            } else {
                getWorld().createJoint(jointDef);
                joinings.put(joining.id, joining);
            }
        }
        jointsToAdd.clear();
    }

    private Joint getJoint(Joining joining) {
        Optional<Particle> maybeA = joining.getParticleA();
        Optional<Particle> maybeB = joining.getParticleB();
        if (!maybeA.isPresent() || !maybeB.isPresent())
            return null;
        Box2DParticle particleA = (Box2DParticle) maybeA.get();
        Box2DParticle particleB = (Box2DParticle) maybeB.get();

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

    public void handleStaleJoints() {
        for (Joining joining : joinings.values()) {

            if (joining.anyDied()) {
                requestJointRemoval(joining);
                continue;
            }

            Joint joint = getJoint(joining);
            if (joint == null)
                continue;

            Body bodyA = joint.getBodyA();
            Body bodyB = joint.getBodyB();

            if (bodyA == null || bodyB == null) {
                requestJointRemoval(joining);
                continue;
            }

            if (joining.notAnchored()
                    && bodyA.getUserData() instanceof Box2DParticle
                    && bodyB.getUserData() instanceof Box2DParticle) {
                handleGrowingParticle(joining);
            }
        }

        for (long joiningID : jointRemovalRequests) {
            if (!joinings.containsKey(joiningID))
                continue;

            Joining joining = joinings.get(joiningID);
            deregisterJoining(joining);
            joinings.remove(joining.id);
            destroyJoining(joining);
        }
        jointRemovalRequests.clear();
    }

    public void destroyJoining(Joining joining) {
        Joint joint = getJoint(joining);
        if (joint != null) {
            getWorld().destroyJoint(joint);
        }
    }

    private void handleGrowingParticle(Joining joining) {
        Joint joint = getJoint(joining);

        if (joint instanceof RopeJoint) {
            float idealJointLength = joining.getIdealLength();
            RopeJoint ropeJoint = (RopeJoint) joint;
            if (1.05f * ropeJoint.getMaxLength() < idealJointLength) {
                getWorld().destroyJoint(joint);
                jointsToAdd.add(joining);
            }
        }
    }

    private RopeJointDef makeRopeJointDef(Joining joining,
                                          Box2DParticle particleA,
                                          Box2DParticle particleB,
                                          Vector2 anchorA,
                                          Vector2 anchorB) {

        RopeJointDef jointDef = new RopeJointDef();
        jointDef.maxLength = joining.getIdealLength();
        jointDef.bodyA = particleA.getBody();
        jointDef.bodyB = particleB.getBody();

        if (joining.anchoredA) {
            Vector2 anchorALocal = jointDef.bodyA.getLocalPoint(anchorA).cpy();
            jointDef.localAnchorA.set(anchorALocal.setLength(particleA.getRadius()));
        }
        else
            jointDef.localAnchorA.set(0, 0);

        if (joining.anchoredB) {
            Vector2 anchorBLocal = jointDef.bodyB.getLocalPoint(anchorB).cpy();
            jointDef.localAnchorB.set(anchorBLocal.setLength(particleB.getRadius()));
        }
        else
            jointDef.localAnchorB.set(0, 0);

        return jointDef;
    }

    private DistanceJointDef makeDistanceJointDef(Joining joining,
                                                  Box2DParticle particleA,
                                                  Box2DParticle particleB,
                                                  Vector2 anchorA,
                                                  Vector2 anchorB) {
        DistanceJointDef jointDef = new DistanceJointDef();
        jointDef.length = joining.getIdealLength();
        jointDef.bodyA = particleA.getBody();
        jointDef.bodyB = particleB.getBody();
        Joining.DistanceMetaData metaData = (Joining.DistanceMetaData) joining.getMetaData();
        jointDef.dampingRatio = metaData.dampingRatio;
        jointDef.frequencyHz = metaData.frequencyHz;

        if (joining.anchoredA) {
            Vector2 anchorALocal = jointDef.bodyA.getLocalPoint(anchorA).cpy();
            jointDef.localAnchorA.set(anchorALocal.setLength(particleA.getRadius()));
        }
        else
            jointDef.localAnchorA.set(0, 0);

        if (joining.anchoredB) {
            Vector2 anchorBLocal = jointDef.bodyB.getLocalPoint(anchorB).cpy();
            jointDef.localAnchorB.set(anchorBLocal.setLength(particleB.getRadius()));
        }
        else
            jointDef.localAnchorB.set(0, 0);

        return jointDef;
    }

    private JointDef makeJointDef(Joining joining) {
        Optional<Particle> maybeParticleA = joining.getParticleA();
        Optional<Particle> maybeParticleB = joining.getParticleB();

        if (!maybeParticleA.isPresent() || !maybeParticleB.isPresent())
            return null;

        Box2DParticle particleA = (Box2DParticle) maybeParticleA.get();
        Box2DParticle particleB = (Box2DParticle) maybeParticleB.get();

        Optional<Vector2> maybeAnchorA = joining.getAnchorA();
        Optional<Vector2> maybeAnchorB = joining.getAnchorB();

        Vector2 anchorA = maybeAnchorA.orElse(particleA.getPos());
        Vector2 anchorB = maybeAnchorB.orElse(particleB.getPos());

        Joining.MetaData metaData = joining.getMetaData();
        Joining.Type type = metaData.getType();

        JointDef jointDef;
        if (type == Joining.Type.ROPE) {
            jointDef = makeRopeJointDef(joining, particleA, particleB, anchorA, anchorB);
        }
        else if (type == Joining.Type.DISTANCE) {
            jointDef = makeDistanceJointDef(joining, particleA, particleB, anchorA, anchorB);
        }
        else {
            throw new RuntimeException("Unknown joint type: " + type);
        }

        jointDef.bodyA = particleA.getBody();
        jointDef.bodyB = particleB.getBody();

        jointDef.collideConnected = true;
        return jointDef;
    }
}
