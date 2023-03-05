package com.protoevo.biology.nodes;

import com.protoevo.biology.ComplexMolecule;
import com.protoevo.biology.cells.Cell;
import com.protoevo.biology.cells.Protozoan;
import com.protoevo.core.Statistics;
import com.protoevo.env.Environment;
import com.protoevo.env.JointsManager;
import com.protoevo.physics.CollisionHandler;

public class AdhesionReceptor extends NodeAttachment {

    private volatile boolean isBound = false;
    private volatile int otherNodeIdx;
    private volatile long joiningID;
    private volatile float[] outgoing = new float[SurfaceNode.ioDim];
    private float constructionMassTransfer, molecularMassTransfer, energyTransfer;

    public AdhesionReceptor(SurfaceNode node) {
        super(node);
    }

    @Override
    public void update(float delta, float[] input, float[] output) {
        if (isBound && !isBindingStillValid())
            unbind();

        if (isBound) {
            handleResourceExchange(delta);
            AdhesionReceptor other = getOtherAdhesionReceptor();
            if (other == null) {
                unbind();
                return;
            }

            for (int i = 0; i < SurfaceNode.ioDim; i++) {
                outgoing[i] = input[i];
                output[i] = other.outgoing[i];
            }
            return;
        }

        Cell cell = node.getCell();
        for (CollisionHandler.Collision contact : cell.getContacts()) {
            Object other = cell.getOther(contact);
            if (other instanceof Protozoan && !isBound) {
                tryBindTo((Protozoan) other);
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

    public Cell getOtherCell() {
        Cell cell = node.getCell();
        JointsManager.Joining joining = cell.getJoining(joiningID);
        if (joining == null)
            return null;
        Object other = joining.getOther(cell);
        if (!(other instanceof Cell))
            return null;
        return (Cell) other;
    }

    public SurfaceNode getOtherNode() {
        Cell other = getOtherCell();
        if (other == null || otherNodeIdx < 0 || otherNodeIdx >= other.getSurfaceNodes().size())
            return null;
        return other.getSurfaceNodes().get(otherNodeIdx);
    }

    public void handleResourceExchange(float delta) {
        Cell cell = node.getCell();
        JointsManager.Joining joining = cell.getJoining(joiningID);
        if (joining == null)
            return;

        Cell other = (Cell) joining.getOther(cell);
        float transferRate = Environment.settings.cellBindingResourceTransport.get();

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
        float transferRate = Environment.settings.cellBindingResourceTransport.get();
        if (massDelta > 0) {
            float massTransfer = transferRate * massDelta * delta;
            molecularMassTransfer += massTransfer;
            dst.addAvailableComplexMolecule(molecule, massTransfer);
            src.depleteComplexMolecule(molecule, massTransfer);
        }
    }

    private boolean isBindingStillValid() {
        SurfaceNode otherNode = getOtherNode();
        if (otherNode == null)
            return false;
        return !otherNode.getCell().isDead()
                && otherNode.exists()
                && otherNode.getAttachment() instanceof AdhesionReceptor;
    }

    public AdhesionReceptor getOtherAdhesionReceptor() {
        SurfaceNode otherNode = getOtherNode();
        if (otherNode == null)
            return null;
        NodeAttachment otherAttachment = otherNode.getAttachment();
        if (!(otherAttachment instanceof AdhesionReceptor))
            return null;
        return (AdhesionReceptor) otherNode.getAttachment();
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
        JointsManager.Joining joining = new JointsManager.Joining(
                cell, otherCell, node.getAngle(), otherNode.getAngle());

        JointsManager jointsManager = cell.getEnv().getJointsManager();
        jointsManager.createJoint(joining);

        cell.registerJoining(joining);
        otherCell.registerJoining(joining);

        setOtherNode(otherNode, joining);
        otherReceptor.setOtherNode(node, joining);
    }

    public void setOtherNode(SurfaceNode otherNode, JointsManager.Joining joining) {
        this.otherNodeIdx = otherNode.getIndex();
        this.joiningID = joining.id;
        isBound = true;
    }

    private boolean createBindingCondition(SurfaceNode otherNode) {
        return otherNode.exists() && notAlreadyBound(otherNode)
                && isCloseEnough(otherNode);
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
        float d = 1.25f * JointsManager.idealJointLength(otherNode.getCell(), node.getCell());
        return node.getWorldPosition().dst2(otherNode.getWorldPosition()) <= d*d;
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
