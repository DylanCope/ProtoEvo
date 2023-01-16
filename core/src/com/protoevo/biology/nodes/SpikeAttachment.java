package com.protoevo.biology.nodes;

public class SpikeAttachment extends NodeAttachment {

    public SpikeAttachment(SurfaceNode node) {
        super(node);
    }

    @Override
    public void update(float delta) {
    }

    @Override
    public void handleIO(float[] input, float[] output) {}

    @Override
    public float energyUsage() {
        return 0;
    }

}
