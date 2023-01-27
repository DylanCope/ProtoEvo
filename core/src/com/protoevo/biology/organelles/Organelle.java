package com.protoevo.biology.organelles;

import com.protoevo.biology.evolution.Evolvable;
import com.protoevo.biology.evolution.GeneExpressionFunction;
import com.protoevo.biology.evolution.RegulatedFloat;

import java.util.Optional;

public class Organelle implements Evolvable.Element {

    private GeneExpressionFunction geneExpressionFunction;
    private int index;
    private final Optional<OrganelleFunction> function = Optional.empty();
    private final float[] inputs = new float[3];

    @Override
    public void setGeneExpressionFunction(GeneExpressionFunction fn) {
        this.geneExpressionFunction = fn;
    }

    @Override
    public GeneExpressionFunction getGeneExpressionFunction() {
        return geneExpressionFunction;
    }

    public void update(float delta) {
        function.ifPresent(f -> f.update(delta, inputs));
    }

    @RegulatedFloat(name = "Input/0")
    public void setInput0(float input) {
        inputs[0] = input;
    }

    @RegulatedFloat(name = "Input/1")
    public void setInput1(float input) {
        inputs[1] = input;
    }

    @RegulatedFloat(name = "Input/2")
    public void setInput2(float input) {
        inputs[2] = input;
    }

    @Override
    public void setIndex(int index) {
        this.index = index;
    }

    @Override
    public int getIndex() {
        return index;
    }
}
