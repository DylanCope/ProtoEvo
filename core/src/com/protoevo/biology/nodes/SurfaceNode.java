package com.protoevo.biology.nodes;

import com.badlogic.gdx.math.Vector2;
import com.protoevo.biology.Cell;
import com.protoevo.biology.evolution.*;
import com.protoevo.core.settings.ProtozoaSettings;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class SurfaceNode implements Evolvable.Element {

    private Cell cell;
    private float angle;
    private final Vector2 relativePosition = new Vector2(), worldPosition = new Vector2();
    private Optional<NodeAttachment> attachment = Optional.empty();
    private final float[] inputActivation = new float[ProtozoaSettings.surfaceNodeActivationSize];
    private final float[] outputActivation = new float[ProtozoaSettings.surfaceNodeActivationSize];
    private int nodeIdx;
    private final Map<String, Float> stats = new HashMap<>();
    private GeneExpressionFunction geneExpressionFunction;

    public SurfaceNode() {
        float p = (float) Math.random();
        if (p < 0.1) {
            attachment = Optional.of(new Flagellum(this));
        } else if (p < 0.4) {
            attachment = Optional.of(new BindingNode(this));
//        } else if (p < 0.6) {
//            attachment = Optional.of(new PhagocyticReceptor(this));
        } else if (p < 0.9) {
            attachment = Optional.of(new Photoreceptor(this));
        }
    }

    public void setCell(Cell cell) {
        this.cell = cell;
    }

    @EvolvableFloat(name = "Angle", max = 2 * (float) Math.PI, regulated = false)
    public void setAngle(float angle) {
        this.angle = angle;
    }

    public Vector2 getRelativePos() {
        float t = cell.getAngle() + angle;
        relativePosition.set((float) Math.cos(t), (float) Math.sin(t)).scl(cell.getRadius());
        return relativePosition;
    }

    public Vector2 getWorldPosition() {
        worldPosition.set(getRelativePos()).add(cell.getPos());
        return worldPosition;
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

    @RegulatedFloat(name="InputActivation/0", min=-1, max=1)
    public void setActivation0(float value) {
        inputActivation[0] = value;
    }

    @GeneRegulator(name="OutputActivation/0")
    public float getActivation0() {
        return outputActivation[0];
    }

    @RegulatedFloat(name="InputActivation/1", min=-1, max=1)
    public void setActivation1(float value) {
        inputActivation[1] = value;
    }

    @GeneRegulator(name="OutputActivation/1")
    public float getActivation1() {
        return outputActivation[1];
    }

    @RegulatedFloat(name="InputActivation/2", min=-1, max=1)
    public void setActivation2(float value) {
        inputActivation[2] = value;
    }

    @GeneRegulator(name="OutputActivation/2")
    public float getActivation2() {
        return outputActivation[2];
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

    @Override
    public void setIndex(int index) {
        this.nodeIdx = index;
    }

    @Override
    public int getIndex() {
        return nodeIdx;
    }

    @Override
    public void setGeneExpressionFunction(GeneExpressionFunction fn) {
        geneExpressionFunction = fn;
    }

    @Override
    public GeneExpressionFunction getGeneExpressionFunction() {
        return geneExpressionFunction;
    }
}
