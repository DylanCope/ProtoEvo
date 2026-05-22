package com.protoevo.core;

import com.badlogic.gdx.math.Vector2;
import com.protoevo.biology.cells.Protozoan;
import com.protoevo.biology.nn.NetworkGenome;
import com.protoevo.env.Environment;
import com.protoevo.utils.FileIO;
import com.protoevo.utils.Utils;

import java.util.*;
import java.util.stream.Collectors;

public class SimulationHistory {

    // In-memory snapshot retention. Snapshots fire every
    // `misc.statisticsSnapshotTime` sim-seconds (default 20s), and the previous
    // implementation kept *every* snapshot for the whole run — at default
    // settings that's 180/hour, ~4300/day, plus the protozoa summary stats
    // stored per snapshot (which include log-distributions per trait). After
    // an overnight session this map alone would balloon to hundreds of MB and
    // dominate save time, GC pauses, and plot extraction.
    //
    // Disk writes are unaffected — every snapshot still goes to
    // stats/summaries/<timestamp>.json. This cap only governs what stays in
    // RAM for live plotting / extractData() calls.
    private static final int MAX_IN_MEMORY_SNAPSHOTS = 500;

    private final LinkedHashMap<String, Statistics> statistics =
            new LinkedHashMap<String, Statistics>(MAX_IN_MEMORY_SNAPSHOTS * 2, 0.75f, false) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Statistics> eldest) {
                    return size() > MAX_IN_MEMORY_SNAPSHOTS;
                }
            };
    private final String statsFolder;

    private final Set<String> commonStatsKeys;

    public SimulationHistory(String savesFolder) {
        this.statsFolder = savesFolder + "/stats";
        commonStatsKeys = new HashSet<>();
    }

    public void makeStatisticsSnapshot(Environment environment) {
        Statistics stats = new Statistics(environment.getStats());
        stats.putAll(environment.getDebugStats());
        stats.putAll(environment.getPhysicsDebugStats());
        stats.putAll(environment.getProtozoaSummaryStats(true, false, true));

        String timeStamp = Utils.getTimeStampString();

        FileIO.writeJson(stats, statsFolder + "/summaries/" + timeStamp);

        statistics.put(timeStamp, stats);

        if (commonStatsKeys.isEmpty()) {
            commonStatsKeys.addAll(stats.getStatsMap().keySet());
        } else {
            commonStatsKeys.retainAll(stats.getStatsMap().keySet());
        }

        if (Environment.settings.misc.writeGenomes.get()) {
            List<NetworkGenome> protozoaGenomes = environment.getCells().stream()
                    .filter(cell -> cell instanceof Protozoan)
                    .map(cell -> ((Protozoan) cell).getGeneExpressionFunction().getGRNGenome())
                    .collect(Collectors.toList());
            FileIO.writeJson(protozoaGenomes, statsFolder + "/protozoa-genomes/" + timeStamp);
        }

//		PythonRunner.runPython("pyprotoevo.create_plots", "--quiet --simulation " + name);
    }

    public Collection<String> getCommonStatisticsKeys() {
        return commonStatsKeys;
    }

    public ArrayList<Vector2> extractData(String variable1, String variable2) {
        ArrayList<Vector2> data = new ArrayList<>();
        for (Statistics stats : statistics.values()) {
            if (stats.isNumeric(variable1) && stats.isNumeric(variable2)) {
                data.add(new Vector2(
                        (float) stats.getStat(variable1).getDouble(),
                        (float) stats.getStat(variable2).getDouble()
                ));
            }
        }
        return data;
    }
}
