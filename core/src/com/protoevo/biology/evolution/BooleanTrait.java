package com.protoevo.biology.evolution;

import com.protoevo.core.Simulation;

import java.util.Map;

public class BooleanTrait implements Trait<Boolean> {

    private final boolean value;
    private final String geneName;

    public BooleanTrait(String geneName) {
        this.geneName = geneName;
        this.value = false;
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
        return new BooleanTrait(geneName, value);
    }

    @Override
    public String getTraitName() {
        return geneName;
    }
}
