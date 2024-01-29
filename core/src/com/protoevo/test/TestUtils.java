package com.protoevo.test;

import com.protoevo.maths.Functions;
import org.junit.Test;

public class TestUtils {

    @Test
    public void testCyclicalRemap() {
        float inMin = -3;
        float inMax = 3;
        float outMin = 0;
        float outMax = 1;
        int samples = 20;
        float inStep = (inMax - inMin) / samples;
        float outStep = (outMax - outMin) / samples;
        float lastOut = outMin;
        for (float in = 2*inMin; in < 2*inMax; in += inStep) {
            float out = Functions.cyclicalLinearRemap(in, inMin, inMax, outMin, outMax);
            assert out >= outMin && out <= outMax;
            assert Math.abs(out - lastOut) - outStep <= 1e9f;
            lastOut = out;
        }
    }
}
