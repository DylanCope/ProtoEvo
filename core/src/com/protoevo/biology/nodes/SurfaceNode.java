package com.protoevo.biology.nodes;

import com.badlogic.gdx.math.Vector2;
import com.protoevo.biology.Cell;
import com.protoevo.biology.evolution.*;
import com.protoevo.core.settings.ProtozoaSettings;
import com.protoevo.utils.Geometry;

import java.util.Arrays;
import java.util.Optional;

public class SurfaceNode implements Evolvable.Component {

    private Cell cell;
    private float angle;
    private final Vector2 position = new Vector2();
    private Optional<NodeAttachment> attachment = Optional.empty();
    private final float[] inputActivation = new float[ProtozoaSettings.surfaceNodeActivationSize];
    private final float[] outputActivation = new float[ProtozoaSettings.surfaceNodeActivationSize];

    public SurfaceNode() {

        float p = (float) Math.random();
        if (p < 0.1) {
            attachment = Optional.of(new FlagellumAttachment(this));
        } else if (p < 0.3) {
            attachment = Optional.of(new SpikeAttachment(this));
        } else if (p < 0.7) {
            attachment = Optional.of(new LightSensitiveAttachment(this));
        }

    }

    public void setCell(Cell cell) {
        this.cell = cell;
    }

    @EvolvableFloat(name = "Node Angle", max = 2 * (float) Math.PI)
    public void setAngle(float angle) {
        this.angle = angle;
    }

    public Vector2 getRelativePos() {
        float t = cell.getAngle() + angle;
        position.set((float) Math.cos(t), (float) Math.sin(t)).scl(cell.getRadius());
        return position;
    }

    public void update(float delta) {
        attachment.ifPresent(a -> {
            a.update(delta, inputActivation, outputActivation);
        });
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

    @RegulatedFloat(name="Activation/0", min=-1, max=1)
    public void setActivation0(float value) {
        inputActivation[0] = value;
    }

    @GeneRegulator(name="Activation/0")
    public float getActivation0() {
        return outputActivation[0];
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
