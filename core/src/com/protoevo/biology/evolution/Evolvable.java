package com.protoevo.biology.evolution;

import com.protoevo.biology.nn.NetworkGenome;
import com.protoevo.core.Simulation;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

public interface Evolvable extends Serializable {

    void setGeneExpressionFunction(GeneExpressionFunction fn);
    GeneExpressionFunction getGeneExpressionFunction();

    default void build() {}

    default String name() {
        return this.getClass().getSimpleName();
    }

    interface Component extends Evolvable {
        default void setGeneExpressionFunction(GeneExpressionFunction fn) {}
        default GeneExpressionFunction getGeneExpressionFunction() {
            return null;
        };
    }

    interface Element extends Evolvable {
        void setIndex(int index);
        int getIndex();

        default String getCollectionName() {
            // Collection name has been prepended to element gene expression function names
            return this.getGeneExpressionFunction().getGenes().keySet().stream()
                    .findAny().map(s -> s.split("/")[0]).orElse("");
        }

        @Override
        default String name() {
            return this.getClass().getSimpleName() + "/" + getIndex();
        }
    }

    static <T extends Evolvable> T asexualClone(T evolvable) {
        GeneExpressionFunction fn = evolvable.getGeneExpressionFunction().cloneWithMutation();
        Supplier<T> constructor = (Supplier<T>) createEvolvableConstructor(evolvable.getClass(), fn);
        T newEvolvable = constructor.get();
        newEvolvable.setGeneExpressionFunction(fn);
        if (evolvable instanceof Evolvable.Element)
            ((Evolvable.Element) newEvolvable).setIndex(((Evolvable.Element) evolvable).getIndex());
        fn.registerTargetEvolvable(newEvolvable.name(), newEvolvable);
        fn.update();
        newEvolvable.build();
        return newEvolvable;
    }

    static <T extends Evolvable> T createChild(
            Class<T> clazz,
            GeneExpressionFunction parent1Genome,
            GeneExpressionFunction parent2Genome
    ) {
        Set<String> allGeneNames = new HashSet<>();
        allGeneNames.addAll(parent1Genome.getTraitNames());
        allGeneNames.addAll(parent2Genome.getTraitNames());

        GeneExpressionFunction.Regulators regulators = new GeneExpressionFunction.Regulators();
        regulators.putAll(parent1Genome.getGeneRegulators());
        regulators.putAll(parent2Genome.getGeneRegulators());
        GeneExpressionFunction childGenome = new GeneExpressionFunction(regulators);

        childGenome.setGRNGenome(parent1Genome.getGRNGenome().crossover(parent2Genome.getGRNGenome()));

        for (String geneName : allGeneNames) {
            if (parent1Genome.hasGene(geneName) && parent2Genome.hasGene(geneName)) {
                childGenome.addNode(geneName,
                        Simulation.RANDOM.nextBoolean() ?
                        parent1Genome.getNode(geneName) :
                        parent2Genome.getNode(geneName)
                );
            } else if (parent1Genome.hasGene(geneName)) {
                childGenome.addNode(geneName, parent1Genome.getNode(geneName));
            } else {
                childGenome.addNode(geneName, parent2Genome.getNode(geneName));
            }
        }

        return Evolvable.createNew(clazz, childGenome.cloneWithMutation());
    }

    static void setTraitValue(Evolvable e, Method setter, Object value) {
        try {
            setter.invoke(e, value);
        }
        catch (IllegalArgumentException | IllegalAccessException
               | InvocationTargetException | NullPointerException ex) {
            throw new RuntimeException(
                    "Could not map value " + value + " to the trait. Check the signature of "
                    + setter.getName() + " on " + e + ": " + ex
            );
        }
        catch (ClassCastException ex) {
            throw new RuntimeException(
                    "Could not cast value " + value + " to the trait. Check the signature of "
                    + setter.getName() + " on " + e + ": " + ex
            );
        }
    }

    static <T extends Evolvable> T createNew(Class<T> clazz) {
        GeneExpressionFunction fn = createGeneMapping(clazz);
        return createNew(clazz, fn);
    }

    static <T extends Evolvable> Supplier<T> createEvolvableConstructor(Class<T> clazz, GeneExpressionFunction fn) {
        return () -> {
            try {
                for (Constructor<?> c : clazz.getConstructors())
                    if (c.getParameterCount() > 0
                            && c.getParameterTypes()[0].equals(GeneExpressionFunction.Regulators.class))
                        return clazz.getConstructor(GeneExpressionFunction.Regulators.class)
                                .newInstance(fn.getGeneRegulators());

                return clazz.getConstructor().newInstance();

            } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                     NoSuchMethodException e) {
                throw new RuntimeException("Could not create new evolvable: " + e);
            }
        };
    }

    static <T extends Evolvable> T createNew(Class<T> clazz, GeneExpressionFunction fn) {
        NetworkGenome grnGenome = fn.getGRNGenome();
        Supplier<T> constructor = createEvolvableConstructor(clazz, fn);
        T newEvolvable = createNew(constructor, fn);
        GeneExpressionFunction newFn = newEvolvable.getGeneExpressionFunction();
        if (grnGenome != null)
            newFn.setGRNGenome(grnGenome);
        newFn.registerTargetEvolvable(newEvolvable.name(), newEvolvable);
        newFn.build();
        newEvolvable.build();
        newFn.update();
        return newEvolvable;
    }

    static <T extends Evolvable.Element> T createNewElement(
            String collectionName, int i, Class<T> clazz, GeneExpressionFunction fn) {
        Supplier<T> constructor = createEvolvableConstructor(clazz, fn);
        T newEvolvable = createNew(constructor, fn);
        newEvolvable.setIndex(i);
        fn = newEvolvable.getGeneExpressionFunction();
        if (collectionName != null)
            fn.prependNames(collectionName + "/" + newEvolvable.name());
        fn.registerTargetEvolvable(newEvolvable.name(), newEvolvable);
        fn.build();
        newEvolvable.build();
        fn.update();
        return newEvolvable;
    }

    static <T extends Evolvable> T createNewComponent(Class<T> componentClass, GeneExpressionFunction fn) {
        Supplier<T> constructor = createEvolvableConstructor(componentClass, fn);
        T newEvolvable = createNew(constructor, fn);
        newEvolvable.build();
        return newEvolvable;
    }

    static <T extends Evolvable> T createNew(Supplier<T> constructor,
                                             GeneExpressionFunction fn)
    {
        T newEvolvable = constructor.get();

        for (Method method : newEvolvable.getClass().getMethods()) {
            if (method.isAnnotationPresent(EvolvableComponent.class)) {

                Class<Evolvable> componentClass = (Class<Evolvable>) method.getParameterTypes()[0];

                GeneExpressionFunction componentFn = createGeneMapping(componentClass);
                Evolvable component = createNewComponent(componentClass, componentFn);
                componentFn.registerTargetEvolvable(component.name(), component);
//                if (component.getGeneExpressionFunction() != null)
//                    geneExpressionFunction.merge(component.getGeneExpressionFunction());

                if (component instanceof GeneExpressionFunction) {
                    GeneExpressionFunction newFn = (GeneExpressionFunction) component;
                    newFn.registerTargetEvolvable(newFn.name(), newFn);
                    newFn.merge(fn);
                    fn = newFn;
                }
                else {
                    fn.merge(componentFn);
                    setTraitValue(newEvolvable, method, component);
                }
            }
        }

        if (newEvolvable instanceof GeneExpressionFunction)
            ((GeneExpressionFunction) newEvolvable).merge(fn);
        else
            newEvolvable.setGeneExpressionFunction(fn);

        return newEvolvable;
    }

    static <T extends Evolvable> GeneExpressionFunction createGeneMapping(Class<T> clazz) {
        return createGeneMapping(clazz, Evolvable.extractRegulators(clazz));
    }

    static <T extends Evolvable> GeneExpressionFunction createGeneMapping(
            Class<T> clazz, GeneExpressionFunction.Regulators regulators
    ) {
        GeneExpressionFunction geneExpressionFunction = new GeneExpressionFunction(regulators);
        for (Method method : clazz.getMethods()) {
            if (method.isAnnotationPresent(EvolvableFloat.class)) {
                EvolvableFloat evolvable = method.getAnnotation(EvolvableFloat.class);
                geneExpressionFunction.addEvolvableFloat(evolvable, method);
            }

            else if (method.isAnnotationPresent(ControlVariable.class)) {
                ControlVariable controlVariable = method.getAnnotation(ControlVariable.class);
                geneExpressionFunction.addControlVariable(controlVariable, method);
            }

            else if (method.isAnnotationPresent(EvolvableInteger.class)) {
                EvolvableInteger evolvable = method.getAnnotation(EvolvableInteger.class);
                geneExpressionFunction.addEvolvableInteger(evolvable, method);
            }

            else if (method.isAnnotationPresent(EvolvableObject.class)) {
                EvolvableObject evolvable = method.getAnnotation(EvolvableObject.class);
                geneExpressionFunction.addEvolvableObject(evolvable, method);
            }

            else if (method.isAnnotationPresent(EvolvableList.class)) {
                EvolvableList evolvable = method.getAnnotation(EvolvableList.class);
                geneExpressionFunction.addEvolvableCollection(geneExpressionFunction, evolvable, method);
            }

            else if (method.isAnnotationPresent(EvolvableComponent.class)) {
                Class<T> componentClass = (Class<T>) method.getParameterTypes()[0];
                Class<?>[] componentInterfaces = componentClass.getInterfaces();

                if (Arrays.stream(componentInterfaces).noneMatch(Evolvable.class::isAssignableFrom))
                    throw new RuntimeException("Method is not a setter for an evolvable: " + method);

                if (componentClass.equals(GeneExpressionFunction.class)) {
                    GeneExpressionFunction newFn = createGeneMapping(componentClass, regulators);
                    newFn.merge(geneExpressionFunction);
                    geneExpressionFunction = newFn;
                } else {
                    geneExpressionFunction.merge(createGeneMapping(componentClass, regulators));
                }
            }
        }

        return geneExpressionFunction;
    }


    static <T extends Evolvable> GeneExpressionFunction.Regulators extractRegulators(Class<T> clazz) {
        GeneExpressionFunction.Regulators regulators = new GeneExpressionFunction.Regulators();
        for (Method method : clazz.getMethods()) {
            if (method.isAnnotationPresent(GeneRegulator.class)) {
                String regulatorName = method.getAnnotation(GeneRegulator.class).name();
                GeneExpressionFunction.RegulationNode regulator =
                        new GeneExpressionFunction.RegulationNode(regulatorName, method);
                regulators.put(regulatorName, regulator);
            }
        }
        return regulators;
    }
}
