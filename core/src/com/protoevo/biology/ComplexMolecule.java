package com.protoevo.biology;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Complex molecules are required for the construction of specialised cell behaviour.
 */
public class ComplexMolecule implements Serializable {

    public static final long serialVersionUID = 1L;

    public static final int possibleMolecules = 128;

    private final float signature, productionCost;

    private final static Map<Float, ComplexMolecule> cache = new HashMap<>(possibleMolecules, 1);

    private ComplexMolecule(float signature, float productionCost) {
        this.signature = signature;
        this.productionCost = productionCost;
    }

    /**
     * @return energy required to produce one mass unit of the complex molecule
     */
    public float getProductionCost() {
        return productionCost;
    }

    public float getSignature() {
        return signature;
    }

    public static ComplexMolecule fromSignature(float signature) {
        signature = (float) (Math.floor(signature * possibleMolecules) / possibleMolecules);
        if (!cache.containsKey(signature)) {
            ComplexMolecule molecule = new ComplexMolecule(signature, 1e3f);
            cache.put(signature, molecule);
            return molecule;
        }
        return cache.get(signature);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (o instanceof ComplexMolecule) {
            ComplexMolecule other = (ComplexMolecule) o;
            return other.getSignature() == getSignature();
        }
        return false;
    }

    @Override
    public String toString() {
        return "ComplexMolecule(" + getSignature() + ")";
    }

    @Override
    public int hashCode() {
        return Float.hashCode(getSignature());
    }
}
