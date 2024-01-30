package com.protoevo.test.sdfdemo;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.utils.ScreenUtils;
import com.protoevo.physics.Particle;
import com.protoevo.physics.box2d.Box2DPhysics;
import com.protoevo.ui.UIStyle;
import com.protoevo.ui.input.InputLayers;
import com.protoevo.ui.input.PanZoomCameraInput;
import com.protoevo.ui.input.ToggleDebug;
import com.protoevo.ui.shaders.ShaderLayers;
import com.protoevo.utils.DebugMode;
import space.earlygrey.shapedrawer.ShapeDrawer;

import java.util.ArrayList;
import java.util.List;

public class SDFCellDemoScreen extends ScreenAdapter {

    private final ShaderLayers cellRenderer;
    private final SDFCellSegmentsDemoShader cellShader;
    private final SpriteBatch uiBatch, debugBatch;
    private final BitmapFont debugFont;
    private final List<DeformableCell> cells;
    private final OrthographicCamera camera;
    private final InputLayers inputLayers;
    private final ShapeDrawer shapeDrawer;
    private final Box2DPhysics physics;
    private final Box2DDebugRenderer box2DDebugRenderer;

    public SDFCellDemoScreen() {
        physics = new Box2DPhysics();
        cells = new ArrayList<>();
        float worldR = 5f;
        for (int i = 0; i < 100; i++) {
            cells.add(new DeformableCell(physics, new Vector2(
                    worldR * (float) (Math.random() * 2 - 1),
                    worldR * (float) (Math.random() * 2 - 1)
            )));
        }

        float graphicsWidth = Gdx.graphics.getWidth();
        float graphicsHeight = Gdx.graphics.getHeight();
        camera = new OrthographicCamera();
        float worldRadius = 1f;
        camera.setToOrtho(
                false, worldRadius,
                worldRadius * graphicsHeight / graphicsWidth);
        camera.position.set(0, 0, 0);
        camera.zoom = 1f;

        cellShader = new SDFCellSegmentsDemoShader(camera);
        cellRenderer = new ShaderLayers(
                new SDFDemoRenderer(),
                cellShader
        );
        uiBatch = new SpriteBatch();
        debugFont = UIStyle.createFiraCode(20);

        inputLayers = new InputLayers(
            new PanZoomCameraInput(camera), new ToggleDebug()
        );

        debugBatch = new SpriteBatch();
        shapeDrawer = new ShapeDrawer(debugBatch, new TextureRegion(UIStyle.getWhite1x1()));
        box2DDebugRenderer = new Box2DDebugRenderer();
    }

    private void handleTouchForce() {

        if (Gdx.input.justTouched() && Gdx.input.isButtonPressed(Input.Buttons.MIDDLE)) {
            Vector2 mousePos = inputLayers.getMousePos();
            Vector3 worldSpaceVec3 = camera.unproject(new Vector3(mousePos.x, mousePos.y, 0));
            float explosionX = worldSpaceVec3.x;
            float explosionY = worldSpaceVec3.y;
            float power = 0.02f;
            for (DeformableCell cell : cells) {
                for (Particle particle : cell.getParticles()) {
                    Vector2 bodyPos = particle.getPos();
                    Vector2 tmp = new Vector2(bodyPos.x - explosionX, bodyPos.y - explosionY);
                    float dist2 = tmp.len2();
                    float explosionFallout = 3f;
                    tmp.setLength((float) (power * Math.exp(-explosionFallout * dist2)));
                    particle.applyImpulse(tmp);
                }
            }
        }

    }

    @Override
    public void render(float delta) {
        camera.update();
        shapeDrawer.update();
        for (DeformableCell cell : cells) {
            cell.update(delta);
        }

        handleTouchForce();
        for (DeformableCell cell : cells) {
            cell.physicsUpdate();
        }

        physics.step(delta);
        physics.getJointsManager().flushJoints();

        ScreenUtils.clear(Color.BLACK);
        for (DeformableCell cell : cells) {
            cellShader.setCell(cell);
            cellRenderer.render(delta);
        }

        if (DebugMode.isDebugMode()) {
            renderDebug();
        }
    }

    private void renderDebug() {
        String debugString = "FPS: " + Gdx.graphics.getFramesPerSecond();

        box2DDebugRenderer.render(physics.getWorld(), camera.combined);

        debugBatch.setProjectionMatrix(camera.combined);
        debugBatch.begin();
        shapeDrawer.setColor(Color.GOLD);
        for (DeformableCell cell : cells) {
            for (Particle particle : cell.getParticles()) {
                Vector2 pos = particle.getPos();
                float r = particle.getRadius();
                shapeDrawer.circle(pos.x, pos.y, r, 0.015f * r);
            }
        }
        debugBatch.end();

        uiBatch.begin();
        debugFont.setColor(Color.GOLD);
        float pad = debugFont.getLineHeight() * 0.5f;
        debugFont.draw(uiBatch, debugString, pad, Gdx.graphics.getHeight() - pad);
        uiBatch.end();
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(inputLayers);
    }

    @Override
    public void dispose() {
        cellRenderer.dispose();
        box2DDebugRenderer.dispose();
        uiBatch.dispose();
        debugBatch.dispose();
        debugFont.dispose();
    }
}
