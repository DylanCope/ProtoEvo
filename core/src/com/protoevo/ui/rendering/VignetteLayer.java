package com.protoevo.ui.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector2;
import com.protoevo.core.settings.SimulationSettings;
import com.protoevo.input.ParticleTracker;

public class VignetteLayer extends ShaderLayer {

    ParticleTracker particleTracker;

    public VignetteLayer(OrthographicCamera camera, ParticleTracker particleTracker) {
        super(camera, "vignette");
        this.particleTracker = particleTracker;
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
        shaderProgram.setUniformi("u_tracking", particleTracker.isTracking() ? 1 : 0);
        shaderProgram.setUniformf("u_void_dist", SimulationSettings.voidStartDistance);
        shaderProgram.setUniformf("u_cam_pos", camera.position);
    }
}
