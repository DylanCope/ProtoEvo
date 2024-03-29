package com.protoevo.ui.nn;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.protoevo.biology.cells.Cell;
import com.protoevo.biology.cells.Protozoan;
import com.protoevo.biology.evolution.GeneExpressionFunction;
import com.protoevo.biology.nn.NeuralNetwork;
import com.protoevo.biology.nn.Neuron;
import com.protoevo.biology.nn.meta.GRNTag;
import com.protoevo.biology.nodes.AdhesionReceptor;
import com.protoevo.biology.nodes.SurfaceNode;
import com.protoevo.physics.Joining;
import com.protoevo.ui.UIStyle;
import com.protoevo.ui.input.InputLayers;
import com.protoevo.utils.Colour;
import com.protoevo.maths.Geometry;
import space.earlygrey.shapedrawer.ShapeDrawer;

import java.util.*;

public class OnCellNetworkRenderer {

    private final Protozoan cell;
    private final Vector2 lastCellPos = new Vector2();
    private float lastCellAngle = 0;
    private final ShapeDrawer shapeRenderer;
    private float neuronMoveSpeedFactor = .05f;

    private final Colour.Gradient weightGradient =
            new Colour.Gradient(-1, 1,
                    Colour.RED, new Colour(1, 1, 1, 0), Colour.GREEN);
    private final Colour.Gradient stateGradient =
            new Colour.Gradient(-1, 1,
                    Colour.RED, Colour.BLACK, Colour.GREEN);

    private final InputLayers inputLayers;
    private final MouseOverNeuronHandler mouseOverNeuronHandler;
    private Neuron mouseOverNeuron = null;
    private final OrthographicCamera camera;
    private final Map<Integer, List<Neuron>> notConnectedNeurons;

    public OnCellNetworkRenderer(Protozoan cell,
                                 SpriteBatch batch,
                                 InputLayers inputLayers,
                                 OrthographicCamera camera,
                                 MouseOverNeuronHandler mouseOverNeuronHandler) {
        this.cell = cell;
        lastCellPos.set(cell.getPos());
        lastCellAngle = cell.getParticle().getAngle();

        this.inputLayers = inputLayers;
        this.mouseOverNeuronHandler = mouseOverNeuronHandler;
        this.camera = camera;

        shapeRenderer = new ShapeDrawer(batch, new TextureRegion(UIStyle.getWhite1x1()));
        notConnectedNeurons = new HashMap<>();

        GeneExpressionFunction geneExpressionFunction = cell.getGeneExpressionFunction();
        NeuralNetwork nn = geneExpressionFunction.getRegulatoryNetwork();
        for (Neuron neuron : nn.getNeurons()) {
            GeneExpressionFunction.Node gene = getNodeTaggedOnNeuron(cell, neuron);
            if (gene != null && gene.getLastTarget() instanceof SurfaceNode) {
                SurfaceNode surfaceNode = (SurfaceNode) gene.getLastTarget();
                computeNeuronSurfaceNodePosition(surfaceNode, gene, neuron);
            } else {
                // set random to start with
                Vector2 randomPos = Geometry.randomPointInCircle(maxInternalNodePos())
                        .add(cell.getPos());
                neuron.setGraphicsPosition(randomPos);
            }

            List<Neuron> notConnected = new ArrayList<>();
            for (Neuron other : nn.getNeurons()) {
                if (other == neuron || Arrays.stream(neuron.getInputs()).anyMatch(n -> n == other))
                    continue;
                notConnected.add(other);
            }
            notConnectedNeurons.put(neuron.getId(), notConnected);
        }
    }

    public void computeNeuronSurfaceNodePosition(SurfaceNode node,
                                                 GeneExpressionFunction.Node geneNode,
                                                 Neuron neuron) {
        Vector2 cellPos = cell.getPos();
        Vector2 nodePos;
        if (node.getAttachment() instanceof AdhesionReceptor) {
            Cell cell = node.getCell();
            AdhesionReceptor receptor = (AdhesionReceptor) node.getAttachment();
            Optional<Joining> joining = cell.getEnv()
                    .flatMap(env -> env.getJointsManager().getJoining(receptor.getJoiningID()));
            nodePos = joining.flatMap(j -> j.getParticleAnchor(cell.getParticle()))
                    .orElse(node.getWorldPosition());
        } else {
            nodePos = node.getWorldPosition();
        }
        Vector2 deltaVec = Geometry.perp(nodePos.cpy().sub(cellPos))
                .setLength(1.5f * getNeuronRadius());

        String displayName = geneNode.getDisplayName();
        String[] parts = displayName.split(SurfaceNode.activationPrefix);
        boolean isInput = parts[0].contains("Input");
        if (isInput) {
            deltaVec.scl(-1);
        }
        if (displayName.contains("Signature")) {
            deltaVec.scl(0.f);
        }

        Vector2 neuronGraphicsPos = nodePos.cpy()
                .sub(cellPos)
                .add(deltaVec)
                .setLength(cell.getRadius() - getNeuronRadius())
                .add(cellPos);

        if (displayName.contains("Signature")) {
            neuronGraphicsPos.add(cellPos.cpy().sub(neuronGraphicsPos).setLength(2.1f*getNeuronRadius()));
        }

        neuron.setGraphicsPosition(neuronGraphicsPos);
    }

    private float maxInternalNodePos() {
        return cell.getRadius() * 0.75f - getNeuronRadius();
    }

    public float getNeuronRadius() {
        NeuralNetwork nn = cell.getGeneExpressionFunction().getRegulatoryNetwork();
        return Math.min(cell.getRadius() * 0.1f, 2f * cell.getRadius() / nn.getSize());
    }

    public float getSynapseLineWidth() {
        return 0.3f * getNeuronRadius();
    }

    public void adjustNeuronPosition(Neuron neuron, float delta) {

        // adjust based on closeness to neighbours
        float neuronR = getNeuronRadius();
        Vector2 neuronPos = neuron.getGraphicsPos();
        float maxDeltaVec = 2f * neuronR;
        float repulsiveFactor = 20f * neuronR * neuronR;

        for (Neuron inputNeuron : neuron.getInputs()) {
            GeneExpressionFunction.Node gene = getNodeTaggedOnNeuron(cell, inputNeuron);
            if (gene != null && gene.getLastTarget() instanceof SurfaceNode)
                continue;

            Vector2 inputPos = inputNeuron.getGraphicsPos();
            float dist = neuronPos.dst(inputPos);
            Vector2 deltaVec = inputPos.cpy()
                    .sub(neuronPos)
                    .setLength(delta * neuronMoveSpeedFactor);
            if (dist > 2.5f * neuronR) {
                deltaVec.scl(dist);
                deltaVec.setLength(Math.min(maxDeltaVec, deltaVec.len()));
                neuronPos.add(deltaVec);
                inputPos.sub(deltaVec);
            }
            else {
                deltaVec.scl(repulsiveFactor / (0.5f * neuronR + dist));
                deltaVec.setLength(Math.min(maxDeltaVec, deltaVec.len()));
                neuronPos.sub(deltaVec);
                inputPos.add(deltaVec);
            }
        }

        for (Neuron other : notConnectedNeurons.get(neuron.getId())) {
            GeneExpressionFunction.Node gene = getNodeTaggedOnNeuron(cell, other);
            if (gene != null && gene.getLastTarget() instanceof SurfaceNode)
                continue;

            Vector2 otherPos = other.getGraphicsPos();
            float dist = neuronPos.dst(otherPos);
            if (dist <= getNeuronRadius() * 5) {
                Vector2 deltaVec = otherPos.cpy()
                        .sub(neuronPos)
                        .setLength(delta * repulsiveFactor * neuronMoveSpeedFactor / (0.5f * neuronR + dist));
                deltaVec.setLength(Math.min(maxDeltaVec, deltaVec.len()));
                neuronPos.sub(deltaVec);
                otherPos.add(deltaVec);
            }
        }
    }

    private void findMouseOverNeuron(float neuronR) {
        Vector2 mouseScreenSpace = inputLayers.getMousePos();
        Vector3 worldSpaceMouse = camera.unproject(new Vector3(mouseScreenSpace.x, mouseScreenSpace.y, 0));
//        Vector2 mousePos = new Vector2();
        mouseOverNeuron = null;
        GeneExpressionFunction geneExpressionFunction = cell.getGeneExpressionFunction();
        NeuralNetwork nn = geneExpressionFunction.getRegulatoryNetwork();
        for (Neuron neuron : nn.getNeurons()) {
            Vector2 neuronPos = neuron.getGraphicsPos();
            if (neuronPos.dst(worldSpaceMouse.x, worldSpaceMouse.y) <= 1.1f * getNeuronRadius()) {
                mouseOverNeuron = neuron;
                mouseOverNeuronHandler.setGeneExpressionFunction(geneExpressionFunction);
                return;
            }
        }
    }

    public static GeneExpressionFunction.Node getNodeTaggedOnNeuron(Protozoan protozoan, Neuron neuron) {
        Object[] tags = neuron.getTags();
        if (tags != null && tags.length > 0 && tags[0] instanceof GRNTag) {
            Object tagged = ((GRNTag) tags[0]).apply(protozoan.getGeneExpressionFunction());
            if (tagged instanceof GeneExpressionFunction.Node)
                return (GeneExpressionFunction.Node) tagged;
        }
        return null;
    }

    public void updateNeuronPositions(float delta) {
        GeneExpressionFunction geneExpressionFunction = cell.getGeneExpressionFunction();
        NeuralNetwork nn = geneExpressionFunction.getRegulatoryNetwork();
        List<Vector2> currentPositions = new ArrayList<>();
        for (Neuron neuron : nn.getNeurons()) {
            currentPositions.add(neuron.getGraphicsPos().cpy());
        }

        Vector2 cellPosDelta = cell.getPos().cpy().sub(lastCellPos);
        lastCellPos.set(cell.getPos());
        float cellAngleDelta = cell.getParticle().getAngle() - lastCellAngle;
        lastCellAngle = cell.getParticle().getAngle();

        for (Neuron neuron : nn.getNeurons()) {
            GeneExpressionFunction.Node gene = getNodeTaggedOnNeuron(cell, neuron);
            if (gene != null && gene.getLastTarget() instanceof SurfaceNode) {
                SurfaceNode surfaceNode = (SurfaceNode) gene.getLastTarget();
                computeNeuronSurfaceNodePosition(surfaceNode, gene, neuron);
            } else {
                neuron.getGraphicsPos()
                        .add(cellPosDelta)
                        .rotateAroundRad(cell.getPos(), cellAngleDelta);
                adjustNeuronPosition(neuron, delta);
            }
        }

        for (int i = 0; i < nn.getSize(); i++) {
            Neuron neuron = nn.getNeurons().get(i);
            Vector2 newPos = neuron.getGraphicsPos();
            Vector2 oldPos = currentPositions.get(i);
            if (newPos.dst(oldPos) <= getNeuronRadius() / 20f) {
                newPos.set(oldPos);
            }
        }

        // after all adjustments are made
        for (Neuron neuron : nn.getNeurons()) {
            GeneExpressionFunction.Node gene = getNodeTaggedOnNeuron(cell, neuron);
            if (!(gene != null && gene.getLastTarget() instanceof SurfaceNode)) {
                if (neuron.getGraphicsPos().dst(cell.getPos()) > maxInternalNodePos()) {
                    neuron.getGraphicsPos()
                            .sub(cell.getPos())
                            .setLength(maxInternalNodePos())
                            .add(cell.getPos());
                }
            }
        }
    }

    private boolean isUnusedSurfaceNode(Neuron neuron) {
        GeneExpressionFunction.Node gene = getNodeTaggedOnNeuron(cell, neuron);
        if (gene != null && gene.getLastTarget() instanceof SurfaceNode) {
            SurfaceNode surfaceNode = (SurfaceNode) gene.getLastTarget();
            return surfaceNode.getAttachment() == null;
        }
        return false;
    }

    public void drawWeights() {
        NeuralNetwork nn = cell.getGeneExpressionFunction().getRegulatoryNetwork();
        float r = getNeuronRadius();
        float lineWidth = getSynapseLineWidth();

        Colour colour = new Colour();
        for (Neuron neuron : nn.getNeurons()) {

            if (!neuron.isGraphicsEnabled())
                continue;

            if (!neuron.getType().equals(Neuron.Type.SENSOR) && neuron.isConnectedToOutput()) {
                for (int i = 0; i < neuron.getInputs().length; i++) {
                    Neuron inputNeuron = neuron.getInputs()[i];

                    if (!inputNeuron.isGraphicsEnabled())
                        continue;

                    float weight = inputNeuron.getLastState() * neuron.getWeights()[i];
                    if (Float.isNaN(weight))
                        shapeRenderer.setColor(Color.BLUE);
                    else {
                        Color weightColor = weightGradient.getColour(colour, weight).getColor();
                        if (mouseOverNeuron != null) {
                            if ((neuron.equals(mouseOverNeuron) || inputNeuron.equals(mouseOverNeuron)))
                                weightColor.set(weightColor.r, weightColor.g, weightColor.b,
                                        0.5f + 0.5f * weightColor.a);
                            else
                                weightColor.set(weightColor.r, weightColor.g, weightColor.b, 0.1f);
                        }

                        shapeRenderer.setColor(weightColor);
                    }

                    if (neuron == inputNeuron) {
                        shapeRenderer.circle(
                                neuron.getGraphicsX() - r,
                                neuron.getGraphicsY() - r,
                                2*r, lineWidth);
                    }
                    else {
                        shapeRenderer.line(
                                neuron.getGraphicsX(), neuron.getGraphicsY(),
                                inputNeuron.getGraphicsX(), inputNeuron.getGraphicsY(), lineWidth);
                    }
                }
            }
        }
    }

    public void drawNodes() {
        NeuralNetwork nn = cell.getGeneExpressionFunction().getRegulatoryNetwork();
        float r = getNeuronRadius();
        float lineWidth = getSynapseLineWidth();

        Colour colour = new Colour();
        for (Neuron neuron : nn.getNeurons()) {
            if (!neuron.isConnectedToOutput() || !neuron.isGraphicsEnabled())
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
                            || neuron.isInput(mouseOverNeuron)
                            || mouseOverNeuron.isInput(neuron))) {
                float t = 0.75f;
                stateColor.lerp(Color.BLACK, t);
                ringColor.lerp(Color.BLACK, t);
            }

            shapeRenderer.setColor(stateColor);


            if (neuron.getType().equals(Neuron.Type.SENSOR)) {
                shapeRenderer.filledRectangle(
                        neuron.getGraphicsX() - r,
                        neuron.getGraphicsY() - r,
                        2*r, 2*r);
                shapeRenderer.setColor(ringColor);
                shapeRenderer.rectangle(
                        neuron.getGraphicsX() - r,
                        neuron.getGraphicsY() - r,
                        2*r, 2*r, lineWidth);
            }
            else if (neuron.getType().equals(Neuron.Type.OUTPUT)) {
                shapeRenderer.filledRectangle(
                        neuron.getGraphicsX() - r,
                        neuron.getGraphicsY() - r,
                        2*r, 2*r, (float) Math.PI / 4.f);
                shapeRenderer.setColor(ringColor);
                shapeRenderer.rectangle(
                        neuron.getGraphicsX() - r,
                        neuron.getGraphicsY() - r,
                        2*r, 2*r, lineWidth, (float) Math.PI / 4.f);
            }
            else {
                shapeRenderer.filledCircle(
                        neuron.getGraphicsX(),
                        neuron.getGraphicsY(),
                        r);

                shapeRenderer.setColor(ringColor);

                shapeRenderer.circle(
                        neuron.getGraphicsX(),
                        neuron.getGraphicsY(),
                        r, lineWidth);
            }
        }
    }


    public void render(float delta) {
        NeuralNetwork nn = cell.getGeneExpressionFunction().getRegulatoryNetwork();

        if (nn == null)
            return;

        updateNeuronPositions(delta);

        shapeRenderer.update();
        findMouseOverNeuron(getNeuronRadius());

        for (Neuron neuron : nn.getNeurons()) {
            neuron.setGraphicsEnabled(!isUnusedSurfaceNode(neuron));
        }
        drawWeights();
        drawNodes();
    }

    public void drawUI(SpriteBatch uiBatch) {

        float r = getNeuronRadius();
        uiBatch.setColor(Color.WHITE.cpy().mul(0.9f));

        if (mouseOverNeuronHandler != null && mouseOverNeuron != null
                && mouseOverNeuron.isGraphicsEnabled()) {

            Vector3 neuronScreenPos = camera.project(new Vector3(
                    mouseOverNeuron.getGraphicsX(),
                    mouseOverNeuron.getGraphicsY(), 0));
            float x = neuronScreenPos.x;
            float y = neuronScreenPos.y;
            mouseOverNeuronHandler.apply(uiBatch, mouseOverNeuron, x, y, r,0);
        }
    }

    public boolean isDead() {
        return cell.isDead();
    }

    public void dispose() {

    }
}
