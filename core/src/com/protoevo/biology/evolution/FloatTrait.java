package com.protoevo.biology.evolution;

import com.protoevo.core.Simulation;

import java.io.Serializable;
import java.util.Map;

public class FloatTrait implements Trait<Float>, Serializable {

    public static final long serialVersionUID = 1L;

    private final float value, minValue, maxValue;
    private final String traitName;

    public FloatTrait(String traitName, float minValue, float maxValue, float value) {
        this.traitName = traitName;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.value = value;
    }

    public FloatTrait(String traitName, float minValue, float maxValue) {
        this.traitName = traitName;
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
        return Simulation.RANDOM.nextFloat(minValue, maxValue);
    }

    @Override
    public Trait<Float> createNew(Float value) {
        return new FloatTrait(traitName, minValue, maxValue, value);
    }

    @Override
    public String getTraitName() {
        return traitName;
    }
}