package com.protoevo.biology.nodes;

import com.badlogic.gdx.math.Vector2;
import com.protoevo.biology.Cell;
import com.protoevo.core.settings.ProtozoaSettings;
import com.protoevo.utils.Geometry;

import java.util.Arrays;
import java.util.Optional;

public class SurfaceNode {

    private final Cell cell;
    private final float angle;
    private final Vector2 position = new Vector2();
    private Optional<NodeAttachment> attachment = Optional.empty();
    private final float[] inputActivation = new float[ProtozoaSettings.surfaceNodeActivationSize];
    private final float[] outputActivation = new float[ProtozoaSettings.surfaceNodeActivationSize];

    public SurfaceNode(Cell cell, float angle) {
        this.cell = cell;
        this.angle = angle;
    }

    public Vector2 getRelativePos() {
        float t = cell.getAngle() + angle;
        position.set((float) Math.cos(t), (float) Math.sin(t)).scl(cell.getRadius());
        return position;
    }

    public void update(float delta) {
        Arrays.fill(outputActivation, 0);
        attachment.ifPresent(a -> {
            a.update(delta, inputActivation, outputActivation);
        });
        Arrays.fill(inputActivation, 0);
    }

    public void setAttachment(NodeAttachment attachment) {
        this.attachment = Optional.of(attachment);
    }

    public Optional<NodeAttachment> getAttachment() {
        return attachment;
    }

    public float[] getInputActivation() {
        return inputActivation;
    }

    public float[] getOutputActivation() {
        return outputActivation;
    }

    public float getAngle() {
        return angle;
    }

    public Cell getCell() {
        return cell;
    }

    public float getInteractionRange() {
        return attachment.map(NodeAttachment::getInteractionRange).orElse(0f);
    }
}
