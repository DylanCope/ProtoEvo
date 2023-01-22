package com.protoevo.biology.evolution;

import com.protoevo.biology.neat.NetworkGenome;
import com.protoevo.biology.neat.Neuron;
import com.protoevo.biology.neat.NeuronGene;

import java.io.Serializable;
import java.util.Map;
import java.util.function.Supplier;

public class GeneRegulatoryNetworkTrait implements Trait<NetworkGenome>, Serializable {
    public static final long serialVersionUID = -1259753801126730417L;

    private final NetworkGenome networkGenome;
    private final String geneName;

    public GeneRegulatoryNetworkTrait(String geneName) {
        this.geneName = geneName;
        networkGenome = newRandomValue();
    }

    public GeneRegulatoryNetworkTrait(String geneName, NetworkGenome networkGenome) {
        this.geneName = geneName;
        this.networkGenome = networkGenome;
    }

    @Override
    public Trait<NetworkGenome> mutate() {
        NetworkGenome mutated = new NetworkGenome(networkGenome);
        mutated.mutate();
        return createNew(mutated);
    }

    private void addFloatGeneIO(String trait, FloatTrait gene) {
        float min = gene.getMinValue();
        float max = gene.getMaxValue();
        NeuronGene sensor = networkGenome.addSensor(
                getInputName(trait),
                z -> 2 * (z - min) / (max - min) - 1
        );
        NeuronGene output = networkGenome.addOutput(
                getOutputName(trait),
                z -> min + (max - min) * Neuron.Activation.SIGMOID.apply(z)
        );
        networkGenome.addSynapse(sensor, output, 1);
    }

    public static String getInputName(String geneName) {
        return geneName + ":Input";
    }

    public static String getOutputName(String geneName) {
        return geneName + ":Output";
    }

    public void prependGeneNames(String prefix) {
        networkGenome.iterateNeuronGenes().forEachRemaining(
                neuronGene -> neuronGene.setLabel(prefix + "/" + neuronGene.getLabel())
        );
    }

    private float getMinValueGiven(int currentValue, int maxIncrement, int absMin) {
        int min = currentValue - maxIncrement;
        return (float) Math.max(min, absMin);
    }

    private float getMaxValueGiven(int currentValue, int maxIncrement, int absMax) {
        int max = currentValue + maxIncrement;
        return (float) Math.min(max, absMax);
    }

    private void addIntegerSynapse(String trait, Supplier<Float> getMin, Supplier<Float> getMax) {
        NeuronGene sensor = networkGenome.addSensor(
                getInputName(trait),
                z -> 2 * (z - getMin.get()) / (getMax.get() - getMin.get()) - 1
        );
        NeuronGene output = networkGenome.addOutput(
                getOutputName(trait),
                z -> (float) Math.round(getMin.get() + (getMax.get() - getMin.get()) * Neuron.Activation.SIGMOID.apply(z))
        );
        networkGenome.addSynapse(sensor, output, 1);
    }

    private void addIntegerGeneIO(String trait, IntegerTrait gene) {
        if (gene.getMutationMethod().equals(EvolvableInteger.MutationMethod.RANDOM_SAMPLE)) {
            int min = gene.getMinValue();
            int max = gene.getMaxValue();
            addIntegerSynapse(trait, () -> (float) min, () -> (float) max);
        } else if (gene.getMutationMethod().equals(EvolvableInteger.MutationMethod.INCREMENT_ANY_DIR)) {
            Supplier<Float> getMin = () -> getMinValueGiven(gene.getValue(), gene.getMaxIncrement(), gene.getMinValue());
            Supplier<Float> getMax = () -> getMaxValueGiven(gene.getValue(), gene.getMaxIncrement(), gene.getMaxValue());
            addIntegerSynapse(trait, getMin, getMax);
        } else {
            Supplier<Float> getMin = () -> Float.valueOf(gene.getValue());
            Supplier<Float> getMax = () -> getMaxValueGiven(gene.getValue(), gene.getMaxIncrement(), gene.getMaxValue());
            addIntegerSynapse(trait, getMin, getMax);
        }
    }

    private void addBooleanGeneIO(String trait, BooleanTrait gene) {
        NeuronGene sensor = networkGenome.addSensor(getInputName(trait));
        NeuronGene output = networkGenome.addOutput(
                trait + ":Output",
                z -> (z < 0) ? -1f : 1f
        );
        networkGenome.addSynapse(sensor, output, 1);
    }

    private void createInitialNetworkGenome(Map<String, Object> dependencies) {
        GeneExpressionFunction.Genes genes =
                (GeneExpressionFunction.Genes) dependencies.get(GeneExpressionFunction.GENES_TRAIT_NAME);

        for (GeneExpressionFunction.ExpressionNode node : genes.values()) {
            Trait<?> trait = node.getTrait();
            String name = node.getName();
            if (trait instanceof RegulatedFloatTrait && !networkGenome.hasSensor(getInputName(name))) {
                RegulatedFloatTrait regulatedFloatTrait = (RegulatedFloatTrait) trait;
                float min = regulatedFloatTrait.getMinValue();
                float max = regulatedFloatTrait.getMaxValue();
                networkGenome.addOutput(
                        getOutputName(name),
                        z -> min + (max - min) * Neuron.Activation.SIGMOID.apply(z)
                );
            } else if (trait instanceof FloatTrait && !networkGenome.hasSensor(getInputName(name)))
                addFloatGeneIO(name, (FloatTrait) trait);
            else if (trait instanceof IntegerTrait && !networkGenome.hasSensor(getInputName(name)))
                addIntegerGeneIO(name, (IntegerTrait) trait);
            else if (trait instanceof BooleanTrait && !networkGenome.hasSensor(getInputName(name)))
                addBooleanGeneIO(name, (BooleanTrait) trait);
        }

        if (dependencies.containsKey("Gene Regulators")) {
            GeneExpressionFunction.GeneRegulators geneRegulators =
                    (GeneExpressionFunction.GeneRegulators) dependencies.get("Gene Regulators");
            for (String regulator : geneRegulators.keySet())
                if (!networkGenome.hasSensor(regulator))
                    networkGenome.addSensor(regulator);
        }

        for (int i = 0; i < 100; i++) {
            networkGenome.mutate();
        }
    }

    @Override
    public NetworkGenome getValue(Map<String, Object> dependencies) {
        if (networkGenome == null && dependencies != null
                && dependencies.containsKey(GeneExpressionFunction.GENES_TRAIT_NAME))
            createInitialNetworkGenome(dependencies);

        return networkGenome;
    }

    @Override
    public NetworkGenome newRandomValue() {
        if (networkGenome == null) {
            NetworkGenome newNetworkGenome = new NetworkGenome();
            newNetworkGenome.addSensor("Bias");
            return newNetworkGenome;
        }
        NetworkGenome newNetworkGenome = new NetworkGenome(networkGenome);
        newNetworkGenome.mutate();
        return newNetworkGenome;
    }

    @Override
    public Trait<NetworkGenome> createNew(NetworkGenome value) {
        return new GeneRegulatoryNetworkTrait(geneName, value);
    }

    @Override
    public String getTraitName() {
        return geneName;
    }
}
