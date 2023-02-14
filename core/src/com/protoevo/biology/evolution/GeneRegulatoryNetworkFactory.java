package com.protoevo.biology.evolution;

import com.badlogic.gdx.math.MathUtils;
import com.protoevo.biology.neat.NetworkGenome;
import com.protoevo.biology.neat.Neuron;
import com.protoevo.biology.neat.NeuronGene;
import com.protoevo.core.Simulation;

import java.util.function.Supplier;

public class GeneRegulatoryNetworkFactory {

    private static void addFloatGeneIO(
            NetworkGenome networkGenome, GeneExpressionFunction.ExpressionNode node, FloatTrait gene) {
        String trait = node.getName();
        float min = gene.getMinValue();
        float max = gene.getMaxValue();
//        NeuronGene sensor = networkGenome.addSensor(
//                getInputName(trait),
//                z -> 2 * (z - min) / (max - min) - 1,
//                node
//        );
        NeuronGene sensor = networkGenome.getNeuronGene("Bias");
        if (sensor == null)
            sensor = networkGenome.addSensor("Bias");

        if (gene.isRegulated()) {
            NeuronGene output = networkGenome.addOutput(
                    getOutputName(trait),
                    z -> MathUtils.clamp((0.5f + 0.5f * z) * (max - min) + min, min, max),
                    node
            );
            networkGenome.addSynapse(sensor, output, Simulation.RANDOM.nextFloat(-1, 1));
        }
//        else {
//            networkGenome.addSensor(sensor);
//        }
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

    private static void addIntegerSynapse(
            NetworkGenome networkGenome, GeneExpressionFunction.ExpressionNode node, IntegerTrait gene,
            Supplier<Float> getMin, Supplier<Float> getMax) {
        String trait = node.getName();
//        NeuronGene sensor = networkGenome.addSensor(
//                getInputName(trait),
//                z -> 2 * (z - getMin.get()) / (getMax.get() - getMin.get()) - 1,
//                node
//        );
        NeuronGene sensor = networkGenome.getNeuronGene("Bias");
        if (sensor == null)
            sensor = networkGenome.addSensor("Bias");

        NeuronGene output = networkGenome.addOutput(
                getOutputName(trait),
                z -> (float) Math.round(getMin.get() + (getMax.get() - getMin.get()) * Neuron.Activation.SIGMOID.apply(z)),
                node
        );
//        networkGenome.addSynapse(sensor, output, 1);
        networkGenome.addSynapse(sensor, output, Simulation.RANDOM.nextFloat(-1, 1));
    }

    private static void addIntegerGeneIO(
            NetworkGenome networkGenome, GeneExpressionFunction.ExpressionNode node, IntegerTrait gene) {
        if (gene.getMutationMethod().equals(EvolvableInteger.MutationMethod.RANDOM_SAMPLE)) {
            int min = gene.getMinValue();
            int max = gene.getMaxValue();
            addIntegerSynapse(networkGenome, node, gene, () -> (float) min, () -> (float) max);
        } else if (gene.getMutationMethod().equals(EvolvableInteger.MutationMethod.INCREMENT_ANY_DIR)) {
            Supplier<Float> getMin = () -> getMinValueGiven(gene.getValue(), gene.getMaxIncrement(), gene.getMinValue());
            Supplier<Float> getMax = () -> getMaxValueGiven(gene.getValue(), gene.getMaxIncrement(), gene.getMaxValue());
            addIntegerSynapse(networkGenome, node, gene, getMin, getMax);
        } else {
            Supplier<Float> getMin = () -> Float.valueOf(gene.getValue());
            Supplier<Float> getMax = () -> getMaxValueGiven(gene.getValue(), gene.getMaxIncrement(), gene.getMaxValue());
            addIntegerSynapse(networkGenome, node, gene, getMin, getMax);
        }
    }

    private static void addBooleanGeneIO(
            NetworkGenome networkGenome, GeneExpressionFunction.ExpressionNode node, BooleanTrait gene) {
        String trait = node.getName();

//        NeuronGene sensor = networkGenome.addSensor(getInputName(trait));

        NeuronGene sensor = networkGenome.getNeuronGene("Bias");
        if (sensor == null)
            sensor = networkGenome.addSensor("Bias");

        NeuronGene output = networkGenome.addOutput(
                getOutputName(trait),
                z -> (z < 0) ? -1f : 1f,
                node
        );
//        networkGenome.addSynapse(sensor, output, 1);
        networkGenome.addSynapse(sensor, output, Simulation.RANDOM.nextFloat(-1, 1));
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
                        z -> min + (max - min) * Neuron.Activation.SIGMOID.apply(z),
                        node
                );
            } else if (trait instanceof FloatTrait && !networkGenome.hasSensor(getInputName(name)))
                addFloatGeneIO(networkGenome, node, (FloatTrait) trait);
            else if (trait instanceof IntegerTrait && !networkGenome.hasSensor(getInputName(name)))
                addIntegerGeneIO(networkGenome, node, (IntegerTrait) trait);
            else if (trait instanceof BooleanTrait && !networkGenome.hasSensor(getInputName(name)))
                addBooleanGeneIO(networkGenome, node, (BooleanTrait) trait);
        }

        GeneExpressionFunction.Regulators regulators = geneExpressionFunction.getGeneRegulators();
        for (String regulator : regulators.keySet())
            networkGenome.addSensor(regulator, regulators.get(regulator));

        for (int i = 0; i < 10; i++) {
            networkGenome.mutate();
        }

        return networkGenome;
    }
}
