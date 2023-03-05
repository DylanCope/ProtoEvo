package com.protoevo.settings;

import com.protoevo.env.Environment;

public class WorldGenerationSettings {
    public final Settings.Parameter<Integer> seed = new Settings.Parameter<>(
            "Seed", "World Generation Random Seed", 1);
    public final Settings.Parameter<Integer> numInitialProtozoa = new Settings.Parameter<>(
            "Initial Protozoa", "Number of protozoa spawned on world generation.", 500);
    public final Settings.Parameter<Integer> numInitialPlantPellets = new Settings.Parameter<>(
            "Initial Plants", "Number of plants spawned on world generation.", 2000);
    public final Settings.Parameter<Integer> numRingClusters = new Settings.Parameter<>(
            "Ring Clusters", "Number of ring clusters.", 4);
    public final Settings.Parameter<Integer> numPopulationStartClusters = new Settings.Parameter<>(
            "Population Start Centres", "Number of spawn centres for cells.", 3);
    public final Settings.Parameter<Float> rockClusterRadius = new Settings.Parameter<>(
            "Rock Cluster Size", "Size of Rock Clusters", 1f
    );
    public final Settings.Parameter<Float> radius = new Settings.Parameter<>(
            "Environment Radius", "Size of the environment.", 10.0f
    );
    public final Settings.Parameter<Float> populationClusterRadius = new Settings.Parameter<>(
            "Population Cluster Radius",
            "Size of initial population spawn clusters.",
            radius.get() / 2f
    );
    public final Settings.Parameter<Float> maxRockSize = new Settings.Parameter<>(
            "", "", Environment.settings.maxParticleRadius.get() * 2.5f);
    public final Settings.Parameter<Float> minRockSize = new Settings.Parameter<>(
            "", "", maxRockSize.get() / 5f);
    public final Settings.Parameter<Float> attachedRockSizeChange = new Settings.Parameter<>(
            "", "", 0.4f);
    public final Settings.Parameter<Float> minRockSpikiness = new Settings.Parameter<>(
            "", "", (float) Math.toRadians(5));
    public final Settings.Parameter<Float> minRockOpeningSize = new Settings.Parameter<>(
            "", "", maxRockSize.get() * 0.8f
    );
    public final Settings.Parameter<Integer> rockGenerationIterations = new Settings.Parameter<>(
            "", "", 200
    );
    public final Settings.Parameter<Float> rockClustering = new Settings.Parameter<>(
            "", "", 0.95f
    );
    public final Settings.Parameter<Float> ringBreakProbability = new Settings.Parameter<>(
            "", "", 0.05f
    );
    public final Settings.Parameter<Float> ringBreakAngleMinSkip = new Settings.Parameter<>(
            "", "", (float) Math.toRadians(8)
    );
    public final Settings.Parameter<Float> ringBreakAngleMaxSkip = new Settings.Parameter<>(
            "", "", (float) Math.toRadians(15)
    );
    public final Settings.Parameter<Float> voidStartDistance = new Settings.Parameter<>(
            "",
            "",
            1.25f * radius.get());

    public final Settings.Parameter<Float> spatialHashRadius = new Settings.Parameter<>(
            "",
            "",
            voidStartDistance.get()
    );
    public final Settings.Parameter<Float> chemicalFieldRadius = new Settings.Parameter<>(
            "",
            "",
            voidStartDistance.get());
}
