package com.protoevo.ui.nn;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.protoevo.biology.evolution.GeneExpressionFunction;
import com.protoevo.biology.nn.GRNTag;
import com.protoevo.biology.nn.Neuron;
import com.protoevo.biology.nodes.SurfaceNode;
import com.protoevo.biology.organelles.Organelle;
import com.protoevo.utils.Utils;

public class MouseOverNeuronHandler {
    private final GlyphLayout layout = new GlyphLayout();
    private final BitmapFont font;
    private GeneExpressionFunction geneExpressionFunction;

    public MouseOverNeuronHandler(BitmapFont font) {
        this.font = font;
    }

    public void setGeneExpressionFunction(GeneExpressionFunction geneExpressionFunction) {
        this.geneExpressionFunction = geneExpressionFunction;
    }

    public String getSurfaceNodeLabel(Neuron neuron,
                                      GeneExpressionFunction.Node geneNode,
                                      SurfaceNode surfaceNode) {
        String label = "Node " + surfaceNode.getIndex() + ":";
        if (surfaceNode.getAttachment() != null)
            label += " " + surfaceNode.getAttachmentName();

        String displayName = geneNode.getDisplayName();
        if (displayName.contains(SurfaceNode.activationPrefix)) {
            String[] parts = displayName.split(SurfaceNode.activationPrefix);
            boolean isInput = parts[0].contains("Input");
            label += (isInput ? "\n" : " ") + getAttachmentIOString(surfaceNode, parts);
        } else {
            label += "\n" + displayName;
        }
        label += " = " + Utils.numberToString(neuron.getLastState(), 2);
        return label;
    }

    private String getAttachmentIOString(SurfaceNode surfaceNode, String[] parts) {
        int idx = Integer.parseInt(parts[1]);
        boolean isInput = parts[0].contains("Input");
        String dir = isInput ? "Input" : "Output";
        if (surfaceNode.getAttachment() != null) {
            String meaning;
            if (isInput)
                meaning = surfaceNode.getAttachment().getInputMeaning(idx);
            else
                meaning = surfaceNode.getAttachment().getOutputMeaning(idx);
            return meaning == null ? "Unused" : meaning;
        } else {
            return dir + " " + idx;
        }
    }

    private String getOrganelleLabel(Neuron neuron,
                                     GeneExpressionFunction.Node geneNode,
                                     Organelle organelle) {
        String label;
        String displayName = geneNode.getDisplayName();
        String[] parts = displayName.split("Input/");
        if (parts.length == 2) {
            label = "Organelle " + organelle.getIndex() + ": ";
            if (organelle.getFunction() != null) {
                int idx = Integer.parseInt(parts[1]);
                label += "\n" + organelle.getFunction().getInputMeaning(idx);
            } else {
                label += "Input " + organelle.getIndex();
            }
        } else {
            label = displayName;
        }
        label += " = " + Utils.numberToString(neuron.getLastState(), 2);
        return label;
    }

    public String getNeuronLabel(Neuron neuron) {

        if (geneExpressionFunction != null && neuron.getTags() != null
                && neuron.getTags().length > 0 && neuron.getTags()[0] instanceof GRNTag)
        {
            Object tagged = ((GRNTag) neuron.getTags()[0]).apply(geneExpressionFunction);
            if (tagged instanceof GeneExpressionFunction.Node) {
                GeneExpressionFunction.Node node = (GeneExpressionFunction.Node) tagged;
                if (node.getLastTarget() instanceof SurfaceNode) {
                    return getSurfaceNodeLabel(neuron, node, (SurfaceNode) node.getLastTarget());
                }
                else if (node.getLastTarget() instanceof Organelle) {
                    return getOrganelleLabel(neuron, node, (Organelle) node.getLastTarget());
                }
            }
        }

        if (neuron.hasLabel()) {
            String label = neuron.getLabel();
            if (label.endsWith(":Output"))
                label = label.substring(0, label.length() - ":Output".length());
            return label + " = " + Utils.numberToString(neuron.getLastState(), 2);
        }

        return neuron.getActivation() + "(z) = "
                + Utils.numberToString(neuron.getLastState(), 2);
    }

    public void apply(SpriteBatch batch, Neuron neuron, float graphicsRadius, float neuronSpacing) {
        float x = neuron.getGraphicsX();
        float y = neuron.getGraphicsY();

        String labelStr = getNeuronLabel(neuron);
        layout.setText(font, labelStr);
        float labelX = x - layout.width / 2;
        float pad = layout.height * 0.3f;
        float infoWidth = layout.width + 2*pad;
        if (labelX + infoWidth >= Gdx.graphics.getWidth())
            labelX = (int) (Gdx.graphics.getWidth() - 1.1 * infoWidth);

        float labelY = (y + 1.1f * graphicsRadius) + layout.height;
        font.draw(batch, labelStr, labelX, labelY);

        if (neuronSpacing < font.getLineHeight())
            return;

        for (Neuron input : neuron.getInputs()) {
            String inputLabelStr = getNeuronLabel(input);
            layout.setText(font, inputLabelStr);
            float inputLabelX = input.getGraphicsX() - layout.width - graphicsRadius * 1.5f;
            if (inputLabelX + layout.width >= Gdx.graphics.getWidth())
                inputLabelX = (int) (Gdx.graphics.getWidth() - 1.1 * layout.width);
            float inputLabelY = input.getGraphicsY() + layout.height / 2f;
            font.draw(batch, inputLabelStr, inputLabelX, inputLabelY);
        }
    }
}
