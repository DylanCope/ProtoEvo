package com.protoevo.utils;

import com.badlogic.gdx.graphics.Color;

import java.io.Serial;
import java.io.Serializable;

public class Colour implements Serializable {

    public static class Gradient {

        public Colour[] colours;
        public float[] positions;

        public Gradient(Colour...colours) {
            this.colours = colours;
            positions = new float[colours.length];
            for (int i = 0; i < colours.length; i++) {
                positions[i] = (float) i / (colours.length - 1);
            }
        }

        public Gradient(float min, float max, Colour...colours) {
            this(colours);
            remap(min, max);
        }

        public Gradient(Colour[] colours, float[] positions) {
            this.colours = colours;
            this.positions = positions;
        }

        public Colour getColour(Colour output, float position) {
            if (position <= positions[0])
                return output.set(colours[0]);
            if (position >= positions[positions.length - 1])
                return output.set(colours[colours.length - 1]);
            for (int i = 0; i < positions.length - 1; i++) {
                if (positions[i] <= position && position <= positions[i + 1]) {
                    float t = (position - positions[i]) / (positions[i + 1] - positions[i]);
                    return output.set(colours[i]).lerp(colours[i + 1], t);
                }
            }
            throw new RuntimeException("Unable to produce a colour for position " + position + " in gradient");
        }

        public void remap(float min, float max) {
            float currMin = positions[0];
            float currMax = positions[positions.length - 1];
            for (int i = 0; i < positions.length; i++) {
                positions[i] = Utils.linearRemap(positions[i], currMin, currMax, min, max);
            }
        }
    }

    public static Colour WHITE = new Colour(1, 1, 1, 1);
    public static Colour BLACK = new Colour(0, 0, 0, 1);
    public static Colour RED = new Colour(1, 0, 0, 1);
    public static Colour GREEN = new Colour(0, 1, 0, 1);
    public static Colour BLUE = new Colour(0, 0, 1, 1);
    public static Colour YELLOW = new Colour(1, 1, 0, 1);
    public static Colour CYAN = new Colour(0, 1, 1, 1);
    public static Colour MAGENTA = new Colour(1, 0, 1, 1);
    public static Colour GRAY = new Colour(0.5f, 0.5f, 0.5f, 1);
    public static Colour LIGHT_GRAY = new Colour(0.75f, 0.75f, 0.75f, 1);
    public static Colour DARK_GRAY = new Colour(0.25f, 0.25f, 0.25f, 1);

    @Serial
    private static final long serialVersionUID = 1L;

    public float r, g, b, a;
    private transient Color color = new Color();

    public Colour(float r, float g, float b, float a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    public Colour(float r, float g, float b) {
        this(r, g, b, 1);
    }

    public Colour() {
        this(0, 0, 0, 0);
    }

    public Colour(Colour c) {
        this(c.r, c.g, c.b, c.a);
    }

    public Colour(Color c) {
        this(c.r, c.g, c.b, c.a);
    }

    public Color getColor() {
        if (color == null)
            color = new Color();
        return color.set(r, g, b, a);
    }

    public Colour set(Colour c) {
        this.r = c.r;
        this.g = c.g;
        this.b = c.b;
        this.a = c.a;
        return this;
    }

    public Color set(Color c) {
        c.r = r;
        c.g = g;
        c.b = b;
        c.a = a;
        return c;
    }

    public void set(float r, float g, float b, float a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    public Colour lerp(Colour c, float t) {
        this.r = (1 - t) * this.r + t * c.r;
        this.g = (1 - t) * this.g + t * c.g;
        this.b = (1 - t) * this.b + t * c.b;
        this.a = (1 - t) * this.a + t * c.a;
        return this;
    }
}
