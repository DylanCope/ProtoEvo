package com.protoevo.biology;

import com.protoevo.core.Simulation;
import com.protoevo.env.CollisionHandler;
import com.protoevo.env.Environment;
import com.protoevo.settings.PlantSettings;
import com.protoevo.settings.Settings;
import com.protoevo.settings.SimulationSettings;
import com.protoevo.utils.Colour;

import java.util.Map;

public class PlantCell extends EdibleCell {
    public static final long serialVersionUID = -3975433688803760076L;

    public static final CellAdhesion.CAM plantCAM = CellAdhesion.newHeterophilicCAM();

    private final float maxRadius;
    private float crowdingFactor;

    public PlantCell(float radius, Environment environment) {
        super(Math.max(radius, PlantSettings.minPlantBirthRadius), Food.Type.Plant, environment);
        setGrowthRate(Simulation.RANDOM.nextFloat(PlantSettings.minPlantGrowth,
                                                  PlantSettings.maxPlantGrowth));

        maxRadius = Simulation.RANDOM.nextFloat(
                2 * SimulationSettings.minParticleRadius, SimulationSettings.maxParticleRadius);

        setMaxAttachedCells(2);
        setCAMAvailable(plantCAM, 1f);

        float darken = 0.9f;
        setHealthyColour(new Colour(
                darken * (30 + Simulation.RANDOM.nextInt(105)) / 255f,
                darken * (150  + Simulation.RANDOM.nextInt(100)) / 255f,
                darken * (10  + Simulation.RANDOM.nextInt(100)) / 255f,
                1f)
        );
    }

    @Override
    public float getMaxRadius() {
        return maxRadius;
    }

    private static float randomPlantRadius() {
        float range = PlantSettings.maxPlantBirthRadius * .5f - PlantSettings.minPlantBirthRadius;
        return PlantSettings.minPlantBirthRadius + range * Simulation.RANDOM.nextFloat();
    }

    public PlantCell(Environment environment) {
        this(randomPlantRadius(), environment);
    }

    private boolean shouldSplit() {
        return hasNotBurst() && getRadius() >= maxRadius &&
                getHealth() > Settings.minHealthToSplit;
    }

    public float getCrowdingFactor() {
        return crowdingFactor;
    }

    public void updateCrowdingFactor() {
        crowdingFactor = 0;
        for (CollisionHandler.FixtureCollision contact : getContacts()) {
            Object other = getOther(contact);
            if (other instanceof Cell) {
                Cell otherCell = (Cell) other;
                float sqDist = otherCell.getPos().dst2(getPos());
                if (sqDist < Math.pow(3 * getRadius(), 2)) {
                    crowdingFactor += otherCell.getRadius() / (getRadius() + sqDist);
                }
            }
        }
        crowdingFactor *= 5;
    }

    @Override
    public void update(float delta) {
        super.update(delta);

        if (isDead())
            return;

        updateCrowdingFactor();
//        if (getGrowthRate() < 0f)
//            damage(-Settings.plantRegen * delta * getGrowthRate());

        addConstructionMass(delta * Settings.plantConstructionRate);
        addAvailableEnergy(delta * Settings.plantEnergyRate);

        if (shouldSplit()) {
            getEnv().requestBurst(
                this, PlantCell.class, r -> new PlantCell(r, getEnv())
            );
        }
    }

    /**
     * <a href="https://www.desmos.com/calculator/hmhjwdk0jc">Desmos Graph</a>
     * @return The growth rate based on the crowding and current radius.
     */
//    @Override
//    public float getGrowthRate() {
//        float x = (-getCrowdingFactor() + PlantSettings.plantCriticalCrowding) / PlantSettings.plantCrowdingGrowthDecay;
//        x = (float) (Math.tanh(x));// * Math.tanh(-0.01 + 50 * getCrowdingFactor() / Settings.plantCriticalCrowding));
//        x = x < 0 ? (float) (1 - Math.exp(-PlantSettings.plantCrowdingGrowthDecay * x)) : x;
//        float growthRate = super.getGrowthRate() * x;
//        if (getRadius() > maxRadius)
//            growthRate *= Math.exp(maxRadius - getRadius());
//        growthRate = growthRate > 0 ? growthRate * getHealth() : growthRate;
//        return growthRate;
//    }

    public Map<String, Float> getStats() {
        Map<String, Float> stats = super.getStats();
        stats.put("Crowding Factor", getCrowdingFactor());
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

//    @Override
//    public void interact(List<Object> interactions) {
//        float maxDist2 = getInteractionRange() * getInteractionRange();
//        float minDist2 = getRadius() * getRadius() * 1.1f * 1.1f;
//        for (Object interaction : interactions) {
//            if (interaction instanceof PlantCell) {
//                PlantCell other = (PlantCell) interaction;
//                Vector2 pos = other.getPos();
//                float dist2 = pos.dst2(getPos());
//                if (minDist2 < dist2 && dist2 < maxDist2) {
//                    Vector2 impulse = pos.cpy().sub(getPos()).setLength(10f / dist2);
//                    applyImpulse(impulse);
//                }
//            }
//        }
//    }
}
