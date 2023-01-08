package com.protoevo.core;


import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;

public interface Collidable {

    boolean pointInside(Vector2 p);
    boolean rayIntersects(Vector2 start, Vector2 end);
    Vector2[] rayCollisions(Vector2 start, Vector2 end);

    Color getColor();

    Vector2[] getBoundingBox();
}
