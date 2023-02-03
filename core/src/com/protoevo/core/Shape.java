package com.protoevo.core;


import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.protoevo.utils.Colour;

public interface Shape {

    class Collision {
        public final Vector2 point = new Vector2();
        public boolean didCollide = false;
    }

    boolean pointInside(Vector2 p);
    boolean rayIntersects(Vector2 start, Vector2 end);
    boolean rayCollisions(Vector2[] ray, Collision[] collisions);

    Colour getColour();

    default Color getColor() {
        return getColour().getColor();
    }

    Vector2[] getBoundingBox();

    Vector2 getPos();
}
