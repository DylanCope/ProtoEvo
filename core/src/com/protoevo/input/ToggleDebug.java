package com.protoevo.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.protoevo.utils.DebugMode;

public class ToggleDebug extends InputAdapter {

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.F3) {
            if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) {
                DebugMode.cycleDebugMode();
            } else {
                DebugMode.toggleDebug();
            }
        }
        return false;
    }

}
