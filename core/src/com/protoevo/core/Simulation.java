package com.protoevo.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;
import com.protoevo.settings.Settings;
import com.protoevo.settings.SimulationSettings;
import com.protoevo.env.Environment;
import com.protoevo.ui.GraphicsAdapter;
import com.protoevo.utils.FileIO;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Stream;

public class Simulation implements Runnable
{
	private final Environment environment;
	private ApplicationManager manager;
	private volatile boolean simulate;
	private static boolean paused = false;
	private float timeDilation = 1, timeSinceSave = 0, timeSinceSnapshot = 0;
	private double updateDelay = GraphicsAdapter.refreshDelay / 1000.0, lastUpdateTime = 0;
	
	public static Random RANDOM = new Random(SimulationSettings.simulationSeed);
	private boolean debug = false, delayUpdate = true, initialised = false;

	private final String name;
	private final String genomeFile, historyFile;
	private List<String> statsNames;
	private final REPL repl = new REPL(this);

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
			Files.createDirectories(Paths.get("saves/" + name + "/stats"));
			Files.createDirectories(Paths.get("saves/" + name + "/stats/summaries"));

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
			Environment env = EnvFileIO.deserialize(filename);
			env.rebuildWorld();
			System.out.println("Loaded tank at: " + filename);
			initialised = true;
			return env;
		} catch (Exception e) {
			System.err.println("Unable to load environment at " + filename + " because: " + e.getMessage());
			throw new RuntimeException(e);
		}
	}

	public void setManager(ApplicationManager manager) {
		this.manager = manager;
		repl.setManager(manager);
	}

	public Environment loadMostRecentEnv() {
		Path dir = Paths.get("saves/" + name + "/env");
		Optional<String> lastFilePath = Optional.empty();
		if (Files.exists(dir))
			try (Stream<Path> pathStream = Files.list(dir)) {
				lastFilePath = pathStream
						.filter(f -> !Files.isDirectory(f))
						.max(Comparator.comparingLong(f -> f.toFile().lastModified()))
						.map(path -> path.toString().replace(".env", ""));
			} catch (IOException e) {
				throw new RuntimeException("Invalid input directory: " + dir, e);
			}

		if (lastFilePath.isPresent())
			return loadEnv(lastFilePath.get());
		else
			throw new RuntimeException("No environment files found in " + dir);
	}

	public void prepare()
	{
		if (!initialised) {
			environment.initialise();
			makeHistorySnapshot();
			initialised = true;
		}
		new Thread(repl).start();

		if (manager != null) {
			manager.notifySimulationReady();
		}
	}

	public void run() {
		while (simulate) {
			if (paused)
				continue;

			update();

			if (environment.numberOfProtozoa() <= 0 && Settings.finishOnProtozoaExtinction) {
				simulate = false;
				System.out.println();
				System.out.println("Finished simulation. All protozoa died.");
				printStats();
			}
		}
	}

	public void printStats() {
		environment.getStats().forEach(
			stat -> System.out.println(stat.toString())
		);
	}

	public void update()
	{
		if (isPaused())
			return;

		float delta = timeDilation * SimulationSettings.simulationUpdateDelta;

		try {
			environment.update(delta);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Error occurred during simulation. Saving and exiting.");
			save();
			throw e;
		}

		timeSinceSave += delta;
		if (timeSinceSave > Settings.timeBetweenSaves) {
			timeSinceSave = 0;
			save();
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
		System.out.println("\nClosing simulation.");
		String saveFile = save();
		System.out.println("Saved environment to: " + saveFile);
		environment.getWorld().dispose();
		repl.close();
	}

	public String getTimeStampString() {
		return new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date());
	}

	public String save() {
		String timeStamp = getTimeStampString();
		String fileName = "saves/" + name + "/env/" + timeStamp;
		EnvFileIO.serialize(environment, fileName);
		return fileName;
	}

	public void makeHistorySnapshot() {
		Statistics stats = new Statistics(environment.getStats());
		stats.putAll(environment.getDebugStats());
		stats.putAll(environment.getPhysicsDebugStats());
		stats.putAll(environment.getProtozoaSummaryStats(true, false, true));

		FileIO.writeJson(stats, "saves/" + name + "/stats/summaries/" + getTimeStampString());
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

	public boolean isReady() {
		return initialised;
	}

	public String getSaveFolder() {
		return "saves/" + name;
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
}
