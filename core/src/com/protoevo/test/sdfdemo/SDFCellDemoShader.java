package com.protoevo.test.sdfdemo;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector2;
import com.protoevo.physics.Particle;
import com.protoevo.ui.shaders.ShaderLayer;

public class SDFCellDemoShader extends ShaderLayer {

    private final float[] centresX;
    private final float[] centresY;
    private final float[] radii;
    private final int maxParticles = 16;
    private int nParticles = 0;
    private final DeformableCell cell;
    private final OrthographicCamera camera;
    private float smoothingFactor = 0f;


    public SDFCellDemoShader(OrthographicCamera camera, DeformableCell cell) {
        super("sdfdemo/cell");

        this.camera = camera;
        this.cell = cell;

        centresX = new float[maxParticles];
        centresY = new float[maxParticles];
        radii = new float[maxParticles];
    }

    @Override
    public void update(float delta) {
        int i = 0;
        nParticles = cell.getParticles().size();
        smoothingFactor = 0f;
        for (Particle particle : cell.getParticles()) {
            Vector2 pos = particle.getPos();
            centresX[i] = pos.x;
            centresY[i] = pos.y;
            radii[i] = particle.getRadius();
            smoothingFactor = Math.max(smoothingFactor, 1.2f * particle.getRadius());
            i++;
        }
    }

    @Override
    public void setShaderUniformVariables(ShaderProgram shaderProgram) {
        // Screen resolution
        shaderProgram.setUniformf("u_resolution", Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        // SDF smoothing
        shaderProgram.setUniformf("u_smoothingK", smoothingFactor);

        // Particle uniforms
        shaderProgram.setUniformi("u_nCircles", nParticles);
        shaderProgram.setUniform1fv("u_centresX", centresX, 0, nParticles);
        shaderProgram.setUniform1fv("u_centresY", centresY, 0, nParticles);
        shaderProgram.setUniform1fv("u_radii", radii, 0, nParticles);

        // Camera uniforms
        shaderProgram.setUniformMatrix("u_projTrans", camera.combined);
        shaderProgram.setUniformMatrix("u_projTransInv", camera.invProjectionView);
        shaderProgram.setUniformf("u_cam_pos", camera.position);
        shaderProgram.setUniformf("u_cam_zoom", camera.zoom);
    }
}
