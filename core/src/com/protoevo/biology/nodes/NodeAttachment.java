package com.protoevo.biology.nodes;

import com.protoevo.biology.ComplexMolecule;
import com.protoevo.biology.Constructable;
import com.protoevo.biology.ConstructionProject;
import com.protoevo.core.Statistics;
import com.protoevo.env.Environment;
import com.protoevo.maths.Geometry;


import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public abstract class NodeAttachment implements Serializable, Constructable {

    private static final long serialVersionUID = 1L;

    public static Class<? extends NodeAttachment>[] possibleAttachments = null;

    public static void setupPossibleAttachments() {
        if (Environment.settings.protozoa.separatePhagoNodes.get()) {
            possibleAttachments = new Class[]{
                    Flagellum.class,
                    Spike.class,
                    PlantOnlyPhagocyticReceptor.class,
                    MeatOnlyPhagocyticReceptor.class,
                    Photoreceptor.class,
                    AdhesionReceptor.class,
            };
        } else {
            possibleAttachments = new Class[]{
                    Flagellum.class,
                    Spike.class,
                    PhagocyticReceptor.class,
                    Photoreceptor.class,
                    AdhesionReceptor.class,
            };
        }
    }

    public static Map<String, Class<? extends NodeAttachment>> attachmentIncompatibilities = new HashMap<>();
    static {
        attachmentIncompatibilities.put(
                PlantOnlyPhagocyticReceptor.class.getSimpleName(),
                MeatOnlyPhagocyticReceptor.class);
        attachmentIncompatibilities.put(
                MeatOnlyPhagocyticReceptor.class.getSimpleName(),
                PlantOnlyPhagocyticReceptor.class);
    }

    protected SurfaceNode node;
    private final ConstructionProject constructionProject;
    private final Map<ComplexMolecule, Float> requiredComplexMolecules = new HashMap<>();

    public float getRequiredMass() {
        float density = Environment.settings.cell.basicParticleMassDensity.get();
        float area = Geometry.getCircleArea(Environment.settings.maxParticleRadius.get());
        return density * area / 40f;
    }

    public float getRequiredEnergy() {
        return 1 / 100f;
    }

    public float getTimeToComplete() {
        return 5f;
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

    public abstract void addStats(Statistics stats);

}
