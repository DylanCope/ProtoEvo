package com.protoevo.biology;

import java.util.Map;

public interface Constructable {
    /**
     * @return the mass required to contribute to the construction project
     */
    float getRequiredMass();

    /**
     * @return the energy required to contribute to the construction project
     */
    float getRequiredEnergy();

    /**
     * @return the time required to contribute to the construction project
     */
    float getTimeToComplete();

    /**
     * @return the complex molecules required to contribute to the construction project
     */
    Map<ComplexMolecule, Float> getRequiredComplexMolecules();

}
