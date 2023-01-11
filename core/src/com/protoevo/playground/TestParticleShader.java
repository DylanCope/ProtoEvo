package com.protoevo.playground;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector2;
import com.protoevo.utils.Geometry;

public class TestParticleShader extends InputAdapter {

    private final SpriteBatch batch;
    private Texture positionsTexture;
    private Texture velocitiesTexture;
    private final ShaderProgram updatePositions, updateVelocities;
    private final FrameBuffer fbo;
    private float time = 0;
    private final float refreshDelay = 0f;
    private int BUFFER_WIDTH = 1920;
    private int BUFFER_HEIGHT = 1080;

    public TestParticleShader() {
        BUFFER_WIDTH = Gdx.graphics.getWidth();
        BUFFER_HEIGHT = Gdx.graphics.getHeight();

        Gdx.input.setInputProcessor(this);

        batch = new SpriteBatch();
        fbo = new FrameBuffer(Pixmap.Format.RGBA8888,
                BUFFER_WIDTH, BUFFER_HEIGHT, false);

        Pixmap pixmap = new Pixmap(BUFFER_WIDTH, BUFFER_HEIGHT, Pixmap.Format.RGBA8888);
        pixmap.setColor(0, 0, 0, 0);
        pixmap.fill();
        pixmap.setColor(1, 1, 1, 1);
        pixmap.fillCircle(BUFFER_WIDTH / 2, BUFFER_HEIGHT / 2, BUFFER_HEIGHT / 100);
        positionsTexture = new Texture(pixmap);

        // initialise velocities texture with random 2D velocities in R, G channels
        Pixmap velPixmap = new Pixmap(BUFFER_WIDTH, BUFFER_HEIGHT, Pixmap.Format.RGBA8888);

        float t = 2f;
        for (int x = 0; x < BUFFER_WIDTH; x++) {
            for (int y = 0; y < BUFFER_HEIGHT; y++) {
                float velX = (float) Math.sin(t * 2 * Math.PI * x / BUFFER_WIDTH);
                float velY = (float) Math.cos(t * 2 * Math.PI * y / BUFFER_HEIGHT);
//                Vector2 vel = Geometry.randomVector(0.1f).add(velX, velY).nor();
                Vector2 vel = new Vector2(velX, velY).nor();
//                vel = Geometry.perp(vel);
                velPixmap.setColor(0.5f + 0.5f * vel.x, 0.5f + 0.5f * vel.y, 0, 1);
                velPixmap.drawPixel(x, y);
            }
        }
        velocitiesTexture = new Texture(velPixmap);

        updatePositions = new ShaderProgram(
                Gdx.files.internal("shaders/test_particle/vertex.glsl").readString(),
                Gdx.files.internal("shaders/test_particle/update_positions_fragment.glsl").readString());
        ShaderProgram.pedantic = false;
        if (!updatePositions.isCompiled()) {
            System.err.println(updatePositions.getLog());
            System.exit(0);
        }

        updateVelocities = new ShaderProgram(
                Gdx.files.internal("shaders/test_particle/vertex.glsl").readString(),
                Gdx.files.internal("shaders/test_particle/update_flows_fragment.glsl").readString());
        ShaderProgram.pedantic = false;
        if (!updateVelocities.isCompiled()) {
            System.err.println(updateVelocities.getLog());
            System.exit(0);
        }
    }

    private Texture updateTexture(Texture current, ShaderProgram program) {
        program.setUniformf("u_resolution", (float) BUFFER_WIDTH, (float) BUFFER_HEIGHT);
        batch.setShader(program);
        batch.enableBlending();

        fbo.begin();
        batch.begin();
        batch.draw(current, 0, 0, current.getWidth(), current.getHeight());
        batch.end();
        fbo.end();

        batch.setShader(null);

        Sprite sprite = new Sprite(fbo.getColorBufferTexture());
        sprite.flip(false, true);
        return sprite.getTexture();
    }

    public void updateVelocities() {
        updateVelocities.bind();

        Gdx.graphics.getGL20().glActiveTexture(GL20.GL_TEXTURE1);
        positionsTexture.bind(1);
        updateVelocities.setUniformi("u_texture_pos", 1);

        Gdx.graphics.getGL20().glActiveTexture(GL20.GL_TEXTURE0);
        velocitiesTexture.bind(0);
        updateVelocities.setUniformi("u_texture_vel", 0);

        velocitiesTexture = updateTexture(velocitiesTexture, updateVelocities);
    }

    private void updatePositions() {
        updatePositions.bind();

        Gdx.graphics.getGL20().glActiveTexture(GL20.GL_TEXTURE1);
        velocitiesTexture.bind(1);
        updatePositions.setUniformi("u_texture_vel", 1);

        Gdx.graphics.getGL20().glActiveTexture(GL20.GL_TEXTURE0);
        positionsTexture.bind(0);
        updatePositions.setUniformi("u_texture_pos", 0);

        positionsTexture = updateTexture(positionsTexture, updatePositions);
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        Pixmap pixmap = new Pixmap(BUFFER_WIDTH, BUFFER_HEIGHT, Pixmap.Format.RGBA8888);
        pixmap.setColor(0, 0, 0, 0);
        pixmap.fill();
        pixmap.setColor(1, 1, 1, 1);
        pixmap.fillCircle(screenX, screenY, BUFFER_HEIGHT / 100);
        positionsTexture = new Texture(pixmap);

        updatePositions();
        return true;
    }

    public void render() {
        time += Gdx.graphics.getDeltaTime();
        if (time > refreshDelay) {
            time = 0;
//            updateVelocities();
        }

        batch.begin();
        batch.setColor(1, 1, 1, 1f);
        batch.draw(positionsTexture, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
//        batch.setColor(1, 1, 1, 0.3f);
//        batch.draw(velocitiesTexture, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.end();
    }
}
