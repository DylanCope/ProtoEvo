package com.protoevo.biology.protozoa;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.protoevo.biology.*;
import com.protoevo.biology.evolution.*;
import com.protoevo.biology.neat.NetworkGenome;
import com.protoevo.biology.neat.NeuralNetwork;
import com.protoevo.biology.nodes.*;
import com.protoevo.core.settings.ProtozoaSettings;
import com.protoevo.core.settings.Settings;
import com.protoevo.core.Simulation;
import com.protoevo.core.settings.SimulationSettings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class Protozoan extends Cell implements Evolvable
{
	private static final long serialVersionUID = 2314292760446370751L;
	public transient int id = Simulation.RANDOM.nextInt();

	private GeneExpressionFunction genome;
	private GeneExpressionFunction crossOverGenome;
	private Protozoan mate;
	private float timeMating = 0;
	public boolean wasJustDamaged = false;
	private List<SurfaceNode> surfaceNodes;
	private List<Object> toInteract;

	private float shieldFactor = 1.3f;
	private final float attackFactor = 10f;
	private float damageRate = 0;
	private final Vector2 dir = new Vector2(0, 0);
	private Brain brain;
	private float herbivoreFactor, splitRadius, maxTurning, growthControlFactor = 1f;
	private float timeSinceLastGeneUpdate = 0;
	private final float geneUpdateTime = Settings.simulationUpdateDelta * 20;

	public Protozoan()
	{
		super();
		float t = (float) (2 * Math.PI * Simulation.RANDOM.nextDouble());
		dir.set(
				(float) (0.1f * Math.cos(t)),
				(float) (0.1f * Math.sin(t))
		);

		surfaceNodes = new ArrayList<>();
		SurfaceNode flagellaNode = new SurfaceNode(this, 0);
		flagellaNode.setAttachment(new FlagellumAttachment(flagellaNode));
		surfaceNodes.add(flagellaNode);

		SurfaceNode lightSensitiveNode = new SurfaceNode(this, (float) Math.PI);
		lightSensitiveNode.setAttachment(new LightSensitiveAttachment(lightSensitiveNode));
		surfaceNodes.add(lightSensitiveNode);

		SurfaceNode spikeNode1 = new SurfaceNode(this, (float) (3 * Math.PI / 4));
		spikeNode1.setAttachment(new SpikeAttachment(spikeNode1));
		surfaceNodes.add(spikeNode1);

		SurfaceNode spikeNode2 = new SurfaceNode(this, (float) (5 * Math.PI / 4));
		spikeNode2.setAttachment(new SpikeAttachment(spikeNode2));
		surfaceNodes.add(spikeNode2);

	}

	@Override
	@EvolvableComponent
	public void setGeneExpressionFunction(GeneExpressionFunction fn) {
		this.genome = fn;
	}

	@EvolvableObject(
			name="Brain",
			geneClassName="com.protoevo.biology.protozoa.ProtozoaControlNetworkGene")
	public void setBrain(NetworkGenome networkGenome) {
		this.brain = new NNBrain(networkGenome.phenotype());
	}

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
	
	public void attack(Protozoan p, Spikes.Spike spike, float delta)
	{
		float myAttack = (float) (
				2*getHealth() +
				ProtozoaSettings.spikeDamage * getSpikeLength(spike) +
				2*Simulation.RANDOM.nextDouble()
		);
		float theirDefense = (float) (
				2*p.getHealth() +
				0.3*p.getRadius() +
				2*Simulation.RANDOM.nextDouble()
		);

		if (myAttack > p.shieldFactor * theirDefense)
			p.damage(delta * attackFactor * (myAttack - p.shieldFactor * theirDefense), CauseOfDeath.MURDER);

	}
	
	public void think(float delta)
	{
		brain.tick(this);
//		dir.rotateRad(delta * 80 * maxTurning * brain.turn(this));
//		growthControlFactor = brain.growthControl();
//		float spikeDecay = (float) Math.pow(ProtozoaSettings.spikeMovementPenalty, spikes.getNumSpikes());
//		float sizePenalty = getRadius() / SimulationSettings.maxParticleRadius; // smaller flagella generate less impulse
//		float speed = Math.abs(brain.speed(this));
//		Vector2 impulse = dir.cpy().setLength(.05f * sizePenalty * speed);
//		applyImpulse(impulse);
//		float v1 = getSpeed();
//		float m = getMass();
//		float work = .5f * m * (v1*v1 - impulse.len2() / (m * m));  // change in kinetic energy
//		if (enoughEnergyAvailable(work)) {
//			useEnergy(work);
//			applyImpulse(impulse);
//		}
	}

	private boolean shouldSplit() {
		return getRadius() > splitRadius && getHealth() > ProtozoaSettings.minHealthToSplit;
	}

	private Protozoan createSplitChild(float r) {
		float stuntingFactor = r / getRadius();
		Protozoan child;
		if (crossOverGenome != null)
			child = Evolvable.createChild(this.getClass(), this.getGeneExpressionFunction(), crossOverGenome);
		else
			child = Evolvable.asexualClone(this);

		child.setEnv(getEnv());
		return child;
	}

	public void handleInteractions(float delta) {
		// no point in interacting if you're dead.
		if (isDead()) {
			return;
		}

		if (toInteract != null)
			toInteract.forEach(o -> interact(o, delta));
	}

	public void interact(Object other, float delta) {
		if (other == this)
			return;
//		if (other instanceof Collidable &&
//				canSeeCondition((Collidable) other))
//				see((Collidable) other);
		if (other instanceof Cell)
			handleSpikeAttacks((Cell) other, delta);
	}

//	public boolean canSeeCondition(Collidable other) {
//		return retina.numberOfCells() > 0 &&
//				retina.getHealth() > 0 &&
//				cullFromRayCasting(other);
//	}

	public void handleSpikeAttacks(Cell other, float delta) {

//		float d = other.getPos().dst(getPos());
//
//		float r = getRadius() + other.getRadius();
//
//		if (other instanceof Protozoan) {
//			Protozoan p = (Protozoan) other;
//			for (Spikes.Spike spike : spikes.getSpikes()) {
//				float spikeLen = getSpikeLength(spike);
//				if (d < r + spikeLen && spikeInContact(spike, spikeLen, p))
//					attack(p, spike, delta);
//			}
//		}
	}

	private boolean spikeInContact(Spikes.Spike spike, float spikeLen, Cell other) {
		Vector2 spikeStartPos = getDir().cpy().nor().rotateRad(spike.angle).setLength(getRadius()).add(getPos());
		Vector2 spikeEndPos = spikeStartPos.add(spikeStartPos.sub(getPos()).setLength(spikeLen));
		return other.getPos().sub(spikeEndPos).len2() < other.getRadius() * other.getRadius();
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

//	@Override
//	public boolean doesInteract() {
//		return retina.numberOfCells() > 0 && retina.getHealth() > 0 || spikes.getNumSpikes() > 0;
//	}

//	@Override
//	public void reset() {
//		super.reset();
//		retina.reset();
//	}

	@Override
	public void kill(CauseOfDeath causeOfDeath) {
		super.kill(causeOfDeath);
		getEnv().requestBurst(this, MeatCell.class, r -> new MeatCell(r, getEnv()));
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
		if (brain instanceof NNBrain) {
			NeuralNetwork nn = ((NNBrain) brain).network;
			stats.put("Network Depth", (float) nn.getDepth());
			stats.put("Network Size", (float) nn.getSize());
		}
		int numLSN = getNumLightSensitiveNodes();
		if (numLSN > 0) {
			stats.put("Light Sensitive Nodes", (float) numLSN);
		}
		stats.put("Herbivore Factor", herbivoreFactor);
		stats.put("Mutation Chance", 100 * genome.getMutationRate());
		return stats;
	}

//	@Override
//	public float getGrowthRate() {
//		float growthRate = super.getGrowthRate();
//		if (getRadius() > splitRadius)
//			growthRate *= getHealth() * splitRadius / (5 * getRadius());
////		for (Spike spike : spikes)
////			growthRate -= ProtozoaSettings.spikeGrowthPenalty * spike.growthRate;
////		growthRate -= ProtozoaSettings.retinaCellGrowthCost * retina.numberOfCells();
//		return growthRate; // * (0.25f + 0.75f * growthControlFactor);
//	}

	public void age(float delta) {
		damageRate = getRadius() * ProtozoaSettings.protozoaStarvationFactor;
//		damageRate += ProtozoaSettings.spikeDeathRatePenalty + spikes.getNumSpikes();
		damage(damageRate * delta, CauseOfDeath.OLD_AGE);
	}

	@Override
	public void update(float delta)
	{
		super.update(delta);

//		timeSinceLastGeneUpdate += delta;
//		if (timeSinceLastGeneUpdate >= geneUpdateTime) {
//			getGeneExpressionFunction().update();
//			timeSinceLastGeneUpdate = 0f;
//		}
		age(delta);
		think(delta);
		handleCollisions(delta);
		surfaceNodes.forEach(n -> n.update(delta));

		if (shouldSplit()) {
			getEnv().requestBurst(this, Protozoan.class, this::createSplitChild);
		}
	}

	public void handleCollisions(float delta) {
		for (Object collided : getContactObjects()) {
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

	public float getShieldFactor() {
		return shieldFactor;
	}

	public void setShieldFactor(float shieldFactor) {
		this.shieldFactor = shieldFactor;
	}

	public Brain getBrain() {
		return brain;
	}

	public float getSpikeLength(Spikes.Spike spike) {
		return brain.attack(this) * spike.currentLength * getRadius() / splitRadius;
	}

	public Vector2 getDir() {
		return dir;
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
		return genome;
	}

	public Collection<SurfaceNode> getSurfaceNodes() {
		return surfaceNodes;
	}
}
