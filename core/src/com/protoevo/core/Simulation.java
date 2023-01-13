package com.protoevo.core;

import com.github.javafaker.Faker;
import com.protoevo.biology.MeatCell;
import com.protoevo.biology.PlantCell;
import com.protoevo.biology.protozoa.Protozoan;
import com.protoevo.core.settings.Settings;
import com.protoevo.core.settings.SimulationSettings;
import com.protoevo.env.Environment;
import com.protoevo.utils.FileIO;
import com.protoevo.utils.Utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Simulation
{
	private final Environment environment;
	private boolean simulate, pause = false;
	private float timeDilation = 1, timeSinceSave = 0, timeSinceSnapshot = 0;
	private double updateDelay = Application.refreshDelay / 1000.0, lastUpdateTime = 0;
	
	public static Random RANDOM = new Random(SimulationSettings.simulationSeed);
	private boolean debug = false, delayUpdate = true;

	private final String name;
	private final String genomeFile, historyFile;
	private List<String> statsNames;

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
		environment.cellCapacities.put(Protozoan.class, SimulationSettings.maxProtozoa);
		environment.cellCapacities.put(PlantCell.class, SimulationSettings.maxPlants);
		environment.cellCapacities.put(MeatCell.class, SimulationSettings.maxMeat);
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

	public void setupEnvironment() {
		environment.initialise();
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

	public Environment loadMostRecentEnv() {
		Path dir = Paths.get("saves/" + name + "/env");
		if (Files.exists(dir))
			try (Stream<Path> pathStream = Files.list(dir)) {
				Optional<String> lastFilePath = pathStream
						.filter(f -> !Files.isDirectory(f))
						.max(Comparator.comparingLong(f -> f.toFile().lastModified()))
						.map(path -> path.toString().replace(".dat", ""));

				return lastFilePath.map(this::loadEnv).orElse(newDefaultEnv());
			} catch (IOException e) {
				return newDefaultEnv();
			}
		return newDefaultEnv();
	}

	public void simulate() {
		setupEnvironment();
		makeHistorySnapshot();
		while (simulate) {
			if (pause)
				continue;

			if (delayUpdate && updateDelay > 0) {
				double currTime = Utils.getTimeSeconds();
				if ((currTime - lastUpdateTime) > updateDelay) {
					update(Settings.simulationUpdateDelta);
					lastUpdateTime = currTime;
				}
			} else {
				update(Settings.simulationUpdateDelta);
			}

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
			(k, v) -> System.out.printf("%s: %.5f\n", k, v)
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
		Map<String, Float> stats = environment.getStats(true);

		if (statsNames == null) {
			statsNames = new ArrayList<>(environment.getStats(true).keySet());
			String statsCsvHeader = String.join(",", statsNames);
			FileIO.appendLine(historyFile, statsCsvHeader);
		}

		String statsString = statsNames.stream()
				.map(k -> String.format("%.5f", stats.get(k)))
				.collect(Collectors.joining(","));

		FileIO.appendLine(historyFile, statsString);
	}

	public void toggleDebug() {
		debug = !debug;
	}

	public synchronized void togglePause() {
		pause = !pause;
	}

	public boolean inDebugMode() {
		return debug;
	}

	public Environment getEnv() { return environment; }

	public long getGeneration() { return environment.getGeneration(); }

	public float getElapsedTime() { return environment.getElapsedTime(); }

	public float getTimeDilation() { return timeDilation; }

	public void setTimeDilation(float td) { timeDilation = td; }

	public void setUpdateDelay(float updateDelay) {
		this.updateDelay = updateDelay;
	}

	public void toggleUpdateDelay() {
		delayUpdate = !delayUpdate;
	}

    public synchronized boolean isPaused() {
		return pause;
    }

	public void dispose() {
		close();
		environment.getWorld().dispose();
	}
}
