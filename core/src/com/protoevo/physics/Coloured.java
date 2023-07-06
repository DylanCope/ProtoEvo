package com.protoevo.physics;

import com.badlogic.gdx.graphics.Color;
import com.protoevo.utils.Colour;

public interface Coloured {

    Colour getColour();

    default Color getColor() {
        return getColour().getColor();
    }

}
