package com.protoevo.env;

import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.physics.box2d.joints.RopeJoint;
import com.badlogic.gdx.physics.box2d.joints.RopeJointDef;
import com.badlogic.gdx.utils.Array;
import com.protoevo.core.Particle;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class JointsManager implements Serializable {
    public static long serialVersionUID = 1L;

    private final Environment environment;
    private final List<JointDef> jointsToAdd = new ArrayList<>();
    private final List<JointDef> jointsToRemove = new ArrayList<>();


    public JointsManager(Environment environment) {
        this.environment = environment;
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
        }
        jointsToAdd.clear();
    }

    private boolean removalRequested(Joint joint) {
        Body bodyA = joint.getBodyA();
        Body bodyB = joint.getBodyB();

        for (JointDef jointDef : jointsToRemove) {
            if (jointDef.bodyA == bodyA && jointDef.bodyB == bodyB && jointDef.type == joint.getType()) {
                environment.getWorld().destroyJoint(joint);
                return true;
            }
        }

        return false;
    }

    public void handleStaleJoints() {
        Array<Joint> joints = new Array<>();
        environment.getWorld().getJoints(joints);

        for (Joint joint : joints) {
            Body bodyA = joint.getBodyA();
            Body bodyB = joint.getBodyB();

            if (bodyA == null || bodyB == null) {
                environment.getWorld().destroyJoint(joint);
                continue;
            }

            if (removalRequested(joint)) {
                environment.getWorld().destroyJoint(joint);
                continue;
            }

            if (bodyA.getUserData() instanceof Particle
                    && bodyB.getUserData() instanceof Particle) {
                Particle p1 = (Particle) joint.getBodyA().getUserData();
                Particle p2 = (Particle) joint.getBodyB().getUserData();

                if (p1.isDead() || p2.isDead()) {
                    environment.getWorld().destroyJoint(joint);
                }

                handleGrowingParticle(joint, p1, p2);
            }
        }
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
        jointsToRemove.add(makeJointDef(particleA, particleB));
    }
}
