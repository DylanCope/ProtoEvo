package com.protoevo.env;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Streams;
import com.protoevo.biology.BurstRequest;
import com.protoevo.biology.CauseOfDeath;
import com.protoevo.biology.cells.Cell;
import com.protoevo.biology.cells.MeatCell;
import com.protoevo.biology.cells.PlantCell;
import com.protoevo.biology.cells.Protozoan;
import com.protoevo.biology.evolution.Evolvable;
import com.protoevo.biology.nodes.NodeAttachment;
import com.protoevo.core.Statistics;
import com.protoevo.maths.Shape;
import com.protoevo.physics.*;
import com.protoevo.physics.box2d.Box2DPhysics;
import com.protoevo.settings.SimulationSettings;
import com.protoevo.maths.Geometry;
import com.protoevo.utils.SerializableFunction;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;


public class Environment implements Serializable
{
	private static final long serialVersionUID = 2804817237950199223L;
	private static final GetCellFunction getParticleFn = new GetCellFunction();
	public static final CellIsProtozoaPredicate isCellProtozoa = new CellIsProtozoaPredicate();
	public static SimulationSettings settings = SimulationSettings.createDefault();

	private final SimulationSettings mySettings;
	private String simulationName;
	private final Physics physics;
	private String loadingStatus;
	private final Statistics stats = new Statistics();
	private final Statistics debugStats = new Statistics();
	public final ConcurrentHashMap<CauseOfDeath, Integer> causeOfDeathCounts =
			new ConcurrentHashMap<>(CauseOfDeath.values().length, 1);

	private transient Chunks chunks;

	private Map<Class<? extends Cell>, SerializableFunction<Float, Vector2>> spawnPositionFns;

	private final ChemicalSolution chemicalSolution;
	private final LightManager light;
	private final TimeManager timeManager;
	private transient CurrentField currentField;
	private transient Vector2 currentForce;
	// Monotonically increasing lineage id counter. Each new cell that isn't
	// a descendant of an existing one (initial spawn, manual injection)
	// gets the next value; burst children inherit their parent's lineage id.
	private long nextLineageId = 1L;
	// Phylogeny records — keyed by cell id, persists past death so the
	// tree view can walk parents. Bounded by `pruneDeadLineages()` which
	// runs periodically. Public-ish via getLineageRecords() for the UI.
	private final java.util.HashMap<Long, LineageRecord> lineageRecords =
			new java.util.HashMap<>();
	private final List<Rock> rocks = new ArrayList<>();
	private final HashMap<Class<? extends Cell>, Long> bornCounts = new HashMap<>(3);
	private final HashMap<Class<? extends Cell>, Long> generationCounts = new HashMap<>(3);
	private final static HashMap<Class<? extends Cell>, String> cellClassNames = new HashMap<>(3);
	static {
		cellClassNames.put(Protozoan.class, "Protozoa");
		cellClassNames.put(PlantCell.class, "Plants");
		cellClassNames.put(MeatCell.class, "Meat");
	}
	private volatile long crossoverEvents = 0;

	@JsonIgnore
	private transient Set<Cell> cellsToAdd;
	private final ConcurrentHashMap<Long, Cell> cells = new ConcurrentHashMap<>();
	private boolean hasInitialised, hasStarted;
	private Vector2[] populationStartCentres;
	@JsonIgnore
	private final ConcurrentHashMap<Cell, BurstRequest<? extends Cell>> burstRequests = new ConcurrentHashMap<>();
	private final Collection<Cell> handledBurstRequests = new ConcurrentLinkedQueue<>();
	private final CellDeadPredicate isDeadPredicate = new CellDeadPredicate();
	// Per-frame consumer for the parallel cell-update pass. Reused so each
	// update doesn't allocate a fresh lambda holder; only the `delta` field
	// changes per call.
	private transient CellUpdateConsumer cellUpdateConsumer;
	// Plant + meat delta accumulator. Their update() is skipped on
	// intermediate substeps at high time dilation, and the accumulated dt
	// is applied on the next "plants update" pass so the linear-in-delta
	// terms (photosynthesis, growth, decay) integrate correctly. Non-linear
	// edge events (plant-protozoa collision damage pulses) are sampled at
	// the protozoa-pass cadence instead, which is fine because protozoa
	// always update every step.
	private transient float plantPendingDt = 0f;

	public Environment()
	{
		this(settings);
	}

	public Environment(SimulationSettings settings) {
		mySettings = settings;
		Environment.settings = settings;

		hasStarted = false;
		createTransientObjects();
		physics = new Box2DPhysics();

		System.out.println("Creating chemicals solution... ");
		if (Environment.settings.enableChemicalField.get()) {
			chemicalSolution = new ChemicalSolution(
					this,
					Environment.settings.worldgen.chemicalFieldResolution.get(),
					Environment.settings.worldgen.chemicalFieldRadius.get());
		} else {
			chemicalSolution = null;
		}

		timeManager = new TimeManager();

		int lightDim = Environment.settings.worldgen.lightMapResolution.get();
		light = new LightManager(lightDim, lightDim, Environment.settings.worldgen.radius.get());
		light.setTimeManager(timeManager);

		hasInitialised = false;
		NodeAttachment.setupPossibleAttachments();
	}

	public void createTransientObjects() {
		// Concurrent set — parallel cell.update() workers can call
		// registerToAdd() (e.g. burst-children path). Plain HashSet under
		// concurrent writes corrupts its internal hash table — silent
		// lost adds, NPEs deep in HashMap, intermittent crashes at high
		// population. ConcurrentHashMap-backed key set has the same
		// semantics for our usage and is lock-free for reads.
		cellsToAdd = java.util.concurrent.ConcurrentHashMap.newKeySet();
		chunks = new Chunks();
		chunks.initialise();
		forceChunkRebuild();
		rebuildCurrentField();
	}

	private void rebuildCurrentField() {
		// Defensive defaults: a save from before these params existed will
		// deserialize EnvironmentSettings without them, and even with
		// CompatibleFieldSerializer the field stays at its constructor
		// default value (which only runs if Kryo invokes the constructor;
		// not all instantiator strategies do).
		float strength = (settings.env != null && settings.env.currentStrength != null)
				? settings.env.currentStrength.get() : 1.5e-5f;
		float scale = (settings.env != null && settings.env.currentSpatialScale != null)
				? settings.env.currentSpatialScale.get() : 4f;
		float rate = (settings.env != null && settings.env.currentTimeRate != null)
				? settings.env.currentTimeRate.get() : 0.05f;
		currentField = new CurrentField(strength, scale, rate);
		currentForce = new Vector2();
	}

	private void applyCurrents() {
		if (currentField == null)
			rebuildCurrentField();
		float intensity = settings.env.currentStrength.get();
		if (intensity <= 0f)
			return;
		// Keep CurrentField in sync with the settings so a live tweak via
		// REPL or UI reflects immediately. Cheap — three field copies.
		currentField.setIntensity(intensity);
		float t = timeManager.getTimeElapsed();
		for (Cell cell : getCells()) {
			Vector2 p = cell.getPos();
			currentField.sample(p.x, p.y, t, currentForce);
			cell.getParticle().applyForce(currentForce);
		}
	}

	public boolean hasStarted() {
		return hasStarted;
	}

	public void rebuildWorld() {
		settings = mySettings;
		NodeAttachment.setupPossibleAttachments();
		physics.rebuildTransientFields(this);
		for (Cell cell : getCells())
			cell.setEnvironment(this);
		forceChunkRebuild();
	}

	public void update(float delta)
	{
		update(delta, delta);
	}

	/**
	 * Split-delta version for fast-forward substepping.
	 *
	 * @param physicsDelta   delta used for the physics step + per-cell update.
	 *                       MUST be the small per-substep dt — running physics
	 *                       with a giant accumulated delta produces unstable
	 *                       behavior (cells tunneling through rocks, etc.).
	 * @param chemicalsDelta total accumulated delta for the chemical pass.
	 *                       Pass 0 to skip chemicals this call entirely; pass
	 *                       the same value as physicsDelta on the last substep
	 *                       to make the deposit reflect the time we *actually*
	 *                       advanced. The previous version skipped deposits on
	 *                       intermediate substeps, which made plants emit ~1/N
	 *                       as much chemical at N× speed — protozoa lost the
	 *                       gradient they navigate by, and populations starved.
	 */
	public void update(float physicsDelta, float chemicalsDelta)
	{
		update(physicsDelta, chemicalsDelta, true);
	}

	/**
	 * @param plantsAndMeatThisStep if true, plant + meat cells run their full
	 *   update this step. Otherwise only protozoa update — used by the fast-
	 *   forward scheduler so we don't pay for 1500 plant updates on every
	 *   substep when the per-render budget only fits one or two passes.
	 *   Plants get a fat-delta update once per render frame instead. Since
	 *   plant logic is linear in delta (photosynthesis, growth, decay) this
	 *   integrates correctly; the only thing that loses fidelity is the
	 *   plant–protozoa contact-death pulse, which evens out over many frames.
	 */
	public void update(float physicsDelta, float chemicalsDelta,
	                   boolean plantsAndMeatThisStep)
	{
		hasStarted = true;
		settings = mySettings;
		// Watchdog: time each sub-step. If anything exceeds the threshold,
		// log it so we can see what's stalling the sim when it feels
		// frozen. Threshold is 500ms — at 60fps GUI mode, anything over
		// ~16ms drops frames, but 500ms is the threshold where the user
		// would actually perceive the sim as "stuck". Numbers add up to
		// total per-step time so the user can see which subsystem is hot.
		long t0 = System.nanoTime();
		for (Cell cell : getCells())
			cell.getParticle().physicsUpdate();
		long t1 = System.nanoTime();

		timeManager.update(physicsDelta);
		light.update(physicsDelta);
		long t2 = System.nanoTime();

		applyCurrents();
		long t3 = System.nanoTime();

		if (physicsDelta > 0f)
			physics.step(physicsDelta);
		long t4 = System.nanoTime();

  		handleCellUpdates(physicsDelta, plantsAndMeatThisStep);
		long t5 = System.nanoTime();

		handleBirthsAndDeaths();
		long t6 = System.nanoTime();

		updateChunkAllocations(physicsDelta);
		pruneDeadLineages(physicsDelta);
		long t7 = System.nanoTime();

		physics.getJointsManager().flushJoints();
		long t8 = System.nanoTime();

		if (chemicalsDelta > 0f && Environment.settings.enableChemicalField.get()) {
			chemicalSolution.update(chemicalsDelta);
		}
		long t9 = System.nanoTime();

		long totalMs = (t9 - t0) / 1_000_000L;
		if (totalMs > 500) {
			System.out.printf(
					"[watchdog] slow tick %dms  physUpd=%dms  light=%dms  currents=%dms  "
					+ "physStep=%dms  cellUpd=%dms  births=%dms  chunks=%dms  joints=%dms  chem=%dms  "
					+ "cells=%d%n",
					totalMs,
					(t1 - t0) / 1_000_000L,
					(t2 - t1) / 1_000_000L,
					(t3 - t2) / 1_000_000L,
					(t4 - t3) / 1_000_000L,
					(t5 - t4) / 1_000_000L,
					(t6 - t5) / 1_000_000L,
					(t7 - t6) / 1_000_000L,
					(t8 - t7) / 1_000_000L,
					(t9 - t8) / 1_000_000L,
					cells.size());
		}
	}


	public void ensureAddedToEnvironment(Cell cell) {
		if (!cells.containsKey(cell.getId()))
			registerToAdd(cell);
	}

	private void handleCellUpdates(float delta, boolean updatePlantsAndMeat) {
		if (cellUpdateConsumer == null)
			cellUpdateConsumer = new CellUpdateConsumer(delta);
		cellUpdateConsumer.delta = delta;
		// Plants/meat get caught-up delta on the steps where they actually run.
		// On skipped steps we accumulate; on running steps they receive
		// (current delta) + (sum of skipped deltas).
		if (updatePlantsAndMeat) {
			cellUpdateConsumer.plantDelta = delta + plantPendingDt;
			plantPendingDt = 0f;
		} else {
			plantPendingDt += delta;
		}
		cellUpdateConsumer.updatePlantsAndMeat = updatePlantsAndMeat;
		getCells().parallelStream().forEach(cellUpdateConsumer);
	}

	private void handleBirthsAndDeaths() {
		handledBurstRequests.clear();
		// Per-tick cap on burst processing. With engulf efficiency at 1.0,
		// the entire population can hit splitRadius+health threshold in
		// the same tick — leading to 500 simultaneous bursts, each creating
		// 2-6 children with Box2D body construction, particle init, GRN
		// cloning, lineage walks, and chunk allocation. That single tick
		// can take seconds in real time, and the user experiences it as a
		// freeze. Cap at 64/tick: any extra requests just sit in the queue
		// for next tick.
		int burstBudget = 64;
		for (Cell parent : burstRequests.keySet()) {
			if (burstBudget <= 0) break;
			BurstRequest<? extends Cell> burstRequest = burstRequests.get(parent);
			if (hasBurstCapacity(parent, burstRequest.getCellType()) && burstRequest.canBurst()) {
				burstRequest.burst();
				handledBurstRequests.add(parent);
				burstBudget--;
			}
		}
		for (Cell parent : handledBurstRequests)
			burstRequests.remove(parent);
		handledBurstRequests.clear();

		flushEntitiesToAdd();

		// Single pass: dispose+deposit+remove for any dead cell. The previous
		// version iterated the map twice (once to dispose/deposit, once to
		// removeIf) — at large populations that's a measurable repeat scan.
		// We also pull the cell out of `chunks` here so the spatial index
		// stays consistent without needing a full clear-and-rebuild on every
		// frame (see updateChunkAllocations comment).
		getCells().removeIf(cell -> {
			if (!cell.isDead())
				return false;
			dispose(cell);
			depositOnDeath(cell);
			chunks.remove(cell);
			recordDeath(cell);
			return true;
		});
	}

	public void createRocks() {
		System.out.println("Creating rocks structures...");
		rocks.addAll(WorldGeneration.generate());
		physics.registerStaticBodies(this);
	}

	public void initialise() {
		System.out.println("Commencing world generation... ");
		loadingStatus = "Generating World";
		createRocks();
		loadingStatus = "Creating Light";
		System.out.println("Baking shadows... ");
		if (settings.worldgen.bakeRockLights.get())
			LightManager.bakeRockShadows(light, rocks);
		if (settings.worldgen.generateLightNoiseTexture.get())
			light.generateNoiseLight(0);

		loadingStatus = "Creating Population";
		initialisePopulation();

		flushEntitiesToAdd();

		if (chemicalSolution != null) {
			loadingStatus = "Creating Chemicals";
			chemicalSolution.initialise();
		}

		loadingStatus = "Initialisation Complete";
		hasInitialised = true;
		System.out.println("Environment initialisation complete.");
	}

	public boolean hasBeenInitialised() {
		return hasInitialised;
	}

	private void buildSpawners() {
		spawnPositionFns = new HashMap<>(3, 1);
		if (populationStartCentres != null) {
			final float clusterR = Environment.settings.worldgen.populationClusterRadius.get();
			spawnPositionFns.put(PlantCell.class,
					new SpawnPlantInClustersFn(this, populationStartCentres, clusterR));
			spawnPositionFns.put(Protozoan.class,
					new SpawnProtozoaInClustersFn(this, populationStartCentres, clusterR));
		}
		else {
			spawnPositionFns.put(PlantCell.class, new SpawnInRandomPositionFn(this));
			spawnPositionFns.put(Protozoan.class, new SpawnInRandomPositionFn(this));
		}
	}

	public void initialisePopulation() {
		populationStartCentres = new Vector2[Environment.settings.worldgen.numPopulationStartClusters.get()];
		final float clusterR = Environment.settings.worldgen.populationClusterRadius.get();
		for (int i = 0; i < populationStartCentres.length; i++)
			populationStartCentres[i] = Geometry.randomPointInCircle(
					Environment.settings.worldgen.radius.get() - clusterR, WorldGeneration.RANDOM
			);

		buildSpawners();

		// Initial-viability seed.
		//
		//   initialPlantSig (50): every plant gets this; every protozoa's
		//     plantReceptorKey is set to a copy → guaranteed full-rate
		//     plant feeding from frame 1.
		//
		//   initialReceiving / initialPhagocytic (75 each): TWO INDEPENDENT
		//     random sequences. Every protozoa gets these two as its
		//     receiving and phagocytic receptors. Because they're
		//     independent random 75-mers, expected identity ≈ 5% << the
		//     15% engulf threshold → no cell can eat any other at t=0
		//     (no kin cannibalism on a fresh world). Mutation will drift
		//     lineages apart; predator-prey relationships only emerge
		//     when some lineage's phagocytic happens to drift toward
		//     another lineage's receiving — a real evolutionary event,
		//     not a freebie.
		com.protoevo.biology.evolution.AminoAcidSequence initialPlantSig =
				new com.protoevo.biology.evolution.AminoAcidSequence(
						com.protoevo.biology.evolution.PlantSignatureTrait.LENGTH);
		com.protoevo.biology.evolution.AminoAcidSequence initialReceiving =
				new com.protoevo.biology.evolution.AminoAcidSequence(
						com.protoevo.biology.evolution.ProtozoaSignatureTrait.LENGTH);
		com.protoevo.biology.evolution.AminoAcidSequence initialPhagocytic =
				new com.protoevo.biology.evolution.AminoAcidSequence(
						com.protoevo.biology.evolution.ProtozoaSignatureTrait.LENGTH);

		int nPlants = Environment.settings.worldgen.numInitialPlantPellets.get();
		nPlants = Math.min(nPlants, chunks.getGlobalCapacity(PlantCell.class));
		System.out.println("Creating population of " + nPlants + " plants..." );
		loadingStatus = "Seeding Plants";
		for (int i = 0; i < nPlants; i++) {
			PlantCell cell;
			if (Environment.settings.plant.evolutionEnabled.get())
				cell = Evolvable.createNew(PlantCell.class);
			else
				cell = new PlantCell();
			// Override the randomized signature with the shared seed so all
			// initial plants are recognizable by the seeded protozoa.
			cell.setSurfaceSignature(
					new com.protoevo.biology.evolution.AminoAcidSequence(initialPlantSig));
			cell.setEnvironmentAndBuildPhysics(this);
			findRandomPositionOrKillCell(cell);
		}

		int nProtozoa = Environment.settings.worldgen.numInitialProtozoa.get();
		nProtozoa = Math.min(nProtozoa, chunks.getGlobalCapacity(Protozoan.class));
		System.out.println("Creating population of " + nProtozoa + " protozoa...");
		loadingStatus = "Spawning Protozoa";
		for (int i = 0; i < nProtozoa; i++) {
			Protozoan p = Evolvable.createNew(Protozoan.class);
			p.setPlantReceptorKey(
					new com.protoevo.biology.evolution.AminoAcidSequence(initialPlantSig));
			p.setProtozoaReceivingReceptor(
					new com.protoevo.biology.evolution.AminoAcidSequence(initialReceiving));
			p.setProtozoaPhagocyticReceptor(
					new com.protoevo.biology.evolution.AminoAcidSequence(initialPhagocytic));
			p.setEnvironmentAndBuildPhysics(this);
			findRandomPositionOrKillCell(p);
		}

		for (Cell cell : cellsToAdd)
			cell.getParticle().applyImpulse(Geometry.randomVector(.01f));
	}

	public void findRandomPositionOrKillCell(Cell cell) {
		Vector2 pos = getRandomPosition(cell);
		if (pos == null) {
			cell.kill(CauseOfDeath.FAILED_TO_CONSTRUCT);
			return;
		}
		cell.getParticle().setPos(pos);
	}

	public Vector2 randomPosition(float entityRadius, Vector2[] clusterCentres) {
		int clusterIdx = MathUtils.random(clusterCentres.length - 1);
		Vector2 clusterCentre = clusterCentres[clusterIdx];
		return randomPosition(entityRadius, clusterCentre, Environment.settings.worldgen.populationClusterRadius.get());
	}

	public Vector2 randomPosition(float entityRadius, Vector2[] clusterCentres, float clusterRadius) {
		int clusterIdx = MathUtils.random(clusterCentres.length - 1);
		Vector2 clusterCentre = clusterCentres[clusterIdx];
		return randomPosition(entityRadius, clusterCentre, clusterRadius);
	}

	public Vector2 randomPosition(float entityRadius, Vector2 centre, float clusterRadius) {
		for (int i = 0; i < 20; i++) {
			float r = WorldGeneration.RANDOM.nextFloat() * clusterRadius;
			Vector2 pos = Geometry.randomPointInCircle(r, WorldGeneration.RANDOM);
			pos.add(centre);
			Optional<? extends Shape> collision = getCollidingShape(pos, entityRadius);
			if (collision.isPresent() && collision.get() instanceof Particle
					&& ((Particle) collision.get()).getUserData() instanceof PlantCell) {
				PlantCell plant = ((Particle) collision.get()).getUserData(PlantCell.class);
				plant.kill(CauseOfDeath.ENV_CAPACITY_EXCEEDED);
				return pos;
			} else if (collision.isEmpty())
				return pos;
		}

		return null;
	}

	public Vector2 getRandomPosition(Cell cell) {
//		return spawnPositionFns.getOrDefault(cell.getClass(), new SerializableFunction<Float, Vector2>() {
//                    @Override
//                    public Vector2 apply(Float entityRadius) {
//                        return Environment.this.randomPosition(entityRadius);
//                    }
//                })
//				.apply(cell.getRadius());
		SerializableFunction<Float, Vector2> fn = spawnPositionFns.get(cell.getClass());
		if (fn == null)
			return randomPosition(cell.getRadius());
		return fn.apply(cell.getRadius());
	}

	public Vector2 getRandomPosition(Class<? extends Cell> cellClass) {
//		return spawnPositionFns.getOrDefault(cellClass, new SerializableFunction<Float, Vector2>() {
//            @Override
//            public Vector2 apply(Float entityRadius) {
//                return Environment.this.randomPosition(entityRadius);
//            }
//        }).apply(0f);
		SerializableFunction<Float, Vector2> fn = spawnPositionFns.get(cellClass);
		if (fn == null)
			return randomPosition(0f);
		return fn.apply(0f);
	}

	public Vector2 randomPosition(float entityRadius) {
		return randomPosition(entityRadius, Geometry.ZERO, Environment.settings.worldgen.minRockClusterRadius.get());
	}

	public void tryAdd(Cell cell) {
		add(cell);
		// Assign a founder lineage id if this cell wasn't already tagged
		// (initial-spawn cells, or any future case where we inject cells
		// without a parent). Burst children get their parent's id set by
		// BurstRequest before they hit this path.
		if (cell.getLineageId() == 0L)
			cell.setLineageId(nextLineageId++);
		recordBirth(cell);
		bornCounts.put(cell.getClass(),
				bornCounts.getOrDefault(cell.getClass(), 0L) + 1);
		generationCounts.put(cell.getClass(),
				Math.max(generationCounts.getOrDefault(cell.getClass(), 0L),
						 cell.getGeneration()));
	}

	public long allocateLineageId() {
		return nextLineageId++;
	}

	private void recordBirth(Cell cell) {
		// Only track *protozoa* in the phylogeny. Plants and meat exist in
		// such churn (a meat pellet's "lineage" is meaningless; plants
		// barely evolve and 1500 of them swamp the tree with single-cell
		// chains) that including them turns the tree into a 10k-leaf
		// hairball nobody can read. The tree is meant to show interesting
		// evolutionary divergence — that's a protozoa-only concept here.
		if (!(cell instanceof Protozoan))
			return;
		LineageRecord r = new LineageRecord();
		r.id = cell.getId();
		r.parentId = cell.getParentId();
		r.generation = cell.getGeneration();
		r.birthTime = timeManager.getTimeElapsed();
		r.deathTime = -1f;
		r.aliveDescendants = 1;
		r.cellType = 0; // Protozoan
		lineageRecords.put(r.id, r);
		// Propagate the +1 alive-descendants count up the parent chain so
		// the renderer can prioritise lineages with the most living
		// descendants without scanning the whole tree every frame.
		long pid = r.parentId;
		// Same safety cap as recordDeath — a cycle in parentId would hang
		// every birth path and lock the sim. 4096 is far past any real depth.
		int hops = 0;
		while (pid != 0L && hops++ < 4096) {
			LineageRecord p = lineageRecords.get(pid);
			if (p == null) break;
			p.aliveDescendants++;
			pid = p.parentId;
		}
	}

	private void recordDeath(Cell cell) {
		LineageRecord r = lineageRecords.get(cell.getId());
		if (r == null) return; // plants/meat were never tracked
		r.deathTime = timeManager.getTimeElapsed();
		r.aliveDescendants--; // self
		long pid = r.parentId;
		// Safety cap: if a parentId chain ever forms a cycle (shouldn't,
		// but a corrupted save or simultaneous-lineage-id collision could
		// in principle create one), this loop would hang the death path
		// and stall the entire sim. 4096 generations is way past any
		// realistic chain depth.
		int hops = 0;
		while (pid != 0L && hops++ < 4096) {
			LineageRecord p = lineageRecords.get(pid);
			if (p == null) break;
			p.aliveDescendants--;
			pid = p.parentId;
		}
	}

	private float lineagePruneTimer = 0f;
	/** Drop lineage records whose entire subtree died out long enough ago
	 *  that they're no longer interesting for the tree view. Keeps the
	 *  record map bounded — without this it grows with every birth. */
	private void pruneDeadLineages(float delta) {
		lineagePruneTimer += delta;
		if (lineagePruneTimer < 30f) return;
		lineagePruneTimer = 0f;
		float now = timeManager.getTimeElapsed();
		final float keepDeadFor = 120f; // sim-sec of grace
		lineageRecords.values().removeIf(r ->
				r.aliveDescendants <= 0
				&& r.deathTime > 0f
				&& (now - r.deathTime) > keepDeadFor);
	}

	public java.util.Map<Long, LineageRecord> getLineageRecords() {
		return lineageRecords;
	}

	public void add(Cell cell) {
		cells.put(cell.getId(), cell);
		chunks.add(cell);
	}

	public Optional<Cell> getCell(long id) {
		return Optional.ofNullable(cells.get(id));
	}

	private void flushEntitiesToAdd() {
		for (Cell cell : cellsToAdd)
			tryAdd(cell);
		cellsToAdd.clear();
	}

	public int getCount(Class<? extends Cell> cellClass) {
		return chunks.getLocalCount(cellClass);
	}

	// Full chunk rebuild is expensive (clear all 1200 sets + re-add every
	// cell). Births/deaths are now tracked incrementally via chunks.add /
	// chunks.remove, so the only thing a full rebuild catches is cells that
	// have *moved* between chunks. Cells take many frames to traverse a chunk,
	// so stale-by-a-second counts are fine for capacity checks. We still do a
	// periodic full rebuild to clear any drift. Transient so adding this
	// field doesn't break older saves.
	private transient float chunkRebuildTimer = 0f;
	private static final float CHUNK_REBUILD_INTERVAL = 0.5f;

	public void updateChunkAllocations(float delta) {
		chunkRebuildTimer += delta;
		if (chunkRebuildTimer < CHUNK_REBUILD_INTERVAL)
			return;
		chunkRebuildTimer = 0f;
		chunks.clear();
		for (Cell cell : getCells())
			chunks.allocate(cell);
	}

	public void forceChunkRebuild() {
		chunks.clear();
		for (Cell cell : getCells())
			chunks.allocate(cell);
		chunkRebuildTimer = 0f;
	}

	private void dispose(Cell e) {
		CauseOfDeath cod = e.getCauseOfDeath();
		if (cod != null) {
			int count = causeOfDeathCounts.getOrDefault(cod, 0);
			causeOfDeathCounts.put(cod, count + 1);
		}
		e.getParticle().dispose();
	}

	public void depositOnDeath(Cell cell) {
		if (settings.enableChemicalField.get()) {
			if (!cell.isEngulfed() && cell.hasNotBurst()) {
				chemicalSolution.depositCircle(
						cell.getPos(), cell.getRadius() * 1.25f,
						cell.getColour());
			}
		}
	}


	public boolean hasCapacity(Class<? extends Cell> cellType, Vector2 pos) {
		return getLocalCount(cellType, pos) < getLocalCapacity(cellType)
				&& getGlobalCount(cellType) < getGlobalCapacity(cellType);
	}

	public boolean hasCapacity(Cell cell) {
		return hasCapacity(cell.getClass(), cell.getPos());
	}

	public int getGlobalCount(Class<? extends Cell> cellType) {
		int toAdd = 0;
		for (Cell cell : cellsToAdd)
			if (cell.getClass().equals(cellType))
				toAdd++;

		return chunks.getGlobalCount(cellType) + toAdd;
	}

	public int getGlobalCapacity(Cell cell) {
		return chunks.getGlobalCapacity(cell);
	}

	public int getGlobalCapacity(Class<? extends Cell> cellType) {
		return chunks.getGlobalCapacity(cellType);
	}

	public int getLocalCount(Cell cell) {
		return getLocalCount(cell.getClass(), cell.getPos());
	}

	public int getLocalCount(Class<? extends Cell> cellType, Vector2 pos) {
		int existingCount = chunks.getChunkCount(cellType, pos);
		SpatialHash<? extends Cell> cellHash = chunks.getCellHash(cellType, pos);
		int chunkX = cellHash.getChunkX(pos.x);
		int chunkY = cellHash.getChunkY(pos.y);
		for (Cell cell : cellsToAdd) {
			int thisChunkX = cellHash.getChunkX(cell.getPos().x);
			int thisChunkY = cellHash.getChunkY(cell.getPos().y);
			if (thisChunkY == chunkY && thisChunkX == chunkX)
				existingCount++;
		}
		return existingCount;
	}

	public int getLocalCapacity(Cell cell) {
		return getLocalCapacity(cell.getClass());
	}

	public int getLocalCapacity(Class<? extends Cell> cellType) {
		return chunks.getChunkCapacity(cellType);
	}

	public void registerToAdd(Cell e) {
		cellsToAdd.add(e);
	}

	public Statistics getStats() {
		Statistics stats = new Statistics();
		stats.putTime("Time Elapsed", timeManager.getTimeElapsed());
		stats.putPercentage("Time of Day", timeManager.getTimeOfDayPercentage());
		stats.put("Days Elapsed", timeManager.getDay());
		stats.putCount("Protozoa", numberOfProtozoa());
		stats.putCount("Plants", getCount(PlantCell.class));
		stats.putCount("Meat Pellets", getCount(MeatCell.class));

		stats.putPercentage("Sky Light Level", 100 * light.getEnvLight());

		stats.putCount("Max Protozoa Generation",
						generationCounts.getOrDefault(Protozoan.class, 1L).intValue());

		stats.putCount("Max Plant Generation",
				generationCounts.getOrDefault(PlantCell.class, 0L).intValue());

		for (Class<? extends Cell> cellClass : bornCounts.keySet())
			stats.putCount(cellClassNames.get(cellClass) + " Created",
					bornCounts.get(cellClass).intValue());

		if (Environment.settings.protozoa.matingEnabled.get())
			stats.putCount("Crossover Events", (int) crossoverEvents);

		for (CauseOfDeath cod : CauseOfDeath.values()) {
			if (cod.isDebugDeath())
				continue;
			int count = causeOfDeathCounts.getOrDefault(cod, 0);
			if (count > 0)
				stats.putCount("Died from " + cod.getReason(), count);
		}
		return stats;
	}

	public Statistics getDebugStats() {
		debugStats.clear();
		for (CauseOfDeath cod : CauseOfDeath.values()) {
			if (!cod.isDebugDeath())
				continue;
			int count = causeOfDeathCounts.getOrDefault(cod, 0);
			if (count > 0)
				debugStats.put("Died from " + cod.getReason(), (float) count);
		}
		return debugStats;
	}

	public Statistics getPhysicsDebugStats() {
		return physics.getDebugStats();
	}

	public Statistics getProtozoaSummaryStats(
			boolean computeLogStats, boolean removeMoleculeStats, boolean allStats) {
		Iterator<Statistics> protozoaStats = getCells().stream()
				.filter(isCellProtozoa)
				.map(new CellStatisticsFn(allStats))
				.iterator();

		Statistics stats = Statistics.computeSummaryStatistics(protozoaStats, computeLogStats);
		final int protozoaCount = numberOfProtozoa();
		stats.putCount("Protozoa Count", protozoaCount);
		stats.getStatsMap().entrySet().removeIf(
				new RemoveStatFromSummaryPredicate(protozoaCount, removeMoleculeStats, allStats)
		);
		return stats;
	}

	public Statistics getProtozoaSummaryStats() {
		return getProtozoaSummaryStats(false, true, false);
	}

	public int numberOfProtozoa() {
		return getCount(Protozoan.class);
	}

	// Per-cause death counters since last reset. Used by the homeostat tick
	// to print "what's killing cells this window".
	//
	// MUST be a thread-safe map: Cell.kill is called from inside the
	// parallelStream cell-update pass, so recordDeath fires concurrently
	// from worker threads. The previous HashMap.merge() was a freeze risk
	// — concurrent merges into a non-thread-safe HashMap can spin in
	// internal bucket traversal and lock up the worker pool, which is
	// exactly what happens in turbo mode where deaths per wall-second
	// spike. ConcurrentHashMap.merge() is lock-free per bucket.
	//
	// Transient because the counts are diagnostic, not save state.
	private transient java.util.concurrent.ConcurrentHashMap<com.protoevo.biology.CauseOfDeath, Integer>
			recentProtozoaDeaths = new java.util.concurrent.ConcurrentHashMap<>();
	private transient java.util.concurrent.ConcurrentHashMap<com.protoevo.biology.CauseOfDeath, Integer>
			recentPlantDeaths = new java.util.concurrent.ConcurrentHashMap<>();

	public void recordDeath(Cell cell, com.protoevo.biology.CauseOfDeath cause) {
		if (recentProtozoaDeaths == null) recentProtozoaDeaths = new java.util.concurrent.ConcurrentHashMap<>();
		if (recentPlantDeaths == null)    recentPlantDeaths    = new java.util.concurrent.ConcurrentHashMap<>();
		java.util.concurrent.ConcurrentHashMap<com.protoevo.biology.CauseOfDeath, Integer> target =
				cell instanceof Protozoan ? recentProtozoaDeaths
				: cell instanceof com.protoevo.biology.cells.PlantCell ? recentPlantDeaths
				: null;
		if (target != null)
			target.merge(cause, 1, Integer::sum);
	}

	public java.util.Map<com.protoevo.biology.CauseOfDeath, Integer> drainProtozoaDeaths() {
		java.util.Map<com.protoevo.biology.CauseOfDeath, Integer> snap =
				recentProtozoaDeaths == null
						? java.util.Collections.emptyMap()
						: new java.util.HashMap<>(recentProtozoaDeaths);
		if (recentProtozoaDeaths != null) recentProtozoaDeaths.clear();
		return snap;
	}

	public java.util.Map<com.protoevo.biology.CauseOfDeath, Integer> drainPlantDeaths() {
		java.util.Map<com.protoevo.biology.CauseOfDeath, Integer> snap =
				recentPlantDeaths == null
						? java.util.Collections.emptyMap()
						: new java.util.HashMap<>(recentPlantDeaths);
		if (recentPlantDeaths != null) recentPlantDeaths.clear();
		return snap;
	}

	public long getGeneration() {
		return generationCounts.getOrDefault(Protozoan.class, 0L);
	}

	public Optional<? extends Shape> getCollidingShape(Vector2 pos, float r) {
		Optional<Rock> collidingRock = rocks.stream()
				.filter(new RockCollisionWithParticlePredicate(pos, r))
				.findAny();
		if (collidingRock.isPresent())
			return collidingRock;

		return Streams.concat(getCells().stream(), cellsToAdd.stream())
				.filter(new DoCirclesCollidePredicate(pos, r))
				.map(getParticleFn)
				.findAny();
	}

	public float getElapsedTime() {
		return timeManager.getTimeElapsed();
	}

	public ChemicalSolution getChemicalSolution() {
		return chemicalSolution;
	}

	public List<Rock> getRocks() {
		return rocks;
	}

	public Physics getPhysics() {
		return physics;
	}

	public Collection<Cell> getCells() {
		return cells.values();
	}

	public Stream<Particle> getParticles() {
		return getCells().stream().map(getParticleFn);
	}

	public JointsManager getJointsManager() {
		return physics.getJointsManager();
	}

	public <T extends Cell> boolean hasBurstRequest(Cell parent, Class<T> cellType) {
		return burstRequests.containsKey(parent) &&
				burstRequests.get(parent).getCellType().equals(cellType);
	}

	public boolean hasBurstCapacity(Cell parent, Class<? extends Cell> cellType) {
		return hasCapacity(cellType, parent.getPos());
	}

	public <T extends Cell> void requestBurst(Cell parent,
											  Class<T> cellType,
											  SerializableFunction<Float, T> createChild,
											  boolean overrideMinParticleSize) {

		if (hasBurstRequest(parent, cellType) || !hasBurstCapacity(parent, cellType))
			return;

		burstRequests.put(parent, new BurstRequest<>(parent, cellType, createChild, overrideMinParticleSize));
	}

	public <T extends Cell> void requestBurst(Cell parent,
											  Class<T> cellType,
											  SerializableFunction<Float, T> createChild) {
		requestBurst(parent, cellType, createChild, false);
	}

	public SpatialHash<Cell> getSpatialHash(Class<? extends Cell> clazz) {
		return chunks.getSpatialHash(clazz);
	}

	public Chunks getChunks() {
		return chunks;
	}

	public void incrementCrossOverCount() {
		crossoverEvents = crossoverEvents + 1;
	}

	public float getRadius() {
		return settings.worldgen.radius.get();
	}

	public void dispose() {
		physics.dispose();
	}

	public LightManager getLightMap() {
		return light;
	}

	public float getLight(Vector2 pos) {
		return light.getLightLevel(pos);
	}

	public float getTemperature(Vector2 pos) {
		return light.getLightLevel(pos) * settings.env.maxLightEnvTemp.get();
	}

	public String getLoadingStatus() {
		return loadingStatus;
	}

	public SimulationSettings getSettings() {
		return mySettings;
	}

	public void setSimulationName(String simulationName) {
		this.simulationName = simulationName;
	}

	public String getSimulationName() {
		return simulationName;
	}

	public static class CellUpdateConsumer implements Serializable, Consumer<Cell> {
		float delta;
		float plantDelta;
		boolean updatePlantsAndMeat = true;

		public CellUpdateConsumer() {}

		public CellUpdateConsumer(float delta) {
			this.delta = delta;
			this.plantDelta = delta;
		}

		@Override
		public void accept(Cell cell) {
			if (cell instanceof Protozoan) {
				cell.update(delta);
				return;
			}
			if (!updatePlantsAndMeat)
				return;
			cell.update(plantDelta);
		}
	}

	public static class GetCellFunction implements SerializableFunction<Cell, Particle> {
		@Override
		public Particle apply(Cell cell) {
			return cell.getParticle();
		}
	}

	public static class DoCirclesCollidePredicate implements Serializable, Predicate<Cell> {
		private Vector2 pos;
		private float r;

		public DoCirclesCollidePredicate() {}

		public DoCirclesCollidePredicate(Vector2 pos, float r) {
			this.pos = pos;
			this.r = r;
		}

		@Override
		public boolean test(Cell cell) {
			return Geometry.doCirclesCollide(pos, r, cell.getPos(), cell.getRadius());
		}
	}

	public static class RemoveStatFromSummaryPredicate implements Serializable, Predicate<Map.Entry<String, Statistics.Stat>> {
		private int protozoaCount;
		private boolean removeMoleculeStats;
		private boolean allStats;

		public RemoveStatFromSummaryPredicate() {}

		public RemoveStatFromSummaryPredicate(int protozoaCount, boolean removeMoleculeStats, boolean allStats) {
			this.protozoaCount = protozoaCount;
			this.removeMoleculeStats = removeMoleculeStats;
			this.allStats = allStats;
		}

		@Override
		public boolean test(Map.Entry<String, Statistics.Stat> entry) {
			return entry.getKey().endsWith("Count")
					&& ((int) entry.getValue().getValue() == 0 || (int) entry.getValue().getValue() == protozoaCount)
					|| (removeMoleculeStats && entry.getKey().contains("Molecule"))
					|| (!allStats && (entry.getKey().contains("Min") || entry.getKey().contains("Max")));
		}
	}

	public static class RockCollisionWithParticlePredicate implements Serializable, Predicate<Rock> {
		private Vector2 pos;
		private float r;

		public RockCollisionWithParticlePredicate() {}

		public RockCollisionWithParticlePredicate(Vector2 pos, float r) {
			this.pos = pos;
			this.r = r;
		}

		@Override
		public boolean test(Rock rock) {
			return rock.intersectsWith(pos, r);
		}
	}

	public static class CellIsProtozoaPredicate implements Serializable, Predicate<Cell> {

		public CellIsProtozoaPredicate() {}

		@Override
		public boolean test(Cell cell) {
			return cell instanceof Protozoan;
		}
	}

	public static class CellDeadPredicate implements Predicate<Cell> {
		@Override
		public boolean test(Cell cell) {
			return cell.isDead();
		}
	}

	public static class SpawnPlantInClustersFn implements SerializableFunction<Float, Vector2> {
		private float clusterR;
		private Environment environment;
		private Vector2[] centres;

		public SpawnPlantInClustersFn() {}

		public SpawnPlantInClustersFn(Environment environment, Vector2[] centres, float clusterR) {
			this.environment = environment;
			this.centres = centres;
			this.clusterR = clusterR;
		}

		@Override
		public Vector2 apply(Float r) {
			return environment.randomPosition(r, centres, clusterR);
		}
	}

	public static class SpawnProtozoaInClustersFn implements SerializableFunction<Float, Vector2> {
		private float clusterR;
		private Environment environment;
		private Vector2[] centres;

		public SpawnProtozoaInClustersFn() {}

		public SpawnProtozoaInClustersFn(Environment environment, Vector2[] centres, float clusterR) {
			this.environment = environment;
			this.centres = centres;
			this.clusterR = clusterR;
		}

		@Override
		public Vector2 apply(Float r) {
			return environment.randomPosition(r, centres, 0.8f * clusterR);
		}
	}

	public static class SpawnInRandomPositionFn implements SerializableFunction<Float, Vector2> {

		private Environment env;

		public SpawnInRandomPositionFn() {}

		public SpawnInRandomPositionFn(Environment env) {
			this.env = env;
		}

		@Override
		public Vector2 apply(Float entityRadius) {
			return env.randomPosition(entityRadius);
		}
	}

	public static class CellStatisticsFn implements SerializableFunction<Cell, Statistics> {
		private boolean allStats;

		public CellStatisticsFn() {}

		public CellStatisticsFn(boolean allStats) {
			this.allStats = allStats;
		}

		@Override
		public Statistics apply(Cell p) {
			if (allStats)
				return ((Protozoan) p).getAllStats();
			return p.getStats();
		}
	}
}
