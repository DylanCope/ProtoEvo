package com.protoevo.ui.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.protoevo.biology.cells.Cell;
import com.protoevo.biology.cells.MultiCellStructure;
import com.protoevo.biology.cells.Protozoan;
import com.protoevo.biology.evolution.GeneExpressionFunction;
import com.protoevo.biology.nn.NeuralNetwork;
import com.protoevo.biology.nn.Neuron;
import com.protoevo.biology.nodes.AdhesionReceptor;
import com.protoevo.biology.nodes.SurfaceNode;
import com.protoevo.ui.UIStyle;
import com.protoevo.ui.input.InputLayers;
import com.protoevo.ui.nn.MouseOverNeuronHandler;
import com.protoevo.ui.nn.OnCellNetworkRenderer;
import com.protoevo.utils.Colour;
import space.earlygrey.shapedrawer.ShapeDrawer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MultiCellGRNRenderer {

    private final MultiCellStructure multiCellStructure;
    private final BitmapFont font;
    private final int infoTextSize;
    private final float graphicsHeight, graphicsWidth;

    private final SpriteBatch batch, uiBatch;
    private final OrthographicCamera camera;
    private final Map<Integer, OnCellNetworkRenderer> networkRendererMap;
    private final Map<Neuron, Neuron> interCellNeuronConnections;
    private final ShapeDrawer shapeRenderer;
    private float interCellLineWidth;

    private final Colour.Gradient weightGradient =
            new Colour.Gradient(-1, 1, Colour.RED, Colour.WHITE, Colour.GREEN);

    public MultiCellGRNRenderer(OrthographicCamera camera,
                                InputLayers inputLayers,
                                MultiCellStructure multiCellStructure) {
        this.multiCellStructure = multiCellStructure;

        graphicsHeight = Gdx.graphics.getHeight();
        graphicsWidth = Gdx.graphics.getWidth();
        infoTextSize = (int) (graphicsHeight / 50f);

        font = UIStyle.createFiraCode(infoTextSize);

        batch = new SpriteBatch();
        this.camera = camera;

        networkRendererMap = new HashMap<>();
        List<Cell> cells = multiCellStructure.getCells();
        uiBatch = new SpriteBatch();

        for (int i = 0; i < cells.size(); i++) {
            MouseOverNeuronHandler mouseOverNeuronHandler = new MouseOverNeuronHandler(font);
            networkRendererMap.put(i, new OnCellNetworkRenderer(
                    (Protozoan) cells.get(i), batch, inputLayers, camera, mouseOverNeuronHandler
            ));
        }

        shapeRenderer = new ShapeDrawer(batch, new TextureRegion(UIStyle.getWhite1x1()));
        interCellNeuronConnections = new HashMap<>();
        for (Cell cell : cells) {
            Protozoan protozoan = (Protozoan) cell;
            NeuralNetwork grn = protozoan.getGeneExpressionFunction().getRegulatoryNetwork();
            for (Neuron outputNeuron : grn.getOutputNeurons()) {
                GeneExpressionFunction.Node gene = OnCellNetworkRenderer.getNodeTaggedOnNeuron(protozoan, outputNeuron);
                if (gene != null && gene.getDisplayName().contains("Signature"))
                    continue;
                // finds and adds to interCellNeuronConnections
                findConnectedInputNeuronInOtherCell(protozoan, outputNeuron);
            }
        }

        interCellLineWidth = Float.MAX_VALUE;
        for (OnCellNetworkRenderer networkRenderer : networkRendererMap.values()) {
            interCellLineWidth = Math.min(interCellLineWidth, networkRenderer.getSynapseLineWidth());
        }
    }

    private boolean validOtherCell(Cell cell, Cell other) {
        return cell != other && other instanceof Protozoan && multiCellStructure.getCells().contains(other);
    }

    private void findConnectedInputNeuronInOtherCell(Protozoan protozoan, Neuron outputNeuron) {
        Optional<AdhesionReceptor> maybeReceptor = getPossibleAdhesionReceptor(protozoan, outputNeuron);
        Optional<AdhesionReceptor> maybeOtherReceptor = maybeReceptor.flatMap(AdhesionReceptor::getOtherAdhesionReceptor);
        if (maybeReceptor.isPresent() && maybeOtherReceptor.isPresent()) {
            AdhesionReceptor receptor = maybeReceptor.get();
            AdhesionReceptor otherReceptor = maybeOtherReceptor.get();
            Optional<Cell> otherCell = receptor.getOtherCell();
            if (!otherCell.isPresent() || !otherCell.map(o -> validOtherCell(protozoan, o)).orElse(false))
                return;
            Protozoan otherProtozoan = (Protozoan) otherCell.get();
            NeuralNetwork otherGRN = otherProtozoan.getGeneExpressionFunction().getRegulatoryNetwork();
            for (Neuron otherNeuron : otherGRN.getInputNeurons()) {
                GeneExpressionFunction.Node gene = OnCellNetworkRenderer.getNodeTaggedOnNeuron(otherProtozoan, otherNeuron);
                if (gene != null && gene.getDisplayName().contains("Signature"))
                    continue;
                Optional<AdhesionReceptor> maybeOtherReceptor2 = getPossibleAdhesionReceptor(otherProtozoan, otherNeuron);
                if (maybeOtherReceptor2.isPresent() && maybeOtherReceptor2.get() == otherReceptor) {
                    interCellNeuronConnections.put(outputNeuron, otherNeuron);
                    return;
                }
            }
        }
    }

    public Optional<AdhesionReceptor> getPossibleAdhesionReceptor(Protozoan protozoan, Neuron neuron) {
        GeneExpressionFunction.Node gene = OnCellNetworkRenderer.getNodeTaggedOnNeuron(protozoan, neuron);
        if (gene == null || !(gene.getLastTarget() instanceof SurfaceNode))
            return Optional.empty();
        SurfaceNode node = (SurfaceNode) gene.getLastTarget();
        if (node.getAttachment() instanceof AdhesionReceptor)
            return Optional.of((AdhesionReceptor) node.getAttachment());
        return Optional.empty();
    }

    private void renderInterCellConnections() {
        shapeRenderer.update();

        Colour colour = new Colour();
        for (Neuron outputNeuron : interCellNeuronConnections.keySet()) {
            Neuron inputNeuron = interCellNeuronConnections.get(outputNeuron);
            Vector2 outputNeuronPos = outputNeuron.getGraphicsPos();
            Vector2 inputNeuronPos = inputNeuron.getGraphicsPos();
            Color weightColour = weightGradient.getColour(colour, inputNeuron.getLastState()).getColor();
            shapeRenderer.setColor(weightColour);
            shapeRenderer.line(
                    outputNeuronPos.x, outputNeuronPos.y,
                    inputNeuronPos.x, inputNeuronPos.y,
                    interCellLineWidth);
        }
    }

    public void renderGRNs(float delta) {
        batch.setProjectionMatrix(camera.combined);
        batch.enableBlending();
        camera.update();
        batch.begin();
        renderInterCellConnections();
        for (int i = 0; i < multiCellStructure.getCells().size(); i++) {
            OnCellNetworkRenderer networkRenderer = networkRendererMap.get(i);
            networkRenderer.render(delta);
        }
        batch.end();
        uiBatch.begin();
        for (int i = 0; i < multiCellStructure.getCells().size(); i++) {
            OnCellNetworkRenderer networkRenderer = networkRendererMap.get(i);
            networkRenderer.drawUI(uiBatch);
        }
        uiBatch.end();
    }

    public void dispose() {
        batch.dispose();
        uiBatch.dispose();
        font.dispose();
        for (OnCellNetworkRenderer networkRenderer : networkRendererMap.values()) {
            networkRenderer.dispose();
        }
    }
}
