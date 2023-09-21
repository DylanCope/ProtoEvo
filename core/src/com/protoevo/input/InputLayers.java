package com.protoevo.input;

import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class InputLayers implements InputProcessor {

    private final List<InputProcessor> layers;
    private final Vector2 mousePosition = new Vector2();

    public InputLayers(InputProcessor... layers) {
        this.layers = new ArrayList<>();
        this.layers.addAll(Arrays.asList(layers));
    }

    public void addLayers(InputProcessor... layers) {
        this.layers.addAll(Arrays.asList(layers));
    }

    public void addLayer(InputProcessor layer) {
        layers.add(layer);
    }

    @Override
    public boolean keyDown(int keycode) {
        for (InputProcessor layer : layers) {
            if (layer.keyDown(keycode)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        for (InputProcessor layer : layers) {
            if (layer.keyUp(keycode)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean keyTyped(char character) {
        for (InputProcessor layer : layers) {
            if (layer.keyTyped(character)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        mousePosition.set(screenX, screenY);
        for (InputProcessor layer : layers) {
            if (layer.touchDown(screenX, screenY, pointer, button)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        mousePosition.set(screenX, screenY);
        for (InputProcessor layer : layers) {
            if (layer.touchUp(screenX, screenY, pointer, button)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
        return touchUp(screenX, screenY, pointer, button);
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        mousePosition.set(screenX, screenY);
        for (InputProcessor layer : layers) {
            if (layer.touchDragged(screenX, screenY, pointer)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        mousePosition.set(screenX, screenY);
        for (InputProcessor layer : layers) {
            if (layer.mouseMoved(screenX, screenY)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        for (InputProcessor layer : layers) {
            if (layer.scrolled(amountX, amountY)) {
                return true;
            }
        }
        return false;
    }

    public Vector2 getMousePos() {
        return mousePosition;
    }
}
