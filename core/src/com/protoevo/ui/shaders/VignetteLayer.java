package com.protoevo.ui.shaders;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector2;
import com.protoevo.env.Environment;
import com.protoevo.ui.input.ParticleTracker;

public class VignetteLayer extends ShaderLayer {

    private final ParticleTracker particleTracker;
    private boolean uiHidden;

    public VignetteLayer(OrthographicCamera camera, ParticleTracker particleTracker) {
        super(camera, "vignette");
        this.particleTracker = particleTracker;
    }

    public void setUiHidden(boolean uiHidden) {
        this.uiHidden = uiHidden;
    }

    @Override
    public void setShaderUniformVariables(ShaderProgram shaderProgram) {
        OrthographicCamera camera = getCamera();
        float graphicsWidth = Gdx.graphics.getWidth();
        float graphicsHeight = Gdx.graphics.getHeight();
        shaderProgram.setUniformf("resolution", new Vector2(graphicsWidth, graphicsHeight));
        shaderProgram.setUniformMatrix("u_projTrans", camera.combined);
        shaderProgram.setUniformMatrix("u_projTransInv", camera.invProjectionView);
        shaderProgram.setUniformf("u_resolution", graphicsWidth, graphicsHeight);
        shaderProgram.setUniformi("u_tracking", particleTracker.isTracking() && !uiHidden ? 1 : 0);
        shaderProgram.setUniformf("u_void_dist", Environment.settings.worldgen.voidStartDistance.get());
        shaderProgram.setUniformf("u_cam_pos", camera.position);
    }
}
