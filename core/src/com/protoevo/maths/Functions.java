package com.protoevo.maths;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.protoevo.utils.Colour;

public class Functions {

    public static Color lerp(Color a, Color b, float t) {
        return new Color(
                a.r + (b.r - a.r) * t,
                a.g + (b.g - a.g) * t,
                a.b + (b.b - a.b) * t,
                a.a + (b.a - a.a) * t
        );
    }

    public static Colour lerp(Colour a, Colour b, float t) {
        return new Colour(
                a.r + (b.r - a.r) * t,
                a.g + (b.g - a.g) * t,
                a.b + (b.b - a.b) * t,
                a.a + (b.a - a.a) * t
        );
    }

    public static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    public static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    public static int lerp(int a, int b, float t) {
        return (int) (a + (b - a) * t);
    }

    public static Vector2 lerp(Vector2 a, Vector2 b, float t) {
        return new Vector2(
                a.x + (b.x - a.x) * t,
                a.y + (b.y - a.y) * t
        );
    }

    public static float clampedLinearRemap(float v, float vStart, float vEnd, float outStart, float outEnd) {
        if (Math.abs(vEnd - vStart) < 1e-12)
            return v <= vStart ? outStart : outEnd;

        float out = outStart + (outEnd - outStart) * ((v - vStart) / (vEnd - vStart));
        if (outStart < outEnd) {
            return MathUtils.clamp(out, outStart, outEnd);
        } else {
            return MathUtils.clamp(out, outEnd, outStart);
        }
    }

    public static float cyclicalLinearRemap(float v, float vStart, float vEnd, float outStart, float outEnd) {
        if (v < vStart || v > vEnd) {
            float vRange = vStart - vEnd;
            v = ((v - vStart) % vRange + vRange) % vRange + vEnd;
        }
        return clampedLinearRemap(v, vStart, vEnd, outStart, outEnd);
    }

    public static float sigmoid(float z) {
        return 1 / (1 + (float) Math.exp(-z));
    }

    public static float linear(float z) {
        return z;
    }

    public static float tanh(float z) {
        return (float) Math.tanh(z);
    }

    public static float step(float z) {
        return z > 0 ? 1f : 0f;
    }

    public static float relu(float z) {
        return z > 0 ? z : 0f;
    }

    public static float gaussian(float z) {
        return (float) Math.exp(-z * z);
    }
}
