package com.protoevo.biology.evolution;

import java.util.Map;

public class GenesGene implements Gene<GeneExpressionFunction.Genes> {

    private final String geneName;
    private final GeneExpressionFunction.Genes genes;

    public GenesGene(String geneName) {
        this.geneName = geneName;
        genes = newRandomValue();
    }

    public GenesGene(String geneName, GeneExpressionFunction.Genes genes) {
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
        if (genes != null)
            newGenes.putAll(genes);
        return newGenes;
    }

    @Override
    public Gene<GeneExpressionFunction.Genes> createNew(GeneExpressionFunction.Genes value) {
        return new GenesGene(geneName, value);
    }

    @Override
    public String getTraitName() {
        return geneName;
    }
}
