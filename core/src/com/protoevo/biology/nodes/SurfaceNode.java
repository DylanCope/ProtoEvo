package com.protoevo.biology.nodes;

import com.badlogic.gdx.math.Vector2;
import com.protoevo.biology.Cell;
import com.protoevo.biology.evolution.*;
import com.protoevo.settings.SimulationSettings;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class SurfaceNode implements Evolvable.Element, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final String activationPrefix = "Activation/";
    public static final String inputActivationPrefix = "Input" + activationPrefix;
    public static final String outputActivationPrefix = "Output" + activationPrefix;

    private Cell cell;
    private float angle;
    private final Vector2 relativePosition = new Vector2(), worldPosition = new Vector2();
    private NodeAttachment attachment = null;
    private final float[] inputActivation = new float[3];
    private final float[] outputActivation = new float[3];
    private int nodeIdx;
    private final Map<String, Float> stats = new HashMap<>();
    private GeneExpressionFunction geneExpressionFunction;
    private boolean nodeExists = false;

    public SurfaceNode() {
        float p = (float) Math.random();
        if (p < 0.1) {
            attachment = new Flagellum(this);
        } else if (p < 0.4) {
            attachment = new AdhesionReceptor(this);
        } else if (p < 0.6) {
            attachment = new PhagocyticReceptor(this);
        } else if (p < 0.9) {
            attachment = new Photoreceptor(this);
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
        if (!nodeExists)
            tryCreate();

        if (attachment != null && nodeExists) {
            attachment.update(delta, inputActivation, outputActivation);
        }
    }

    public float requiredArcLength() {
        return (float) (SimulationSettings.maxParticleRadius * 2 * Math.PI / 15f);
    }

    public void tryCreate() {
        if (cell.getSurfaceNodes() == null)
            return;

        for (SurfaceNode node : cell.getSurfaceNodes()) {
            if (node == this || !node.exists())
                continue;
            float dAngle = Math.abs(node.getAngle() - angle);
            float arcLen = dAngle * cell.getRadius();
            if (arcLen < this.requiredArcLength() / 2f + node.requiredArcLength() / 2f) {
                return;
            }
        }
        nodeExists = true;
    }

    public boolean exists() {
        return nodeExists;
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

    @RegulatedFloat(name=inputActivationPrefix + "0", min=-1, max=1)
    public void setActivation0(float value) {
        inputActivation[0] = value;
    }

    @GeneRegulator(name=outputActivationPrefix + "0")
    public float getActivation0() {
        return outputActivation[0];
    }

    @RegulatedFloat(name=inputActivationPrefix + "1", min=-1, max=1)
    public void setActivation1(float value) {
        inputActivation[1] = value;
    }

    @GeneRegulator(name=outputActivationPrefix + "1")
    public float getActivation1() {
        return outputActivation[1];
    }

    @RegulatedFloat(name=inputActivationPrefix + "2", min=-1, max=1)
    public void setActivation2(float value) {
        inputActivation[2] = value;
    }

    @GeneRegulator(name=outputActivationPrefix + "2")
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
        if (attachment == null)
            return 0f;
        else
            return attachment.getInteractionRange();
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
