package com.protoevo.biology.protozoa;

import com.protoevo.biology.evolution.Evolvable;
import com.protoevo.biology.evolution.EvolvableFloat;
import com.protoevo.biology.evolution.EvolvableInteger;

import java.io.Serializable;
import java.util.Arrays;

public class Spikes implements Evolvable.Component, Serializable {

    public static class Spike implements Component, Serializable {
        private static final long serialVersionUID = 1L;
        public float length;
        public float angle;
        public float growthRate;
        public float currentLength = 0;

        public void update(float delta) {
            if (currentLength < length) {
                currentLength = Math.min(currentLength + delta * growthRate, length);
            }
        }

        @EvolvableFloat(name="Spike Length")
        public void setLength(float length) {
            this.length = length;
        }

        @EvolvableFloat(name="Spike Growth")
        public void setGrowthRate(float growthRate) {
            this.growthRate = growthRate;
        }

        @EvolvableFloat(name="Spike Angle", max = (float) (2*Math.PI))
        public void setSpikeAngle(float angle) {
            this.angle = angle;
        }
    }

    private Spike[] spikes;

    public Spike[] getSpikes() {
        return spikes;
    }

    @EvolvableInteger(name="Number of Spikes", randomInitialValue=false, max=16)
    public void setNumSpikes(int numSpikes) {
        if (spikes == null) {
            spikes = new Spike[numSpikes];
            for (int i = 0; i < numSpikes; i++)
                spikes[i] = new Spike();
        }
        else if (numSpikes > spikes.length) {
            int originalLen = spikes.length;
            spikes = Arrays.copyOf(spikes, numSpikes);
            for (int i = originalLen; i < spikes.length; i++) {
                spikes[i] = new Spike();
            }
        } else if (numSpikes < spikes.length) {
            spikes = Arrays.copyOf(spikes, numSpikes);
        }
    }

    public void update(float delta) {
        for (Spike spike : spikes)
            spike.update(delta);
    }

    public int getNumSpikes() {
        return spikes.length;
    }

}
