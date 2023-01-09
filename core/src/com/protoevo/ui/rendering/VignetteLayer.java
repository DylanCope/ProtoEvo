package com.protoevo.ui.rendering;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
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
        shaderProgram.setUniformMatrix("u_projTrans", camera.combined);
        shaderProgram.setUniformf("u_resolution", camera.viewportWidth, camera.viewportHeight);
        shaderProgram.setUniformi("u_tracking", particleTracker.isTracking() ? 1 : 0);
    }
}
