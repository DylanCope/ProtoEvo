package com.protoevo.biology.evolution;

import com.badlogic.gdx.math.MathUtils;
import com.protoevo.core.Simulation;

import java.io.Serializable;
import java.util.Map;


public class FloatTrait implements Trait<Float>, Serializable {


    public static final long serialVersionUID = 1L;

    private boolean regulated;
    private float value, minValue, maxValue;
    private String traitName;
    private float mutationRate;
    private int mutationCount = 0;

    public FloatTrait() {}

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

    /**
     * Override the default {@link Trait#cloneWithMutation()} (which fully
     * re-rolls every value to a uniform random) with NEAT-style perturbation:
     * 90% of mutations are small Gaussian nudges from the existing value,
     * 10% are full re-rolls. Same rationale as in SynapseGene — the previous
     * "always re-roll" made it impossible to fine-tune signature-driven traits
     * (e.g. SurfaceNode.constructionSignature, which decides whether a node
     * becomes an eye, mouth, flagellum, etc.). Every mutation was a coin flip
     * across the whole trait range, so populations couldn't converge on
     * "this signature builds a photoreceptor" — eyes evolve reliably with
     * perturbation.
     */
    @Override
    public Trait<Float> cloneWithMutation() {
        if (Math.random() > getMutationRate())
            return copy();

        if (Simulation.RANDOM.nextBoolean())
            mutateMutationRate();

        float current = value;
        float mutated;
        if (Math.random() < 0.9) {
            float range = maxValue - minValue;
            // sigma = 10% of the trait range; small enough to fine-tune,
            // large enough to escape local optima over generations.
            float sigma = 0.1f * (range > 0f ? range : 1f);
            mutated = current + (float) (Simulation.RANDOM.nextGaussian() * sigma);
        } else {
            mutated = newRandomValue();
        }
        if (mutated < minValue) mutated = minValue;
        else if (mutated > maxValue) mutated = maxValue;

        Trait<Float> newTrait = createNew(mutated);
        newTrait.incrementMutationCount();
        return newTrait;
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
