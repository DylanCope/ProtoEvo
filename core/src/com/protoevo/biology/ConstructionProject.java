package com.protoevo.biology;

import com.badlogic.gdx.math.MathUtils;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;


public class ConstructionProject implements Serializable {

    private static final long serialVersionUID = 1L;

    private float timeSpent;
    private Constructable target;

    public ConstructionProject() {}

    public ConstructionProject(Constructable constructable) {
        this.target = constructable;
    }

    public float getRequiredMass() {
        return target.getRequiredMass();
    }

    public boolean requiresComplexMolecules() {
        Map<ComplexMolecule, Float> requiredComplexMolecules = target.getRequiredComplexMolecules();
        return requiredComplexMolecules != null && !requiredComplexMolecules.isEmpty();
    }

    public Collection<ComplexMolecule> getRequiredMolecules() {
        Map<ComplexMolecule, Float> requiredComplexMolecules = target.getRequiredComplexMolecules();
        return requiredComplexMolecules.keySet();
    }

    public float getRequiredComplexMoleculeAmount(ComplexMolecule molecule) {
        Map<ComplexMolecule, Float> requiredComplexMolecules = target.getRequiredComplexMolecules();
        if (requiredComplexMolecules.containsKey(molecule))
            return requiredComplexMolecules.get(molecule);
        return 0f;
    }

    public boolean canMakeProgress(float availableEnergy,
                                   float availableMass,
                                   Map<ComplexMolecule, Float> availableComplexMolecules,
                                   float delta) {
        if (availableEnergy < energyToMakeProgress(delta) || availableMass < massToMakeProgress(delta))
            return false;
        if (requiresComplexMolecules() && availableComplexMolecules != null)
            for (ComplexMolecule molecule : getRequiredMolecules()) {
                float available = availableComplexMolecules.getOrDefault(molecule, 0f);
                if (available < complexMoleculesToMakeProgress(delta, molecule))
                    return false;
            }
        return true;
    }

    public float getTimeToComplete() {
        return target.getTimeToComplete();
    }

    public float getProgress() {
        return MathUtils.clamp(timeSpent / getTimeToComplete(), 0f, 1f);
    }

    public boolean notFinished() {
        return timeSpent < getTimeToComplete();
    }

    /**
     * Make progress on the project
     * @param delta Change in time
     */
    public void progress(float delta) {
        timeSpent = Math.min(timeSpent + delta, getTimeToComplete());
    }

    public void reset() {
        timeSpent = 0;
    }

    public void deconstruct(float delta) {
        timeSpent = Math.max(timeSpent - delta, 0);
    }

    public float massToMakeProgress(float delta) {
        return delta * getRequiredMass() / getTimeToComplete();
    }

    public float getRequiredEnergy() {
        return target.getRequiredEnergy();
    }

    public float energyToMakeProgress(float delta) {
        return delta * getRequiredEnergy() / getTimeToComplete();
    }

    public float complexMoleculesToMakeProgress(float delta, ComplexMolecule molecule) {
        float amount = getRequiredComplexMoleculeAmount(molecule);
        return delta * amount / getTimeToComplete();
    }
}