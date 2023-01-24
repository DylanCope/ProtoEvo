package com.protoevo.biology.evolution;

import com.protoevo.biology.neat.NetworkGenome;
import com.protoevo.biology.neat.Neuron;
import com.protoevo.biology.neat.NeuronGene;

import java.util.function.Supplier;

public class GeneRegulatoryNetworkFactory {

    private static void addFloatGeneIO(NetworkGenome networkGenome, String trait, FloatTrait gene) {
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

    private static float getMinValueGiven(int currentValue, int maxIncrement, int absMin) {
        int min = currentValue - maxIncrement;
        return (float) Math.max(min, absMin);
    }

    private static float getMaxValueGiven(int currentValue, int maxIncrement, int absMax) {
        int max = currentValue + maxIncrement;
        return (float) Math.min(max, absMax);
    }

    private static void addIntegerSynapse(NetworkGenome networkGenome, String trait, Supplier<Float> getMin, Supplier<Float> getMax) {
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

    private static void addIntegerGeneIO(NetworkGenome networkGenome, String trait, IntegerTrait gene) {
        if (gene.getMutationMethod().equals(EvolvableInteger.MutationMethod.RANDOM_SAMPLE)) {
            int min = gene.getMinValue();
            int max = gene.getMaxValue();
            addIntegerSynapse(networkGenome, trait, () -> (float) min, () -> (float) max);
        } else if (gene.getMutationMethod().equals(EvolvableInteger.MutationMethod.INCREMENT_ANY_DIR)) {
            Supplier<Float> getMin = () -> getMinValueGiven(gene.getValue(), gene.getMaxIncrement(), gene.getMinValue());
            Supplier<Float> getMax = () -> getMaxValueGiven(gene.getValue(), gene.getMaxIncrement(), gene.getMaxValue());
            addIntegerSynapse(networkGenome, trait, getMin, getMax);
        } else {
            Supplier<Float> getMin = () -> Float.valueOf(gene.getValue());
            Supplier<Float> getMax = () -> getMaxValueGiven(gene.getValue(), gene.getMaxIncrement(), gene.getMaxValue());
            addIntegerSynapse(networkGenome, trait, getMin, getMax);
        }
    }

    private static void addBooleanGeneIO(NetworkGenome networkGenome, String trait, BooleanTrait gene) {
        NeuronGene sensor = networkGenome.addSensor(getInputName(trait));
        NeuronGene output = networkGenome.addOutput(
                trait + ":Output",
                z -> (z < 0) ? -1f : 1f
        );
        networkGenome.addSynapse(sensor, output, 1);
    }

    public static NetworkGenome createNetworkGenome(GeneExpressionFunction geneExpressionFunction) {
        GeneExpressionFunction.ExpressionNodes expressionNodes = geneExpressionFunction.getGenes();

        NetworkGenome networkGenome = new NetworkGenome();
        networkGenome.addSensor("Bias");

        for (GeneExpressionFunction.ExpressionNode node : expressionNodes.values()) {
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
                addFloatGeneIO(networkGenome, name, (FloatTrait) trait);
            else if (trait instanceof IntegerTrait && !networkGenome.hasSensor(getInputName(name)))
                addIntegerGeneIO(networkGenome, name, (IntegerTrait) trait);
            else if (trait instanceof BooleanTrait && !networkGenome.hasSensor(getInputName(name)))
                addBooleanGeneIO(networkGenome, name, (BooleanTrait) trait);
        }

        GeneExpressionFunction.Regulators regulators = geneExpressionFunction.getGeneRegulators();
        for (String regulator : regulators.keySet())
            networkGenome.addSensor(regulator);

//        for (int i = 0; i < 10; i++) {
//            networkGenome.mutate();
//        }

        return networkGenome;
    }
}
