package com.protoevo.test.sdfdemo;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector2;
import com.protoevo.physics.Particle;
import com.protoevo.ui.shaders.ShaderLayer;

import java.util.*;

public class SDFCellSegmentsDemoShader extends ShaderLayer {

    private final float[] centresX;
    private final float[] centresY;
    private final float[] radii;
    private final List<Long> particleIDs;
    private final float[] particleConnections;  // only floats allows, but actually ints
    private static final int maxParticles = 16;
    private int nParticles = 0;
    private int nConnections = 0;
    private final Set<Long> alreadyAccountedFor;
    private DeformableCell cell;
    private final OrthographicCamera camera;
    private final static float smoothingFactor = 1.25f;

    public SDFCellSegmentsDemoShader(OrthographicCamera camera) {
        super("sdfdemo/segmentedcells");

        this.camera = camera;

        centresX = new float[maxParticles];
        centresY = new float[maxParticles];
        radii = new float[maxParticles];
        particleConnections = new float[maxParticles * maxParticles];
        particleIDs = new ArrayList<>();
        alreadyAccountedFor = new HashSet<>();
    }

    public void setCell(DeformableCell cell) {
        this.cell = cell;
    }

    @Override
    public void update(float delta) {
        List<Particle> particles = cell.getParticles();
        nParticles = particles.size();

        particleIDs.clear();
        for (Particle particle : particles) {
            if (particle.isDead())
                continue;
            particleIDs.add(particle.getId());
        }

        nConnections = 0;
        alreadyAccountedFor.clear();
        for (int i = 0; i < nParticles; i++) {
            Particle particle = particles.get(i);
            if (particle.isDead())
                continue;
            Map<Long, Long> joinings = particle.getJoiningIds();
            alreadyAccountedFor.add(particle.getId());
            for (long joinedParticleID : joinings.keySet()) {
                if (!alreadyAccountedFor.contains(joinedParticleID) && particleIDs.contains(joinedParticleID)) {
                    particleConnections[2 * nConnections] = (float) particleIDs.indexOf(particle.getId());
                    particleConnections[2 * nConnections + 1] = (float) particleIDs.indexOf(joinedParticleID);
                    nConnections += 1;
                }
            }
            Vector2 pos = particle.getPos();
            centresX[i] = pos.x;
            centresY[i] = pos.y;
            radii[i] = particle.getRadius();
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
        shaderProgram.setUniformi("u_nConnections", nConnections);
        shaderProgram.setUniform1fv("u_connections", particleConnections, 0, 2*nConnections);

        // Camera uniforms
        shaderProgram.setUniformMatrix("u_projTrans", camera.combined);
        shaderProgram.setUniformMatrix("u_projTransInv", camera.invProjectionView);
        shaderProgram.setUniformf("u_cam_pos", camera.position);
        shaderProgram.setUniformf("u_cam_zoom", camera.zoom);
    }
}
