package com.protoevo.biology.nodes;

import com.badlogic.gdx.math.Vector2;
import com.protoevo.biology.Cell;
import com.protoevo.core.settings.ProtozoaSettings;
import com.protoevo.utils.Geometry;

import java.util.Arrays;

public class SurfaceNode {

    private final Cell cell;
    private final float angle;
    private final Vector2 position;
    private NodeAttachment attachment;
    private final float[] inputActivation = new float[ProtozoaSettings.surfaceNodeActivationSize];
    private final float[] outputActivation = new float[ProtozoaSettings.surfaceNodeActivationSize];

    public SurfaceNode(Cell cell, float angle) {
        this.cell = cell;
        this.angle = angle;
        position = Geometry.fromAngle(angle);
    }

    public Vector2 getRelativePos() {
        return position;
    }

    public void update(float delta) {
        Arrays.fill(outputActivation, 0);
        position.setLength(cell.getRadius());

        if (attachment != null) {
            attachment.update(delta);
            attachment.handleIO(inputActivation, outputActivation);
        }
        Arrays.fill(inputActivation, 0);
    }

    public void setAttachment(NodeAttachment attachment) {
        this.attachment = attachment;
    }

    public NodeAttachment getAttachment() {
        return attachment;
    }

    public float[] getInputActivation() {
        return inputActivation;
    }

    public float[] getOutputActivation() {
        return outputActivation;
    }
}
