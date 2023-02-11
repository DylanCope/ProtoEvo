package com.protoevo.biology.nodes;

import com.protoevo.biology.ComplexMolecule;
import com.protoevo.biology.Constructable;
import com.protoevo.biology.ConstructionProject;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

public abstract class NodeAttachment implements Serializable, Constructable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static Class<NodeAttachment>[] possibleAttachments = new Class[]{
            Flagellum.class,
            Spike.class,
            PhagocyticReceptor.class,
            Photoreceptor.class,
            AdhesionReceptor.class,
    };

    protected SurfaceNode node;
    private final ConstructionProject constructionProject;

    public float getRequiredMass() {
        return 0;
    }

    public float getRequiredEnergy() {
        return 0;
    }

    public float getTimeToComplete() {
        return 1f;
    }

    public Map<ComplexMolecule, Float> getRequiredComplexMolecules() {
        return Map.of();
    }

    public float getConstructionProgress() {
        return constructionProject.getProgress();
    }

    public ConstructionProject getConstructionProject() {
        return constructionProject;
    }

    public NodeAttachment(SurfaceNode node) {
        this.node = node;
        constructionProject = new ConstructionProject(this);
    }

    public abstract void update(float delta, float[] input, float[] output);

    public float getInteractionRange() {
        return 0;
    }

    public SurfaceNode getNode() {
        return node;
    }

    public abstract String getName();

    public abstract String getInputMeaning(int index);
    public abstract String getOutputMeaning(int index);
}
