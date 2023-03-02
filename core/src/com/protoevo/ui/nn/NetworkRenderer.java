package com.protoevo.ui.nn;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.protoevo.biology.nn.NeuralNetwork;
import com.protoevo.biology.nn.Neuron;
import com.protoevo.core.Simulation;
import com.protoevo.ui.SimulationInputManager;
import com.protoevo.ui.SimulationScreen;
import com.protoevo.ui.UIStyle;
import com.protoevo.ui.rendering.Renderer;
import com.protoevo.utils.Colour;
import com.protoevo.utils.DebugMode;
import space.earlygrey.shapedrawer.ShapeDrawer;

import java.util.Arrays;
import java.util.TreeMap;

public class NetworkRenderer extends InputAdapter implements Renderer {

    private final Simulation simulation;
    private final SimulationScreen simulationScreen;
    private NeuralNetwork nn;
//    private final ShapeRenderer shapeRenderer;
    private final ShapeDrawer shapeRenderer;
    private final SpriteBatch batch;
    private final BitmapFont font;
    private MouseOverNeuronCallback mouseOverNeuronCallback;
    private final Colour.Gradient weightGradient =
            new Colour.Gradient(-1, 1,
                    Colour.RED, new Colour(1, 1, 1, 0), Colour.GREEN);
    private final Colour.Gradient stateGradient =
            new Colour.Gradient(-1, 1,
                    Colour.RED, Colour.BLACK, Colour.GREEN);

    private float boxXStart, boxYStart, boxWidth, boxHeight, infoTextSize;
    private Neuron mouseOverNeuron = null;

    public NetworkRenderer(Simulation simulation, SimulationScreen simulationScreen,
                           SpriteBatch batch,
                           MouseOverNeuronCallback mouseOverNeuronCallback,
                           float x, float y, float width, float height, int infoTextSize) {
        this.simulation = simulation;
        this.simulationScreen = simulationScreen;
        this.boxXStart = x;
        this.boxYStart = y;
        this.boxWidth = width;
        this.boxHeight = height;
        this.infoTextSize = infoTextSize;
//        shapeRenderer = new ShapeRenderer();
        shapeRenderer = new ShapeDrawer(batch, new TextureRegion(UIStyle.getWhite1x1()));
        this.batch = batch;
        this.mouseOverNeuronCallback = mouseOverNeuronCallback;

        font = UIStyle.createFiraCode(infoTextSize);
    }

    public void setMouseOverNeuronCallback(MouseOverNeuronCallback callback) {
        this.mouseOverNeuronCallback = callback;
    }

    public void setNeuralNetwork(NeuralNetwork nn) {
        this.nn = nn;
    }

    private void findMouseOverNeuron(float neuronR) {
        SimulationInputManager inputManager = simulationScreen.getInputManager();
        Vector2 mouse = inputManager.getMousePos();
        mouseOverNeuron = null;
        float mouseY = Gdx.graphics.getHeight() - mouse.y;
        if (boxXStart - 2* neuronR < mouse.x && mouse.x < boxXStart + boxWidth + 2* neuronR &&
                boxYStart - 2*neuronR < mouseY && mouseY < boxYStart + boxHeight + 2*neuronR) {
            for (Neuron neuron : nn.getNeurons()) {
                float x = neuron.getGraphicsX();
                float y = neuron.getGraphicsY();
                if (x - 2*neuronR <= mouse.x && mouse.x <= x + 2*neuronR
                        && y - 2*neuronR <= mouseY && mouseY <= y + 2*neuronR) {
                    mouseOverNeuron = neuron;
                    return;
                }
            }
        }
    }

    public void render(float delta) {
        if (nn == null)
            return;

        int networkDepth = nn.getDepth();

        if (!nn.hasComputedGraphicsPositions())
            precomputeGraphicsPositions(nn, boxXStart, boxYStart, boxWidth, boxHeight);

        shapeRenderer.update();
        float r = nn.getGraphicsNodeSpacing() / 6;

        findMouseOverNeuron(r);

        Colour colour = new Colour();
        for (Neuron neuron : nn.getNeurons()) {
            if (!neuron.getType().equals(Neuron.Type.SENSOR) && neuron.isConnectedToOutput()) {
                for (int i = 0; i < neuron.getInputs().length; i++) {
                    Neuron inputNeuron = neuron.getInputs()[i];

                    float weight = inputNeuron.getLastState() * neuron.getWeights()[i];
                    if (Float.isNaN(weight))
                        shapeRenderer.setColor(Color.BLUE);
                    else {
                        Color weightColor = weightGradient.getColour(colour, weight).getColor();
                        if (mouseOverNeuron != null) {
                            if ((neuron.equals(mouseOverNeuron) || inputNeuron.equals(mouseOverNeuron)))
                                weightColor.set(weightColor.r, weightColor.g, weightColor.b, 0.5f + 0.5f * weightColor.a);
                            else
                                weightColor.set(weightColor.r, weightColor.g, weightColor.b, 0.1f);
                        }

                        shapeRenderer.setColor(weightColor);
                    }

                    if (neuron == inputNeuron) {
                        shapeRenderer.circle(
                                neuron.getGraphicsX() - r,
                                neuron.getGraphicsY() - r,
                                2*r);
                    }
                    else if (inputNeuron.getDepth() == neuron.getDepth()) {
                        float w = boxWidth / (3 * networkDepth);
                        float h = Math.abs(neuron.getGraphicsY() - inputNeuron.getGraphicsY());
                        float x1 = neuron.getGraphicsX();
                        float y1 = Math.min(neuron.getGraphicsY(), inputNeuron.getGraphicsY());
                        float y = y1 + h / 2;
                        float x = x1 + w / 2 - h*h / (w * 8);
                        float arcR = (float) Math.sqrt((x1 - x)*(x1 - x) + (y1 - y)*(y1 - y));
                        float t = (float) Math.atan2(h / 2, x1 - x);
                        shapeRenderer.arc(x, y, arcR, -t, 2*t);
                    }
                    else {
                        shapeRenderer.line(
                                neuron.getGraphicsX(), neuron.getGraphicsY(),
                                inputNeuron.getGraphicsX(), inputNeuron.getGraphicsY());
                    }
                }
            }
        }

        for (Neuron neuron : nn.getNeurons()) {
            if (!neuron.isConnectedToOutput())
                continue;

            float state = neuron.getLastState();
            Color stateColor;
            if (Float.isNaN(state))
                stateColor = Color.BLUE.cpy();
            else
                stateColor = stateGradient.getColour(colour, state).getColor();

            Color ringColor = Color.WHITE.cpy();

            if (mouseOverNeuron != null &&
                    !(neuron.equals(mouseOverNeuron)
                            || neuron.isInput(mouseOverNeuron) || mouseOverNeuron.isInput(neuron))) {
                float t = 0.75f;
                stateColor.lerp(Color.BLACK, t);
                ringColor.lerp(Color.BLACK, t);
            }

            shapeRenderer.setColor(stateColor);

            shapeRenderer.filledCircle(
                    neuron.getGraphicsX(),
                    neuron.getGraphicsY(),
                    r);

            shapeRenderer.setColor(ringColor);

            shapeRenderer.circle(
                    neuron.getGraphicsX(),
                    neuron.getGraphicsY(),
                    r, (int) (0.3*r));
        }

        batch.setColor(Color.WHITE.cpy().mul(0.9f));
        if (mouseOverNeuronCallback != null && mouseOverNeuron != null) {
            mouseOverNeuronCallback.apply(batch, mouseOverNeuron, r);
        }
    }

    private void precomputeGraphicsPositions(NeuralNetwork nn,
                                             float boxXStart,
                                             float boxYStart,
                                             float boxWidth,
                                             float boxHeight) {
        Neuron[] neurons = nn.getNeurons();
        int networkDepth = nn.calculateDepth();

        int[] depthWidthValues = new int[networkDepth + 1];
        Arrays.fill(depthWidthValues, 0);
        for (Neuron n : neurons)
            if (n.isConnectedToOutput())
                depthWidthValues[n.getDepth()]++;

        int maxWidth = 0;
        for (int width : depthWidthValues)
            maxWidth = Math.max(maxWidth, width);

        float nodeSpacing = boxHeight / maxWidth;
        nn.setGraphicsNodeSpacing(nodeSpacing);

        final TreeMap<String, Neuron> sortedNeurons = new TreeMap<>();
        for (int depth = 0; depth <= networkDepth; depth++) {
            sortedNeurons.clear();
            final int currDepth = depth;
            Arrays.stream(neurons)
                    .filter(n -> n.getDepth() == currDepth && n.isConnectedToOutput())
                    .forEach(n -> sortedNeurons.put(n.getLabel(), n));

            float x = boxXStart + depth * boxWidth / networkDepth;
            int nNodes = depthWidthValues[depth];

            int i = 0;
            for (Neuron n : sortedNeurons.values()) {
                if (n.getDepth() == depth && n.isConnectedToOutput()) {
                    int y = (int) (boxYStart + nodeSpacing / 2f + boxHeight / 2f - (nNodes / 2f - i) * nodeSpacing);
                    n.setGraphicsPosition(x, y);
                    i++;
                }
            }
        }
        nn.setComputedGraphicsPositions(true);
    }

    @Override
    public void dispose() {
        font.dispose();
        shapeRenderer.getRegion().getTexture().dispose();
    }
}
