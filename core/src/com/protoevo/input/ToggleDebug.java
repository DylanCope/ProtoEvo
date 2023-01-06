package com.protoevo.input;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;

public class ToggleDebug extends InputAdapter {
    private boolean debug = false;

    public boolean isDebug() {
        return debug;
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.F3) {
            debug = !debug;
            return true;
        }
        return false;
    }

}
