package com.protoevo.biology.evolution;

import com.badlogic.gdx.math.MathUtils;

import java.io.Serializable;
import java.util.Map;


public class FloatTrait implements Trait<Float>, Serializable {


    public static final long serialVersionUID = 1L;

    private boolean regulated;
    private final float value, minValue, maxValue;
    private final String traitName;
    private float mutationRate;
    private int mutationCount = 0;

    public FloatTrait(FloatTrait other, float value) {
        this.traitName = other.traitName;
        this.minValue = other.minValue;
        this.maxValue = other.maxValue;
        this.mutationRate = other.mutationRate;
        this.mutationCount = other.mutationCount;
        this.value = value;
    }

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
    public void setMutationRate(float rate) {
        this.mutationRate = rate;
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
        mutationCount++;
    }

    @Override
    public Float newRandomValue() {
        return MathUtils.random(minValue, maxValue);
    }

    @Override
    public Trait<Float> createNew(Float value) {
        return new FloatTrait(this, value);
    }

    @Override
    public String getTraitName() {
        return traitName;
    }

    public void setRegulated(boolean regulated) {
        this.regulated = regulated;
    }

    public boolean isRegulated() {
        return regulated;
    }
}
