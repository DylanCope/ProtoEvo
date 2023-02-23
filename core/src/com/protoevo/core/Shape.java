package com.protoevo.core;


import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.protoevo.utils.Colour;


import java.io.Serializable;

public interface Shape {

    class Intersection implements Serializable {
        
        private static final long serialVersionUID = 1L;

        public final Vector2 point = new Vector2();
        public boolean didCollide = false;
    }

    boolean pointInside(Vector2 p);
    boolean rayIntersects(Vector2 start, Vector2 end);
    boolean rayCollisions(Vector2[] ray, Intersection[] intersections);

    Colour getColour();

    default Color getColor() {
        return getColour().getColor();
    }

    Vector2[] getBoundingBox();

    Vector2 getPos();
}
