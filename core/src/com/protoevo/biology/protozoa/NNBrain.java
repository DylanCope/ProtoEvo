package com.protoevo.biology.protozoa;

import com.badlogic.gdx.graphics.Color;
import com.protoevo.biology.neat.NeuralNetwork;
import com.protoevo.core.settings.Settings;
import com.protoevo.env.ChemicalSolution;

public class NNBrain implements Brain {

    public final NeuralNetwork network;
    private float[] outputs;
    private final float[] inputs;

    public NNBrain(NeuralNetwork network) {
        this.network = network;
        outputs = network.outputs();
        inputs = new float[network.getInputSize()];
    }

    @Override
    public void tick(Protozoan p)
    {
        int i = 0;

        inputs[i++] = 1; // bias term
        inputs[i++] = p.getHealth() * 2 - 1;
        inputs[i++] = 2 * p.getRadius() / p.getSplitRadius() - 1;
        inputs[i++] = 2 * p.getConstructionMassAvailable() / p.getConstructionMassCap() - 1;

        if (p.getContactSensors() != null)
//            for (ContactSensor sensor : p.getContactSensors())
//                inputs[i++] = sensor.inContact() ? 1f : 0f;

        if (Settings.enableChemicalField) {
            ChemicalSolution chemicalSolution = p.getEnv().getChemicalSolution();
            int chemicalX1 = chemicalSolution.toChemicalGridX(p.getPos().x - p.getRadius());
            int chemicalX2 = chemicalSolution.toChemicalGridX(p.getPos().x + p.getRadius());
            int chemicalY1 = chemicalSolution.toChemicalGridY(p.getPos().x - p.getRadius());
            int chemicalY2 = chemicalSolution.toChemicalGridY(p.getPos().x + p.getRadius());
            inputs[i++] = chemicalSolution.getPlantPheromoneDensity(chemicalX1, chemicalY1) -
                    chemicalSolution.getPlantPheromoneDensity(chemicalX2, chemicalY2);
            inputs[i++] = chemicalSolution.getPlantPheromoneDensity(chemicalX1, chemicalY2) -
                    chemicalSolution.getPlantPheromoneDensity(chemicalX2, chemicalY1);
            int chemicalX = chemicalSolution.toChemicalGridX(p.getPos().x);
            int chemicalY = chemicalSolution.toChemicalGridY(p.getPos().y);
            inputs[i++] = 2 * chemicalSolution.getPlantPheromoneDensity(chemicalX, chemicalY) - 1;
        }

        float retinaHealth = p.getRetina().getHealth();
        for (Retina.Cell cell : p.getRetina()) {
            if (cell.anythingVisible()) {
                Color colour = cell.getColour();
                inputs[i++] = retinaHealth * (-1 + 2 * colour.r / 255f);
                inputs[i++] = retinaHealth * (-1 + 2 * colour.g / 255f);
                inputs[i++] = retinaHealth * (-1 + 2 * colour.b / 255f);
            } else {
                inputs[i++] = 0f;
                inputs[i++] = 0f;
                inputs[i++] = 0f;
            }
        }

        network.setInput(inputs);
        network.tick();
        outputs = network.outputs();
    }

    @Override
    public float turn(Protozoan p)
    {
        float turn = outputs[0];
        return turn;
    }

    @Override
    public float speed(Protozoan p)
    {
        return Math.min(
                Settings.maxProtozoaSpeed * outputs[1],
                Settings.maxProtozoaSpeed
        );
    }

    @Override
    public boolean wantToMateWith(Protozoan p) {
        return outputs[2] > 0;
    }

    @Override
    public float attack(Protozoan p) {
        return outputs[3] > 0 ? 1 : 0;
    }

    @Override
    public float energyConsumption() {
        return 0;
    }

    @Override
    public float growthControl() {
        return outputs[4];
    }
}
