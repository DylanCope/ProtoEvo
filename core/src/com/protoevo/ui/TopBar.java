package com.protoevo.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.protoevo.utils.ImageUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class TopBar {

    private final Stage stage;
    private final List<Actor> leftActors;
    private final List<Actor> rightActors;
    private final float topBarHeight;
    private final float topBarButtonSize;
    private final float topBarPadding = 10f;
    private final ShapeRenderer shapeRenderer;
    private final Set<ImageButton> buttons = new java.util.HashSet<>();

    public TopBar(Stage stage, float fontSize) {
        this.stage = stage;
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

    public ImageButton createImageButton(String texturePath, float width, float height, EventListener listener) {
        Texture texture = ImageUtils.getTexture(texturePath);
        Drawable drawable = new TextureRegionDrawable(new TextureRegion(texture));
        ImageButton button = new ImageButton(drawable);
        button.setSize(width, height);
        button.setTouchable(Touchable.enabled);
        button.addListener(listener);
        stage.addActor(button);
        buttons.add(button);
        return button;
    }

    public ImageButton createToggleImageButton(
            String texturePathUp, String texturePathDown, float width, float height) {
        Texture texture1 = ImageUtils.getTexture(texturePathUp);
        Drawable drawable1 = new TextureRegionDrawable(new TextureRegion(texture1));
        Texture texture2 = ImageUtils.getTexture(texturePathDown);
        Drawable drawable2 = new TextureRegionDrawable(new TextureRegion(texture2));

        ImageButton button = new ImageButton(drawable1, drawable2);
        button.setSize(width, height);
        button.setTouchable(Touchable.enabled);
        stage.addActor(button);
        buttons.add(button);
        return button;
    }

    public ImageButton createBarToggleImageButton(String texturePathUp, String texturePathDown, Runnable onTouchToggle) {
        ImageButton button = createToggleImageButton(texturePathUp, texturePathDown, getButtonSize(), getButtonSize());
        button.addListener(event -> {
            if (event.toString().equals("touchDown")) {
//                button.toggle();
                onTouchToggle.run();
            }
            return true;
        });
        return button;
    }

    public void createLeftBarToggleImageButton(String texturePathUp, String texturePathDown, Runnable onTouchToggle) {
        ImageButton button = createBarToggleImageButton(texturePathUp, texturePathDown, onTouchToggle);
        addLeft(button);
    }

    public void createRightBarToggleImageButton(String texturePathUp, String texturePathDown, Runnable onTouchToggle) {
        ImageButton button = createBarToggleImageButton(texturePathUp, texturePathDown, onTouchToggle);
        addRight(button);
    }

    public ImageButton createBarImageButton(String texturePath, Runnable onTouch) {
        return createImageButton(texturePath, getButtonSize(), getButtonSize(), event -> {
            if (event.toString().equals("touchDown")) {
                onTouch.run();
            }
            return true;
        });
    }

    public void createLeftBarImageButton(String texturePath, Runnable onTouch) {
        ImageButton button = createBarImageButton(texturePath, onTouch);
        addLeft(button);
    }

    public void createRightBarImageButton(String texturePath, Runnable onTouch) {
        ImageButton button = createBarImageButton(texturePath, onTouch);
        addRight(button);
    }

    public Vector2 nextLeftPosition() {

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

    public void addLeft(Actor actor) {
        Vector2 nextLeft = nextLeftPosition();
        actor.setPosition(nextLeft.x, nextLeft.y);
        leftActors.add(actor);
        stage.addActor(actor);
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

    public void addRight(Actor actor) {
        Vector2 nextRight = nextRightButtonPosition();
        actor.setPosition(nextRight.x - actor.getWidth(), nextRight.y);
        rightActors.add(actor);
        stage.addActor(actor);
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

//        for (ImageButton button : buttons) {
//            Button.ButtonStyle style = button.getStyle();
//            for (Field field : button.getClass().getDeclaredFields()) {
//                if (field.isAccessible()) {
//                    try {
//                        Object attr = field.get(style);
//                        if (attr instanceof TextureRegionDrawable)
//                            ((TextureRegionDrawable) attr).getRegion().getTexture().dispose();
//                        else if (attr instanceof TextureRegion)
//                            ((TextureRegion) attr).getTexture().dispose();
//                        else if (attr instanceof Texture)
//                            ((Texture) attr).dispose();
//                        else if (attr instanceof Disposable)
//                            ((Disposable) attr).dispose();
//                    } catch (IllegalAccessException e) {
//                        throw new RuntimeException(e);
//                    }
//                }
//            }
//        }
    }
}
