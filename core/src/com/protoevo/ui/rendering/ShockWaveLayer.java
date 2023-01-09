package com.protoevo.ui.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

public class ShockWaveLayer extends ShaderLayer {
    private float time;

    private boolean disabled;

    private Vector3 worldSpaceShockPos;

    static private ShockWaveLayer shockWave;
    private final static float SHOCK_WAVE_TIME = 1f;

    static public ShockWaveLayer getInstance() {
        return shockWave;
    }

    public ShockWaveLayer(OrthographicCamera camera) {
        super(camera, "shockwave");
        shockWave = this;
        disabled = true;
        time = 0;
    }

    @Override
    public void update(float delta) {
        if (!disabled) {
            time += delta;
            if (time > SHOCK_WAVE_TIME) {
                disabled = true;
            }
        }
    }

    public void start(float worldSpacePosX, float worldSpacePosY) {
        System.out.println(worldSpacePosX + " " + worldSpacePosY);
        worldSpaceShockPos = new Vector3(worldSpacePosX, worldSpacePosY, 0);
        disabled = false;
        time = 0;
    }

    @Override
    public void setShaderUniformVariables(ShaderProgram shaderProgram) {
        if (worldSpaceShockPos == null)
            throw new RuntimeException("Attempting to run shockwave shader without setting shockwave position");

        OrthographicCamera camera = getCamera();
        Vector3 viewSpacePos = camera.project(new Vector3(worldSpaceShockPos.x, worldSpaceShockPos.y, 0));
        float graphicsWidth = Gdx.graphics.getWidth();
        float graphicsHeight = Gdx.graphics.getHeight();
        viewSpacePos.x = viewSpacePos.x / graphicsWidth;
        viewSpacePos.y = viewSpacePos.y / graphicsHeight;
        System.out.println("viewSpacePos: " + viewSpacePos);
        System.out.println("worldSpaceShockPos: " + worldSpaceShockPos);
        shaderProgram.setUniformf("cameraZoom", camera.zoom);
        shaderProgram.setUniformf("resolution", new Vector2(graphicsWidth, graphicsHeight));
        shaderProgram.setUniformf("time", time / SHOCK_WAVE_TIME);
        shaderProgram.setUniformf("center", new Vector2(viewSpacePos.x, viewSpacePos.y));
    }

    @Override
    public boolean isEnabled(){
        return !disabled;
    }
}
