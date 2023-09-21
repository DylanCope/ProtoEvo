package com.protoevo.biology.nn.meta;

public class NeuronMetaData {

    private boolean isHidden = false;
    private boolean hasLocation = false;
    private float graphicsX = -1;
    private float graphicsY = -1;

    public boolean isHidden() {
        return isHidden;
    }

    public void setHidden(boolean hidden) {
        isHidden = hidden;
        hasLocation = false;
    }
}
