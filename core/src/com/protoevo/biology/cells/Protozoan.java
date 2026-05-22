package com.protoevo.biology.cells;

import com.badlogic.gdx.math.Vector2;
import com.protoevo.biology.*;
import com.protoevo.biology.evolution.*;
import com.protoevo.biology.nn.NeuralNetwork;
import com.protoevo.biology.nodes.*;
import com.protoevo.biology.organelles.Organelle;
import com.protoevo.core.Simulation;
import com.protoevo.core.Statistics;
import com.protoevo.env.ChemicalSolution;
import com.protoevo.env.Environment;
import com.protoevo.maths.Functions;
import com.protoevo.physics.Collision;
import com.protoevo.utils.Colour;
import com.protoevo.utils.SerializableFunction;


import java.io.Serializable;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Protozoan extends EvolvableCell
{
	private static final long serialVersionUID = 2314292760446370751L;

	private GeneExpressionFunction crossOverGenome;
	private float matingCooldown = 0;
	// Continuous NN signals in [0,1]. Earlier these were thresholded to a
	// boolean at 0.5, which collapsed selection: a cell with signal 0.51
	// behaved identically to one at 1.0, and 0.49 identically to 0.0. That
	// made it impossible for evolution to find smooth strategies like
	// "split cautiously when food is plentiful" or "mate rarely". With
	// continuous signals + per-frame probability, mating/splitting frequency
	// scales linearly with the output, so the gradient is preserved.
	// Transient: the GRN re-populates these every expression tick, so old
	// saves can omit them without losing meaningful state.
	private transient float mateDesireSignal = 0f;
	private transient float splitDesireSignal = 0f;
	// Multi-layered memory with NN-controlled write gates and read-only
	// product slots. Three temporal layers (Fast/Med/Slow), each with
	// 2 slots and per-slot blend rate α. On write, the slot updates:
	//
	//   gate    = clamp(setMemoryGate_*, 0, 1)   // NN-controlled
	//   eff_α   = α · gate                       // gate scales blend rate
	//   slot    = (1−eff_α)·slot + eff_α·input
	//
	// gate=0 means "don't write this tick" — turns the slot into a
	// proper latch that remembers specific events instead of always
	// smoothing recent inputs. That's the LSTM input-gate pattern.
	//
	// In addition to the 6 directly-writable slots, the NN reads two
	// PRODUCT slots that are multiplicative compositions of pairs of
	// regular slots. Additive NNs can't synthesize "A AND B"-type
	// features without specifically-tuned activations; exposing the
	// product gives evolution access to conjunctive sensing for free.
	//
	// All transient; the GRN refills them every tick.
	private static final int MEMORY_SLOTS = 6;
	private static final float[] MEMORY_ALPHA = {
			0.7f, 0.7f,   // Fast 0, 1
			0.3f, 0.3f,   // Medium 0, 1
			0.05f, 0.05f  // Slow 0, 1
	};
	private transient float[] memory;
	private transient float[] memoryGate; // NN-controlled write gate per slot
	private List<SurfaceNode> surfaceNodes;

	private float damageRate = 0;
	private float herbivoreFactor, splitRadius;
	private final Vector2 tmp = new Vector2();
	private final Collection<Cell> engulfedCells = new ArrayList<>(0);
	private final Vector2 thrust = new Vector2(), dir = new Vector2();
	private float thrustAngle = (float) (2 * Math.PI * Math.random());
	private float thrustTurn = 0, thrustMag;
	private final SpawnChildFn createChild = new SpawnChildFn(this);
	private final SpawnMeatCell createMeatOnDeath = new SpawnMeatCell(this);

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
			// Multiply by the receptor's signature-match efficiency that
			// was stamped onto the prey at engulf time. Perfect match
			// digests at full rate; a barely-passing match digests at
			// ~5% (floored). This is what makes a well-matched receptor
			// actually pay off in the arms race.
			float rate = Environment.settings.protozoa.engulfEatingRateMultiplier.get()
					* delta * engulfedCell.getEngulfEfficiency();
			eat(engulfedCell, rate);
		}
		engulfedCells.removeIf(this::removeEngulfedCondition);

		if (shouldSplit(delta) && hasNotBurst() && getEnv().isPresent()) {
			Environment e = getEnv().get();
			e.requestBurst(Protozoan.this, Protozoan.class, createChild);
		}

		generateThrust(delta);
		handleMating(delta);

		getParticle().setRangedInteractionRadius(getInteractionRange());
	}

	private void handleMating(float delta) {
		if (matingCooldown > 0) {
			matingCooldown -= delta;
			return;
		}
		if (!Environment.settings.protozoa.matingEnabled.get())
			return;

		// Probabilistic per-frame check on a continuous signal — at signal=1
		// the cell tries to mate ~MATE_BASE_RATE times per second on average,
		// at signal=0 never. This preserves selection gradients (small
		// signal → rare mating) instead of a binary threshold that snaps
		// between full-rate and never. Partner consent uses the same scale.
		final float MATE_BASE_RATE = 2f;
		if (Simulation.RANDOM.nextFloat() >= mateDesireSignal * delta * MATE_BASE_RATE)
			return;

		for (Collision contact : getParticle().getContacts()) {
			Object other = contact.getOther(contact);
			if (other instanceof Protozoan) {
				Protozoan partner = (Protozoan) other;
				if (Simulation.RANDOM.nextFloat() < partner.mateDesireSignal) {
					setMate(partner);
					return;
				}
			}
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

	// Three cell-level evolvable sequences, all applied to every phagocytic
	// receptor this cell builds (so the cell as a whole has one "diet").
	//
	//   plantReceptorKey (50 chars):  matched against PlantCell.surfaceSignature.
	//     Higher identity = faster plant digestion.
	//
	//   protozoaReceivingReceptor (75 chars): cell's IDENTITY. What predators
	//     target when they try to eat this cell.
	//
	//   protozoaPhagocyticReceptor (75 chars): what THIS cell looks for in
	//     prey protozoa. Matched against the prey's protozoaReceivingReceptor.
	//
	// Predation is ASYMMETRIC. A eats B iff A.phagocytic matches B.receiving.
	// B eats A iff B.phagocytic matches A.receiving. The two are independent
	// — predator status is directional.
	//
	// Critically: for a cell NOT to eat its own kin, the lineage must keep
	// its own phagocytic ≠ its own receiving. A cell whose phag ≈ recv would
	// match its siblings as prey AND be matched by its siblings as prey —
	// the lineage cannibalizes itself and goes extinct. Selection thus
	// drives phag ≠ recv. Net effect: lineages diverge into "I am X /
	// I hunt Y" specializations where X and Y are different sequences.
	private AminoAcidSequence plantReceptorKey;
	private AminoAcidSequence protozoaReceivingReceptor;
	private AminoAcidSequence protozoaPhagocyticReceptor;

	@EvolvableObject(name="Plant Receptor Key",
					 traitClass="com.protoevo.biology.evolution.PlantSignatureTrait")
	public void setPlantReceptorKey(AminoAcidSequence key) {
		this.plantReceptorKey = key;
	}

	@EvolvableObject(name="Receiving Receptor",
					 traitClass="com.protoevo.biology.evolution.ProtozoaSignatureTrait")
	public void setProtozoaReceivingReceptor(AminoAcidSequence key) {
		this.protozoaReceivingReceptor = key;
	}

	@EvolvableObject(name="Phagocytic Receptor",
					 traitClass="com.protoevo.biology.evolution.ProtozoaSignatureTrait")
	public void setProtozoaPhagocyticReceptor(AminoAcidSequence key) {
		this.protozoaPhagocyticReceptor = key;
	}

	public AminoAcidSequence getPlantReceptorKey()           { return plantReceptorKey; }
	public AminoAcidSequence getProtozoaReceivingReceptor()  { return protozoaReceivingReceptor; }
	public AminoAcidSequence getProtozoaPhagocyticReceptor() { return protozoaPhagocyticReceptor; }

	// Contact-only signature-match sensors. The NN can only know about
	// compatibility with a neighbour it's PHYSICALLY TOUCHING — same as
	// real cells, which sense membrane proteins via direct contact rather
	// than at a distance. Returns 0 if not in contact with anything of
	// the relevant type. Evolved NNs can use these to:
	//   - approach/stay near a high-match plant (more food per second)
	//   - engulf-or-not when touching another protozoa (Prey Match)
	//   - flee/retract when touching a predator (Predator Threat)

	@GeneRegulator(name="Touch Plant Match", min=0, max=1)
	public float getTouchPlantMatch() {
		if (plantReceptorKey == null || getParticle() == null) return 0f;
		float best = 0f;
		for (Collision c : getParticle().getContacts()) {
			Object o = c.getOther(getParticle());
			if (!(o instanceof com.protoevo.physics.Particle)) continue;
			Object u = ((com.protoevo.physics.Particle) o).getUserData();
			if (u instanceof PlantCell) {
				AminoAcidSequence sig = ((PlantCell) u).getSurfaceSignature();
				if (sig != null) {
					float m = plantReceptorKey.identityWith(sig);
					if (m > best) best = m;
				}
			}
		}
		return best;
	}

	@GeneRegulator(name="Touch Prey Match", min=0, max=1)
	public float getTouchPreyMatch() {
		if (protozoaPhagocyticReceptor == null || getParticle() == null) return 0f;
		float best = 0f;
		for (Collision c : getParticle().getContacts()) {
			Object o = c.getOther(getParticle());
			if (!(o instanceof com.protoevo.physics.Particle)) continue;
			Object u = ((com.protoevo.physics.Particle) o).getUserData();
			if (u instanceof Protozoan && u != this) {
				AminoAcidSequence sig = ((Protozoan) u).getProtozoaReceivingReceptor();
				if (sig != null) {
					float m = protozoaPhagocyticReceptor.identityWith(sig);
					if (m > best) best = m;
				}
			}
		}
		return best;
	}

	@GeneRegulator(name="Touch Predator Threat", min=0, max=1)
	public float getTouchPredatorThreat() {
		if (protozoaReceivingReceptor == null || getParticle() == null) return 0f;
		float best = 0f;
		for (Collision c : getParticle().getContacts()) {
			Object o = c.getOther(getParticle());
			if (!(o instanceof com.protoevo.physics.Particle)) continue;
			Object u = ((com.protoevo.physics.Particle) o).getUserData();
			if (u instanceof Protozoan && u != this) {
				AminoAcidSequence theirPhag =
						((Protozoan) u).getProtozoaPhagocyticReceptor();
				if (theirPhag != null) {
					float m = theirPhag.identityWith(protozoaReceivingReceptor);
					if (m > best) best = m;
				}
			}
		}
		return best;
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

		// Sample well outside the cell's own footprint, otherwise the readings
		// are dominated by chemicals the cell itself just consumed — that's
		// the "surrounded by plants but gradient = 0 or negative" case.
		// Normalize by the sum so the *direction* signal stays meaningful
		// when the field is densely saturated front and back.
		Vector2 pos = getPos();
		float sampleR = 4f * getRadius();
		tmp.set(pos).add(dir.setLength(sampleR));
		float plantAhead  = solution.getPlantDensity(tmp);
		tmp.set(pos).sub(dir.setLength(sampleR));
		float plantBehind = solution.getPlantDensity(tmp);
		float sum = plantAhead + plantBehind;
		if (sum <= 1e-6f) return 0f;
		return (plantAhead - plantBehind) / sum;
	}

	@GeneRegulator(name="Plant Density Local", min=0, max=1)
	public float getPlantDensityLocal() {
		Optional<Environment> env = getEnv();
		if (!env.isPresent())
			return 0;
		// Direct spatial-hash count of plant CELLS in the same chunk as us.
		// We deliberately do NOT use the chemical field here: chemicals at
		// the cell's own position get consumed by the cell's own extraction
		// pass, so a protozoan sitting on plants reads ~0 chemical — that's
		// what made the original "Plant Density" signal misleading.
		// Counting actual plant entities is the correct "I'm in a food
		// cluster" signal: it reflects ground truth, not a depleted field.
		Environment e = env.get();
		int count = e.getLocalCount(PlantCell.class, getPos());
		int cap = Math.max(1, e.getLocalCapacity(PlantCell.class));
		float density = (float) count / (float) cap;
		return density > 1f ? 1f : density;
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

	// The old getOrientation was broken — Java's `%` returns a negative
	// result for negative operands, so thrustAngle % 2π for any
	// negative-rotation lineage returned values outside [0,1]. Also: a
	// single-channel circular value with a discontinuity at the wrap point
	// is hostile to NN training — the NN sees angle 359° and 1° as far
	// apart even though they're adjacent. The standard fix is to split
	// the angle into sin/cos components, both bounded in [-1,1] with no
	// discontinuity.
	@GeneRegulator(name="Heading Sin", min=-1, max=1)
	public float getHeadingSin() {
		return (float) Math.sin(thrustAngle);
	}
	@GeneRegulator(name="Heading Cos", min=-1, max=1)
	public float getHeadingCos() {
		return (float) Math.cos(thrustAngle);
	}

	// getProtozoaSpeed was advertised as min=0,max=1 but `getSpeed()/getRadius()`
	// can easily exceed 1 (a small fast cell). Clamp into the advertised
	// range so the NN input isn't silently saturated and lying about the
	// dynamic range available to it.
	@GeneRegulator(name="Speed", min=0, max=1)
	public float getProtozoaSpeed() {
		float r = getRadius();
		if (r <= 1e-9f)
			return 0f;
		float s = getSpeed() / r;
		return s > 1f ? 1f : s;
	}

	@ControlVariable(name="Mate Desire", min=0, max=1)
	public void setMateDesire(float mate) {
		this.mateDesireSignal = mate;
	}

	@ControlVariable(name="Split Desire", min=0, max=1)
	public void setSplitDesire(float split) {
		this.splitDesireSignal = split;
	}

	// ===== Memory state =====
	private float[] mem() {
		if (memory == null) memory = new float[MEMORY_SLOTS];
		return memory;
	}
	private float[] memGates() {
		if (memoryGate == null) {
			memoryGate = new float[MEMORY_SLOTS];
			// Default to 1.0 (always-write) so a freshly-spawned cell with
			// no GRN influence yet still behaves like the old un-gated
			// memory until evolution wires the gates up.
			for (int i = 0; i < MEMORY_SLOTS; i++) memoryGate[i] = 1f;
		}
		return memoryGate;
	}
	/** Gated blend: slot = (1 - α·gate)·slot + α·gate·input.
	 *  When gate ≈ 0 the slot doesn't update this tick (true latch);
	 *  when gate ≈ 1 it blends at the layer's base α. */
	private void writeMemory(int slot, float input) {
		float[] m = mem();
		float gate = memGates()[slot];
		if (gate < 0f) gate = 0f;
		else if (gate > 1f) gate = 1f;
		float a = MEMORY_ALPHA[slot] * gate;
		m[slot] = (1f - a) * m[slot] + a * input;
	}

	// --- Direct slot writes (value the NN wants to commit if its gate fires) ---
	@ControlVariable(name="Memory Fast 0", min=-1, max=1)
	public void setMemoryFast0(float v) { writeMemory(0, v); }
	@ControlVariable(name="Memory Fast 1", min=-1, max=1)
	public void setMemoryFast1(float v) { writeMemory(1, v); }
	@ControlVariable(name="Memory Med 0", min=-1, max=1)
	public void setMemoryMed0(float v) { writeMemory(2, v); }
	@ControlVariable(name="Memory Med 1", min=-1, max=1)
	public void setMemoryMed1(float v) { writeMemory(3, v); }
	@ControlVariable(name="Memory Slow 0", min=-1, max=1)
	public void setMemorySlow0(float v) { writeMemory(4, v); }
	@ControlVariable(name="Memory Slow 1", min=-1, max=1)
	public void setMemorySlow1(float v) { writeMemory(5, v); }

	// --- Per-slot write gates (LSTM-style "should I write this tick?") ---
	@ControlVariable(name="Gate Fast 0", min=0, max=1)
	public void setGateFast0(float v) { memGates()[0] = v; }
	@ControlVariable(name="Gate Fast 1", min=0, max=1)
	public void setGateFast1(float v) { memGates()[1] = v; }
	@ControlVariable(name="Gate Med 0", min=0, max=1)
	public void setGateMed0(float v) { memGates()[2] = v; }
	@ControlVariable(name="Gate Med 1", min=0, max=1)
	public void setGateMed1(float v) { memGates()[3] = v; }
	@ControlVariable(name="Gate Slow 0", min=0, max=1)
	public void setGateSlow0(float v) { memGates()[4] = v; }
	@ControlVariable(name="Gate Slow 1", min=0, max=1)
	public void setGateSlow1(float v) { memGates()[5] = v; }

	// --- Direct slot reads ---
	@GeneRegulator(name="Memory Fast 0", min=-1, max=1)
	public float getMemoryFast0() { return mem()[0]; }
	@GeneRegulator(name="Memory Fast 1", min=-1, max=1)
	public float getMemoryFast1() { return mem()[1]; }
	@GeneRegulator(name="Memory Med 0", min=-1, max=1)
	public float getMemoryMed0() { return mem()[2]; }
	@GeneRegulator(name="Memory Med 1", min=-1, max=1)
	public float getMemoryMed1() { return mem()[3]; }
	@GeneRegulator(name="Memory Slow 0", min=-1, max=1)
	public float getMemorySlow0() { return mem()[4]; }
	@GeneRegulator(name="Memory Slow 1", min=-1, max=1)
	public float getMemorySlow1() { return mem()[5]; }

	// --- Read-only product slots: the multiplicative composition of two
	//     memory slots from different temporal layers. Additive NNs can't
	//     synthesize "A AND B"-type conjunctive features without
	//     specifically-tuned activations on hidden neurons; exposing
	//     the product gives evolution that operation directly.
	//     Range is [-1, 1] × [-1, 1] = [-1, 1] so the regulator mapping
	//     is correct without renormalising.
	@GeneRegulator(name="Memory Fast×Slow", min=-1, max=1)
	public float getMemoryFastSlow() { return mem()[0] * mem()[4]; }
	@GeneRegulator(name="Memory Med×Med", min=-1, max=1)
	public float getMemoryMedMed() { return mem()[2] * mem()[3]; }

	/** Read-only view of the memory state, for UI inspection. */
	public float[] getMemory() {
		return mem();
	}
	public float[] getMemoryGates() {
		return memGates();
	}
	// ===== end Memory state =====

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

	private boolean shouldSplit(float delta) {
		// Hard gates: must be big enough and healthy enough.
		if (getRadius() < splitRadius)
			return false;
		if (getHealth() < Environment.settings.protozoa.minHealthToSplit.get())
			return false;
		// Probabilistic check on the continuous NN signal so split rate
		// scales smoothly with desire instead of snapping at 0.5. At
		// signal=1 the cell averages ~SPLIT_BASE_RATE splits per second
		// (still gated by radius/health regrowth between splits).
		final float SPLIT_BASE_RATE = 1.5f;
		return Simulation.RANDOM.nextFloat() < splitDesireSignal * delta * SPLIT_BASE_RATE;
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
		if (burstMeat && hasNotBurst()) {
			if (getEnv().isPresent()) {
				Environment e = getEnv().get();
				e.requestBurst(
						Protozoan.this,
						MeatCell.class,
						createMeatOnDeath,
						true);
			}
		}
		super.kill(causeOfDeath);
	}

	private MeatCell createMeat(float r) {
		if (getEnv().isEmpty())
			throw new RuntimeException("Cannot create meat cell without environment");

		return new MeatCell(r, getEnv().get());
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
		if (geneExpressionFunction.getGRNGenome() != null) {
			stats.put("Lineage Mutation Rate ×",
					geneExpressionFunction.getGRNGenome().getMutationRateMultiplier());
		}

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
		// 1. STARVATION damage: only when actually energy-starved (below 25%
		//    of capacity). Without this guard the original code bled well-fed
		//    cells to death on starvationFactor — the "dying with a ton of
		//    energy" case.
		float energyCap = getRadius()
				* Environment.settings.cell.energyCapFactor.get();
		if (energyCap <= 0f || getEnergyAvailable() < 0.25f * energyCap) {
			damageRate = getRadius()
					* Environment.settings.protozoa.starvationFactor.get();
			damage(damageRate * delta, CauseOfDeath.OLD_AGE);
		}

		// 2. SENESCENCE damage: age-driven, applies regardless of energy.
		//    Without this, a cell that maxed its radius and pinned its
		//    energy near the cap had ZERO death pathway — growth stopped,
		//    splits were blocked by health < minHealthToSplit, and the cell
		//    just sat there. Users observed individuals alive for 2000+ sim
		//    seconds doing nothing. Quadratic ramp past maxLifespan
		//    guarantees no immortals: at 2× lifespan damage hits peak rate,
		//    at 3× lifespan it's 4× peak — death is inevitable.
		float maxLifespan = Environment.settings.protozoa.maxLifespan.get();
		if (maxLifespan > 0f) {
			float excess = (getTimeAlive() - maxLifespan) / maxLifespan;
			if (excess > 0f) {
				float rate = excess * excess
						* Environment.settings.protozoa.senescenceDeathRate.get();
				damage(rate * delta, CauseOfDeath.OLD_AGE);
			}
		}
	}

	@Override
	public boolean isEdible() {
		// Living protozoa are now edible — but only by phagocytic receptors
		// whose protozoa-key matches this cell's surface signature closely
		// enough to clear the 80% identity threshold. So in practice, a
		// generalist receptor with random keys can't just gobble its
		// neighbours: predation requires a specifically-evolved key that
		// matches the prey lineage. See PhagocyticReceptor.engulfCondition.
		return !isDead();
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

	public static class SpawnChildFn implements SerializableFunction<Float, Protozoan> {

		private Protozoan protozoa;

		public SpawnChildFn() {}

		public SpawnChildFn(Protozoan protozoa) {
			this.protozoa = protozoa;
		}

		@Override
		public Protozoan apply(Float r) {
			return protozoa.createSplitChild(r);
		}
	}

	public static class SpawnMeatCell implements SerializableFunction<Float, MeatCell> {

		private Protozoan protozoa;

		public SpawnMeatCell() {}

		public SpawnMeatCell(Protozoan protozoa) {
			this.protozoa = protozoa;
		}

		@Override
		public MeatCell apply(Float r) {
			return protozoa.createMeat(r);
		}
	}
}
