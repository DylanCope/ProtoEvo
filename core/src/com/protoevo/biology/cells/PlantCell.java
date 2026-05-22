package com.protoevo.biology.cells;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.protoevo.biology.CauseOfDeath;
import com.protoevo.biology.evolution.AminoAcidSequence;
import com.protoevo.biology.evolution.Evolvable;
import com.protoevo.biology.evolution.EvolvableFloat;
import com.protoevo.biology.evolution.EvolvableList;
import com.protoevo.biology.evolution.EvolvableObject;
import com.protoevo.biology.nodes.SurfaceNode;
import com.protoevo.biology.organelles.Organelle;
import com.protoevo.core.Statistics;
import com.protoevo.env.Environment;
import com.protoevo.maths.Functions;
import com.protoevo.physics.Joining;
import com.protoevo.physics.JointsManager;
import com.protoevo.physics.Collision;
import com.protoevo.physics.Particle;
import com.protoevo.utils.Colour;
import com.protoevo.maths.Geometry;
import com.protoevo.utils.SerializableFunction;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class PlantCell extends EvolvableCell {
    public static final long serialVersionUID = -3975433688803760076L;

    private float maxRadius;
    private float photosynthesisRate = 0;
    private List<SurfaceNode> surfaceNodes = new ArrayList<>();
    // Surface signature recognized by phagocytic receptors. Evolves point-by-point.
    private AminoAcidSequence surfaceSignature;
    private static final Statistics.ComplexUnit photosynthesisUnit =
            new Statistics.ComplexUnit(Statistics.BaseUnit.ENERGY).divide(Statistics.BaseUnit.TIME);
    private final PlantSplitFn splitFn = new PlantSplitFn(this);

    public PlantCell(float radius, Environment environment) {
        super();
        setRadius(Math.max(radius, Environment.settings.plant.minBirthRadius.get()));
        setEnvironmentAndBuildPhysics(environment);
        // Low angular damping so a grazer bumping a spike-bearing plant
        // actually spins it — the spike then sweeps through nearby cells
        // instead of being permanently locked to its spawn orientation.
        // Selection can then evolve spike-angle/cell-rotation strategies.
        getParticle().setAngularDamping(0.3f);
        setGrowthRate(MathUtils.random(minGrowthRate(), maxGrowthRate()));

        maxRadius = randomMaxRadius();

        setRandomPlantColour();
    }

    public PlantCell() {
        super();
        setRadius(MathUtils.random(
                Environment.settings.plant.minBirthRadius.get(),
                Environment.settings.plant.maxBirthRadius.get()));
        maxRadius = randomMaxRadius();
        setGrowthRate(MathUtils.random(minGrowthRate(), maxGrowthRate()));
        setRandomPlantColour();
    }

    private float randomMaxRadius() {
        // 10% larger than the max plant birth radius
        float minMaxR = 2f * Environment.settings.plant.minBirthRadius.get();
        // 50% of the max particle radius
        float maxMaxR = Environment.settings.maxParticleRadius.get() / 2f;

        return minMaxR < maxMaxR ? MathUtils.random(minMaxR, maxMaxR) : maxMaxR;
    }

    public void setRandomPlantColour() {
        float darken = 0.9f;
        setHealthyColour(new Colour(
                darken * (30 + MathUtils.random(105)) / 255f,
                darken * (150 + MathUtils.random(100)) / 255f,
                darken * (10 + MathUtils.random(100)) / 255f,
                1f)
        );

        setDegradedColour(degradeColour(getHealthyColour(), 0.5f));
    }

    @Override
    public float getMaxRadius() {
        return maxRadius;
    }

    @EvolvableFloat(name="Split Radius", min=0, max=1)
    public void setMaxRadius(float splitRadius) {
        this.maxRadius = Functions.clampedLinearRemap(
                splitRadius, 0, 1,
                1.5f * Environment.settings.minParticleRadius.get(),
                Environment.settings.maxParticleRadius.get() / 2f
        );
    }

    @Override
    public boolean isEdible() {
        return true;
    }

    private static float randomPlantRadius() {
        float range = Environment.settings.plant.maxBirthRadius.get() * .5f - Environment.settings.plant.minBirthRadius.get();
        return Environment.settings.plant.minBirthRadius.get() + range * MathUtils.random();
    }

    public PlantCell(Environment environment) {
        this(randomPlantRadius(), environment);
    }

    private boolean shouldSplit(float delta) {
        // Hard gates first.
        if (!hasNotBurst())
            return false;
        if (getRadius() < 0.99f * maxRadius)
            return false;
        if (getHealth() <= Environment.settings.plant.minHealthToSplit.get())
            return false;
        // Probabilistic split: a mature plant lingers as adult, splitting
        // at a rate controlled by `plant.splitRate` (driven by the plant
        // PID). Without this, every plant that ripened on the same tick
        // burst on the same tick — that's the divide-then-die churn.
        float rate = Environment.settings.plant.splitRate.get();
        return com.protoevo.core.Simulation.RANDOM.nextFloat() < rate * delta;
    }

    @Override
    public float minGrowthRate() {
        return Environment.settings.plant.minPlantGrowth.get();
    }

    @Override
    public float maxGrowthRate() {
        return Environment.settings.plant.maxPlantGrowth.get();
    }

    // Plants now grow surface nodes too — same evolvable mechanism as protozoa,
    // but we cap them at fewer initial slots since most attachments (flagellum,
    // adhesion, phagocytic mouth) don't make sense on a plant. The pool gives
    // evolution room to find spikes (defensive — punish grazers) and
    // photoreceptors (let plants react to light beyond the simple
    // photosynthesisRate calculation).
    @EvolvableList(
            name = "Surface Nodes",
            elementClassPath = "com.protoevo.biology.nodes.SurfaceNode",
            minSize = 0,
            initialSize = 2
    )
    public void setSurfaceNodes(ArrayList<SurfaceNode> surfaceNodes) {
        this.surfaceNodes = surfaceNodes;
        for (SurfaceNode node : surfaceNodes)
            node.setCell(this);
    }

    // Plants need organelles too — without them the cell has no
    // ComplexMolecule pool, so SurfaceNode.handleAttachmentConstructionProjects
    // has nothing to iterate and attachments never finish construction.
    // initialSize=1 keeps plants cheaper than protozoa (which get 2).
    @EvolvableList(
            name = "Organelles",
            elementClassPath = "com.protoevo.biology.organelles.Organelle",
            initialSize = 1
    )
    public void setOrganelles(ArrayList<Organelle> organelles) {
        for (Organelle organelle : organelles) {
            organelle.setCell(this);
            addOrganelle(organelle);
        }
    }

    @Override
    public List<SurfaceNode> getSurfaceNodes() {
        return surfaceNodes;
    }

    // Plant surface signature: 50-residue evolvable string that the
    // phagocytic receptors on grazers have to match in order to engulf.
    // Mutates per-character so a lineage drifts gradually instead of
    // re-rolling, giving predators a hill to climb instead of a
    // lottery ticket. See AminoAcidSequence / PlantSignatureTrait.
    @EvolvableObject(name = "Surface Signature",
                     traitClass = "com.protoevo.biology.evolution.PlantSignatureTrait")
    public void setSurfaceSignature(AminoAcidSequence sig) {
        this.surfaceSignature = sig;
    }

    public AminoAcidSequence getSurfaceSignature() {
        return surfaceSignature;
    }

    // Plants don't translate. If evolution wires a Flagellum onto a plant,
    // the construction cost is paid but the thrust is dropped — natural
    // selection should weed out flagella from the plant gene pool quickly.
    // They CAN rotate though, controlled by the Rotate GRN output below —
    // letting them aim their spikes and photoreceptors at threats.
    @Override
    public void generateMovement(Vector2 thrustVector) {
        // no-op (no thrust)
    }

    @Override
    public float generateMovement(Vector2 thrustVector, float torque) {
        return 0f;
    }

    // Active rotation control. The GRN outputs a signed [-1,1] signal that
    // we apply as torque each update — positive = counterclockwise. Combined
    // with the low angular damping we set in setEnvironmentAndBuildPhysics,
    // this lets plants actually swivel toward attackers given a working
    // photoreceptor or contact sensor. Transient because it's just the
    // last-tick NN output, regenerated on every gene-expression pass.
    private transient float rotateSignal = 0f;

    @com.protoevo.biology.evolution.ControlVariable(name="Rotate", min=-1, max=1)
    public void setRotate(float r) {
        this.rotateSignal = r;
    }

    @Override
    public void update(float delta) {
        super.update(delta);

        if (isDead())
            return;

        float light = getLightAtCell();
        float maxArea = Geometry.getCircleArea(Environment.settings.maxParticleRadius.get());
        float photoRate = light * getParticle().getArea() / maxArea;
        photosynthesisRate = photoRate * Environment.settings.plant.photosynthesizeEnergyRate.get();

        addConstructionMass(delta * Environment.settings.plant.constructionRate.get());
        addAvailableEnergy(delta * photosynthesisRate);

        int nProtozoaContacts = 0;
        for (Collision collision : getParticle().getContacts()) {
            Object o = collision.getOther(getParticle());
            if (o instanceof Particle && ((Particle) o).getUserData() instanceof Protozoan)
                nProtozoaContacts++;
        }
        if (nProtozoaContacts >= 1) {
            float dps = Environment.settings.plant.collisionDestructionRate.get() * nProtozoaContacts;
            removeMass(delta * dps, CauseOfDeath.OVERCROWDING);
        }

        // Apply the GRN-controlled rotation as Box2D torque. Tiny scale —
        // plants are heavy and we don't want them spinning wildly, just
        // enough that an evolved lineage with good sensors can aim its
        // spike at a grazer it sees. Bounded by the angular damping (0.3)
        // we set at body creation, so the rotation settles at a terminal
        // angular velocity instead of accelerating without limit.
        if (rotateSignal != 0f) {
            float maxTorque = Environment.settings.plant.maxRotationTorque.get();
            getParticle().applyTorque(rotateSignal * maxTorque);
        }

        // Tick surface nodes so they can call tryCreate() once and progress
        // attachment construction projects every frame after that. Without
        // this, plants got SurfaceNode instances from the @EvolvableList
        // setter but nodeExists stayed false forever — the gene-expression
        // pipeline finished and then nobody ever drove the node lifecycle.
        if (surfaceNodes != null && !surfaceNodes.isEmpty()) {
            for (int i = 0; i < surfaceNodes.size(); i++) {
                SurfaceNode node = surfaceNodes.get(i);
                node.setIndex(i);
                node.update(delta);
            }
            // Spikes and photoreceptors need a sensor radius to find targets.
            getParticle().setRangedInteractionRadius(getInteractionRange());
        }

        handleAttachments();

        if (shouldSplit(delta) && getEnv().isPresent()) {
            Environment env = getEnv().get();
            env.requestBurst(PlantCell.this, PlantCell.class, splitFn);
        }
    }

    // Plants need a Box2D sensor fixture so spike/photoreceptor attachments
    // can pick up surrounding cells. The sensor is created once at physics
    // setup; the radius is updated each frame from getInteractionRange().
    @Override
    public boolean isRangedInteractionEnabled() {
        return true;
    }

    @Override
    public float getInteractionRange() {
        if (surfaceNodes == null || surfaceNodes.isEmpty())
            return 0f;
        float maxRange = 0f;
        for (SurfaceNode node : surfaceNodes)
            maxRange = Math.max(maxRange, node.getInteractionRange());
        return maxRange;
    }

    @Override
    public float getExpressionInterval() {
        return Environment.settings.plant.geneExpressionInterval.get();
    }

    @Override
    protected float getVoidStartDistance2() {
        return super.getVoidStartDistance2() * 0.9f * 0.9f;
    }

    public PlantCell createChild(float r) {
        PlantCell child;
        if (Environment.settings.plant.evolutionEnabled.get()) {
            child = Evolvable.asexualClone(this);
            child.setRadius(r);
            if (getEnv().isPresent())
                child.setEnvironmentAndBuildPhysics(getEnv().get());
        } else {
            Optional<Environment> env = getEnv();
            if (env.isEmpty())
                throw new RuntimeException(
                        "Cannot create cell without environment"
                );
            child = new PlantCell(r, env.get());
        }
        return child;
    }

    public void attach(Cell otherCell) {
        Joining joining = new Joining(this.getParticle(), otherCell.getParticle());
        getEnv().ifPresent(new Consumer<Environment>() {
            @Override
            public void accept(Environment env) {
                JointsManager jointsManager = env.getJointsManager();
                jointsManager.createJoint(joining);
                PlantCell.this.registerJoining(joining);
                otherCell.registerJoining(joining);
            }
        });
    }

    public void handleAttachments() {
        if (getNumAttachedCells() < 2) {
            for (Collision collision : getParticle().getContacts()) {
                Object o = collision.getOther(getParticle());
                if (!(o instanceof Particle && ((Particle) o).getUserData() instanceof PlantCell))
                    continue;

                PlantCell otherPlant = (PlantCell) ((Particle) o).getUserData();

                if (otherPlant.getNumAttachedCells() < 2 && !isAttachedTo(otherPlant)) {
                    attach(otherPlant);
                    if (getNumAttachedCells() >= 2)
                        break;
                }
            }
        }
    }

    @Override
    public Statistics getStats() {
        Statistics stats = super.getStats();
        stats.putDistance("Split Radius", maxRadius);
        stats.put("Photosynthesis Rate", photosynthesisRate, photosynthesisUnit);
        return stats;
    }

    @Override
    public String getPrettyName() {
        return "Plant";
    }

    public int burstMultiplier() {
        return 3;
    }

    public static class PlantSplitFn implements SerializableFunction<Float, PlantCell> {

        private PlantCell plantCell;

        public PlantSplitFn() {}

        public PlantSplitFn(PlantCell plantCell) {
            this.plantCell = plantCell;
        }

        @Override
        public PlantCell apply(Float r) {
            return plantCell.createChild(r);
        }
    }
}
