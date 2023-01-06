package com.protoevo.biology.evolution;

import com.protoevo.core.Simulation;

import java.util.Map;

public class BooleanGene implements Gene<Boolean> {

    private final boolean value;
    private final String geneName;

    public BooleanGene(String geneName) {
        this.geneName = geneName;
        this.value = false;
    }

    public BooleanGene(String geneName, boolean value) {
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
    public Gene<Boolean> createNew(Boolean value) {
        return new BooleanGene(geneName, value);
    }

    @Override
    public String getTraitName() {
        return geneName;
    }
}
