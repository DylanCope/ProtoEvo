package com.protoevo.utils;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

public class Utils {

    public static String numberToString(float d, int dp) {
        float ten = (float) Math.pow(10, dp);
        float v = ((int) (d * ten)) / ten;
        if ((int) v == v)
            return Integer.toString((int) v);
        else
            return Float.toString(v);
    }

    public static double getTimeSeconds() {
        return System.currentTimeMillis() / 1000.0;
    }

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

    public static float linearRemap(float v, float vStart, float vEnd, float outStart, float outEnd) {
        float value = outStart + (outEnd - outStart) * ((v - vStart) / (vEnd - vStart));
        if (outStart < outEnd) {
            return MathUtils.clamp(value, outStart, outEnd);
        } else {
            return MathUtils.clamp(value, outEnd, outStart);
        }
    }

    public static String superscript(String str) {
        str = str.replaceAll("0", "⁰");
        str = str.replaceAll("1", "¹");
        str = str.replaceAll("2", "²");
        str = str.replaceAll("3", "³");
        str = str.replaceAll("4", "⁴");
        str = str.replaceAll("5", "⁵");
        str = str.replaceAll("6", "⁶");
        str = str.replaceAll("7", "⁷");
        str = str.replaceAll("8", "⁸");
        str = str.replaceAll("9", "⁹");
        return str;
    }

    public static String subscript(String str) {
        str = str.replaceAll("0", "₀");
        str = str.replaceAll("1", "₁");
        str = str.replaceAll("2", "₂");
        str = str.replaceAll("3", "₃");
        str = str.replaceAll("4", "₄");
        str = str.replaceAll("5", "₅");
        str = str.replaceAll("6", "₆");
        str = str.replaceAll("7", "₇");
        str = str.replaceAll("8", "₈");
        str = str.replaceAll("9", "₉");
        return str;
    }
}
