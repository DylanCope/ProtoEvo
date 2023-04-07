package com.protoevo.biology.evolution;

import com.badlogic.gdx.math.MathUtils;
import com.protoevo.biology.nn.NetworkGenome;
import com.protoevo.biology.nn.NeuralNetwork;
import com.protoevo.biology.nn.NeuronGene;
import com.protoevo.biology.nn.SynapseGene;
import com.protoevo.utils.Utils;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;


public class GeneExpressionFunction implements Evolvable.Component, Serializable {
    public static class ExpressionNodes extends HashMap<String, ExpressionNode> {}
    public static class Regulators extends HashMap<String, RegulationNode> {}


    public static abstract class Node {
        public final long id = Utils.randomLong();

        public long getID() {
            return id;
        }

        public abstract Object getLastTarget();
        public abstract String getDisplayName();
    }

    public static class RegulationNode extends Node implements Serializable {
        public static final long serialVersionUID = 1L;
        public String name;
        private transient Function<Evolvable, Float> regulatorGetter;
        private final String methodGetterName;
        private String targetID;
        private Object lastTarget;

        public RegulationNode(RegulationNode other) {
            this.name = other.name;
            this.regulatorGetter = other.regulatorGetter;
            this.methodGetterName = other.methodGetterName;
            this.targetID = other.targetID;
            this.lastTarget = null;
        }

        private Function<Evolvable, Float> createGetter(Method method) {
            String regulatorName = method.getAnnotation(GeneRegulator.class).name();
            float max = method.getAnnotation(GeneRegulator.class).max();
            float min = method.getAnnotation(GeneRegulator.class).min();
            return evolvable -> {
                try {
                    return 2f * ((Float) method.invoke(evolvable) - min) / (max - min) - 1f;
                } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException e) {
                    throw new RuntimeException(
                        "Failed to get value for gene regulator "
                        + regulatorName + "(" + method.getName() + ":" + method.getDeclaringClass() + ")", e);
                }
            };
        }

        public RegulationNode(String name, Method getterMethod) {
            this.name = name;
            this.regulatorGetter = createGetter(getterMethod);
            this.methodGetterName = getterMethod.getName();
        }

        public RegulationNode(String name, Function<Evolvable, Float> regulatorGetter,
                              String targetID, String methodGetterName) {
            this.name = name;
            this.regulatorGetter = regulatorGetter;
            this.methodGetterName = methodGetterName;
            this.targetID = targetID;
        }

        public void setTargetID(Evolvable target) {
            this.targetID = target.name();
        }

        public float getValue(Evolvable evolvable) {
            lastTarget = evolvable;
            if (regulatorGetter == null) {
                for (Method method : evolvable.getClass().getMethods())
                    if (method.getName().equals(methodGetterName)) {
                        regulatorGetter = createGetter(method);
                        break;
                    }

                if (regulatorGetter == null)
                    throw new RuntimeException(
                        "Failed to find method " + methodGetterName + " in " + evolvable.getClass());
            }
            return regulatorGetter.apply(evolvable);
        }

        @Override
        public Object getLastTarget() {
            return lastTarget;
        }

        public Function<Evolvable, Float> getGetter() {
            return regulatorGetter;
        }

        public RegulationNode copy() {
//            return new RegulationNode(name, regulatorGetter, targetID, methodGetterName);
            return new RegulationNode(this);
        }

        public String getName() {
            return name;
        }

        public String getDisplayName() {
            return name;
        }
    }

    public static class ExpressionNode extends Node implements Serializable {
        public static final long serialVersionUID = 1L;
        private String name;
        private Trait<?> trait;
        private transient Method traitSetter;
        private final String methodName;
        private final Map<String, Object> dependencies;
        private String[] dependents;
        private Object lastTraitValue;
        private String targetID;
        private Object lastTarget;

        public ExpressionNode(String name, Trait<?> trait, Method traitSetter, String[] dependencies) {
            this.name = name;
            this.trait = trait;
            this.traitSetter = traitSetter;
            this.methodName = traitSetter != null ? traitSetter.getName() : null;
            this.dependencies = new HashMap<>();
            for (String str : dependencies)
                if (!str.equals(""))
                    addDependency(str);
            dependents = new String[]{};
        }

        public ExpressionNode(String name, Trait<?> trait, Method traitSetter, String targetID,
                              Map<String, Object> dependencies, String[] dependents) {
            this.name = name;
            this.trait = trait;
            this.traitSetter = traitSetter;
            this.methodName = traitSetter != null ? traitSetter.getName() : null;
            this.targetID = targetID;
            this.dependencies = dependencies;
            this.dependents = dependents;
        }

        public ExpressionNode(String name, Trait<?> trait, Method traitSetter) {
            this(name, trait, traitSetter, new String[]{});
        }

        public ExpressionNode(ExpressionNode other) {
            this.name = other.name;
            this.trait = other.trait.copy();
            this.traitSetter = other.traitSetter;
            this.methodName = other.methodName;
            this.dependencies = new HashMap<>();
            for (String str : other.dependencies.keySet())
                if (!str.equals(""))
                    addDependency(str);
            this.dependents = other.dependents;
            this.targetID = other.targetID;
            this.lastTarget = null;
            this.lastTraitValue = null;
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

        public ExpressionNode cloneWithMutation() {
            ExpressionNode newNode = copy();
            newNode.trait = trait.cloneWithMutation();
            return newNode;
        }

        public ExpressionNode copy() {
//            return new ExpressionNode(
//                    name, trait.copy(), traitSetter, targetID, dependencies, dependents);
            return new ExpressionNode(this);
        }

        public Trait<?> getTrait() {
            return trait;
        }

        public void setTargetEvolvable(String id) {
            this.targetID = id;
        }

        public String getTargetID() {
            return targetID;
        }

        public boolean acceptsEvolvable(Class<? extends Evolvable> evolvableType) {
            return mapsToTrait() && getTraitSetter().getDeclaringClass().isAssignableFrom(evolvableType);
        }

        public void setTraitValue(Evolvable target, Object traitValue) {
            Method setter = getTraitSetter(target);
            if (!traitValue.equals(lastTraitValue) && setter != null) {
                Evolvable.setTraitValue(target, setter, traitValue);
                lastTraitValue = traitValue;
                lastTarget = target;
            }
        }

        @Override
        public Object getLastTarget() {
            return lastTarget;
        }

        @Override
        public String getDisplayName() {
            return getTrait().getTraitName();
        }

        public String getName() {
            return name;
        }

        public Map<String, Object> getDependencies() {
            return dependencies;
        }

        public boolean mapsToTrait() {
            return methodName != null;
        }

        public Method getTraitSetter() {
            return getTraitSetter(lastTarget);
        }

        public Method getTraitSetter(Object target) {
            if (traitSetter != null)
                return traitSetter;

            if (target == null || methodName == null)
                return null;

            // this is to handle transient method field.
            // if the node was serialized and rebuilt, the method field will be null.
            for (Method method : target.getClass().getMethods()) {
                if (method.getName().equals(methodName)) {
                    traitSetter = method;
                    return method;
                }
            }

            return null;
        }

        public void prependName(String name) {
            this.name = name + "/" + this.name;
        }
    }

    public static final long serialVersionUID = 1L;
    private ExpressionNodes expressionNodes;
    private NetworkGenome grnGenome;
    private NeuralNetwork geneRegulatoryNetwork;
    private Regulators regulators;
    private Collection<String> regulatedTraits = new ArrayList<>();
    private final Map<String, Evolvable> targetMap = new HashMap<>();

    public GeneExpressionFunction(ExpressionNodes expressionNodes, Regulators regulators) {
        this.expressionNodes = expressionNodes;
        this.regulators = regulators;
    }

    public GeneExpressionFunction(Regulators regulators) {
        this(new ExpressionNodes(), regulators);
    }

    public void buildGeneRegulatoryNetwork() {
        grnGenome = GRNFactory.createNetworkGenome(this);
        geneRegulatoryNetwork = grnGenome.phenotype();

        for (int i = 0; i < geneRegulatoryNetwork.getDepth() + 1; i++)
            tick();
    }

    @Override
    public void build() {
        Component.super.build();
        buildGeneRegulatoryNetwork();
    }

    public void setGRNGenome(NetworkGenome genome) {
        grnGenome = genome;
    }

    public NeuralNetwork getRegulatoryNetwork() {
        return geneRegulatoryNetwork;
    }

    private void setGRNInputs() {
        geneRegulatoryNetwork.setInput("Bias", 1f);
        geneRegulatoryNetwork.setInput("Random Source", MathUtils.random(-1f, 1f));
        for (String geneName : getTraitNames()) {
            String inputName = GRNFactory.getInputName(geneName);
            if (geneRegulatoryNetwork.hasSensor(inputName)) {
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

                    geneRegulatoryNetwork.setInput(inputName, value);
                } else {
                    geneRegulatoryNetwork.setInput(inputName, 0);
                }
            }
        }

        for (String regulatorName : regulators.keySet()) {
            RegulationNode node = regulators.get(regulatorName);
            if (!targetMap.containsKey(node.targetID))
                continue;

            Evolvable target = targetMap.get(node.targetID);
            if (geneRegulatoryNetwork.hasSensor(regulatorName))
                geneRegulatoryNetwork.setInput(regulatorName, node.getValue(target));
        }
    }

    public void setGeneRegulators(Regulators regulators) {
        this.regulators = regulators;
    }

    public void registerTargetEvolvable(String id, Evolvable evolvable) {
        targetMap.put(id, evolvable);

        for (String trait : getTraitNames()) {
            ExpressionNode node = expressionNodes.get(trait);

            // only register to node if it is not already registered to another evolvable
            if (node.getTargetID() == null && node.acceptsEvolvable(evolvable.getClass())) {
                node.setTargetEvolvable(id);
                node.setTraitValue(evolvable, getTraitValue(trait));
            }
        }

        for (String regulatorName : regulators.keySet()) {
            RegulationNode node = regulators.get(regulatorName);
            if (node.targetID == null)
                node.targetID = id;
        }
    }

    public void tick() {
        if (geneRegulatoryNetwork == null)
            return;
        setGRNInputs();
        geneRegulatoryNetwork.tick();
        setGRNInputs();
    }

    public void update() {
        tick();
        for (String trait : getTraitNames()) {
            ExpressionNode node = expressionNodes.get(trait);
            if (node.mapsToTrait() && targetMap.containsKey(node.getTargetID()))
                node.setTraitValue(targetMap.get(node.getTargetID()), getTraitValue(trait));
        }
    }

    public void addControlVariable(ControlVariable controlVariable, Method method) {
        String name = controlVariable.name();
        float minValue = controlVariable.min();
        float maxValue = controlVariable.max();
        ControlTrait trait = new ControlTrait(name, minValue, maxValue);
        trait.setGeneExpressionFunction(this);
        trait.init();
        ExpressionNode node = new ExpressionNode(name, trait, method);
        addNode(name, node);
    }

    public void addEvolvableFloat(EvolvableFloat evolvableFloat, Method method) {
        String name = evolvableFloat.name();
        float minValue = evolvableFloat.min();
        float maxValue = evolvableFloat.max();
        FloatTrait trait;
        if (evolvableFloat.randomInitialValue())
            trait = new FloatTrait(name, minValue, maxValue);
        else
            trait = new FloatTrait(name, minValue, maxValue, evolvableFloat.initValue());

        trait.setRegulated(evolvableFloat.regulated());
        trait.setGeneExpressionFunction(this);
        trait.init();
        ExpressionNode node = new ExpressionNode(name, trait, method, evolvableFloat.geneDependencies());
        addNode(name, node);
    }

    public void addEvolvableInteger(EvolvableInteger evolvableInteger, Method method) {
        String name = evolvableInteger.name();

        IntegerTrait trait;
        if (evolvableInteger.randomInitialValue())
            trait = new IntegerTrait(evolvableInteger);
        else
            trait = new IntegerTrait(evolvableInteger, evolvableInteger.initValue());

        trait.setGeneExpressionFunction(this);
        trait.init();
        addNode(name, new ExpressionNode(name, trait, method, evolvableInteger.geneDependencies()));
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
        trait.setGeneExpressionFunction(this);
        trait.init();
        addNode(name, new ExpressionNode(name, trait, method, evolvable.dependencies()));
    }

    public void addEvolvableCollection(GeneExpressionFunction geneExpressionFunction,
                                       EvolvableList evolvable,
                                       Method method) {
        String name = evolvable.name();
        int minSize = evolvable.minSize();
        int maxSize = evolvable.maxSize();
        int initialSize = evolvable.initialSize();

        Class<Evolvable.Element> elementClass;
        try {
            elementClass = (Class<Evolvable.Element>) Class.forName(evolvable.elementClassPath());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to find class for evolvable collection: " + e);
        } catch (ClassCastException e) {
            throw new RuntimeException("EvolvableCollection element class path " + evolvable.elementClassPath() +
                    " did not refer to a class that implements Evolvable: " + e);
        }

        CollectionTrait trait = new CollectionTrait(
                geneExpressionFunction, elementClass, name, minSize, maxSize, initialSize);
        trait.setGeneExpressionFunction(this);
        trait.init();
        addNode(name, new ExpressionNode(name, trait, method, evolvable.geneDependencies()));
    }

    public Method getTraitSetter(String geneName) {
        return expressionNodes.get(geneName).getTraitSetter();
    }

    public Trait<?> getTraitGene(String geneName) {
        return expressionNodes.get(geneName).getTrait();
    }

    public Collection<String> getTraitNames() {
        return expressionNodes.keySet();
    }

    public void merge(GeneExpressionFunction other) {
        other.expressionNodes.forEach(expressionNodes::putIfAbsent);
        other.regulators.forEach(regulators::putIfAbsent);
        other.targetMap.forEach(targetMap::putIfAbsent);
    }

    public Regulators getGeneRegulators() {
        return regulators;
    }

    public Collection<String> getRegulatedTraits() {
        return regulatedTraits;
    }

    public Object getGeneValue(String name) {
        Map<String, Object> deps = expressionNodes.get(name).getDependencies();
        deps.replaceAll((d, v) -> getTraitValue(d));
        return getTraitGene(name).getValue(deps);
    }

    public Object getTraitValue(String name) {
        if (hasGene(name)) {
            if (notDisabled(name) && geneRegulatoryNetwork != null
                    && geneRegulatoryNetwork.hasOutput(GRNFactory.getOutputName(name))) {
                float grnOutput = geneRegulatoryNetwork.getOutput(GRNFactory.getOutputName(name));
                Trait<?> trait = getTraitGene(name);
                return parseGRNOutput(trait, grnOutput);
            } else
                return getGeneValue(name);
        }
        throw new RuntimeException("Asked to get value for trait " + name + " that does not exist: " + this);
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
        ExpressionNodes newNodes = new ExpressionNodes();
        Regulators newRegulators = new Regulators();
        GeneExpressionFunction newFn = new GeneExpressionFunction(newNodes, newRegulators);

        for (Map.Entry<String, ExpressionNode> entry : expressionNodes.entrySet()) {
            ExpressionNode newNode = entry.getValue().cloneWithMutation();
            newNode.trait.setGeneExpressionFunction(newFn);
            newNodes.put(entry.getKey(), newNode);
        }

        regulators.forEach(
                (regulator, regulationNode) -> newRegulators.put(regulator, regulationNode.copy()));

        if (grnGenome != null) {
            newFn.grnGenome = GRNFactory.createIO(new NetworkGenome(grnGenome), newFn);
            newFn.grnGenome.mutate();
            newFn.geneRegulatoryNetwork = newFn.grnGenome.phenotype();
        }

        return newFn;
    }

    public GeneExpressionFunction copy() {
        ExpressionNodes newNodes = new ExpressionNodes();
        Regulators newRegulators = new Regulators();
        GeneExpressionFunction newFn = new GeneExpressionFunction(newNodes, newRegulators);

        for (Map.Entry<String, ExpressionNode> entry : expressionNodes.entrySet()) {
            ExpressionNode newNode = entry.getValue().copy();
            newNode.trait.setGeneExpressionFunction(newFn);
            newNodes.put(entry.getKey(), newNode);
        }

        regulators.forEach(
                (regulator, regulationNode) -> newRegulators.put(regulator, regulationNode.copy()));

        if (grnGenome != null) {
            newFn.grnGenome = new NetworkGenome(grnGenome);
            newFn.geneRegulatoryNetwork = newFn.grnGenome.phenotype();
        }

        return newFn;
    }

    public float getMeanMutationRate() {
        int count = 0;
        float sum = 0;
        for (ExpressionNode node : expressionNodes.values()) {
            sum += node.getTrait().getMutationRate();
            count++;
        }
        if (grnGenome != null) {
            for (Iterator<NeuronGene> it = grnGenome.iterateNeuronGenes(); it.hasNext(); ) {
                NeuronGene gene = it.next();
                sum += gene.getMutationRate();
                count++;
            }
            for (Iterator<SynapseGene> it = grnGenome.iterateSynapseGenes(); it.hasNext(); ) {
                SynapseGene gene = it.next();
                sum += gene.getMutationRate();
                count++;
            }
        }
        return sum / count;
    }

    public int getMutationCount() {
        int mutationCount = grnGenome.getMutationCount();
        for (ExpressionNode node : expressionNodes.values()) {
            mutationCount += node.getTrait().getMutationCount();
        }
        return mutationCount;
    }

    public boolean hasGene(String geneName) {
        return expressionNodes.containsKey(geneName);
    }

    public ExpressionNode getNode(String geneName) {
        return expressionNodes.get(geneName);
    }

    public void addNode(String geneName, ExpressionNode node) {
        expressionNodes.put(geneName, node);
        if (node.getTrait().canDisable()) {
            String disableName = "Disable " + geneName;
            node.addDependency(disableName);
            BooleanTrait disableGene = new BooleanTrait(disableName, false);
            ExpressionNode disableNode = new ExpressionNode(disableName, disableGene, null);
            disableNode.addDependent(geneName);
            expressionNodes.put(disableName, disableNode);
        }
    }

    public void prependNames(String name) {
        ExpressionNodes newNodes = new ExpressionNodes();
        for (Map.Entry<String, ExpressionNode> entry : expressionNodes.entrySet()) {
            ExpressionNode node = entry.getValue();
            String newName = name + "/" + entry.getKey();
            newNodes.put(newName, node);

            node.prependName(name);
            for (String dependent : node.getDependents()) {
                Map<String, Object> dependentsDependencies = getNode(dependent).getDependencies();
                dependentsDependencies.put(
                        name + "/" + node.name, dependentsDependencies.get(node.name)
                );
            }
        }
        expressionNodes = newNodes;

        Regulators newRegulators = new Regulators();
        for (String regulator : regulators.keySet()) {
            RegulationNode node = regulators.get(regulator);
            node.name = name + "/" + node.name;
            newRegulators.put(name + "/" + regulator, node);
        }
        regulators = newRegulators;

        List<String> newRegulatedTraits = new ArrayList<>();
        for (String regulatedTrait : regulatedTraits) {
            newRegulatedTraits.add(name + "/" + regulatedTrait);
        }
        regulatedTraits = newRegulatedTraits;
    }

    public ExpressionNodes getGenes() {
        return expressionNodes;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        for (String geneName : expressionNodes.keySet()) {
            s.append(geneName).append(": ").append(expressionNodes.get(geneName)).append("\n");
        }
        return s.toString();
    }

    public ExpressionNode getExpressionNode(String name) {
        if (expressionNodes.containsKey(name))
            return expressionNodes.get(name);
        return null;
    }

    public NetworkGenome getGRNGenome() {
        return grnGenome;
    }

}
