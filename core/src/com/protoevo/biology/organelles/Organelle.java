package com.protoevo.biology.organelles;

import com.protoevo.biology.cells.Cell;
import com.protoevo.biology.evolution.Evolvable;
import com.protoevo.biology.evolution.EvolvableFloat;
import com.protoevo.biology.evolution.GeneExpressionFunction;
import com.protoevo.core.Statistics;


public class Organelle implements Evolvable.Element {


    private GeneExpressionFunction geneExpressionFunction;
    private int index;
    private OrganelleFunction function;
    private final float[] inputs = new float[2];
    private Cell cell;
    private final Statistics stats = new Statistics();

    public Organelle() {
        function = new MoleculeProductionOrganelle(this);
    }

    public Organelle(Cell cell) {
        this();
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
        if (hasFunction()) {
            function.update(delta, inputs);
        }
    }

    public boolean hasFunction() {
        return function != null;
    }

    public OrganelleFunction getFunction() {
        return function;
    }

    public void setFunction(OrganelleFunction function) {
        this.function = function;
    }

    @EvolvableFloat(name = "Input/0", min=-1, max=1)
    public void setInput0(float input) {
        inputs[0] = input;
    }

    @EvolvableFloat(name = "Input/1", min=-1, max=1)
    public void setInput1(float input) {
        inputs[1] = input;
    }

    @Override
    public void setIndex(int index) {
        this.index = index;
    }

    @Override
    public int getIndex() {
        return index;
    }

    public Statistics getStats() {
        stats.clear();
        stats.put("Function", function != null ? function.getName() : "None");
        if (function != null) {
            Statistics functionStats = function.getStats();
            if (functionStats != null)
                stats.putAll(function.getStats());
        }
        return stats;
    }
}
