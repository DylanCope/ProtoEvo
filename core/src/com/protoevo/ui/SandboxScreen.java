package com.protoevo.ui;

import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.utils.ScreenUtils;
import com.protoevo.ui.rendering.EnvironmentRenderer;

public class SandboxScreen extends ScreenAdapter {

    private final GraphicsAdapter graphics;

    public SandboxScreen(GraphicsAdapter graphics) {
        this.graphics = graphics;
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(EnvironmentRenderer.backgroundColor);
    }

    @Override
    public void resize(int width, int height) {
    }

    @Override
    public void show() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
    }
}
