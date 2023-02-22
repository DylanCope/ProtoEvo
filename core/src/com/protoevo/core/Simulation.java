package com.protoevo.core;

import com.github.javafaker.Faker;
import com.protoevo.settings.Settings;
import com.protoevo.settings.SimulationSettings;
import com.protoevo.env.ChemicalSolution;
import com.protoevo.env.Environment;
import com.protoevo.ui.SimulationScreen;
import com.protoevo.utils.FileIO;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Simulation implements Runnable
{
	private final Environment environment;
	private SimulationScreen simulationScreen;
	private boolean simulate;
	private static boolean paused = false;
	private float timeDilation = 1, timeSinceSave = 0, timeSinceSnapshot = 0;
	private double updateDelay = Application.refreshDelay / 1000.0, lastUpdateTime = 0;
	
	public static Random RANDOM = new Random(SimulationSettings.simulationSeed);
	private boolean debug = false, delayUpdate = true, initialised = false;

	private final String name;
	private final String genomeFile, historyFile;
	private List<String> statsNames;
	private REPL repl;
	private Thread replThread;

	public Simulation() {
		this(Settings.simulationSeed);
	}

	public Simulation(long seed)
	{
		RANDOM = new Random(seed);
		simulate = true;
		name = generateSimName();
		System.out.println("Created new simulation named: " + name);
		genomeFile = "saves/" + name + "/genomes.csv";
		historyFile = "saves/" + name + "/history.csv";
		newSaveDir();
		environment = newDefaultEnv();
		loadSettings();
	}

	public Simulation(long seed, String name)
	{
		RANDOM = new Random(seed);
		simulate = true;
		this.name = name;
		genomeFile = "saves/" + name + "/genomes.csv";
		historyFile = "saves/" + name + "/history.csv";

		newSaveDir();
		environment = loadMostRecentEnv();
		loadSettings();
	}

	public Simulation(long seed, String name, String save)
	{
		RANDOM = new Random(seed);
		simulate = true;
		this.name = name;
		genomeFile = "saves/" + name + "/genomes.csv";
		historyFile = "saves/" + name + "/history.csv";

		newSaveDir();
		environment = loadEnv("saves/" + name + "/env/" + save);
		loadSettings();
	}

	private void loadSettings() {
//		environment.cellCapacities.put(Protozoan.class, SimulationSettings.maxProtozoa);
//		environment.cellCapacities.put(PlantCell.class, SimulationSettings.maxPlants);
//		environment.cellCapacities.put(MeatCell.class, SimulationSettings.maxMeat);
	}

	private void newSaveDir() {
		try {
			Files.createDirectories(Paths.get("saves/" + name));
			Files.createDirectories(Paths.get("saves/" + name + "/env"));

			Path genomePath = Paths.get(genomeFile);
			if (!Files.exists(genomePath))
				Files.createFile(genomePath);

			Path historyPath = Paths.get(historyFile);
			if (!Files.exists(historyPath))
				Files.createFile(historyPath);

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
		Environment environment = new Environment();
		environment.setGenomeFile(genomeFile);
		return environment;
	}

	public Environment loadEnv(String filename)
	{
		try {
			Environment env = (Environment) FileIO.load(filename);
			System.out.println("Loaded tank at: " + filename);
			return env;
		} catch (IOException | ClassNotFoundException e) {
			System.out.println("Unable to load environment at " + filename + " because: " + e.getMessage());
			return newDefaultEnv();
		}
	}

	public void setSimulationScreen(SimulationScreen screen) {
		this.simulationScreen = screen;
	}

	public Environment loadMostRecentEnv() {
		Path dir = Paths.get("assets/saves/" + name + "/env");
		if (Files.exists(dir))
			try (Stream<Path> pathStream = Files.list(dir)) {
				Optional<String> lastFilePath = pathStream
						.filter(f -> !Files.isDirectory(f))
						.max(Comparator.comparingLong(f -> f.toFile().lastModified()))
						.map(path -> path.toString().replace(".dat", ""));

				if (lastFilePath.isPresent())
					return loadEnv(lastFilePath.get());
				else
					return newDefaultEnv();
			} catch (IOException e) {
				return newDefaultEnv();
			}
		return newDefaultEnv();
	}

	public void prepare()
	{
		if (!initialised) {
			environment.initialise();
			makeHistorySnapshot();
			initialised = true;
			if (simulationScreen != null) {
				simulationScreen.notifySimulationLoaded();
			}
			repl = new REPL(this, simulationScreen);
			replThread = new Thread(repl);
			replThread.start();
		}
	}

	public void run() {

		ChemicalSolution chemicalSolution = environment.getChemicalSolution();
		if (chemicalSolution != null)
			chemicalSolution.initialise();

		while (simulate) {
			if (paused)
				continue;

			update(Settings.simulationUpdateDelta);

			if (environment.numberOfProtozoa() <= 0 && Settings.finishOnProtozoaExtinction) {
				simulate = false;
				System.out.println();
				System.out.println("Finished simulation. All protozoa died.");
				printStats();
			}
		}
	}

	public void printStats() {
		environment.getStats(true).forEach(
			stat -> System.out.println(stat.toString())
		);
	}

	public void update(float delta)
	{
		if (isPaused())
			return;

//		float delta = timeDilation * Settings.simulationUpdateDelta;
		synchronized (environment) {
			environment.update(delta);
		}

		timeSinceSave += delta;
		if (timeSinceSave > Settings.timeBetweenSaves) {
			timeSinceSave = 0;
			saveTank();
		}

		timeSinceSnapshot += delta;
		if (timeSinceSnapshot > Settings.historySnapshotTime) {
			timeSinceSnapshot = 0;
			makeHistorySnapshot();
		}
	}

	public void interruptSimulationLoop() {
		simulate = false;
	}

	public void close() {
		simulate = false;
		System.out.println();
		System.out.println("Closing simulation.");
		saveTank();
	}

	public void saveTank() {
		String timeStamp = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date());
		String fileName = "saves/" + name + "/env/" + timeStamp;
		FileIO.save(environment, fileName);
	}

	public void makeHistorySnapshot() {
//		Statistics stats = environment.getStats(true);
//
//		if (statsNames == null) {
//			statsNames = new ArrayList<>();
//			for (Statistics.Stat stat : stats.getStats())
//				statsNames.add(stat.getName());
//
//			String statsCsvHeader = String.join(",", statsNames);
//			FileIO.appendLine(historyFile, statsCsvHeader);
//		}
//
//		Map<String, Statistics.Stat> statMap = stats.getStatsMap();
//
//		String statsString = statsNames.stream()
//				.map(k -> statMap.get(k).getValueString())
//				.collect(Collectors.joining(","));
//
//		FileIO.appendLine(historyFile, statsString);
	}

	public void toggleDebug() {
		debug = !debug;
	}

	public void togglePause() {
		paused = !paused;
	}

	public boolean inDebugMode() {
		return debug;
	}

	public Environment getEnv() { return environment; }

	public float getElapsedTime() { return environment.getElapsedTime(); }

	public float getTimeDilation() { return timeDilation; }

	public void setTimeDilation(float td) { timeDilation = td; }

	public void setUpdateDelay(float updateDelay) {
		this.updateDelay = updateDelay;
	}

	public void toggleUpdateDelay() {
		delayUpdate = !delayUpdate;
	}

    public static boolean isPaused() {
		return paused;
    }

	public void dispose() {
		close();
		environment.getWorld().dispose();
		repl.close();
		replThread.interrupt();
		System.exit(0);
	}

	public boolean isReady() {
		return initialised;
	}
}
