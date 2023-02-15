package com.protoevo.biology.nodes;

import com.protoevo.biology.*;
import com.protoevo.biology.cells.Cell;
import com.protoevo.biology.cells.Protozoan;
import com.protoevo.env.CollisionHandler;
import com.protoevo.env.JointsManager;
import com.protoevo.settings.Settings;

import java.util.Optional;

public class AdhesionReceptor extends NodeAttachment {

    private SurfaceNode otherNode;

    public AdhesionReceptor(SurfaceNode node) {
        super(node);
    }

    @Override
    public void update(float delta, float[] input, float[] output) {
        if (otherNode != null && ensureBindingStillValid()) {
            handleResourceExchange(delta);
        }
        Cell cell = node.getCell();
        for (CollisionHandler.FixtureCollision contact : cell.getContacts()) {
            Object other = cell.getOther(contact);
            if (other instanceof Protozoan)
                tryBindTo((Protozoan) other);
        }
    }

    public void handleResourceExchange(float delta) {
        Cell cell = node.getCell();
        Cell other = otherNode.getCell();
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

    private boolean ensureBindingStillValid() {
        if (otherNode.getCell().isDead()
                || !otherNode.exists()
                || !(otherNode.getAttachment() instanceof AdhesionReceptor)
                || otherNode.getCell().notBoundTo(node.getCell())
                || node.getCell().notBoundTo(otherNode.getCell())){
            node.getCell().requestJointRemoval(otherNode.getCell());
            otherNode = null;
            return false;
        }
        return true;
    }

    private void tryBindTo(Protozoan otherCell) {
        if (Math.random() < getConstructionProgress())
            return;

        for (SurfaceNode otherNode : otherCell.getSurfaceNodes()) {
            if (createBindingCondition(otherNode)) {
                Cell cell = node.getCell();
                JointsManager.JoinedParticles joining = new JointsManager.JoinedParticles(
                        cell, otherCell, node.getAngle(), otherNode.getAngle());

                JointsManager jointsManager = cell.getEnv().getJointsManager();
                jointsManager.createJoint(joining);

                cell.registerJoining(joining);
                otherCell.registerJoining(joining);

                this.otherNode = otherNode;
                if (otherNode.getAttachment() != null && otherNode.getAttachment() instanceof AdhesionReceptor)
                    ((AdhesionReceptor) otherNode.getAttachment()).setOtherNode(node);

                break;
            }
        }
    }

    public Optional<SurfaceNode> getOtherNode() {
        return Optional.ofNullable(otherNode);
    }

    public void setOtherNode(SurfaceNode otherNode) {
        this.otherNode = otherNode;
    }

    private boolean createBindingCondition(SurfaceNode otherNode) {
        return otherNode.exists() && notAlreadyBound(otherNode) && otherIsBinding(otherNode) && isCloseEnough(otherNode);
    }

    private boolean notAlreadyBound(SurfaceNode otherNode) {
        Cell otherCell = otherNode.getCell();
        Cell cell = node.getCell();
        return otherCell.notBoundTo(cell) && cell.notBoundTo(otherCell);
    }

    private boolean otherIsBinding(SurfaceNode otherNode) {
        return otherNode.getAttachment() instanceof AdhesionReceptor;
    }

    private boolean isCloseEnough(SurfaceNode otherNode) {
        float d = JointsManager.idealJointLength(otherNode.getCell(), node.getCell());
        return node.getWorldPosition().dst2(otherNode.getWorldPosition()) < d*d;
    }

    @Override
    public String getName() {
        return "Binding";
    }

    @Override
    public String getInputMeaning(int index) {
        return "Input: " + index;
    }

    @Override
    public String getOutputMeaning(int index) {
        return "Output: " + index;
    }
}
