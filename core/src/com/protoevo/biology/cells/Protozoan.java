package com.protoevo.biology.cells;

import com.badlogic.gdx.math.Vector2;
import com.protoevo.biology.*;
import com.protoevo.biology.evolution.*;
import com.protoevo.biology.nn.NeuralNetwork;
import com.protoevo.biology.nodes.*;
import com.protoevo.biology.organelles.Organelle;
import com.protoevo.core.Simulation;
import com.protoevo.core.Statistics;
import com.protoevo.settings.ProtozoaSettings;
import com.protoevo.settings.Settings;
import com.protoevo.settings.SimulationSettings;
import com.protoevo.utils.Colour;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Protozoan extends Cell implements Evolvable
{
	@Serial
	private static final long serialVersionUID = 2314292760446370751L;
	public transient int id = Simulation.RANDOM.nextInt();

	private GeneExpressionFunction geneExpressionFunction;
	private GeneExpressionFunction crossOverGenome;
	private Protozoan mate;
	private float timeMating = 0;
	public boolean wasJustDamaged = false;
	private List<SurfaceNode> surfaceNodes;

	private float damageRate = 0;
	private float herbivoreFactor, splitRadius;
	private float timeSinceLastGeneUpdate = 0;
	private final static float geneUpdateTime = Settings.simulationUpdateDelta * 20;
	private final Vector2 tmp = new Vector2();
	private final Collection<Cell> engulfedCells = new ArrayList<>(0);

	@Override
	public void update(float delta)
	{
		super.update(delta);

		timeSinceLastGeneUpdate += delta;
		if (timeSinceLastGeneUpdate >= geneUpdateTime) {
			geneExpressionFunction.update();
			timeSinceLastGeneUpdate = 0;
		}
		age(delta);
		surfaceNodes.forEach(n -> n.update(delta));

		engulfedCells.forEach(c -> eat((EdibleCell) c, delta));
		engulfedCells.removeIf(c -> c.getHealth() < 0.1f);

		if (shouldSplit() && hasNotBurst()) {
			getEnv().requestBurst(this, Protozoan.class, this::createSplitChild);
		}
	}

	@Override
	@EvolvableComponent
	public void setGeneExpressionFunction(GeneExpressionFunction fn) {
		this.geneExpressionFunction = fn;
	}

	@EvolvableList(
			name = "Surface Nodes",
			elementClassPath = "com.protoevo.biology.nodes.SurfaceNode",
			minSize = 1,
			initialSize = 3
	)
	public void setSurfaceNodes(ArrayList<SurfaceNode> surfaceNodes) {
		this.surfaceNodes = surfaceNodes;
		for (SurfaceNode node : surfaceNodes)
			node.setCell(this);
	}

	@EvolvableList(
			name = "Organelles",
			elementClassPath = "com.protoevo.biology.organelles.Organelle",
			initialSize = 2
	)
	public void setOrganelles(ArrayList<Organelle> organelles) {
		for (Organelle organelle : organelles) {
			organelle.setCell(this);
			addOrganelle(organelle);
		}
	}

	@EvolvableFloat(name="Herbivore Factor", min=0.5f, max=2f)
	public void setHerbivoreFactor(float herbivoreFactor) {
		this.herbivoreFactor = herbivoreFactor;
		setDigestionRate(Food.Type.Meat, 1 / herbivoreFactor);
		setDigestionRate(Food.Type.Plant, herbivoreFactor);
	}

	@EvolvableFloat(name="Split Radius",
			min=ProtozoaSettings.maxProtozoanBirthRadius,
			max=SimulationSettings.maxParticleRadius)
	public void setSplitRadius(float splitRadius) {
		this.splitRadius = splitRadius;
	}

	@EvolvableObject(name="Cell Colour",
					 traitClass ="com.protoevo.biology.cells.ProtozoaColourTrait")
	public void setColour(Colour colour) {
		setHealthyColour(colour);
	}

	@EvolvableFloat(name="Growth Rate",
					min=ProtozoaSettings.minProtozoanGrowthRate,
					max=ProtozoaSettings.maxProtozoanGrowthRate)
	public void setGrowth(float growthRate) {
		setGrowthRate(growthRate);
	}

	@EvolvableFloat(name="Repair Rate")
	public void setRepairRate(float repairRate) {
		super.setRepairRate(repairRate);
	}

	@Override
	@GeneRegulator(name="Health")
	public float getHealth() {
		return super.getHealth();
	}

	@Override
	@GeneRegulator(name="Size",
			       min=ProtozoaSettings.minProtozoanSplitRadius,
			       max=ProtozoaSettings.maxProtozoanSplitRadius)
	public float getRadius() {
		return super.getRadius();
	}

	@GeneRegulator(name="Construction Mass Available", max=1/1000f)
	public float getConstructionMass() {
		return getConstructionMassAvailable();
	}

	private boolean shouldSplit() {
		return getRadius() > splitRadius && getHealth() > ProtozoaSettings.minHealthToSplit;
	}

	private Protozoan createSplitChild(float r) {
		Protozoan child;
		if (crossOverGenome != null)
			child = Evolvable.createChild(this.getClass(), this.getGeneExpressionFunction(), crossOverGenome);
		else
			child = Evolvable.asexualClone(this);

		child.setEnv(getEnv());
		return child;
	}

	@Override
	public float getInteractionRange() {
		return surfaceNodes.stream()
				.map(SurfaceNode::getInteractionRange)
				.max(Float::compare)
				.orElse(0f);
	}

	@Override
	public boolean canPossiblyInteract() {
		return true;
	}

	@Override
	public void kill(CauseOfDeath causeOfDeath) {
		if (!super.isDead() && hasNotBurst())
			getEnv().requestBurst(
					this,
					MeatCell.class,
					r -> new MeatCell(r, getEnv()),
					true);
		super.kill(causeOfDeath);
	}

	private void handleEngulfing(EdibleCell e, float delta) {
		// Move engulfed cell towards the centre of this cell
		Vector2 vel = tmp.set(getPos()).sub(e.getPos());
		float d2 = vel.len2();
		vel.setLength(ProtozoaSettings.engulfForce * tmp.len2());
		if (!e.isFullyEngulfed())
			vel.add(getVel());
		else
			vel.add(getVel().cpy().scl(0.8f));

		e.getPos().add(vel.scl(delta));
		float maxD = 0.7f * (getRadius() - e.getRadius());

		// Ensure the engulfed cell doesn't exit the cell if fully engulfed
		if (d2 > maxD*maxD && e.isFullyEngulfed()) {
			tmp.set(e.getPos()).sub(getPos()).setLength(maxD);
			e.getPos().set(getPos()).add(tmp);
		}
		if (d2 < maxD*maxD) {
			e.setFullyEngulfed();
		}

		// Ensure the engulfed cells don't overlap too much
		for (Cell other : engulfedCells) {
			float rr = e.getRadius() + other.getRadius();
			d2 = other.getPos().dst2(e.getPos());
			if (other != e && d2 < rr*rr) {
				tmp.set(e.getPos()).sub(other.getPos());
				tmp.setLength(ProtozoaSettings.engulfForce * delta * (rr*rr - d2));
				e.getPos().add(tmp);
			}
		}
	}

	@Override
	public void eat(EdibleCell e, float delta)
	{
		handleEngulfing(e, delta);

		float extraction = .5f;
		if (e instanceof PlantCell) {
			extraction *= getHerbivoreFactor();
		} else if (e instanceof MeatCell) {
			extraction /= getHerbivoreFactor();
		}

		super.eat(e, extraction * delta);
	}


	@Override
	public String getPrettyName() {
		return "Protozoan";
	}

	public int getNumOfAttachments(Class<? extends NodeAttachment> type) {
		return (int) surfaceNodes.stream()
				.map(SurfaceNode::getAttachment)
				.filter(type::isInstance)
				.count();
	}

	public int getNumSpikes() {
		return getNumOfAttachments(Spike.class);
	}

	public int getNumLightSensitiveNodes() {
		return getNumOfAttachments(Photoreceptor.class);
	}

	@Override
	public Statistics getStats() {
		Statistics stats = super.getStats();
		stats.put("Death Rate", 100 * damageRate, Statistics.ComplexUnit.PERCENTAGE_PER_TIME);
		stats.putDistance("Split Radius", splitRadius);
		stats.putBoolean("Has Mated", crossOverGenome == null);
		int numSpikes = getNumSpikes();
		if (numSpikes > 0)
			stats.putCount("Num Spikes", numSpikes);
		NeuralNetwork grn = geneExpressionFunction.getRegulatoryNetwork();
		if (grn != null) {
			stats.putCount("GRN Depth", grn.getDepth());
			stats.putCount("GRN Size", grn.getSize());
		}
		int numLSN = getNumLightSensitiveNodes();
		if (numLSN > 0) {
			stats.putCount("Light Sensitive Nodes", numLSN);
		}

		stats.put("Herbivore Factor", herbivoreFactor);
		stats.putPercentage("Mutation Chance", 100 * geneExpressionFunction.getMutationRate());

		return stats;
	}

	@Override
	public float getGrowthRate() {
		float growthRate = super.getGrowthRate();
		if (getRadius() > splitRadius)
			growthRate *= getHealth() * splitRadius / (5 * getRadius());
		return growthRate; // * (0.25f + 0.75f * growthControlFactor);
	}

	public void age(float delta) {
		damageRate = getRadius() * ProtozoaSettings.protozoaStarvationFactor;
		damage(damageRate * delta, CauseOfDeath.OLD_AGE);
	}

	@Override
	public boolean isEdible() {
		return false;
	}


	public boolean isHarbouringCrossover() {
		return crossOverGenome != null;
	}

	public Protozoan getMate() {
		return mate;
	}

	public float getSplitRadius() {
		return splitRadius;
	}

	@Override
	public GeneExpressionFunction getGeneExpressionFunction() {
		return geneExpressionFunction;
	}

	@Override
	public Collection<SurfaceNode> getSurfaceNodes() {
		return surfaceNodes;
	}

	public Collection<Cell> getEngulfedCells() {
		return engulfedCells;
	}

	public float getHerbivoreFactor() {
		return herbivoreFactor;
	}
}
