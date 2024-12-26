package com.protoevo.physics;

import com.badlogic.gdx.math.Vector2;

import java.io.Serializable;

public class Collision implements Serializable {
    public static long serialVersionUID = 1L;

    public Object objA, objB;
    public Vector2 point;

    public Collision() {}

    public Collision(Object objA, Object objB, Vector2 point) {
        this.objA = objA;
        this.objB = objB;
        this.point = point;
    }

    public Object getOther(Object obj) {
        return obj == objA ? objB : objA;
    }
}
