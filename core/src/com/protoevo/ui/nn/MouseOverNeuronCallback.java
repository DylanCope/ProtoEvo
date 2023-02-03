package com.protoevo.ui.nn;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.protoevo.biology.evolution.GeneExpressionFunction;
import com.protoevo.biology.neat.Neuron;
import com.protoevo.biology.nodes.SurfaceNode;
import com.protoevo.utils.Utils;

public class MouseOverNeuronCallback {
    private final GlyphLayout layout = new GlyphLayout();
    private final BitmapFont font;

    public MouseOverNeuronCallback(BitmapFont font) {
        this.font = font;
    }

    public String getSurfaceNodeLabel(Neuron neuron,
                                      GeneExpressionFunction.ExpressionNode geneNode,
                                      SurfaceNode surfaceNode) {
        String label = "Node";
        if (surfaceNode.getAttachment() != null)
            label += " " + surfaceNode.getAttachment().getName() + ": ";

        if (geneNode.getTrait().getTraitName().contains(SurfaceNode.activationPrefix)) {
            String[] parts = geneNode.getTrait().getTraitName().split(SurfaceNode.activationPrefix);
            label = getAttachmentIOString(surfaceNode, label, parts);
        } else {
            label += " " + geneNode.getTrait().getTraitName();
        }
        label += " = " + Utils.numberToString(neuron.getLastState(), 2);
        return label;
    }

    public String getSurfaceNodeLabel(Neuron neuron,
                                      GeneExpressionFunction.RegulationNode regulationNode,
                                      SurfaceNode surfaceNode) {
        String label = "Node";
        if (surfaceNode.getAttachment() != null)
            label += " " + surfaceNode.getAttachment().getName() + ": ";

        if (regulationNode.getName().contains(SurfaceNode.activationPrefix)) {
            String[] parts = regulationNode.getName().split(SurfaceNode.activationPrefix);
            label = getAttachmentIOString(surfaceNode, label, parts);
        } else {
            label += " " + regulationNode.getName();
        }
        label += " = " + Utils.numberToString(neuron.getLastState(), 2);
        return label;
    }

    private String getAttachmentIOString(SurfaceNode surfaceNode, String label, String[] parts) {
        int idx = Integer.parseInt(parts[1]);
        if (parts[0].equals("Input")) {
            if (surfaceNode.getAttachment() != null)
                label += " " + surfaceNode.getAttachment().getInputMeaning(idx);
            else
                label += "Input " + idx;
        } else {
            if (surfaceNode.getAttachment() != null)
                label += " " + surfaceNode.getAttachment().getOutputMeaning(idx);
            else
                label += "Output " + idx;
        }
        return label;
    }

    public String getNeuronLabel(Neuron neuron) {

        if (neuron.getTags() != null && neuron.getTags().length > 0) {
            if (neuron.getTags()[0] instanceof GeneExpressionFunction.ExpressionNode) {
                GeneExpressionFunction.ExpressionNode node = (GeneExpressionFunction.ExpressionNode) neuron.getTags()[0];
                if (node.getLastTarget() instanceof SurfaceNode) {
                    return getSurfaceNodeLabel(neuron, node, (SurfaceNode) node.getLastTarget());
                }
            }
            else if (neuron.getTags()[0] instanceof GeneExpressionFunction.RegulationNode) {
                GeneExpressionFunction.RegulationNode node = (GeneExpressionFunction.RegulationNode) neuron.getTags()[0];
                if (node.getLastTarget() instanceof SurfaceNode) {
                    return getSurfaceNodeLabel(neuron, node, (SurfaceNode) node.getLastTarget());
                }
            }
        }

        if (neuron.getLabel() != null)
            return neuron.getLabel() + " = " +
                    Utils.numberToString(neuron.getLastState(), 2);

        return Utils.numberToString(neuron.getLastState(), 2);
    }

    public void apply(SpriteBatch batch, Neuron neuron, float graphicsRadius) {
        float x = neuron.getGraphicsX();
        float y = neuron.getGraphicsY();

        String labelStr = getNeuronLabel(neuron);
        layout.setText(font, labelStr);
        float labelX = x - layout.width / 2;
        float pad = font.getLineHeight() * 0.3f;
        float infoWidth = layout.width + 2*pad;
        if (labelX + infoWidth >= Gdx.graphics.getWidth())
            labelX = (int) (Gdx.graphics.getWidth() - 1.1 * infoWidth);

        float labelY = Gdx.graphics.getHeight() - (y + 1.1f * graphicsRadius) + font.getLineHeight();
        font.draw(batch, labelStr, labelX, labelY);
    }
}
