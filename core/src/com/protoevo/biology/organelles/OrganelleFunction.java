package com.protoevo.biology.organelles;

import com.protoevo.core.Statistics;

import java.io.Serializable;

public abstract class OrganelleFunction  implements Serializable {

    public static long serialVersionUID = 1L;

    protected Organelle organelle;

    public OrganelleFunction(Organelle organelle) {
        this.organelle = organelle;
    }

    public Organelle getOrganelle() {
        return organelle;
    }

    public abstract void update(float delta, float[] input);

    public abstract String getName();

    public abstract String getInputMeaning(int idx);

    public Statistics getStats() {
        return null;
    }
}
