package com.protoevo.biology.organelles;

import com.protoevo.biology.Cell;
import com.protoevo.biology.ComplexMolecule;
import com.protoevo.utils.Utils;

import java.io.Serializable;

public class MoleculeProductionOrganelle extends OrganelleFunction implements Serializable {

    public static long serialVersionUID = 1L;
    float productionSignature = 0;
    private ComplexMolecule productionMolecule;

    public MoleculeProductionOrganelle(Organelle organelle) {
        super(organelle);
    }

    public static final int possibleMolecules = 256;

    @Override
    public void update(float delta, float[] input) {
        if (input[0] != productionSignature || productionMolecule == null) {
            float x = Utils.linearRemap(input[0], -1, 1, 0, 1);
            productionSignature = ((int) (possibleMolecules * x)) / (float) possibleMolecules;
            productionMolecule = new ComplexMolecule(productionSignature, 1);
        }
        float rate = input[1];

        float producedMass = delta * rate;
        float requiredEnergy = productionMolecule.getProductionCost() * producedMass;

        Cell cell = organelle.getCell();

        float constructionMassAvailable = cell.getConstructionMassAvailable();
        float energyAvailable = cell.getEnergyAvailable();
        if (producedMass > 0 && constructionMassAvailable > producedMass && energyAvailable > requiredEnergy) {
            cell.addAvailableComplexMolecule(productionMolecule, producedMass);
            cell.depleteConstructionMass(producedMass);
            cell.depleteEnergy(requiredEnergy);
        }
    }
}
