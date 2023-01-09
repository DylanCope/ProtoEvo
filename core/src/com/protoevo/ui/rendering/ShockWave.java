package com.protoevo.ui.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Group;

public class ShockWave extends Group {

    private final FrameBuffer fbo;
    private final ShaderProgram shaderProgram;
    private float time;


    private boolean disabled;

    private Vector3 shockWavePos;
    private OrthographicCamera camera;
    private EnvRenderer envRenderer;
    private SpriteBatch batch;

    static private ShockWave shockWave;

    static public ShockWave getInstance() {
        if (shockWave == null) {
            shockWave = new ShockWave();
        }
        return shockWave;
    }

    private ShockWave() {
        disabled = true;
        time = 0;
        String vertexShader = Gdx.files.internal("shaders/shock_wave/vertex.glsl").readString();
        String fragmentShader = Gdx.files.internal("shaders/shock_wave/fragment.glsl").readString();
        shaderProgram = new ShaderProgram(vertexShader, fragmentShader);

        fbo = new FrameBuffer(
                Pixmap.Format.RGBA8888,
                Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), false);

        batch = new SpriteBatch();
    }

    public boolean isEnabled(){
        return !disabled;
    }

    public void setCamera(OrthographicCamera camera) {
        this.camera = camera;
    }

    public void setRenderer(EnvRenderer envRenderer) {
        this.envRenderer = envRenderer;
    }

    public void start(float x, float y) {
        this.shockWavePos = new Vector3(x, y, 0);
        disabled = false;
        time = 0;
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        if (!disabled) {
            time += delta;
            if (time > 1) {
                disabled = true;
            }
        }
    }

    public Vector2 getShockWavePos(){
        return new Vector2(shockWavePos.x, shockWavePos.y);
    }

    public ShaderProgram getShaderProgram(){
        return shaderProgram;
    }

    public float getTime(){
        return time;
    }

    public void render(float delta) {
        if (!disabled) {
//            fbo.begin();
//            renderer.render(delta);
//            fbo.end();
//
//            Vector3 viewSpacePos = camera.unproject(shockWavePos);
//            shaderProgram.bind();
//            shaderProgram.setUniformf("time", time);
//            shaderProgram.setUniformf("center", new Vector2(shockWavePos.x / camera.viewportWidth, shockWavePos.y / camera.viewportHeight));
//            batch.setShader(shaderProgram);
//
//            batch.begin();
//            TextureRegion textureRegion = new TextureRegion(fbo.getColorBufferTexture());
//            textureRegion.flip(false, true);
//            batch.draw(textureRegion, 0, 0, camera.viewportWidth, camera.viewportHeight);
//            batch.end();

//            batch.setShader(null);
        }
    }

    public void dispose() {
        fbo.dispose();
        shaderProgram.dispose();
        batch.dispose();
    }
}
