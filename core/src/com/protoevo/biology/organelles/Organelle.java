package com.protoevo.biology.organelles;

import com.protoevo.biology.Cell;
import com.protoevo.biology.evolution.Evolvable;
import com.protoevo.biology.evolution.GeneExpressionFunction;
import com.protoevo.biology.evolution.RegulatedFloat;

import java.util.Optional;

public class Organelle implements Evolvable.Element {

    private GeneExpressionFunction geneExpressionFunction;
    private int index;
    private OrganelleFunction function = null;
    private final float[] inputs = new float[3];
    private Cell cell;

    public Organelle() {}

    public Organelle(Cell cell) {
        this.cell = cell;
    }

    @Override
    public void setGeneExpressionFunction(GeneExpressionFunction fn) {
        this.geneExpressionFunction = fn;
    }

    @Override
    public GeneExpressionFunction getGeneExpressionFunction() {
        return geneExpressionFunction;
    }

    public Cell getCell() {
        return cell;
    }

    public void setCell(Cell cell) {
        this.cell = cell;
    }

    public void update(float delta) {
        if (function != null) {
            function.update(delta, inputs);
        }
    }

    public void setFunction(OrganelleFunction function) {
        this.function = function;
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
