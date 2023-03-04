package com.protoevo.biology.organelles;

import com.protoevo.biology.cells.Cell;
import com.protoevo.biology.ComplexMolecule;
import com.protoevo.core.Statistics;
import com.protoevo.settings.SimulationSettings;
import com.protoevo.utils.Utils;

import java.io.Serializable;

public class MoleculeProductionOrganelle extends OrganelleFunction implements Serializable {

    public static long serialVersionUID = 1L;
    private float productionSignature = 0;
    private float productionChangeCooldown = SimulationSettings.simulationUpdateDelta * 50;
    private float productionChangeTimer = 0f;
    private ComplexMolecule productionMolecule;
    private float lastRate;
    private final Statistics stats = new Statistics();

    public MoleculeProductionOrganelle(Organelle organelle) {
        super(organelle);
    }

    /**
     * @param delta time since last update
     * @param input input[0] is the production signature, input[1] is the production rate
     */
    @Override
    public void update(float delta, float[] input) {
        if ((input[0] != productionSignature && productionChangeTimer <= 0) || productionMolecule == null) {
            productionSignature = Utils.cyclicalLinearRemap(input[0], -1, 1, 0, 1);
            productionMolecule = ComplexMolecule.fromSignature(productionSignature);
            productionChangeTimer = productionChangeCooldown;
        }

        if (productionChangeTimer > 0)
            productionChangeTimer -= delta;

        float rate = SimulationSettings.maxMoleculeProductionRate *
                Utils.cyclicalLinearRemap(input[1], -1, 1,0, 1);;
        lastRate = rate;

        float producedMass = rate * delta;
        float requiredEnergy = productionMolecule.getProductionCost() * producedMass;

        Cell cell = organelle.getCell();

        float constructionMassAvailable = cell.getConstructionMassAvailable();
        float energyAvailable = cell.getEnergyAvailable();
        if (producedMass > 0 && constructionMassAvailable >= producedMass && energyAvailable >= requiredEnergy) {
            cell.addAvailableComplexMolecule(productionMolecule, producedMass);
            cell.depleteConstructionMass(producedMass);
            cell.depleteEnergy(requiredEnergy);
        }
    }

    @Override
    public String getName() {
        return "Molecule Production";
    }

    @Override
    public String getInputMeaning(int idx) {
        if (idx == 0)
            return "Signature";
        if (idx == 1)
            return "Production Rate";
        return null;
    }

    @Override
    public Statistics getStats() {
        stats.put("Signature", productionSignature);
        stats.put("Production Rate", lastRate, Statistics.ComplexUnit.MASS_PER_TIME);
        return stats;
    }
}
