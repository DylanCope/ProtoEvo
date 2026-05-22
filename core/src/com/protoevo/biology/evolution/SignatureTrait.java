package com.protoevo.biology.evolution;

import java.io.Serializable;
import java.util.Map;

/**
 * Base evolvable trait wrapping an {@link AminoAcidSequence}. Concrete
 * subclasses fix the sequence length (e.g. {@link PlantSignatureTrait} is
 * 50, {@link ProtozoaSignatureTrait} is 24). Mutation is per-character
 * point substitution rather than full re-roll, so an arms-race opponent
 * can hill-climb on a partial match instead of needing a lucky full hit.
 */
public abstract class SignatureTrait
        implements Trait<AminoAcidSequence>, Serializable {

    public static final long serialVersionUID = 1L;

    private AminoAcidSequence value;
    private String geneName;
    private float mutationRate;
    private int mutationCount;

    protected SignatureTrait() {}

    protected SignatureTrait(String geneName) {
        this.geneName = geneName;
        this.value = newRandomValue();
    }

    protected SignatureTrait(SignatureTrait other, AminoAcidSequence value) {
        this.geneName = other.geneName;
        this.mutationRate = other.mutationRate;
        this.mutationCount = other.mutationCount;
        this.value = value;
    }

    /** Subclasses fix the sequence length (plant vs protozoa signature). */
    protected abstract int sequenceLength();

    /**
     * Per-residue mutation rate. With length 50 and rate 0.04 we expect ~2
     * substitutions per mutation event; with length 24 and rate 0.04 we
     * expect ~1. Subclasses can override if they want a different cadence.
     */
    protected float perResidueMutationRate() {
        return 0.04f;
    }

    @Override
    public AminoAcidSequence getValue(Map<String, Object> dependencies) {
        return value;
    }

    @Override
    public AminoAcidSequence newRandomValue() {
        return new AminoAcidSequence(sequenceLength());
    }

    /**
     * Override the default Trait.cloneWithMutation (which would fully
     * re-roll the sequence and destroy any partial-match adaptation the
     * lineage had accumulated). Instead apply point mutations to the
     * existing residues so the sequence drifts continuously and
     * descendants stay recognizably similar to the parent.
     */
    @Override
    public Trait<AminoAcidSequence> cloneWithMutation() {
        if (Math.random() > getMutationRate())
            return copy();
        AminoAcidSequence mutated = value == null
                ? newRandomValue()
                : value.mutated(perResidueMutationRate());
        Trait<AminoAcidSequence> child = createNew(mutated);
        child.incrementMutationCount();
        return child;
    }

    @Override
    public void setMutationRate(float rate) {
        this.mutationRate = rate;
    }

    @Override
    public float getMutationRate() {
        return mutationRate;
    }

    @Override
    public int getMutationCount() {
        return mutationCount;
    }

    @Override
    public void incrementMutationCount() {
        mutationCount++;
    }

    @Override
    public String valueString() {
        return value == null ? "" : value.toString();
    }

    @Override
    public String getTraitName() {
        return geneName;
    }
}
