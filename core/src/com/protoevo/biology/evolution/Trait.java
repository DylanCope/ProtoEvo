package com.protoevo.biology.evolution;

import com.protoevo.core.Simulation;
import com.protoevo.settings.SimulationSettings;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public interface Trait<T> extends Evolvable.Component, Serializable {

    HashMap<String, Object> NO_DEPS = new HashMap<>();

    default T getValue() {
        return getValue(Trait.NO_DEPS);
    }

    T getValue(Map<String, Object> dependencies);
    T newRandomValue();

    Trait<T> createNew(T value);

    void setMutationRate(float rate);
    float getMutationRate();

    default void init() {
        mutateMutationRate();
    }

    default void mutateMutationRate() {
        setMutationRate(Simulation.RANDOM.nextFloat(
                SimulationSettings.minTraitMutationChance,
                SimulationSettings.maxTraitMutationChance
        ));
    }

    default Trait<T> cloneWithMutation() {
        if (Math.random() > getMutationRate())
            return copy();

        if (Simulation.RANDOM.nextBoolean())
            mutateMutationRate();

        Trait<T> newTrait = createNew(newRandomValue());
        if (newTrait == null)
            throw new RuntimeException(
                "Failed to mutate " + getTraitName() + ": received null. "  +
                "Ensure that createNew and newRandomValue are implemented for " + this.getClass()
            );
        return newTrait;
    }

    default boolean canDisable() {
        return false;
    }

    default boolean isDisabled() {
        return false;
    }

    String getTraitName();

    default String valueString() {
        return getValue().toString();
    }

    default String string() {
        return valueString() + ":" + (isDisabled() ? "0" : "1");
    }

    default Trait<?> crossover(Trait<?> other) {
        if (Simulation.RANDOM.nextBoolean())
            return this;
        else
            return other;
    }

    default Trait<T> copy() {
        return createNew(getValue());
    }
}
