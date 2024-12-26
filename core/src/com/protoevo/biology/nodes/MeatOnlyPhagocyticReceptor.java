package com.protoevo.biology.nodes;

import com.protoevo.biology.cells.Cell;
import com.protoevo.biology.cells.Protozoan;
import com.protoevo.core.Statistics;

public class MeatOnlyPhagocyticReceptor extends PhagocyticReceptor {

    private static final long serialVersionUID = 1L;

    public MeatOnlyPhagocyticReceptor() {
        super(null);
    }

    public MeatOnlyPhagocyticReceptor(SurfaceNode node) {
        super(node);
    }

    @Override
    public void update(float delta, float[] input, float[] output) {
        Cell cell = node.getCell();
        if (!(cell instanceof Protozoan)) {
            return;
        }

        handleDim1IO(input, output);
        tryEngulfContacts();
    }

    private void handleDim1IO(float[] input, float[] output) {
        Cell cell = node.getCell();
        setShouldEngulfMeat(input[0] > 0f);
        setShouldEngulfPlant(false);

        if (!((Protozoan) cell).getEngulfedCells().contains(lastEngulfed))
            lastEngulfed = null;
        if (lastEngulfed != null) {
            output[0] = lastEngulfed.getHealth();
        }
    }

    @Override
    public String getName() {
        return "Meat Phagocytic Receptor";
    }

    @Override
    public String getInputMeaning(int index) {
        if (node.getIODimension() == 1) {
            if (index == 0)
                return "Should Engulf";
        }
        return null;
    }

    @Override
    public String getOutputMeaning(int index) {
            if (index == 0)
                return "Last Engulfed Health";
        return null;
    }

    @Override
    public void addStats(Statistics stats) {
        stats.putCount("Engulfed Cells", ((Protozoan) node.getCell()).getEngulfedCells().size());
        stats.putBoolean("Will Engulf?", getShouldEngulfPlant());
    }
}
