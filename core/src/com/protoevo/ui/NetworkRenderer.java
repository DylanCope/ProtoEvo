package com.protoevo.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.protoevo.biology.neat.NeuralNetwork;
import com.protoevo.biology.neat.Neuron;
import com.protoevo.core.Simulation;
import com.protoevo.ui.rendering.Renderer;
import com.protoevo.utils.DebugMode;
import com.protoevo.utils.Utils;

import java.util.Arrays;

public class NetworkRenderer extends InputAdapter implements Renderer {

    private final Simulation simulation;
    private final SimulationScreen simulationScreen;
    private NeuralNetwork nn;
    private final ShapeRenderer shapeRenderer;
    private final SpriteBatch batch;
    private final GlyphLayout layout = new GlyphLayout();
    private final BitmapFont font;

    private float boxXStart, boxYStart, boxWidth, boxHeight, infoTextSize;

    public NetworkRenderer(Simulation simulation, SimulationScreen simulationScreen,
                           float x, float y, float width, float height, int infoTextSize) {
        this.simulation = simulation;
        this.simulationScreen = simulationScreen;
        this.boxXStart = x;
        this.boxYStart = y;
        this.boxWidth = width;
        this.boxHeight = height;
        this.infoTextSize = infoTextSize;
        shapeRenderer = new ShapeRenderer();
        batch = new SpriteBatch();

        font = SimulationScreen.createFiraCode(infoTextSize);
    }

    public void setNeuralNetwork(NeuralNetwork nn) {
        this.nn = nn;
    }

    public void render(float delta) {
        if (nn == null)
            return;

        shapeRenderer.setAutoShapeType(true);
        shapeRenderer.begin();
        shapeRenderer.set(ShapeRenderer.ShapeType.Filled);

        int networkDepth = nn.getDepth();

        if (DebugMode.isDebugMode()) {
            shapeRenderer.setColor(Color.GOLD);
            shapeRenderer.set(ShapeRenderer.ShapeType.Line);
            shapeRenderer.box(boxXStart, boxYStart, 0, boxWidth, boxHeight, 0);
            for (float y = boxYStart; y < boxYStart + boxHeight; y += boxHeight / networkDepth)
                shapeRenderer.line(boxXStart, y, boxXStart + boxWidth, y);
        }

        if (!nn.hasComputedGraphicsPositions())
            precomputeGraphicsPositions(nn, boxXStart, boxYStart, boxWidth, boxHeight);

        float r = nn.getGraphicsNodeSpacing() / 8;
        for (Neuron neuron : nn.getNeurons()) {
            if (!neuron.getType().equals(Neuron.Type.SENSOR) && neuron.isConnectedToOutput()) {

                for (int i = 0; i < neuron.getInputs().length; i++) {
                    Neuron inputNeuron = neuron.getInputs()[i];

                    float weight = inputNeuron.getLastState() * neuron.getWeights()[i];
                    if (Math.abs(weight) <= 1e-4)
                        continue;

                    if (weight > 0) {
                        float p = weight > 1 ? 1 : weight;
                        shapeRenderer.setColor(new Color(
                                (int) (240 - 100 * p) / 255f, 240 / 255f, (int) (255 - 100 * p) / 255f, 1f
                        ));
                    } else if (weight < 0) {
                        float p = weight < -1 ? 1 : -weight;
                        shapeRenderer.setColor(new Color(
                                240 / 255f, (int) (240 - 100 * p) / 255f, (int) (255 - 100 * p) / 255f, 1f
                        ));
                    } else {
                        shapeRenderer.setColor(new Color(240 / 255f, 240 / 255f, 240 / 255f, 1f));
                    }

                    if (neuron == inputNeuron) {
                        shapeRenderer.circle(
                                neuron.getGraphicsX() - r,
                                neuron.getGraphicsY() - r,
                                3*r);
                    }
//                    else if (inputNeuron.getDepth() == neuron.getDepth()) {
//                        float width = boxWidth / (2 * networkDepth);
//                        float height = Math.abs(neuron.getGraphicsY() - inputNeuron.getGraphicsY());
//                        float x = neuron.getGraphicsX() - width / 2;
//                        float y = Math.min(neuron.getGraphicsY(), inputNeuron.getGraphicsY());
//                        g.drawArc(x, y, width, height,-90, 180);
//                    }
                    else {
                        shapeRenderer.line(neuron.getGraphicsX(), neuron.getGraphicsY(),
                                inputNeuron.getGraphicsX(), inputNeuron.getGraphicsY());
                    }
                }
            }
        }

        for (Neuron neuron : nn.getNeurons()) {
            if (!neuron.isConnectedToOutput())
                continue;

            Color colour;
            double state = neuron.getLastState();
            if (state > 0) {
                state = state > 1 ? 1 : state;
                colour = new Color(
                        30 / 255f, (int) (50 + state * 150) / 255f, 30 / 255f, 1f
                );
            } else if (state < 0) {
                state = state < -1 ? -1 : state;
                colour = new Color(
                        (int) (50 - state * 150) / 255f, 30 / 255f, 30 / 255f, 1f
                );
            } else {
                colour = new Color(10 / 255f, 10 / 255f, 10 / 255f, 1f);
            }

            shapeRenderer.setColor(colour);
//            g.fillOval(
//                    neuron.getGraphicsX() - r,
//                    neuron.getGraphicsY() - r,
//                    2*r,
//                    2*r);


//            if (simulation.inDebugMode())
//                if (neuron.getType().equals(Neuron.Type.HIDDEN))
//                    g.setColor(Color.YELLOW.cpy().mul(0.9f));
//                else if (neuron.getType().equals(Neuron.Type.SENSOR))
//                    g.setColor(Color.BLUE.cpy().mul(1.1f));
//                else
//                    g.setColor(Color.WHITE.cpy().mul(0.9f));
//            else {
//                shapeRenderer.setColor(Color.WHITE.cpy().mul(0.9f));
//            }
            if (neuron.getDepth() == networkDepth && neuron.getType().equals(Neuron.Type.HIDDEN))
                shapeRenderer.setColor(new Color(150 / 255f, 30 / 255f, 150 / 255f, 1f));

//            Stroke s = shapeRenderer.getStroke();
//            shapeRenderer.setStroke(new BasicStroke((int) (0.3*r)));

            shapeRenderer.circle(
                    neuron.getGraphicsX(),
                    neuron.getGraphicsY(),
                    r);
        }

        shapeRenderer.end();

        batch.begin();
        batch.setColor(Color.WHITE.cpy().mul(0.9f));
        InputManager inputManager = simulationScreen.getInputManager();
        Vector2 mouse = inputManager.getMousePos();

        if (boxXStart - 2*r < mouse.x && mouse.x < boxXStart + boxWidth + 2*r &&
                boxYStart - 2*r < mouse.y && mouse.y < boxYStart + boxHeight + 2*r) {
            for (Neuron neuron : nn.getNeurons()) {
                float x = neuron.getGraphicsX();
                float y = neuron.getGraphicsY();
//                if (simulation.inDebugMode()) {
//                    shapeRenderer.setColor(Color.YELLOW.darker());
//                    shapeRenderer.drawRect(x - 2*r, y - 2*r, 4*r, 4*r);
//                    shapeRenderer.setColor(Color.RED);
//                    int r2 = r / 5;
//                    shapeRenderer.drawOval(mouseX - r2, mouseY - r2, 2*r2, 2*r2);
//                }
                if (x - 2*r <= mouse.x && mouse.x <= x + 2*r
                        && y - 2*r <= mouse.y && mouse.y <= y + 2*r) {
                    String labelStr;
                    if (neuron.getLabel() != null)
                        labelStr = neuron.getLabel() + " = " +
                                Utils.numberToString(neuron.getLastState(), 2);
                    else
                        labelStr = Utils.numberToString(neuron.getLastState(), 2);

                    layout.setText(font, labelStr);
//                    TextObject label = new TextObject(labelStr, infoTextSize);
                    float labelX = x - layout.width / 2;
                    float pad = infoTextSize * 0.3f;
                    float infoWidth = layout.width + 2*pad;
                    if (labelX + infoWidth >= Gdx.graphics.getWidth())
                        labelX = (int) (Gdx.graphics.getWidth() - 1.1 * infoWidth);

                    float labelY = Gdx.graphics.getHeight() - (y + 1.1f * r);
//                    label.setPosition(new Vector2(labelX, labelY));

//                    shapeRenderer.setColor(Color.BLACK);
//                    shapeRenderer.fillRoundRect(labelX - pad, labelY - 2*pad - label.getHeight() / 2,
//                            infoWidth, label.getHeight() + pad,
//                            pad, pad);
//                    shapeRenderer.setColor(Color.WHITE.darker());
                    font.draw(batch, labelStr, labelX, labelY);
                }
            }
        }
        batch.end();
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

        for (int depth = 0; depth <= networkDepth; depth++) {
            float x = boxXStart + depth * boxWidth / networkDepth;
            int nNodes = depthWidthValues[depth];

            int i = 0;
            for (Neuron n : neurons) {
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

    }

//    @Override
//    public void setPosition(Vector2 pos) {
//        boxXStart = (int) pos.getX();
//        boxYStart = (int) pos.getY();
//    }
//
//    @Override
//    public Vector2 getPosition() {
//        return new Vector2(boxXStart, boxYStart);
//    }
//
//    @Override
//    public int getWidth() {
//        return boxWidth;
//    }
//
//    @Override
//    public int getHeight() {
//        return boxHeight;
//    }
}
