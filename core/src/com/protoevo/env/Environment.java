package com.protoevo.env;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.google.common.collect.Streams;
import com.protoevo.biology.*;
import com.protoevo.biology.evolution.Evolvable;
import com.protoevo.biology.protozoa.Protozoan;
import com.protoevo.core.FixtureCategories;
import com.protoevo.core.Particle;
import com.protoevo.core.SpatialHash;
import com.protoevo.core.settings.WorldGenerationSettings;
import com.protoevo.core.settings.Settings;
import com.protoevo.core.Simulation;
import com.protoevo.core.settings.SimulationSettings;
import com.protoevo.utils.FileIO;
import com.protoevo.utils.Geometry;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Environment implements Serializable
{
	private static final long serialVersionUID = 2804817237950199223L;
	private final World world;
	private float elapsedTime;
	private final ConcurrentHashMap<String, Float> stats = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, Float> debugStats = new ConcurrentHashMap<>();
	public final ConcurrentHashMap<CauseOfDeath, Integer> causeOfDeathCounts =
			new ConcurrentHashMap<>(CauseOfDeath.values().length, 1);
	private final ConcurrentHashMap<Class<? extends Cell>, SpatialHash<Cell>> spatialHashes;
	private final Map<Class<? extends Particle>, Function<Float, Vector2>> spawnPositionFns
			= new HashMap<>(3, 1);
	private ChemicalSolution chemicalSolution;
	private final List<Rock> rocks = new ArrayList<>();
	private long generation = 1, protozoaBorn = 0, totalCellsAdded = 0, crossoverEvents = 0;

	private String genomeFile = null;
	private final List<String> genomesToWrite = new ArrayList<>();

	private final Set<Cell> cellsToAdd = new HashSet<>();
	private final Set<Cell> cells = new HashSet<>();
	private boolean hasInitialised;
	private Vector2[] populationStartCentres;

	private final JointsManager jointsManager;
	private final ConcurrentLinkedQueue<BurstRequest<? extends Cell>> burstRequests = new ConcurrentLinkedQueue<>();

	public Environment()
	{
		world = new World(new Vector2(0, 0), true);
		world.setContinuousPhysics(false);

		jointsManager = new JointsManager(this);
		world.setContactListener(new CollisionHandler(this));

		System.out.println("Creating chemicals solution... ");
		if (Settings.enableChemicalField) {
			chemicalSolution = new ChemicalSolution(
					this,
					SimulationSettings.chemicalFieldResolution,
					SimulationSettings.chemicalFieldRadius);
		}

		int resolution = SimulationSettings.spatialHashResolution;
//		int protozoaCap = (int) Math.ceil(SimulationSettings.maxProtozoa / (float) (resolution * resolution));
//		int plantCap = (int) Math.ceil(SimulationSettings.maxPlants / (float) (resolution * resolution));
//		int meatCap = (int) Math.ceil(SimulationSettings.maxMeat / (float) (resolution * resolution));
		int protozoaCap = 20;
		int plantCap = 50;
		int meatCap = 50;

		spatialHashes = new ConcurrentHashMap<>(3, 1);
		spatialHashes.put(Protozoan.class, new SpatialHash<>(resolution, protozoaCap, SimulationSettings.spatialHashRadius));
		spatialHashes.put(PlantCell.class, new SpatialHash<>(resolution, plantCap, SimulationSettings.spatialHashRadius));
		spatialHashes.put(MeatCell.class, new SpatialHash<>(resolution, meatCap, SimulationSettings.spatialHashRadius));

		elapsedTime = 0;
		hasInitialised = false;
	}

	public void update(float delta)
	{
		cells.parallelStream().forEach(Particle::reset);

		elapsedTime += delta;
		world.step(
				delta,
				SimulationSettings.physicsVelocityIterations,
				SimulationSettings.physicsPositionIterations);

		handleCellUpdates(delta);
		handleBirthsAndDeaths();
		updateSpatialHashes();

		jointsManager.flushJoints();

		if (Settings.enableChemicalField) {
			chemicalSolution.update(delta);
		}
	}

	private void handleCellUpdates(float delta) {
		cells.parallelStream().forEach(cell -> cell.update(delta));
	}

	private void handleBirthsAndDeaths() {
		burstRequests.forEach(BurstRequest::burst);
		burstRequests.clear();
		flushEntitiesToAdd();

		cells.forEach(this::handleDeadEntity);
		cells.removeIf(Cell::isDead);
	}

	public Vector2[] createRocks() {
		System.out.println("Creating rocks structures...");
		WorldGeneration.generateClustersOfRocks(
				this, new Vector2(0, 0), 1, WorldGenerationSettings.rockClusterRadius);

		int numClusterCentres = 8;
		Vector2[] clusterCentres = new Vector2[numClusterCentres];

		for (int i = 0; i < numClusterCentres; i++) {
			float minR = WorldGenerationSettings.rockClusterRadius;
			float maxR = WorldGenerationSettings.environmentRadius / 5f - WorldGenerationSettings.rockClusterRadius;

			Vector2 centre = WorldGeneration.randomPosition(minR, maxR);
			clusterCentres[i] = centre;

			int nRings = WorldGeneration.RANDOM.nextInt(1, 3);
			float radiusRange = WorldGeneration.RANDOM.nextFloat(8.f) * WorldGenerationSettings.rockClusterRadius;
			WorldGeneration.generateClustersOfRocks(this, centre, nRings, radiusRange);
		}

		WorldGeneration.generateRocks(this, 200);

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
		return clusterCentres;
	}

	public void initialise() {
		System.out.println("Commencing world generation... ");
		populationStartCentres = createRocks();

		// random shuffle population start centres
		List<Vector2> populationStartCentresList = Arrays.asList(populationStartCentres);
		Collections.shuffle(populationStartCentresList);
		populationStartCentres = populationStartCentresList.toArray(new Vector2[0]);

		if (populationStartCentres.length > 0)
			initialisePopulation(Arrays.copyOfRange(
					populationStartCentres, 0, WorldGenerationSettings.numPopulationClusters));
		else
			initialisePopulation();

		flushEntitiesToAdd();

		if (Settings.writeGenomes && genomeFile != null)
			writeGenomeHeaders();

		if (chemicalSolution != null)
			chemicalSolution.initialise();

		hasInitialised = true;
		System.out.println("Environment initialisation complete.");
	}

	public void writeGenomeHeaders() {
//		Protozoan protozoan = chunkManager.getAllCells()
//				.stream()
//				.filter(cell -> cell instanceof Protozoan)
//				.map(cell -> (Protozoan) cell)
//				.findAny()
//				.orElseThrow(() -> new RuntimeException("No initial population present"));
//
//		StringBuilder headerStr = new StringBuilder();
//		headerStr.append("Generation,Time Elapsed,Parent 1 ID,Parent 2 ID,ID,");
////		for (Gene<?> gene : protozoan.getGenome().getGenes())
////			headerStr.append(gene.getTraitName()).append(",");
//
//		FileIO.appendLine(genomeFile, headerStr.toString());
	}

	public boolean hasBeenInitialised() {
		return hasInitialised;
	}

	public void initialisePopulation(Vector2[] clusterCentres) {
		Function<Float, Vector2> findPlantPosition = this::randomPosition;
		Function<Float, Vector2> findProtozoaPosition = this::randomPosition;
		if (clusterCentres != null) {
			findPlantPosition = r -> randomPosition(2f*r, clusterCentres);
			findProtozoaPosition = r -> randomPosition(r, clusterCentres);
		}

		spawnPositionFns.put(PlantCell.class, findPlantPosition);
		spawnPositionFns.put(Protozoan.class, findProtozoaPosition);

		System.out.println("Creating initial plant population...");
		for (int i = 0; i < WorldGenerationSettings.numInitialPlantPellets; i++) {
			PlantCell cell = new PlantCell(this);
			if (cell.isDead()) {
				System.out.println(
					"Failed to find position for plant pellet. " +
					"Try increasing the number of population clusters or reduce the number of initial plants.");
				break;
			}
		}

		System.out.println("Creating initial protozoan population...");
		for (int i = 0; i < WorldGenerationSettings.numInitialProtozoa; i++) {
			Protozoan p = Evolvable.createNew(Protozoan.class);
			p.setEnv(this);
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

	public void initialisePopulation() {
		Vector2[] clusterCentres = new Vector2[WorldGenerationSettings.numPopulationClusters];
		for (int i = 0; i < clusterCentres.length; i++)
			clusterCentres[i] = randomPosition(WorldGenerationSettings.populationClusterRadius);
		initialisePopulation(clusterCentres);
	}

	public Vector2 randomPosition(float entityRadius, Vector2[] clusterCentres) {
		int clusterIdx = Simulation.RANDOM.nextInt(clusterCentres.length);
		Vector2 clusterCentre = clusterCentres[clusterIdx];
		return randomPosition(entityRadius, clusterCentre, WorldGenerationSettings.populationClusterRadius);
	}

	public Vector2 randomPosition(float entityRadius, Vector2 centre, float clusterRadius) {
		for (int i = 0; i < 20; i++) {
			float r = clusterRadius * Simulation.RANDOM.nextFloat();
			Vector2 pos = Geometry.randomVector(r*r);
			pos.setLength((float) Math.sqrt(pos.len()));
			pos.add(centre);
			if (notCollidingWithAnything(pos, entityRadius))
				return pos;
		}

		return null;
	}

	public Vector2 getRandomPosition(Particle particle) {
		return spawnPositionFns.getOrDefault(particle.getClass(), this::randomPosition)
				.apply(particle.getRadius());
	}

	public Vector2 randomPosition(float entityRadius) {
		return randomPosition(entityRadius, Geometry.ZERO, WorldGenerationSettings.rockClusterRadius);
	}

	private void flushEntitiesToAdd() {
		for (Cell cell : cellsToAdd) {
			if (getLocalCount(cell) < getLocalCapacity(cell)) {
				cells.add(cell);
				// update local counts
				spatialHashes.get(cell.getClass()).add(cell);
			}
			else {
				cell.kill(CauseOfDeath.ENV_CAPACITY_EXCEEDED);
				handleDeadEntity(cell);
			}
		}
		cellsToAdd.clear();
	}

	private void flushWrites() {
		List<String> genomeWritesHandled = new ArrayList<>();
		for (String line : genomesToWrite) {
			FileIO.appendLine(genomeFile, line);
			genomeWritesHandled.add(line);
		}
		genomesToWrite.removeAll(genomeWritesHandled);
	}

	public int getCount(Class<? extends Cell> cellClass) {
		return spatialHashes.get(cellClass).size();
	}

	private void updateSpatialHashes() {
		spatialHashes.values().forEach(SpatialHash::clear);
		cells.forEach(cell -> spatialHashes.get(cell.getClass()).add(cell));
	}

	public int getCapacity(Class<? extends Cell> cellClass) {
		return spatialHashes.get(cellClass).getTotalCapacity();
	}

	private void handleDeadEntity(Particle e) {
		if (!e.isDead())
			return;
		CauseOfDeath cod = e.getCauseOfDeath();
		if (cod != null) {
			int count = causeOfDeathCounts.getOrDefault(cod, 0);
			causeOfDeathCounts.put(cod, count + 1);
		}
		if (Settings.enableChemicalField)
			chemicalSolution.depositCircle(e.getPos(), e.getRadius() * 1.5f, e.getColor());
		e.dispose();
	}

	private void handleNewProtozoa(Protozoan p) {
		protozoaBorn++;
		generation = Math.max(generation, p.getGeneration());

//		if (genomeFile != null && Settings.writeGenomes) {
//			String genomeLine = p.getGeneration() + "," + elapsedTime + "," + p.getGenome().toString();
//			genomesToWrite.add(genomeLine);
//		}
	}

	public int getLocalCount(Particle cell) {
		return getLocalCount(cell.getClass(), cell.getPos());
	}

	public int getLocalCount(Class<? extends Particle> cellType, Vector2 pos) {
		return spatialHashes.get(cellType).getCount(pos);
	}

	public int getLocalCapacity(Particle cell) {
		return getLocalCapacity(cell.getClass());
	}

	public int getLocalCapacity(Class<? extends Particle> cellType) {
		return spatialHashes.get(cellType).getChunkCapacity();
	}

	public void add(Cell e) {
		if (getLocalCount(e) >= getLocalCapacity(e)) {
			e.kill(CauseOfDeath.ENV_CAPACITY_EXCEEDED);
			handleDeadEntity(e);
		}

		if (!cellsToAdd.contains(e)) {
			totalCellsAdded++;
			cellsToAdd.add(e);

			if (e instanceof Protozoan)
				handleNewProtozoa((Protozoan) e);
		}
	}

	public Map<String, Float> getStats(boolean includeProtozoaStats) {
		stats.clear();
		stats.put("Protozoa", (float) numberOfProtozoa());
		stats.put("Plants", (float) getCount(PlantCell.class));
		stats.put("Meat Pellets", (float) getCount(MeatCell.class));
		stats.put("Max Generation", (float) generation);
		stats.put("Time Elapsed", elapsedTime);
		stats.put("Protozoa Born", (float) protozoaBorn);
		stats.put("Crossover Events", (float) crossoverEvents);
		for (CauseOfDeath cod : CauseOfDeath.values()) {
			if (cod.isDebugDeath())
				continue;
			int count = causeOfDeathCounts.getOrDefault(cod, 0);
			if (count > 0)
				stats.put("Died from " + cod.getReason(), (float) count);
		}
		if (includeProtozoaStats)
			stats.putAll(getProtozoaStats());
		return stats;
	}

	public Map<String, Float> getDebugStats() {
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

	public Map<String, Float> getStats() {
		return getStats(false);
	}

	public Map<String, Float> getProtozoaStats() {
		Map<String, Float> stats = new TreeMap<>();
		Collection<Protozoan> protozoa = cells.stream()
				.filter(cell -> cell instanceof Protozoan)
				.map(cell -> (Protozoan) cell)
				.collect(Collectors.toSet());

		for (Cell e : protozoa) {
			for (Map.Entry<String, Float> stat : e.getStats().entrySet()) {
				String key = "Sum " + stat.getKey();
				float currentValue = stats.getOrDefault(key, 0f);
				stats.put(key, stat.getValue() + currentValue);
			}
		}

		int numProtozoa = protozoa.size();
		for (Cell e : protozoa) {
			for (Map.Entry<String, Float> stat : e.getStats().entrySet()) {
				float sumValue = stats.getOrDefault("Sum " + stat.getKey(), 0f);
				float mean = sumValue / numProtozoa;
				stats.put("Mean " + stat.getKey(), mean);
				float currVar = stats.getOrDefault("Var " + stat.getKey(), 0f);
				float deltaVar = (float) Math.pow(stat.getValue() - mean, 2) / numProtozoa;
				stats.put("Var " + stat.getKey(), currVar + deltaVar);
			}
		}
		return stats;
	}
	
	public int numberOfProtozoa() {
		return getCount(Protozoan.class);
	}


	public long getGeneration() {
		return generation;
	}

	public boolean notCollidingWithAnything(Vector2 pos, float r) {
		boolean anyCellCollisions = Streams.concat(cells.stream(), cellsToAdd.stream())
				.noneMatch(cell -> Geometry.doCirclesCollide(pos, r, cell.getPos(), cell.getRadius()));
		if (!anyCellCollisions)
			return false;

		return rocks.stream().noneMatch(rock -> rock.intersectsWith(pos, r));
	}

	public float getElapsedTime() {
		return elapsedTime;
	}

	public void setGenomeFile(String genomeFile) {
		this.genomeFile = genomeFile;
	}

	public ChemicalSolution getChemicalSolution() {
		return chemicalSolution;
	}

	public List<Rock> getRocks() {
		return rocks;
	}

	public void registerCrossoverEvent() {
		crossoverEvents++;
	}

	public World getWorld() {
		return world;
	}

	public Collection<Cell> getCells() {
		return cells;
	}

	public Collection<? extends Particle> getParticles() {
		return cells;
	}

	public void ensureAddedToEnvironment(Particle particle) {
		if (particle instanceof Cell) {
			Cell cell = (Cell) particle;
			if (!cells.contains(cell))
				add(cell);
		}
	}

	public JointsManager getJointsManager() {
		return jointsManager;
	}

	public <T extends Cell> void requestBurst(Cell parent,
											  Class<T> cellType,
											  Function<Float, T> createChild,
											  boolean overrideMinParticleSize) {

		if (getLocalCount(cellType, parent.getPos()) >= getLocalCapacity(cellType))
			return;

		if (burstRequests.stream().anyMatch(request -> request.parentEquals(parent)))
			return;

		BurstRequest<T> request = new BurstRequest<>(parent, cellType, createChild, overrideMinParticleSize);
		burstRequests.add(request);
	}

	public <T extends Cell> void requestBurst(Cell parent, Class<T> cellType, Function<Float, T> createChild) {
		requestBurst(parent, cellType, createChild, false);
	}

	public SpatialHash<Cell> getSpatialHash(Class<? extends Cell> clazz) {
		return spatialHashes.get(clazz);
	}
}
