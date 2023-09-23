package com.protoevo.biology.evolution;

import com.badlogic.gdx.math.MathUtils;
import com.protoevo.biology.nn.*;
import com.protoevo.biology.nn.meta.GRNTag;
import com.protoevo.env.Environment;

import java.util.function.Supplier;

public class GRNFactory {

    private static void addFloatGeneIO(
            NetworkGenome networkGenome, GeneExpressionFunction.ExpressionNode node, FloatTrait gene) {
        String trait = node.getName();
        float min = gene.getMinValue();
        float max = gene.getMaxValue();

        if (gene.isRegulated()) {
            NeuronGene output = networkGenome.addOutput(
                    getOutputName(trait),
                    ActivationFn.getOutputMapper(min, max),
                    (GRNTag) fn -> fn.getExpressionNode(node.getName())
            );
            createBiasExpressionConnection(networkGenome, output);
        }
    }

    private static void createBiasExpressionConnection(
            NetworkGenome networkGenome, NeuronGene output) {
        NeuronGene sensor = getBias(networkGenome);
        output.setMutationRange(
                Environment.settings.evo.minTraitMutationChance.get(),
                Environment.settings.evo.maxTraitMutationChance.get());

        SynapseGene synapseGene = networkGenome.addSynapse(sensor, output);
        synapseGene.setMutationRange(
                Environment.settings.evo.minTraitMutationChance.get(),
                Environment.settings.evo.maxTraitMutationChance.get());
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

    private static float getMaxValueGiven(int currentValue, int maxIncrement, int absMax){
        int max = currentValue + maxIncrement;
        return (float) Math.min(max, absMax);
    }

    private static NeuronGene getBias(NetworkGenome networkGenome)
    {
        NeuronGene sensor = networkGenome.getNeuronGene("Bias");
        if (sensor == null) {
            sensor = networkGenome.addSensor("Bias");
            sensor.setMutationRange(
                    Environment.settings.evo.minMutationChance.get(),
                    Environment.settings.evo.maxMutationChance.get());
        }
        return sensor;
    }

    private static void addIntegerSynapse(
            NetworkGenome networkGenome,
            GeneExpressionFunction.ExpressionNode node,
            IntegerTrait gene,
            Supplier<Float> getMin, Supplier<Float> getMax)
    {
        String trait = node.getName();

        NeuronGene output = networkGenome.addOutput(
                getOutputName(trait),
                ActivationFn.getOutputMapper(getMax.get(), getMin.get()),
                (GRNTag) fn -> fn.getExpressionNode(node.getName())
        );
        createBiasExpressionConnection(networkGenome, output);
    }

    private static void addIntegerGeneIO(
            NetworkGenome networkGenome,
            GeneExpressionFunction.ExpressionNode node,
            IntegerTrait gene)
    {
        if (gene.getMutationMethod().equals(EvolvableInteger.MutationMethod.RANDOM_SAMPLE)) {
            int min = gene.getMinValue();
            int max = gene.getMaxValue();
            addIntegerSynapse(networkGenome, node, gene, () -> (float) min, () -> (float) max);
        }
        else if (gene.getMutationMethod().equals(EvolvableInteger.MutationMethod.INCREMENT_ANY_DIR)) {
            Supplier<Float> getMin = () -> getMinValueGiven(gene.getValue(), gene.getMaxIncrement(), gene.getMinValue());
            Supplier<Float> getMax = () -> getMaxValueGiven(gene.getValue(), gene.getMaxIncrement(), gene.getMaxValue());
            addIntegerSynapse(networkGenome, node, gene, getMin, getMax);
        }
        else {
            Supplier<Float> getMin = () -> Float.valueOf(gene.getValue());
            Supplier<Float> getMax = () -> getMaxValueGiven(gene.getValue(), gene.getMaxIncrement(), gene.getMaxValue());
            addIntegerSynapse(networkGenome, node, gene, getMin, getMax);
        }
    }

    private static void addBooleanGeneIO(
            NetworkGenome networkGenome,
            GeneExpressionFunction.ExpressionNode node,
            BooleanTrait gene)
    {
        String trait = node.getName();
        NeuronGene output = networkGenome.addOutput(
                getOutputName(trait),
                ActivationFn.getBooleanInputMapper(),
                (GRNTag) fn -> fn.getExpressionNode(node.getName())
        );
        createBiasExpressionConnection(networkGenome, output);
    }

    public static void addExpressionIO(
            NetworkGenome networkGenome,
            GeneExpressionFunction.Regulators regulators,
            GeneExpressionFunction.ExpressionNode node)
    {
        Trait<?> trait = node.getTrait();
        String name = node.getName();
        if (trait instanceof ControlTrait
                && !networkGenome.hasOutput(getOutputName(name))) {
            ControlTrait controlTrait = (ControlTrait) trait;
            float min = controlTrait.getMinValue();
            float max = controlTrait.getMaxValue();
            NeuronGene outputGene = networkGenome.addOutput(
                    getOutputName(name),
                    ActivationFn.getOutputMapper(min, max),
                    (GRNTag) fn -> fn.getExpressionNode(node.getName())
            );
            for (String regulator : regulators.keySet()) {
                if (MathUtils.randomBoolean(Environment.settings.evo.initialGenomeConnectivity.get()))
                    continue;

                SynapseGene synapseGene = networkGenome.addSynapse(
                        networkGenome.getNeuronGene(regulator), outputGene);
                synapseGene.setMutationRange(
                        Environment.settings.evo.minRegulationMutationChance.get(),
                        Environment.settings.evo.maxRegulationMutationChance.get());
            }
        }

        else if (trait instanceof FloatTrait && !networkGenome.hasSensor(getInputName(name)))
            addFloatGeneIO(networkGenome, node, (FloatTrait) trait);

        else if (trait instanceof IntegerTrait && !networkGenome.hasSensor(getInputName(name)))
            addIntegerGeneIO(networkGenome, node, (IntegerTrait) trait);

        else if (trait instanceof BooleanTrait && !networkGenome.hasSensor(getInputName(name)))
            addBooleanGeneIO(networkGenome, node, (BooleanTrait) trait);
    }

    public static NetworkGenome createIO(NetworkGenome networkGenome,
                                         GeneExpressionFunction geneExpressionFunction)
    {
        GeneExpressionFunction.ExpressionNodes expressionNodes = geneExpressionFunction.getGenes();

        if (!networkGenome.hasSensor("Bias"))
            networkGenome.addSensor("Bias");

        if (!networkGenome.hasSensor("Random Source"))
            networkGenome.addSensor("Random Source");

        GeneExpressionFunction.Regulators regulators = geneExpressionFunction.getGeneRegulators();
        for (String regulator : regulators.keySet()) {
            if (!networkGenome.hasSensor(regulator)) {
                NeuronGene regulatorSensor = networkGenome.addSensor(regulator,
                        (GRNTag) fn -> fn.getGeneRegulators().get(regulator));
                regulatorSensor.setMutationRange(
                        Environment.settings.evo.minRegulationMutationChance.get(),
                        Environment.settings.evo.maxRegulationMutationChance.get());
            }
        }

        for (GeneExpressionFunction.ExpressionNode node : expressionNodes.values()) {
            if (!networkGenome.hasOutput(getOutputName(node.getName())))
                addExpressionIO(networkGenome, regulators, node);
        }

        return networkGenome;
    }

    public static NetworkGenome createNetworkGenome(GeneExpressionFunction geneExpressionFunction)
    {
        NetworkGenome networkGenome = createIO(new NetworkGenome(), geneExpressionFunction);

        for (int i = 0; i < Environment.settings.evo.initialGRNMutations.get(); i++) {
            networkGenome.mutate();
        }

        return networkGenome;
    }
}
