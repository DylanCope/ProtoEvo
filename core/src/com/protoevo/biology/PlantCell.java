package com.protoevo.biology;

import com.badlogic.gdx.graphics.Color;
import com.protoevo.core.settings.Settings;
import com.protoevo.core.Simulation;
import com.protoevo.env.Environment;

import java.util.Map;

public class PlantCell extends EdibleCell {
    public static final long serialVersionUID = -3975433688803760076L;

    public static final CellAdhesion.CAM plantCAM = CellAdhesion.newHeterophilicCAM();

    private final float maxRadius;
    private float crowdingFactor;

    public PlantCell(float radius, Environment environment) {
        super(Math.max(radius, Settings.minPlantBirthRadius), Food.Type.Plant, environment);
        setGrowthRate(Settings.minPlantGrowth + Settings.plantGrowthRange * Simulation.RANDOM.nextFloat());

//        float range = 2 * Settings.maxPlantBirthRadius - radius;
//        maxRadius = Math.max(2 * radius, Settings.minPlantBirthRadius) + 2 * radius * Simulation.RANDOM.nextFloat();
        float range = Settings.maxParticleRadius - getRadius();
//        maxRadius = (float) (2*max + range * Simulation.RANDOM.nextDouble());
        maxRadius = Simulation.RANDOM.nextFloat(2 * Settings.minParticleRadius, Settings.maxParticleRadius);

        setMaxAttachedCells(2);
        setCAMAvailable(plantCAM, 1f);

        setHealthyColour(new Color(
                (30 + Simulation.RANDOM.nextInt(105)) / 255f,
                (150  + Simulation.RANDOM.nextInt(100)) / 255f,
                (10  + Simulation.RANDOM.nextInt(100)) / 255f,
                1f)
        );
    }

    private static float randomPlantRadius() {
        float range = Settings.maxPlantBirthRadius * .5f - Settings.minPlantBirthRadius;
        return Settings.minPlantBirthRadius + range * Simulation.RANDOM.nextFloat();
    }

    public PlantCell(Environment environment) {
        this(randomPlantRadius(), environment);
    }

    private boolean shouldSplit() {
        return getRadius() > maxRadius &&
                getHealth() > Settings.minHealthToSplit;
    }

    public float getCrowdingFactor() {
        return crowdingFactor;
    }

    @Override
    public void onCollision(Cell other) {
        super.onCollision(other);
        updateCrowding(other);
    }

    private void updateCrowding(Cell e) {
        float sqDist = e.getPos().dst2(getPos());
        if (sqDist < Math.pow(3 * getRadius(), 2)) {
            crowdingFactor += e.getRadius() / (getRadius() + sqDist);
        }
    }

    @Override
    public void update(float delta) {
        super.update(delta);

        if (isDead())
            return;

//        crowdingFactor = 0;
//        ChunkManager chunkManager = getTank().getChunkManager();
//        Iterator<Cell> entities = chunkManager.broadEntityDetection(getPos(), getRadius());
//        entities.forEachRemaining(this::updateCrowding);

        if (getGrowthRate() < 0f)
            setHealth(getHealth() + Settings.plantRegen * delta * getGrowthRate());

        addConstructionMass(delta);
        addAvailableEnergy(delta / 3f);

        if (shouldSplit()) {
            getEnv().requestBurst(
                new BurstRequest<>(this, PlantCell.class, r -> new PlantCell(r, getEnv()))
            );
        }

    }

    /**
     * <a href="https://www.desmos.com/calculator/hmhjwdk0jc">Desmos Graph</a>
     * @return The growth rate based on the crowding and current radius.
     */
    @Override
    public float getGrowthRate() {
        float x = (-getCrowdingFactor() + Settings.plantCriticalCrowding) / Settings.plantCrowdingGrowthDecay;
        x = (float) (Math.tanh(x));// * Math.tanh(-0.01 + 50 * getCrowdingFactor() / Settings.plantCriticalCrowding));
        x = x < 0 ? (float) (1 - Math.exp(-Settings.plantCrowdingGrowthDecay * x)) : x;
        float growthRate = super.getGrowthRate() * x;
        if (getRadius() > maxRadius)
            growthRate *= Math.exp(maxRadius - getRadius());
        growthRate = growthRate > 0 ? growthRate * getHealth() : growthRate;
        return growthRate;
    }

    public Map<String, Float> getStats() {
        Map<String, Float> stats = super.getStats();
        stats.put("Crowding Factor", crowdingFactor);
        stats.put("Split Radius", Settings.statsDistanceScalar * maxRadius);
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
