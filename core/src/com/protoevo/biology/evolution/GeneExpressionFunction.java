package com.protoevo.biology.evolution;

import com.protoevo.biology.neat.NetworkGenome;
import com.protoevo.biology.neat.NeuralNetwork;
import com.protoevo.core.Simulation;
import com.protoevo.core.settings.SimulationSettings;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;

public class GeneExpressionFunction implements Evolvable.Component, Serializable {

    public static class Genes extends HashMap<String, ExpressionNode> {}
    public static class GeneRegulators extends HashMap<String, Function<Evolvable, Float>> {}

    public static class ExpressionNode implements Serializable {
        public static final long serialVersionUID = 1L;
        private String name;
        private final Trait<?> trait;
        private final Method traitSetter;
        private final Map<String, Object> dependencies;
        private String[] dependents;
        private Object lastTraitValue;
        private Evolvable target;

        public ExpressionNode(String name, Trait<?> trait, Method traitSetter, String[] dependencies) {
            this.name = name;
            this.trait = trait;
            this.traitSetter = traitSetter;
            this.dependencies = new HashMap<>();
            for (String str : dependencies)
                if (!str.equals(""))
                    addDependency(str);
            dependents = new String[]{};
        }

        public ExpressionNode(String name, Trait<?> trait, Method traitSetter,
                              Map<String, Object> dependencies, String[] dependents) {
            this.name = name;
            this.trait = trait;
            this.traitSetter = traitSetter;
            this.dependencies = dependencies;
            this.dependents = dependents;
        }

        public ExpressionNode(String name, Trait<?> trait, Method traitSetter) {
            this(name, trait, traitSetter, new String[]{});
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

        public ExpressionNode copy(float mutationChance) {
            Trait<?> newTrait = Simulation.RANDOM.nextFloat() < mutationChance ? trait.mutate() : trait.copy();
            return new ExpressionNode(name, newTrait, traitSetter, dependencies, dependents);
        }

        public Trait<?> getTrait() {
            return trait;
        }

        public void setEvolvable(Evolvable evolvable, Object traitValue) {
            this.target = evolvable;
            setTraitValue(traitValue);
        }

        public void setTraitValue(Object traitValue) {
            if (!traitValue.equals(lastTraitValue)) {
                Evolvable.setTraitValue(target, traitSetter, traitValue);
                lastTraitValue = traitValue;
            }
        }

        public String getName() {
            return name;
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

        public void prependName(String name) {
            this.name = name + "/" + this.name;
        }
    }

    public static final long serialVersionUID = 1L;
    private Genes genes;
    private float mutationChance = SimulationSettings.globalMutationChance;
    private NeuralNetwork geneRegulatoryNetwork;
    private GeneRegulators geneRegulators;
    private Collection<String> regulatedGenes = new ArrayList<>();
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

    public static final String GENES_TRAIT_NAME = "GeneExpressionFunction/Genes";
    public static final String GRN_TRAIT_NAME = "GeneExpressionFunction/Gene Regulatory Network";

    @EvolvableObject(
            name="Gene Regulatory Network",
            traitClass = "com.protoevo.biology.evolution.GeneRegulatoryNetworkTrait",
            dependencies = GENES_TRAIT_NAME)
    public void setGeneRegulatoryNetwork(NetworkGenome grnGenome) {
//        for (String regulator : geneRegulators.keySet())
//            if (!grnGenome.hasSensor(regulator))
//                grnGenome.addSensor(regulator);

        regulatedGenes.clear();
        for (String traitName : getTraitNames()) {
            if (grnGenome.hasSensor(GeneRegulatoryNetworkTrait.getInputName(traitName)))
                regulatedGenes.add(traitName);
        }

        geneRegulatoryNetwork = grnGenome.phenotype();

        for (int i = 0; i < geneRegulatoryNetwork.getDepth() + 1; i++)
            tick();
    }

    @EvolvableObject(
            name="Genes",
            traitClass ="com.protoevo.biology.evolution.GenesTrait"
    )
    public void setGenes(Genes genes) {
        this.genes = genes;
    }

    public NeuralNetwork getRegulatoryNetwork() {
        return geneRegulatoryNetwork;
    }

    private void setGRNInputs() {
        geneRegulatoryNetwork.setInput("Bias", 1f);
        for (String geneName : getTraitNames()) {
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
        for (String geneName : getTraitNames()) {
            ExpressionNode gene = genes.get(geneName);
            if (gene.mapsToTrait())
                gene.setTraitValue(getTraitValue(geneName));
        }
    }

    public void addRegulatedFloat(RegulatedFloat regulatedFloat, Method method) {
        String name = method.getDeclaringClass().getSimpleName() + "/" + regulatedFloat.name();
        float minValue = regulatedFloat.min();
        float maxValue = regulatedFloat.max();
        RegulatedFloatTrait trait = new RegulatedFloatTrait(name, minValue, maxValue);
        ExpressionNode node = new ExpressionNode(name, trait, method);
        addNode(name, node);
    }

    public void addEvolvableFloat(EvolvableFloat evolvableFloat, Method method) {
        String name = method.getDeclaringClass().getSimpleName() + "/" + evolvableFloat.name();
        float minValue = evolvableFloat.min();
        float maxValue = evolvableFloat.max();
        FloatTrait trait;
        if (evolvableFloat.randomInitialValue())
            trait = new FloatTrait(name, minValue, maxValue);
        else
            trait = new FloatTrait(name, minValue, maxValue, evolvableFloat.initValue());

        ExpressionNode node = new ExpressionNode(name, trait, method, evolvableFloat.geneDependencies());
        addNode(name, node);
    }

    public void addEvolvableInteger(EvolvableInteger evolvableInteger, Method method) {
        String name = method.getDeclaringClass().getSimpleName() + "/" + evolvableInteger.name();
        int minValue = evolvableInteger.min();
        int maxValue = evolvableInteger.max();
        EvolvableInteger.MutationMethod mutMethod = evolvableInteger.mutateMethod();
        int maxInc = evolvableInteger.maxIncrement();
        boolean canDisable = evolvableInteger.canDisable();
        int disableValue = evolvableInteger.disableValue();

        IntegerTrait gene;
        if (evolvableInteger.randomInitialValue())
            gene = new IntegerTrait(
                    name, minValue, maxValue, mutMethod, maxInc, canDisable, disableValue, false);
        else
            gene = new IntegerTrait(
                    name, minValue, maxValue, mutMethod, maxInc, canDisable, disableValue, false,
                    evolvableInteger.initValue());

        addNode(name, new ExpressionNode(name, gene, method, evolvableInteger.geneDependencies()));
    }

    private Trait<?> constructTrait(String className, String traitName) {
        Class<Trait<?>> traitClass;
        try {
            traitClass = (Class<Trait<?>>) Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to find trait class: " + e);
        } catch (ClassCastException e) {
            throw new RuntimeException("Class is not a trait: " + e);
        }

        Constructor<Trait<?>> traitConstructor;
        try {
            traitConstructor = traitClass.getConstructor(String.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(
                    "EvolvableObject gene class " + className + " did not have " +
                    "a constructor that takes a single string (the name): " + e);
        }

        try {
            return traitConstructor.newInstance(traitName);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Failed to construct trait from " + className + ": " + e);
        }
    }

    public void addEvolvableObject(EvolvableObject evolvable, Method method) {
        String name = method.getDeclaringClass().getSimpleName() + "/" + evolvable.name();
        String geneClassName = evolvable.traitClass();
        Trait<?> trait = constructTrait(geneClassName, name);
        if (name.equals(GENES_TRAIT_NAME)) {
            Genes genesGenes = (Genes) trait.getValue();
            genesGenes.putAll(genes);
            genes = genesGenes;
        }
        addNode(name, new ExpressionNode(name, trait, method, evolvable.dependencies()));
    }

    public void addEvolvableCollection(GeneExpressionFunction geneExpressionFunction,
                                       EvolvableCollection evolvable,
                                       Method method) {
        String name = method.getDeclaringClass().getSimpleName() + "/" + evolvable.name();
        int minSize = evolvable.minSize();
        int maxSize = evolvable.maxSize();
        int initialSize = evolvable.initialSize();

        Class<Evolvable> elementClass;
        try {
            elementClass = (Class<Evolvable>) Class.forName(evolvable.elementClassPath());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to find class for evolvable collection: " + e);
        } catch (ClassCastException e) {
            throw new RuntimeException("EvolvableCollection element class path " + evolvable.elementClassPath() +
                    " did not refer to a class that implements Evolvable: " + e);
        }

        CollectionTrait trait = new CollectionTrait(
                geneExpressionFunction, elementClass, name, minSize, maxSize, initialSize);
        addNode(name, new ExpressionNode(name, trait, method, evolvable.geneDependencies()));
    }

    public Method getTraitSetter(String geneName) {
        return genes.get(geneName).getTraitSetter();
    }

    public Trait<?> getTraitGene(String geneName) {
        return genes.get(geneName).getTrait();
    }

    public Collection<String> getTraitNames() {
        return genes.keySet();
    }

    public void merge(GeneExpressionFunction geneMapping) {
        genes.putAll(geneMapping.genes);
        geneRegulators.putAll(geneMapping.geneRegulators);
    }

    public GeneRegulators getGeneRegulators() {
        return geneRegulators;
    }

    public Collection<String> getRegulatedGenes() {
        return regulatedGenes;
    }

    public Object getGeneValue(String name) {
        Map<String, Object> deps = genes.get(name).getDependencies();
        deps.replaceAll((d, v) -> getTraitValue(d));
        deps.put("Gene Regulators", geneRegulators);
        return getTraitGene(name).getValue(deps);
    }

    public Object getTraitValue(String name) {
        if (name.equals("Gene Regulators"))
            return geneRegulators;

        if (hasGene(name)) {
            if (notDisabled(name) && geneRegulatoryNetwork != null
                    && geneRegulatoryNetwork.hasSensor(name + " Input")) {
                float grnOutput = geneRegulatoryNetwork.getOutput(name + " Output");
                Trait<?> trait = getTraitGene(name);
                return parseGRNOutput(trait, grnOutput);
            } else
                return getGeneValue(name);
        }
        throw new RuntimeException("Asked to get value for gene " + name + " that does not exist: " + this);
    }

    private boolean notDisabled(String name) {
        return !getTraitGene(name).isDisabled();
    }

    private Object parseGRNOutput(Trait<?> trait, float grnOutput) {
        if (trait instanceof FloatTrait)
            return grnOutput;
        if (trait instanceof IntegerTrait)
            return IntegerTrait.fromFloat(grnOutput);
        if (trait instanceof BooleanTrait)
            return BooleanTrait.fromFloat(grnOutput);
        throw new RuntimeException("Could not parse GRN output: gene=" + trait + ", output=" + grnOutput);
    }

    public GeneExpressionFunction cloneWithMutation() {
        Genes newNodes = new Genes();

        for (Map.Entry<String, ExpressionNode> entry : genes.entrySet()) {
            ExpressionNode newNode = entry.getValue().copy(mutationChance);
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

    public ExpressionNode getNode(String geneName) {
        return genes.get(geneName);
    }

    public void addNode(String geneName, ExpressionNode node) {
        genes.put(geneName, node);
        if (node.getTrait().canDisable()) {
            String disableName = "Disable " + geneName;
            node.addDependency(disableName);
            BooleanTrait disableGene = new BooleanTrait(disableName, false);
            ExpressionNode disableNode = new ExpressionNode(disableName, disableGene, null);
            disableNode.addDependent(geneName);
            genes.put(disableName, disableNode);
        }
    }

    public void prependGeneNames(String name) {
        for (ExpressionNode node : genes.values()) {
            node.prependName(name);
            for (String dependent : node.getDependents()) {
                Map<String, Object> dependentsDependencies = getNode(dependent).getDependencies();
                dependentsDependencies.put(
                        name + "/" + node.name, dependentsDependencies.get(node.name)
                );
            }
        }
        for (int i = 0; i < genes.entrySet().size(); i++) {
            Map.Entry<String, ExpressionNode> entry = genes.entrySet().iterator().next();
            genes.put(name + "/" + entry.getKey(), entry.getValue());
            genes.remove(entry.getKey());
        }

        Iterator<String> regulatedGenesIterator = regulatedGenes.iterator();
        for (int i = 0; i < regulatedGenes.size(); i++) {
            String regulatedGene = regulatedGenesIterator.next();
            regulatedGenes.remove(regulatedGene);
            regulatedGenes.add(name + "/" + regulatedGene);
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
