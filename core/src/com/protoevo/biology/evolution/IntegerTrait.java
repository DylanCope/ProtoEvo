package com.protoevo.biology.evolution;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.protoevo.core.Simulation;

import java.util.Map;

@JsonIdentityInfo(
        generator = ObjectIdGenerators.PropertyGenerator.class,
        property = "id")
public class IntegerTrait implements Trait<Integer> {

    public static final long serialVersionUID = 1L;
    public int id;

    private final boolean canDisable;
    private boolean disabled;
    private final int value, minValue, maxValue, maxIncrement, disableValue;
    private final String geneName;
    private final EvolvableInteger.MutationMethod mutationMethod;
    private float mutationRate;

    public IntegerTrait(String geneName, int minValue, int maxValue,
                        EvolvableInteger.MutationMethod mutationMethod, int maxIncrement,
                        boolean canDisable, int disableValue, boolean disabled, int value) {
        this.geneName = geneName;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.mutationMethod = mutationMethod;
        this.maxIncrement = maxIncrement;
        this.canDisable = canDisable;
        this.disableValue = disableValue;
        this.disabled = disabled;

        this.value = value;
    }

    public IntegerTrait(String geneName, int minValue, int maxValue,
                        EvolvableInteger.MutationMethod mutationMethod, int maxIncrement,
                        boolean canDisable, int disableValue, boolean disabled) {
        this.geneName = geneName;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.mutationMethod = mutationMethod;
        this.maxIncrement = maxIncrement;
        this.canDisable = canDisable;
        this.disableValue = disableValue;
        this.disabled = disabled;

        value = newRandomValue();
    }

    public static int fromFloat(float value) {
        return Math.round(value);
    }

    @Override
    public Integer getValue(Map<String, Object> dependencies) {
        if (canDisable && dependencies.containsKey("Disable " + geneName)) {
            disabled = (boolean) dependencies.get("Disable " + geneName);
            if (disabled)
                return disableValue;
        }
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
    public Integer newRandomValue() {
        int maxNewValue, minNewValue;
        if (mutationMethod.equals(EvolvableInteger.MutationMethod.INCREMENT_ONLY_UP)) {
            minNewValue = value;
            maxNewValue = Math.min(value + maxIncrement, maxValue);
        } else if (mutationMethod.equals(EvolvableInteger.MutationMethod.INCREMENT_ANY_DIR)) {
            minNewValue = Math.max(value - maxIncrement, minValue);
            maxNewValue = Math.min(value + maxIncrement, maxValue);
        } else {
            minNewValue = minValue;
            maxNewValue = maxValue;
        }
        return Simulation.RANDOM.nextInt(maxNewValue - minNewValue) + minNewValue;
    }

    public EvolvableInteger.MutationMethod getMutationMethod() {
        return mutationMethod;
    }

    public int getMaxIncrement() {
        return maxIncrement;
    }

    public int getMaxValue() {
        return maxValue;
    }

    public int getMinValue() {
        return minValue;
    }

    @Override
    public Trait<Integer> createNew(Integer value) {
        return new IntegerTrait(geneName, minValue, maxValue, mutationMethod,
                maxIncrement, canDisable, disableValue, disabled, value);
    }

    @Override
    public boolean canDisable() {
        return canDisable;
    }

    @Override
    public boolean isDisabled() {
        return disabled;
    }

    @Override
    public String getTraitName() {
        return geneName;
    }
}
