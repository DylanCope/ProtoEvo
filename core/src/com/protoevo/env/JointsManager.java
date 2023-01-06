package com.protoevo.env;

import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.physics.box2d.joints.RopeJoint;
import com.badlogic.gdx.physics.box2d.joints.RopeJointDef;
import com.badlogic.gdx.utils.Array;
import com.protoevo.core.Particle;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class JointsManager implements ContactListener, Serializable {
    public static long serialVersionUID = 1L;

    private final Environment environment;
    private final List<JointDef> jointsToAdd = new ArrayList<>();


    public JointsManager(Environment environment) {
        this.environment = environment;
    }

    public void flushJoints() {
        handleStaleJoints();

        for (JointDef jointDef : jointsToAdd) {
            environment.getWorld().createJoint(jointDef);
        }
        jointsToAdd.clear();
    }

    public void handleStaleJoints() {
        Array<Joint> joints = new Array<>();
        environment.getWorld().getJoints(joints);

        for (Joint joint : joints) {
            if (joint.getBodyA().getUserData() instanceof Particle
                    && joint.getBodyB().getUserData() instanceof Particle) {
                Particle p1 = (Particle) joint.getBodyA().getUserData();
                Particle p2 = (Particle) joint.getBodyB().getUserData();

                if (p1.isDead() || p2.isDead()) {
                    environment.getWorld().destroyJoint(joint);
                }

                if (joint instanceof RopeJoint) {
                    float idealJointLength = (p1.getRadius() + p2.getRadius()) * 1.1f;
                    RopeJoint ropeJoint = (RopeJoint) joint;
                    if (1.05f * ropeJoint.getMaxLength() < idealJointLength) {
                        environment.getWorld().destroyJoint(joint);
                        jointsToAdd.add(makeJointDef(p1, p2));
                    }
                }
            }
        }
    }

    public float idealJointLength(Particle p1, Particle p2) {
        return (p1.getRadius() + p2.getRadius()) * 1.2f;
    }

    public JointDef makeJointDef(Particle particleA, Particle particleB) {
        RopeJointDef defJoint = new RopeJointDef();
        defJoint.maxLength = idealJointLength(particleA, particleB);
        defJoint.bodyA = particleA.getBody();
        defJoint.bodyB = particleB.getBody();
        defJoint.collideConnected = true;
        return defJoint;
    }

    @Override
    public void beginContact(Contact contact) {}

    @Override
    public void endContact(Contact contact) {
        Body bodyA = contact.getFixtureA().getBody();
        Body bodyB = contact.getFixtureB().getBody();

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

    @Override
    public void preSolve(Contact contact, Manifold oldManifold) {}

    @Override
    public void postSolve(Contact contact, ContactImpulse impulse) {}
}
