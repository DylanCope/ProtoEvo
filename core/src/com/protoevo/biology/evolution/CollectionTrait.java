package com.protoevo.biology.evolution;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.protoevo.core.Simulation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@JsonIdentityInfo(
        generator = ObjectIdGenerators.PropertyGenerator.class,
        property = "id")
public class CollectionTrait implements Trait<List<Evolvable>> {
    public int id;

    private final String name;
    private final List<Evolvable> collection;
    private final Class<Evolvable.Element> collectionType;
    private GeneExpressionFunction geneExpressionFunction;
    private final int minSize, maxSize;
    private final static int nMutationTypes = 3;
    private float mutationRate;

    public CollectionTrait(
            GeneExpressionFunction geneExpressionFunction,
            Class<Evolvable.Element> collectionType, List<Evolvable> collection,
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
            Class<Evolvable.Element> collectionType, String name,
            int minSize, int maxSize, int size) {
        this.geneExpressionFunction = geneExpressionFunction;
        this.minSize = minSize;
        this.maxSize = maxSize;
        this.collectionType = collectionType;
        this.name = name;

        this.collection = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            Evolvable.Element element = createNewElement(i);
            collection.add(element);
            GeneExpressionFunction elementFn = element.getGeneExpressionFunction();
            geneExpressionFunction.merge(elementFn);
        }
    }

    private Evolvable.Element createNewElement(int index) {
        GeneExpressionFunction elementFn = Evolvable.createGeneMapping(collectionType);
        return Evolvable.createNewElement(name, index, collectionType, elementFn);
    }

    @Override
    public void setGeneExpressionFunction(GeneExpressionFunction fn) {
        geneExpressionFunction = fn;
        for (Evolvable e : collection) {
            geneExpressionFunction.merge(e.getGeneExpressionFunction());
        }
    }

    @Override
    public GeneExpressionFunction getGeneExpressionFunction() {
        return geneExpressionFunction;
    }

    @Override
    public List<Evolvable> getValue(Map<String, Object> dependencies) {
        return collection;
    }

    @Override
    public void setMutationRate(float rate) {
        this.mutationRate = rate;
    }

    @Override
    public float getMutationRate() {
        return mutationRate;
    }

    @Override
    public List<Evolvable> newRandomValue() {
        double p = Math.random();
        boolean removeRandom = collection.size() > minSize && p < 1.0 / nMutationTypes;
        boolean addRandom = collection.size() < maxSize && !removeRandom && p < 2.0 / nMutationTypes;

        List<Evolvable> newCollection = new ArrayList<>();

        int removeIdx = removeRandom ? Simulation.RANDOM.nextInt(collection.size()) : -1;
        for (int i = 0; i < collection.size(); i++) {
            if (i == removeIdx && removeRandom) {
                continue;
            }
            Evolvable evolvable = collection.get(i);
            Evolvable clone = Evolvable.asexualClone(evolvable);
            newCollection.add(clone);
        }

        if (addRandom)
            newCollection.add(createNewElement(newCollection.size()));

        return newCollection;
    }

    @Override
    public Trait<List<Evolvable>> createNew(List<Evolvable> value) {
        return new CollectionTrait(
                geneExpressionFunction, collectionType, value, name, minSize, maxSize);
    }

    private List<Evolvable> copyCollection() {
        List<Evolvable> newCollection = new ArrayList<>();
        for (Evolvable e : collection)
            newCollection.add(Evolvable.asexualClone(e));
        return newCollection;
    }

    public Trait<List<Evolvable>> copy() {
        return createNew(copyCollection());
    }

    @Override
    public String getTraitName() {
        return "Evolvable Collection: " + collectionType.getSimpleName();
    }
}