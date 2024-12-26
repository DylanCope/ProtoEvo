package com.protoevo.biology.evolution;

import com.badlogic.gdx.math.MathUtils;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.protoevo.core.Simulation;
import com.protoevo.env.Environment;

import java.util.Map;

@JsonIdentityInfo(
        generator = ObjectIdGenerators.IntSequenceGenerator.class,
        scope = Environment.class)
public class IntegerTrait implements Trait<Integer> {

    public static final long serialVersionUID = 1L;

    private boolean canDisable;
    private boolean disabled;
    private int value, minValue, maxValue, maxIncrement, disableValue;
    private String geneName;
    private EvolvableInteger.MutationMethod mutationMethod;
    private float mutationRate;
    private int mutationCount = 0;

    public IntegerTrait(String geneName) {}

    public IntegerTrait(IntegerTrait other, int value) {
        this.geneName = other.geneName;
        this.minValue = other.minValue;
        this.maxValue = other.maxValue;
        this.mutationMethod = other.mutationMethod;
        this.maxIncrement = other.maxIncrement;
        this.canDisable = other.canDisable;
        this.disableValue = other.disableValue;
        this.disabled = other.disabled;
        this.mutationRate = other.mutationRate;
        this.mutationCount = other.mutationCount;
        this.value = value;
    }

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

    public IntegerTrait(EvolvableInteger evolvableInteger, int value) {
        this.geneName = evolvableInteger.name();
        this.minValue = evolvableInteger.min();
        this.maxValue = evolvableInteger.max();
        this.mutationMethod = evolvableInteger.mutateMethod();
        this.maxIncrement = evolvableInteger.maxIncrement();
        this.canDisable = evolvableInteger.canDisable();
        this.disableValue = evolvableInteger.disableValue();
        this.disabled = false;
        this.value = value;
    }

    public IntegerTrait(EvolvableInteger evolvableInteger) {
        this.geneName = evolvableInteger.name();
        this.minValue = evolvableInteger.min();
        this.maxValue = evolvableInteger.max();
        this.mutationMethod = evolvableInteger.mutateMethod();
        this.maxIncrement = evolvableInteger.maxIncrement();
        this.canDisable = evolvableInteger.canDisable();
        this.disableValue = evolvableInteger.disableValue();
        this.disabled = false;
        this.value = newRandomValue();
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
    public int getMutationCount() {
        return mutationCount;
    }

    @Override
    public void incrementMutationCount() {
        mutationCount++;
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
        return MathUtils.random(maxNewValue - minNewValue - 1) + minNewValue;
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
        return new IntegerTrait(this, value);
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
