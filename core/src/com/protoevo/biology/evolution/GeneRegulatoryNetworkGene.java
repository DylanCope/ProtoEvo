package com.protoevo.biology.evolution;

import com.protoevo.biology.neat.NetworkGenome;
import com.protoevo.biology.neat.Neuron;
import com.protoevo.biology.neat.NeuronGene;

import java.io.Serializable;
import java.util.Map;
import java.util.function.Supplier;

public class GeneRegulatoryNetworkGene implements Gene<NetworkGenome>, Serializable {
    public static final long serialVersionUID = -1259753801126730417L;

    private final NetworkGenome networkGenome;
    private final String geneName;

    public GeneRegulatoryNetworkGene(String geneName) {
        this.geneName = geneName;
        networkGenome = newRandomValue();
    }

    public GeneRegulatoryNetworkGene(String geneName, NetworkGenome networkGenome) {
        this.geneName = geneName;
        this.networkGenome = networkGenome;
    }

    @Override
    public Gene<NetworkGenome> mutate() {
        NetworkGenome mutated = new NetworkGenome(networkGenome);
        mutated.mutate();
        return createNew(mutated);
    }

    private void addFloatGeneIO(String trait, FloatGene gene) {
        float min = gene.getMinValue();
        float max = gene.getMaxValue();
        NeuronGene sensor = networkGenome.addSensor(
                trait + " Input",
                z -> 2 * (z - min) / (max - min) - 1
        );
        NeuronGene output = networkGenome.addOutput(
                trait + " Output",
                z -> min + (max - min) * Neuron.Activation.SIGMOID.apply(z)
        );
        networkGenome.addSynapse(sensor, output, 1);
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
                trait + " Input",
                z -> 2 * (z - getMin.get()) / (getMax.get() - getMin.get()) - 1
        );
        NeuronGene output = networkGenome.addOutput(
                trait + " Output",
                z -> (float) Math.round(getMin.get() + (getMax.get() - getMin.get()) * Neuron.Activation.SIGMOID.apply(z))
        );
        networkGenome.addSynapse(sensor, output, 1);
    }

    private void addIntegerGeneIO(String trait, IntegerGene gene) {
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

    private void addBooleanGeneIO(String trait, BooleanGene gene) {
        NeuronGene sensor = networkGenome.addSensor(trait + " Input");
        NeuronGene output = networkGenome.addOutput(
                trait + " Output",
                z -> (z < 0) ? -1f : 1f
        );
        networkGenome.addSynapse(sensor, output, 1);
    }

    @Override
    public NetworkGenome getValue(Map<String, Object> dependencies) {
        if (dependencies != null && dependencies.containsKey("Genes")) {
            GeneExpressionFunction.Genes genes = (GeneExpressionFunction.Genes) dependencies.get("Genes");
            for (GeneExpressionFunction.GeneExpressionNode node : genes.values()) {
                Gene<?> gene = node.getGene();
                String trait = gene.getTraitName();
                if (gene instanceof FloatGene && !networkGenome.hasSensor(trait + " Input"))
                    addFloatGeneIO(trait, (FloatGene) gene);
                else if (gene instanceof IntegerGene && !networkGenome.hasSensor(trait + " Input"))
                    addIntegerGeneIO(trait, (IntegerGene) gene);
                else if (gene instanceof BooleanGene && !networkGenome.hasSensor(trait + " Input"))
                    addBooleanGeneIO(trait, (BooleanGene) gene);
            }
        }
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
    public Gene<NetworkGenome> createNew(NetworkGenome value) {
        return new GeneRegulatoryNetworkGene(geneName, value);
    }

    @Override
    public String getTraitName() {
        return geneName;
    }
}
