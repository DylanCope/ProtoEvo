package com.protoevo.biology.evolution;

import com.protoevo.core.Simulation;

import java.util.Map;

public class BooleanTrait implements Trait<Boolean> {


    private final boolean value;
    private final String geneName;
    private float mutationRate;
    private int mutationCount = 0;

    public BooleanTrait(String geneName) {
        this.geneName = geneName;
        this.value = false;
    }

    public BooleanTrait(BooleanTrait trait, boolean value) {
        this.geneName = trait.geneName;
        this.mutationCount = trait.mutationCount;
        this.mutationRate = trait.mutationRate;
        this.value = value;
    }

    public BooleanTrait(String geneName, boolean value) {
        this.geneName = geneName;
        this.value = value;
    }

    public static boolean fromFloat(float value) {
        return value > 0;
    }

    @Override
    public Boolean getValue(Map<String, Object> dependencies) {
        return value;
    }

    @Override
    public Boolean newRandomValue() {
        return Simulation.RANDOM.nextBoolean();
    }

    @Override
    public Trait<Boolean> createNew(Boolean value) {
        return new BooleanTrait(this, value);
    }

    @Override
    public void setMutationRate(float rate) {
        mutationRate = rate;
    }

    @Override
    public float getMutationRate() {
        return mutationRate;
    }

    @Override
    public int getMutationCount() {
        return mutationCount;
    }

    @Override
    public void incrementMutationCount() {
        mutationCount += 1;
    }

    @Override
    public String getTraitName() {
        return geneName;
    }
}
