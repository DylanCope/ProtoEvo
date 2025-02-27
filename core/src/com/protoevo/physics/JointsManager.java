package com.protoevo.physics;

import com.protoevo.biology.cells.Cell;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public abstract class JointsManager implements Serializable {
    public static long serialVersionUID = 1L;

    protected Physics physics;
    protected Collection<Joining> jointsToAdd = new ConcurrentLinkedQueue<>();
    protected Collection<Long> jointRemovalRequests = new ConcurrentLinkedQueue<>();
    protected Map<Long, Joining> joinings = new ConcurrentHashMap<>();

    public JointsManager() {}

    public JointsManager(Physics physics) {
        this.physics = physics;
    }

    public Collection<Joining> getJoinings() {
        return joinings.values();
    }

    public abstract void rebuild(Physics physics);

    public Optional<Joining> getJoining(long id) {
        if (joinings.containsKey(id))
            return Optional.of(joinings.get(id));
        if (!jointsToAdd.isEmpty()) {
            for (Joining joining : jointsToAdd) {
                if (joining.id == id)
                    return Optional.of(joining);
            }
        }
        return Optional.empty();
    }

    public abstract void flushJoints();

    public static float idealJoinedParticleDistance(Particle p1, Particle p2) {
        // Ideal distance between the surfaces two joined particles
        return (p1.getRadius() + p2.getRadius()) * .2f;
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
            registerJoining(joining);
            jointsToAdd.add(joining);
        }
    }

    public void requestJointRemoval(Joining joining) {
        if (joining == null)
            return;
        requestJointRemoval(joining.id);
    }

    public void deregisterJoining(Particle particle, Joining joining) {
        Optional<Particle> other = joining.getOther(particle);
        other.ifPresent(otherParticle -> particle.getJoiningIds().remove(otherParticle.getId()));
    }

    protected void deregisterJoining(Joining joining) {
        if (joining == null)
            return;
        Optional<Particle> particleA = joining.getParticleA();
        Optional<Particle> particleB = joining.getParticleB();

        particleA.ifPresent(p -> deregisterJoining(p, joining));
        particleB.ifPresent(p -> deregisterJoining(p, joining));

        if (particleA.map(p -> p.getUserData() instanceof Cell).orElse(false))
            ((Cell) particleA.get().getUserData()).deregisterJoining(joining);
        if (particleB.map(p -> p.getUserData() instanceof Cell).orElse(false))
            ((Cell) particleB.get().getUserData()).deregisterJoining(joining);
    }

    public void registerJoining(Particle particle, Joining joining) {
        Optional<Particle> other = joining.getOther(particle);
        other.ifPresent(otherParticle -> particle.getJoiningIds().put(otherParticle.getId(), joining.id));
    }

    protected void registerJoining(Joining joining) {
        if (joining == null)
            return;
        Optional<Particle> particleA = joining.getParticleA();
        Optional<Particle> particleB = joining.getParticleB();

        particleA.ifPresent(p -> registerJoining(p, joining));
        particleB.ifPresent(p -> registerJoining(p, joining));

        if (particleA.map(p -> p.getUserData() instanceof Cell).orElse(false))
            ((Cell) particleA.get().getUserData()).registerJoining(joining);
        if (particleB.map(p -> p.getUserData() instanceof Cell).orElse(false))
            ((Cell) particleB.get().getUserData()).registerJoining(joining);
    }

    public void requestJointRemoval(long id) {
        if (!jointRemovalRequests.contains(id)) {
            jointRemovalRequests.add(id);
        }
        if (joinings.containsKey(id)) {
            deregisterJoining(joinings.get(id));
        }
    }

    public boolean areJoined(Particle p1, Particle p2) {
        return getJoining(Joining.getId(p1.getId(), p2.getId())).isPresent();
    }
}
