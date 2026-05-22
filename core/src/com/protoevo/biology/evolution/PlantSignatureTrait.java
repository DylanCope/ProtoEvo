package com.protoevo.biology.evolution;

/**
 * 50-residue surface signature carried by plants and matched by the
 * "plant key" on each PhagocyticReceptor. Used for the relatively loose
 * plant-recognition arms race: a 50% identity match is the threshold to
 * engulf at all, perfect match gives full engulf rate.
 */
public class PlantSignatureTrait extends SignatureTrait {

    public static final long serialVersionUID = 1L;

    public static final int LENGTH = 50;

    public PlantSignatureTrait() {}

    public PlantSignatureTrait(String geneName) {
        super(geneName);
    }

    public PlantSignatureTrait(PlantSignatureTrait other, AminoAcidSequence value) {
        super(other, value);
    }

    @Override
    protected int sequenceLength() {
        return LENGTH;
    }

    @Override
    public Trait<AminoAcidSequence> createNew(AminoAcidSequence value) {
        return new PlantSignatureTrait(this, new AminoAcidSequence(value));
    }
}
