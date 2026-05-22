package com.protoevo.ui.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.CatmullRomSpline;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.ScreenUtils;
import com.protoevo.biology.cells.Cell;
import com.protoevo.biology.cells.EvolvableCell;
import com.protoevo.biology.cells.MultiCellStructure;
import com.protoevo.biology.evolution.GeneExpressionFunction;
import com.protoevo.biology.nn.NeuralNetwork;
import com.protoevo.biology.nodes.SurfaceNode;
import com.protoevo.biology.cells.Protozoan;
import com.protoevo.biology.organelles.Organelle;
import com.protoevo.core.*;
import com.protoevo.env.serialization.Serialization;
import com.protoevo.env.Environment;
import com.protoevo.maths.Functions;
import com.protoevo.ui.input.ParticleTracker;
import com.protoevo.physics.Particle;
import com.protoevo.ui.GraphicsAdapter;
import com.protoevo.ui.SimulationInputManager;
import com.protoevo.ui.TopBar;
import com.protoevo.ui.UIStyle;
import com.protoevo.ui.nn.MouseOverNeuronHandler;
import com.protoevo.ui.nn.MultiCellGRNRenderer;
import com.protoevo.ui.nn.NetworkRenderer;
import com.protoevo.ui.rendering.*;
import com.protoevo.ui.shaders.BrightnessLayer;
import com.protoevo.ui.shaders.ShaderLayers;
import com.protoevo.ui.shaders.ShockWaveLayer;
import com.protoevo.ui.shaders.VignetteLayer;
import com.protoevo.utils.*;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

public class SimulationScreen extends ScreenAdapter {

    private final GraphicsAdapter graphics;
    private final Simulation simulation;
    private Environment environment;
    private final SimulationInputManager inputManager;
    private final ShaderLayers environmentRenderer;
    private VignetteLayer vignetteLayer;
    private final SpriteBatch uiBatch;
    private final Stage stage;
    private final GlyphLayout layout = new GlyphLayout();
    private final OrthographicCamera camera;
    private final BitmapFont font, debugFont, statsTitle;
    private final TopBar topBar;
    private final int infoTextSize, textAwayFromEdge;
    private final NetworkRenderer networkRenderer;
    private final MouseOverNeuronHandler mouseOverNeuronHandler;
    private final static float pollStatsInterval = .02f;
    private float elapsedTime = 0, pollStatsCounter = 0;
    private final Statistics stats = new Statistics();
    private final TreeMap<String, String> sortedStats = new TreeMap<>();
    private Callable<Statistics> getStats;
    private final Map<String, Callable<Statistics>> statGetters = new HashMap<>();
    private final Statistics debugStats = new Statistics();
    private Particle trackedParticle;
    private final ImageButton saveTrackedParticleButton, addTagButton, multiCellViewerTrackedParticleButton;
    private final TextField saveTrackedParticleTextField, addTagTextField;
    private final Set<ImageButton> buttons = new java.util.HashSet<>();
    private final Map<Supplier<Boolean>, Runnable> conditionalTasks = new HashMap<>();
    private Optional<MultiCellGRNRenderer> multiCellGRNRenderer = Optional.empty();

    private final SelectBox<String> statsSelectBox;
    private final float graphicsHeight;
    private final float graphicsWidth;
    private boolean uiHidden = false;

    private boolean meanderingCamera = false;
    private float meanderingTargetZoom = 1f;
    private Vector2 meanderingTargetPos;
    private final CatmullRomSpline<Vector3> meanderingSpline = new CatmullRomSpline<>();
    private float meanderingT = 0f, meanderTThreshold = 0f;
    private final BrightnessLayer brightnessLayer;

    private final StatsGraphsScreen statsGraphsScreen;


    public SimulationScreen(GraphicsAdapter graphics, Simulation simulation) {
        this.graphics = graphics;
        this.simulation = simulation;
        environment = simulation.getEnv();
        getStats = environment::getStats;

        statsGraphsScreen = new StatsGraphsScreen(graphics, simulation, this);

        CursorUtils.setDefaultCursor();

        graphicsHeight = Gdx.graphics.getHeight();
        graphicsWidth = Gdx.graphics.getWidth();

        camera = new OrthographicCamera();
        camera.setToOrtho(
                false, Environment.settings.worldgen.radius.get(),
                Environment.settings.worldgen.radius.get() * graphicsHeight / graphicsWidth);
        camera.position.set(0, 0, 0);
        camera.zoom = 1f;

        stage = new Stage();
        uiBatch = new SpriteBatch();

        stage.getRoot().addCaptureListener(event -> {
            CursorUtils.setDefaultCursor();
            if (stage.getKeyboardFocus() instanceof TextField
                    && !(event.getTarget() instanceof TextField))
                stage.setKeyboardFocus(null);
            return false;
        });
        infoTextSize = (int) (graphicsHeight / 50f);
        textAwayFromEdge = (int) (graphicsWidth / 60);

        font = UIStyle.createFiraCode(infoTextSize);

        Skin skin = graphics.getSkin();
        debugFont = skin.getFont("debug");

        statsTitle = skin.getFont("statsTitle");

        topBar = new TopBar(stage, font.getLineHeight());

        inputManager = new SimulationInputManager(this);
        brightnessLayer = new BrightnessLayer(camera);
//        vignetteLayer = new VignetteLayer(camera, inputManager.getParticleTracker());
        environmentRenderer = new ShaderLayers(
                new EnvironmentRenderer(camera, simulation.getEnv(), inputManager),
                new ShockWaveLayer(camera),
               // vignetteLayer,
                brightnessLayer
        );

        saveTrackedParticleTextField = new TextField("", skin);
        stage.addActor(saveTrackedParticleTextField);
        saveTrackedParticleTextField.setVisible(false);
        saveTrackedParticleTextField.setMessageText("Save cell as...");

        saveTrackedParticleButton = createImageButton(
                "icons/save.png", topBar.getButtonSize(), topBar.getButtonSize(), event -> {
            if (event.toString().equals("touchDown")) {
                if (trackedParticle != null && trackedParticle.getUserData() instanceof Protozoan) {
                    String name = saveTrackedParticleTextField.getText();
                    Serialization.saveCell(trackedParticle.getUserData(Cell.class),  name);
                    inputManager.registerNewCloneableCell(name, trackedParticle.getUserData(Protozoan.class));
                }
            }
            return true;
        });
        saveTrackedParticleButton.setVisible(false);

        multiCellViewerTrackedParticleButton = createImageButton(
                "icons/multicell-icon.png", topBar.getButtonSize(), topBar.getButtonSize(), event -> {
                    if (event.toString().equals("touchDown")) {
                        Object particleData = trackedParticle.getUserData();
                        if (particleData instanceof Cell) {
                            if (multiCellGRNRenderer.isPresent()) {
                                multiCellGRNRenderer.get().dispose();
                                multiCellGRNRenderer = Optional.empty();
                            } else {
                                Cell cell = (Cell) particleData;
                                MultiCellStructure multiCellStructure = cell.getMulticellularStructure();
//                            graphics.setScreen(new MultiCellViewerScreen(graphics, multiCellStructure));
                                multiCellGRNRenderer = Optional.of(new MultiCellGRNRenderer(
                                        camera, inputManager.getInputLayers(), multiCellStructure
                                ));
                            }
                        }
                    }
                    return true;
                });

        stage.addActor(multiCellViewerTrackedParticleButton);
        multiCellViewerTrackedParticleButton.setVisible(false);

        addTagTextField = new TextField("", skin);
        stage.addActor(addTagTextField);
        addTagTextField.setVisible(false);
        addTagTextField.setMessageText("Add tag...");

        addTagButton = createImageButton(
                "icons/add.png", topBar.getButtonSize(), topBar.getButtonSize(), event -> {
            if (event.toString().equals("touchDown")) {
                if (trackedParticle != null && trackedParticle.getUserData() instanceof Protozoan) {
                    String tag = addTagTextField.getText();
                    trackedParticle.getUserData(Protozoan.class).tag(tag);
                }
            }
            return true;
        });
        addTagButton.setVisible(false);

        statsSelectBox = new SelectBox<>(skin, "statsTitle");
        stage.addActor(statsSelectBox);
        statsSelectBox.setHeight(statsSelectBox.getStyle().font.getLineHeight());
        setEnvStatOptions();

        float boxWidth = (graphicsWidth / 2.0f - 1.2f * graphicsHeight * .4f);
        float boxHeight = 3 * graphicsHeight / 4;
        float boxXStart = graphicsWidth - boxWidth * 1.1f;
        float boxYStart = (graphicsHeight - boxHeight) / 2;
        mouseOverNeuronHandler = new MouseOverNeuronHandler(font);

        networkRenderer = new NetworkRenderer(
                simulation, this, uiBatch, mouseOverNeuronHandler,
                boxXStart, boxYStart, boxWidth, boxHeight, infoTextSize);
    }

    public void moveToPauseScreen() {
        simulation.setPaused(true);
        graphics.setScreen(new PauseScreen(graphics, this));
    }

    public void updateEnvironment() {
        this.environment = simulation.getEnv();
        ((EnvironmentRenderer) environmentRenderer.getBaseRenderer()).setEnvironment(environment);
        getStats = environment::getStats;
        setEnvStatOptions();
    }

    public EnvironmentRenderer getBaseEnvironmentRenderer() {
        return (EnvironmentRenderer) environmentRenderer.getBaseRenderer();
    }

    // ===== Lineage tree overlay =====
    // Phylogenetic tree restricted to *currently-living* protozoa and their
    // branch-point ancestors. Time on X (oldest branch point left, now
    // right); each visible lineage is a coloured horizontal segment. Linear
    // chains (parent → only-child → only-child …) are collapsed: the tree
    // only shows actual divergence events plus living leaves, so the
    // visible row count is bounded by living population × branchiness
    // rather than total cells ever born.
    private boolean lineageOverlay = false;
    private com.badlogic.gdx.graphics.glutils.ShapeRenderer lineageShape;
    private static final int LINEAGE_MAX_VISIBLE_LEAVES = 80;
    // Per-render scratch state.
    private final HashSet<Long> lineageOnLivingPath = new HashSet<>();
    private final HashMap<Long, ArrayList<com.protoevo.env.LineageRecord>> lineageLivingChildren =
            new HashMap<>();
    private final ArrayList<com.protoevo.env.LineageRecord> lineageVisibleRoots = new ArrayList<>();

    public void toggleLineageOverlay() {
        lineageOverlay = !lineageOverlay;
        if (lineageOverlay && lineageShape == null)
            lineageShape = new com.badlogic.gdx.graphics.glutils.ShapeRenderer();
        System.out.println("Lineage tree: " + (lineageOverlay ? "ON" : "OFF"));
    }
    public boolean isLineageOverlay() { return lineageOverlay; }

    private static void lineageColor(long id, com.badlogic.gdx.graphics.Color out) {
        // Hashed → saturated colour. Stable per lineage so the eye can
        // follow a branch across the tree.
        float r = ((id * 2654435761L) >>> 0  & 0xff) / 255f;
        float g = ((id * 40503L)        >>> 8  & 0xff) / 255f;
        float b = ((id * 2246822519L)   >>> 16 & 0xff) / 255f;
        float maxC = Math.max(r, Math.max(g, b));
        if (maxC > 1e-3f) { r /= maxC; g /= maxC; b /= maxC; }
        out.set(r, g, b, 0.95f);
    }

    private void rebuildLineageChildren(java.util.Map<Long, com.protoevo.env.LineageRecord> recs) {
        lineageOnLivingPath.clear();
        lineageLivingChildren.clear();
        lineageVisibleRoots.clear();
        // Walk from each currently-alive record up through its ancestors,
        // marking every record on a path to a living leaf. We render only
        // these. Dead branches (subtrees with no living leaves) are
        // automatically excluded.
        for (com.protoevo.env.LineageRecord r : recs.values()) {
            if (!r.isAlive()) continue;
            long id = r.id;
            while (id != 0L) {
                if (!lineageOnLivingPath.add(id)) break; // already marked
                com.protoevo.env.LineageRecord step = recs.get(id);
                if (step == null) break;
                id = step.parentId;
            }
        }
        // Build the parent → living-children map and find roots.
        for (Long id : lineageOnLivingPath) {
            com.protoevo.env.LineageRecord r = recs.get(id);
            if (r == null) continue;
            if (r.parentId == 0L || !lineageOnLivingPath.contains(r.parentId)) {
                lineageVisibleRoots.add(r);
            } else {
                lineageLivingChildren.computeIfAbsent(r.parentId, k -> new ArrayList<>()).add(r);
            }
        }
        for (ArrayList<com.protoevo.env.LineageRecord> kids : lineageLivingChildren.values())
            kids.sort((a, b) -> Integer.compare(b.aliveDescendants, a.aliveDescendants));
        lineageVisibleRoots.sort((a, b) -> Integer.compare(b.aliveDescendants, a.aliveDescendants));
    }

    /** Walk down single-child chains starting at r, returning the first
     *  descendant that either branches (≥2 living children) or is a
     *  living leaf. Used to collapse linear chains so the tree shows only
     *  divergence events. */
    private com.protoevo.env.LineageRecord collapseChain(com.protoevo.env.LineageRecord r) {
        while (true) {
            ArrayList<com.protoevo.env.LineageRecord> kids = lineageLivingChildren.get(r.id);
            if (kids == null || kids.size() != 1) return r;
            // Exactly one living child — chain continues. If r is dead and
            // its single child is also internal, skip through.
            r = kids.get(0);
        }
    }

    private final com.badlogic.gdx.graphics.Color lineageColScratch = new com.badlogic.gdx.graphics.Color();

    /**
     * Draw `r` as a horizontal segment starting at chainStartTime (the
     * birth time of the linear-chain's *first* ancestor that was the
     * collapse-anchor), and at its branch point recurse into kids.
     */
    private void drawLineageSubtree(com.protoevo.env.LineageRecord r,
                                    float chainStartTime,
                                    float xLeft, float xRight,
                                    float yTop, float yBottom,
                                    float tStart, float tEnd) {
        float tSpan = Math.max(1e-3f, tEnd - tStart);
        float xStart = xLeft + ((chainStartTime - tStart) / tSpan) * (xRight - xLeft);
        // For a living leaf, draw out to "now" (xRight). For a dead branch
        // point, draw to its own deathTime — that's when its descendants
        // diverged from a single common stem.
        float xEnd = (r.isAlive() ? xRight
                : xLeft + ((Math.max(r.deathTime, r.birthTime) - tStart) / tSpan)
                        * (xRight - xLeft));
        if (xStart < xLeft) xStart = xLeft;
        if (xEnd > xRight) xEnd = xRight;
        if (xEnd < xStart) xEnd = xStart;
        float yMid = 0.5f * (yTop + yBottom);

        lineageColor(r.id, lineageColScratch);
        lineageShape.setColor(lineageColScratch);
        // Thickness scales with descendant count (log) so dominant
        // branches visually stand out.
        float thickness = (float) Math.max(1.5, 1.5 + Math.log(1 + r.aliveDescendants));
        lineageShape.rectLine(xStart, yMid, xEnd, yMid, thickness);

        ArrayList<com.protoevo.env.LineageRecord> kids = lineageLivingChildren.get(r.id);
        if (kids == null || kids.isEmpty()) return;

        int totalKidDescendants = 0;
        for (com.protoevo.env.LineageRecord k : kids) totalKidDescendants += k.aliveDescendants;
        if (totalKidDescendants <= 0) return;

        float yRange = yTop - yBottom;
        float yCursor = yTop;
        for (com.protoevo.env.LineageRecord k : kids) {
            // Collapse chains: walk down single-child paths from k to its
            // first divergence point or living leaf. The horizontal segment
            // we draw for the chain runs from k.birthTime out to the
            // resolved record's lifespan.
            com.protoevo.env.LineageRecord collapsed = collapseChain(k);
            float frac = collapsed.aliveDescendants / (float) totalKidDescendants;
            float kSize = frac * yRange;
            float kYTop = yCursor;
            float kYBottom = yCursor - kSize;
            float kYMid = 0.5f * (kYTop + kYBottom);
            float kBirthX = xLeft + ((k.birthTime - tStart) / tSpan) * (xRight - xLeft);
            if (kBirthX < xStart) kBirthX = xStart;
            if (kBirthX > xRight) kBirthX = xRight;
            // Vertical branch line at the divergence point.
            lineageColor(collapsed.id, lineageColScratch);
            lineageShape.setColor(lineageColScratch);
            lineageShape.rectLine(kBirthX, yMid, kBirthX, kYMid, 1.5f);
            drawLineageSubtree(collapsed, k.birthTime,
                    xLeft, xRight, kYTop, kYBottom, tStart, tEnd);
            yCursor = kYBottom;
        }
    }

    private int countLivingLeaves(com.protoevo.env.LineageRecord r) {
        ArrayList<com.protoevo.env.LineageRecord> kids = lineageLivingChildren.get(r.id);
        if (kids == null || kids.isEmpty())
            return r.isAlive() ? 1 : 0;
        int n = 0;
        for (com.protoevo.env.LineageRecord k : kids)
            n += countLivingLeaves(collapseChain(k));
        return n;
    }

    private void renderLineageOverlay() {
        if (environment == null) return;
        java.util.Map<Long, com.protoevo.env.LineageRecord> recs =
                environment.getLineageRecords();
        if (recs.isEmpty()) return;

        rebuildLineageChildren(recs);
        if (lineageVisibleRoots.isEmpty()) return;

        // Count living leaves per root (post-chain-collapse) and pick the
        // strongest roots that together fit in our visible-leaf budget.
        // This keeps each row tall enough to actually see — at 80 leaves
        // and ~500px tree height we still get ~6px per row.
        int aliveCount = 0;
        int rootsShown = 0;
        ArrayList<com.protoevo.env.LineageRecord> roots = lineageVisibleRoots;
        for (int i = 0; i < roots.size(); i++) {
            com.protoevo.env.LineageRecord root = collapseChain(roots.get(i));
            int leaves = countLivingLeaves(root);
            if (leaves <= 0) continue;
            if (aliveCount + leaves > LINEAGE_MAX_VISIBLE_LEAVES && rootsShown > 0)
                break;
            aliveCount += leaves;
            rootsShown++;
        }
        if (rootsShown == 0) return;

        // Time window — from oldest visible root's birth to now.
        float now = environment.getElapsedTime();
        float tStart = now;
        int totalLeaves = 0;
        for (int i = 0; i < rootsShown; i++) {
            com.protoevo.env.LineageRecord root = collapseChain(roots.get(i));
            if (root.birthTime < tStart) tStart = root.birthTime;
            totalLeaves += countLivingLeaves(root);
        }
        if (totalLeaves <= 0) return;
        float tEnd = now + Math.max(1f, (now - tStart) * 0.02f);

        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();
        float panelW = w * 0.55f;
        float panelH = h * 0.7f;
        float panelX = w - panelW - 24f;
        float panelY = h * 0.05f;

        Gdx.gl.glEnable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
        lineageShape.setProjectionMatrix(uiBatch.getProjectionMatrix());
        lineageShape.begin(com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Filled);
        lineageShape.setColor(0f, 0f, 0f, 0.7f);
        lineageShape.rect(panelX, panelY, panelW, panelH);

        float padX = 14f, padY = 36f;
        float treeLeft = panelX + padX;
        float treeRight = panelX + panelW - padX;
        float treeTop = panelY + panelH - padY;
        float treeBottom = panelY + padY;
        float yRange = treeTop - treeBottom;
        float yCursor = treeTop;
        for (int i = 0; i < rootsShown; i++) {
            com.protoevo.env.LineageRecord root = collapseChain(roots.get(i));
            int leaves = countLivingLeaves(root);
            float frac = leaves / (float) totalLeaves;
            float rSize = frac * yRange;
            float rYTop = yCursor;
            float rYBottom = yCursor - rSize;
            drawLineageSubtree(root, root.birthTime,
                    treeLeft, treeRight, rYTop, rYBottom, tStart, tEnd);
            yCursor = rYBottom;
        }
        lineageShape.end();
        Gdx.gl.glDisable(com.badlogic.gdx.graphics.GL20.GL_BLEND);

        uiBatch.begin();
        font.setColor(1f, 1f, 1f, 1f);
        font.draw(uiBatch,
                String.format("Phylogeny (currently alive)  %d/%d roots, %d leaves",
                        rootsShown, lineageVisibleRoots.size(), totalLeaves),
                panelX + 10f, panelY + panelH - 10f);
        font.setColor(0.6f, 0.6f, 0.6f, 1f);
        font.draw(uiBatch, String.format("t=%.0fs", tStart),
                panelX + 10f, panelY + 22f);
        font.draw(uiBatch, String.format("now: t=%.0fs", now),
                panelX + panelW - 140f, panelY + 22f);
        uiBatch.end();
    }
    // ===== end Lineage tree overlay =====

    // ===== Turbo mode =====
    // Black-screen fast-forward: skip all environment rendering, run sim updates
    // in a tight loop within each render frame, and draw a small line chart of
    // population over time. Useful for letting the sim evolve overnight.
    private boolean turboMode = false;
    private static final int TURBO_SAMPLES_CAP = 600;
    private final float[] turboSampleTime = new float[TURBO_SAMPLES_CAP];
    private final int[] turboSampleProto = new int[TURBO_SAMPLES_CAP];
    private final int[] turboSamplePlants = new int[TURBO_SAMPLES_CAP];
    private int turboSampleHead = 0, turboSampleSize = 0;
    private float turboLastSampleSimTime = -1f;
    private long turboLastLogMs = 0;
    private int turboLastLoggedProto = -1;
    private com.badlogic.gdx.graphics.glutils.ShapeRenderer turboShape;

    public void toggleTurboMode() {
        turboMode = !turboMode;
        if (turboMode && turboShape == null) {
            turboShape = new com.badlogic.gdx.graphics.glutils.ShapeRenderer();
        }
        System.out.println("Turbo mode: " + (turboMode
                ? "ON (rendering disabled, max sim throughput)"
                : "OFF"));
        if (turboMode) {
            turboLastLogMs = 0; // force a log on first frame
        }
    }

    public boolean isTurboMode() { return turboMode; }

    private void turboMaybeSample() {
        if (environment == null) return;
        float simTime = environment.getElapsedTime();
        if (turboLastSampleSimTime < 0f || simTime - turboLastSampleSimTime >= 1f) {
            int proto = environment.numberOfProtozoa();
            int plants = environment.getCount(com.protoevo.biology.cells.PlantCell.class);
            int slot = turboSampleHead;
            turboSampleTime[slot] = simTime;
            turboSampleProto[slot] = proto;
            turboSamplePlants[slot] = plants;
            turboSampleHead = (turboSampleHead + 1) % TURBO_SAMPLES_CAP;
            if (turboSampleSize < TURBO_SAMPLES_CAP) turboSampleSize++;
            turboLastSampleSimTime = simTime;
        }
    }

    private void turboMaybeLog() {
        if (environment == null) return;
        long now = System.currentTimeMillis();
        int proto = environment.numberOfProtozoa();
        boolean dueByTime = now - turboLastLogMs > 3L * 60L * 1000L;
        boolean dueByChange = turboLastLoggedProto > 0
                && Math.abs(proto - turboLastLoggedProto) >= 0.25 * turboLastLoggedProto;
        if (turboLastLogMs == 0 || dueByTime || dueByChange) {
            int plants = environment.getCount(com.protoevo.biology.cells.PlantCell.class);
            // Diagnostic: average protozoan energy / construction-mass /
            // food-mass so we can see at a glance whether eating delivers
            // anything. If avgFoodMass > 0 but avgConstrMass stays at 0,
            // digest isn't converting. If avgFoodMass is also 0, the eat
            // path isn't filling the food pool. If construction-mass cap
            // is full but cells aren't growing/splitting, the bottleneck
            // is elsewhere.
            double sumE = 0, sumCM = 0, sumFood = 0, sumR = 0, sumAge = 0;
            int n = 0, engulfing = 0;
            for (com.protoevo.biology.cells.Cell c : environment.getCells()) {
                if (!(c instanceof com.protoevo.biology.cells.Protozoan)) continue;
                com.protoevo.biology.cells.Protozoan p = (com.protoevo.biology.cells.Protozoan) c;
                if (p.isDead()) continue;
                sumE += p.getEnergyAvailable();
                sumCM += p.getConstructionMassAvailable();
                for (com.protoevo.biology.Food f : p.getFoodToDigest().values())
                    sumFood += f.getSimpleMass();
                sumR += p.getRadius();
                sumAge += p.getTimeAlive();
                if (!p.getEngulfedCells().isEmpty()) engulfing++;
                n++;
            }
            if (n > 0) {
                System.out.printf("[TURBO] sim=%.1fs  protozoa=%d  plants=%d  "
                        + "avgE=%.1f  avgCM=%.5g  avgFood=%.5g  avgR=%.4f  avgAge=%.1fs  engulfing=%d%n",
                        environment.getElapsedTime(), proto, plants,
                        sumE / n, sumCM / n, sumFood / n, sumR / n, sumAge / n, engulfing);
            } else {
                System.out.printf("[TURBO] sim=%.1fs  protozoa=%d  plants=%d  (no live protozoa)%n",
                        environment.getElapsedTime(), proto, plants);
            }
            turboLastLogMs = now;
            turboLastLoggedProto = proto;
        }
    }

    private void turboRender() {
        ScreenUtils.clear(0f, 0f, 0f, 1f);

        // Run sim as hard as we can within ~12ms so the render thread still
        // returns in time for libGDX to process input events (so F8 / ESC
        // still work). simulation.update() already substeps internally up to
        // its safety cap, so each call here advances `timeDilation` substeps.
        long deadline = System.currentTimeMillis() + 12;
        int iters = 0;
        while (System.currentTimeMillis() < deadline) {
            simulation.update();
            iters++;
            if (iters >= 1000) break; // safety: don't pin the thread on a fast sim
        }

        turboMaybeSample();
        turboMaybeLog();

        // Layout: top 1/8 reserved for status text; rest for line chart.
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();
        float marginX = w * 0.06f;
        float chartTop = h * 0.85f;
        float chartBottom = h * 0.10f;
        float chartLeft = marginX;
        float chartRight = w - marginX;

        // Compute chart bounds.
        int n = turboSampleSize;
        if (n >= 2) {
            float tMin = Float.POSITIVE_INFINITY, tMax = Float.NEGATIVE_INFINITY;
            int yMax = 1;
            int start = (turboSampleHead - n + TURBO_SAMPLES_CAP) % TURBO_SAMPLES_CAP;
            for (int i = 0; i < n; i++) {
                int idx = (start + i) % TURBO_SAMPLES_CAP;
                float t = turboSampleTime[idx];
                if (t < tMin) tMin = t;
                if (t > tMax) tMax = t;
                if (turboSampleProto[idx] > yMax) yMax = turboSampleProto[idx];
                if (turboSamplePlants[idx] > yMax) yMax = turboSamplePlants[idx];
            }
            float dt = Math.max(1e-3f, tMax - tMin);

            turboShape.begin(com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Line);

            // Frame
            turboShape.setColor(0.4f, 0.4f, 0.4f, 1f);
            turboShape.rect(chartLeft, chartBottom,
                    chartRight - chartLeft, chartTop - chartBottom);

            // Plot helper inline: protozoa green, plants gray.
            for (int series = 0; series < 2; series++) {
                if (series == 0) turboShape.setColor(0.4f, 1f, 0.4f, 1f);
                else turboShape.setColor(0.55f, 0.55f, 0.55f, 1f);
                float prevX = 0, prevY = 0;
                for (int i = 0; i < n; i++) {
                    int idx = (start + i) % TURBO_SAMPLES_CAP;
                    int v = (series == 0)
                            ? turboSampleProto[idx]
                            : turboSamplePlants[idx];
                    float fx = (turboSampleTime[idx] - tMin) / dt;
                    float fy = (float) v / yMax;
                    float x = chartLeft + fx * (chartRight - chartLeft);
                    float y = chartBottom + fy * (chartTop - chartBottom);
                    if (i > 0) turboShape.line(prevX, prevY, x, y);
                    prevX = x; prevY = y;
                }
            }
            turboShape.end();
        }

        // Status text + legend via the existing UI batch + font.
        int proto = environment != null ? environment.numberOfProtozoa() : 0;
        int plants = environment != null
                ? environment.getCount(com.protoevo.biology.cells.PlantCell.class) : 0;
        float simT = environment != null ? environment.getElapsedTime() : 0f;
        uiBatch.begin();
        font.setColor(1f, 1f, 1f, 1f);
        String top = String.format(
                "TURBO MODE  |  protozoa: %d  |  plants: %d  |  sim time: %.1fs  |  iters/frame: %d",
                proto, plants, simT, iters);
        font.draw(uiBatch, top, marginX, h - h * 0.04f);

        font.setColor(0.4f, 1f, 0.4f, 1f);
        font.draw(uiBatch, "protozoa", marginX, chartTop + 18f);
        font.setColor(0.55f, 0.55f, 0.55f, 1f);
        font.draw(uiBatch, "plants", marginX + 110f, chartTop + 18f);

        font.setColor(0.6f, 0.6f, 0.6f, 1f);
        font.draw(uiBatch, "F8 to exit turbo", marginX, chartBottom - 12f);
        uiBatch.end();
    }
    // ===== end Turbo mode =====

    /**
     * Low-detail mode: skip the chemical-field overlay and the baked shadow
     * texture. Both are screen-filling fragment passes and dominate GPU time
     * on weak hardware. Toggles both flags together.
     */
    public void toggleLowDetailMode() {
        EnvironmentRenderer r = getBaseEnvironmentRenderer();
        boolean nowLow = r.isRenderChemicals() || r.isRenderShadows();
        r.setRenderChemicals(!nowLow);
        r.setRenderShadows(!nowLow);
        System.out.println("Low-detail mode: " + (nowLow ? "ON (chemicals+shadows hidden)" : "OFF"));
    }

    public void renderEnvironment(float delta) {
        float envLight = Functions.clampedLinearRemap(
                environment.getLightMap().getEnvLight(),
                0f, 1f, 0.5f, 1f
        );
        brightnessLayer.setBrightness(envLight);
        environmentRenderer.render(delta);
    }

    @Override
    public void render(float delta) {

        if (turboMode) {
            turboRender();
            return;
        }

        conditionalTasks.forEach((condition, task) -> {
            if (condition.get())
                task.run();
        });
        conditionalTasks.entrySet().removeIf(entry -> entry.getKey().get());

        ScreenUtils.clear(EnvironmentRenderer.backgroundColor);

        simulation.update();

        elapsedTime += delta;

        handleMeander(delta);
        camera.update();

        ParticleTracker particleTracker = inputManager.getParticleTracker();
        if (particleTracker.isTracking()) {
            if (particleTracker.getTrackedParticle().isDead())
                particleTracker.untrack();
            else
                camera.position.set(particleTracker.getTrackedParticlePosition());
        }

        renderEnvironment(delta);

        multiCellGRNRenderer.ifPresent(renderer -> renderer.renderGRNs(delta));

        if (uiHidden)
            return;

        topBar.draw(delta);

        if (lineageOverlay)
            renderLineageOverlay();

        uiBatch.begin();

        if (simulation.isBusyOnOtherThread())
            drawSavingText();

        pollStatsCounter += delta;
        if (pollStatsCounter > pollStatsInterval) {
            pollStatsCounter = 0;
            pollStats();
        }

        handleNetworkRenderer(delta);

        renderStats();

        if (DebugMode.isDebugMode())
            drawDebugInfo();

        drawSignatureHud();

        uiBatch.end();

        stage.setDebugAll(DebugMode.isModeOrHigher(DebugMode.SIMPLE_INFO));
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void show() {
        CursorUtils.setDefaultCursor();
        inputManager.registerAsInputProcessor();
    }

    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
    }

    public void drawSavingText() {
        StringBuilder textWithDots = new StringBuilder("Saving Simulation Data");
        for (int i = 0; i < Math.max(0, (int) (elapsedTime * 2) % 4); i++)
            textWithDots.append(".");

        float x = 3 * font.getLineHeight();
        font.draw(uiBatch, textWithDots.toString(), x, x);
    }

    // BLAST-style signature HUD with directional, semantic pairing. Plants
    // have 1 surface signature; protozoa have 3 (plant-receptor key,
    // receiving receptor = identity, phagocytic receptor = what they hunt).
    // For two-cell comparison we don't pair by length — we pair by what's
    // ecologically meaningful:
    //   * A.phagocytic ↔ B.receiving  ("Can A eat B?")
    //   * A.receiving ↔ B.phagocytic  ("Can B eat A?")
    //   * A.plantKey  ↔ B.plantKey    ("Diet overlap")
    // Mixed types (protozoa↔plant) compare the protozoa's plant-receptor
    // key to the plant's surface signature.
    private void drawSignatureHud() {
        com.protoevo.physics.Particle tracked =
                inputManager.getParticleTracker().getTrackedParticle();
        if (tracked == null) return;
        com.protoevo.physics.Particle compared =
                inputManager.getParticleTracker().getComparedParticle();

        float h = font.getLineHeight();
        float x = h;
        float y = h * 1.2f;  // anchor at bottom, grow upward

        if (compared == null) {
            // Single-cell view: list every sequence with its label.
            java.util.List<String[]> rows = signatureRowsFor(tracked);
            if (rows.isEmpty()) return;
            for (int i = rows.size() - 1; i >= 0; i--) {
                String[] row = rows.get(i);
                font.draw(uiBatch, row[0] + ":  " + row[1], x, y);
                y += h;
            }
            font.draw(uiBatch,
                    "[ shift+click another cell to BLAST-compare ]", x, y);
            return;
        }

        // Two-cell view: build the semantic pair list, iterate bottom-up.
        java.util.List<Pair> pairs = buildBlastPairs(tracked, compared);
        if (pairs.isEmpty()) return;
        for (int i = pairs.size() - 1; i >= 0; i--) {
            Pair p = pairs.get(i);
            if (p.b == null) {
                // No counterpart in B — show A alone.
                font.draw(uiBatch, p.aLabel + ":  " + p.a, x, y);
                y += h;
            } else {
                // Align marker to where the residues start: 2 ("B ") +
                // p.bLabel + 3 (":  ").
                String prefix = spaces(2 + p.bLabel.length() + 3);
                font.draw(uiBatch, "B " + p.bLabel + ":  " + p.b, x, y);
                y += h;
                font.draw(uiBatch,
                        prefix + matchMarkerLine(p.a, p.b)
                                + "   " + String.format("id %.0f%%  run %d",
                                        100f * identityFraction(p.a, p.b),
                                        longestContiguousMatchLen(p.a, p.b)),
                        x, y);
                y += h;
                font.draw(uiBatch, "A " + p.aLabel + ":  " + p.a, x, y);
                y += h;
                // Tag line above the block: what does this pair *mean*?
                font.draw(uiBatch, "  " + p.description, x, y);
                y += h * 1.05f;
            }
        }
        font.draw(uiBatch, "[ A = tracked, B = shift-clicked ]", x, y);
    }

    private static final class Pair {
        final String aLabel, a, bLabel, b, description;
        Pair(String aLabel, String a, String bLabel, String b, String description) {
            this.aLabel = aLabel; this.a = a;
            this.bLabel = bLabel; this.b = b;
            this.description = description;
        }
    }

    /** Build the semantic pair list for a two-cell HUD comparison.
     *  Returns the pairs in the order they'll be rendered (top→bottom). */
    private java.util.List<Pair> buildBlastPairs(
            com.protoevo.physics.Particle ap, com.protoevo.physics.Particle bp) {
        Object a = ap.getUserData();
        Object b = bp.getUserData();
        java.util.List<Pair> out = new java.util.ArrayList<>();

        boolean ap1 = a instanceof com.protoevo.biology.cells.Protozoan;
        boolean bp1 = b instanceof com.protoevo.biology.cells.Protozoan;
        boolean apl = a instanceof com.protoevo.biology.cells.PlantCell;
        boolean bpl = b instanceof com.protoevo.biology.cells.PlantCell;

        if (ap1 && bp1) {
            com.protoevo.biology.cells.Protozoan A = (com.protoevo.biology.cells.Protozoan) a;
            com.protoevo.biology.cells.Protozoan B = (com.protoevo.biology.cells.Protozoan) b;
            String aPhag = str(A.getProtozoaPhagocyticReceptor());
            String aRecv = str(A.getProtozoaReceivingReceptor());
            String aPlnt = str(A.getPlantReceptorKey());
            String bPhag = str(B.getProtozoaPhagocyticReceptor());
            String bRecv = str(B.getProtozoaReceivingReceptor());
            String bPlnt = str(B.getPlantReceptorKey());
            if (aPhag != null && bRecv != null)
                out.add(new Pair("Phagocytic   (75)", aPhag,
                                 "Receiving    (75)", bRecv, "Can A eat B?"));
            if (aRecv != null && bPhag != null)
                out.add(new Pair("Receiving    (75)", aRecv,
                                 "Phagocytic   (75)", bPhag, "Can B eat A?"));
            if (aPlnt != null && bPlnt != null)
                out.add(new Pair("Plant Key    (50)", aPlnt,
                                 "Plant Key    (50)", bPlnt, "Diet overlap"));
        } else if (ap1 && bpl) {
            com.protoevo.biology.cells.Protozoan A = (com.protoevo.biology.cells.Protozoan) a;
            com.protoevo.biology.cells.PlantCell B = (com.protoevo.biology.cells.PlantCell) b;
            String aPlnt = str(A.getPlantReceptorKey());
            String bSurf = str(B.getSurfaceSignature());
            if (aPlnt != null && bSurf != null)
                out.add(new Pair("Plant Key    (50)", aPlnt,
                                 "Plant Surface(50)", bSurf, "Can A eat this plant?"));
        } else if (apl && bp1) {
            com.protoevo.biology.cells.PlantCell A = (com.protoevo.biology.cells.PlantCell) a;
            com.protoevo.biology.cells.Protozoan B = (com.protoevo.biology.cells.Protozoan) b;
            String aSurf = str(A.getSurfaceSignature());
            String bPlnt = str(B.getPlantReceptorKey());
            if (aSurf != null && bPlnt != null)
                out.add(new Pair("Plant Surface(50)", aSurf,
                                 "Plant Key    (50)", bPlnt, "Can B eat this plant?"));
        } else if (apl && bpl) {
            com.protoevo.biology.cells.PlantCell A = (com.protoevo.biology.cells.PlantCell) a;
            com.protoevo.biology.cells.PlantCell B = (com.protoevo.biology.cells.PlantCell) b;
            String aSurf = str(A.getSurfaceSignature());
            String bSurf = str(B.getSurfaceSignature());
            if (aSurf != null && bSurf != null)
                out.add(new Pair("Plant Surface(50)", aSurf,
                                 "Plant Surface(50)", bSurf, "Surface similarity"));
        }
        return out;
    }

    private static String str(com.protoevo.biology.evolution.AminoAcidSequence s) {
        return s == null ? null : s.toString();
    }

    /** Returns ordered (label, sequence) rows for whatever signatures the
     *  cell carries. Used in single-cell view. */
    private java.util.List<String[]> signatureRowsFor(com.protoevo.physics.Particle p) {
        java.util.List<String[]> rows = new java.util.ArrayList<>();
        Object data = p.getUserData();
        if (data instanceof com.protoevo.biology.cells.PlantCell) {
            com.protoevo.biology.evolution.AminoAcidSequence s =
                    ((com.protoevo.biology.cells.PlantCell) data).getSurfaceSignature();
            if (s != null) rows.add(new String[]{"Plant Surface (50)", s.toString()});
        } else if (data instanceof com.protoevo.biology.cells.Protozoan) {
            com.protoevo.biology.cells.Protozoan c =
                    (com.protoevo.biology.cells.Protozoan) data;
            com.protoevo.biology.evolution.AminoAcidSequence pk = c.getPlantReceptorKey();
            com.protoevo.biology.evolution.AminoAcidSequence rr = c.getProtozoaReceivingReceptor();
            com.protoevo.biology.evolution.AminoAcidSequence pr = c.getProtozoaPhagocyticReceptor();
            if (pk != null) rows.add(new String[]{"Plant Receptor Key (50)", pk.toString()});
            if (rr != null) rows.add(new String[]{"Receiving Receptor (75)", rr.toString()});
            if (pr != null) rows.add(new String[]{"Phagocytic Receptor(75)", pr.toString()});
        }
        return rows;
    }

    private static String matchMarkerLine(String a, String b) {
        int n = Math.min(a.length(), b.length());
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++)
            sb.append(a.charAt(i) == b.charAt(i) ? '|' : '.');
        return sb.toString();
    }

    private static String spaces(int n) {
        char[] c = new char[n];
        java.util.Arrays.fill(c, ' ');
        return new String(c);
    }

    private static float identityFraction(String a, String b) {
        int n = Math.min(a.length(), b.length());
        if (n == 0) return 0f;
        int match = 0;
        for (int i = 0; i < n; i++) if (a.charAt(i) == b.charAt(i)) match++;
        return (float) match / n;
    }

    /** Length of the longest run of consecutive identical residues. This
     *  is what the engulf gate actually keys on, so it's more useful for
     *  the user than overall identity %. */
    private static int longestContiguousMatchLen(String a, String b) {
        int n = Math.min(a.length(), b.length());
        int best = 0, cur = 0;
        for (int i = 0; i < n; i++) {
            if (a.charAt(i) == b.charAt(i)) {
                cur++;
                if (cur > best) best = cur;
            } else {
                cur = 0;
            }
        }
        return best;
    }

    public ImageButton createImageButton(String texturePath, float width, float height, EventListener listener) {
        Texture texture = ImageUtils.getTexture(texturePath);
        Drawable drawable = new TextureRegionDrawable(new TextureRegion(texture));
        ImageButton button = new ImageButton(drawable);
        button.setSize(width, height);
        button.setTouchable(Touchable.enabled);
        button.addListener(listener);
        stage.addActor(button);
        buttons.add(button);
        return button;
    }

    public Stage getStage() {
        return stage;
    }

    public Environment getEnvironment() {
        return environment;
    }

    public OrthographicCamera getCamera() {
        return camera;
    }

    public TopBar getTopBar() {
        return topBar;
    }

    public void drawDebugInfo() {
        String separator = " | ";
        String debugString = "FPS: " + Gdx.graphics.getFramesPerSecond();
        debugString += separator + "Zoom: " + ((int) (100 * camera.zoom)) / 100.f;
        debugString += separator + "Pos: " + Utils.numberToString(camera.position.x, 2)
                + ", " + Utils.numberToString(camera.position.y, 2);

        ParticleTracker tracker = inputManager.getParticleTracker();
        debugStats.clear();
        if (tracker.isTracking()) {
            Particle trackedParticle = tracker.getTrackedParticle();
            if (trackedParticle.getUserData() instanceof Cell)
                debugStats.putAll(trackedParticle.getUserData(Cell.class).getDebugStats());
            debugStats.putAll(trackedParticle.getDebugStats());
        }
        else if (DebugMode.isDebugModePhysicsDebug())
            debugStats.putAll(environment.getPhysicsDebugStats());
        else
            debugStats.putAll(environment.getDebugStats());

        int lineNumber = 0;
        int maxLength = 0;
        for (Statistics.Stat entityStat : debugStats) {
            int statLen = entityStat.getValueString().length();
            maxLength = Math.max(maxLength, statLen);
        }
        maxLength += 3;

        for (Statistics.Stat entityStat : debugStats) {
            String valueStr = entityStat.getValueString();
            StringBuilder text = new StringBuilder(entityStat.getName() + ": ");
            for (int i = 0; i < Math.max(0, maxLength - valueStr.length()); i++)
                text.append(" ");
            text.append(valueStr);
            layout.setText(debugFont, text.toString());
            float x = graphicsWidth - layout.width - textAwayFromEdge;
            debugFont.draw(uiBatch, text.toString(), x, getYPosRHS(lineNumber));
            lineNumber++;
        }
        debugFont.draw(uiBatch, debugString,
                2 * topBar.getPadding(), font.getLineHeight() + topBar.getPadding());
    }

    public float getYPosLHS(int i) {
        return graphicsHeight - (1.3f*infoTextSize*i + 3 * graphicsHeight / 20f);
    }

    public float getYPosRHS(int i) {
        return graphicsHeight - topBar.getHeight() * 1.5f - 1.3f * infoTextSize * i;
    }

    private int renderStats(Statistics stats, int lineNumber, BitmapFont statsFont) {
        sortedStats.clear();
        stats.forEach(stat -> sortedStats.put(stat.getName(), stat.toString()));
        for (String statString : sortedStats.values()) {
            statsFont.draw(uiBatch, statString, textAwayFromEdge, getYPosLHS(lineNumber));
            lineNumber++;
        }
        return lineNumber;
    }

    public int renderStats(Statistics stats) {
        return renderStats(stats, 0, font);
    }

    public void setParticleTopBarUI() {
        saveTrackedParticleButton.setVisible(true);
        float fieldWidthMul = 8f;

        Vector2 pos = topBar.nextLeftPosition();

        multiCellViewerTrackedParticleButton.setVisible(true);
        multiCellViewerTrackedParticleButton.setPosition(pos.x, pos.y);

        float pad = 1.5f * topBar.getTopBarPadding();
        float x = multiCellViewerTrackedParticleButton.getX()
                  + multiCellViewerTrackedParticleButton.getWidth() + pad;
        saveTrackedParticleButton.setPosition(x, pos.y);
        saveTrackedParticleTextField.setVisible(true);
        saveTrackedParticleTextField.setBounds(
                saveTrackedParticleButton.getX() + saveTrackedParticleButton.getWidth() + pad,
                saveTrackedParticleButton.getY(),
                fieldWidthMul * saveTrackedParticleButton.getWidth(),
                saveTrackedParticleButton.getHeight()
        );

        addTagButton.setVisible(true);
        addTagButton.setPosition(
                saveTrackedParticleTextField.getX() + saveTrackedParticleTextField.getWidth() + pad,
                saveTrackedParticleTextField.getY()
        );

        addTagTextField.setVisible(true);
        addTagTextField.setBounds(
                addTagButton.getX() + addTagButton.getWidth() + pad,
                addTagButton.getY(),
                fieldWidthMul * addTagButton.getWidth(),
                addTagButton.getHeight()
        );
    }

    public void hideParticleTopBarUI() {
        saveTrackedParticleButton.setVisible(false);
        saveTrackedParticleTextField.setVisible(false);
        addTagButton.setVisible(false);
        addTagTextField.setVisible(false);
        multiCellViewerTrackedParticleButton.setVisible(false);
    }

    public void setProtozoaStatOptions(Protozoan protozoan) {
        statGetters.clear();
        ArrayList<String> statOptions = new ArrayList<>();
        statOptions.add("Protozoan Stats");
        statGetters.put("Protozoan Stats", protozoan::getStats);

        statOptions.add("Resource Stats");
        statGetters.put("Resource Stats", protozoan::getResourceStats);

        getStats = protozoan::getStats;

        layout.setText(statsSelectBox.getStyle().font, "Protozoan Stats");
        float maxWidth = layout.width;

        for (SurfaceNode node : protozoan.getSurfaceNodes()) {
            String option;
            if (node.getAttachment() != null)
                option = "Node " + node.getIndex() + " (" + node.getAttachmentName() + ") Stats";
            else
                option = "Node " + node.getIndex() + " Stats";
            statOptions.add(option);
            statGetters.put(option, node::getStats);
            layout.setText(statsSelectBox.getStyle().font, option);
            maxWidth = Math.max(maxWidth, layout.width);
        }

        for (Organelle organelle : protozoan.getOrganelles()) {
            String option;
            if (organelle.getFunction() != null)
                option = "Organelle " + organelle.getIndex()
                        + " (" + organelle.getFunction().getName() + ") Stats";
            else
                option = "Organelle " + organelle.getIndex() + " Stats";
            statOptions.add(option);
            statGetters.put(option, organelle::getStats);
            layout.setText(statsSelectBox.getStyle().font, option);
            maxWidth = Math.max(maxWidth, layout.width);
        }

        statsSelectBox.setItems(statOptions.toArray(new String[0]));
        statsSelectBox.setWidth(maxWidth);
        statsSelectBox.setSelected("Protozoan Stats");
    }

    public void addStatOption(String name, Callable<Statistics> getter) {
        statGetters.put(name, getter);
        layout.setText(statsSelectBox.getStyle().font, name);
        if (layout.width > statsSelectBox.getWidth())
            statsSelectBox.setWidth(layout.width);
        statsSelectBox.setItems(statGetters.keySet().toArray(new String[0]));
    }

    public void setEnvStatOptions() {
        statGetters.clear();
        addStatOption("Simulation Stats", environment::getStats);
        addStatOption("Protozoa Summary", environment::getProtozoaSummaryStats);
        statsSelectBox.setSelected("Simulation Stats");
    }

    public void renderStats() {
        float titleY = (float) (17 * graphicsHeight / 20f + 1.5 * statsTitle.getLineHeight());

        if (statsSelectBox.getSelected() != null && statsSelectBox.isVisible())
            getStats = statGetters.get(statsSelectBox.getSelected());

        ParticleTracker particleTracker = inputManager.getParticleTracker();
        if (particleTracker.isTracking()) {
            Particle particle = particleTracker.getTrackedParticle();

            if ((trackedParticle != particle)) {
                if (particle.getUserData() instanceof Protozoan) {
                    setParticleTopBarUI();
                    setProtozoaStatOptions(particle.getUserData(Protozoan.class));
                }
                else if (particle.getUserData() instanceof Cell) {
                    Cell cell = particle.getUserData(Cell.class);
                    hideParticleTopBarUI();
                    statGetters.clear();
                    addStatOption(cell.getPrettyName() + " Stats", cell::getStats);
                    addStatOption("Resource Stats", cell::getResourceStats);
                    statsSelectBox.setSelectedIndex(0);
                }
                trackedParticle = particle;
            }
        }
        else if (trackedParticle != null) {
            trackedParticle = null;
            hideParticleTopBarUI();
            setEnvStatOptions();
        }

        statsSelectBox.setPosition(textAwayFromEdge, titleY - statsSelectBox.getStyle().font.getLineHeight() / 2f);

        renderStats(stats);
    }

    private void handleNetworkRenderer(float delta) {
        ParticleTracker particleTracker = inputManager.getParticleTracker();

        if (!multiCellGRNRenderer.isPresent() && particleTracker.isTracking()) {
            Particle particle = particleTracker.getTrackedParticle();
            if (particle.getUserData() instanceof EvolvableCell) {
                EvolvableCell evolvableCell = particle.getUserData(EvolvableCell.class);
                GeneExpressionFunction gef = evolvableCell.getGeneExpressionFunction();
                if (gef != null) {
                    NeuralNetwork grn = gef.getRegulatoryNetwork();
                    networkRenderer.setNeuralNetwork(grn);
                    mouseOverNeuronHandler.setGeneExpressionFunction(evolvableCell.getGeneExpressionFunction());
                    networkRenderer.render(delta);
                }
            }
        }
    }

    public void pollStats() {
        ParticleTracker particleTracker = inputManager.getParticleTracker();
        if (getStats == null)
            return;

        try {
            stats.clear();
            stats.putAll(getStats.call());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (particleTracker.isTracking()) {
            Particle particle = particleTracker.getTrackedParticle();
            if (DebugMode.isDebugModePhysicsDebug()) {
                debugStats.clear();
                debugStats.putAll(particle.getDebugStats());
            }

        } else {
            if (DebugMode.isDebugModePhysicsDebug()) {
                debugStats.clear();
                debugStats.putAll(simulation.getEnv().getDebugStats());
            }
        }
    }

    public void dispose() {
        stage.dispose();
        uiBatch.dispose();
        font.dispose();
        topBar.dispose();
        environmentRenderer.dispose();
        networkRenderer.dispose();
        multiCellGRNRenderer.ifPresent(MultiCellGRNRenderer::dispose);
        inputManager.dispose();
        for (ImageButton button : buttons)
            ((TextureRegionDrawable) button.getImage().getDrawable()).getRegion().getTexture().dispose();
    }

    public boolean overOnScreenControls(int screenX, int screenY) {
        return topBar.pointOnBar(screenX, screenY);
    }

    public SimulationInputManager getInputManager() {
        return inputManager;
    }

    public Simulation getSimulation() {
        return simulation;
    }

    public void toggleUI() {
        uiHidden = !uiHidden;
//        vignetteLayer.setUiHidden(uiHidden);
    }

    public boolean hasSimulationNotLoaded() {
        return environment == null;
    }

    public GraphicsAdapter getGraphics() {
        return graphics;
    }

    public void addConditionalTask(Supplier<Boolean> trigger, Runnable action) {
        conditionalTasks.put(trigger, action);
    }

    private void handleMeander(float delta) {
        if (meanderingCamera) {
//            if (meanderingTargetPos1.dst(camera.position.x, camera.position.y) < SimulationSettings.maxParticleRadius) {
            if (meanderingT >= meanderTThreshold) {
                pickRandomMeanderTarget();
            } else {
                float speed = 0.001f;
                meanderingT += delta * speed;
                meanderingSpline.valueAt(camera.position, meanderingT);
                camera.zoom = camera.position.z;
                camera.position.z = 0;
            }
        }
    }

    private Vector2 randomMeanderTargetPos() {
        int nProtozoa = simulation.getEnv().numberOfProtozoa();

        if (nProtozoa > 0) {
            Optional<Cell> protozoan = simulation.getEnv().getCells().stream()
                    .filter(cell -> cell instanceof Protozoan)
                    .skip(MathUtils.random(nProtozoa - 1))
                    .filter(cell -> cell.getPos().len() < environment.getRadius() * 0.9f)
                    .findFirst();

            if (protozoan.isPresent())
                return protozoan.get().getPos();
        }

        return new Vector2(0, 0);
    }

    private float randomMeanderZoom(float currZoom, Vector2 pos) {
        float zoom = currZoom * MathUtils.random(0.5f, 2f);
        float zoomMax = Functions.clampedLinearRemap(
                pos.len(),
                0, environment.getRadius(),
                4, 1f
        );
        return MathUtils.clamp(zoom, 0.5f, zoomMax);
    }

    private void pickRandomMeanderTarget() {
        Vector2 meanderingTargetPosMid = meanderingTargetPos == null
                ? randomMeanderTargetPos() : meanderingTargetPos;
        float meanderingTargetZoomMind = meanderingTargetPos == null
                ? randomMeanderZoom(camera.zoom, meanderingTargetPosMid) : meanderingTargetZoom;

        meanderingTargetPos = randomMeanderTargetPos();
        meanderingTargetZoom = randomMeanderZoom(meanderingTargetZoomMind, meanderingTargetPos);

        meanderingSpline.set(
                new Vector3[]{
                        new Vector3(camera.position.x, camera.position.y, camera.zoom),
                        new Vector3(meanderingTargetPosMid.x, meanderingTargetPosMid.y, meanderingTargetZoomMind),
                        new Vector3(meanderingTargetPos.x, meanderingTargetPos.y, meanderingTargetZoom)
                }, true
        );

        meanderingT = 0f;
        meanderTThreshold = meanderingSpline.locate(
                new Vector3(meanderingTargetPosMid.x, meanderingTargetPosMid.y, meanderingTargetZoomMind));
    }

    public void disableMeandering() {
        meanderingCamera = false;
        meanderingTargetPos = null;
    }

    public void setMeandering() {
        meanderingCamera = true;
        pickRandomMeanderTarget();
    }

    public void toggleMeandering() {
        meanderingCamera = !meanderingCamera;
        if (meanderingCamera)
            pickRandomMeanderTarget();
    }

    public void resetCamera() {
        camera.position.set(0, 0, 0);
        camera.zoom = 2f;
    }

    public GlyphLayout getLayout() {
        return layout;
    }
}
