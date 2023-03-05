package com.protoevo.biology.cells;

import com.badlogic.gdx.math.Vector2;
import com.protoevo.biology.*;
import com.protoevo.biology.evolution.*;
import com.protoevo.biology.nn.NeuralNetwork;
import com.protoevo.biology.nodes.*;
import com.protoevo.biology.organelles.Organelle;
import com.protoevo.core.Statistics;
import com.protoevo.env.Environment;
import com.protoevo.physics.CollisionHandler;
import com.protoevo.utils.Colour;
import com.protoevo.utils.Utils;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Protozoan extends Cell implements Evolvable
{
	
	private static final long serialVersionUID = 2314292760446370751L;

	private GeneExpressionFunction geneExpressionFunction;
	private GeneExpressionFunction crossOverGenome;
	private float matingCooldown = 0;
	private boolean mateDesire;
	private List<SurfaceNode> surfaceNodes;

	private float damageRate = 0;
	private float herbivoreFactor, splitRadius;
	private float timeSinceLastGeneUpdate = 0;
	private final Vector2 tmp = new Vector2();
	private final Collection<Cell> engulfedCells = new ArrayList<>(0);
	private final Vector2 thrust = new Vector2();
	private float thrustAngle = (float) (2 * Math.PI * Math.random());
	private float thrustTurn = 0, thrustMag;

	@Override
	public void update(float delta)
	{
		super.update(delta);

		timeSinceLastGeneUpdate += delta;
		if (timeSinceLastGeneUpdate >= Environment.settings.protozoa.geneExpressionInterval.get()) {
			geneExpressionFunction.update();
			timeSinceLastGeneUpdate = 0;
		}
		age(delta);

		if (surfaceNodes != null)
			for (SurfaceNode node : surfaceNodes)
				node.update(delta);

		for (Cell engulfedCell : engulfedCells) {
			handleEngulfing(engulfedCell, delta);
			eat(engulfedCell, Environment.settings.protozoa.engulfEatingRateMultiplier.get() * delta);
		}
		engulfedCells.removeIf(this::removeEngulfedCondition);

		if (shouldSplit() && hasNotBurst()) {
			getEnv().requestBurst(this, Protozoan.class, this::createSplitChild);
		}

		generateThrust(delta);
		handleMating(delta);
	}

	private void handleMating(float delta) {
		if (Environment.settings.protozoa.matingEnabled.get() && mateDesire && matingCooldown <= 0) {
			for (CollisionHandler.Collision contact : getContacts()) {
				Object other = getOther(contact);
				if (other instanceof Protozoan) {
					Protozoan protozoan = (Protozoan) other;
					if (protozoan.mateDesire) {
						crossOverGenome = protozoan.geneExpressionFunction;
						protozoan.crossOverGenome = geneExpressionFunction;
						protozoan.matingCooldown = 1;
					}
				}
			}
		} else {
			matingCooldown -= delta;
		}
	}

	private boolean removeEngulfedCondition(Cell c) {
		return c.getHealth() <= 0f;
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

	@EvolvableFloat(name="Split Radius", min=0, max=1)
	public void setSplitRadius(float splitRadius) {
		this.splitRadius = Utils.clampedLinearRemap(
				splitRadius, 0, 1,
				Environment.settings.protozoa.maxProtozoanBirthRadius.get(),
				Environment.settings.maxParticleRadius.get()
		);
	}

	@EvolvableObject(name="Cell Colour",
					 traitClass ="com.protoevo.biology.cells.ProtozoaColourTrait")
	public void setColour(Colour colour) {
		setHealthyColour(colour);
		setDegradedColour(degradeColour(colour, 0.3f));
	}

	@EvolvableFloat(name="Growth Rate", min=0, max=1)
	public void setGrowth(float growthRate) {
		growthRate = Utils.clampedLinearRemap(
				growthRate, 0, 1,
				Environment.settings.protozoa.minProtozoanGrowthRate.get(),
				Environment.settings.protozoa.maxProtozoanGrowthRate.get()
		);
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
	@GeneRegulator(name="Size", min=0, max=1)
	public float getRadius() {
		return Utils.clampedLinearRemap(
				super.getRadius(),
				Environment.settings.minParticleRadius.get(),
				Environment.settings.maxParticleRadius.get(),
				0, 1
		);
	}

	@GeneRegulator(name="Construction Mass Available")
	public float getConstructionMass() {
		return getConstructionMassAvailable() / getConstructionMassCap();
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

	@GeneRegulator(name="Contact Sensor")
	public float getContact() {
		return getContacts().size() > 0 ? 1 : 0;
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

	@ControlVariable(name="Mate Desire", min=0, max=1)
	public void setMateDesire(float mate) {
		this.mateDesire = mate > 0.5f;
	}

	public void generateThrust(float delta) {
		if (thrustMag <= 1e-12)
			return;

		thrustAngle += delta * thrustTurn;
		thrust.set(thrustMag * (float) Math.cos(thrustAngle),
				   thrustMag * (float) Math.sin(thrustAngle));

		generateMovement(thrust);
	}

	private boolean shouldSplit() {
		return getRadius() >= splitRadius
				&& getHealth() >= Environment.settings.protozoa.minHealthToSplit.get();
	}

	private Protozoan createSplitChild(float r) {
		Protozoan child;
		if (crossOverGenome != null) {
			child = Evolvable.createChild(this.getClass(), this.getGeneExpressionFunction(), crossOverGenome);
			getEnv().incrementCrossOverCount();
		} else
			child = Evolvable.asexualClone(this);

		child.setRadius(r);
		child.setEnv(getEnv());

		return child;
	}

	@Override
	public float getInteractionRange() {
		if (surfaceNodes == null)
			return 0;

		float maxRange = 0;
		for (SurfaceNode node : surfaceNodes)
			maxRange = Math.max(maxRange, node.getInteractionRange());
		return maxRange;
	}

	@Override
	public boolean canPossiblyInteract() {
		return true;
	}

	@Override
	public void kill(CauseOfDeath causeOfDeath) {
		if (!super.isDead() && hasNotBurst() && getEnv() != null)
			getEnv().requestBurst(
					this,
					MeatCell.class,
					this::createMeat,
					true);
		super.kill(causeOfDeath);
	}

	private MeatCell createMeat(float r) {
		return new MeatCell(r, getEnv());
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
				tmp.setLength(Environment.settings.protozoa.engulfForce.get() * delta * (rr*rr - d2));
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
	public GeneExpressionFunction getGeneExpressionFunction() {
		return geneExpressionFunction;
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
}
