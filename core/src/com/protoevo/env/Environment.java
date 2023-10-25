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
import com.protoevo.core.Statistics;
import com.protoevo.physics.*;
import com.protoevo.physics.box2d.Box2DPhysics;
import com.protoevo.settings.SimulationSettings;
import com.protoevo.utils.Geometry;
import com.protoevo.utils.SerializableFunction;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Stream;


public class Environment implements Serializable
{
	private static final long serialVersionUID = 2804817237950199223L;
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
	}

	public void createTransientObjects() {
		cellsToAdd = new HashSet<>();
		chunks = new Chunks();
		chunks.initialise();
		updateChunkAllocations();
	}

	public boolean hasStarted() {
		return hasStarted;
	}

	public void rebuildWorld() {
		settings = mySettings;
		physics.rebuildTransientFields(this);
		getCells().forEach(cell -> cell.setEnvironment(this));
		updateChunkAllocations();
	}

	public void update(float delta)
	{
		hasStarted = true;
		settings = mySettings;
		getCells().forEach(cell -> cell.getParticle().physicsUpdate());

		timeManager.update(delta);
		light.update(delta);

		physics.step(delta);

  		handleCellUpdates(delta);
		handleBirthsAndDeaths();
		updateChunkAllocations();

		physics.getJointsManager().flushJoints();

		if (Environment.settings.enableChemicalField.get()) {
			chemicalSolution.update(delta);
		}
	}

	public void ensureAddedToEnvironment(Cell cell) {
		if (!cells.containsKey(cell.getId()))
			registerToAdd(cell);
	}

	private void handleCellUpdates(float delta) {
		getCells().parallelStream().forEach(cell -> cell.update(delta));
	}

	private void handleBirthsAndDeaths() {
		handledBurstRequests.clear();
		for (Cell parent : burstRequests.keySet()) {
			BurstRequest<? extends Cell> burstRequest = burstRequests.get(parent);
			if (hasBurstCapacity(parent, burstRequest.getCellType()) && burstRequest.canBurst()) {
				burstRequest.burst();
				handledBurstRequests.add(parent);
			}
		}
		for (Cell parent : handledBurstRequests)
			burstRequests.remove(parent);
		handledBurstRequests.clear();

		flushEntitiesToAdd();

		for (Cell cell : getCells()) {
			if (cell.isDead()) {
				dispose(cell);
				depositOnDeath(cell);
			}
		}
		getCells().removeIf(Cell::isDead);
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
			spawnPositionFns.put(PlantCell.class, r -> randomPosition(r, populationStartCentres, clusterR));
			spawnPositionFns.put(Protozoan.class, r -> randomPosition(r, populationStartCentres, 0.8f * clusterR));
		}
		else {
			spawnPositionFns.put(PlantCell.class, this::randomPosition);
			spawnPositionFns.put(Protozoan.class, this::randomPosition);
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
			cell.setEnvironmentAndBuildPhysics(this);
			findRandomPositionOrKillCell(cell);
		}

		int nProtozoa = Environment.settings.worldgen.numInitialProtozoa.get();
		nProtozoa = Math.min(nProtozoa, chunks.getGlobalCapacity(Protozoan.class));
		System.out.println("Creating population of " + nProtozoa + " protozoa...");
		loadingStatus = "Spawning Protozoa";
		for (int i = 0; i < nProtozoa; i++) {
			Protozoan p = Evolvable.createNew(Protozoan.class);
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
			} else if (!collision.isPresent())
				return pos;
		}

		return null;
	}

	public Vector2 getRandomPosition(Cell cell) {
		return spawnPositionFns.getOrDefault(cell.getClass(), this::randomPosition)
				.apply(cell.getRadius());
	}

	public Vector2 getRandomPosition(Class<? extends Cell> cellClass) {
		return spawnPositionFns.getOrDefault(cellClass, this::randomPosition).apply(0f);
	}

	public Vector2 randomPosition(float entityRadius) {
		return randomPosition(entityRadius, Geometry.ZERO, Environment.settings.worldgen.minRockClusterRadius.get());
	}

	public void tryAdd(Cell cell) {
		add(cell);
		bornCounts.put(cell.getClass(),
				bornCounts.getOrDefault(cell.getClass(), 0L) + 1);
		generationCounts.put(cell.getClass(),
				Math.max(generationCounts.getOrDefault(cell.getClass(), 0L),
						 cell.getGeneration()));
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

	public void updateChunkAllocations() {
		chunks.clear();
		getCells().forEach(chunks::allocate);
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
				.filter(cell -> cell instanceof Protozoan)
				.map(p -> {
					if (allStats)
						return ((Protozoan) p).getAllStats();
					return p.getStats();
				})
				.iterator();

		Statistics stats = Statistics.computeSummaryStatistics(protozoaStats, computeLogStats);
		final int protozoaCount = numberOfProtozoa();
		stats.putCount("Protozoa Count", protozoaCount);
		stats.getStatsMap().entrySet().removeIf(
			entry -> entry.getKey().endsWith("Count")
					&& ((int) entry.getValue().getValue() == 0 || (int) entry.getValue().getValue() == protozoaCount)
				|| (removeMoleculeStats && entry.getKey().contains("Molecule"))
				|| (!allStats && (entry.getKey().contains("Min") || entry.getKey().contains("Max")))
		);
		return stats;
	}

	public Statistics getProtozoaSummaryStats() {
		return getProtozoaSummaryStats(false, true, false);
	}

	public int numberOfProtozoa() {
		return getCount(Protozoan.class);
	}

	public long getGeneration() {
		return generationCounts.getOrDefault(Protozoan.class, 0L);
	}

	public Optional<? extends Shape> getCollidingShape(Vector2 pos, float r) {
		Optional<Rock> collidingRock = rocks.stream().filter(rock -> rock.intersectsWith(pos, r)).findAny();
		if (collidingRock.isPresent())
			return collidingRock;

		return Streams.concat(getCells().stream(), cellsToAdd.stream())
				.filter(cell -> Geometry.doCirclesCollide(pos, r, cell.getPos(), cell.getRadius()))
				.map(Cell::getParticle)
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
		return getCells().stream().map(Cell::getParticle);
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
}
