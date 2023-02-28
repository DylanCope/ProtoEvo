package com.protoevo.biology;

import com.badlogic.gdx.math.MathUtils;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface MoleculeFunctionalContext extends Consumer<ComplexMolecule>, Serializable {

    interface MoleculeFunction extends BiConsumer<ComplexMolecule, Float>, Serializable {
        void accept(ComplexMolecule molecule, Float potency);
    }

    default float toFunctionSignature(ComplexMolecule molecule) {
        return molecule.getSignature();
    }

    Map<MoleculeFunction, Float> getMoleculeFunctionSignatures();

    default float potencyFalloff(MoleculeFunction fn) {
        return 1f / (3 * getMoleculeFunctionSignatures().size());
    }

    default float moleculeCriticalMatching() {
        return 1f / (2 * getMoleculeFunctionSignatures().size());
    }

    default float getMoleculeMatching(float signature1, float signature2) {
        float diff = Math.abs(signature1 - signature2);
        float criticalMatching = moleculeCriticalMatching();
        return MathUtils.clamp(1 - diff / criticalMatching, 0, 1);
    }

    default float getMatching(ComplexMolecule molecule, float signature) {
        return getMoleculeMatching(toFunctionSignature(molecule), signature);
    }

    default float getMatching(ComplexMolecule molecule1, ComplexMolecule molecule2) {
        return getMoleculeMatching(toFunctionSignature(molecule1), toFunctionSignature(molecule2));
    }

    default float cyclicalDistance(float signature1, float signature2) {
        float sMin = Math.min(signature1, signature2);
        float sMax = Math.max(signature1, signature2);
        return Math.min(sMax - sMin, sMin + 1 - sMax);
    }

    default MoleculeFunction getClosestFunction(float moleculeSignature) {
        Map<MoleculeFunction, Float> functionSignatures = getMoleculeFunctionSignatures();
        MoleculeFunction closestFunction = null;
        float distance = Float.MAX_VALUE;
        for (MoleculeFunction fn : functionSignatures.keySet()) {
            float fnSignature = functionSignatures.get(fn);
            float diff = cyclicalDistance(fnSignature, moleculeSignature);
            if (diff < distance) {
                distance = diff;
                closestFunction = fn;
            }
        }
        return closestFunction;
    }

    default float getMoleculePotency(float moleculeSignature, MoleculeFunction fn) {
        float fnSignature = getMoleculeFunctionSignatures().get(fn);
        float sd = potencyFalloff(fn);
        float x = moleculeSignature - fnSignature;
        return (float) Math.exp(-x*x / (sd*sd));
    }

    default ComplexMolecule getClosestMolecule(Collection<ComplexMolecule> molecules, float moleculeSignature) {
        return molecules.stream().min((m1, m2) -> {
            float diff1 = Math.abs(toFunctionSignature(m1) - moleculeSignature);
            float diff2 = Math.abs(toFunctionSignature(m2) - moleculeSignature);
            return Float.compare(diff1, diff2);
        }).orElse(null);
    }

    default void accept(ComplexMolecule molecule) {
        float moleculeSignature = toFunctionSignature(molecule);
        MoleculeFunction function = getClosestFunction(moleculeSignature);

        if (function != null) {
            float potency = getMoleculePotency(moleculeSignature, function);
            function.accept(molecule, potency);
        }
    }
}
