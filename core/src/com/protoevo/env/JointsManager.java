package com.protoevo.env;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Joint;
import com.badlogic.gdx.physics.box2d.JointDef;
import com.badlogic.gdx.physics.box2d.JointEdge;
import com.badlogic.gdx.physics.box2d.joints.RopeJoint;
import com.badlogic.gdx.physics.box2d.joints.RopeJointDef;
import com.badlogic.gdx.utils.Array;
import com.protoevo.biology.cells.Cell;
import com.protoevo.physics.Particle;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class JointsManager implements Serializable {
    public static long serialVersionUID = 1L;
    private static JointsManager instance;

    @FunctionalInterface
    public interface JoiningListener extends Serializable {
        void onDestroyed(Joining joining);
    }

    public static class Joining implements Serializable {
        public static long serialVersionUID = 1L;
        public final long id;

        public long particleAId, particleBId;
        public float anchorAngleA, anchorAngleB;
        public boolean anchoredA, anchoredB;
        private final Vector2 anchorA = new Vector2();
        private final Vector2 anchorB = new Vector2();

        public Joining(Particle particleA, Particle particleB) {
            particleAId = particleA.getId();
            particleBId = particleB.getId();
            id = particleAId ^ particleBId;
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

        public Particle getParticleA() {
            return instance.environment.getCell(particleAId);
        }

        public Particle getParticleB() {
            return instance.environment.getCell(particleBId);
        }

        public boolean anyDied() {
            Particle a = getParticleA();
            Particle b = getParticleB();
            return a == null || b == null || a.isDead() || b.isDead();
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

        public Vector2 getAnchorA() {
            return getAnchor(anchorA, getParticleA(), anchorAngleA, anchoredA);
        }

        public Vector2 getAnchorB() {
            return getAnchor(anchorB, getParticleB(), anchorAngleB, anchoredB);
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

        public Particle getOther(Particle particle) {
            if (particle.getId() == particleAId)
                return getParticleB();
            else if (particle.getId() == particleBId)
                return getParticleA();

            throw new IllegalArgumentException("Particle is not part of this binding");
        }

        public Joint getJoint() {
            Particle particleA = getParticleA();
            Particle particleB = getParticleB();
            if (particleA == null || particleB == null)
                return null;

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
            Environment env = instance.environment;
            Joint joint = getJoint();
            while (joint != null) {
                env.getWorld().destroyJoint(joint);
                joint = getJoint();
            }
        }

        public float getIdealLength() {
            Particle particleA = getParticleA();
            Particle particleB = getParticleB();
            float len = JointsManager.idealJointLength(particleA, particleB);
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

    private Environment environment;
    private final Collection<Joining> jointsToAdd = new ConcurrentLinkedQueue<>();
    private final Collection<Long> jointRemovalRequests = new ConcurrentLinkedQueue<>();
    private final Map<Long, Joining> joinings = new ConcurrentHashMap<>();

    public JointsManager(Environment environment) {
        this.environment = environment;
        instance = this;
    }

    public Collection<Joining> getJoinings() {
        return joinings.values();
    }

    public void rebuild(Environment environment) {
        this.environment = environment;
        instance = this;
        joinings.entrySet().removeIf(
                entry -> entry.getValue().getParticleA() == null
                        || entry.getValue().getParticleB() == null
        );
        jointsToAdd.clear();
        jointsToAdd.addAll(joinings.values());
        flushJoints();
    }

    public Joining getJoining(long id) {
        if (joinings.containsKey(id))
            return joinings.get(id);
        if (!jointsToAdd.isEmpty()) {
            for (Joining joining : jointsToAdd) {
                if (joining.id == id)
                    return joining;
            }
        }
        return null;
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
            if (joining.anyDied()) {
                deregisterJoining(joining);
                continue;
            }
            JointDef jointDef = makeJointDef(joining);
            if (jointDefIsStale(jointDef)) {
                deregisterJoining(joining);
            } else {
                environment.getWorld().createJoint(jointDef);
                joinings.put(joining.id, joining);
            }
        }
        jointsToAdd.clear();
    }

    public void handleStaleJoints() {
        for (Joining joining : joinings.values()) {

            if (joining.anyDied()) {
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

            if (joining.notAnchored()
                    && bodyA.getUserData() instanceof Particle
                    && bodyB.getUserData() instanceof Particle) {
                handleGrowingParticle(joining);
            }
        }

        for (long joiningID : jointRemovalRequests) {
            if (!joinings.containsKey(joiningID))
                continue;

            Joining joining = joinings.get(joiningID);
            deregisterJoining(joining);
            joinings.remove(joining.id);
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
        Particle particleA = joining.getParticleA();
        Particle particleB = joining.getParticleB();
        Vector2 anchorA = joining.getAnchorA();
        Vector2 anchorB = joining.getAnchorB();

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
        if (joining == null)
            return false;

        return jointsToAdd.contains(joining)
                || joinings.containsKey(joining.id)
                || jointRemovalRequests.contains(joining.id);
    }

    public void createJoint(Joining joining) {
        if (!joiningExists(joining)) {
            jointsToAdd.add(joining);
        }
    }

    private void deregisterJoining(Joining joining) {
        if (joining == null)
            return;
        Particle particleA = joining.getParticleA();
        Particle particleB = joining.getParticleB();

        if (particleA instanceof Cell)
            ((Cell) particleA).deregisterJoining(joining);
        if (particleB instanceof Cell)
            ((Cell) particleB).deregisterJoining(joining);
    }

    private void deregisterJoining(long id) {
        deregisterJoining(joinings.get(id));
    }

    public void requestJointRemoval(Joining joining) {
        if (joining == null)
            return;
        requestJointRemoval(joining.id);
    }

    public void requestJointRemoval(long id) {
        if (!jointRemovalRequests.contains(id)) {
            deregisterJoining(id);
            jointRemovalRequests.add(id);
        }
    }
}
