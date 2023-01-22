package com.protoevo.biology.protozoa;


import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.protoevo.biology.evolution.Trait;
import com.protoevo.core.Simulation;

import java.io.Serializable;
import java.util.Map;

public class ProtozoaColorTrait implements Trait<Color>, Serializable {
    public static final long serialVersionUID = -1821863048303900554L;

    private final Color value;
    private final String geneName;

    private final int minVal = 80;
    private final int maxVal = 150;

    public ProtozoaColorTrait(String geneName) {
        this.geneName = geneName;
        value = newRandomValue();
    }

    public ProtozoaColorTrait(String geneName, Color value) {
        this.geneName = geneName;
        this.value = value;
    }

    @Override
    public Color getValue(Map<String, Object> dependencies) {
        return value;
    }

    @Override
    public Color newRandomValue() {
        Color color = getValue();
        if (color == null)
            return new Color(
                    (minVal + Simulation.RANDOM.nextInt(maxVal)) / 255f,
                    (minVal + Simulation.RANDOM.nextInt(maxVal)) / 255f,
                    (minVal + Simulation.RANDOM.nextInt(maxVal)) / 255f,
                    1f
            );

        float p = Simulation.RANDOM.nextFloat();
        float valChange = (-15 + Simulation.RANDOM.nextInt(30)) / 255f;

        if (p < 1 / 3f) {
            float v = MathUtils.clamp(color.r + valChange, maxVal, minVal);
            return new Color(v, color.g, v, 1f);
        } else if (p < 2 / 3f) {
            float v = MathUtils.clamp(color.g + valChange, maxVal, minVal);
            return new Color(color.r, v, color.b, 1f);
        } else {
            float v = MathUtils.clamp(color.b + valChange, maxVal, minVal);
            return new Color(color.r, color.g, v, 1f);
        }
    }

    @Override
    public Trait<Color> createNew(Color value) {
        return new ProtozoaColorTrait(geneName, value);
    }

    @Override
    public String valueString() {
        Color value = getValue();
        return value.r + ";" + value.g + ";" + value.b;
    }

    @Override
    public String getTraitName() {
        return geneName;
    }
}