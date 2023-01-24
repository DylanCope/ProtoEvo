package com.protoevo.biology.evolution;

import com.protoevo.core.Simulation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class CollectionTrait implements Trait<List<Evolvable>> {

    private final String name;
    private final List<Evolvable> collection;
    private final Class<Evolvable> collectionType;
    private final GeneExpressionFunction geneExpressionFunction;
    private final int minSize, maxSize;
    private final static int nMutationTypes = 3;

    public CollectionTrait(
            GeneExpressionFunction geneExpressionFunction,
            Class<Evolvable> collectionType, List<Evolvable> collection,
            String name, int minSize, int maxSize) {
        this.geneExpressionFunction = geneExpressionFunction;
        this.minSize = minSize;
        this.maxSize = maxSize;
        this.collection = collection;
        this.collectionType = collectionType;
        this.name = name;
    }

    public CollectionTrait(
            GeneExpressionFunction geneExpressionFunction,
            Class<Evolvable> collectionType, String name,
            int minSize, int maxSize, int size) {
        this.geneExpressionFunction = geneExpressionFunction;
        this.minSize = minSize;
        this.maxSize = maxSize;
        this.collection = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            GeneExpressionFunction componentFn = Evolvable.createGeneMapping(collectionType);
            Evolvable.Element component = (Evolvable.Element) Evolvable.createNewComponent(collectionType, componentFn);
            component.setIndex(i);
            componentFn.prependNames(name + "/" + i);
            componentFn.registerTargetEvolvable(name + "/" + i, component);
            geneExpressionFunction.merge(componentFn);
            collection.add(component);
        }
        this.collectionType = collectionType;
        this.name = name;
    }

    @Override
    public List<Evolvable> getValue(Map<String, Object> dependencies) {
        return collection;
    }

    @Override
    public List<Evolvable> newRandomValue() {
        double p = Math.random();
        boolean removeRandom = collection.size() > minSize && p < 1.0 / nMutationTypes;
        boolean addRandom = collection.size() < maxSize && !removeRandom && p < 2.0 / nMutationTypes;

        List<Evolvable> newCollection = new ArrayList<>();

        if (addRandom)
            newCollection.add(Evolvable.createNew(collectionType));

        int removeIdx = Simulation.RANDOM.nextInt(collection.size());
        for (int i = 0; i < collection.size(); i++) {
            if (i == removeIdx && removeRandom) {
                continue;
            }
            Evolvable evolvable = collection.get(i);
            newCollection.add(Evolvable.asexualClone(evolvable));
        }

        return newCollection;
    }

    @Override
    public Trait<List<Evolvable>> createNew(List<Evolvable> value) {
        return new CollectionTrait(
                geneExpressionFunction, collectionType, value, name, minSize, maxSize);
    }

    @Override
    public String getTraitName() {
        return "Evolvable Collection: " + collectionType.getSimpleName();
    }
}