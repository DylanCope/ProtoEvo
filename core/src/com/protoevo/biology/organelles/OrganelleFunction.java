package com.protoevo.biology.organelles;

public abstract class OrganelleFunction {

    protected final Organelle organelle;

    public OrganelleFunction(Organelle organelle) {
        this.organelle = organelle;
    }

    public Organelle getOrganelle() {
        return organelle;
    }

    public abstract void update(float delta, float[] input);
}
