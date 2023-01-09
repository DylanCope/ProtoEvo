package com.protoevo.env;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.google.common.collect.Streams;
import com.protoevo.biology.BurstRequest;
import com.protoevo.biology.Cell;
import com.protoevo.biology.MeatCell;
import com.protoevo.biology.PlantCell;
import com.protoevo.biology.evolution.Evolvable;
import com.protoevo.biology.protozoa.Protozoan;
import com.protoevo.core.Particle;
import com.protoevo.core.settings.Constants;
import com.protoevo.core.settings.EnvironmentSettings;
import com.protoevo.core.settings.Settings;
import com.protoevo.core.Simulation;
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
	public final ConcurrentHashMap<Class<? extends Cell>, Integer> cellCounts =
			new ConcurrentHashMap<>(3, 1);
	public final ConcurrentHashMap<Class<? extends Cell>, Integer> cellCapacities =
			new ConcurrentHashMap<>(3, 1);
	private final ChemicalSolution chemicalSolution = null;
	private final List<Rock> rocks;
	private long generation = 1, protozoaBorn = 0, totalCellsAdded = 0, crossoverEvents = 0;

	private String genomeFile = null;
	private final List<String> genomesToWrite = new ArrayList<>();

	private final List<Cell> cellsToAdd = new ArrayList<>();
	private final List<Cell> cells = new ArrayList<>();
	private boolean hasInitialised;

	private final JointsManager jointsManager;
	private final InteractionsManager interactionsManager;
	private final ConcurrentLinkedQueue<BurstRequest<? extends Cell>> burstRequests = new ConcurrentLinkedQueue<>();

	public Environment()
	{
		world = new World(new Vector2(0, 0), true);
		world.setContinuousPhysics(false);

		interactionsManager = new InteractionsManager(this);
		jointsManager = new JointsManager(this);
		world.setContactListener(new CollisionHandler(this));

//		TODO: revisit chemical field implementation
//		if (Settings.enableChemicalField) {
//			float chemicalGridSize = 2 * radius / Settings.numChemicalBreaks;
//			chemicalSolution = new ChemicalSolution(-radius, radius, -radius, radius, chemicalGridSize);
//		} else {
//			chemicalSolution = null;
//		}

		rocks = new ArrayList<>();

		elapsedTime = 0;
		hasInitialised = false;
	}

	public void update(float delta)
	{
		flushEntitiesToAdd();

		elapsedTime += delta;
		world.step(delta, Constants.VELOCITY_ITERATIONS, Constants.POSITION_ITERATIONS);

//		interactionsManager.update(delta);
		cells.parallelStream().forEach(cell -> cell.update(delta));
		burstRequests.forEach(BurstRequest::burst);
		burstRequests.clear();

		cells.forEach(this::handleDeadEntity);
		cells.removeIf(Cell::isDead);
		updateCounts(cells);

		cells.forEach(Particle::reset);

		jointsManager.flushJoints();

	}

	public Vector2[] createRocks() {
		System.out.println("Creating rocks structures...");
		RockGeneration.generateClustersOfRocks(
				this, new Vector2(0, 0), 1, EnvironmentSettings.rockClusterRadius);

		int numClusterCentres = 8;
		Vector2[] clusterCentres = new Vector2[numClusterCentres];

		for (int i = 0; i < numClusterCentres; i++) {
			float minR = EnvironmentSettings.rockClusterRadius;
			float maxR = EnvironmentSettings.environmentSize / 5f - EnvironmentSettings.rockClusterRadius;

			Vector2 centre = RockGeneration.randomPosition(minR, maxR);
			clusterCentres[i] = centre;

			int nRings = Simulation.RANDOM.nextInt(1, 3);
			float radiusRange = Simulation.RANDOM.nextFloat(8.f) * EnvironmentSettings.rockClusterRadius;
			RockGeneration.generateClustersOfRocks(this, centre, nRings, radiusRange);
		}

		RockGeneration.generateRocks(this, 500);

		for (Rock rock : this.getRocks()) {
			BodyDef rockBodyDef = new BodyDef();
			Body rockBody = world.createBody(rockBodyDef);
			PolygonShape rockShape = new PolygonShape();
			rockShape.set(rock.getPoints());
			rockBody.setUserData(rock);
			rockBody.createFixture(rockShape, 0.0f);
		}
		return clusterCentres;
	}

	public Vector2[] initialise() {
		System.out.print("Initialising environment... ");
		Vector2[] populationStartCentres = createRocks();

		if (populationStartCentres != null)
			initialisePopulation(Arrays.copyOfRange(populationStartCentres, 0, EnvironmentSettings.numPopulationClusters));
		else
			initialisePopulation();

		flushEntitiesToAdd();

		if (Settings.writeGenomes && genomeFile != null)
			writeGenomeHeaders();

		hasInitialised = true;

//		for (int i = 0; i < 3; i++)
//			update(Settings.simulationUpdateDelta);

		return populationStartCentres;
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

		for (int i = 0; i < EnvironmentSettings.numInitialPlantPellets; i++) {
			PlantCell cell = new PlantCell(this);
			Vector2 pos = findPlantPosition.apply(cell.getRadius());
			if (pos != null)
				cell.setPos(pos);
			else {
				System.out.println(
					"Failed to find position for plant pellet. " +
					"Try increasing the number of population clusters or reduce the number of initial plants.");
				break;
			}
		}

		for (int i = 0; i < EnvironmentSettings.numInitialProtozoa; i++) {
			Protozoan p = Evolvable.createNew(Protozoan.class);
			p.setEnv(this);
			Vector2 pos = findProtozoaPosition.apply(p.getRadius());
			if (pos != null)
				p.setPos(pos);
			else {
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
		Vector2[] clusterCentres = new Vector2[EnvironmentSettings.numPopulationClusters];
		for (int i = 0; i < clusterCentres.length; i++)
			clusterCentres[i] = randomPosition(EnvironmentSettings.populationClusterRadius);
		initialisePopulation(clusterCentres);
	}

	public Vector2 randomPosition(float entityRadius, Vector2[] clusterCentres) {
		int clusterIdx = Simulation.RANDOM.nextInt(clusterCentres.length);
		Vector2 clusterCentre = clusterCentres[clusterIdx];
		return randomPosition(entityRadius, clusterCentre, EnvironmentSettings.populationClusterRadius);
	}

	public Vector2 randomPosition(float entityRadius, Vector2 centre, float clusterRadius) {
		for (int i = 0; i < 20; i++) {
			Vector2 pos = Geometry.randomVector(clusterRadius * Simulation.RANDOM.nextFloat()).add(centre);
			if (notCollidingWithAnything(pos, entityRadius))
				return pos;
		}

		return null;
	}

	public Vector2 randomPosition(float entityRadius) {
		return randomPosition(entityRadius, Geometry.ZERO, EnvironmentSettings.rockClusterRadius);
	}

	private void flushEntitiesToAdd() {
		cells.addAll(cellsToAdd);
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

	private void updateCounts(Collection<Cell> entities) {
		cellCounts.clear();
		for (Cell e : entities)
			cellCounts.put(e.getClass(), 1 + cellCounts.getOrDefault(e.getClass(), 0));
	}

	private void handleDeadEntity(Particle e) {
		if (!e.isDead())
			return;
		e.dispose();
	}

	private void handleNewProtozoa(Protozoan p) {
		protozoaBorn++;
		generation = Math.max(generation, p.getGeneration());

		if (genomeFile != null && Settings.writeGenomes) {
			String genomeLine = p.getGeneration() + "," + elapsedTime + "," + p.getGenome().toString();
			genomesToWrite.add(genomeLine);
		}
	}

	public void add(Cell e) {
		if (cellCounts.getOrDefault(e.getClass(), 0)
				>= cellCapacities.getOrDefault(e.getClass(), 0)) {
			e.kill();
		}

		if (!cellsToAdd.contains(e)) {
			totalCellsAdded++;
			cellsToAdd.add(e);

			if (e instanceof Protozoan)
				handleNewProtozoa((Protozoan) e);
		}
	}

	public Map<String, Float> getStats(boolean includeProtozoaStats) {
		Map<String, Float> stats = new TreeMap<>();
		stats.put("Protozoa", (float) numberOfProtozoa());
		stats.put("Plants", (float) cellCounts.getOrDefault(PlantCell.class, 0));
		stats.put("Meat Pellets", (float) cellCounts.getOrDefault(MeatCell.class, 0));
		stats.put("Max Generation", (float) generation);
		stats.put("Time Elapsed", elapsedTime);
		stats.put("Protozoa Born", (float) protozoaBorn);
		stats.put("Crossover Events", (float) crossoverEvents);
		if (includeProtozoaStats)
			stats.putAll(getProtozoaStats());
		return stats;
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
		return cellCounts.getOrDefault(Protozoan.class, 0);
	}
	
	public int numberOfPellets() {
		int nPellets = cellCounts.getOrDefault(PlantCell.class, 0);
		nPellets += cellCounts.getOrDefault(MeatCell.class, 0);
		return nPellets;
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

	public InteractionsManager getForceManager() {
		return interactionsManager;
	}

	public <T extends Cell> void requestBurst(Cell parent, Class<T> cellType, Function<Float, T> createChild) {
		if (burstRequests.stream().anyMatch(request -> request.parentEquals(parent)))
			return;

		BurstRequest<T> request = new BurstRequest<>(parent, cellType, createChild);
		if (!burstRequests.contains(request))
			burstRequests.add(request);
	}

}
