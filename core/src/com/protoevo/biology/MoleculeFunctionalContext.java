package com.protoevo.biology;

import com.badlogic.gdx.math.MathUtils;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface MoleculeFunctionalContext extends Consumer<ComplexMolecule> {

    interface MoleculeFunction extends BiConsumer<ComplexMolecule, Float> {
        void accept(ComplexMolecule molecule, Float potency);
    }

    default float toFunctionSignature(ComplexMolecule molecule) {
        return molecule.signature();
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

    default Optional<MoleculeFunction> getClosestFunction(float moleculeSignature) {
        Map<MoleculeFunction, Float> functionSignatures = getMoleculeFunctionSignatures();
        return functionSignatures.entrySet().stream().min((e1, e2) -> {
            float diff1 = Math.abs(e1.getValue() - moleculeSignature);
            float diff2 = Math.abs(e2.getValue() - moleculeSignature);
            return Float.compare(diff1, diff2);
        }).map(Map.Entry::getKey);
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
        Optional<MoleculeFunction> function = getClosestFunction(moleculeSignature);

        if (function.isPresent()) {
            MoleculeFunction closestFunction = function.get();
            float potency = getMoleculePotency(moleculeSignature, closestFunction);
            closestFunction.accept(molecule, potency);
        }
    }
}
