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
        String label = "Node "+ surfaceNode.getIndex() + surfaceNode.getAttachment()
                .map(attachment -> ": " + attachment.getName())
                .orElse("");

        if (geneNode.getTrait().getTraitName().contains("Activation/")) {
            String[] parts = geneNode.getTrait().getTraitName().split("Activation/");
            int idx = Integer.parseInt(parts[1]);
            if (parts[0].equals("Input")) {
                label += surfaceNode.getAttachment()
                        .map(attachment -> attachment.getInputMeaning(idx))
                        .orElse("Input " + idx);
            } else {
                label += surfaceNode.getAttachment()
                        .map(attachment -> attachment.getOutputMeaning(idx))
                        .orElse("Output " + idx);
            }
        } else {
            label += " " + geneNode.getTrait().getTraitName();
        }
        label += " = " + Utils.numberToString(neuron.getLastState(), 2);
        return label;
    }

    public String getSurfaceNodeLabel(Neuron neuron,
                                      GeneExpressionFunction.RegulationNode geneNode,
                                      SurfaceNode surfaceNode) {
        String label = "Node "+ surfaceNode.getIndex() + surfaceNode.getAttachment()
                .map(attachment -> ": " + attachment.getName())
                .orElse("");

        if (geneNode.getName().contains("Activation/")) {
            String[] parts = geneNode.getName().split("Activation/");
            int idx = Integer.parseInt(parts[1]);
            if (parts[0].equals("Input")) {
                label += " " + surfaceNode.getAttachment()
                        .map(attachment -> attachment.getInputMeaning(idx))
                        .orElse("Input " + idx);
            } else {
                label += " " + surfaceNode.getAttachment()
                        .map(attachment -> attachment.getOutputMeaning(idx))
                        .orElse("Output " + idx);
            }
        } else {
            label += " " + geneNode.getName();
        }
        label += " = " + Utils.numberToString(neuron.getLastState(), 2);
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

        float labelY = Gdx.graphics.getHeight() - (y + 1.1f * graphicsRadius);
        font.draw(batch, labelStr, labelX, labelY);
    }
}
