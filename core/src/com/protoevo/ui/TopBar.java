package com.protoevo.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;

import java.util.ArrayList;
import java.util.List;

public class TopBar {

    private UI ui;
    private final List<Actor> leftActors;
    private final List<Actor> rightActors;
    private float topBarHeight;
    private float topBarButtonSize;
    private final float topBarPadding = 10f;
    private final ShapeRenderer shapeRenderer;

    public TopBar(UI ui, float fontSize) {
        this.ui = ui;
        shapeRenderer = new ShapeRenderer();
        leftActors = new ArrayList<>();
        rightActors = new ArrayList<>();
        topBarHeight = fontSize + 4 * topBarPadding;
        topBarButtonSize = topBarHeight - 2 * topBarPadding;
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

        float y = ui.getCamera().viewportHeight - topBarButtonSize - (topBarHeight - topBarButtonSize) / 2f;

        return new Vector2(x, y);
    }

    public void addLeft(ImageButton button) {
        Vector2 nextLeft = nextLeftButtonPosition();
        button.setPosition(nextLeft.x, nextLeft.y);
        leftActors.add(button);
        ui.getStage().addActor(button);
    }

    public Vector2 nextRightButtonPosition() {
        float x;
        if (rightActors.size() > 0) {
            Actor lastActor = rightActors.get(rightActors.size() - 1);
            x = lastActor.getX() - 1.5f * topBarPadding;
        } else {
            x = ui.getCamera().viewportWidth - 2 * topBarPadding;
        }

        float y = ui.getCamera().viewportHeight - topBarButtonSize - (topBarHeight - topBarButtonSize) / 2f;

        return new Vector2(x, y);
    }

    public void addRight(ImageButton button) {
        Vector2 nextRight = nextRightButtonPosition();
        button.setPosition(nextRight.x - button.getWidth(), nextRight.y);
        rightActors.add(button);
        ui.getStage().addActor(button);
    }

    public void draw(float delta) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0, 0, 0, 0.5f);
        shapeRenderer.box(0, ui.getCamera().viewportHeight - topBarHeight, 0, ui.getCamera().viewportWidth,
                topBarHeight, 0);
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    public void dispose() {
        shapeRenderer.dispose();
    }
}
