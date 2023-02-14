package com.protoevo.biology;

import java.io.Serializable;

/**
 * Complex molecules are required for the construction of specialised cell behaviour.
 */
public class ComplexMolecule implements Serializable {
    public static final long serialVersionUID = 1L;

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

    @Override
    public boolean equals(Object o) {
        if (o.equals(this))
            return true;
        if (o instanceof ComplexMolecule) {
            ComplexMolecule other = (ComplexMolecule) o;
            return other.getSignature() == getSignature();
        }
        return false;
    }
}
