package com.protoevo.biology.protozoa;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.protoevo.biology.*;
import com.protoevo.biology.evolution.*;
import com.protoevo.biology.neat.NeuralNetwork;
import com.protoevo.biology.nodes.Photoreceptor;
import com.protoevo.biology.nodes.NodeAttachment;
import com.protoevo.biology.nodes.Spike;
import com.protoevo.biology.nodes.SurfaceNode;
import com.protoevo.core.Simulation;
import com.protoevo.core.settings.ProtozoaSettings;
import com.protoevo.core.settings.Settings;
import com.protoevo.core.settings.SimulationSettings;
import com.protoevo.env.CollisionHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class Protozoan extends Cell implements Evolvable
{
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
	private final float geneUpdateTime = Settings.simulationUpdateDelta * 20;
	private Collection<Cell> engulfedCells = new ArrayList<>();
	private Vector2 tmp = new Vector2();

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
		handleCollisions(delta);
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

	@EvolvableCollection(
			name = "Surface Nodes",
			elementClassPath = "com.protoevo.biology.nodes.SurfaceNode",
			minSize = 1,
			maxSize = 10,
			initialSize = 3
	)
	public void setSurfaceNodes(ArrayList<SurfaceNode> surfaceNodes) {
		this.surfaceNodes = surfaceNodes;
		for (SurfaceNode node : surfaceNodes)
			node.setCell(this);
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
					 traitClass ="com.protoevo.biology.protozoa.ProtozoaColorTrait")
	public void setColour(Color colour) {
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

	@EvolvableFloat(name="Retinal Production")
	public void setRetinalProduction(float production) {
		setComplexMoleculeProductionRate(Food.ComplexMolecule.Retinal, production);
	}

	@EvolvableObject(name="CAM Production",
					 traitClass ="com.protoevo.biology.protozoa.CAMProductionTrait")
	public void setCAMProduction(Map<CellAdhesion.CAM, Float> camProduction) {
		for (CellAdhesion.CAM cam : camProduction.keySet())
			setCAMProductionRate(cam, camProduction.get(cam));
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

	@GeneRegulator(name="Retinal Available", max=1/1000f)
	public float getRetinalAmount() {
		return getComplexMoleculeAvailable(Food.ComplexMolecule.Retinal);
	}

	@GeneRegulator(name="Construction Mass Available", max=1/1000f)
	public float getConstructionMass() {
		return getConstructionMassAvailable();
	}

	@Override
	public void eat(EdibleCell e, float delta)
	{
		Vector2 vel = tmp.set(getPos()).sub(e.getPos());
		float d2 = vel.len2();
		vel.setLength(ProtozoaSettings.engulfForce * tmp.len2());
		if (!e.isFullyEngulfed())
			vel.add(getVel());
		else
			vel.add(getVel().cpy().scl(0.8f));

		e.getPos().add(vel.scl(delta));
		float maxD = 0.7f * (getRadius() - e.getRadius());

		if (d2 > maxD*maxD && e.isFullyEngulfed()) {
			tmp.set(e.getPos()).sub(getPos()).setLength(maxD);
			e.getPos().set(getPos()).add(tmp);
		}
		if (d2 < maxD*maxD) {
			e.setFullyEngulfed();
		}

		for (Cell other : engulfedCells) {
			float rr = e.getRadius() + other.getRadius();
			d2 = other.getPos().dst2(e.getPos());
			if (other != e && d2 < rr*rr) {
				tmp.set(e.getPos()).sub(other.getPos());
				tmp.setLength(ProtozoaSettings.engulfForce * delta * (rr*rr - d2));
				e.getPos().add(tmp);
			}
		}

		float extraction = .5f;
		if (e instanceof PlantCell) {
			extraction *= herbivoreFactor;
		} else if (e instanceof MeatCell) {
			extraction /= herbivoreFactor;
		}

		super.eat(e, extraction * delta);
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
	public Map<String, Float> getStats() {
		Map<String, Float> stats = super.getStats();
		stats.put("Death Rate", 100 * damageRate);
		stats.put("Split Radius", Settings.statsDistanceScalar * splitRadius);
		stats.put("Has Mated", crossOverGenome == null ? 0f : 1f);
		int numSpikes = getNumSpikes();
		if (numSpikes > 0)
			stats.put("Num Spikes", (float) numSpikes);
		NeuralNetwork grn = geneExpressionFunction.getRegulatoryNetwork();
		if (grn != null) {
			stats.put("GRN Depth", (float) grn.getDepth());
			stats.put("GRN Size", (float) grn.getSize());
		}
		int numLSN = getNumLightSensitiveNodes();
		if (numLSN > 0) {
			stats.put("Light Sensitive Nodes", (float) numLSN);
		}

		stats.put("Herbivore Factor", herbivoreFactor);
		stats.put("Mutation Chance", 100 * geneExpressionFunction.getMutationRate());
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

	public void handleCollisions(float delta) {
		for (CollisionHandler.FixtureCollision contact : getContacts()) {
			Object collided = getOther(contact);
			if (collided instanceof Cell && engulfCondition((Cell) collided)) {
//				eat((EdibleCell) collided, delta);
				engulf((Cell) collided, delta);
			}
		}
	}

	public boolean engulfCondition(Cell cell) {
		return cell instanceof EdibleCell && cell.getRadius() < getRadius() / 2
				&& getRadius() > 2 * SimulationSettings.minParticleRadius;
	}

	public void engulf(Cell cell, float delta) {
		cell.setEngulfer(this);
//		if (cell.getPhantomPos().dst2(getPos()) < getRadius() * getRadius()) {
//			cell.kill(CauseOfDeath.EATEN);
//		}
		cell.kill(CauseOfDeath.EATEN);
		engulfedCells.add(cell);
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

	public Collection<SurfaceNode> getSurfaceNodes() {
		return surfaceNodes;
	}

	public Collection<Cell> getEngulfedCells() {
		return engulfedCells;
	}
}
