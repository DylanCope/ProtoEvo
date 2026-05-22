package com.protoevo.core;

import com.github.javafaker.Faker;
import com.protoevo.biology.cells.Protozoan;
import com.protoevo.biology.nn.NetworkGenome;
import com.protoevo.core.repl.REPL;
import com.protoevo.env.serialization.Serialization;
import com.protoevo.env.Environment;
import com.protoevo.settings.SimulationSettings;
import com.protoevo.utils.EnvironmentImageRenderer;
import com.protoevo.utils.FileIO;
import com.protoevo.utils.TimedEventsManager;
import com.protoevo.utils.Utils;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Simulation implements Runnable
{
	protected Environment environment;
	protected ApplicationManager manager;
	private volatile boolean simulate, saveRequested = false, busyOnOtherThread = false;
	private static boolean paused = false;
	private float timeDilation = 1, timeSinceSave = 0, timeSinceSnapshot = 0, timeSinceAutoSave = 0;
	private TimedEventsManager timedEventsManager;
	
	public static Random RANDOM = new Random(Environment.settings.simulationSeed.get());
	private boolean debug = false;
	protected boolean initialised = false;

	protected Supplier<Environment> environmentLoader;
	private String name;
	private List<String> statsNames;
	private final REPL repl = new REPL(this);
	private SimulationHistory history;

	public Simulation() {
		this(Environment.settings.simulationSeed.get());
	}

	public Simulation(long seed)
	{
		RANDOM = new Random(seed);
		simulate = true;
		name = generateSimName();
		environmentLoader = this::newDefaultEnv;
		loadSettings();
	}

	public Simulation(String name) {
		this(Environment.settings.simulationSeed.get(), name);
	}

	public Simulation(String name, SimulationSettings settings) {
		RANDOM = new Random(settings.simulationSeed.get());
		this.name = name;
		simulate = true;
		environmentLoader = () -> newEnvironment(settings);
	}

	public Simulation(long seed, String name)
	{
		RANDOM = new Random(seed);
		simulate = true;
		this.name = name;

		environmentLoader = this::loadMostRecentEnv;
		loadSettings();
	}

	public Simulation(String name, String save) {
		this(Environment.settings.simulationSeed.get(), name, save);
	}

	public Simulation(long seed, String name, String save)
	{
		RANDOM = new Random(seed);
		simulate = true;
		this.name = name;

		environmentLoader = () -> loadEnv(FileIO.getSavesDir() + "/" + name + "/env/" + save);
		loadSettings();
	}

	private void loadSettings() {}

	public static void newSaveDir(String simulationName) {
		try {
			System.out.println("Created new simulation named: " + simulationName);
			Files.createDirectories(FileIO.getSavesDir());
			Files.createDirectories(Paths.get(FileIO.getSavesDir() + "/" + simulationName));
			Files.createDirectories(Paths.get(FileIO.getSavesDir() + "/" + simulationName + "/env"));
			Files.createDirectories(Paths.get(FileIO.getSavesDir() + "/" + simulationName + "/stats"));
			Files.createDirectories(Paths.get(FileIO.getSavesDir() + "/" + simulationName + "/stats/summaries"));
			Files.createDirectories(Paths.get(FileIO.getSavesDir() + "/" + simulationName + "/stats/protozoa-genomes"));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static String generateSimName() {
		Faker faker = new Faker();
		return String.format("%s-%s-%s",
				faker.ancient().primordial().toLowerCase().replaceAll(" ", "-"),
				faker.pokemon().name().toLowerCase().replaceAll(" ", "-"),
				faker.lorem().word().toLowerCase().replaceAll(" ", "-"));
	}

	public Environment newDefaultEnv()
	{
		newSaveDir(name);
		return new Environment();
	}

	public Environment newEnvironment(SimulationSettings settings)
	{
		newSaveDir(name);
		return new Environment(settings);
	}

	public Environment loadEnv(String filename)
	{
		try {
			Environment env = Serialization.reloadEnvironment(filename);
			System.out.println("Loaded tank at: " + filename);
			initialised = true;
			return env;
		} catch (Exception e) {
//			return newDefaultEnv();
			throw new RuntimeException(e);
		}
	}

	public void setManager(ApplicationManager manager) {
		this.manager = manager;
		repl.setManager(manager);
	}

	public static Stream<Path> getStatsPaths(String name) {
		Path dir = Paths.get(FileIO.getSavesDir() + "/" + name + "/stats/summaries");
		if (Files.exists(dir)) {
			try (Stream<Path> paths = Files.list(dir)){
				return paths.collect(Collectors.toList()).stream()
						.filter(path -> path.getFileName().toString().endsWith(".json"));
			} catch (IOException e) {
				System.out.println("Unable to find environment of given name: " + e.getMessage());
				System.exit(0);
				return Stream.empty();
			}
		}
		return Stream.empty();
	}

	public static Optional<Path> getClosestStatsPath(String name, Long time) {
		return getStatsPaths(name).min(
				Comparator.comparingLong(
					path -> Math.abs(time - path.toFile().lastModified())));
	}

	public static Stream<Path> getSavePaths(String name) {
		Path dir = Paths.get(FileIO.getSavesDir() + "/" + name + "/env");
		if (Files.exists(dir)) {
			try (Stream<Path> paths = Files.list(dir)){
				return paths.collect(Collectors.toList()).stream().filter(Files::isDirectory);
			} catch (IOException e) {
				System.out.println("Unable to find environment of given name: " + e.getMessage());
				System.exit(0);
				return Stream.empty();
			}
		}
		return Stream.empty();
	}

	public static Long saveModifiedTime(Path path) {
		return Paths.get(path.toString() + "/environment.dat")
				.toFile().lastModified();
	}

	public static Optional<Path> getMostRecentSave(String name) {
		return getSavePaths(name).max(Comparator.comparingLong(Simulation::saveModifiedTime));
	}

	public Environment loadMostRecentEnv() {
		Optional<String> lastFilePath = getMostRecentSave(name).map(Path::toString);
		if (lastFilePath.isPresent())
			return loadEnv(lastFilePath.get());
		else {
			System.out.println("Unable to find environment of given name.");
			return newDefaultEnv();
		}
	}

	public void prepare()
	{
		paused = false;
		environment = environmentLoader.get();
		environment.setSimulationName(name);
		history = new SimulationHistory(getSaveFolder());

		if (!initialised) {
			environment.initialise();
			makeStatisticsSnapshot();
			initialised = true;
		}
		new Thread(repl).start();

		if (manager != null) {
			manager.notifySimulationReady();
		}

		timedEventsManager = new TimedEventsManager();
		timedEventsManager.add(
			t -> t >= Environment.settings.misc.timeBetweenHistoricalSaves.get() || saveRequested,
			this::save
		);
		timedEventsManager.add(
			Environment.settings.misc.statisticsSnapshotTime::get,
			this::makeStatisticsSnapshot
		);
		timedEventsManager.add(
			Environment.settings.misc.timeBetweenAutoSaves::get,
			this::createAutoSave
		);
		timedEventsManager.add(
			() -> homeostasisInterval,
			this::homeostasisTick
		);
	}

	// ===== Homeostatic difficulty controller (PID) =====
	//
	// Earlier versions used a static `scale = f(pop/target)` lookup that
	// snapped immediately and capped at fixed multipliers. That was too
	// rigid — sustained overpopulation didn't cause growing pressure, and
	// undercrowding didn't get an extra push to recover.
	//
	// This is now a proper PID controller on the normalized population error
	// e(t) = (pop - target) / target. The control signal u(t) =
	// Kp·e + Ki·∫e dt + Kd·de/dt  is then mapped through per-parameter
	// exponentials onto the four levers (decay, plant ED, meat ED, plant
	// contact death). Highlights of the design:
	//
	//   • No hard caps. Sustained over-target population grows the integral
	//     term over time, so pressure mounts continuously — exactly the
	//     "actively change grazing over time" behavior wanted. The
	//     parameters never reach zero/infinity because exp() is bounded by
	//     `MAX_LOG_DEVIATION`.
	//
	//   • Derivative damping. When pop is rapidly approaching target the
	//     derivative term softens the response, preventing the classic
	//     PI-only oscillate-and-overshoot pattern.
	//
	//   • Per-parameter gains. Plant contact-death is a fast lever (high
	//     gain) — it directly cuts the food supply at its source. Plant
	//     energy density is a medium lever. Decay rate is slow. Starting
	//     energy is the gentlest because it only affects future newborns.
	//
	//   • Integral anti-windup. The integral is clamped so a long-term
	//     extinction or runaway can't lock the controller into max output.
	//
	//   • Active equilibrium. At pop == target the controller does almost
	//     nothing; tiny errors are nudged away. Above/below, pressure
	//     gradually builds — this naturally creates the oscillating
	//     "active competition" the user wanted, with periods of grazing
	//     stress alternating with growth windows.
	private boolean homeostasisEnabled = true;
	// 250 → 500. Initial spawn cohort is 500 protozoa from worldgen. With
	// target 250 the homeostat saw 100% over-target on the very first tick
	// and cranked decay to ~3.7× — draining a typical cell's 250J starting
	// energy in ~3 sim-seconds. The diagnostic showed the entire initial
	// cohort dying from this artificial throttling, not from real ecology.
	// Aligning target with the spawn size lets the new cohort settle
	// without immediate hostile pressure; the natural selection-pressure
	// ratchet still tightens the world over time.
	private int homeostasisTargetPop = 500;
	private int homeostasisTargetPlants = 1000;
	private float homeostasisInterval = 5f; // sim-seconds between updates

	// PID gains. Levers are all *natural* parameters (food density, decay,
	// grazing pressure, chemical drip availability) — never a hard population
	// cap, so the population is shaped by selection, not gated.
	//
	// Tuning notes from extensive sim experience:
	//   • Earlier the integral was hard-capped at ±30. That worked as crude
	//     anti-windup but ruined fine tracking: once saturated, the lever
	//     stayed maxed even as pop crossed target, causing big undershoot
	//     after a big overshoot. Replaced with a *leaky* integral that
	//     decays a fixed fraction per tick; this prevents windup naturally
	//     and lets D actually do the fine-correction work.
	//   • MAX_LOG_DEVIATION was 7 (≈ 1100× lever swing). That capped
	//     plantED-style levers at 0.001 — not zero. Bumped to 18 so exp
	//     can produce effectively-zero multipliers when the controller
	//     needs to fully shut down a food source.
	//   • Kd doubled to 3.0 — damps the discrete-PID oscillation that
	//     comes from a 5-second tick observing a population that can
	//     change by 10%+ between ticks.
	private static final float PID_KP = 1.1f;
	private static final float PID_KI = 0.06f;
	private static final float PID_KD = 3.0f;
	private static final float PID_INTEGRAL_LEAK = 0.08f; // per tick
	private static final float MAX_LOG_DEVIATION = 18f;   // exp(-18) ≈ 1.5e-8 (effectively zero)

	// Per-parameter gains. Sign = direction (positive control = more pressure):
	//   over-target → less food, faster decay, gentler newborn energy.
	// All levers are *consumption-side* now. Earlier we also drove
	// `plant.collisionDestructionRate` (kill plants on grazer contact harder
	// when protozoa over target) — but that backfired: dead plants become
	// MEAT which then feeds the same overpopulated protozoa we're trying to
	// starve. It also caused thousands of plant deaths per second at high u
	// since the lever was unbounded. Plant abundance is now solely the plant
	// PID's job, leaving this controller to throttle food and accelerate
	// starvation.
	private static final float GAIN_DECAY        = 0.8f;
	private static final float GAIN_PLANT_ED     = -0.85f;
	private static final float GAIN_MEAT_ED      = -0.7f;
	private static final float GAIN_START_E      = -0.5f;
	// Reduced from -1.5 to -0.4. A high-magnitude gain combined with the
	// leaky integral was producing >1000× chemDrip multipliers at low pop,
	// which let cells nibble-feed indefinitely without needing to match
	// plant signatures — defeating the whole receptor system. With -0.4
	// the lever's max swing is ~exp(0.4 × ~4) ≈ 5×, so the homeostat
	// can still encourage feeding during a crash but can't turn drips
	// into a primary food source. Engulf (where receptor match matters)
	// is now the dominant feeding path.
	private static final float GAIN_CHEM_EXTRACT = -0.4f;

	private float pidIntegral = 0f;
	private float pidLastError = Float.NaN;
	private long pidLastLogMs = 0;

	// Plant-side PID. Same leaky-integral / high-Kd structure as the
	// protozoa controller. Slightly softer Kp (plants react slower than
	// protozoa to setting changes, so we want gentler proportional gain
	// to avoid chasing noise) and a stronger Kd (plant lifecycle is on
	// the same time-scale as the controller tick, so damping matters more).
	private static final float PLANT_PID_KP = 0.9f;
	private static final float PLANT_PID_KI = 0.05f;
	private static final float PLANT_PID_KD = 4.0f;
	private static final float PLANT_PID_INTEGRAL_LEAK = 0.10f;

	// Plant levers. Photosynthesis is the heavy lever — directly controls
	// energy income, which combined with the universal starvation damage
	// gives selection pressure with teeth. Construction rate limits how
	// much mass a plant has for growth/repair. Growth rate caps how fast
	// they reach split radius. Split-health threshold makes splitting
	// impossible when over-target (threshold > 1 → no plant can split).
	// The new plantSplitRate lever directly scales the probabilistic
	// split rate so even healthy mature plants slow their reproduction
	// when over-target, instead of the old "instant burst on reaching
	// maxRadius" that caused the divide-then-die churn.
	private static final float GAIN_PLANT_PHOTOSYNTHESIS = -1.0f;
	private static final float GAIN_PLANT_CONSTRUCTION   = -0.8f;
	private static final float GAIN_PLANT_GROWTH         = -0.7f;
	private static final float GAIN_PLANT_SPLIT_HEALTH   = 1.2f;
	private static final float GAIN_PLANT_SPLIT_RATE     = -1.3f;

	private float plantPidIntegral = 0f;
	private float plantPidLastError = Float.NaN;

	// Reference values: must match the in-Java defaults set in CellSettings /
	// SimulationSettings. Multipliers are computed relative to these.
	// Re-interpreted: this is now a FRACTIONAL coefficient applied to
	// (radius/minR × energyAvailable) in Cell.decayResources, not a flat
	// J/sec rate. Keep in sync with CellSettings.energyDecayRate default.
	private static final float HOMEO_BASE_DECAY = 0.005f;
	private static final float HOMEO_BASE_PLANT_ED = 2e5f;
	private static final float HOMEO_BASE_MEAT_ED = 6e5f;
	private static final float HOMEO_BASE_START_E = 50f;
	private static final float HOMEO_BASE_CHEM_EXTRACT = 100f;
	private static final float HOMEO_BASE_PLANT_PHOTOSYNTHESIS = 300f;
	private static final float HOMEO_BASE_PLANT_CONSTRUCTION = 10f;
	private static final float HOMEO_BASE_PLANT_MAX_GROWTH = 1.5f;
	private static final float HOMEO_BASE_PLANT_MIN_GROWTH = 0f;
	private static final float HOMEO_BASE_PLANT_SPLIT_HEALTH = 0.15f;
	// Per-second probability a mature plant splits this update. Baseline
	// gives ~30 sim-sec adult lifetime before splitting; the PID scales
	// this to throttle reproduction without snapping it fully off.
	private static final float HOMEO_BASE_PLANT_SPLIT_RATE = 1f / 30f;


	private void homeostasisTick() {
		if (!homeostasisEnabled || environment == null) return;
		// Run the plant controller alongside the protozoa one — they target
		// different populations with different levers, so they can't interfere.
		plantHomeostasisTick();
		// Ratchet the selection-pressure exponent up when conditions allow.
		// Independent of the PID — only goes one direction.
		maybeRatchetSelection();
		int pop = environment.numberOfProtozoa();
		if (pop <= 0) {
			// Extinct: spawn a fresh seeded cohort so the sim keeps running.
			// Reset PID integral so we don't yank the freshly-respawned
			// world into an over-tight state on the first tick.
			pidIntegral = 0f;
			pidLastError = Float.NaN;
			respawnProtozoaIfExtinct();
			return;
		}

		// Normalized error: positive = over target, negative = under.
		float error = ((float) pop - homeostasisTargetPop) / (float) homeostasisTargetPop;

		// Leaky integral: each tick decays the accumulated integral by a
		// fixed fraction, then adds current error. This bounds the integral
		// naturally (steady-state I = error * interval / leak) without the
		// hard clamp that caused undershoot after a big overshoot.
		pidIntegral = pidIntegral * (1f - PID_INTEGRAL_LEAK) + error * homeostasisInterval;

		// Derivative term — first call has no history, treat as zero.
		float derivative = 0f;
		if (!Float.isNaN(pidLastError))
			derivative = (error - pidLastError) / homeostasisInterval;
		pidLastError = error;

		float control = PID_KP * error + PID_KI * pidIntegral + PID_KD * derivative;

		// Apply per-parameter exponential mapping. This keeps multipliers
		// strictly positive and bounded (no zeros, no infinities).
		Environment.settings.cell.energyDecayRate.set(
				HOMEO_BASE_DECAY * mul(GAIN_DECAY * control));
		Environment.settings.plantEnergyDensity.set(
				HOMEO_BASE_PLANT_ED * mul(GAIN_PLANT_ED * control));
		Environment.settings.meatEnergyDensity.set(
				HOMEO_BASE_MEAT_ED * mul(GAIN_MEAT_ED * control));
		Environment.settings.cell.startingAvailableCellEnergy.set(
				HOMEO_BASE_START_E * mul(GAIN_START_E * control));
		Environment.settings.cell.chemicalExtractionFactor.set(
				HOMEO_BASE_CHEM_EXTRACT * mul(GAIN_CHEM_EXTRACT * control));

		// Log roughly every 30 seconds so an overnight run leaves a record
		// of how the controller reacted, without spamming during steady state.
		long now = (long)(environment.getElapsedTime() * 1000);
		if (pidLastLogMs == 0 || now - pidLastLogMs > 30_000L) {
			int plantPop = environment.getCount(com.protoevo.biology.cells.PlantCell.class);
			float plantCtrl = PLANT_PID_KP * ((plantPop - homeostasisTargetPlants) / (float) homeostasisTargetPlants)
					+ PLANT_PID_KI * plantPidIntegral;
			System.out.printf(
					"[homeostat] proto=%d/%d err=%+.2f u=%+.2f decayx%.2f plantEDx%.3f chemDripx%.3f  "
					+ "plants=%d/%d uP=%+.2f photoSynx%.3f splitRatex%.3f%n",
					pop, homeostasisTargetPop, error, control,
					mul(GAIN_DECAY * control), mul(GAIN_PLANT_ED * control),
					mul(GAIN_CHEM_EXTRACT * control),
					plantPop, homeostasisTargetPlants, plantCtrl,
					mul(GAIN_PLANT_PHOTOSYNTHESIS * plantCtrl),
					mul(GAIN_PLANT_SPLIT_RATE * plantCtrl));

			// Diagnostic: what's actually inside a typical protozoan? If
			// avgFood > 0 but avgConstrMass ≈ 0 the digest path isn't
			// converting. If avgFood ≈ 0 too, eat isn't filling food. If
			// avgEnergy stays low even after the food-bug fixes, the
			// problem is the energy economy, not the food pipeline.
			double sumE = 0, sumCM = 0, sumFood = 0, sumR = 0, sumAge = 0;
			int n = 0, engulfing = 0, withMass = 0;
			for (com.protoevo.biology.cells.Cell c : environment.getCells()) {
				if (!(c instanceof com.protoevo.biology.cells.Protozoan)) continue;
				com.protoevo.biology.cells.Protozoan p = (com.protoevo.biology.cells.Protozoan) c;
				if (p.isDead()) continue;
				sumE += p.getEnergyAvailable();
				sumCM += p.getConstructionMassAvailable();
				if (p.getConstructionMassAvailable() > 1e-9f) withMass++;
				for (com.protoevo.biology.Food f : p.getFoodToDigest().values())
					sumFood += f.getSimpleMass();
				sumR += p.getRadius();
				sumAge += p.getTimeAlive();
				if (!p.getEngulfedCells().isEmpty()) engulfing++;
				n++;
			}
			if (n > 0) {
				System.out.printf("[diag] n=%d avgE=%.1f/%.0f  avgCM=%.5g  avgFood=%.5g  "
						+ "avgR=%.4f  avgAge=%.1fs  engulfing=%d  withMass=%d%n",
						n, sumE / n,
						Environment.settings.cell.energyCapFactor.get() * (sumR / n)
								/ Environment.settings.minParticleRadius.get(),
						sumCM / n, sumFood / n, sumR / n, sumAge / n,
						engulfing, withMass);
			}

			// What's actually killing things this window? Drains the
			// per-cause counters from Environment so the next window
			// reports a fresh delta. Empty maps print as "[deaths] none"
			// so the line is grep-able either way.
			logDeathCauses("protozoa", environment.drainProtozoaDeaths());
			logDeathCauses("plants  ", environment.drainPlantDeaths());

			pidLastLogMs = now;
		}
	}

	private static void logDeathCauses(String label,
			java.util.Map<com.protoevo.biology.CauseOfDeath, Integer> counts) {
		if (counts == null || counts.isEmpty()) {
			System.out.printf("[deaths %s] none%n", label);
			return;
		}
		// Print highest-count first so the dominant killer is immediately visible.
		java.util.List<java.util.Map.Entry<com.protoevo.biology.CauseOfDeath, Integer>>
				sorted = new java.util.ArrayList<>(counts.entrySet());
		sorted.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
		StringBuilder sb = new StringBuilder();
		for (java.util.Map.Entry<com.protoevo.biology.CauseOfDeath, Integer> e : sorted) {
			if (sb.length() > 0) sb.append(", ");
			sb.append(e.getKey().name()).append("=").append(e.getValue());
		}
		System.out.printf("[deaths %s] %s%n", label, sb.toString());
	}

	private void plantHomeostasisTick() {
		int plantPop = environment.getCount(com.protoevo.biology.cells.PlantCell.class);
		if (plantPop <= 0) {
			// All plants gone: clear integral so when seedlings return the
			// world isn't already pressed all the way to no-photosynthesis.
			plantPidIntegral = 0f;
			plantPidLastError = Float.NaN;
			return;
		}

		float error = ((float) plantPop - homeostasisTargetPlants) / (float) homeostasisTargetPlants;
		plantPidIntegral = plantPidIntegral * (1f - PLANT_PID_INTEGRAL_LEAK)
				+ error * homeostasisInterval;

		float derivative = 0f;
		if (!Float.isNaN(plantPidLastError))
			derivative = (error - plantPidLastError) / homeostasisInterval;
		plantPidLastError = error;

		float control = PLANT_PID_KP * error + PLANT_PID_KI * plantPidIntegral + PLANT_PID_KD * derivative;

		Environment.settings.plant.photosynthesizeEnergyRate.set(
				HOMEO_BASE_PLANT_PHOTOSYNTHESIS * mul(GAIN_PLANT_PHOTOSYNTHESIS * control));
		Environment.settings.plant.constructionRate.set(
				HOMEO_BASE_PLANT_CONSTRUCTION * mul(GAIN_PLANT_CONSTRUCTION * control));
		Environment.settings.plant.maxPlantGrowth.set(
				HOMEO_BASE_PLANT_MAX_GROWTH * mul(GAIN_PLANT_GROWTH * control));
		Environment.settings.plant.minPlantGrowth.set(
				HOMEO_BASE_PLANT_MIN_GROWTH * mul(GAIN_PLANT_GROWTH * control));
		Environment.settings.plant.minHealthToSplit.set(
				HOMEO_BASE_PLANT_SPLIT_HEALTH * mul(GAIN_PLANT_SPLIT_HEALTH * control));
		Environment.settings.plant.splitRate.set(
				HOMEO_BASE_PLANT_SPLIT_RATE * mul(GAIN_PLANT_SPLIT_RATE * control));
	}

	/** Map a log-deviation into a positive multiplier, clamped so neither
	 *  end can produce 0 or runaway values. */
	private static float mul(float x) {
		if (x >  MAX_LOG_DEVIATION) x =  MAX_LOG_DEVIATION;
		if (x < -MAX_LOG_DEVIATION) x = -MAX_LOG_DEVIATION;
		return (float) Math.exp(x);
	}

	// ===== Selection-pressure ratchet =====
	//
	// Monotonic difficulty escalator separate from the PID. The PID keeps the
	// population near its target; the ratchet makes the *task* harder over
	// time so lineages can never coast. PhagocyticReceptor uses match^(1+p)
	// as digestion efficiency, where p is the ratcheted exponent — higher p
	// means partial signature matches digest worse and worse.
	//
	// Ratchet rules:
	//   * Only fires every RATCHET_INTERVAL sim-seconds (delay so a transient
	//     stable patch doesn't multiply pressure).
	//   * Population must be at least RATCHET_POP_FRACTION (80%) of target.
	//     A world stable at 400/500 still ratchets — only an actively
	//     crashing or much-under population is filtered out.
	//   * |derivative| of error must be small (don't tighten while population
	//     is changing fast either direction).
	//   * Only goes UP, never down. Cap at MAX_PRESSURE so it can't drive
	//     evolution to literal impossibility.
	// Bumped 60 → 240 sim-seconds. The 60-second cadence was firing 4
	// simultaneous tightening steps (exponent, plant MIN_RUN, protozoa
	// MIN_RUN, and base efficiency) every cycle. After 3-4 cycles
	// (~3 sim-minutes), engulfBaseEfficiency had dropped from 1.0 to
	// ~0.6, which is enough to choke off engulf feeding — cells without
	// well-evolved receptors stopped digesting effectively, switched to
	// chemical-drip-only survival, and gradually bled out from old age.
	// 240-second cadence (4 sim-minutes per step) gives ~30 sim-minutes
	// for the receptor system to fully tighten — long enough for several
	// reproductive cycles per step, so evolution can actually track
	// each new constraint before the next one lands.
	private static final float RATCHET_INTERVAL  = 240f;
	private static final float RATCHET_STEP      = 0.05f; // exponent step
	private static final float MAX_PRESSURE      = 3.0f;  // hard cap (match⁴ digest)
	private static final float STABLE_DERIV_LIMIT = 0.05f; // |dError/dt| below this counts as stable
	// Caps for the MIN_RUN ratchet — sequence lengths are 50 (plant) and 75
	// (protozoa), so caps at 30% and ~27% leave headroom even after
	// substantial signature drift. Going higher than this risks the
	// signature-drift rate outrunning the receptor-evolution rate and
	// crashing the population.
	private static final int   MAX_PLANT_MIN_RUN    = 15;
	// Cap raised from 20 → 35 so the ratchet still has climbing room
	// above the new PhagocyticReceptor PROTOZOA_MIN_RUN_FLOOR (24).
	// 35 of 75 = 47% of sequence — borderline impossible even with
	// strong evolutionary pressure; effectively a hard ceiling on the
	// cannibalism arms race.
	private static final int   MAX_PROTOZOA_MIN_RUN = 35;
	private float lastRatchetSimTime = -RATCHET_INTERVAL;  // -interval so first eligible tick fires

	// What fraction of the target population is "close enough to stable" to
	// count as ratchet-eligible? 0.8 = if the world settles at 80% of
	// target and stays there, that's a valid (if slightly undersized)
	// equilibrium — the ratchet still fires so the difficulty curve doesn't
	// stall just because the world isn't QUITE at the nominal target.
	// Previously this was 1.0 (strict >= target), which meant a world that
	// settled at 480/500 would never tighten.
	private static final float RATCHET_POP_FRACTION = 0.8f;

	private void maybeRatchetSelection() {
		float now = environment.getElapsedTime();
		if (now - lastRatchetSimTime < RATCHET_INTERVAL) return;

		int pop = environment.numberOfProtozoa();
		// Ratchet-eligibility threshold lowered from `pop >= target` to
		// `pop >= target × 0.8`. Combined with the stability check below,
		// this means "the world is stable at or close-to target", not the
		// strict "at-or-over target" that left a slightly-undersized
		// equilibrium permanently un-ratcheted. The pressure still scales
		// only with stability — a crashing population (pop falling fast)
		// is filtered out by the derivative check, regardless of absolute
		// pop level.
		if (pop < homeostasisTargetPop * RATCHET_POP_FRACTION) return;

		// Use the most recent derivative the PID just computed. NaN guard
		// because the very first homeostat tick has no history.
		float deriv = Float.isNaN(pidLastError) ? 0f
				: (((float) pop - homeostasisTargetPop) / homeostasisTargetPop - pidLastError)
						/ homeostasisInterval;
		if (Math.abs(deriv) > STABLE_DERIV_LIMIT) return; // changing fast — wait

		boolean stepped = false;

		// 1. Exponent ratchet (digestion-efficiency curve gets steeper).
		float current = Environment.settings.cell.selectionPressureExponent.get();
		if (current < MAX_PRESSURE) {
			float next = Math.min(MAX_PRESSURE, current + RATCHET_STEP);
			Environment.settings.cell.selectionPressureExponent.set(next);
			System.out.printf(
					"[ratchet] selectionPressureExponent %.3f -> %.3f  (digest = match^%.2f)%n",
					current, next, 1f + next);
			stepped = true;
		}

		// 2. MIN_RUN ratchet (engulf gate gets stricter). Independent of the
		//    exponent ratchet — both can fire on the same tick. Without this
		//    second axis, a stable population at high exponent + low MIN_RUN
		//    would coast on weak receptors that scrape past a tiny gate;
		//    raising the gate forces co-evolution of longer binding regions.
		int curPlantRun = Environment.settings.cell.plantEngulfMinRun.get();
		if (curPlantRun < MAX_PLANT_MIN_RUN) {
			int next = curPlantRun + 1;
			Environment.settings.cell.plantEngulfMinRun.set(next);
			System.out.printf(
					"[ratchet] plantEngulfMinRun %d -> %d (of 50)%n",
					curPlantRun, next);
			stepped = true;
		}
		int curProtoRun = Environment.settings.cell.protozoaEngulfMinRun.get();
		if (curProtoRun < MAX_PROTOZOA_MIN_RUN) {
			int next = curProtoRun + 1;
			Environment.settings.cell.protozoaEngulfMinRun.set(next);
			System.out.printf(
					"[ratchet] protozoaEngulfMinRun %d -> %d (of 75)%n",
					curProtoRun, next);
			stepped = true;
		}

		// 3. Engulf base efficiency ratchet (digestion floor gets lower).
		//    Drops from 1.0 (receptor system off — every engulf is full
		//    efficiency) toward 0.02 (the original hard floor — only good
		//    matches digest well). Geometric decay: each step multiplies
		//    by 0.85 so the floor halves every ~4 ratchet ticks (~4 min
		//    sim time at the default RATCHET_INTERVAL=60). Lineages have
		//    plenty of generations to evolve specific receptors before
		//    the floor approaches the hard minimum.
		float curBase = Environment.settings.cell.engulfBaseEfficiency.get();
		if (curBase > 0.02f) {
			float next = Math.max(0.02f, curBase * 0.85f);
			Environment.settings.cell.engulfBaseEfficiency.set(next);
			System.out.printf(
					"[ratchet] engulfBaseEfficiency %.3f -> %.3f%n",
					curBase, next);
			stepped = true;
		}

		if (stepped)
			lastRatchetSimTime = now;
	}

	// ===== Auto-respawn on extinction =====
	//
	// When all protozoa die, spawn a fresh seeded cohort so the sim keeps
	// running unattended. New cells get a plantReceptorKey copied from
	// some live plant's surface signature → they can immediately eat that
	// plant lineage at full rate. Their protozoa receiving / phagocytic
	// receptors are fresh independent random sequences (so they can't
	// kin-cannibalize, same logic as initialisePopulation).
	//
	// If there are no live plants either, we don't respawn — that would
	// just spawn cells with no food. The sim continues running empty
	// and the user can fix it manually.
	private static final int RESPAWN_PROTOZOA_COUNT = 250;

	private void respawnProtozoaIfExtinct() {
		if (environment == null) return;
		if (environment.numberOfProtozoa() > 0) return;

		// Find a representative live plant to align the new protozoa keys to.
		com.protoevo.biology.cells.PlantCell anchor = null;
		for (com.protoevo.biology.cells.Cell c : new java.util.ArrayList<>(environment.getCells())) {
			if (c instanceof com.protoevo.biology.cells.PlantCell && !c.isDead()) {
				anchor = (com.protoevo.biology.cells.PlantCell) c;
				break;
			}
		}
		com.protoevo.biology.evolution.AminoAcidSequence anchorSig =
				anchor == null ? null : anchor.getSurfaceSignature();
		if (anchorSig == null) {
			System.out.println("[respawn] no live plants found; skipping respawn so we don't spawn starvers.");
			return;
		}

		// Fresh independent keys for the protozoa-on-protozoa side so the
		// new cohort can't immediately kin-cannibalize.
		com.protoevo.biology.evolution.AminoAcidSequence freshReceiving =
				new com.protoevo.biology.evolution.AminoAcidSequence(
						com.protoevo.biology.evolution.ProtozoaSignatureTrait.LENGTH);
		com.protoevo.biology.evolution.AminoAcidSequence freshPhag =
				new com.protoevo.biology.evolution.AminoAcidSequence(
						com.protoevo.biology.evolution.ProtozoaSignatureTrait.LENGTH);

		int target = Math.min(RESPAWN_PROTOZOA_COUNT,
				environment.getGlobalCapacity(com.protoevo.biology.cells.Protozoan.class));
		int spawned = 0;
		for (int i = 0; i < target; i++) {
			try {
				com.protoevo.biology.cells.Protozoan p =
						com.protoevo.biology.evolution.Evolvable.createNew(
								com.protoevo.biology.cells.Protozoan.class);
				p.setPlantReceptorKey(
						new com.protoevo.biology.evolution.AminoAcidSequence(anchorSig));
				p.setProtozoaReceivingReceptor(
						new com.protoevo.biology.evolution.AminoAcidSequence(freshReceiving));
				p.setProtozoaPhagocyticReceptor(
						new com.protoevo.biology.evolution.AminoAcidSequence(freshPhag));
				p.setEnvironmentAndBuildPhysics(environment);
				environment.findRandomPositionOrKillCell(p);
				spawned++;
			} catch (Throwable t) {
				// If anything goes wrong creating a cell, skip it; we still
				// want to spawn as many as possible.
			}
		}
		System.out.printf("[respawn] extinction detected — spawned %d new protozoa, keys seeded against plant lineage with sig %s%n",
				spawned, anchorSig.toString());
	}

	public boolean isHomeostasisEnabled() { return homeostasisEnabled; }
	public void setHomeostasisEnabled(boolean enabled) {
		homeostasisEnabled = enabled;
		if (!enabled) {
			// Restore baselines AND reset PID state so re-enabling later
			// starts from a clean slate, not the integral we'd built up.
			Environment.settings.cell.energyDecayRate.set(HOMEO_BASE_DECAY);
			Environment.settings.plantEnergyDensity.set(HOMEO_BASE_PLANT_ED);
			Environment.settings.meatEnergyDensity.set(HOMEO_BASE_MEAT_ED);
			Environment.settings.cell.startingAvailableCellEnergy.set(HOMEO_BASE_START_E);
			Environment.settings.cell.chemicalExtractionFactor.set(HOMEO_BASE_CHEM_EXTRACT);
			Environment.settings.plant.photosynthesizeEnergyRate.set(HOMEO_BASE_PLANT_PHOTOSYNTHESIS);
			Environment.settings.plant.constructionRate.set(HOMEO_BASE_PLANT_CONSTRUCTION);
			Environment.settings.plant.maxPlantGrowth.set(HOMEO_BASE_PLANT_MAX_GROWTH);
			Environment.settings.plant.minPlantGrowth.set(HOMEO_BASE_PLANT_MIN_GROWTH);
			Environment.settings.plant.minHealthToSplit.set(HOMEO_BASE_PLANT_SPLIT_HEALTH);
			Environment.settings.plant.splitRate.set(HOMEO_BASE_PLANT_SPLIT_RATE);
			pidIntegral = 0f;
			pidLastError = Float.NaN;
			plantPidIntegral = 0f;
			plantPidLastError = Float.NaN;
			pidLastLogMs = 0;
		}
		System.out.println("Homeostat: " + (enabled ? "ON" : "OFF (defaults restored)"));
	}
	public int getHomeostasisTargetPop() { return homeostasisTargetPop; }
	public void setHomeostasisTargetPop(int target) {
		homeostasisTargetPop = Math.max(10, target);
		// Reset integral so the controller doesn't carry over windup from the
		// old setpoint into the new one — common cause of big initial swings
		// after a target change.
		pidIntegral = 0f;
		pidLastError = Float.NaN;
		System.out.println("Homeostat target population: " + homeostasisTargetPop);
	}
	public int getHomeostasisTargetPlants() { return homeostasisTargetPlants; }
	public void setHomeostasisTargetPlants(int target) {
		homeostasisTargetPlants = Math.max(10, target);
		plantPidIntegral = 0f;
		plantPidLastError = Float.NaN;
		System.out.println("Homeostat target plants: " + homeostasisTargetPlants);
	}
	// ===== end Homeostatic controller =====

	public void cancelPreparation() {}

	public void run() {
		while (simulate) {
			if (paused) {
				// Sleep briefly instead of busy-waiting. The original tight
				// `if (paused) continue;` pinned a full CPU core whenever the
				// sim was paused, which starved the render thread enough that
				// opening a heavy modal screen could make Windows mark the
				// LWJGL window "not responding".
				try {
					Thread.sleep(20);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
				continue;
			}

			update();

			if (isFinished()) {
				simulate = false;
				System.out.println();
				System.out.println("Finished simulation. All protozoa died.");
				printStats();
			}
		}
	}

	public boolean isFinished() {
		return environment.hasStarted() && environment.numberOfProtozoa() <= 0
				&& Environment.settings.finishOnProtozoaExtinction.get();
	}

	public void requestSave() {
		saveRequested = true;
	}

	public void printStats() {
		environment.getStats().forEach(
			stat -> System.out.println(stat.toString())
		);
	}

	public void update()
	{
		if (isPaused() || busyOnOtherThread || environment == null)
			return;

		try {
			// Sim-time advancement model:
			//   one render call advances sim time by `timeDilation` × baseDt.
			// At low td we run the inner loop with dt=baseDt (small, stable).
			// At high td we'd otherwise call physics+cell update hundreds of
			// times per frame; instead we use BIGGER physics steps capped at a
			// stability ceiling that grows with td, so 128× ends up doing
			// ~8 medium steps per render frame instead of 128 tiny ones. Cell
			// logic is linear in dt so batching is safe; physics has loads of
			// headroom because max cell speed is ~0.05 units/s (terminal
			// velocity under fluid damping 10 with cilia thrust 0.0005); even
			// dt=0.05 won't tunnel a 0.02-radius cell. Chemicals need the
			// per-pixel cap in protozoanIO (already in place) to be safe.
			float baseDt = Environment.settings.simulationUpdateDelta.get();
			float td = Math.max(0f, timeDilation);
			float totalDt = td * baseDt;

			// Stability ceiling scales with time dilation so the per-render
			// step count stays small even at very high td. Max cell speed is
			// ~0.05 units/s, min radius ~0.02, so even dt=0.1s only moves a
			// cell 1/4 of its diameter per step — Box2D handles this fine.
			// At td=1×: stepDt = 8×baseDt = 0.008 (1 step per render)
			// At td=32: stepDt = 8×baseDt → 4 steps per render
			// At td=128: stepDt = 32×baseDt = 0.032 → 4 steps per render
			// At td=256: stepDt = 64×baseDt = 0.064 → 4 steps per render
			float maxStableStep = baseDt * Math.min(64f, Math.max(8f, td / 4f));
			// Per-render-frame cap on the number of big steps. Above this the
			// sim just won't keep up — better to run slow than hang.
			int maxStepsPerFrame = 128;

			try {
				// Chemicals AND plant/meat updates are batched once per render
				// frame. Both are expensive (chem deposit is a parallel stream
				// over all cells painting their footprint; plant cell updates
				// are 1500× per pass and mostly do linear-in-delta work) and
				// neither benefits from sub-frame granularity. Per-pixel
				// extraction cap in protozoanIO makes the chem batching safe;
				// plant/meat batching is safe because their update is linear
				// in delta and Environment accumulates skipped dt internally.
				// Protozoa still update every step — they're the active
				// agents and need sub-frame collision/eating fidelity.
				float remaining = totalDt;
				float pendingChem = 0f;
				int stepsThisFrame = 0;
				while (remaining > 1e-9f && stepsThisFrame < maxStepsPerFrame) {
					float stepDt = Math.min(remaining, maxStableStep);
					pendingChem += stepDt;
					boolean isLastStep =
							(remaining - stepDt) <= 1e-9f
							|| (stepsThisFrame + 1) >= maxStepsPerFrame;
					float chemDt = isLastStep ? pendingChem : 0f;
					environment.update(stepDt, chemDt, isLastStep);
					timedEventsManager.update(stepDt);
					if (isLastStep)
						pendingChem = 0f;
					remaining -= stepDt;
					stepsThisFrame++;
				}
			} catch (Exception e) {
				writeCrashReport(e);
				e.printStackTrace();
				System.out.println("Error occurred during simulation. Saving and exiting.");
				save();
				repl.close();
				throw e;
			}

		} catch (Exception e) {
			writeCrashReport(e);
			System.exit(0);
		}
	}

	public void writeCrashReport(Exception e) {
		String crashFolder = getSaveFolder() + "/crash_" + Utils.getTimeStampString();
		try {
			Files.createDirectories(Paths.get(crashFolder));
			FileWriter fileWriter = new FileWriter(crashFolder + "/report.txt");
			PrintWriter printWriter = new PrintWriter(fileWriter);
			printWriter.println("Timestamp: " + new Date());
			printWriter.println("Application version: " + ApplicationManager.APPLICATION_VERSION);
			printWriter.println("Operating system: " + System.getProperty("os.name"));
			printWriter.println("Operating system version: " + System.getProperty("os.version"));
			printWriter.println("Java version: " + System.getProperty("java.version"));
			Runtime runtime = Runtime.getRuntime();
			printWriter.println("Free memory: " + runtime.freeMemory());
			printWriter.println("Max memory: " + runtime.maxMemory());
			printWriter.println("Total memory: " + runtime.totalMemory());
			printWriter.println("Processors: " + runtime.availableProcessors());
			printWriter.println();
			printWriter.println("Stack Trace:");
			e.printStackTrace(printWriter);
			printWriter.close();
			System.out.println("Wrote crash report to: " + crashFolder);
		} catch (IOException ioException) {
			System.err.println("Failed to create crash report: " + ioException + "\nWhen handling exception: " + e);
		}
	}

	public void saveOnOtherThread() {
		onOtherThread(this::save);
	}

	public void onOtherThread(Runnable runnable) {
		if (busyOnOtherThread)
			return;
		busyOnOtherThread = true;
		new Thread(() -> {
			runnable.run();
			busyOnOtherThread = false;
		}).start();
	}

	public boolean isBusyOnOtherThread() {
		return busyOnOtherThread;
	}

	public void interruptSimulationLoop() {
		simulate = false;
	}

	public void close() {
		simulate = false;
		System.out.println("\nClosing simulation.");
		String saveFile = save();
		System.out.println("Saved environment to: " + saveFile);
		repl.close();
	}

	public void dispose() {
		if (environment != null) {
			environment.dispose();
			environment = null;
		}
	}

	public String save() {
		if (environment == null)
			return null;

		EnvironmentImageRenderer renderer = new EnvironmentImageRenderer(1024, 1024, environment);
		renderer.render(getSaveFolder() + "/screenshots");
		System.out.println("Created screenshot in directory: " + getSaveFolder() + "/screenshots");

		String timeStamp = Utils.getTimeStampString();
		String fileName = getSaveFolder() + "/env/" + timeStamp;
		Serialization.saveEnvironment(environment, fileName);

		// Dump a human/AI-readable report on whatever lineage is currently
		// dominant. Lets us inspect what evolution actually built without
		// having to deserialize the binary env file.
		try {
			com.protoevo.core.DominantLineageReport.write(
					environment, getSaveFolder() + "/dominant_lineage.txt");
		} catch (Throwable t) {
			System.out.println("[dominant-lineage] failed: " + t.getMessage());
		}

		return fileName;
	}

	public void createAutoSave() {
		if (environment == null)
			return;

		EnvironmentImageRenderer renderer = new EnvironmentImageRenderer(1024, 1024, environment);
		renderer.render(getSaveFolder() + "/screenshots");
		System.out.println("Created screenshot in directory: " + getSaveFolder() + "/screenshots");

		String fileName = getSaveFolder() + "/env/autosave";
		Serialization.saveEnvironment(environment, fileName);
	}

	public void makeStatisticsSnapshot() {
		history.makeStatisticsSnapshot(environment);
//		Statistics stats = new Statistics(environment.getStats());
//		stats.putAll(environment.getDebugStats());
//		stats.putAll(environment.getPhysicsDebugStats());
//		stats.putAll(environment.getProtozoaSummaryStats(true, false, true));
//
//		String timeStamp = Utils.getTimeStampString();
//
//		FileIO.writeJson(stats, getSaveFolder() + "/stats/summaries/" + timeStamp);
//
//		if (Environment.settings.misc.writeGenomes.get()) {
//			List<NetworkGenome> protozoaGenomes = environment.getCells().stream()
//					.filter(cell -> cell instanceof Protozoan)
//					.map(cell -> ((Protozoan) cell).getGeneExpressionFunction().getGRNGenome())
//					.collect(Collectors.toList());
//			FileIO.writeJson(protozoaGenomes, getSaveFolder() + "/stats/protozoa-genomes/" + timeStamp);
//		}

//		PythonRunner.runPython("pyprotoevo.create_plots", "--quiet --simulation " + name);
	}

	public SimulationHistory getHistory() {
		return history;
	}

	public void toggleDebug() {
		debug = !debug;
	}

	public void togglePause() {
		paused = !paused;
	}

	public void setPaused(boolean paused) {
		Simulation.paused = paused;
	}

	public boolean inDebugMode() {
		return debug;
	}

	public Environment getEnv() { return environment; }

	public float getElapsedTime() { return environment.getElapsedTime(); }

	public float getTimeDilation() { return timeDilation; }

	public void setTimeDilation(float td) { timeDilation = td; }

    public static boolean isPaused() {
		return paused;
    }

	public boolean isReady() {
		return initialised;
	}

	public String getSaveFolder() {
		return FileIO.getSavesDir() + "/" + name;
	}

	public void openSaveFolderOnDesktop() {
		try {
			Desktop.getDesktop().open(new File(getSaveFolder()));
		} catch (IOException e) {
			System.out.println("\nFailed to open folder: " + e.getMessage() + "\n");
		}
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void toggleTimeDilation() {
		if (timeDilation <= 1f)
			timeDilation = 2f;
		else if (timeDilation <= 2f)
			timeDilation = 5f;
//		else if (timeDilation <= 5f)
//			timeDilation = 10f;
		else
			timeDilation = 1f;
	}

	public String getLoadingStatus() {
		if (initialised)
			return "Ready to Simulate";
		else if (environment == null)
			return "Creating Environment";
		else
			return environment.getLoadingStatus();
	}
}
