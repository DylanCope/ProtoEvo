package com.protoevo.biology.nodes;

public abstract class NodeAttachment {

    protected SurfaceNode node;

    public NodeAttachment(SurfaceNode node) {
        this.node = node;
    }

    public abstract void update(float delta);
    public abstract void handleIO(float[] input, float[] output);
}
