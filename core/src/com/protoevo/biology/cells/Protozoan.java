package com.protoevo.biology.cells;

import com.badlogic.gdx.math.Vector2;
import com.protoevo.biology.*;
import com.protoevo.biology.evolution.*;
import com.protoevo.biology.nn.NeuralNetwork;
import com.protoevo.biology.nodes.*;
import com.protoevo.biology.organelles.Organelle;
import com.protoevo.core.Statistics;
import com.protoevo.env.ChemicalSolution;
import com.protoevo.env.Environment;
import com.protoevo.maths.Functions;
import com.protoevo.physics.Collision;
import com.protoevo.utils.Colour;


import java.io.Serializable;
import java.util.*;

public class Protozoan extends EvolvableCell
{
	private static final long serialVersionUID = 2314292760446370751L;

	private GeneExpressionFunction crossOverGenome;
	private float matingCooldown = 0;
	private boolean mateDesire, splitDesire = false;
	private List<SurfaceNode> surfaceNodes;

	private float damageRate = 0;
	private float herbivoreFactor, splitRadius;
	private final Vector2 tmp = new Vector2();
	private final Collection<Cell> engulfedCells = new ArrayList<>(0);
	private final Vector2 thrust = new Vector2(), dir = new Vector2();
	private float thrustAngle = (float) (2 * Math.PI * Math.random());
	private float thrustTurn = 0, thrustMag;

	public static class LineageTag implements Serializable, Comparable<LineageTag> {
		public static final long serialVersionUID = 1L;
		public String tag;
		public float timeStamp;
		public int generation;

		public LineageTag(String tag, float timeStamp, int generation) {
			this.tag = tag;
			this.timeStamp = timeStamp;
			this.generation = generation;
		}

		@Override
		public int compareTo(LineageTag o) {
			return Integer.compare(generation, o.generation);
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof LineageTag))
				return false;
			return tag.equals(((LineageTag) obj).tag);
		}
	}

	private final Set<LineageTag> tags = new HashSet<>();

	@Override
	public void update(float delta)
	{
		super.update(delta);
		age(delta);

		if (surfaceNodes != null) {
			for (int i = 0; i < surfaceNodes.size(); i++) {
				SurfaceNode node = surfaceNodes.get(i);
				node.setIndex(i);
				node.update(delta);
			}
		}

		for (Cell engulfedCell : engulfedCells) {
			handleEngulfing(engulfedCell, delta);
			eat(engulfedCell, Environment.settings.protozoa.engulfEatingRateMultiplier.get() * delta);
		}
		engulfedCells.removeIf(this::removeEngulfedCondition);

		if (shouldSplit() && hasNotBurst()) {
			getEnv().ifPresent(e -> e.requestBurst(this, Protozoan.class, this::createSplitChild));
		}

		generateThrust(delta);
		handleMating(delta);

		getParticle().setRangedInteractionRadius(getInteractionRange());
	}

	private void handleMating(float delta) {
		if (Environment.settings.protozoa.matingEnabled.get() && mateDesire && matingCooldown <= 0) {
			for (Collision contact : getParticle().getContacts()) {
				Object other = contact.getOther(contact);
				if (other instanceof Protozoan) {
					Protozoan protozoan = (Protozoan) other;
					if (protozoan.mateDesire) {
						setMate(protozoan);
						return;
					}
				}
			}
		} else {
			matingCooldown -= delta;
		}
	}

	public void setMate(Protozoan other) {
		crossOverGenome = other.getGeneExpressionFunction();
		other.crossOverGenome = getGeneExpressionFunction();
		other.matingCooldown = 1;
	}

	private boolean removeEngulfedCondition(Cell c) {
		return c.getHealth() <= 0f;
	}

	@EvolvableList(
			name = "Surface Nodes",
			elementClassPath = "com.protoevo.biology.nodes.SurfaceNode",
			minSize = 1,
			initialSize = 5
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

	@EvolvableFloat(name="Split Radius", min=0, max=1)
	public void setSplitRadius(float splitRadius) {
		this.splitRadius = Functions.clampedLinearRemap(
				splitRadius, 0, 1,
				Environment.settings.protozoa.maxBirthRadius.get(),
				Environment.settings.maxParticleRadius.get()
		);
	}

	@EvolvableObject(name="Cell Colour",
					 traitClass ="com.protoevo.biology.cells.ProtozoaColourTrait")
	public void setColour(Colour colour) {
		setHealthyColour(colour);
		setDegradedColour(degradeColour(colour, 0.3f));
	}

	@GeneRegulator(name="Plant to Digest")
	public float getPlantToDigest() {
		if (!getFoodToDigest().containsKey(Food.Type.Plant))
			return 0;
		return getFoodToDigest().get(Food.Type.Plant).getSimpleMass() / getFoodToDigestMassCap();
	}

	@GeneRegulator(name="Meat to Digest")
	public float getMeatToDigest() {
		if (!getFoodToDigest().containsKey(Food.Type.Meat))
			return 0;
		return getFoodToDigest().get(Food.Type.Meat).getSimpleMass() / getFoodToDigestMassCap();
	}

	@GeneRegulator(name="Plant Gradient", min=-1, max=1)
	public float getPlantGradient() {
		Optional<Environment> env = getEnv();
		if (!env.isPresent())
			return 0;
		ChemicalSolution solution = env.get().getChemicalSolution();
		if (solution == null)
			return 0;

		Vector2 pos = getPos();
		tmp.set(pos).add(dir.setLength(1.1f * getRadius()));
		float plantGradientAhead = solution.getPlantDensity(tmp);
		tmp.set(pos).sub(dir.setLength(1.1f * getRadius()));
		float plantGradientBehind = solution.getPlantDensity(tmp);
		return plantGradientAhead - plantGradientBehind;
	}

	@ControlVariable(name="Cilia Thrust", min=0, max=1)
	public void setCiliaThrust(float thrust) {
		float sizePenalty = getRadius() / Environment.settings.maxParticleRadius.get();
		this.thrustMag = sizePenalty * thrust * Environment.settings.protozoa.maxCiliaThrust.get();
	}

	@ControlVariable(name="Cilia Turn", min=0, max=1)
	public void setCiliaTurn(float turn) {
		this.thrustTurn = Environment.settings.protozoa.maxCiliaTurn.get() * turn;
	}

	@GeneRegulator(name="Orientation", min=0, max=1)
	public float getOrientation() {
		return (float) (thrustAngle % (2 * Math.PI)) / (2 * (float) Math.PI);
	}

	@GeneRegulator(name="Speed", min=0, max=1)
	public float getProtozoaSpeed() {
		return getSpeed() / getRadius();
	}

	@ControlVariable(name="Mate Desire", min=0, max=1)
	public void setMateDesire(float mate) {
		this.mateDesire = mate > 0.5f;
	}

	@ControlVariable(name="Split Desire", min=0, max=1)
	public void setSplitDesire(float split) {
		this.splitDesire = split > 0.5f;
	}

	public void generateThrust(float delta) {
		if (thrustMag <= 1e-12)
			return;

		thrustAngle += delta * thrustTurn;
		dir.set((float) Math.cos(thrustAngle),
				(float) Math.sin(thrustAngle));
		thrust.set(thrustMag * (float) Math.cos(thrustAngle),
				   thrustMag * (float) Math.sin(thrustAngle));

		generateMovement(thrust);
	}

	@Override
	public float getMaxRadius() {
		return 1.05f * splitRadius;
	}

	private boolean shouldSplit() {
		return splitDesire && getRadius() >= splitRadius
				&& getHealth() >= Environment.settings.protozoa.minHealthToSplit.get();
	}

	private Protozoan createSplitChild(float r) {
		Protozoan child;
		if (crossOverGenome != null) {
			child = Evolvable.createChild(this.getClass(), this.getGeneExpressionFunction(), crossOverGenome);
			getEnv().ifPresent(Environment::incrementCrossOverCount);
		} else {
			child = Evolvable.asexualClone(this);
		}

		getEnv().ifPresent(child::setEnvironmentAndBuildPhysics);
		child.setRadius(r);

		child.tags.addAll(tags);

		return child;
	}

	public void tag(String tag) {
		tags.add(new LineageTag(tag, getEnv().map(Environment::getElapsedTime).orElse(0f), getGeneration()));
	}

	@Override
	public boolean isRangedInteractionEnabled() {
		return true;
	}

	public float getInteractionRange() {
		if (surfaceNodes == null)
			return 0;

		float maxRange = 0;
		for (SurfaceNode node : surfaceNodes)
			maxRange = Math.max(maxRange, node.getInteractionRange());
		return maxRange;
	}

	@Override
	public void kill(CauseOfDeath causeOfDeath) {
		kill(causeOfDeath, true);
	}

	public void kill(CauseOfDeath causeOfDeath, boolean burstMeat) {
		if (burstMeat && hasNotBurst())
			getEnv().ifPresent(e -> e.requestBurst(
					this,
					MeatCell.class,
					this::createMeat,
					true));
		super.kill(causeOfDeath);
	}

	private MeatCell createMeat(float r) {
		return new MeatCell(
				r, getEnv().orElseThrow(() -> new RuntimeException("Cannot create meat cell without environment")));
	}

	@Override
	public float getMass() {
		float mass = super.getMass();

		for (Cell engulfedCell : engulfedCells)
			mass += engulfedCell.getMass();

		return mass;
	}

	private void handleEngulfing(Cell e, float delta) {
		// Move engulfed cell towards the centre of this cell
		Vector2 vel = tmp.set(getPos()).sub(e.getPos());
		float d2 = vel.len2();
		vel.setLength(Environment.settings.protozoa.engulfForce.get() * tmp.len2());
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
				float force = Environment.settings.protozoa.engulfForce.get();
				tmp.setLength(force * delta * (rr*rr - d2));
				e.getPos().add(tmp);
			}
		}
	}

	@Override
	public void eat(Cell engulfed, float delta)
	{
		float extraction = .5f;
		if (engulfed instanceof PlantCell) {
			extraction *= getHerbivoreFactor();
		} else if (engulfed instanceof MeatCell) {
			extraction /= getHerbivoreFactor();
		}

		super.eat(engulfed, extraction * delta);
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
		stats.putBoolean("Has Mated", hasMated());
		int numSpikes = getNumSpikes();
		if (numSpikes > 0)
			stats.putCount("Num Spikes", numSpikes);

		GeneExpressionFunction geneExpressionFunction = getGeneExpressionFunction();
		NeuralNetwork grn = geneExpressionFunction.getRegulatoryNetwork();
		if (grn != null) {
			stats.putCount("GRN Depth", grn.getDepth());
			stats.putCount("GRN Size", grn.getSize());
		}
		int numLSN = getNumLightSensitiveNodes();
		if (numLSN > 0) {
			stats.putCount("Light Sensitive Nodes", numLSN);
		}
		int numEngulfed = engulfedCells.size();
		if (numEngulfed > 0) {
			stats.putCount("Num Engulfed", numEngulfed);
		}
		int numBindings = getNumAttachedCells();
		if (numBindings > 0) {
			stats.putCount("Num Cell Bindings", numBindings);
		}

		stats.put("Herbivore Factor", herbivoreFactor);
		stats.putPercentage("Mean Mutation Chance", 100 * geneExpressionFunction.getMeanMutationRate());
		stats.putCount("Num Mutations", geneExpressionFunction.getMutationCount());

		int i = 0;
		for (LineageTag tag : tags) {
			stats.put("Tag " + i, tag.tag + " (Gen " + tag.generation + ")");
			i++;
		}

		return stats;
	}

	public Statistics getAllStats() {
		Statistics stats = new Statistics(getStats());
		for (SurfaceNode node : getSurfaceNodes())
			stats.putAll("Node " + node.getIndex() +": ", node.getStats());
		for (Organelle organelle : getOrganelles())
			stats.putAll("Organelle " + organelle.getIndex() +": ", organelle.getStats());
		stats.putAll(getResourceStats());
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
		damageRate = getRadius() * Environment.settings.protozoa.starvationFactor.get();
		damage(damageRate * delta, CauseOfDeath.OLD_AGE);
	}

	@Override
	public boolean isEdible() {
		return false;
	}

	public boolean hasMated() {
		return crossOverGenome != null;
	}

	public float getSplitRadius() {
		return splitRadius;
	}

	@Override
	public List<SurfaceNode> getSurfaceNodes() {
		return surfaceNodes;
	}

	public Collection<Cell> getEngulfedCells() {
		return engulfedCells;
	}

	public float getHerbivoreFactor() {
		return herbivoreFactor;
	}

	@Override
	public float getExpressionInterval() {
		return Environment.settings.protozoa.geneExpressionInterval.get();
	}

	@Override
	public float minGrowthRate() {
		return Environment.settings.protozoa.minProtozoanGrowthRate.get();
	}

	@Override
	public float maxGrowthRate() {
		return Environment.settings.protozoa.maxProtozoanGrowthRate.get();
	}
}
