package com.protoevo.biology.evolution;

import com.badlogic.gdx.math.MathUtils;
import com.protoevo.biology.nn.*;
import com.protoevo.biology.nn.meta.GRNTag;
import com.protoevo.env.Environment;

import java.util.function.Supplier;

public class GRNFactory {

    public static class ExpressionNodeGRNTag implements GRNTag {
        private GeneExpressionFunction.ExpressionNode node;

        public ExpressionNodeGRNTag() {}

        public ExpressionNodeGRNTag(GeneExpressionFunction.ExpressionNode node) {
            this.node = node;
        }

        @Override
        public Object apply(GeneExpressionFunction fn) {
            return fn.getExpressionNode(node.getName());
        }
    }

    private static void addFloatGeneIO(
            NetworkGenome networkGenome, GeneExpressionFunction.ExpressionNode node, FloatTrait gene) {
        String trait = node.getName();
        float min = gene.getMinValue();
        float max = gene.getMaxValue();

        if (gene.isRegulated()) {
            NeuronGene output = networkGenome.addOutput(
                    getOutputName(trait),
                    ActivationFn.getOutputMapper(min, max),
//                    (GRNTag) fn -> fn.getExpressionNode(node.getName())
                    new ExpressionNodeGRNTag(node)
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
//                (GRNTag) fn -> fn.getExpressionNode(node.getName())
                new ExpressionNodeGRNTag(node)
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
//                (GRNTag) fn -> fn.getExpressionNode(node.getName())
                new ExpressionNodeGRNTag(node)
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
        if (trait instanceof ControlTrait controlTrait
                && !networkGenome.hasOutput(getOutputName(name))) {
            float min = controlTrait.getMinValue();
            float max = controlTrait.getMaxValue();
            NeuronGene outputGene = networkGenome.addOutput(
                    getOutputName(name),
                    ActivationFn.getOutputMapper(min, max),
//                    (GRNTag) fn -> fn.getExpressionNode(node.getName())
                    new ExpressionNodeGRNTag(node)
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

    public static class GRNTagRegulatorNode implements GRNTag {
        private String node;

        public GRNTagRegulatorNode() {}

        public GRNTagRegulatorNode(String node) {
            this.node = node;
        }

        @Override
        public Object apply(GeneExpressionFunction fn) {
            return fn.getGeneRegulators().get(node);
        }
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
                GRNTagRegulatorNode regulatorNode = new GRNTagRegulatorNode(regulator);
                NeuronGene regulatorSensor = networkGenome.addSensor(regulator, regulatorNode);
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

        // Seed basic foraging reflexes. Two signals:
        //   - "Plant Density Local" → "Cilia Thrust": swim whenever you smell
        //     food. This keeps cells moving inside dense plant clusters where
        //     the gradient is ~0.
        //   - "Plant Gradient" → "Cilia Turn": when food is behind you the
        //     gradient is negative; with negative weight that becomes positive
        //     turn output, so the cell rotates until plants are ahead.
        // Wrapped in try/catch in case a non-protozoan genome (surface-node
        // sub-genomes etc.) ever flows through here with a different shape.
        try {
            seedReflexSynapse(networkGenome, "Plant Density Local", "Cilia Thrust:Output", 2.0f);
            seedReflexSynapse(networkGenome, "Plant Gradient",       "Cilia Turn:Output",  -1.0f);
        } catch (Throwable t) {
            System.err.println("[seed] failed: " + t);
        }

        return networkGenome;
    }

    private static void seedReflexSynapse(NetworkGenome g, String inLabel,
                                          String outLabel, float weight) {
        com.protoevo.biology.nn.NeuronGene in  = g.getNeuronGene(inLabel);
        com.protoevo.biology.nn.NeuronGene out = g.getNeuronGene(outLabel);
        // For non-protozoan genomes (plants, surface-node sub-genomes, etc.)
        // these labels don't exist. Bail silently.
        if (in == null || out == null) return;

        // Strict-edit version: only nudge an existing synapse if there is one.
        // We deliberately don't add a new synapse — appending was suspected of
        // interacting badly with downstream phenotype building during world
        // gen, so this strictly mutates state already present.
        for (com.protoevo.biology.nn.SynapseGene s : g.getSynapseGenes()) {
            if (s.getIn() != null && s.getOut() != null
                    && s.getIn().getId() == in.getId()
                    && s.getOut().getId() == out.getId()) {
                s.setWeight(weight);
                s.setDisabled(false);
                return;
            }
        }
        // No existing synapse → silently do nothing. The 50% initial
        // connectivity will mean some protozoa get the seed and some don't,
        // which is fine — the gene pool will spread it.
    }
}
