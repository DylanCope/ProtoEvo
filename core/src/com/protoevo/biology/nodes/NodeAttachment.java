package com.protoevo.biology.nodes;

import com.protoevo.biology.ComplexMolecule;
import com.protoevo.biology.Constructable;
import com.protoevo.biology.ConstructionProject;
import com.protoevo.settings.SimulationSettings;
import com.protoevo.utils.Geometry;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
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
    private final Map<ComplexMolecule, Float> requiredComplexMolecules = new HashMap<>();

    public float getRequiredMass() {
        float density = SimulationSettings.basicParticleMassDensity;
        float area = Geometry.getCircleArea(SimulationSettings.maxParticleRadius);
        return density * area / 50f;
    }

    public float getRequiredEnergy() {
        return SimulationSettings.startingAvailableCellEnergy / 200f;
    }

    public float getTimeToComplete() {
        return .5f;
    }

    public Map<ComplexMolecule, Float> getRequiredComplexMolecules() {
        return requiredComplexMolecules;
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
