package com.protoevo.biology.organelles;

import java.io.Serializable;

public abstract class OrganelleFunction  implements Serializable {

    public static long serialVersionUID = 1L;

    protected final Organelle organelle;

    public OrganelleFunction(Organelle organelle) {
        this.organelle = organelle;
    }

    public Organelle getOrganelle() {
        return organelle;
    }

    public abstract void update(float delta, float[] input);
}
