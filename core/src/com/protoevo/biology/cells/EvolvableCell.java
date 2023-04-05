package com.protoevo.biology.cells;

import com.protoevo.biology.evolution.*;
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
        float t = Environment.settings.maxLightEnvTemp.get() / 2f;
        return (getEnv().getTemperature(getPos()) - t) / t;
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
        if (timeSinceLastGeneExpression >= getExpressionInterval()) {
            geneExpressionFunction.update();
            timeSinceLastGeneExpression = 0;
        }
    }

    public abstract float getExpressionInterval();
}
