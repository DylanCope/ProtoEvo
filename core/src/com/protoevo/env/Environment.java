package com.protoevo.env;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Streams;
import com.protoevo.biology.BurstRequest;
import com.protoevo.biology.CauseOfDeath;
import com.protoevo.biology.cells.Cell;
import com.protoevo.biology.cells.MeatCell;
import com.protoevo.biology.cells.PlantCell;
import com.protoevo.biology.cells.Protozoan;
import com.protoevo.biology.evolution.Evolvable;
import com.protoevo.biology.nodes.SurfaceNode;
import com.protoevo.biology.organelles.Organelle;
import com.protoevo.core.Statistics;
import com.protoevo.physics.Shape;
import com.protoevo.physics.*;
import com.protoevo.settings.EnvironmentSettings;
import com.protoevo.utils.Geometry;
import com.protoevo.utils.SerializableFunction;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;


public class Environment implements Serializable
{
	private static final long serialVersionUID = 2804817237950199223L;
	public static EnvironmentSettings settings = new EnvironmentSettings();

	private final EnvironmentSettings mySettings;
	private transient World world;
	private float elapsedTime, physicsStepTime;
	private final Statistics stats = new Statistics();
	private final Statistics debugStats = new Statistics();
	public final ConcurrentHashMap<CauseOfDeath, Integer> causeOfDeathCounts =
			new ConcurrentHashMap<>(CauseOfDeath.values().length, 1);

	private final Chunks chunks = new Chunks();

	private Map<Class<? extends Particle>, SerializableFunction<Float, Vector2>> spawnPositionFns;

	private final ChemicalSolution chemicalSolution;
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
	private transient ConcurrentHashMap<Long, Cell> cells;
	private boolean hasInitialised, hasStarted;
	private Vector2[] populationStartCentres;
	private final JointsManager jointsManager;
	@JsonIgnore
	private final ConcurrentLinkedQueue<BurstRequest<? extends Cell>> burstRequests = new ConcurrentLinkedQueue<>();

	public Environment()
	{
		this(settings);
	}

	public Environment(EnvironmentSettings settings) {
		mySettings = settings;
		Environment.settings = settings;

		hasStarted = false;
		createTransientObjects();
		buildWorld();
		jointsManager = new JointsManager(this);

		System.out.println("Creating chemicals solution... ");
		if (Environment.settings.enableChemicalField.get()) {
			chemicalSolution = new ChemicalSolution(
					this,
					Environment.settings.chemicalFieldResolution.get(),
					Environment.settings.chemicalFieldRadius.get());
		} else {
			chemicalSolution = null;
		}

		elapsedTime = 0;
		hasInitialised = false;
	}

	public void createTransientObjects() {
		cellsToAdd = new HashSet<>();
		cells = new ConcurrentHashMap<>();
		chunks.initialise();
	}

	public void buildWorld() {
		world = new World(new Vector2(0, 0), true);
		world.setContinuousPhysics(false);
		world.setAutoClearForces(true);
		world.setContactListener(new CollisionHandler(this));
	}

	public boolean hasStarted() {
		return hasStarted;
	}

	public void rebuildWorld() {
		settings = mySettings;
		buildWorld();
		createRockFixtures();
		for (Cell cell : getCells())
			cell.setEnv(this);
		jointsManager.rebuild();
		updateChunkAllocations();
	}

	public void update(float delta)
	{
		hasStarted = true;
		getCells().forEach(Particle::physicsUpdate);

		elapsedTime += delta;
		long startTime = System.nanoTime();
		world.step(
				delta,
				Environment.settings.misc.physicsVelocityIterations.get(),
				Environment.settings.misc.physicsPositionIterations.get());
		physicsStepTime = TimeUnit.SECONDS.convert(
				System.nanoTime() - startTime, TimeUnit.NANOSECONDS);

  		handleCellUpdates(delta);
		handleBirthsAndDeaths();
		updateChunkAllocations();

		jointsManager.flushJoints();

		if (Environment.settings.enableChemicalField.get()) {
			chemicalSolution.update(delta);
		}
	}

	private void handleCellUpdates(float delta) {
		getCells().parallelStream().forEach(cell -> cell.update(delta));
	}

	private void handleBirthsAndDeaths() {
		for (BurstRequest<? extends Cell> burstRequest : burstRequests)
			if (burstRequest.canBurst())
				burstRequest.burst();
		burstRequests.clear();

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
		createRockFixtures();
	}

	public void createRockFixtures() {
		for (Rock rock : this.getRocks()) {
			BodyDef rockBodyDef = new BodyDef();
			Body rockBody = world.createBody(rockBodyDef);
			PolygonShape rockShape = new PolygonShape();
			rockShape.set(rock.getPoints());
			rockBody.setUserData(rock);

			FixtureDef rockFixtureDef = new FixtureDef();
			rockFixtureDef.shape = rockShape;
			rockFixtureDef.density = 0.0f;
			rockFixtureDef.friction = 0.7f;
			rockFixtureDef.filter.categoryBits = ~FixtureCategories.SENSOR;

			rockBody.createFixture(rockFixtureDef);
		}
	}

	public void initialise() {
		System.out.println("Commencing world generation... ");
		createRocks();

		initialisePopulation();

		flushEntitiesToAdd();

		if (chemicalSolution != null)
			chemicalSolution.initialise();

		hasInitialised = true;
		System.out.println("Environment initialisation complete.");
	}

	public boolean hasBeenInitialised() {
		return hasInitialised;
	}

	private void buildSpawners() {
		spawnPositionFns = new HashMap<>(3, 1);
		if (populationStartCentres != null) {
			final float clusterR = Environment.settings.world.populationClusterRadius.get();
			spawnPositionFns.put(PlantCell.class, r -> randomPosition(r, populationStartCentres, 1.5f*clusterR));
			spawnPositionFns.put(Protozoan.class, r -> randomPosition(r, populationStartCentres, clusterR));
		}
		else {
			spawnPositionFns.put(PlantCell.class, this::randomPosition);
			spawnPositionFns.put(Protozoan.class, this::randomPosition);
		}
	}

	public void initialisePopulation() {
		populationStartCentres = new Vector2[Environment.settings.world.numPopulationStartClusters.get()];
		for (int i = 0; i < populationStartCentres.length; i++)
			populationStartCentres[i] = Geometry.randomPointInCircle(
					Environment.settings.world.radius.get() / 2f, WorldGeneration.RANDOM);

		buildSpawners();

		System.out.println("Creating initial plant population...");
		for (int i = 0; i < Environment.settings.world.numInitialPlantPellets.get(); i++) {
			PlantCell cell = new PlantCell(this);
			if (cell.isDead()) {
				System.out.println(
					"Failed to find position for plant pellet. " +
					"Try increasing the number of population clusters or reduce the number of initial plants.");
				break;
			}
		}

		System.out.println("Creating initial protozoan population...");
		for (int i = 0; i < Environment.settings.world.numInitialProtozoa.get(); i++) {
			Protozoan p = Evolvable.createNew(Protozoan.class);
			p.addToEnv(this);
			if (p.isDead()) {
				System.out.println(
					"Failed to find position for protozoan. " +
					"Try increasing the number of population clusters or reduce the number of initial protozoa.");
				break;
			}
		}

		for (Particle p : cellsToAdd)
			p.applyImpulse(Geometry.randomVector(.01f));
	}

	public Vector2 randomPosition(float entityRadius, Vector2[] clusterCentres) {
		int clusterIdx = MathUtils.random(clusterCentres.length - 1);
		Vector2 clusterCentre = clusterCentres[clusterIdx];
		return randomPosition(entityRadius, clusterCentre, Environment.settings.world.populationClusterRadius.get());
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
			Optional<? extends Shape> collision = getCollision(pos, entityRadius);
			if (collision.isPresent() && collision.get() instanceof PlantCell) {
				PlantCell plant = (PlantCell) collision.get();
				plant.kill(CauseOfDeath.ENV_CAPACITY_EXCEEDED);
				return pos;
			} else if (!collision.isPresent())
				return pos;
		}

		return null;
	}

	public Vector2 getRandomPosition(Particle particle) {
		return spawnPositionFns.getOrDefault(particle.getClass(), this::randomPosition)
				.apply(particle.getRadius());
	}

	public Vector2 randomPosition(float entityRadius) {
		return randomPosition(entityRadius, Geometry.ZERO, Environment.settings.world.rockClusterRadius.get());
	}

	public void tryAdd(Cell cell) {
		if (getLocalCount(cell) < getLocalCapacity(cell)
				&& chunks.getGlobalCount(cell) < chunks.getGlobalCapacity(cell)) {
			add(cell);
			bornCounts.put(cell.getClass(),
					bornCounts.getOrDefault(cell.getClass(), 0L) + 1);
			generationCounts.put(cell.getClass(),
					Math.max(generationCounts.getOrDefault(cell.getClass(), 0L),
							 cell.getGeneration()));
		}
		else {
			cell.kill(CauseOfDeath.ENV_CAPACITY_EXCEEDED);
			dispose(cell);
		}
	}

	public void add(Cell cell) {
		cells.put(cell.getId(), cell);
		chunks.add(cell);
	}

	public Cell getCell(long id) {
		if (cells.containsKey(id))
			return cells.get(id);
		return null;
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

	private void dispose(Particle e) {
		CauseOfDeath cod = e.getCauseOfDeath();
		if (cod != null) {
			int count = causeOfDeathCounts.getOrDefault(cod, 0);
			causeOfDeathCounts.put(cod, count + 1);
		}
		e.dispose();
	}

//	private final Colour meatColourDeposit = new Colour(Color.FIREBRICK);
//	private final Colour plantColourDeposit = new Colour(Color.FOREST);

	public void depositOnDeath(Cell cell) {
		if (settings.enableChemicalField.get()) {
			if (!cell.isEngulfed() && cell.hasNotBurst()) {
				chemicalSolution.depositCircle(
						cell.getPos(), cell.getRadius() * 1.25f,
						cell.getColour());
//				if (cell instanceof Protozoan || cell instanceof MeatCell)
//					chemicalSolution.depositCircle(
//							cell.getPos(), cell.getRadius() * 1.5f,
//							meatColourDeposit.cpy().mul(0.25f + 0.75f * cell.getHealth()));
//				else if (cell instanceof PlantCell)
//					chemicalSolution.depositCircle(
//							cell.getPos(), cell.getRadius() * 1.5f,
//							plantColourDeposit.cpy().mul(0.25f + 0.75f * cell.getHealth()));
			}
		}
	}

	public int getLocalCount(Particle cell) {
		return getLocalCount(cell.getClass(), cell.getPos());
	}

	public int getLocalCount(Class<? extends Particle> cellType, Vector2 pos) {
		return chunks.getChunkCount(cellType, pos);
	}

	public int getLocalCapacity(Particle cell) {
		return getLocalCapacity(cell.getClass());
	}

	public int getLocalCapacity(Class<? extends Particle> cellType) {
		return chunks.getChunkCapacity(cellType);
	}

	public void registerToAdd(Cell e) {
		if (getLocalCount(e) >= getLocalCapacity(e)) {
			e.kill(CauseOfDeath.ENV_CAPACITY_EXCEEDED);
			dispose(e);
		}

		cellsToAdd.add(e);
	}

	public Statistics getStats() {
		Statistics stats = new Statistics();
		stats.putTime("Time Elapsed", elapsedTime);
		stats.putCount("Protozoa", numberOfProtozoa());
		stats.putCount("Plants", getCount(PlantCell.class));
		stats.putCount("Meat Pellets", getCount(MeatCell.class));

		stats.putCount("Max Protozoa Generation",
						generationCounts.get(Protozoan.class).intValue());

		stats.putCount("Max Plant Generation",
				generationCounts.get(PlantCell.class).intValue());

		for (Class<? extends Cell> cellClass : bornCounts.keySet())
			stats.putCount(cellClassNames.get(cellClass) + " Created",
					bornCounts.get(cellClass).intValue());

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
		debugStats.clear();

		debugStats.putCount("Bodies", world.getBodyCount());
		debugStats.putCount("Contacts", world.getContactCount());
		debugStats.putCount("Joints", world.getJointCount());
		debugStats.putCount("Fixtures", world.getFixtureCount());
		debugStats.putCount("Proxies", world.getProxyCount());
		debugStats.putTime("Physics Step Time", physicsStepTime);

		int totalCells = cells.size();
		int sleepCount = 0;
		for (Cell cell : getCells())
			if (cell.getBody() != null && !cell.getBody().isAwake())
				sleepCount++;

		debugStats.putPercentage("Sleeping",  100f * sleepCount / totalCells);

		return debugStats;
	}

	public Statistics getProtozoaSummaryStats(
			boolean computeLogStats, boolean removeMoleculeStats, boolean allStats) {
		Iterator<Statistics> protozoaStats = getCells().stream()
				.filter(cell -> cell instanceof Protozoan)
				.map(p -> {
					if (allStats) {
						Statistics stats = new Statistics(p.getStats());
						for (SurfaceNode node : p.getSurfaceNodes())
							stats.putAll("Node " + node.getIndex() +": ", node.getStats());
						for (Organelle organelle : p.getOrganelles())
							stats.putAll("Organelle " + organelle.getIndex() +": ", organelle.getStats());
						return stats;
					}
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

	public Optional<? extends Shape> getCollision(Vector2 pos, float r) {
		Optional<Cell> collidingCell = Streams.concat(getCells().stream(), cellsToAdd.stream())
				.filter(cell -> Geometry.doCirclesCollide(pos, r, cell.getPos(), cell.getRadius()))
				.findAny();

		if (collidingCell.isPresent())
			return collidingCell;

		return rocks.stream().filter(rock -> rock.intersectsWith(pos, r)).findAny();
	}

	public float getElapsedTime() {
		return elapsedTime;
	}

	public ChemicalSolution getChemicalSolution() {
		return chemicalSolution;
	}

	public List<Rock> getRocks() {
		return rocks;
	}

	public World getWorld() {
		return world;
	}

	public Collection<Cell> getCells() {
		return cells.values();
	}

	public Collection<? extends Particle> getParticles() {
		return getCells();
	}

	public void ensureAddedToEnvironment(Particle particle) {
		if (particle instanceof Cell) {
			Cell cell = (Cell) particle;
			if (!cells.containsKey(cell.getId()))
				registerToAdd(cell);
		}
	}

	public JointsManager getJointsManager() {
		return jointsManager;
	}

	public <T extends Cell> void requestBurst(Cell parent,
											  Class<T> cellType,
											  SerializableFunction<Float, T> createChild,
											  boolean overrideMinParticleSize) {

		if (getLocalCount(cellType, parent.getPos()) >= getLocalCapacity(cellType))
			return;

		if (burstRequests.stream().anyMatch(request -> request.parentEquals(parent)))
			return;

		BurstRequest<T> request = new BurstRequest<>(
				parent, cellType, createChild, overrideMinParticleSize);
		burstRequests.add(request);
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
		crossoverEvents++;
	}

	public float getRadius() {
		return settings.world.radius.get();
	}
}
