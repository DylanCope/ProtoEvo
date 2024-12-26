package com.protoevo.biology.evolution;

public class ControlTrait extends FloatTrait {

    public ControlTrait() {
        super(null, 0, 0);
    }

    public ControlTrait(String geneName, float minValue, float maxValue) {
        super(geneName, minValue, maxValue);
    }

}
