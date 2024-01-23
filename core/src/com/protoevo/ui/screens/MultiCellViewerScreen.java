package com.protoevo.ui.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.protoevo.biology.cells.Cell;
import com.protoevo.biology.cells.MultiCellStructure;
import com.protoevo.biology.cells.Protozoan;
import com.protoevo.ui.GraphicsAdapter;
import com.protoevo.ui.TopBar;
import com.protoevo.ui.UIStyle;
import com.protoevo.ui.input.InputLayers;
import com.protoevo.ui.input.PanZoomCameraInput;
import com.protoevo.ui.nn.MultiCellGRNRenderer;
import com.protoevo.ui.rendering.ProtozoaRenderer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultiCellViewerScreen extends ScreenAdapter {

    private final MultiCellStructure multiCellStructure;
    private final TopBar topBar;
    private final Stage stage;
    private final BitmapFont font;

    private final SpriteBatch batch;
    private final OrthographicCamera camera;
    private final Map<Integer, ProtozoaRenderer> renderers;
    private final InputLayers inputLayers;
    private final MultiCellGRNRenderer multiCellGRNRenderer;

    public static OrthographicCamera createNewCamera(MultiCellStructure multiCellStructure) {
        OrthographicCamera camera = new OrthographicCamera();
        float multiCellR = multiCellStructure.computeMultiCellRadius();
        float graphicsHeight = Gdx.graphics.getHeight();
        float graphicsWidth = Gdx.graphics.getWidth();
        camera.setToOrtho(false, multiCellR, multiCellR * graphicsHeight / graphicsWidth);
        Vector2 centre = multiCellStructure.computeMultiCellCentre();
        camera.position.set(centre.x, centre.y, 0);
        camera.zoom = .5f / multiCellR;
        return camera;
    }

    public MultiCellViewerScreen(GraphicsAdapter graphics,
                                 MultiCellStructure multiCellStructure) {
        this(graphics, createNewCamera(multiCellStructure), multiCellStructure);
    }

    public MultiCellViewerScreen(GraphicsAdapter graphics,
                                 OrthographicCamera camera,
                                 MultiCellStructure multiCellStructure) {
        this.multiCellStructure = multiCellStructure;
        stage = new Stage();

        float graphicsHeight = Gdx.graphics.getHeight();
        float graphicsWidth = Gdx.graphics.getWidth();
        int infoTextSize = (int) (graphicsHeight / 50f);

        font = UIStyle.createFiraCode(infoTextSize);
        this.topBar = new TopBar(stage, font.getLineHeight());
        topBar.createRightBarImageButton(
                "icons/back.png",
                graphics::returnToPreviousScreenAndDispose);
        this.camera = camera;
        this.batch = new SpriteBatch();
        PanZoomCameraInput panZoomCameraInput = new PanZoomCameraInput(camera);
        inputLayers = new InputLayers(stage, panZoomCameraInput);

        multiCellGRNRenderer = new MultiCellGRNRenderer(camera, inputLayers, multiCellStructure);

        renderers = new HashMap<>();
        List<Cell> cells = multiCellStructure.getCells();

        for (int i = 0; i < cells.size(); i++) {
            renderers.put(i, new ProtozoaRenderer((Protozoan) cells.get(i)));
        }
    }

    public void renderProtozoa(float delta) {
        batch.setProjectionMatrix(camera.combined);
        batch.enableBlending();
        camera.update();
        batch.begin();
        for (int i = 0; i < multiCellStructure.getCells().size(); i++) {
            ProtozoaRenderer renderer = renderers.get(i);
            renderer.render(delta, camera, batch);
        }
        batch.end();
    }

    @Override
    public void render(float delta) {
        GraphicsAdapter.renderBackground(delta);
        renderProtozoa(delta);
        multiCellGRNRenderer.renderGRNs(delta);
        topBar.draw(delta);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(inputLayers);
    }

    @Override
    public void dispose() {
        font.dispose();
        stage.dispose();
        topBar.dispose();
        multiCellGRNRenderer.dispose();
    }
}
