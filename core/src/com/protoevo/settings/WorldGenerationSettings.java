package com.protoevo.settings;

import com.protoevo.env.Environment;
import com.protoevo.utils.Utils;

public class WorldGenerationSettings extends Settings {
    public final Settings.Parameter<Long> seed = new Settings.Parameter<>(
            "Seed", "World Generation Random Seed", Utils::randomLong);
    public final Settings.Parameter<Float> radius = new Settings.Parameter<>(
            "Environment Radius", "Size of the environment.", 10.0f
    );
    public final Settings.Parameter<Integer> numInitialProtozoa = new Settings.Parameter<>(
            "Initial Protozoa", "Number of protozoa spawned on world generation.", 800);
    public final Settings.Parameter<Integer> numInitialPlantPellets = new Settings.Parameter<>(
            "Initial Plants", "Number of plants spawned on world generation.", 2000);
    public final Settings.Parameter<Integer> numRingClusters = new Settings.Parameter<>(
            "Ring Clusters", "Number of ring clusters.", 4);
    public final Settings.Parameter<Integer> numPopulationStartClusters = new Settings.Parameter<>(
            "Population Start Centres", "Number of spawn centres for cells.", 1);
    public final Settings.Parameter<Float> minRockClusterRadius = new Settings.Parameter<>(
            "Minimum Ring Radius", "Size of Rock Clusters", radius.get() / 5f
    );
    public final Settings.Parameter<Float> maxRockClusterRadius = new Settings.Parameter<>(
            "Maximum Ring Radius", "Size of Rock Clusters", radius.get() / 3f
    );
    public final Settings.Parameter<Float> populationClusterRadius = new Settings.Parameter<>(
            "Population Cluster Radius",
            "Size of initial population spawn clusters.",
            radius.get()
    );
    public final Settings.Parameter<Float> maxRockSize = new Settings.Parameter<>(
            "Maximum Rock Size", "", Environment.settings.maxParticleRadius.get() * 2.25f);
    public final Settings.Parameter<Float> minRockSize = new Settings.Parameter<>(
            "Minimum Rock Size", "", maxRockSize.get() / 3f);
    public final Settings.Parameter<Float> attachedRockSizeChange = new Settings.Parameter<>(
            "Attached Rock Size Variation", "", 0.6f
    );
    public final Settings.Parameter<Float> minRockSpikiness = new Settings.Parameter<>(
            "Min Rock Spikiness",
            "",
            (float) Math.toRadians(5));
    public final Settings.Parameter<Float> minRockOpeningSize = new Settings.Parameter<>(
            "Minimum Opening Size",
            "Amount of space that must be left between rocks",
            maxRockSize.get() * 1.2f
    );
    public final Settings.Parameter<Boolean> closedRingBorder = new Settings.Parameter<>(
            "Closed Ring Border",
            "Whether or not to close the world in a ring of rocks.",
            true
    );
    public final Settings.Parameter<Integer> rockGenerationIterations = new Settings.Parameter<>(
            "Rock Generation Iterations", "", 200
    );
    public final Settings.Parameter<Float> rockClustering = new Settings.Parameter<>(
            "Rock Clustering", "", 0.95f
    );
    public final Settings.Parameter<Float> ringBreakProbability = new Settings.Parameter<>(
            "Ring Break Probability", "", 0.05f
    );
    public final Settings.Parameter<Float> ringBreakAngleMinSkip = new Settings.Parameter<>(
            "Ring Break Min Skip",
            "Minimum angle (in radians) of a break in a rock ring",
            (float) Math.toRadians(8)
    );
    public final Settings.Parameter<Float> ringBreakAngleMaxSkip = new Settings.Parameter<>(
            "Ring Break Max Skip",
            "Maximum angle (in radians) of a break in a rock ring",
            (float) Math.toRadians(15)
    );
    public final Settings.Parameter<Float> voidStartDistance = new Settings.Parameter<>(
            "Void Start Distance",
            "Distance from the centre of the environment where the void starts.",
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
    public final Settings.Parameter<Integer> chemicalFieldResolution = new Settings.Parameter<>(
            "Chemical Field Resolution",
            "How many cells wide the chemical field is.",
            1024);
    public final Settings.Parameter<Boolean> generateLightNoiseTexture = new Settings.Parameter<>(
            "Generate Light Noise",
            "Whether or not light noise texture is generated",
            true);
    public final Settings.Parameter<Boolean> bakeRockLights = new Settings.Parameter<>(
            "Bake Rock Shadows",
            "Whether or not shadows are baked from rocks",
            true);
    public final Settings.Parameter<Integer> lightMapResolution = new Settings.Parameter<>(
            "Light Map Resolution",
            "How many cells wide the light map is.",
            256);

    public WorldGenerationSettings() {
        super("World Generation");
    }
}
