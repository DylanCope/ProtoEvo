package com.protoevo.biology.cells;

import com.badlogic.gdx.math.MathUtils;
import com.protoevo.biology.CauseOfDeath;
import com.protoevo.biology.evolution.Evolvable;
import com.protoevo.biology.evolution.EvolvableFloat;
import com.protoevo.core.Statistics;
import com.protoevo.env.Environment;
import com.protoevo.physics.Joining;
import com.protoevo.physics.JointsManager;
import com.protoevo.physics.Collision;
import com.protoevo.physics.Particle;
import com.protoevo.utils.Colour;
import com.protoevo.utils.Geometry;
import com.protoevo.utils.Utils;

public class PlantCell extends EvolvableCell {
    public static final long serialVersionUID = -3975433688803760076L;

    private float maxRadius;
    private float photosynthesisRate = 0;
    private static final Statistics.ComplexUnit photosynthesisUnit =
            new Statistics.ComplexUnit(Statistics.BaseUnit.ENERGY).divide(Statistics.BaseUnit.TIME);

    public PlantCell(float radius, Environment environment) {
        super();
        setRadius(Math.max(radius, Environment.settings.plant.minBirthRadius.get()));
        setEnvironmentAndBuildPhysics(environment);
        setGrowthRate(MathUtils.random(minGrowthRate(), maxGrowthRate()));

        maxRadius = randomMaxRadius();

        setRandomPlantColour();
    }

    public PlantCell() {
        super();
        setRadius(MathUtils.random(
                Environment.settings.plant.minBirthRadius.get(),
                Environment.settings.plant.maxBirthRadius.get()));
        maxRadius = randomMaxRadius();
        setGrowthRate(MathUtils.random(minGrowthRate(), maxGrowthRate()));
        setRandomPlantColour();
    }

    private float randomMaxRadius() {
        // 10% larger than the max plant birth radius
        float minMaxR = 2f * Environment.settings.plant.minBirthRadius.get();
        // 50% of the max particle radius
        float maxMaxR = Environment.settings.maxParticleRadius.get() / 2f;

        return minMaxR < maxMaxR ? MathUtils.random(minMaxR, maxMaxR) : maxMaxR;
    }

    public void setRandomPlantColour() {
        float darken = 0.9f;
        setHealthyColour(new Colour(
                darken * (30 + MathUtils.random(105)) / 255f,
                darken * (150 + MathUtils.random(100)) / 255f,
                darken * (10 + MathUtils.random(100)) / 255f,
                1f)
        );

        setDegradedColour(degradeColour(getHealthyColour(), 0.5f));
    }

    @Override
    public float getMaxRadius() {
        return maxRadius;
    }

    @EvolvableFloat(name="Split Radius", min=0, max=1)
    public void setMaxRadius(float splitRadius) {
        this.maxRadius = Utils.clampedLinearRemap(
                splitRadius, 0, 1,
                1.5f * Environment.settings.minParticleRadius.get(),
                Environment.settings.maxParticleRadius.get() / 2f
        );
    }

    @Override
    public boolean isEdible() {
        return true;
    }

    private static float randomPlantRadius() {
        float range = Environment.settings.plant.maxBirthRadius.get() * .5f - Environment.settings.plant.minBirthRadius.get();
        return Environment.settings.plant.minBirthRadius.get() + range * MathUtils.random();
    }

    public PlantCell(Environment environment) {
        this(randomPlantRadius(), environment);
    }

    private boolean shouldSplit() {
        return hasNotBurst() && getRadius() >= 0.99f * maxRadius &&
                getHealth() > Environment.settings.plant.minHealthToSplit.get();
    }

    @Override
    public float minGrowthRate() {
        return Environment.settings.plant.minPlantGrowth.get();
    }

    @Override
    public float maxGrowthRate() {
        return Environment.settings.plant.maxPlantGrowth.get();
    }

    @Override
    public void update(float delta) {
        super.update(delta);

        if (isDead())
            return;

        float light = getLightAtCell();
        float maxArea = Geometry.getCircleArea(Environment.settings.maxParticleRadius.get());
        float photoRate = light * getParticle().getArea() / maxArea;
        photosynthesisRate = photoRate * Environment.settings.plant.photosynthesizeEnergyRate.get();

        addConstructionMass(delta * Environment.settings.plant.constructionRate.get());
        addAvailableEnergy(delta * photosynthesisRate);

        int nProtozoaContacts = 0;
        for (Collision collision : getParticle().getContacts()) {
            Object o = collision.getOther(getParticle());
            if (o instanceof Particle && ((Particle) o).getUserData() instanceof Protozoan)
                nProtozoaContacts++;
        }
        if (nProtozoaContacts >= 1) {
            float dps = Environment.settings.plant.collisionDestructionRate.get() * nProtozoaContacts;
            removeMass(delta * dps, CauseOfDeath.OVERCROWDING);
        }

        handleAttachments();

        if (shouldSplit()) {
            getEnv().ifPresent(env -> env.requestBurst(
                this, PlantCell.class, this::createChild
            ));
        }
    }

    @Override
    public float getExpressionInterval() {
        return Environment.settings.plant.geneExpressionInterval.get();
    }

    @Override
    protected float getVoidStartDistance2() {
        return super.getVoidStartDistance2() * 0.9f * 0.9f;
    }

    public PlantCell createChild(float r) {
        PlantCell child;
        if (Environment.settings.plant.evolutionEnabled.get()) {
            child = Evolvable.asexualClone(this);
            child.setRadius(r);
            getEnv().ifPresent(child::setEnvironmentAndBuildPhysics);
        } else
            child = new PlantCell(
                    r, getEnv().orElseThrow(() -> new RuntimeException("Cannot create cell without environment")));
        return child;
    }

    public void attach(Cell otherCell) {
        Joining joining = new Joining(this.getParticle(), otherCell.getParticle());
        getEnv().ifPresent(env -> {
            JointsManager jointsManager = env.getJointsManager();
            jointsManager.createJoint(joining);
            registerJoining(joining);
            otherCell.registerJoining(joining);
        });
    }

    public void handleAttachments() {
        if (getNumAttachedCells() < 2) {
            for (Collision collision : getParticle().getContacts()) {
                Object o = collision.getOther(getParticle());
                if (!(o instanceof Particle && ((Particle) o).getUserData() instanceof PlantCell))
                    continue;

                PlantCell otherPlant = (PlantCell) ((Particle) o).getUserData();

                if (otherPlant.getNumAttachedCells() < 2 && !isAttachedTo(otherPlant)) {
                    attach(otherPlant);
                    if (getNumAttachedCells() >= 2)
                        break;
                }
            }
        }
    }

    @Override
    public Statistics getStats() {
        Statistics stats = super.getStats();
        stats.putDistance("Split Radius", maxRadius);
        stats.put("Photosynthesis Rate", photosynthesisRate, photosynthesisUnit);
        return stats;
    }

    @Override
    public String getPrettyName() {
        return "Plant";
    }

    public int burstMultiplier() {
        return 3;
    }
}
