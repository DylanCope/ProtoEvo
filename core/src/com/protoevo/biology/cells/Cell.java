package com.protoevo.biology.cells;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.protoevo.biology.*;
import com.protoevo.biology.nodes.SurfaceNode;
import com.protoevo.biology.organelles.Organelle;
import com.protoevo.core.Statistics;
import com.protoevo.env.Environment;
import com.protoevo.env.Spawnable;
import com.protoevo.maths.Functions;
import com.protoevo.physics.Collision;
import com.protoevo.physics.Coloured;
import com.protoevo.physics.Joining;
import com.protoevo.physics.Particle;
import com.protoevo.utils.Colour;
import com.protoevo.maths.Geometry;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.protoevo.maths.Functions.lerp;

public abstract class Cell implements Serializable, Coloured, Spawnable {
	private static final long serialVersionUID = 1L;

	private Particle particle;
	private Environment environment;
	// Phylogeny: each cell carries a lineage tag inherited from its parent
	// (or assigned new at initial spawn for "founder" lineages). Combined
	// with parentId, we can build a full ancestry tree without snapshotting
	// every cell — most lineages die out within a few generations, only a
	// few "successful" founders accumulate descendants. Default 0 means
	// "uninitialised"; assigned on first creation through Environment.
	private long lineageId = 0L;
	private long parentId = 0L;
	private final Colour healthyColour = new Colour(Color.WHITE);
	private final Colour fullyDegradedColour = new Colour(Color.WHITE);
	private final Colour currentColour = new Colour();
	private int generation = 1;
	private float timeAlive = 0f;
	private float health = 1f;
	private DamageEvent lastDamageEvent = null;
	private float growthRate = 0.0f;
	private float energyAvailable = Environment.settings.cell.startingAvailableCellEnergy.get();
	private double constructionMassAvailable = Environment.settings.cell.startingAvailableConstructionMass.get();
	private double massChangeForGrowth = 0f;
	private double radius = Environment.settings.minParticleRadius.get() * (1 + 2 * Math.random());
	private final Map<ComplexMolecule, Float> availableComplexMolecules = new ConcurrentHashMap<>(0);
	private final Collection<Long> attachedCellIDs = new ConcurrentLinkedQueue<>(); // cells attached to this cell
	private final Set<Long> cellIdsInMultiCellGroup = new HashSet<>(0); // cells in the same multi-cell group
	private final Map<Food.Type, Float> foodDigestionRates = new HashMap<>(0);
	private final Map<Food.Type, Food> foodToDigest = new HashMap<>(0);
	private final Set<ConstructionProject> constructionProjects = new LinkedHashSet<>(0);
	private ArrayList<Organelle> organelles = new ArrayList<>();
	private boolean hasBurst = false;
	private float repairRate = 1f;
	private float activity = 0f, lastActivity = 0f;
	private float idealTemperature = Environment.settings.env.maxLightEnvTemp.get();
	private float temperature = idealTemperature;
	private float temperatureTolerance = Environment.settings.cell.minTemperatureTolerance.get();
	private float membraneThermalConductance = 1f;
	private float temperatureSatisfaction = 0f;

	private Cell engulfer = null;
	private boolean fullyEngulfed = false;
	private float joiningCheckCounter = 0f;
	// Signature-match efficiency at engulf time, in (0, 1]. Receptor sets
	// this on its prey when it engulfs; eat() multiplies extraction by it
	// so a poorly-matched receptor digests prey slower than a perfect one.
	// Default 1.0 means "no signature gate" (e.g. meat with no genome).
	private float engulfEfficiency = 1f;

	public float getEngulfEfficiency() { return engulfEfficiency; }
	public void setEngulfEfficiency(float e) {
		this.engulfEfficiency = Math.max(0.01f, Math.min(1f, e));
	}

	public void update(float delta) {
		if (particle.isDead()) {
			kill(particle.getCauseOfDeath());
			return;
		}
		if (health <= 0.05f && !particle.isDead()) {
			kill(CauseOfDeath.HEALTH_TOO_LOW);
			return;
		}

		handleTemperature(delta);
		lastActivity = activity;
		activity = 0f;

		particle.setRadius(radius);
		particle.update(delta);
		timeAlive += delta;

		voidDamage(delta);
		digest(delta);
		grow(delta);
		repair(delta);

		for (Organelle organelle : organelles)
			organelle.update(delta);


		if (joiningCheckCounter >= Environment.settings.misc.checkCellJoiningsInterval.get()) {
			particle.getJoiningIds().entrySet().removeIf(this::detachCellCondition);
			attachedCellIDs.clear();
			attachedCellIDs.addAll(particle.getJoiningIds().keySet());
			joiningCheckCounter = 0;
		}
		joiningCheckCounter += delta;

		decayResources(delta);
	}

	public void handleTemperature(float delta) {
		float envTemp = getExternalTemperature();
		temperature = Functions.lerp(
				temperature, envTemp, membraneThermalConductance * delta);
		for (Long otherId : getAttachedCellIDs()) {
			Optional<Cell> other = getCell(otherId);
			other.ifPresent(cell -> {
				float otherTemp = cell.getInternalTemperature();
				temperature = Functions.lerp(
						temperature, otherTemp, membraneThermalConductance * delta);
			});
		}

		temperature += delta * activity * Environment.settings.cell.activityHeatGeneration.get();

		updateTemperatureSatisfaction(delta);
		handleTemperatureDamage(delta);
	}

	private void updateTemperatureSatisfaction(float delta) {
		float tolerance = temperatureTolerance;

		float energyRequired = delta * tolerance * Environment.settings.cell.temperatureToleranceEnergyCost.get();
		if (getEnergyAvailable() < energyRequired) {
			tolerance = Math.max(
					Environment.settings.cell.minTemperatureTolerance.get(),
					tolerance * getEnergyAvailable() / energyRequired);
		}
		depleteEnergy(energyRequired);

		if (temperature < idealTemperature - tolerance) {
			temperatureSatisfaction = Functions.clampedLinearRemap(
					temperature,
					idealTemperature - tolerance,
					idealTemperature - tolerance*1.5f,
					1, 0
			);
		} else if (temperature > idealTemperature + tolerance) {
			temperatureSatisfaction = Functions.clampedLinearRemap(
					temperature,
					idealTemperature + tolerance,
					idealTemperature + tolerance*1.5f,
					1, 0
			);
		} else {
			temperatureSatisfaction = 1f;
		}
	}

	private void handleTemperatureDamage(float delta) {
		if (temperatureSatisfaction < 1f) {
			float damage = delta * (1 - temperatureSatisfaction) * Environment.settings.cell.temperatureDeathRate.get();
			if (temperature < idealTemperature)
				damage(damage, CauseOfDeath.HYPOTHERMIA);
			else
				damage(damage, CauseOfDeath.HYPERTHERMIA);
		}
	}

	public float getInteractionRange() {
		return 0f;
	}

	public Collection<Object> getInteractionQueue() {
		return particle.getInteractionQueue();
	}

	public Collection<Collision> getContacts() {
		return particle.getContacts();
	}

	public float getExternalTemperature() {
		if (environment == null)
			return 0f;
		return environment.getTemperature(particle.getPos());
	}

	public float getInternalTemperature() {
		return temperature;
	}

	public void setIdealTemperature(float t) {
		idealTemperature = t;
	}

	public float getIdealTemperature() {
		return idealTemperature;
	}

	public float getTemperatureTolerance() {
		return temperatureTolerance;
	}

	public void setTemperatureTolerance(float t) {
		temperatureTolerance = t;
	}

	public void setThermalConductance(float t) {
		membraneThermalConductance = t;
	}

	public void decayResources(float delta) {
		foodToDigest.values().forEach(food -> food.decay(delta));

		// Energy decay = baseRate × (radius/minR) × energyAvailable.
		// Three properties:
		//   1. Proportional to stored energy → no "park at the ceiling and live forever"
		//      exploit. A full cell burns through stored energy at the rate's natural
		//      half-life (~55s at default settings); decay slows automatically as the
		//      cell drains, so you don't starve cells faster than they can recover.
		//   2. Proportional to cell size → basal metabolism. Bigger cells (which can
		//      reproduce) pay more to maintain. Creates a real fitness trade-off
		//      between reproductive size and survival-while-fasting.
		//   3. Previous flat-rate formula meant the default 0.025 J/sec was a literal
		//      0.025 J per second regardless of stored energy or size, so a 1200-J cell
		//      took 48,000 sim-seconds to drain. Sitting still was essentially free,
		//      which is exactly the exploit evolution discovered ("low Cilia Thrust,
		//      live forever").
		float baseRate = Environment.settings.cell.energyDecayRate.get();
		float radiusFactor = (float) (radius / Environment.settings.minParticleRadius.get());
		depleteEnergy(delta * baseRate * radiusFactor * energyAvailable);

		for (ComplexMolecule molecule : availableComplexMolecules.keySet()) {
			depleteComplexMolecule(molecule, delta * Environment.settings.cell.complexMoleculeDecayRate.get());
		}

		applyStarvationDamage(delta);
	}

	/**
	 * Damage health when energy is below the starvation threshold. Without
	 * this, the energyDecayRate lever (the homeostat's main starvation knob)
	 * has no death pathway — cells just stop growing at 0 energy and live
	 * forever, so the population could only drop via spike damage or void
	 * deaths. Adding this gives the homeostat real authority and makes
	 * "food is scarce" a survivable challenge rather than a free pass.
	 */
	private void applyStarvationDamage(float delta) {
		float cap = getAvailableEnergyCap();
		if (cap <= 0f)
			return;
		float threshold = Environment.settings.cell.starvationThresholdFraction.get() * cap;
		if (energyAvailable >= threshold)
			return;
		float deficit = 1f - (energyAvailable / threshold);   // 0..1
		float rate = Environment.settings.cell.starvationDeathRate.get();
		damage(delta * deficit * rate, CauseOfDeath.STARVATION);
	}

	public void voidDamage(float delta) {
		if (particle.getPos().len2() > getVoidStartDistance2())
			damage(delta * Environment.settings.env.voidDamagePerSecond.get(), CauseOfDeath.THE_VOID);
	}

	protected float getVoidStartDistance2() {
		return Environment.settings.worldgen.voidStartDistance.get()
				* Environment.settings.worldgen.voidStartDistance.get();
	}

	public void requestJointRemoval(Long joiningId) {
		particle.getJoining(joiningId).ifPresent(this::requestJointRemoval);
	}

	public void requestJointRemoval(Joining joining) {
		particle.requestJointRemoval(joining);
		particle.getJoiningIds().remove(joining.getOtherId(getId()));
		Optional<Cell> otherCell = joining.getOther(particle).map(particle -> particle.getUserData(Cell.class));
		otherCell.ifPresent(cell -> cell.particle.getJoiningIds().remove(particle.getId()));
	}

	public Optional<Environment> getEnv() {
		return Optional.ofNullable(environment);
	}

	public float getLightAt(Vector2 pos) {
		return getEnv().map(env -> env.getLight(pos)).orElse(0f);
	}

	public float getLightAtCell() {
		return getLightAt(getPos());
	}

	public boolean isRangedInteractionEnabled() {
		return false;
	}

	public void setEnvironmentAndBuildPhysics(Environment env) {
		setEnvironment(env);
		particle = env.getPhysics().createNewParticle();
		particle.setUserData(this);
		if (isRangedInteractionEnabled())
			particle.setCanInteractAtRange();
		environment.ensureAddedToEnvironment(this);
	}

	public void setEnvironment(Environment env) {
		this.environment = env;
	}

	public Optional<Cell> getCell(Long id) {
		return getEnv().flatMap(env -> env.getCell(id));
	}

	public long getId() {
		return particle.getId();
	}

	public Particle getParticle() {
		return particle;
	}

	public Collection<Long> getAttachedCellIDs() {
		return attachedCellIDs;
	}

	public boolean isAttachedTo(Cell other) {
		return attachedCellIDs.contains(other.getId());
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
				(float) constructionMassAvailable,
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

	public float getDigestionRate(Food.Type foodType) {
		return foodDigestionRates.getOrDefault(foodType, 0f);
	}

	public void setDigestionRate(Food.Type foodType, float rate) {
		foodDigestionRates.put(foodType, rate);
	}

	public float getTotalFoodMassToDigest() {
		float total = 0;
		for (Food food : foodToDigest.values()) {
			total += food.getSimpleMass();
		}
		return total;
	}

	public float getFoodToDigestMassCap() {
		return particle.getMass() * 0.25f;
	}

	public void eat(Cell engulfed, float extraction) {

		if (getTotalFoodMassToDigest() >= getFoodToDigestMassCap())
			return;

		Food.Type foodType = engulfed instanceof PlantCell ? Food.Type.Plant : Food.Type.Meat;
		float extractedMass = engulfed.getMass() * extraction;
		float removeMultiplier = Environment.settings.cell.engulfExtractionWasteMultiplier.get();
		engulfed.removeMass(removeMultiplier * extractedMass, CauseOfDeath.EATEN);

		Food food;
		if (foodToDigest.containsKey(foodType)) {
			food = foodToDigest.get(foodType);
			food.addSimpleMass(extractedMass);
		} else {
			// Constructor already sets mass=extractedMass; previously this branch
			// then ALSO called addSimpleMass(extractedMass) below, doubling the
			// mass of every fresh food chunk.
			food = new Food(extractedMass, foodType);
			foodToDigest.put(foodType, food);
		}

		// Energy must be transferred, not duplicated: deplete the victim's
		// stockpile by the same amount we hand to the food chunk. Without this,
		// `cell A energy -> meat -> cell B energy` was a free copy.
		float energyTransferred = engulfed.getEnergyAvailable() * extraction;
		engulfed.depleteEnergy(energyTransferred);
		food.addEnergy(energyTransferred);

		for (ComplexMolecule molecule : engulfed.getComplexMolecules()) {
			if (engulfed.getComplexMoleculeAvailable(molecule) > 0) {
				float extractedAmount = extraction * engulfed.getComplexMoleculeAvailable(molecule);
				engulfed.depleteComplexMolecule(molecule, extractedAmount);
				if (extractedAmount <= 1e-12)
					continue;
				// Was: food.addComplexMoleculeMass(molecule, extractedMass)
				// — credited the food chunk with the prey's ENTIRE body mass
				// once per molecule. That created arbitrarily many "molecules"
				// each weighted by total prey mass, breaking mass conservation
				// and inflating digest yields by N×bodyMass per molecule type.
				food.addComplexMoleculeMass(molecule, extractedAmount);
			}
		}
	}

	public void addFood(Food.Type foodType, float amount) {
		// Same double-mass bug as the old Cell.eat had: fresh Food gets
		// mass=amount via the constructor, then addSimpleMass(amount) ran
		// unconditionally — every chemical-drip absorption was actually 2×
		// the intended yield. Combined with chemical drip being an unsourced
		// mass spring (plants don't lose mass when their chemicals are
		// extracted), this let large protozoa populations sustain on tiny
		// plant counts.
		Food food = foodToDigest.get(foodType);
		if (food == null) {
			food = new Food(amount, foodType);
			foodToDigest.put(foodType, food);
		} else {
			food.addSimpleMass(amount);
		}
	}

	public void digest(float delta) {
		if (foodToDigest.isEmpty())
			return;  // avoids creating iterator if there's nothing to digest

		for (Food food : foodToDigest.values()) {
			float rate = delta * Environment.settings.cell.digestionFactor.get() * getDigestionRate(food.getType());
			activity += rate * Environment.settings.cell.digestionActivity.get();
			if (food.getSimpleMass() > 0) {
				float massExtracted = food.getSimpleMass() * rate;
				addConstructionMass(massExtracted);
				food.subtractSimpleMass(massExtracted);
				addAvailableEnergy(food.getEnergy(massExtracted));
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
			float repair = delta * getRepairRate();
			float massRequired = getBaseMass() * Environment.settings.cell.repairMassFactor.get() * repair;
			float energyRequired = massRequired * Environment.settings.cell.repairEnergyFactor.get();
			if (massRequired < constructionMassAvailable && energyRequired < energyAvailable) {
				depleteEnergy(energyRequired);
				depleteConstructionMass(massRequired);
				heal(repair);
				activity += Environment.settings.cell.repairActivity.get() * repair;
			}
		}
	}
	private float getRepairRate() {
		return Environment.settings.cell.repairRate.get() * repairRate;
	}

	private void requestJointRemoval(long joiningID) {
		getEnv().map(Environment::getJointsManager)
				.ifPresent(manager -> manager.requestJointRemoval(joiningID));
	}

	public boolean detachCellCondition(Map.Entry<Long, Long> joiningEntry) {
		Optional<Cell> other = getCell(joiningEntry.getKey());
		long joiningID = joiningEntry.getValue();
		Optional<Joining> maybeJoining = particle.getJoining(joiningEntry.getValue());

		if (!maybeJoining.isPresent() || !other.isPresent()) {
			requestJointRemoval(joiningID);
			return true;
		}

		if (other.get().isDead()) {
			requestJointRemoval(joiningID);
			return true;
		}

		if (maybeJoining.get().maxLengthExceeded()) {
			requestJointRemoval(joiningID);
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

		double gr = getGrowthRate();
		double dr = Environment.settings.cell.growthFactor.get() * gr * ((double) delta);
		double currR = getRadiusDouble();
		double newR = currR + dr;

		if (newR > getMaxRadius())
			newR = getMaxRadius();
		if (newR < getMinRadius())
			newR = getMinRadius();
		if (newR == currR)
			return;

		massChangeForGrowth = particle.getMassIfRadius(newR) - particle.getMassIfRadius(currR);
		float energyForGrowth = (float) (massChangeForGrowth
				* Environment.settings.cell.energyRequiredForGrowth.get());

		if (massChangeForGrowth > constructionMassAvailable) {
			double dr2 = constructionMassAvailable / (Math.PI * particle.getMassDensity());
			newR = Math.sqrt(currR * currR + dr2);
			massChangeForGrowth = constructionMassAvailable;
		}

		if (massChangeForGrowth <= constructionMassAvailable
				&& energyForGrowth <= energyAvailable) {
			setRadius(newR);
			if (massChangeForGrowth > 0) {
				depleteConstructionMass(massChangeForGrowth);
				depleteEnergy(energyForGrowth);
			}
			activity += Environment.settings.cell.growthActivity.get() * newR / currR;
		}

		if (newR < getMinRadius())
			kill(CauseOfDeath.SHRUNK_TOO_MUCH);
	}

	public float getMaxRadius() {
		return Environment.settings.maxParticleRadius.get();
	}

	public float getMinRadius() {
		return Environment.settings.minParticleRadius.get();
	}

	public void setGrowthRate(float gr) {
		growthRate = gr;
	}

	public float getGrowthRate() {
		return growthRate;
	}

	public void registerJoining(Joining joining) {
		Optional<Particle> other = joining.getOther(particle);
		other.ifPresent(otherParticle -> particle.getJoiningIds().put(otherParticle.getId(), joining.id));
	}

	public void deregisterJoining(Joining joining) {
		Optional<Particle> other = joining.getOther(particle);
		other.ifPresent(otherParticle -> particle.getJoiningIds().remove(otherParticle.getId()));
	}

	public boolean notBoundTo(Cell otherCell) {
		return !(
			attachedCellIDs.contains(otherCell.getId())
			|| otherCell.attachedCellIDs.contains(getId())
		);
	}

	public abstract boolean isEdible();

	public void heal(float h) {
		damage(-h, CauseOfDeath.HEALED_TO_DEATH);
	}

	public void damage(float d, CauseOfDeath cause) {
		if (d > 0) {
			if (lastDamageEvent != null) {
				lastDamageEvent.setDamageAmount(d);
				lastDamageEvent.setDamageTime(timeAlive);
				lastDamageEvent.setCauseOfDamage(cause);
			}
			else {
				lastDamageEvent = new DamageEvent(timeAlive, d, cause);
			}
		}

		health -= d;
		if (health > 1)
			health = 1;

		if (health < 0.05)
			kill(cause);
	}

	public Optional<DamageEvent> getLastDamageEvent() {
		return Optional.ofNullable(lastDamageEvent);
	}

	public String getPrettyName() {
		return "Cell";
	}

	public float getTimeAlive() {
		return timeAlive;
	}

	public float getSpeed() {
		if (particle == null)
			return 0;
		return particle.getSpeed();
	}

	public Vector2 getPos() {
		if (particle == null)
			return Vector2.Zero;
		return particle.getPos();
	}

	public Vector2 getVel() {
		if (particle == null)
			return Vector2.Zero;
		return particle.getVel();
	}

	public void setRadius(double radius) {
		this.radius = radius;
	}

	public float getRadius() {
		return (float) radius;
	}

	public double getRadiusDouble() {
		return radius;
	}

	public float getKineticEnergyRequiredForThrust(Vector2 thrustVector) {
		float mass = getMass();
		if (mass <= 0f) return 0f;
		// Minimum kinetic energy required to deliver this impulse: I²/(2m).
		// Equivalent to the KE a stationary cell would gain from the impulse;
		// real cost is at least this much (more if v·I > 0).
		//
		// The OLD formula was 0.5·m·(v² − I²/m²) and was tagged with the
		// comment "This is surely not correct". For slow cells (v small,
		// |I/m| moderate) the second term dominated and the function
		// returned a NEGATIVE value. generateMovement then did
		// depleteEnergy(work) which became energy *gain* — every flagellar
		// stroke FILLED the energy pool. That's the long-standing "cells
		// generate infinite energy by sitting still and flailing" exploit.
		// Switching to I²/(2m) makes movement always cost energy, which is
		// what every other game-balance assumption in the sim was built on.
		return 0.5f * thrustVector.len2() / mass;
	}

	public void generateMovement(Vector2 thrustVector) {
		generateMovement(thrustVector, 0);
	}

	public float generateMovement(Vector2 thrustVector, float torque) {
		float work = getKineticEnergyRequiredForThrust(thrustVector);
		// TODO: add work to apply torque

		if (enoughEnergyAvailable(work)) {
			activity += work * Environment.settings.cell.kineticEnergyActivity.get();
			depleteEnergy(work);
			particle.applyImpulse(thrustVector);
			particle.applyTorque(torque);
			return 1f;
		} else {
			// not accurate scaling of thrust and torque
			float p = getEnergyAvailable() / work;
			thrustVector.scl(p);
			torque = torque * p;
			setEnergyAvailable(0);
			particle.applyImpulse(thrustVector);
			particle.applyTorque(torque);
			return p;
		}
	}

	public Statistics getResourceStats() {
		Statistics stats = particle.getStats();
		stats.clear();

		stats.putEnergy("Available Energy", energyAvailable);
		stats.putEnergy("Energy Limit", getAvailableEnergyCap());
		stats.putMass("Construction Mass", (float) constructionMassAvailable);
		stats.putMass("Construction Mass Limit", getConstructionMassCap());

		if (lastDamageEvent != null) {
			stats.putPercentage("Last Damage", lastDamageEvent.getDamageAmount());
			stats.putTime("Last Damage Time", lastDamageEvent.getDamageTime());
			stats.put("Last Damage Cause", lastDamageEvent.getCauseOfDamage().getReasonSentence());
		}
		for (ComplexMolecule molecule : availableComplexMolecules.keySet())
			if (availableComplexMolecules.get(molecule) >= 1e-12)
				stats.putMass(
						String.format("Molecule %.2f Available", molecule.getSignature()),
						availableComplexMolecules.get(molecule));

		Statistics.ComplexUnit massPerTime = Statistics.ComplexUnit.MASS_PER_TIME;

		for (Food.Type foodType : foodDigestionRates.keySet())
			if (foodDigestionRates.get(foodType) > 0)
				stats.put(foodType + " Digestion Rate", foodDigestionRates.get(foodType), massPerTime);

		for (Food food : foodToDigest.values())
			stats.putMass(food + " to Digest", food.getSimpleMass());

		return stats;
	}

	public Statistics getStats() {
		Statistics stats = particle.getStats();
		stats.put("Activity", lastActivity);
		stats.putTime("Age", timeAlive);
		stats.putPercentage("Health", 100 * getHealth());
		stats.putCount("Generation", getGeneration());
		stats.putEnergy("Available Energy", energyAvailable);
		stats.putMass("Construction Mass", (float) constructionMassAvailable);

		stats.putSpeed("Growth Rate", getGrowthRate());
		stats.put("Repair Rate", 100 * getRepairRate(),
				Statistics.ComplexUnit.PERCENTAGE_PER_TIME);

		if (getNumAttachedCells() > 0) {
			stats.putCount("Num Cell Bindings", getNumAttachedCells());
			stats.putCount("Multicell Structure Size", getNumCellsInMulticellularOrganism());
		}

		stats.putBoolean("Being Engulfed", engulfer != null);

		if (lineageId != 0L)
			stats.putCount("Lineage ID", (int) lineageId);

		stats.putPercentage("Light Level", 100f * getLightAtCell());
		stats.putTemperature("Temperature (Internal)", temperature);
		stats.putTemperature("Temperature (External)", getExternalTemperature());
		stats.put("Thermal Conductance", membraneThermalConductance);
		stats.putTemperature("Temperature Tolerance", temperatureTolerance);
		stats.putTemperature("Ideal Temperature", idealTemperature);

		return stats;
	}

	public int getNumCellsInMulticellularOrganism() {
		cellIdsInMultiCellGroup.clear();
		cellIdsInMultiCellGroup.add(getId());
		return getNumCellsInMulticellularOrganism(cellIdsInMultiCellGroup);
	}

	public int getNumCellsInMulticellularOrganism(Set<Long> visited) {
		int numCells = 1;
		for (Long cellId : particle.getJoiningIds().keySet()) {
			if (!visited.contains(cellId)) {
				visited.add(cellId);
				numCells += getCell(cellId)
						.map(cell -> cell.getNumCellsInMulticellularOrganism(visited))
						.orElse(0);
			}
		}
		return numCells;
	}

	public MultiCellStructure getMulticellularStructure() {
		getNumCellsInMulticellularOrganism();
		return new MultiCellStructure(
				cellIdsInMultiCellGroup,
				getEnv().orElseThrow(() -> new RuntimeException("Cell has no environment")));
	}

	public Statistics getDebugStats() {
		Statistics stats = particle.getDebugStats();
		stats.putCount("Local Count", environment.getLocalCount(this));
		stats.putCount("Local Cap", environment.getLocalCapacity(this));
		stats.putCount("Num Attached Cells", getNumAttachedCells());
		stats.putMass("Mass To Grow", (float) massChangeForGrowth);
		return stats;
	}

	public float getHealth() {
		return MathUtils.clamp(health, 0, 1);
	}

	public boolean isDead() {
		return particle.isDead();
	}

	public void kill(CauseOfDeath causeOfDeath) {
		// Bump the per-cause death counter so we can see in the homeostat
		// log what's actually killing cells (hyperthermia? starvation?
		// being eaten?). Only ticks once per cell, even if kill() is
		// called multiple times — particle.isDead() guards re-entry.
		if (environment != null && !particle.isDead())
			environment.recordDeath(this, causeOfDeath);
		for (Long joiningId : particle.getJoiningIds().values()) {
			requestJointRemoval(joiningId);
		}
		particle.getJoiningIds().clear();
		attachedCellIDs.clear();
		particle.kill(causeOfDeath);
	}

	public Colour getColour() {
		Colour healthyColour = getHealthyColour();
		Colour degradedColour = getFullyDegradedColour();
		return currentColour.set(healthyColour).lerp(degradedColour, 1 - getHealth());
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

	public Colour degradeColour(Colour colour, float t) {
		return lerp(colour, Colour.GRAY, t);
	}

	public int getGeneration() {
		return generation;
	}

	public void setGeneration(int generation) {
		this.generation = generation;
	}

	public long getLineageId() { return lineageId; }
	public void setLineageId(long id) { this.lineageId = id; }
	public long getParentId() { return parentId; }
	public void setParentId(long id) { this.parentId = id; }

	public int burstMultiplier() {
		return 1;
	}

	public float getMinBurstRadius() {
		return 2 * Environment.settings.minParticleRadius.get();
	}

	public void setFoodToDigest(Food.Type foodType, Food food) {
		foodToDigest.put(foodType, food);
	}

//	public float getCAMAvailable(CellAdhesion.CAM cam) {
//		return surfaceCAMs.getOrDefault(cam, 0f);
//	}
//
//	public void setCAMAvailable(CellAdhesion.CAM cam, float amount) {
//		surfaceCAMs.put(cam, amount);
//	}

	public boolean enoughEnergyAvailable(float work) {
		return work < energyAvailable;
	}

	public float getEnergyAvailable() {
		return energyAvailable;
	}

	public void addAvailableEnergy(float energy) {
		energyAvailable = Math.min(energyAvailable + energy, getAvailableEnergyCap());
	}

	private float getAvailableEnergyCap() {
		return Environment.settings.cell.energyCapFactor.get() * getRadius()
				/ Environment.settings.minParticleRadius.get();
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
		if (particle == null)
			return 1f;

		return (float) particle.getMassIfRadius(getRadius() * 0.1f);
	}

	public void setComplexMoleculeAvailable(ComplexMolecule molecule, float amount) {
		availableComplexMolecules.put(molecule, Math.max(0, amount));
	}

	public float getConstructionMassCap() {
		if (particle == null)
			return 1f;

		// Old formula was 2 × density × area(r × 0.25) — only ~12.5% of the
		// cell's own physical mass (~1 mg at min radius, ~9 mg at max). That
		// was too tight: a single digested plant delivers more than the cap
		// in a fraction of a sim-second, so most digested mass got clamped
		// away. Worse, MoleculeProductionOrganelle + SurfaceNode attachment
		// construction drain mass every tick, so anything that DID land in
		// the pool got siphoned before growth could spend it. End result:
		// users saw Construction Mass = 0 in stats and cells that never grew
		// despite eating constantly.
		//
		// New formula: cap = 2 × cell's own physical mass. Big enough that a
		// digestion burst can actually buffer in the pool, growth can spend
		// against the buffer over multiple ticks, and side-consumers
		// (organelles, surface-node attachments) don't strip-mine the pool
		// to 0 before grow() runs the next tick.
		return 2f * (float) particle.getMassIfRadius(getRadius());
	}

	public void setAvailableConstructionMass(float mass) {
		constructionMassAvailable = Math.min(mass, getConstructionMassCap());
	}

	public void setAvailableConstructionMass(double mass) {
		constructionMassAvailable = Math.min(mass, getConstructionMassCap());
	}

	public float getConstructionMassAvailable() {
		return (float) constructionMassAvailable;
	}

	public double getConstructionMassAvailableDouble() {
		return constructionMassAvailable;
	}

	public void addConstructionMass(float mass) {
		setAvailableConstructionMass(constructionMassAvailable + mass);
	}

	public void addConstructionMass(double mass) {
		setAvailableConstructionMass(constructionMassAvailable + mass);
	}

	public void depleteConstructionMass(float mass) {
		constructionMassAvailable = Math.max(0, constructionMassAvailable - mass);
	}

	public void depleteConstructionMass(double mass) {
		constructionMassAvailable = Math.max(0, constructionMassAvailable - mass);
	}

	public float getMass() {
		if (particle == null)
			return 1f;

		float extraMass = (float) constructionMassAvailable;
		for (float mass : availableComplexMolecules.values())
			extraMass += mass;
		for (Food food : foodToDigest.values())
			extraMass += food.getMass();
		return particle.getMass() + extraMass;
	}

	public float getBaseMass() {
		if (particle == null)
			return 1f;
		return particle.getMass();
	}

	/**
	 * Changes the radius of the cell to remove the given amount of mass
	 *
	 * @param mass mass to remove
	 */
	public void removeMass(float mass, CauseOfDeath causeOfDeath) {
		float totalMass = getMass();
		if (totalMass <= 0f) {
			// Defensive: avoid NaN propagation into health. A fully-depleted
			// shell of a cell would otherwise compute NaN for percentRemoved,
			// poison the health field, and become un-killable (NaN compares
			// false against everything). Just kill it directly.
			kill(causeOfDeath);
			return;
		}
		float percentRemoved = mass / totalMass;
		damage(percentRemoved, causeOfDeath);

		// Area-conserving shrink. Old formula: newR = (1 - p) × r — that
		// treats r as if it were proportional to mass, but for a disc
		// mass ∝ r², so removing fraction p of mass should give
		// new_area = (1-p)·area → newR = r·√(1-p), not r·(1-p).
		// Old code shrunk the prey ~2× too fast in radius, which made plants
		// die from a few bites before the predator could finish digesting —
		// each tick of eat() also removeMass'd the plant, the plant hit
		// minRadius and died, and the predator lost the rest of the meal.
		double newR = getRadius() * Math.sqrt(Math.max(0f, 1f - percentRemoved));
		if (newR < Environment.settings.minParticleRadius.get() * 0.5f)
			kill(causeOfDeath);

		setRadius(newR);
	}

	public void removeMass(float mass) {
		removeMass(mass, CauseOfDeath.LOST_TOO_MUCH_MASS);
	}

	public Map<Food.Type, Food> getFoodToDigest() {
		return foodToDigest;
	}

	public int getNumAttachedCells() {
		return attachedCellIDs.size();
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

	public List<SurfaceNode> getSurfaceNodes() {
		return null;
	}

	public float getActivity() {
		return lastActivity;
	}

	public void setActivity(float activity) {
		this.activity = activity;
	}

	public void addActivity(float a) {
		activity += a;
	}

	public CauseOfDeath getCauseOfDeath() {
		return particle.getCauseOfDeath();
	}

	@Override
	public void spawn(Environment environment, float x, float y) {
		setEnvironmentAndBuildPhysics(environment);
		Particle particle = getParticle();
		particle.setPos(new Vector2(x, y));
		Vector2 impulse = Geometry
				.fromAngle((float) (Math.random() * Math.PI * 2))
				.scl(.01f);
		particle.applyImpulse(impulse);
	}
}
