package com.protoevo.biology;

import com.badlogic.gdx.graphics.Color;
import com.protoevo.core.Simulation;
import com.protoevo.env.Environment;


public class MeatCell extends EdibleCell {

    public static final long serialVersionUID = -5549426815144079228L;

    private float rottenness = 0.0f;

    public MeatCell(float radius, Environment environment) {
        super(radius, Food.Type.Meat, environment);

        float r = (150 + Simulation.RANDOM.nextInt(105)) / 255f;
        float g = (25  + Simulation.RANDOM.nextInt(100)) / 255f;
        float b = (25  + Simulation.RANDOM.nextInt(100)) / 255f;
        setHealthyColour(new Color(r, g, b, 1f));
        setDegradedColour(new Color(158, 121, 79, 1f));
    }

    public void age(float delta) {
        float deathRate = getRadius() * delta * 100;
        setHealth(getHealth() * (1 - deathRate));
        rottenness = rottenness * (1 - deathRate);
        if (rottenness > 1)
            rottenness = 1;
    }

    @Override
    public void update(float delta)
    {
        age(delta);
        super.update(delta);
    }

    @Override
    public String getPrettyName() {
        return "Meat";
    }

    @Override
    public boolean canMakeBindings() {
        return false;
    }
}
