package com.protoevo.biology.nodes;

import com.protoevo.biology.Cell;
import com.protoevo.biology.PlantCell;
import com.protoevo.biology.protozoa.Protozoan;
import com.protoevo.env.CollisionHandler;
import com.protoevo.env.JointsManager;

import java.util.Optional;

public class AdhesionReceptor extends NodeAttachment {

    private SurfaceNode otherNode;

    public AdhesionReceptor(SurfaceNode node) {
        super(node);
    }

    @Override
    public void update(float delta, float[] input, float[] output) {
        if (otherNode != null)
            ensureBindingStillValid();

        Cell cell = node.getCell();
        for (CollisionHandler.FixtureCollision contact : cell.getContacts()) {
            Object other = cell.getOther(contact);
            if (other instanceof Protozoan) {
                tryBindTo((Protozoan) other);
            }
        }
    }

    private void ensureBindingStillValid() {
        if (otherNode.getCell().isDead()
                || !otherNode.exists()
                || !(otherNode.getAttachment() instanceof AdhesionReceptor)
                || otherNode.getCell().notBoundTo(node.getCell())
                || node.getCell().notBoundTo(otherNode.getCell())){
            node.getCell().requestJointRemoval(otherNode.getCell());
            otherNode = null;
        }
    }

    private void tryBindTo(Protozoan otherCell) {
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
