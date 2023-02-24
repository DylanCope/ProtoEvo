package com.protoevo.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class TopBar {

    private final SimulationScreen simulationScreen;
    private final List<Actor> leftActors;
    private final List<Actor> rightActors;
    private final float topBarHeight;
    private final float topBarButtonSize;
    private final float topBarPadding = 10f;
    private final ShapeRenderer shapeRenderer;

    public TopBar(SimulationScreen simulationScreen, float fontSize) {
        this.simulationScreen = simulationScreen;
        shapeRenderer = new ShapeRenderer();
        leftActors = new ArrayList<>();
        rightActors = new ArrayList<>();
        topBarHeight = fontSize + 4 * topBarPadding;
        topBarButtonSize = 0.8f * (topBarHeight - 2 * topBarPadding);
    }

    public float getButtonSize() {
        return topBarButtonSize;
    }

    public float getPadding() {
        return topBarPadding;
    }

    public float getHeight() {
        return topBarHeight;
    }

    public Vector2 nextLeftButtonPosition() {

        float x;
        if (leftActors.size() > 0) {
            Actor lastActor = leftActors.get(leftActors.size() - 1);
            x = lastActor.getX() + lastActor.getWidth() + 1.5f * topBarPadding;
        } else {
             x = 2 * topBarPadding;
        }

        float y = Gdx.graphics.getHeight() - topBarButtonSize - (topBarHeight - topBarButtonSize) / 2f;

        return new Vector2(x, y);
    }

    public void addLeft(ImageButton button) {
        Vector2 nextLeft = nextLeftButtonPosition();
        button.setPosition(nextLeft.x, nextLeft.y);
        leftActors.add(button);
        simulationScreen.getStage().addActor(button);
    }

    public Vector2 nextRightButtonPosition() {
        float x;
        if (rightActors.size() > 0) {
            Actor lastActor = rightActors.get(rightActors.size() - 1);
            x = lastActor.getX() - 1.5f * topBarPadding;
        } else {
            x = Gdx.graphics.getWidth() - 2 * topBarPadding;
        }

        float y = Gdx.graphics.getHeight() - topBarButtonSize - (topBarHeight - topBarButtonSize) / 2f;

        return new Vector2(x, y);
    }

    public void addRight(ImageButton button) {
        Vector2 nextRight = nextRightButtonPosition();
        button.setPosition(nextRight.x - button.getWidth(), nextRight.y);
        rightActors.add(button);
        simulationScreen.getStage().addActor(button);
    }

    public void draw(float delta) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0, 0, 0, 0.35f);
        shapeRenderer.box(0, Gdx.graphics.getHeight() - topBarHeight, 0, Gdx.graphics.getWidth(),
                topBarHeight, 0);
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    public boolean pointOnBar(float x, float y) {
        return y < topBarHeight;
    }

    public void dispose() {
        shapeRenderer.dispose();
    }
}
