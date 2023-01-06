package com.protoevo.biology.evolution;

import com.protoevo.core.Simulation;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public interface Gene<T> extends Evolvable.Component, Serializable {

    HashMap<String, Object> NO_DEPS = new HashMap<>();

    default T getValue() {
        return getValue(Gene.NO_DEPS);
    }

    T getValue(Map<String, Object> dependencies);
    T newRandomValue();

    Gene<T> createNew(T value);

    default Gene<T> mutate() {
        Gene<T> newGene = createNew(newRandomValue());
        if (newGene == null)
            throw new RuntimeException(
                "Failed to mutate " + getTraitName() + ": received null. "  +
                "Ensure that createNew and newRandomValue are implemented for " + this.getClass()
            );
        return newGene;
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

    default String geneString() {
        return valueString() + ":" + (isDisabled() ? "0" : "1");
    }

    default Gene<?> crossover(Gene<?> other) {
        if (Simulation.RANDOM.nextBoolean())
            return this;
        else
            return other;
    }

    default Gene<?> copy() {
        return createNew(getValue());
    }

}
