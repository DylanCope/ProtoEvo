package com.protoevo.biology;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.protoevo.core.Particle;
import com.protoevo.core.settings.Settings;
import com.protoevo.core.settings.SimulationSettings;
import com.protoevo.env.CollisionHandler;
import com.protoevo.env.JointsManager;
import com.protoevo.env.Rock;
import com.protoevo.utils.Geometry;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.protoevo.utils.Utils.lerp;

public abstract class Cell extends Particle implements Serializable
{
	private static final long serialVersionUID = -4333766895269415282L;

	private Color healthyColour = new Color(Color.WHITE), fullyDegradedColour = new Color(Color.WHITE);
	private int generation = 1;
	protected boolean hasHandledDeath = false;
	private float timeAlive = 0f;
	private float health = 1f;
	private float growthRate = 0.0f;
	private float energyAvailable = SimulationSettings.startingAvailableCellEnergy;
	private float constructionMassAvailable, wasteMass;
	private final Map<Food.ComplexMolecule, Float> availableComplexMolecules = new HashMap<>(0);
	private int maxAttachedCells = 0;
	private final ConcurrentLinkedQueue<Cell> attachedCells = new ConcurrentLinkedQueue<>();
	private final ConcurrentLinkedQueue<Cell> cellsToDetach = new ConcurrentLinkedQueue<>();
	private final ConcurrentLinkedQueue<CellAdhesion.Binding> cellBindings = new ConcurrentLinkedQueue<>();
	private final Map<CellAdhesion.CAM, Float> surfaceCAMs = new HashMap<>(0);
	private final Map<Food.Type, Float> foodDigestionRates = new HashMap<>(0);
	private final Map<Food.Type, Food> foodToDigest = new HashMap<>(0);
	private final Set<ConstructionProject> constructionProjects = new HashSet<>(0);
	private final Map<Food.ComplexMolecule, Float> complexMoleculeProductionRates = new HashMap<>(0);
	private final Map<CellAdhesion.CAM, Float> camProductionRates = new HashMap<>(0);
	private final ArrayList<Cell> children = new ArrayList<>();
	private boolean hasBurst= false;
	private float repairRate = 1f;

	private Cell engulfer = null;
	private boolean fullyEngulfed = false;

	public void update(float delta) {
		super.update(delta);
		timeAlive += delta;

		voidDamage(delta);
		digest(delta);
		repair(delta);
		grow(delta);
		resourceProduction(delta);
		progressConstructionProjects(delta);

		cellsToDetach.clear();
		attachedCells.stream()
				.filter(this::detachCellCondition)
				.forEach(this::requestJointRemoval);
		attachedCells.removeIf(cellsToDetach::contains);
		cellsToDetach.clear();

		for (CellAdhesion.Binding binding : cellBindings)
			handleBindingInteraction(binding, delta);
	}

	public void voidDamage(float delta) {
		if (getPos().len2() > SimulationSettings.voidStartDistance2)
			damage(delta * SimulationSettings.voidDamagePerSecond, CauseOfDeath.THE_VOID);
	}

	private void requestJointRemoval(Cell other) {
		getEnv().getJointsManager().requestJointRemoval(this, other);
		cellsToDetach.add(other);
	}

	public void progressConstructionProjects(float delta) {
		for (ConstructionProject project : constructionProjects) {
			if (project.notFinished() && project.canMakeProgress(
					energyAvailable,
					constructionMassAvailable,
					availableComplexMolecules,
					delta)) {
				useEnergy(project.energyToMakeProgress(delta));
				useConstructionMass(project.massToMakeProgress(delta));
				if (project.requiresComplexMolecules())
					for (Food.ComplexMolecule molecule : project.getRequiredMolecules()) {
						float amountUsed = project.complexMoleculesToMakeProgress(delta, molecule);
						depleteComplexMolecule(molecule, amountUsed);
					}
				project.progress(delta);
			}
		}
	}

	public void resourceProduction(float delta) {
		for (Food.ComplexMolecule molecule : complexMoleculeProductionRates.keySet()) {
			float producedMass = delta * complexMoleculeProductionRates.getOrDefault(molecule, 0f);
			float requiredEnergy = molecule.getProductionCost() * producedMass;
			if (producedMass > 0 && constructionMassAvailable > producedMass && energyAvailable > requiredEnergy) {
				addAvailableComplexMolecule(molecule, producedMass);
				useConstructionMass(producedMass);
				useEnergy(requiredEnergy);
			}
		}
		for (CellAdhesion.CAM cam : camProductionRates.keySet()) {
			float producedMass = delta * camProductionRates.getOrDefault(cam, 0f);
			float requiredEnergy = cam.getProductionCost() * producedMass;
			if (producedMass > 0 && constructionMassAvailable > producedMass && energyAvailable > requiredEnergy) {
				float currentAmount = surfaceCAMs.getOrDefault(cam, 0f);
				surfaceCAMs.put(cam, currentAmount + producedMass);
				useConstructionMass(producedMass);
				useEnergy(requiredEnergy);
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

		Food food = foodToDigest.getOrDefault(foodType, new Food(extractedMass, foodType));
		food.addSimpleMass(extractedMass);

		for (Food.ComplexMolecule molecule : cell.getComplexMolecules()) {
			if (cell.getComplexMoleculeAvailable(molecule) > 0) {
				float extractedAmount = extraction * cell.getComplexMoleculeAvailable(molecule);
				cell.depleteComplexMolecule(molecule, extractedAmount);
				food.addComplexMoleculeMass(molecule, extractedMass);
			}
		}
		foodToDigest.put(foodType, food);
	}

	public void addFood(Food.Type foodType, float amount) {
		Food food = foodToDigest.getOrDefault(foodType, new Food(amount, foodType));
		food.addSimpleMass(amount);
		foodToDigest.put(foodType, food);
	}

	public void digest(float delta) {
		for (Food food : foodToDigest.values()) {
			float rate = delta * 2f * getDigestionRate(food.getType());
			if (food.getSimpleMass() > 0) {
				float massExtracted = food.getSimpleMass() * rate;
				addConstructionMass(massExtracted);
				food.subtractSimpleMass(massExtracted);
				energyAvailable += food.getEnergy(massExtracted);
			}
			for (Food.ComplexMolecule molecule : food.getComplexMolecules()) {
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
			if (massRequired < constructionMassAvailable && energyRequired < energyAvailable) {
				useEnergy(energyRequired);
				useConstructionMass(massRequired);
				heal(delta * Settings.cellRepairRate * getRepairRate());
			}
		}
	}

	private float getRepairRate() {
		return repairRate;
	}

	public boolean detachCellCondition(Cell other) {
		if (other.isDead())
			return true;

		float dist2 = other.getPos().cpy().sub(getPos()).len2();
		float maxDist = (other.getRadius() + getRadius()) + 0.5f * JointsManager.idealJointLength(this, other);
		float minDist = 0.9f * (other.getRadius() + getRadius());
		return dist2 > maxDist*maxDist || dist2 < minDist*minDist;
	}

	public boolean attachCondition(Cell other) {
		if (other.isDead() || attachedCells.size() >= maxAttachedCells
				|| other.attachedCells.size() >= maxAttachedCells)
			return false;

		for (CellAdhesion.CAM cam : surfaceCAMs.keySet()) {
			if (getCAMAvailable(cam) > 0 && other.getCAMAvailable(cam) > 0)
				return true;
		}
		return false;
	}

	public void addConstructionProject(ConstructionProject project) {
		constructionProjects.add(project);
	}

	public void handleInteractions(float delta) {}

	public void grow(float delta) {
		if (constructionMassAvailable <= 0)
			return;

		float gr = getGrowthRate();
		float newR = getRadius() * (1 + 5f * gr * delta);

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
				useConstructionMass(massChange);
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

	public void createCellBinding(CollisionHandler.FixtureCollision collision, Cell other, CellAdhesion.CAM cam) {
		CellAdhesion.Binding binding = new CellAdhesion.Binding(this, other, cam);
		cellBindings.add(binding);
		if (!isBoundTo(other)) {
			attachedCells.add(other);
			JointsManager jointsManager = getEnv().getJointsManager();
			jointsManager.createJoint(collision, getBody(), other.getBody());
		}
	}

	public int getMaxAttachedCells() {
		return maxAttachedCells;
	}

	public void setMaxAttachedCells(int maxAttachedCells) {
		this.maxAttachedCells = maxAttachedCells;
	}

	public Collection<CellAdhesion.Binding> getCellBindings() {
		return cellBindings;
	}

	public Collection<CellAdhesion.CAM> getSurfaceCAMs() {
		return surfaceCAMs.keySet();
	}

	public boolean canMakeBindings() {
		return true;
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

		if (canMakeBindings() && other instanceof Cell) {
			Cell otherCell = (Cell) other;
			onCollision(contact, otherCell);
		}
	}

	public void onCollision(CollisionHandler.FixtureCollision contact, Cell other) {
		if (attachCondition(other)) {
			for (CellAdhesion.CAM cam : surfaceCAMs.keySet()) {
				if (getCAMAvailable(cam) > 0 && other.getCAMAvailable(cam) > 0) {
					createCellBinding(contact, other, cam);
//					other.createCellBinding(contact, this, cam);
				}
			}
		}
	}

	private boolean isBoundTo(Cell otherCell) {
		return attachedCells.contains(otherCell);
	}

	public void handleBindingInteraction(CellAdhesion.Binding binding, float delta) {
		CellAdhesion.CAMJunctionType junctionType = binding.getCAM().getJunctionType();
		if (junctionType.equals(CellAdhesion.CAMJunctionType.OCCLUDING))
			handleOcclusionBindingInteraction(binding, delta);
		else if (junctionType.equals(CellAdhesion.CAMJunctionType.CHANNEL_FORMING))
			handleChannelBindingInteraction(binding, delta);
		else if (junctionType.equals(CellAdhesion.CAMJunctionType.SIGNAL_RELAYING))
			handleSignallingBindingInteraction(binding, delta);
	}

	public void handleOcclusionBindingInteraction(CellAdhesion.Binding binding, float delta) {}

	public void handleChannelBindingInteraction(CellAdhesion.Binding binding, float delta) {
		Cell other = binding.getDestinationEntity();
		float transferRate = Settings.channelBindingEnergyTransport;

		float massDelta = getConstructionMassAvailable() - other.getConstructionMassAvailable();
		float constructionMassTransfer = Math.abs(transferRate * massDelta * delta);
		if (massDelta > 0) {
			other.addConstructionMass(constructionMassTransfer);
			useConstructionMass(constructionMassTransfer);
		} else {
			addConstructionMass(constructionMassTransfer);
			other.useConstructionMass(constructionMassTransfer);
		}

		float energyDelta = getEnergyAvailable() - other.getEnergyAvailable();
		float energyTransfer = Math.abs(transferRate * energyDelta * delta);
		if (energyDelta > 0) {
			other.addAvailableEnergy(energyTransfer);
			useEnergy(energyTransfer);
		} else {
			addAvailableEnergy(energyTransfer);
			other.useEnergy(energyTransfer);
		}

		for (Food.ComplexMolecule molecule : getComplexMolecules())
			handleComplexMoleculeTransport(other, molecule, delta);
		for (Food.ComplexMolecule molecule : other.getComplexMolecules())
			other.handleComplexMoleculeTransport(this, molecule, delta);
	}

	private void handleComplexMoleculeTransport(Cell other, Food.ComplexMolecule molecule, float delta) {
		float massDelta = getComplexMoleculeAvailable(molecule) - other.getComplexMoleculeAvailable(molecule);
		float transferRate = Settings.occludingBindingEnergyTransport;
		if (massDelta > 0) {
			float massTransfer = transferRate * massDelta * delta;
			other.addAvailableComplexMolecule(molecule, massTransfer);
			depleteComplexMolecule(molecule, massTransfer);
		}
	}

	public void handleSignallingBindingInteraction(CellAdhesion.Binding binding, float delta) {}

	public boolean isAttached(Cell e) {
		return cellBindings.stream().anyMatch(binding -> binding.getDestinationEntity().equals(e));
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

	public Map<String, Float> getStats() {
		Map<String, Float> stats = super.getStats();
		stats.put("Age", 100 * timeAlive);
		stats.put("Health", 100 * getHealth());
		stats.put("Generation", (float) getGeneration());
		float energyScalar = Settings.statsMassScalar * Settings.statsDistanceScalar * Settings.statsDistanceScalar;
		stats.put("Available Energy", energyScalar * energyAvailable);
		stats.put("Total Mass", Settings.statsMassScalar * getMass());
		stats.put("Construction Mass", Settings.statsMassScalar * constructionMassAvailable);
		if (wasteMass > 0)
			stats.put("Waste Mass", Settings.statsDistanceScalar * wasteMass);

		stats.put("Growth Rate", 10000 * Settings.statsDistanceScalar * getGrowthRate());
		stats.put("Repair Rate", Settings.statsDistanceScalar * getRepairRate());

		for (Food.ComplexMolecule molecule : availableComplexMolecules.keySet())
			if (availableComplexMolecules.get(molecule) > 0)
				stats.put(molecule + " Available", availableComplexMolecules.get(molecule));

		if (cellBindings.size() > 0)
			stats.put("Num Cell Bindings", (float) cellBindings.size());

		for (CellAdhesion.CAMJunctionType junctionType : CellAdhesion.CAMJunctionType.values()) {
			float camMass = 0;
			for (CellAdhesion.CAM molecule : surfaceCAMs.keySet())
				if (molecule.getJunctionType().equals(junctionType))
					camMass += surfaceCAMs.get(molecule);
			if (camMass > 0)
				stats.put(junctionType + " CAM Mass", camMass);
		}

		if (engulfer != null)
			stats.put("Being Engulfed", 1f);

		float massTimeScalar = Settings.statsMassScalar / Settings.statsTimeScalar;
		for (Food.ComplexMolecule molecule : complexMoleculeProductionRates.keySet())
			if (complexMoleculeProductionRates.get(molecule) > 0)
				stats.put(molecule + " Production", massTimeScalar * complexMoleculeProductionRates.get(molecule));

		for (Food.ComplexMolecule molecule : availableComplexMolecules.keySet())
			if (availableComplexMolecules.get(molecule) > 0)
				stats.put(molecule + " Available", 100f * Settings.statsMassScalar * availableComplexMolecules.get(molecule));

		for (Food.Type foodType : foodDigestionRates.keySet())
			if (foodDigestionRates.get(foodType) > 0)
				stats.put(foodType + " Digestion Rate", massTimeScalar * foodDigestionRates.get(foodType));

		for (Food food : foodToDigest.values())
			stats.put(food + " to Digest", Settings.statsMassScalar * food.getSimpleMass());

		return stats;
	}

	public Map<String, Float> getDebugStats() {
		Map<String, Float> stats = super.getDebugStats();
		stats.put("Num Attached Cells", (float) attachedCells.size());
		stats.put("Num Cell Bindings", (float) cellBindings.size());
		return stats;
	}
	
	public float getHealth() {
		return MathUtils.clamp(health, 0, 1);
	}

	public boolean isDead() {
		if (health <= 0.05f && !super.isDead())
			kill(CauseOfDeath.HEALTH_TOO_LOW);
		return super.isDead();
	}

	public void kill(CauseOfDeath causeOfDeath) {
		super.kill(causeOfDeath);
	}

	@Override
	public Color getColor() {
		Color healthyColour = getHealthyColour();
		Color degradedColour = getFullyDegradedColour();
		return lerp(healthyColour, degradedColour, 1 - getHealth());
	}

	public Color getHealthyColour() {
		return healthyColour;
	}

	public void setHealthyColour(Color healthyColour) {
		this.healthyColour.set(healthyColour);
	}

	public void setDegradedColour(Color fullyDegradedColour) {
		this.fullyDegradedColour.set(fullyDegradedColour);
	}

	public Color getFullyDegradedColour() {
		if (fullyDegradedColour == null) {
			Color healthyColour = getHealthyColour();
			float r = healthyColour.r;
			float g = healthyColour.g;
			float b = healthyColour.b;
			float mean = (r + g + b) / 3;
			float p = 0.4f;  // proportion of colour to keep
			return lerp(healthyColour, new Color(mean, mean, mean, 1), 1 - p);
		}
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

	public void useEnergy(float energy) {
		energyAvailable = Math.max(0, energyAvailable - energy);
	}

	public Collection<Food.ComplexMolecule> getComplexMolecules() {
		return availableComplexMolecules.keySet();
	}

	public void depleteComplexMolecule(Food.ComplexMolecule molecule, float amount) {
		float currAmount = getComplexMoleculeAvailable(molecule);
		setComplexMoleculeAvailable(molecule, currAmount - amount);
	}

	public float getComplexMoleculeAvailable(Food.ComplexMolecule molecule) {
		return availableComplexMolecules.getOrDefault(molecule, 0f);
	}

	private void addAvailableComplexMolecule(Food.ComplexMolecule molecule, float amount) {
		float currentAmount = availableComplexMolecules.getOrDefault(molecule, 0f);
		float newAmount = Math.min(getComplexMoleculeMassCap(), currentAmount + amount);
		availableComplexMolecules.put(molecule, newAmount);
	}

	private float getComplexMoleculeMassCap() {
		return getMass(getRadius() * 0.1f);
	}

	public void setComplexMoleculeAvailable(Food.ComplexMolecule molecule, float amount) {
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

	public void useConstructionMass(float mass) {
		constructionMassAvailable = Math.max(0, constructionMassAvailable - mass);
	}

	public void setComplexMoleculeProductionRate(Food.ComplexMolecule molecule, float rate) {
		complexMoleculeProductionRates.put(molecule, rate);
	}

	public void setCAMProductionRate(CellAdhesion.CAM cam, float rate) {
		camProductionRates.put(cam, rate);
	}

	@Override
	public float getMass() {
		float extraMass = constructionMassAvailable + wasteMass;
		for (float mass : complexMoleculeProductionRates.values())
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

	public Collection<Cell> getAttachedCells() {
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
}
