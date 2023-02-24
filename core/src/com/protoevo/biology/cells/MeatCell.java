package com.protoevo.biology.cells;

import com.badlogic.gdx.math.MathUtils;
import com.protoevo.biology.CauseOfDeath;
import com.protoevo.core.Simulation;
import com.protoevo.env.Environment;
import com.protoevo.utils.Colour;


public class MeatCell extends Cell {

    public static final long serialVersionUID = -5549426815144079228L;

    public MeatCell(float radius, Environment environment) {
        super();
        setRadius(radius);
        addToEnv(environment);

        float r = (150 + MathUtils.random(105)) / 255f;
        float g = (25  + MathUtils.random(100)) / 255f;
        float b = (25  + MathUtils.random(100)) / 255f;
        setHealthyColour(new Colour(r, g, b, 1f));
        setDegradedColour(degradeColour(getHealthyColour(), 0.3f));
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
    public boolean isEdible() {
        return true;
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
