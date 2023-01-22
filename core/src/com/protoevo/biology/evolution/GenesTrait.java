package com.protoevo.biology.evolution;

import java.util.Map;

public class GenesTrait implements Trait<GeneExpressionFunction.Genes> {

    private final String geneName;
    private final GeneExpressionFunction.Genes genes;

    public GenesTrait(String geneName) {
        this.geneName = geneName;
        genes = newRandomValue();
    }

    public GenesTrait(String geneName, GeneExpressionFunction.Genes genes) {
        this.geneName = geneName;
        this.genes = genes;
    }

    @Override
    public GeneExpressionFunction.Genes getValue(Map<String, Object> dependencies) {
        return genes;
    }

    @Override
    public GeneExpressionFunction.Genes newRandomValue() {
        GeneExpressionFunction.Genes newGenes = new GeneExpressionFunction.Genes();
        if (genes != null)  // todo: implement some mutation here (e.g. duplication, deletion, etc.)
            newGenes.putAll(genes);
        return newGenes;
    }

    @Override
    public Trait<GeneExpressionFunction.Genes> createNew(GeneExpressionFunction.Genes value) {
        return new GenesTrait(geneName, value);
    }

    @Override
    public String getTraitName() {
        return geneName;
    }
}
