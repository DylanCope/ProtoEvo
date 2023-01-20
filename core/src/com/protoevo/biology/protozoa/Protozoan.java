package com.protoevo.biology.protozoa;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Contact;
import com.protoevo.biology.*;
import com.protoevo.biology.evolution.*;
import com.protoevo.biology.nodes.LightSensitiveAttachment;
import com.protoevo.biology.nodes.NodeAttachment;
import com.protoevo.biology.nodes.SpikeAttachment;
import com.protoevo.biology.nodes.SurfaceNode;
import com.protoevo.core.Simulation;
import com.protoevo.core.settings.ProtozoaSettings;
import com.protoevo.core.settings.Settings;
import com.protoevo.core.settings.SimulationSettings;

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
	private float herbivoreFactor, splitRadius, maxTurning, growthControlFactor = 1f;
	private float timeSinceLastGeneUpdate = 0;
	private final float geneUpdateTime = Settings.simulationUpdateDelta * 20;

	@Override
	public void update(float delta)
	{
		super.update(delta);

		timeSinceLastGeneUpdate += delta;
		if (timeSinceLastGeneUpdate >= geneUpdateTime) {
			geneExpressionFunction.update();
			timeSinceLastGeneUpdate %= geneUpdateTime;
		}
		age(delta);
		handleCollisions(delta);
		surfaceNodes.forEach(n -> n.update(delta));

		if (shouldSplit()) {
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
		for (SurfaceNode node : surfaceNodes) {
			node.setCell(this);
		}
	}

//	@EvolvableObject(
//			name="Brain",
//			geneClassName="com.protoevo.biology.protozoa.ProtozoaControlNetworkGene")
//	public void setBrain(NetworkGenome networkGenome) {
//		this.brain = new NNBrain(networkGenome.phenotype());
//	}

	@EvolvableFloat(name="Herbivore Factor", min=0.5f, max=2f)
	public void setHerbivoreFactor(float herbivoreFactor) {
		this.herbivoreFactor = herbivoreFactor;
		setDigestionRate(Food.Type.Meat, 1 / herbivoreFactor);
		setDigestionRate(Food.Type.Plant, herbivoreFactor);
	}

	@EvolvableFloat(name="Split Radius",
			min=ProtozoaSettings.maxProtozoanBirthRadius, max=SimulationSettings.maxParticleRadius)
	public void setSplitRadius(float splitRadius) {
		this.splitRadius = splitRadius;
	}

	@EvolvableFloat(name="Birth Radius",
			min=ProtozoaSettings.minProtozoanBirthRadius, max=ProtozoaSettings.maxProtozoanBirthRadius)
	public void setBirthRadius(float birthRadius) {
		if (getRadius() < birthRadius)
			setRadius(birthRadius);
	}

	@EvolvableFloat(name="Max Turn", min=ProtozoaSettings.protozoaMinMaxTurn, max=ProtozoaSettings.protozoaMaxMaxTurn)
	public void setMaxTurning(float maxTurning) {
		this.maxTurning = maxTurning;
	}

	@EvolvableObject(
			name="Cell Colour",
			geneClassName="com.protoevo.biology.protozoa.ProtozoaColorGene")
	public void setColour(Color colour) {
		setHealthyColour(colour);
	}

	@EvolvableFloat(name="Growth Rate", min=.05f, max=.1f)
	public void setGrowth(float growthRate) {
		setGrowthRate(growthRate);
	}

	@EvolvableFloat(name="Retinal Production")
	public void setRetinalProduction(float production) {
		setComplexMoleculeProductionRate(Food.ComplexMolecule.Retinal, production);
	}

	@EvolvableObject(
			name="CAM Production",
			geneClassName="com.protoevo.biology.protozoa.CAMProductionGene")
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
	@GeneRegulator(name="Size", min=ProtozoaSettings.minProtozoanBirthRadius, max=ProtozoaSettings.maxProtozoanSplitRadius)
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
		float extraction = 5f * getRadius() / e.getRadius();
		if (e instanceof PlantCell) {
//			if (spikes.getNumSpikes() > 0)
//				extraction *= Math.pow(ProtozoaSettings.spikePlantConsumptionPenalty, spikes.getNumSpikes());
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
		super.kill(causeOfDeath);
		getEnv().requestBurst(
				this,
				MeatCell.class,
				r -> new MeatCell(r, getEnv()),
				true);
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
		return getNumOfAttachments(SpikeAttachment.class);
	}

	public int getNumLightSensitiveNodes() {
		return getNumOfAttachments(LightSensitiveAttachment.class);
	}

	@Override
	public Map<String, Float> getStats() {
		Map<String, Float> stats = super.getStats();
		stats.put("Death Rate", 100 * damageRate);
		stats.put("Split Radius", Settings.statsDistanceScalar * splitRadius);
		stats.put("Max Turning", maxTurning);
		stats.put("Has Mated", crossOverGenome == null ? 0f : 1f);
		int numSpikes = getNumSpikes();
		if (numSpikes > 0)
			stats.put("Num Spikes", (float) numSpikes);
//		if (brain instanceof NNBrain) {
//			NeuralNetwork nn = ((NNBrain) brain).network;
//			stats.put("Network Depth", (float) nn.getDepth());
//			stats.put("Network Size", (float) nn.getSize());
//		}
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
		for (Contact contact : getContacts()) {
			Object collided = getOther(contact);
			if (collided instanceof EdibleCell) {
				eat((EdibleCell) collided, delta);
			}
		}
	}

//	private void maintainRetina(float delta) {
//		float availableRetinal = getComplexMoleculeAvailable(Food.ComplexMolecule.Retinal);
//		float usedRetinal = retina.updateHealth(delta, availableRetinal);
//		depleteComplexMolecule(Food.ComplexMolecule.Retinal, usedRetinal);
//	}

	@Override
	public boolean isEdible() {
		return false;
	}


//	public Brain getBrain() {
//		return brain;
//	}


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
}
