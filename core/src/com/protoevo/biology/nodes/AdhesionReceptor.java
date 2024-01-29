package com.protoevo.biology.nodes;

import com.badlogic.gdx.math.Vector2;
import com.protoevo.biology.ComplexMolecule;
import com.protoevo.biology.cells.Cell;
import com.protoevo.biology.cells.Protozoan;
import com.protoevo.core.Statistics;
import com.protoevo.env.Environment;
import com.protoevo.physics.Joining;
import com.protoevo.physics.JointsManager;
import com.protoevo.physics.Particle;
import com.protoevo.physics.Collision;
import com.protoevo.maths.Geometry;

import java.util.Optional;

public class AdhesionReceptor extends NodeAttachment {

    private volatile boolean isBound = false;
    private volatile int otherNodeIdx;
    private volatile long joiningID;
    private float[] outgoing;
    private float constructionMassTransfer, molecularMassTransfer, energyTransfer;
    private Vector2 bindingAnchor = null;

    public AdhesionReceptor(SurfaceNode node) {
        super(node);
    }

    @Override
    public void update(float delta, float[] input, float[] output) {
        if (isBound)
            updateExistingBinding(delta, input, output);
        else
            checkContactsForNewBindings();
    }

    private void updateExistingBinding(float delta, float[] input, float[] output) {

        Optional<AdhesionReceptor> maybeOtherReceptor = getOtherAdhesionReceptor();

        if (!maybeOtherReceptor.isPresent() || !isBindingStillValid()) {
            unbind();
            return;
        }

        handleResourceExchange(delta);

        AdhesionReceptor otherReceptor = maybeOtherReceptor.get();

        ensureOutgoingCorrect(input);
        otherReceptor.ensureOutgoingCorrect(output);

        for (int i = 0; i < output.length; i++) {
            outgoing[i] = input[i];
            output[i] = otherReceptor.outgoing[i];
        }
    }

    private void ensureOutgoingCorrect(float[] values) {
        if (outgoing == null || outgoing.length != values.length)
            outgoing = new float[values.length];
    }

    private void checkContactsForNewBindings() {
        Cell cell = node.getCell();
        for (Collision contact : cell.getParticle().getContacts()) {
            Object other = contact.getOther(cell.getParticle());
            if (other instanceof Particle && ((Particle) other).getUserData() instanceof Protozoan) {
                tryBindTo(((Particle) other).getUserData(Protozoan.class));
                if (isBound)
                    break;
            }
        }
    }

    public boolean isBound() {
        return isBound;
    }

    public void unbind() {
        isBound = false;
        node.getCell().requestJointRemoval(joiningID);
        joiningID = -1;
        otherNodeIdx = -1;
        energyTransfer = 0;
        molecularMassTransfer = 0;
        constructionMassTransfer = 0;
    }

    public Optional<Cell> getOtherCell() {
        Cell cell = node.getCell();
        return cell.getParticle().getJoining(joiningID)
                .flatMap(j -> j.getOther(cell.getParticle()))
                .map(p -> p.getUserData(Cell.class));
    }

    public Optional<SurfaceNode> getOtherNode() {
        Optional<Cell> other = getOtherCell();
        if (!other.isPresent() || otherNodeIdx < 0 || otherNodeIdx >= other.get().getSurfaceNodes().size())
            return Optional.empty();
        return other.map(cell -> cell.getSurfaceNodes().get(otherNodeIdx));
    }

    public void handleResourceExchange(float delta) {
        Cell cell = node.getCell();

        Optional<Cell> maybeOther = getOtherCell();
        if (!maybeOther.isPresent())
            return;
        Cell other = maybeOther.get();

        float transferRate = Environment.settings.cell.bindingResourceTransport.get();

        float massDelta = cell.getConstructionMassAvailable() - other.getConstructionMassAvailable();
        constructionMassTransfer = Math.abs(transferRate * massDelta * delta);
        if (massDelta > 0) {
            other.addConstructionMass(constructionMassTransfer);
            cell.depleteConstructionMass(constructionMassTransfer);
        } else {
            cell.addConstructionMass(constructionMassTransfer);
            other.depleteConstructionMass(constructionMassTransfer);
        }

        float energyDelta = cell.getEnergyAvailable() - other.getEnergyAvailable();
        energyTransfer = Math.abs(transferRate * energyDelta * delta);
        if (energyDelta > 0) {
            other.addAvailableEnergy(energyTransfer);
            cell.depleteEnergy(energyTransfer);
        } else {
            cell.addAvailableEnergy(energyTransfer);
            other.depleteEnergy(energyTransfer);
        }

        molecularMassTransfer = 0;
        for (ComplexMolecule molecule : cell.getComplexMolecules())
            handleComplexMoleculeTransport(other, cell, molecule, delta);
        for (ComplexMolecule molecule : other.getComplexMolecules())
            handleComplexMoleculeTransport(cell, other, molecule, delta);
    }

    private void handleComplexMoleculeTransport(Cell src, Cell dst, ComplexMolecule molecule, float delta) {
        float massDelta = dst.getComplexMoleculeAvailable(molecule) - src.getComplexMoleculeAvailable(molecule);
        float transferRate = Environment.settings.cell.bindingResourceTransport.get();
        if (massDelta > 0) {
            float massTransfer = transferRate * massDelta * delta;
            molecularMassTransfer += massTransfer;
            dst.addAvailableComplexMolecule(molecule, massTransfer);
            src.depleteComplexMolecule(molecule, massTransfer);
        }
    }

    private boolean isBindingStillValid() {
        Optional<SurfaceNode> maybeOtherNode = getOtherNode();
        if (!maybeOtherNode.isPresent())
            return false;
        SurfaceNode otherNode = maybeOtherNode.get();
        return !otherNode.getCell().isDead()
                && otherNode.exists()
                && otherNode.getAttachment() instanceof AdhesionReceptor;
    }

    public Optional<AdhesionReceptor> getOtherAdhesionReceptor() {
        return getOtherNode().map(SurfaceNode::getAttachment)
                .filter(a -> a instanceof AdhesionReceptor)
                .map(a -> (AdhesionReceptor) a);
    }

    private void tryBindTo(Protozoan otherCell) {
        for (SurfaceNode otherNode : otherCell.getSurfaceNodes()) {
            NodeAttachment otherAttachment = otherNode.getAttachment();
            if (otherAttachment instanceof AdhesionReceptor
                    && createBindingCondition(otherNode)) {
                bindTo(otherCell, (AdhesionReceptor) otherAttachment);
            }
        }
    }

    private void bindTo(Protozoan otherCell, AdhesionReceptor otherReceptor) {
        if (otherReceptor.isBound || otherReceptor.node.getCell() == null)
            return;

        Cell cell = node.getCell();
        SurfaceNode otherNode = otherReceptor.getNode();

//        float myAngle = node.getAngle();
//        float otherAngle = otherNode.getAngle();

        float myAngle = Geometry.angle(otherCell.getPos().cpy().sub(cell.getPos())) - cell.getParticle().getAngle();
        float otherAngle = Geometry.angle(cell.getPos().cpy().sub(otherCell.getPos())) - otherCell.getParticle().getAngle();

        Joining joining = new Joining(cell.getParticle(), otherCell.getParticle(), myAngle, otherAngle);
        bindingAnchor = joining.getParticleAnchor(cell.getParticle()).orElse(null);

        JointsManager jointsManager = cell.getEnv()
                .orElseThrow(() -> new RuntimeException("Cell has no environment"))
                .getJointsManager();
        jointsManager.createJoint(joining);

        cell.registerJoining(joining);
        otherCell.registerJoining(joining);

        setOtherNode(otherNode, joining);
        otherReceptor.setOtherNode(node, joining);
    }

    public Optional<Vector2> getBindingAnchor() {
        if (bindingAnchor == null)
            return Optional.empty();
        return Optional.of(bindingAnchor);
    }

    public void setOtherNode(SurfaceNode otherNode, Joining joining) {
        this.otherNodeIdx = otherNode.getIndex();
        this.joiningID = joining.id;
        isBound = true;
    }

    private boolean createBindingCondition(SurfaceNode otherNode) {
        return otherNode.exists() && notAlreadyBound(otherNode);
//                && isCloseEnough(otherNode);
    }

    private boolean notAlreadyBound(SurfaceNode otherNode) {
        Cell otherCell = otherNode.getCell();
        Cell cell = node.getCell();
        return otherCell.notBoundTo(cell);
    }

    private boolean otherIsBinding(SurfaceNode otherNode) {
        return otherNode.getAttachment() instanceof AdhesionReceptor;
    }

    private boolean isCloseEnough(SurfaceNode otherNode) {
        float d = 1.25f * JointsManager.idealJointLength(otherNode.getCell().getParticle(), node.getCell().getParticle());
        return node.getWorldPosition().dst2(otherNode.getWorldPosition()) <= d*d;
    }

    public long getJoiningID() {
        return joiningID;
    }

    @Override
    public String getName() {
        return "Binding";
    }

    @Override
    public String getInputMeaning(int index) {
        if (!isBound)
            return null;
        return "Outgoing Signal " + index;
    }

    @Override
    public String getOutputMeaning(int index) {
        if (!isBound)
            return null;
        return "Incoming Signal " + index;
    }

    @Override
    public void addStats(Statistics stats) {
        stats.putBoolean("Is Bound", isBound);
        stats.putMass("Construction Mass Transfer", constructionMassTransfer);
        stats.putEnergy("Energy Transfer", energyTransfer);
        stats.putMass("Molecular Mass Transfer", molecularMassTransfer);
    }
}
