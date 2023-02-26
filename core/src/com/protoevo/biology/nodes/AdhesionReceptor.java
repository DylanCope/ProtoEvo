package com.protoevo.biology.nodes;

import com.protoevo.biology.*;
import com.protoevo.biology.cells.Cell;
import com.protoevo.biology.cells.Protozoan;
import com.protoevo.physics.CollisionHandler;
import com.protoevo.env.JointsManager;
import com.protoevo.settings.Settings;

public class AdhesionReceptor extends NodeAttachment {

    private boolean isBound = false;
    private int otherNodeIdx;
    private long joiningID;
    private float[] outgoing = new float[SurfaceNode.ioDim];

    public AdhesionReceptor(SurfaceNode node) {
        super(node);
    }

    @Override
    public void update(float delta, float[] input, float[] output) {
        if (!isBindingStillValid())
            unbind();

        if (isBound) {
            handleResourceExchange(delta);
            AdhesionReceptor other = getOtherAdhesionReceptor();
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

    public void unbind() {
        isBound = false;
        joiningID = -1;
        otherNodeIdx = -1;
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
        if (other == null)
            return null;
        return other.getSurfaceNodes().get(otherNodeIdx);
    }

    public void handleResourceExchange(float delta) {
        Cell cell = node.getCell();
        JointsManager.Joining joining = cell.getJoining(joiningID);
        Cell other = (Cell) joining.getOther(cell);
        float transferRate = Settings.channelBindingEnergyTransport;

        float massDelta = cell.getConstructionMassAvailable() - other.getConstructionMassAvailable();
        float constructionMassTransfer = Math.abs(transferRate * massDelta * delta);
        if (massDelta > 0) {
            other.addConstructionMass(constructionMassTransfer);
            cell.depleteConstructionMass(constructionMassTransfer);
        } else {
            cell.addConstructionMass(constructionMassTransfer);
            other.depleteConstructionMass(constructionMassTransfer);
        }

        float energyDelta = cell.getEnergyAvailable() - other.getEnergyAvailable();
        float energyTransfer = Math.abs(transferRate * energyDelta * delta);
        if (energyDelta > 0) {
            other.addAvailableEnergy(energyTransfer);
            cell.depleteEnergy(energyTransfer);
        } else {
            cell.addAvailableEnergy(energyTransfer);
            other.depleteEnergy(energyTransfer);
        }

        for (ComplexMolecule molecule : cell.getComplexMolecules())
            handleComplexMoleculeTransport(other, cell, molecule, delta);
        for (ComplexMolecule molecule : other.getComplexMolecules())
            handleComplexMoleculeTransport(cell, other, molecule, delta);
    }

    private void handleComplexMoleculeTransport(Cell src, Cell dst, ComplexMolecule molecule, float delta) {
        float massDelta = dst.getComplexMoleculeAvailable(molecule) - src.getComplexMoleculeAvailable(molecule);
        float transferRate = Settings.occludingBindingEnergyTransport;
        if (massDelta > 0) {
            float massTransfer = transferRate * massDelta * delta;
            dst.addAvailableComplexMolecule(molecule, massTransfer);
            src.depleteComplexMolecule(molecule, massTransfer);
        }
    }

    private boolean isBindingStillValid() {
        SurfaceNode otherNode = getOtherNode();
        return otherNode != null && !(otherNode.getCell().isDead()
                || !otherNode.exists()
                || !(otherNode.getAttachment() instanceof AdhesionReceptor)
                || otherNode.getCell().notBoundTo(node.getCell())
                || node.getCell().notBoundTo(otherNode.getCell()));
    }

    public AdhesionReceptor getOtherAdhesionReceptor() {
        return (AdhesionReceptor) getOtherNode().getAttachment();
    }

    private void tryBindTo(Protozoan otherCell) {
        if (Math.random() > getConstructionProgress())
            return;

        for (SurfaceNode otherNode : otherCell.getSurfaceNodes()) {
            if (createBindingCondition(otherNode)) {
                Cell cell = node.getCell();
                JointsManager.Joining joining = new JointsManager.Joining(
                        cell, otherCell, node.getAngle(), otherNode.getAngle());

                JointsManager jointsManager = cell.getEnv().getJointsManager();
                jointsManager.createJoint(joining);

                cell.registerJoining(joining);
                otherCell.registerJoining(joining);

                this.otherNodeIdx = otherNode.getIndex();
                this.joiningID = joining.id;
                isBound = true;

                if (otherNode.getAttachment() instanceof AdhesionReceptor)
                    ((AdhesionReceptor) otherNode.getAttachment()).setOtherNode(node);
            }
        }
    }

    public void setOtherNode(SurfaceNode otherNode) {
        this.otherNodeIdx = otherNode.getIndex();
    }

    private boolean createBindingCondition(SurfaceNode otherNode) {
        return otherNode.exists() && notAlreadyBound(otherNode)
                && otherIsBinding(otherNode) && isCloseEnough(otherNode);
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
        float d = JointsManager.idealJointLength(otherNode.getCell(), node.getCell())
                + otherNode.getCell().getRadius() + node.getCell().getRadius();
        return node.getWorldPosition().dst2(otherNode.getWorldPosition()) < d*d;
    }

    @Override
    public String getName() {
        return "Binding";
    }

    @Override
    public String getInputMeaning(int index) {
        return "Outgoing Signal " + index;
    }

    @Override
    public String getOutputMeaning(int index) {
        if (!isBound)
            return null;
        return "Incoming Signal " + index;
    }
}
