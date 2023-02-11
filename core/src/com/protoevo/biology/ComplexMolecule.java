package com.protoevo.biology;

/**
 * Complex molecules are required for the construction of specialised cell behaviour.
 */
public record ComplexMolecule(float signature, float getProductionCost) {

    /**
     * @return energy required to produce one unit of the complex molecule
     */
    public float getProductionCost() {
        return getProductionCost;
    }
}
