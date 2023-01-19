package com.protoevo.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector3;

public class PanZoomCameraInput extends InputAdapter {
    OrthographicCamera cam;
    Vector3 lastTouch, tmp;

    private boolean panningDisabled = false;

    public PanZoomCameraInput(OrthographicCamera cam) {
        this.cam = cam;
        lastTouch = new Vector3();
        tmp = new Vector3();
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        lastTouch.set(screenX, screenY, 0);
        return false;
    }

    public void setPanningDisabled(boolean panningDisabled) {
        this.panningDisabled = panningDisabled;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (panningDisabled)
            return false;

        if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
            tmp.set(cam.unproject(tmp.set(screenX, screenY, 0)).sub(cam.unproject(lastTouch)));
            tmp.scl(-1f, -1f, 0);
            cam.translate(tmp);
            lastTouch.set(screenX, screenY, 0);
            return true;
        }
        return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        cam.zoom *= amountY > 0 ? 1.05f : 0.95f;
        return false;
    }

}