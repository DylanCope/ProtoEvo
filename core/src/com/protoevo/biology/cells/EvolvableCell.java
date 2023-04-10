package com.protoevo.biology.cells;

import com.protoevo.biology.evolution.*;
import com.protoevo.biology.nn.Neuron;
import com.protoevo.env.Environment;
import com.protoevo.utils.Utils;

public abstract class EvolvableCell extends Cell implements Evolvable {

    private GeneExpressionFunction geneExpressionFunction;
    private float timeSinceLastGeneExpression = 0;

    @EvolvableFloat(name="Growth Rate", min=0, max=1)
    public void setGrowth(float growthRate) {
        growthRate = Utils.clampedLinearRemap(
                growthRate,
                0, 1,
                minGrowthRate(), maxGrowthRate()
        );
        setGrowthRate(growthRate);
    }

    public abstract float minGrowthRate();
    public abstract float maxGrowthRate();

    @EvolvableFloat(name="Repair Rate")
    public void setRepairRate(float repairRate) {
        super.setRepairRate(repairRate);
    }

    @Override
    @EvolvableFloat(
            name="Temperature Tolerance",
            min=0, max=1, regulated = false
    )
    public void setTemperatureTolerance(float t) {
        float tolerance = Utils.clampedLinearRemap(
                t, 0, 1,
                Environment.settings.cell.minTemperatureTolerance.get(),
                Environment.settings.cell.maxTemperatureTolerance.get()
        );
        super.setTemperatureTolerance(tolerance);
    }

    @Override
    @EvolvableFloat(
            name="Ideal Temperature",
            min=0, max=1, regulated = false
    )
    public void setIdealTemperature(float t) {
        float temp = Utils.clampedLinearRemap(
                t, 0, 1,
                0, Environment.settings.env.maxLightEnvTemp.get()
        );
        super.setIdealTemperature(temp);
    }

    @Override
    @ControlVariable(
            name="Thermal Conductance",
            min=0, max=2
    )
    public void setThermalConductance(float t) {
        super.setThermalConductance(t);
    }

    @Override
    @GeneRegulator(name="Health")
    public float getHealth() {
        return super.getHealth();
    }

    @GeneRegulator(name="Size", min=0, max=1)
    public float getRadiusAsProportionOfMax() {
        return Utils.clampedLinearRemap(
                super.getRadius(),
                Environment.settings.minParticleRadius.get(),
                Environment.settings.maxParticleRadius.get(),
                0, 1
        );
    }

    @GeneRegulator(name="Construction Mass Available")
    public float getConstructionMass() {
        return getConstructionMassAvailable() / getConstructionMassCap();
    }

    @GeneRegulator(name="Contact Sensor")
    public float getContact() {
        return getContacts().size() > 0 ? 1 : 0;
    }

    @GeneRegulator(name="Light Level")
    public float getLightLevel() {
        if (getEnv() == null)
            return 0;
        return getEnv().getLight(getPos());
    }

    @GeneRegulator(name="Temperature")
    public float getNormalisedTemperature() {
        if (getEnv() == null)
            return 0;
        float t = getIdealTemperature();
        float dt = getTemperatureTolerance();
        return (getInternalTemperature() - t) / dt;
    }

    @Override
    @EvolvableComponent
    public void setGeneExpressionFunction(GeneExpressionFunction fn) {
        geneExpressionFunction = fn;
    }

    @Override
    public GeneExpressionFunction getGeneExpressionFunction() {
        return geneExpressionFunction;
    }

    @Override
    public void update(float delta) {
        super.update(delta);

        timeSinceLastGeneExpression += delta;
        if (geneExpressionFunction != null
                && timeSinceLastGeneExpression >= getExpressionInterval()) {
            geneExpressionFunction.update();
            timeSinceLastGeneExpression = 0;

            for (Neuron n : geneExpressionFunction.getRegulatoryNetwork().getNeurons()) {
                if (n.getType().equals(Neuron.Type.HIDDEN))
                    addActivity(Environment.settings.cell.grnHiddenNodeActivity.get() * n.getLastState());
            }
        }
    }

    public abstract float getExpressionInterval();
}
