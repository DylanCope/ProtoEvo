package com.protoevo.biology.protozoa;


import com.badlogic.gdx.math.MathUtils;
import com.protoevo.biology.evolution.Trait;
import com.protoevo.core.Simulation;
import com.protoevo.utils.Colour;

import java.io.Serializable;
import java.util.Map;

public class ProtozoaColourTrait implements Trait<Colour>, Serializable {
    public static final long serialVersionUID = -1821863048303900554L;

    private final Colour value;
    private final String geneName;

    private final int minVal = 80;
    private final int maxVal = 150;

    public ProtozoaColourTrait(String geneName) {
        this.geneName = geneName;
        value = newRandomValue();
    }

    public ProtozoaColourTrait(String geneName, Colour value) {
        this.geneName = geneName;
        this.value = value;
    }

    @Override
    public Colour getValue(Map<String, Object> dependencies) {
        return value;
    }

    @Override
    public Colour newRandomValue() {
        Colour colour = getValue();
        if (colour == null)
            return new Colour(
                    (minVal + Simulation.RANDOM.nextInt(maxVal)) / 255f,
                    (minVal + Simulation.RANDOM.nextInt(maxVal)) / 255f,
                    (minVal + Simulation.RANDOM.nextInt(maxVal)) / 255f,
                    1f
            );

        float p = Simulation.RANDOM.nextFloat();
        float valChange = (-15 + Simulation.RANDOM.nextInt(30)) / 255f;

        if (p < 1 / 3f) {
            float v = MathUtils.clamp(colour.r + valChange, maxVal, minVal);
            return new Colour(v, colour.g, v, 1f);
        } else if (p < 2 / 3f) {
            float v = MathUtils.clamp(colour.g + valChange, maxVal, minVal);
            return new Colour(colour.r, v, colour.b, 1f);
        } else {
            float v = MathUtils.clamp(colour.b + valChange, maxVal, minVal);
            return new Colour(colour.r, colour.g, v, 1f);
        }
    }

    @Override
    public Trait<Colour> createNew(Colour value) {
        return new ProtozoaColourTrait(geneName, value);
    }

    @Override
    public String valueString() {
        Colour value = getValue();
        return value.r + ";" + value.g + ";" + value.b;
    }

    @Override
    public String getTraitName() {
        return geneName;
    }
}