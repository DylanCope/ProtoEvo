package com.protoevo.biology.nodes;

import com.badlogic.gdx.physics.box2d.Contact;
import com.protoevo.biology.Cell;
import com.protoevo.biology.PlantCell;
import com.protoevo.biology.protozoa.Protozoan;
import com.protoevo.env.CollisionHandler;
import com.protoevo.env.JointsManager;

import java.util.Optional;

public class BindingAttachment extends NodeAttachment {

    private SurfaceNode otherNode;

    public BindingAttachment(SurfaceNode node) {
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
                otherNode.getAttachment()
                        .ifPresent(a -> ((BindingAttachment) a).setOtherNode(node));
            }
        }
    }

    public void setOtherNode(SurfaceNode otherNode) {
        this.otherNode = otherNode;
    }

    private boolean createBindingCondition(SurfaceNode otherNode) {
        Optional<NodeAttachment> otherAttachment = otherNode.getAttachment();
        return otherAttachment.map(a -> a instanceof BindingAttachment).orElse(false)
                && node.getWorldPosition().dst2(otherNode.getWorldPosition())
                   < JointsManager.idealJointLength(otherNode.getCell(), node.getCell());
    }

    @Override
    public String getName() {
        return "Binding";
    }
}
