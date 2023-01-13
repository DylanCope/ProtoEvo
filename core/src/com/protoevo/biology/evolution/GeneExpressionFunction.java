package com.protoevo.biology.evolution;

import com.protoevo.biology.neat.NetworkGenome;
import com.protoevo.biology.neat.NeuralNetwork;
import com.protoevo.core.settings.Settings;
import com.protoevo.core.Simulation;
import com.protoevo.core.settings.SimulationSettings;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;

public class GeneExpressionFunction implements Evolvable.Component, Serializable {

    public static class Genes extends HashMap<String, GeneExpressionNode> {}
    public static class GeneRegulators extends HashMap<String, Function<Evolvable, Float>> {}

    public static class GeneExpressionNode implements Serializable {
        public static final long serialVersionUID = 1L;
        private final String geneName;
        private final Gene<?> gene;
        private final Method traitSetter;
        private final Map<String, Object> dependencies;
        private String[] dependents;
        private Object lastTraitValue;
        private Evolvable target;

        public GeneExpressionNode(String name, Gene<?> gene, Method traitSetter, String[] dependencies) {
            this.geneName = name;
            this.gene = gene;
            this.traitSetter = traitSetter;
            this.dependencies = new HashMap<>();
            for (String str : dependencies)
                if (!str.equals(""))
                    addDependency(str);
            dependents = new String[]{};
        }

        public GeneExpressionNode(String name, Gene<?> gene, Method traitSetter,
                                  Map<String, Object> dependencies, String[] dependents) {
            this.geneName = name;
            this.gene = gene;
            this.traitSetter = traitSetter;
            this.dependencies = dependencies;
            this.dependents = dependents;
        }

        public GeneExpressionNode(String name, Gene<?> gene, Method traitSetter) {
            this(name, gene, traitSetter, new String[]{});
        }

        public void addDependency(String geneName) {
            dependencies.put(geneName, null);
        }

        public void addDependent(String dependentName) {
            dependents = Arrays.copyOf(dependents, dependents.length + 1);
            dependents[dependents.length - 1] = dependentName;
        }

        public String[] getDependents() {
            return dependents;
        }

        public GeneExpressionNode copy(float mutationChance) {
            Gene<?> newGene = Simulation.RANDOM.nextFloat() < mutationChance ? gene.mutate() : gene.copy();
            return new GeneExpressionNode(geneName, newGene, traitSetter, dependencies, dependents);
        }

        public Gene<?> getGene() {
            return gene;
        }

        public void setEvolvable(Evolvable evolvable, Object traitValue) {
            this.target = evolvable;
            setTraitValue(traitValue);
        }

        public boolean setTraitValue(Object traitValue) {
            if (!traitValue.equals(lastTraitValue)) {
                Evolvable.setTraitValue(target, traitSetter, traitValue);
                lastTraitValue = traitValue;
                return true;
            }
            return false;
        }

        public Map<String, Object> getDependencies() {
            return dependencies;
        }

        public boolean mapsToTrait() {
            return target != null && traitSetter != null;
        }

        public Method getTraitSetter() {
            return traitSetter;
        }
    }

    public static final long serialVersionUID = 1L;
    private Genes genes;
    private float mutationChance = SimulationSettings.globalMutationChance;
    private NeuralNetwork geneRegulatoryNetwork;
    private GeneRegulators geneRegulators;
    private Collection<String> regulatedGenes;
    private Evolvable targetEvolvable;

    public GeneExpressionFunction(Genes genes, GeneRegulators geneRegulators) {
        this.genes = genes;
        this.geneRegulators = geneRegulators;
    }

    public GeneExpressionFunction(GeneRegulators geneRegulators) {
        this(new Genes(), geneRegulators);
    }

    @EvolvableFloat(name="Mutation Chance",
            min=SimulationSettings.minMutationChance, max=SimulationSettings.maxMutationChance)
    public void setMutationChance(float mutationChance) {
        this.mutationChance = mutationChance;
    }

    @EvolvableObject(
            name="Gene Regulatory Network",
            geneClassName="com.protoevo.biology.evolution.GeneRegulatoryNetworkGene",
            geneDependencies="Genes")
    public void setGeneRegulatoryNetwork(NetworkGenome grnGenome) {
        for (String regulator : geneRegulators.keySet())
            if (!grnGenome.hasSensor(regulator))
                grnGenome.addSensor(regulator);

        regulatedGenes = new ArrayList<>();
        for (String geneName : getGeneNames())
            if (grnGenome.hasSensor(geneName + " Input"))
                regulatedGenes.add(geneName);

        geneRegulatoryNetwork = grnGenome.phenotype();
        for (int i = 0; i < geneRegulatoryNetwork.getDepth() + 1; i++)
            tick();
    }

    @EvolvableObject(
            name="Genes",
            geneClassName="com.protoevo.biology.evolution.GenesGene"
    )
    public void setGenes(Genes genes) {
        this.genes = genes;
    }

    public NeuralNetwork getRegulatoryNetwork() {
        return geneRegulatoryNetwork;
    }

    private void setGRNInputs() {
        geneRegulatoryNetwork.setInput("Bias", 1f);
        for (String geneName : getGeneNames()) {
            if (geneRegulatoryNetwork.hasSensor(geneName + " Input")) {
                if (notDisabled(geneName)) {
                    Object geneValue = getGeneValue(geneName);
                    float value;
                    if (geneValue instanceof Float)
                        value = (float) geneValue;
                    else if (geneValue instanceof Integer)
                        value = (int) geneValue;
                    else if (geneValue instanceof Boolean)
                        value = ((boolean) geneValue) ? 1f : -1f;
                    else
                        throw new RuntimeException("Could not cast gene " + geneName + " value to float.");

                    geneRegulatoryNetwork.setInput(geneName + " Input", value);
                } else {
                    geneRegulatoryNetwork.setInput(geneName + " Input", 0);
                }
            }
        }

        if (targetEvolvable == null)
            return;

        for (String regulatorName : geneRegulators.keySet())
            if (geneRegulatoryNetwork.hasSensor(regulatorName))
                geneRegulatoryNetwork.setInput(regulatorName,
                        geneRegulators.get(regulatorName).apply(targetEvolvable));
    }

    public void setGeneRegulators(GeneRegulators geneRegulators) {
        this.geneRegulators = geneRegulators;
    }

    public void setTargetEvolvable(Evolvable evolvable, String geneName) {
        genes.get(geneName).setEvolvable(evolvable, getTraitValue(geneName));
    }
    public void setTargetEvolvable(Evolvable evolvable) {
        targetEvolvable = evolvable;
    }

    public void tick() {
        setGRNInputs();
        geneRegulatoryNetwork.tick();
        setGRNInputs();
    }

    public void update() {
        tick();
        for (String geneName : getGeneNames()) {
            GeneExpressionNode gene = genes.get(geneName);
            if (gene.mapsToTrait())
                gene.setTraitValue(getTraitValue(geneName));
        }
    }

    public void addEvolvableFloat(EvolvableFloat evolvableFloat, Method method) {
        String name = evolvableFloat.name();
        float minValue = evolvableFloat.min();
        float maxValue = evolvableFloat.max();
        FloatGene gene;
        if (evolvableFloat.randomInitialValue())
            gene = new FloatGene(name, minValue, maxValue);
        else
            gene = new FloatGene(name, minValue, maxValue, evolvableFloat.initValue());

        GeneExpressionNode node = new GeneExpressionNode(name, gene, method, evolvableFloat.geneDependencies());
        node.addDependency("Gene Regulatory Network");
        addNode(name, node);
    }

    public void addEvolvableInteger(EvolvableInteger evolvableInteger, Method method) {
        String name = evolvableInteger.name();
        int minValue = evolvableInteger.min();
        int maxValue = evolvableInteger.max();
        EvolvableInteger.MutationMethod mutMethod = evolvableInteger.mutateMethod();
        int maxInc = evolvableInteger.maxIncrement();
        boolean canDisable = evolvableInteger.canDisable();
        int disableValue = evolvableInteger.disableValue();

        IntegerGene gene;
        if (evolvableInteger.randomInitialValue())
            gene = new IntegerGene(
                    name, minValue, maxValue, mutMethod, maxInc, canDisable, disableValue, false);
        else
            gene = new IntegerGene(
                    name, minValue, maxValue, mutMethod, maxInc, canDisable, disableValue, false,
                    evolvableInteger.initValue());

        addNode(name, new GeneExpressionNode(name, gene, method, evolvableInteger.geneDependencies()));
    }

    private Gene<?> constructGene(String geneClassName, String geneName) {
        Class<Gene<?>> geneClass;
        try {
            geneClass = (Class<Gene<?>>) Class.forName(geneClassName);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to find gene class: " + e);
        }

        Constructor geneConstructor;
        try {
            geneConstructor = geneClass.getConstructor(String.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(
                    "EvolvableObject gene class " + geneClassName + " did not have " +
                    "a constructor that takes a single string (the name): " + e);
        }

        try {
            return (Gene<?>) geneConstructor.newInstance(geneName);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Failed to construct gene from " + geneClassName + ": " + e);
        }
    }

    public void addEvolvableObject(EvolvableObject evolvable, Method method) {
        String name = evolvable.name();
        String geneClassName = evolvable.geneClassName();
        Gene<?> gene = constructGene(geneClassName, name);
        if (name.equals("Genes")) {
            Genes genesGenes = (Genes) gene.getValue();
            genesGenes.putAll(genes);
            genes = genesGenes;
        }
        addNode(name, new GeneExpressionNode(name, gene, method, evolvable.geneDependencies()));
    }

    public Method getTraitSetter(String geneName) {
        return genes.get(geneName).getTraitSetter();
    }

    public Gene<?> getTraitGene(String geneName) {
        return genes.get(geneName).getGene();
    }

    public Collection<String> getGeneNames() {
        return genes.keySet();
    }

    public void merge(GeneExpressionFunction geneMapping) {
        genes.putAll(geneMapping.genes);
        geneRegulators.putAll(geneMapping.geneRegulators);
    }

    public GeneRegulators getGeneRegulators() {
        return geneRegulators;
    }

    public Collection<String> geRegulatedGenes() {
        return regulatedGenes;
    }

    public Object getGeneValue(String name) {
        Map<String, Object> deps = genes.get(name).getDependencies();
        deps.replaceAll((d, v) -> getTraitValue(d));
        return getTraitGene(name).getValue(deps);
    }

    public Object getTraitValue(String name) {
        if (hasGene(name)) {
            if (notDisabled(name) && geneRegulatoryNetwork != null
                    && geneRegulatoryNetwork.hasSensor(name + " Input")) {
                float grnOutput = geneRegulatoryNetwork.getOutput(name + " Output");
                Gene<?> gene = getTraitGene(name);
                return parseGRNOutput(gene, grnOutput);
            } else
                return getGeneValue(name);
        }
        throw new RuntimeException("Asked to get value for gene " + name + " that does not exist: " + this);
    }

    private boolean notDisabled(String name) {
        return !getTraitGene(name).isDisabled();
    }

    private Object parseGRNOutput(Gene<?> gene, float grnOutput) {
        if (gene instanceof FloatGene)
            return grnOutput;
        if (gene instanceof IntegerGene)
            return IntegerGene.fromFloat(grnOutput);
        if (gene instanceof BooleanGene)
            return BooleanGene.fromFloat(grnOutput);
        throw new RuntimeException("Could not parse GRN output: gene=" + gene + ", output=" + grnOutput);
    }

    public GeneExpressionFunction cloneWithMutation() {
        Genes newNodes = new Genes();

        for (Map.Entry<String, GeneExpressionNode> entry : genes.entrySet()) {
            GeneExpressionNode newNode = entry.getValue().copy(mutationChance);
            newNodes.put(entry.getKey(), newNode);
        }

        return new GeneExpressionFunction(newNodes, geneRegulators);
    }

    public float getMutationRate() {
        return mutationChance;
    }

    public boolean hasGene(String geneName) {
        return genes.containsKey(geneName);
    }

    public GeneExpressionNode getNode(String geneName) {
        return genes.get(geneName);
    }

    public void addNode(String geneName, GeneExpressionNode node) {
        genes.put(geneName, node);
        if (node.getGene().canDisable()) {
            String disableName = "Disable " + geneName;
            node.addDependency(disableName);
            BooleanGene disableGene = new BooleanGene(disableName, false);
            GeneExpressionNode disableNode = new GeneExpressionNode(disableName, disableGene, null);
            disableNode.addDependent(geneName);
            genes.put(disableName, disableNode);
        }
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        for (String geneName : genes.keySet()) {
            s.append(geneName).append(": ").append(genes.get(geneName)).append("\n");
        }
        return s.toString();
    }
}
