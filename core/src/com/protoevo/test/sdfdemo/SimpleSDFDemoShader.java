package com.protoevo.test.sdfdemo;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector2;
import com.protoevo.ui.shaders.ShaderLayer;
import com.protoevo.maths.Geometry;

public class SimpleSDFDemoShader extends ShaderLayer {

    private final float[] centresX;
    private final float[] centresY;
    private final float[] radii;
    private final Vector2[] speed;
    private final int nCircles;

    public SimpleSDFDemoShader() {
        super("sdfdemo/simple");

        nCircles = 256;
        centresX = new float[nCircles];
        centresY = new float[nCircles];
        radii = new float[nCircles];
        speed = new Vector2[nCircles];

        float maxSpeed = 0.1f;
        for (int i = 0; i < nCircles; i++) {
            centresX[i] = (float) Math.random();
            centresY[i] = (float) Math.random();
            radii[i] = (float) Math.random() * 0.02f;
            speed[i] = Geometry.randomVector(maxSpeed);
        }
    }

    @Override
    public void update(float delta) {
        for (int i = 0; i < nCircles; i++) {
            centresX[i] += speed[i].x * delta;
            centresY[i] += speed[i].y * delta;

            if (centresX[i] < 0) {
                centresX[i] = 0;
                speed[i].x *= -1;
            } else if (centresX[i] > 1) {
                centresX[i] = 1;
                speed[i].x *= -1;
            }

            if (centresY[i] < 0) {
                centresY[i] = 0;
                speed[i].y *= -1;
            } else if (centresY[i] > 1) {
                centresY[i] = 1;
                speed[i].y *= -1;
            }
        }
    }

    @Override
    public void setShaderUniformVariables(ShaderProgram shaderProgram) {
        float graphicsWidth = Gdx.graphics.getWidth();
        float graphicsHeight = Gdx.graphics.getHeight();
        shaderProgram.setUniformf("resolution", new Vector2(graphicsWidth, graphicsHeight));
        shaderProgram.setUniformf("u_resolution", graphicsWidth, graphicsHeight);
        shaderProgram.setUniformf("u_smoothingK", 0.03f);
        shaderProgram.setUniformi("u_nCircles", nCircles);
        shaderProgram.setUniform1fv("u_centresX", centresX, 0, nCircles);
        shaderProgram.setUniform1fv("u_centresY", centresY, 0, nCircles);
        shaderProgram.setUniform1fv("u_radii", radii, 0, nCircles);
    }
}
