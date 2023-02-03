package com.protoevo.utils;

import com.badlogic.gdx.graphics.Color;

import java.io.Serial;
import java.io.Serializable;

public class Colour implements Serializable {

    public static Colour WHITE = new Colour(1, 1, 1, 1);

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

    public Colour lerp(Colour c, float t) {
        this.r = (1 - t) * this.r + t * c.r;
        this.g = (1 - t) * this.g + t * c.g;
        this.b = (1 - t) * this.b + t * c.b;
        this.a = (1 - t) * this.a + t * c.a;
        return this;
    }
}
