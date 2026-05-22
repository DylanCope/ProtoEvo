package com.protoevo.biology.evolution;

/**
 * 24-residue surface signature carried by protozoa and matched by the
 * "protozoa key" on each PhagocyticReceptor. Predation is meant to be
 * harder than grazing, so this is shorter (smaller search space) AND
 * the match threshold is stricter (80% vs 50%).
 *
 * Combined effect: a predator lineage has to closely track a prey
 * lineage's evolving signature to keep eating it. Prey can escape
 * predation by drifting their signature faster than predators can
 * follow — a Red Queen race in 24 dimensions.
 */
public class ProtozoaSignatureTrait extends SignatureTrait {

    public static final long serialVersionUID = 1L;

    // 75-residue sequence. Used for BOTH the receiving receptor (identity)
    // and the phagocytic receptor (target-seeking) on a Protozoan — same
    // trait class, two independent fields, so each evolves on its own
    // trajectory. The asymmetric A.phag-vs-B.recv match rule then creates
    // selection pressure for a lineage to keep its OWN phag ≠ recv, which
    // prevents kin cannibalism (since same-lineage A and B have
    // A.phag ≈ B.recv only if a lineage's own phag ≈ recv).
    public static final int LENGTH = 75;

    public ProtozoaSignatureTrait() {}

    public ProtozoaSignatureTrait(String geneName) {
        super(geneName);
    }

    public ProtozoaSignatureTrait(ProtozoaSignatureTrait other, AminoAcidSequence value) {
        super(other, value);
    }

    @Override
    protected int sequenceLength() {
        return LENGTH;
    }

    /**
     * Lower per-residue rate (0.025) because the sequence is now 75
     * long: expected ~1.9 substitutions per generation. Faster than that
     * and lineages diverge so quickly that co-evolutionary tracking
     * (predator following prey) can never catch up.
     */
    @Override
    protected float perResidueMutationRate() {
        return 0.025f;
    }

    @Override
    public Trait<AminoAcidSequence> createNew(AminoAcidSequence value) {
        return new ProtozoaSignatureTrait(this, new AminoAcidSequence(value));
    }
}
