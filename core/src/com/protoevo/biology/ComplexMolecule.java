package com.protoevo.biology;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.protoevo.env.Environment;

import java.io.Serializable;

/**
 * Complex molecules are required for the construction of specialised cell behaviour.
 */

@JsonIdentityInfo(
        generator = ObjectIdGenerators.IntSequenceGenerator.class,
        scope = Environment.class)
public class ComplexMolecule implements Serializable {

    public static final long serialVersionUID = 1L;

    private final float signature, getProductionCost;

    public ComplexMolecule(float signature, float getProductionCost) {
        this.signature = signature;
        this.getProductionCost = getProductionCost;
    }

    /**
     * @return energy required to produce one mass unit of the complex molecule
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

    public static ComplexMolecule fromString(String s) {
        if (s.startsWith("ComplexMolecule(") && s.endsWith(")"))
            s = s.substring(16, s.length() - 1);
        return new ComplexMolecule(Float.parseFloat(s), 0);
    }

    @Override
    public int hashCode() {
        return Float.hashCode(getSignature());
    }
}
