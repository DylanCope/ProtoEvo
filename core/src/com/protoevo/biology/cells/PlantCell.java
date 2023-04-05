package com.protoevo.biology.cells;

import com.badlogic.gdx.math.MathUtils;
import com.protoevo.biology.CauseOfDeath;
import com.protoevo.core.Statistics;
import com.protoevo.physics.CollisionHandler;
import com.protoevo.env.Environment;
import com.protoevo.env.JointsManager;
import com.protoevo.utils.Colour;
import com.protoevo.utils.Geometry;

public class PlantCell extends Cell {
    public static final long serialVersionUID = -3975433688803760076L;

    private final float maxRadius;
    private float photosynthesisRate = 0;
    private static final Statistics.ComplexUnit photosynthesisUnit =
            new Statistics.ComplexUnit(Statistics.BaseUnit.ENERGY).divide(Statistics.BaseUnit.TIME);

    public PlantCell(float radius, Environment environment) {
        super();
        setRadius(Math.max(radius, Environment.settings.plant.minBirthRadius.get()));
        addToEnv(environment);
        setGrowthRate(MathUtils.random(
                Environment.settings.plant.minPlantGrowth.get(),
                Environment.settings.plant.maxPlantGrowth.get()));

        maxRadius = MathUtils.random(
                1.5f * Environment.settings.minParticleRadius.get(),
                Environment.settings.maxParticleRadius.get() / 2f);

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
    public void update(float delta) {
        super.update(delta);

        if (isDead())
            return;

        float light = getEnv().getLight(getPos());
        float photoRate = light * getArea() / Geometry.getCircleArea(Environment.settings.maxParticleRadius.get());
        photosynthesisRate = photoRate * Environment.settings.plantPhotosynthesizeEnergyRate.get();

        addConstructionMass(delta * Environment.settings.plantConstructionRate.get());
        addAvailableEnergy(delta * photosynthesisRate);

        int nProtozoaContacts = 0;
        for (CollisionHandler.Collision collision : getContacts()) {
            Object o = getOther(collision);
            if (o instanceof Protozoan)
                nProtozoaContacts++;
        }
        if (nProtozoaContacts >= 1) {
            float dps = Environment.settings.plant.collisionDestructionRate.get() * nProtozoaContacts;
            removeMass(delta * dps, CauseOfDeath.OVERCROWDING);
        }

        handleAttachments();

        if (shouldSplit()) {
            getEnv().requestBurst(
                this, PlantCell.class, this::createChild
            );
        }
    }

    @Override
    protected float getVoidStartDistance2() {
        return super.getVoidStartDistance2() * 0.9f * 0.9f;
    }

    public PlantCell createChild(float r) {
        return new PlantCell(r, getEnv());
    }

    public void attach(Cell otherCell) {
        JointsManager.Joining joining = new JointsManager.Joining(this, otherCell);
        JointsManager jointsManager = getEnv().getJointsManager();
        jointsManager.createJoint(joining);
        registerJoining(joining);
        otherCell.registerJoining(joining);
    }

    public void handleAttachments() {
        if (getNumAttachedCells() < 2) {
            for (CollisionHandler.Collision collision : getContacts()) {
                Object o = getOther(collision);
                if (o instanceof PlantCell
                        && ((PlantCell) o).getNumAttachedCells() < 2
                        && !isAttachedTo((PlantCell) o)) {
                    attach((PlantCell) o);
                    if (getNumAttachedCells() >= 2)
                        break;
                }
            }
        }
    }

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
