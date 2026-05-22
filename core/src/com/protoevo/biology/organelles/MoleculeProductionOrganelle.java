package com.protoevo.biology.organelles;

import com.protoevo.biology.cells.Cell;
import com.protoevo.biology.ComplexMolecule;
import com.protoevo.core.Statistics;
import com.protoevo.env.Environment;
import com.protoevo.maths.Functions;

import java.io.Serializable;

public class MoleculeProductionOrganelle extends OrganelleFunction implements Serializable {

    public static long serialVersionUID = 1L;
    private float productionSignature = 0;
    private float productionChangeCooldown = Environment.settings.simulationUpdateDelta.get() * 50;
    private float productionChangeTimer = 0f;
    private ComplexMolecule productionMolecule;
    private float lastRate;
    private Statistics stats = new Statistics();

    public MoleculeProductionOrganelle() {
        super(null);
    }

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
            productionSignature = Functions.cyclicalLinearRemap(input[0], -1, 1, 0, 1);
            productionMolecule = ComplexMolecule.fromSignature(productionSignature);
            productionChangeTimer = productionChangeCooldown;
        }

        if (productionChangeTimer > 0)
            productionChangeTimer -= delta;

        float rate = Environment.settings.maxMoleculeProductionRate.get() *
                Functions.cyclicalLinearRemap(input[1], -1, 1,0, 1);;
        lastRate = rate;

        float producedMass = rate * delta;
        float requiredEnergy = productionMolecule.getProductionCost() * producedMass;

        Cell cell = organelle.getCell();

        float constructionMassAvailable = cell.getConstructionMassAvailable();
        float energyAvailable = cell.getEnergyAvailable();
        // Reserve gate: only produce molecules when the cell has surplus
        // construction mass beyond a 50%-of-cap reserve. Without this gate,
        // organelles strip-mine the mass pool at ~5 mg/sec while chemical
        // drip (the nibble path that's supposed to bootstrap new cells)
        // delivers only ~0.3 mg/sec. Result: cells survive energy-wise but
        // never accumulate enough mass to grow → never split → no
        // evolution toward better foraging. The reserve guarantees grow()
        // and repair() get first claim on incoming mass, and organelles
        // only fire when the cell is genuinely well-fed.
        float reserve = 0.5f * cell.getConstructionMassCap();
        if (producedMass > 0
                && constructionMassAvailable >= reserve + producedMass
                && energyAvailable >= requiredEnergy) {
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
