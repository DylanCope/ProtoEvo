package com.protoevo.biology.nn.meta;

import com.protoevo.biology.evolution.GeneExpressionFunction;

import java.io.Serializable;
import java.util.function.Function;

/**
 * A GRNTag is a function that takes a GeneExpressionFunction and returns an Object.
 * It is used to tag neurons in the GRN with information that can be used to track their source
 * and destination. This is stored as a function so that when it is copied from child to
 * offspring, we always point to the objects in the new GRN.
 */
public interface GRNTag extends Function<GeneExpressionFunction, Object>, Serializable {

    Object apply(GeneExpressionFunction geneExpressionFunction);
}
