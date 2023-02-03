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
        Cell cell = node.getCell();
        for (CollisionHandler.FixtureCollision contact : cell.getContacts()) {
            Object other = cell.getOther(contact);
            if (other instanceof Protozoan) {
                tryBindTo(contact, (Protozoan) other);
            }
        }
    }

    private void tryBindTo(CollisionHandler.FixtureCollision contact, Protozoan otherCell) {
        for (SurfaceNode otherNode : otherCell.getSurfaceNodes()) {
            if (createBindingCondition(otherNode)) {
                Cell cell = node.getCell();
                cell.createCellBinding(contact, otherCell, PlantCell.plantCAM);
                otherCell.createCellBinding(contact, cell, PlantCell.plantCAM);
                this.otherNode = otherNode;
                if (otherNode.getAttachment() != null && otherNode.getAttachment() instanceof AdhesionReceptor)
                    ((AdhesionReceptor) otherNode.getAttachment()).setOtherNode(node);
            }
        }
    }

    public void setOtherNode(SurfaceNode otherNode) {
        this.otherNode = otherNode;
    }

    private boolean createBindingCondition(SurfaceNode otherNode) {
        return otherIsBinding(otherNode) && isCloseEnough(otherNode);
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
