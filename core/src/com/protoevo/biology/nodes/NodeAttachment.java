package com.protoevo.biology.nodes;

public abstract class NodeAttachment {

    protected SurfaceNode node;

    public NodeAttachment(SurfaceNode node) {
        this.node = node;
    }

    public abstract void update(float delta, float[] input, float[] output);

    public float getInteractionRange() {
        return 0;
    }

    public SurfaceNode getNode() {
        return node;
    }
}
