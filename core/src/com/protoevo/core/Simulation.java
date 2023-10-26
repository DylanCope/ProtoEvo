package com.protoevo.core;

import com.github.javafaker.Faker;
import com.protoevo.biology.cells.Protozoan;
import com.protoevo.biology.nn.NetworkGenome;
import com.protoevo.env.EnvFileIO;
import com.protoevo.env.Environment;
import com.protoevo.settings.SimulationSettings;
import com.protoevo.utils.EnvironmentImageRenderer;
import com.protoevo.utils.FileIO;
import com.protoevo.utils.TimedEventsManager;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
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

		environmentLoader = () -> loadEnv("saves/" + name + "/env/" + save);
		loadSettings();
	}

	private void loadSettings() {}

	public static void newSaveDir(String simulationName) {
		try {
			System.out.println("Created new simulation named: " + simulationName);
			Files.createDirectories(Paths.get("saves/"));
			Files.createDirectories(Paths.get("saves/" + simulationName));
			Files.createDirectories(Paths.get("saves/" + simulationName + "/env"));
			Files.createDirectories(Paths.get("saves/" + simulationName + "/stats"));
			Files.createDirectories(Paths.get("saves/" + simulationName + "/stats/summaries"));
			Files.createDirectories(Paths.get("saves/" + simulationName + "/stats/protozoa-genomes"));
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
			Environment env = EnvFileIO.reloadEnvironment(filename);
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
		Path dir = Paths.get("saves/" + name + "/stats/summaries");
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
		Path dir = Paths.get("saves/" + name + "/env");
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
	}

	public void run() {
		while (simulate) {
			if (paused)
				continue;

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
			float delta = timeDilation * Environment.settings.simulationUpdateDelta.get();

			try {
				environment.update(delta);
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("Error occurred during simulation. Saving and exiting.");
				save();
				repl.close();
				throw e;
			}

			timedEventsManager.update(delta);
			// timeSinceSave += delta;
			// timeSinceSnapshot += delta;
			// timeSinceAutoSave += delta;

			// if (timeSinceSave >= Environment.settings.misc.timeBetweenHistoricalSaves.get() || saveRequested) {
			// 	timeSinceSave = 0;
			// 	if (saveRequested) {
			// 		saveRequested = false;
			// 		System.out.println("\nSaving environment.");
			// 	}
			// 	save();
			// }

			// if (timeSinceSnapshot >= Environment.settings.misc.statisticsSnapshotTime.get()) {
			// 	timeSinceSnapshot = 0;
			// 	onOtherThread(this::makeStatisticsSnapshot);
			// }
		} catch (Exception e) {
			handleCrash(e);
		}
	}

	public void handleCrash(Exception e) {
		String crashFolder = getSaveFolder() + "/crash";
		try {
			Files.createDirectories(Paths.get(crashFolder));
			FileWriter fileWriter = new FileWriter(crashFolder + "/report.txt");
			PrintWriter printWriter = new PrintWriter(fileWriter);
			printWriter.println("Timestamp: " + new Date());
			printWriter.println("Operating system: " + System.getProperty("os.name"));
			printWriter.println("Operating system version: " + System.getProperty("os.version"));
			printWriter.println("Free memory: " + Runtime.getRuntime().freeMemory());
			printWriter.println("Stack Trace:");
			e.printStackTrace(printWriter);
			printWriter.close();
			System.out.println("Wrote crash report to: " + crashFolder);
		} catch (IOException ioException) {
			System.err.println("Failed to create crash report: " + ioException + "\nWhen handling exception: " + e);
		}
		System.exit(0);
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

	public String getTimeStampString() {
		return new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date());
	}

	public String save() {
		if (environment == null)
			return null;
		String timeStamp = getTimeStampString();
		String fileName = "saves/" + name + "/env/" + timeStamp;

		EnvironmentImageRenderer renderer = new EnvironmentImageRenderer(1024, 1024, environment);
		renderer.render(fileName);
		System.out.println("Created screenshot in directory: " + fileName);

		EnvFileIO.saveEnvironment(environment, fileName);
		return fileName;
	}

	public void createAutoSave() {
		if (environment == null)
			return;
		String fileName = "saves/" + name + "/env/autosave";

		EnvironmentImageRenderer renderer = new EnvironmentImageRenderer(1024, 1024, environment);
		renderer.render(fileName);
		System.out.println("Created screenshot in directory: " + fileName);

		EnvFileIO.saveEnvironment(environment, fileName);
	}

	public void makeStatisticsSnapshot() {
		Statistics stats = new Statistics(environment.getStats());
		stats.putAll(environment.getDebugStats());
		stats.putAll(environment.getPhysicsDebugStats());
		stats.putAll(environment.getProtozoaSummaryStats(true, false, true));

		String timeStamp = getTimeStampString();

		FileIO.writeJson(stats, "saves/" + name + "/stats/summaries/" + timeStamp);

		if (Environment.settings.misc.writeGenomes.get()) {
			List<NetworkGenome> protozoaGenomes = environment.getCells().stream()
					.filter(cell -> cell instanceof Protozoan)
					.map(cell -> ((Protozoan) cell).getGeneExpressionFunction().getGRNGenome())
					.collect(Collectors.toList());
			FileIO.writeJson(protozoaGenomes, "saves/" + name + "/stats/protozoa-genomes/" + timeStamp);
		}

//		PythonRunner.runPython("pyprotoevo.create_plots", "--quiet --simulation " + name);
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
		return "saves/" + name;
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
