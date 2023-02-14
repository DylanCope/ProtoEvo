package com.protoevo.biology;

/**
 * Complex molecules are required for the construction of specialised cell behaviour.
 */
public class ComplexMolecule {

    private final float signature, getProductionCost;

    public ComplexMolecule(float signature, float getProductionCost) {
        this.signature = signature;
        this.getProductionCost = getProductionCost;
    }

    /**
     * @return energy required to produce one unit of the complex molecule
     */
    public float getProductionCost() {
        return getProductionCost;
    }

    public float getSignature() {
        return signature;
    }

    public static ComplexMolecule fromSignature(float signature) {
        return new ComplexMolecule(signature, 0);
    }
}
