package com.protoevo.biology.evolution;

import com.protoevo.core.Simulation;

import java.io.Serializable;
import java.util.Map;

public class FloatGene implements Gene<Float>, Serializable {

    public static final long serialVersionUID = 1L;

    private final float value, minValue, maxValue;
    private final String geneName;

    public FloatGene(String geneName, float minValue, float maxValue, float value) {
        this.geneName = geneName;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.value = value;
    }

    public FloatGene(String geneName, float minValue, float maxValue) {
        this.geneName = geneName;
        this.minValue = minValue;
        this.maxValue = maxValue;
        value = newRandomValue();
    }

    public float getMinValue() {
        return minValue;
    }

    public float getMaxValue() {
        return maxValue;
    }

    @Override
    public Float getValue(Map<String, Object> dependencies) {
        return value;
    }

    @Override
    public Float newRandomValue() {
        return Simulation.RANDOM.nextFloat() * (maxValue - minValue) + minValue;
    }

    @Override
    public Gene<Float> createNew(Float value) {
        return new FloatGene(geneName, minValue, maxValue, value);
    }

    @Override
    public String getTraitName() {
        return geneName;
    }
}
