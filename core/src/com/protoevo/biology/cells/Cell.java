package com.protoevo.biology.cells;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.protoevo.biology.*;
import com.protoevo.biology.nodes.SurfaceNode;
import com.protoevo.biology.organelles.Organelle;
import com.protoevo.core.Particle;
import com.protoevo.core.Statistics;
import com.protoevo.settings.Settings;
import com.protoevo.settings.SimulationSettings;
import com.protoevo.env.CollisionHandler;
import com.protoevo.env.JointsManager;
import com.protoevo.env.Rock;
import com.protoevo.utils.Colour;
import com.protoevo.utils.Geometry;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.protoevo.utils.Utils.lerp;

public abstract class Cell extends Particle implements Serializable
{
	@Serial
	private static final long serialVersionUID = 1L;

	private final Colour healthyColour = new Colour(Color.WHITE);
	private final Colour fullyDegradedColour = new Colour(Color.WHITE);
	private int generation = 1;
	protected boolean hasHandledDeath = false;
	private float timeAlive = 0f;
	private float health = 1f;
	private float growthRate = 0.0f;
	private float energyAvailable = SimulationSettings.startingAvailableCellEnergy;
	private float constructionMassAvailable = SimulationSettings.startingAvailableConstructionMass;
	private final Map<ComplexMolecule, Float> availableComplexMolecules = new HashMap<>(0);
	private final ConcurrentLinkedQueue<JointsManager.JoinedParticles> attachedCells = new ConcurrentLinkedQueue<>();
	private final Map<CellAdhesion.CAM, Float> surfaceCAMs = new HashMap<>(0);
	private final Map<Food.Type, Float> foodDigestionRates = new HashMap<>(0);
	private final Map<Food.Type, Food> foodToDigest = new HashMap<>(0);
	private final Set<ConstructionProject> constructionProjects = new HashSet<>(0);
	private final Map<CellAdhesion.CAM, Float> camProductionRates = new HashMap<>(0);
	private final ArrayList<Cell> children = new ArrayList<>();
	private ArrayList<Organelle> organelles = new ArrayList<>();
	private boolean hasBurst= false;
	private float repairRate = 1f;

	private Cell engulfer = null;
	private boolean fullyEngulfed = false;

	public void update(float delta) {

		if (health <= 0.05f && !super.isDead()) {
			kill(CauseOfDeath.HEALTH_TOO_LOW);
			return;
		}

		super.update(delta);
		timeAlive += delta;

		voidDamage(delta);
		digest(delta);
		repair(delta);
		grow(delta);

		organelles.forEach(organelle -> organelle.update(delta));

//		resourceProduction(delta);
//		progressConstructionProjects(delta);

		attachedCells.removeIf(this::detachCellCondition);
	}

	public void voidDamage(float delta) {
		if (getPos().len2() > SimulationSettings.voidStartDistance2)
			damage(delta * SimulationSettings.voidDamagePerSecond, CauseOfDeath.THE_VOID);
	}

	public void requestJointRemoval(JointsManager.JoinedParticles joining) {
		if (attachedCells.contains(joining)) {
			getEnv().getJointsManager().requestJointRemoval(joining);
			attachedCells.remove(joining);
		}
	}

	public void requestJointRemoval(Cell other) {
		JointsManager.JoinedParticles toRemove = null;
		for (JointsManager.JoinedParticles joining : attachedCells) {
			if (joining.getOther(this) == other) {
				getEnv().getJointsManager().requestJointRemoval(joining);
				toRemove = joining;
				break;
			}
		}
		if (toRemove != null)
			attachedCells.remove(toRemove);
	}

	public void setOrganelles(ArrayList<Organelle> organelles) {
		this.organelles = organelles;
	}

	public void addOrganelle(Organelle organelle) {
		organelles.add(organelle);
	}

	public void progressConstructionProjects(float delta) {
		for (ConstructionProject project : constructionProjects) {
			progressProject(project, delta);
		}
	}

	public void progressProject(ConstructionProject project, float delta) {
		if (delta > 0 && project.notFinished() && project.canMakeProgress(
				energyAvailable,
				constructionMassAvailable,
				availableComplexMolecules,
				delta)) {
			depleteEnergy(project.energyToMakeProgress(delta));
			depleteConstructionMass(project.massToMakeProgress(delta));
			if (project.requiresComplexMolecules())
				for (ComplexMolecule molecule : project.getRequiredMolecules()) {
					float amountUsed = project.complexMoleculesToMakeProgress(delta, molecule);
					depleteComplexMolecule(molecule, amountUsed);
				}
			project.progress(delta);
		}
	}

	public void resourceProduction(float delta) {
		for (CellAdhesion.CAM cam : camProductionRates.keySet()) {
			float producedMass = delta * camProductionRates.getOrDefault(cam, 0f);
			float requiredEnergy = cam.getProductionCost() * producedMass;
			if (producedMass > 0 && constructionMassAvailable > producedMass && energyAvailable > requiredEnergy) {
				float currentAmount = surfaceCAMs.getOrDefault(cam, 0f);
				surfaceCAMs.put(cam, currentAmount + producedMass);
				depleteConstructionMass(producedMass);
				depleteEnergy(requiredEnergy);
			}
		}
	}

	public float getDigestionRate(Food.Type foodType) {
		return foodDigestionRates.getOrDefault(foodType, 0f);
	}

	public void setDigestionRate(Food.Type foodType, float rate) {
		foodDigestionRates.put(foodType, rate);
	}

	public void eat(EdibleCell cell, float extraction) {

		Food.Type foodType = cell.getFoodType();
		float extractedMass = cell.getMass() * extraction;
		cell.removeMass(Settings.foodExtractionWasteMultiplier * extractedMass, CauseOfDeath.EATEN);

		Food food;
		if (foodToDigest.containsKey(foodType))
			food = foodToDigest.get(foodType);
		else {
			food = new Food(extractedMass, foodType);
			foodToDigest.put(foodType, food);
		}

		food.addSimpleMass(extractedMass);

		for (ComplexMolecule molecule : cell.getComplexMolecules()) {
			if (cell.getComplexMoleculeAvailable(molecule) > 0) {
				float extractedAmount = extraction * cell.getComplexMoleculeAvailable(molecule);
				cell.depleteComplexMolecule(molecule, extractedAmount);
				food.addComplexMoleculeMass(molecule, extractedMass);
			}
		}
	}

	public void addFood(Food.Type foodType, float amount) {
		Food food = foodToDigest.getOrDefault(foodType, new Food(amount, foodType));
		food.addSimpleMass(amount);
		foodToDigest.put(foodType, food);
	}

	public void digest(float delta) {
		for (Food food : foodToDigest.values()) {
			float rate = delta * SimulationSettings.digestionFactor * getDigestionRate(food.getType());
			if (food.getSimpleMass() > 0) {
				float massExtracted = food.getSimpleMass() * rate;
				addConstructionMass(massExtracted);
				food.subtractSimpleMass(massExtracted);
				energyAvailable += food.getEnergy(massExtracted);
			}
			for (ComplexMolecule molecule : food.getComplexMolecules()) {
				float amount = food.getComplexMoleculeMass(molecule);
				if (amount == 0)
					continue;
				float extracted = Math.min(amount, amount * rate);
				addAvailableComplexMolecule(molecule, extracted);
				food.subtractComplexMolecule(molecule, extracted);
			}
		}
	}

	public void repair(float delta) {
		if (!isDead() && getHealth() < 1f && getRepairRate() > 0) {
			float massRequired = getMass() * 0.01f * delta;
			float energyRequired = massRequired * 3f;
			if (massRequired < constructionMassAvailable
					&& energyRequired < energyAvailable) {
				depleteEnergy(energyRequired);
				depleteConstructionMass(massRequired);
				heal(delta * getRepairRate());
			}
		}
	}

	private float getRepairRate() {
		return Settings.cellRepairRate * repairRate;
	}

	public boolean detachCellCondition(JointsManager.JoinedParticles joining) {
		Cell other = (Cell) joining.getOther(this);

		boolean detach = other.isDead();

		if (detach) {
			requestJointRemoval(joining);
			return true;
		}

		float dist2 = joining.getAnchorA().dst2(joining.getAnchorB());
		float idealLength = JointsManager.idealJointLength(this, other);
		float maxDist = 1.2f * idealLength;
		detach = dist2 > maxDist*maxDist;

		if (detach)
			requestJointRemoval(joining);

		return detach;
	}

	public void addConstructionProject(ConstructionProject project) {
		constructionProjects.add(project);
	}

	public void handleInteractions(float delta) {}

	public void grow(float delta) {
		if (constructionMassAvailable <= 0)
			return;

		float gr = getGrowthRate();
		double dr = SimulationSettings.cellGrowthFactor * ((double) gr) * ((double) delta);
		float newR = (float) ((double) getRadius() + dr);

		if (newR == getRadius() || gr == 0)
			return;

		if (newR > getMaxRadius())
			newR = getMaxRadius();

		float massChange = getMass(newR) - getMass(super.getRadius());

		if (massChange < constructionMassAvailable) {
			newR = (newR - getRadius()) * constructionMassAvailable / massChange + getRadius();
			massChange = constructionMassAvailable;
		}

		if (newR > SimulationSettings.minParticleRadius || gr > 0) {
			setRadius(newR);
			if (massChange > 0)
				depleteConstructionMass(massChange);
		}
		if (newR < getMinRadius())
			kill(CauseOfDeath.GREW_TOO_SMALL);
	}

	public float getMaxRadius() {
		return SimulationSettings.maxParticleRadius;
	}

	public float getMinRadius() {
		return SimulationSettings.minParticleRadius;
	}

	public void setGrowthRate(float gr) {
		growthRate = gr;
	}

	public float getGrowthRate() {
		return growthRate;
	}

	public void registerJoining(JointsManager.JoinedParticles joining) {
		attachedCells.add(joining);
	}

	public void deregisterJoining(JointsManager.JoinedParticles joining) {
		attachedCells.remove(joining);
	}

	public Collection<CellAdhesion.CAM> getSurfaceCAMs() {
		return surfaceCAMs.keySet();
	}

	@Override
	public void onCollision(CollisionHandler.FixtureCollision contact, Rock rock) {
		super.onCollision(contact, rock);
		if (rock.pointInside(getPos())) {
			kill(CauseOfDeath.SUFFOCATION);
		}
	}

	@Override
	public void onCollision(CollisionHandler.FixtureCollision contact, Particle other) {
		super.onCollision(contact, other);
		if (other.isPointInside(getPos())) {
			kill(CauseOfDeath.SUFFOCATION);
		}
	}

	public boolean notBoundTo(Cell otherCell) {
		for (JointsManager.JoinedParticles joining : attachedCells) {
			if (joining.getOther(this) == otherCell)
				return false;
		}
		for (JointsManager.JoinedParticles joining : otherCell.attachedCells) {
			if (joining.getOther(otherCell) == this)
				return false;
		}
		return true;
	}
	
	public abstract boolean isEdible();

	public void heal(float h) {
		damage(-h, CauseOfDeath.HEALED_TO_DEATH);
	}

	public void damage(float d, CauseOfDeath cause)
	{
		health -= d;
		if (health > 1) 
			health = 1;

		if (health < 0.05)
			kill(cause);
	}

	public String getPrettyName() {
		return "Cell";
	}

	public float getTimeAlive() {
		return timeAlive;
	}

	public float getKineticEnergyRequiredForThrust(Vector2 thrustVector) {
		float speed = getSpeed();
		float mass = getMass();
		return .5f * mass * (speed*speed - thrustVector.len2() / (mass * mass));
	}

	public void generateMovement(Vector2 thrustVector) {
		generateMovement(thrustVector, 0);
	}

	public void generateMovement(Vector2 thrustVector, float torque) {
		float work = getKineticEnergyRequiredForThrust(thrustVector);
		// TODO: add torque to work

		if (enoughEnergyAvailable(work)) {
			depleteEnergy(work);
			applyImpulse(thrustVector);
			applyTorque(torque);
		}
		else if (getEnergyAvailable() > 0) {
			// not accurate scaling of thrust and torque
			thrustVector.scl(getEnergyAvailable() / work);
			torque = torque * getEnergyAvailable() / work;
			setEnergyAvailable(0);
			applyImpulse(thrustVector);
			applyTorque(torque);
		}
	}

	public Statistics getStats() {
		Statistics stats = super.getStats();
		stats.putTime("Age", timeAlive);
		stats.putPercentage("Health", 100 * getHealth());
		stats.putCount("Generation", getGeneration());
		stats.putEnergy("Available Energy", energyAvailable);
		stats.putMass("Construction Mass", constructionMassAvailable);
//		if (wasteMass > 0)
//			stats.putMass("Waste Mass", wasteMass);

		stats.putSpeed("Growth Rate", getGrowthRate());
		stats.put("Repair Rate", 100 * getRepairRate(),
				Statistics.ComplexUnit.PERCENTAGE_PER_TIME);

		for (ComplexMolecule molecule : availableComplexMolecules.keySet())
			if (availableComplexMolecules.get(molecule) > 0)
				stats.putMass("Molecule %.2f Available".formatted(molecule.getSignature()),
						availableComplexMolecules.get(molecule));

		if (attachedCells.size() > 0)
			stats.putCount("Num Cell Bindings", attachedCells.size());

		for (CellAdhesion.CAMJunctionType junctionType : CellAdhesion.CAMJunctionType.values()) {
			float camMass = 0;
			for (CellAdhesion.CAM molecule : surfaceCAMs.keySet())
				if (molecule.getJunctionType().equals(junctionType))
					camMass += surfaceCAMs.get(molecule);
			if (camMass > 0)
				stats.putMass(junctionType + " CAM Mass", camMass);
		}

		stats.putBoolean("Being Engulfed", engulfer != null);

		Statistics.ComplexUnit massPerTime = Statistics.ComplexUnit.MASS_PER_TIME;

		for (Food.Type foodType : foodDigestionRates.keySet())
			if (foodDigestionRates.get(foodType) > 0)
				stats.put(foodType + " Digestion Rate", foodDigestionRates.get(foodType), massPerTime);

		for (Food food : foodToDigest.values())
			stats.putMass(food + " to Digest", food.getSimpleMass());

		return stats;
	}

	public Statistics getDebugStats() {
		Statistics stats = super.getDebugStats();
		stats.put("Num Attached Cells", (float) attachedCells.size());
		return stats;
	}
	
	public float getHealth() {
		return MathUtils.clamp(health, 0, 1);
	}

	public boolean isDead() {
		return super.isDead();
	}

	public void kill(CauseOfDeath causeOfDeath) {
		super.kill(causeOfDeath);
	}

	@Override
	public Colour getColour() {
		Colour healthyColour = getHealthyColour();
		Colour degradedColour = getFullyDegradedColour();
		return lerp(healthyColour, degradedColour, 1 - getHealth());
	}

	public Collection<Organelle> getOrganelles() {
		return organelles;
	}

	public Colour getHealthyColour() {
		return healthyColour;
	}

	public void setHealthyColour(Colour healthyColour) {
		this.healthyColour.set(healthyColour);
	}

	public void setDegradedColour(Colour fullyDegradedColour) {
		this.fullyDegradedColour.set(fullyDegradedColour);
	}

	public Colour getFullyDegradedColour() {
		return fullyDegradedColour;
	}

	public int getGeneration() {
		return generation;
	}

	public void setGeneration(int generation) {
		this.generation = generation;
	}

	public int burstMultiplier() {
		return 1;
	}

	public float getMinBurstRadius() {
		return 2 * SimulationSettings.minParticleRadius;
	}

	public void setFoodToDigest(Food.Type foodType, Food food) {
		foodToDigest.put(foodType, food);
	}

	public Collection<Cell> getChildren() {
		return children;
	}

	public float getCAMAvailable(CellAdhesion.CAM cam) {
		return surfaceCAMs.getOrDefault(cam, 0f);
	}

	public void setCAMAvailable(CellAdhesion.CAM cam, float amount) {
		surfaceCAMs.put(cam, amount);
	}

	public boolean enoughEnergyAvailable(float work) {
		return work < energyAvailable;
	}

	public float getEnergyAvailable() {
		return energyAvailable;
	}

	public void addAvailableEnergy(float energy) {
		energyAvailable = Math.min(energyAvailable + energy, getAvailableEnergyCap()) ;
	}

	private float getAvailableEnergyCap() {
		return SimulationSettings.startingAvailableCellEnergy * getRadius() / SimulationSettings.minParticleRadius;
	}

	public void setEnergyAvailable(float energy) {
		energyAvailable = energy;
	}

	public void depleteEnergy(float energy) {
		energyAvailable = Math.max(0, energyAvailable - energy);
	}

	public Collection<ComplexMolecule> getComplexMolecules() {
		return availableComplexMolecules.keySet();
	}

	public void depleteComplexMolecule(ComplexMolecule molecule, float amount) {
		float currAmount = getComplexMoleculeAvailable(molecule);
		setComplexMoleculeAvailable(molecule, currAmount - amount);
	}

	public float getComplexMoleculeAvailable(ComplexMolecule molecule) {
		return availableComplexMolecules.getOrDefault(molecule, 0f);
	}

	public void addAvailableComplexMolecule(ComplexMolecule molecule, float amount) {
		float currentAmount = availableComplexMolecules.getOrDefault(molecule, 0f);
		float newAmount = Math.min(getComplexMoleculeMassCap(), currentAmount + amount);
		availableComplexMolecules.put(molecule, newAmount);
	}

	private float getComplexMoleculeMassCap() {
		return getMass(getRadius() * 0.1f);
	}

	public void setComplexMoleculeAvailable(ComplexMolecule molecule, float amount) {
		availableComplexMolecules.put(molecule, Math.max(0, amount));
	}

	public float getConstructionMassCap() {
		return 2 * getMassDensity() * Geometry.getSphereVolume(getRadius() * 0.25f);
	}

	public void setAvailableConstructionMass(float mass) {
		constructionMassAvailable = Math.min(mass, getConstructionMassCap());
	}

	public float getConstructionMassAvailable() {
		return constructionMassAvailable;
	}

	public void addConstructionMass(float mass) {
		setAvailableConstructionMass(constructionMassAvailable + mass);
	}

	public void depleteConstructionMass(float mass) {
		constructionMassAvailable = Math.max(0, constructionMassAvailable - mass);
	}

	public void setCAMProductionRate(CellAdhesion.CAM cam, float rate) {
		camProductionRates.put(cam, rate);
	}

	@Override
	public float getMass() {
		float extraMass = constructionMassAvailable;
		for (float mass : availableComplexMolecules.values())
			extraMass += mass;
		return getMass(getRadius(), extraMass);
	}

	/**
	 * Changes the radius of the cell to remove the given amount of mass
	 * @param mass mass to remove
	 */
	public void removeMass(float mass, CauseOfDeath causeOfDeath) {
		float percentRemoved = mass / getMass();
		damage(percentRemoved, causeOfDeath);

		float newR = (1 - percentRemoved) * getRadius();
		if (newR < SimulationSettings.minParticleRadius * 0.5f)
			kill(causeOfDeath);

		setRadius(newR);
	}

	public void removeMass(float mass) {
		removeMass(mass, CauseOfDeath.LOST_TOO_MUCH_MASS);
	}

	public Map<Food.Type, Food> getFoodToDigest() {
		return foodToDigest;
	}

	public Collection<JointsManager.JoinedParticles> getAttachedCells() {
		return attachedCells;
	}

	public float getShieldFactor() {
		return 1.3f;
	}

	public void setHasBurst(boolean hasBurst) {
		this.hasBurst = hasBurst;
	}

	public boolean hasNotBurst() {
		return !hasBurst;
	}

	public void setRepairRate(float repairRate) {
		this.repairRate = repairRate;
	}

	public void setEngulfer(Cell engulfer) {
		this.engulfer = engulfer;
	}

	public boolean isEngulfed() {
		return engulfer != null;
	}

	public boolean isFullyEngulfed() {
		return fullyEngulfed;
	}

	public void setFullyEngulfed() {
		this.fullyEngulfed = true;
	}

	public Collection<SurfaceNode> getSurfaceNodes() {
		return null;
	}
}
