package com.protoevo.biology;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.protoevo.biology.cells.Cell;
import com.protoevo.biology.cells.MeatCell;
import com.protoevo.biology.cells.Protozoan;
import com.protoevo.core.Simulation;
import com.protoevo.env.Environment;
import com.protoevo.physics.Particle;
import com.protoevo.maths.Geometry;
import com.protoevo.utils.SerializableFunction;

import java.io.Serializable;


public class BurstRequest<T extends Cell> implements Serializable {
    public static final long serialVersionUID = 1L;

    private SerializableFunction<Float, T> createChild;
    private Cell parent;
    private Class<T> cellType;
    private boolean overrideMinRadius, ready = false;

    public BurstRequest() {}

    public BurstRequest(Cell cell) {
        this.parent = cell;
    }

    public BurstRequest(Cell parent, Class<T> cellType, SerializableFunction<Float, T> createChild) {
        this.createChild = createChild;
        this.parent = parent;
        this.cellType = cellType;
        this.overrideMinRadius = false;
        ready = true;
    }

    public BurstRequest(Cell parent,
                        Class<T> cellType,
                        SerializableFunction<Float, T> createChild,
                        boolean overrideMinRadius) {
        this.createChild = createChild;
        this.parent = parent;
        this.cellType = cellType;
        this.overrideMinRadius = overrideMinRadius;
        ready = true;
    }

    public void set(Class<T> cellType,
                    SerializableFunction<Float, T> createChild,
                    boolean overrideMinRadius) {
        this.createChild = createChild;
        this.cellType = cellType;
        this.overrideMinRadius = overrideMinRadius;
        ready = true;
    }

    public boolean canBurst() {
        return parent.getRadius() > parent.getMinBurstRadius() || overrideMinRadius;
    }

    public void burst() {
        if (!ready)
            return;

        if (!parent.isDead()) {
            if (parent instanceof Protozoan)
                ((Protozoan) parent).kill(CauseOfDeath.CYTOKINESIS, false);
            else
                parent.kill(CauseOfDeath.CYTOKINESIS);
        } else if (!cellType.equals(MeatCell.class)) {
            return;
        }

        parent.setHasBurst(true);

        float angle = (float) (2 * Math.PI * Simulation.RANDOM.nextDouble());

        float volume = Geometry.getSphereVolume(parent.getRadius());
        float minVolume = Geometry.getSphereVolume(Environment.settings.minParticleRadius.get());
        int maxChildren = Math.min(6, (int) (volume / minVolume));

        int nChildren = 2;
        if (maxChildren > 2) {
            int chances = parent.burstMultiplier();
            for (int i = 0; i < chances; i++)
                nChildren = Math.max(nChildren, MathUtils.random(2, maxChildren));
        }

        for (int i = 0; i < nChildren; i++) {
            Vector2 dir = new Vector2((float) Math.cos(angle), (float) Math.sin(angle));
            float p = 0.3f + 0.7f * MathUtils.random() / nChildren;

            T child = createChild.apply(parent.getRadius() * p);
            Particle childParticle = child.getParticle();
            childParticle.setPos(parent.getParticle().getPos().cpy().add(dir.scl(2 * childParticle.getRadius())));
            childParticle.applyImpulse(dir.scl(.005f));

            child.setGeneration(parent.getGeneration() + 1);
            allocateChildResources(child, p);

            angle += 2 * Math.PI / nChildren;
        }
    }

    public void allocateChildResources(Cell child, float p) {
        child.setAvailableConstructionMass(parent.getConstructionMassAvailable() * p);
        child.setEnergyAvailable(parent.getEnergyAvailable() * p);
        for (ComplexMolecule molecule : parent.getComplexMolecules())
            child.setComplexMoleculeAvailable(molecule, p * parent.getComplexMoleculeAvailable(molecule));

//        for (CellAdhesion.CAM cam : parent.getSurfaceCAMs())
//            child.setCAMAvailable(cam, p * parent.getCAMAvailable(cam));

        for (Food.Type foodType : parent.getFoodToDigest().keySet()) {
            Food oldFood = parent.getFoodToDigest().get(foodType);
            Food newFood = new Food(p * oldFood.getSimpleMass(), foodType);
            if (newFood.getSimpleMass() <= 1e-12)
                continue;
            for (ComplexMolecule molecule : oldFood.getComplexMolecules()) {
                float moleculeAmount = p * oldFood.getComplexMoleculeMass(molecule);
                if (moleculeAmount <= 1e-12)
                    continue;
                newFood.addComplexMoleculeMass(molecule, moleculeAmount);
            }
            child.setFoodToDigest(foodType, newFood);
        }
    }

    public boolean parentEquals(Cell parent) {
        return this.parent.equals(parent);
    }

    public Class<T> getCellType() {
        return cellType;
    }
}
