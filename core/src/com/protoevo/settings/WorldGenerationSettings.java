package com.protoevo.settings;

import com.protoevo.env.Environment;
import com.protoevo.utils.Utils;

public class WorldGenerationSettings extends Settings {
    public final Parameter<Long> seed = new Parameter<>(
            "Seed", "World Generation Random Seed", Utils::randomLong);
    public final Parameter<Float> radius = new Parameter<>(
            "Environment Radius", "Size of the environment.", 10.0f
    );
    public final Parameter<Integer> numInitialProtozoa = new Parameter<>(
            "Initial Protozoa", "Number of protozoa spawned on world generation.", 800);
    public final Parameter<Integer> numInitialPlantPellets = new Parameter<>(
            "Initial Plants", "Number of plants spawned on world generation.", 2000);
    public final Parameter<Integer> numRingClusters = new Parameter<>(
            "Ring Clusters", "Number of ring clusters.", 4);
    public final Parameter<Integer> numPopulationStartClusters = new Parameter<>(
            "Population Start Centres", "Number of spawn centres for cells.", 1);
    public final Parameter<Float> minRockClusterRadius = new Parameter<>(
            "Minimum Ring Radius", "Size of Rock Clusters", radius.get() / 5f
    );
    public final Parameter<Float> maxRockClusterRadius = new Parameter<>(
            "Maximum Ring Radius", "Size of Rock Clusters", radius.get() / 3f
    );
    public final Parameter<Float> populationClusterRadius = new Parameter<>(
            "Population Cluster Radius",
            "Size of initial population spawn clusters.",
            radius.get()
    );
    public final Parameter<Float> maxRockSize = new Parameter<>(
            "Maximum Rock Size", "", Environment.settings.maxParticleRadius.get() * 2.25f);
    public final Parameter<Float> minRockSize = new Parameter<>(
            "Minimum Rock Size", "", maxRockSize.get() / 3f);
    public final Parameter<Float> attachedRockSizeChange = new Parameter<>(
            "Attached Rock Size Variation", "", 0.6f
    );
    public final Parameter<Float> minRockSpikiness = new Parameter<>(
            "Min Rock Spikiness",
            "",
            (float) Math.toRadians(5));
    public final Parameter<Float> minRockOpeningSize = new Parameter<>(
            "Minimum Opening Size",
            "Amount of space that must be left between rocks",
            maxRockSize.get() * 1.2f
    );
    public final Parameter<Boolean> closedRingBorder = new Parameter<>(
            "Closed Ring Border",
            "Whether or not to close the world in a ring of rocks.",
            true
    );
    public final Parameter<Integer> rockGenerationIterations = new Parameter<>(
            "Rock Generation Iterations", "", 200
    );
    public final Parameter<Float> rockClustering = new Parameter<>(
            "Rock Clustering", "", 0.95f
    );
    public final Parameter<Float> ringBreakProbability = new Parameter<>(
            "Ring Break Probability", "", 0.05f
    );
    public final Parameter<Float> ringBreakAngleMinSkip = new Parameter<>(
            "Ring Break Min Skip",
            "Minimum angle (in radians) of a break in a rock ring",
            (float) Math.toRadians(8)
    );
    public final Parameter<Float> ringBreakAngleMaxSkip = new Parameter<>(
            "Ring Break Max Skip",
            "Maximum angle (in radians) of a break in a rock ring",
            (float) Math.toRadians(15)
    );
    public final Parameter<Float> voidStartDistance = new Parameter<>(
            "Void Start Distance",
            "Distance from the centre of the environment where the void starts.",
            1.25f * radius.get());

    public final Parameter<Float> spatialHashRadius = new Parameter<>(
            "",
            "",
            voidStartDistance.get()
    );
    public final Parameter<Float> chemicalFieldRadius = new Parameter<>(
            "",
            "",
            voidStartDistance.get());
    public final Parameter<Integer> chemicalFieldResolution = new Parameter<>(
            "Chemical Field Resolution",
            "How many cells wide the chemical field is.",
            1024);
    public final Parameter<Boolean> generateLightNoiseTexture = new Parameter<>(
            "Generate Light Noise",
            "Whether or not light noise texture is generated",
            true);
    public final Parameter<Boolean> bakeRockLights = new Parameter<>(
            "Bake Rock Shadows",
            "Whether or not shadows are baked from rocks",
            true);
    public final Parameter<Integer> lightMapResolution = new Parameter<>(
            "Light Map Resolution",
            "How many cells wide the light map is.",
            256);

    public WorldGenerationSettings() {
        super("World Generation");
    }
}
