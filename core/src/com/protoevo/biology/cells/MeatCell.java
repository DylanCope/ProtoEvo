package com.protoevo.biology.cells;

import com.protoevo.biology.CauseOfDeath;
import com.protoevo.biology.Food;
import com.protoevo.core.Simulation;
import com.protoevo.env.Environment;
import com.protoevo.utils.Colour;


public class MeatCell extends EdibleCell {

    public static final long serialVersionUID = -5549426815144079228L;

    public MeatCell(float radius, Environment environment) {
        super(radius, Food.Type.Meat, environment);

        float r = (150 + Simulation.RANDOM.nextInt(105)) / 255f;
        float g = (25  + Simulation.RANDOM.nextInt(100)) / 255f;
        float b = (25  + Simulation.RANDOM.nextInt(100)) / 255f;
        setHealthyColour(new Colour(r, g, b, 1f));
//        setDegradedColour(new Color(158 / 255f, 121, 79, 1f));
        setDegradedColour(degradeColour(getHealthyColour(), 0.5f));
    }

    public void age(float delta) {
        float deathRate = getRadius() * delta * 20f;
        damage(getHealth() * deathRate, CauseOfDeath.OLD_AGE);
    }

    @Override
    public void update(float delta) {
        age(delta);
        super.update(delta);
    }

    @Override
    public float getMinRadius() {
        return super.getMinRadius() / 5f;
    }

    @Override
    public void kill(CauseOfDeath causeOfDeath) {
        super.kill(causeOfDeath);
    }

    @Override
    public String getPrettyName() {
        return "Meat";
    }

}