package com.protoevo.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.protoevo.core.ApplicationManager;
import com.protoevo.ui.rendering.EnvironmentRenderer;

public class LoadingScreen extends ScreenAdapter {

    private final ApplicationManager applicationManager;
    private final GraphicsAdapter graphicsAdapter;
    private final SpriteBatch batch;
    private float elapsedTime = 0;
    private BitmapFont font;
    private volatile boolean simulationReady = false;
    private int updateCounts;
    private static final int updatesBeforeRendering = 50;

    public LoadingScreen(GraphicsAdapter graphicsAdapter,
                         ApplicationManager applicationManager) {
        this.graphicsAdapter = graphicsAdapter;
        this.applicationManager = applicationManager;
        batch = graphicsAdapter.getSpriteBatch();
    }

    @Override
    public void show() {
        simulationReady = false;
        updateCounts = 0;
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        font = graphicsAdapter.getSkin().getFont("default");
    }

    public void loadingString(String text) {
        ScreenUtils.clear(EnvironmentRenderer.backgroundColor);
        elapsedTime += Gdx.graphics.getDeltaTime();

        batch.begin();
        StringBuilder textWithDots = new StringBuilder(text);
        for (int i = 0; i < Math.max(0, (int) (elapsedTime * 2) % 4); i++)
            textWithDots.append(".");

        float x = 3 * font.getLineHeight();
        font.draw(batch, textWithDots.toString(), x, x);
        batch.end();
    }

    @Override
    public void render(float delta) {
        if (simulationReady) {
            applicationManager.update();
            updateCounts++;
        }
        if (updateCounts > updatesBeforeRendering) {
            graphicsAdapter.setSimulationScreen();
        }
        loadingString("Loading Simulation");
    }

    public void notifySimulationReady() {
        simulationReady = true;
    }
}
