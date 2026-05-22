package com.protoevo.ui.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.protoevo.biology.cells.Cell;
import com.protoevo.biology.cells.Protozoan;
import com.protoevo.biology.nn.NetworkGenome;
import com.protoevo.core.Simulation;
import com.protoevo.ui.GraphicsAdapter;
import com.protoevo.ui.TopBar;
import com.protoevo.utils.CursorUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Snapshot of living protozoa grouped by NEAT compatibility distance.
 *
 * Intentionally minimal: no FBO, no own SpriteBatch, no own ShaderProgram, no
 * worker thread. The previous fancier version triggered a hang in the libGDX
 * stack on click; stripping it down avoids touching that path.
 */
public class GeneticClustersScreen extends ScreenAdapter {

    private static final float SPECIATION_THRESHOLD = 1.5f;
    private static final int MAX_CLUSTERS_DISPLAYED = 30;
    private static final int MAX_PROTOZOA_TO_CLUSTER = 250;
    private static final int MAX_CLUSTERS_TRACKED = 40;

    private final Simulation simulation;
    private final GraphicsAdapter graphics;
    private final Stage stage;

    private boolean wasSimulationPaused;
    private final List<Texture> swatchTextures = new ArrayList<>();

    public GeneticClustersScreen(GraphicsAdapter graphics, Simulation simulation,
                                 SimulationScreen simulationScreen) {
        this.graphics = graphics;
        this.simulation = simulation;
        this.stage = new Stage();

        TopBar topBar = new TopBar(stage,
                graphics.getSkin().getFont("default").getLineHeight());
        topBar.createRightBarImageButton("icons/back.png", graphics::moveToPreviousScreen);
    }

    private static final class Cluster {
        final NetworkGenome representative;
        int count = 0;
        float energySum = 0f, healthSum = 0f, ageSum = 0f, genSum = 0f;
        float colorRSum = 0f, colorGSum = 0f, colorBSum = 0f;

        Cluster(NetworkGenome rep) { this.representative = rep; }

        void add(Protozoan p) {
            count++;
            energySum += safe(p.getEnergyAvailable());
            healthSum += safe(p.getHealth());
            ageSum    += safe(p.getTimeAlive());
            genSum    += p.getGeneration();
            try {
                var c = p.getColour();
                colorRSum += c.r; colorGSum += c.g; colorBSum += c.b;
            } catch (Throwable ignored) {}
        }

        private static float safe(float v) { return Float.isFinite(v) ? v : 0f; }
    }

    private List<Cluster> buildClusters() {
        List<Cluster> built = new ArrayList<>();
        if (simulation.getEnv() == null) return built;

        List<Protozoan> kept = new ArrayList<>();
        List<NetworkGenome> genomes = new ArrayList<>();
        try {
            for (Cell cell : new ArrayList<>(simulation.getEnv().getCells())) {
                if (!(cell instanceof Protozoan)) continue;
                Protozoan p = (Protozoan) cell;
                NetworkGenome g;
                try { g = p.getGeneExpressionFunction().getGRNGenome(); }
                catch (Throwable t) { continue; }
                if (g == null) continue;
                kept.add(p);
                genomes.add(g);
            }
        } catch (Throwable t) {
            t.printStackTrace();
            return built;
        }

        if (kept.size() > MAX_PROTOZOA_TO_CLUSTER) {
            ArrayList<Integer> idx = new ArrayList<>(kept.size());
            for (int i = 0; i < kept.size(); i++) idx.add(i);
            Collections.shuffle(idx);
            List<Protozoan> kp = new ArrayList<>(MAX_PROTOZOA_TO_CLUSTER);
            List<NetworkGenome> kg = new ArrayList<>(MAX_PROTOZOA_TO_CLUSTER);
            for (int i = 0; i < MAX_PROTOZOA_TO_CLUSTER; i++) {
                int k = idx.get(i);
                kp.add(kept.get(k)); kg.add(genomes.get(k));
            }
            kept = kp; genomes = kg;
        }

        long t0 = System.currentTimeMillis();
        for (int i = 0; i < kept.size(); i++) {
            NetworkGenome g = genomes.get(i);
            Protozoan p = kept.get(i);
            Cluster best = null;
            float bestDist = Float.POSITIVE_INFINITY;
            for (Cluster c : built) {
                float d;
                try { d = g.distance(c.representative); }
                catch (Throwable t) { continue; }
                if (Float.isNaN(d)) continue;
                if (d < bestDist) { bestDist = d; best = c; }
            }
            if (best != null && bestDist < SPECIATION_THRESHOLD) {
                best.add(p);
            } else if (built.size() < MAX_CLUSTERS_TRACKED) {
                Cluster fresh = new Cluster(g);
                fresh.add(p);
                built.add(fresh);
            } else if (best != null) {
                best.add(p);
            }
        }
        built.sort((a, b) -> Integer.compare(b.count, a.count));
        System.out.printf("Genetic clusters: %d cells -> %d clusters in %d ms%n",
                kept.size(), built.size(), System.currentTimeMillis() - t0);
        return built;
    }

    @Override
    public void show() {
        CursorUtils.setDefaultCursor();
        Gdx.input.setInputProcessor(stage);
        wasSimulationPaused = Simulation.isPaused();
        simulation.setPaused(true);
        renderResults(buildClusters());
    }

    private void renderResults(List<Cluster> clusters) {
        disposeSwatches();
        Table content = new Table();
        content.setFillParent(true);
        content.top();
        stage.addActor(content);

        // Plain default style label — avoiding "mediumTitle" in case skin
        // lookups were on the path that previously hung.
        Label title = new Label("Genetic Clusters (" + clusters.size() + ")",
                graphics.getSkin());
        title.setAlignment(Align.center);
        content.add(title).colspan(7)
                .padTop(Gdx.graphics.getHeight() / 20f)
                .padBottom(Gdx.graphics.getHeight() / 40f).row();

        Table grid = new Table();
        addHeader(grid, "Color");
        addHeader(grid, "#");
        addHeader(grid, "Pop");
        addHeader(grid, "Avg Gen");
        addHeader(grid, "Avg Energy");
        addHeader(grid, "Avg Health");
        addHeader(grid, "Avg Age");
        grid.row();

        if (clusters.isEmpty()) {
            grid.add(new Label("No living protozoa to cluster.", graphics.getSkin()))
                    .colspan(7).pad(20f).row();
        } else {
            int shown = Math.min(clusters.size(), MAX_CLUSTERS_DISPLAYED);
            for (int i = 0; i < shown; i++) addRow(grid, i + 1, clusters.get(i));
            if (clusters.size() > shown) {
                grid.add(new Label(
                        "... " + (clusters.size() - shown) + " more not shown",
                        graphics.getSkin()))
                        .colspan(7).pad(10f).row();
            }
        }
        content.add(grid).colspan(7).row();
    }

    private void addHeader(Table grid, String text) {
        Label l = new Label(text, graphics.getSkin());
        l.setAlignment(Align.center);
        grid.add(l).pad(8f).minWidth(Gdx.graphics.getWidth() / 14f);
    }

    private void addCell(Table grid, String text) {
        Label l = new Label(text, graphics.getSkin());
        l.setAlignment(Align.center);
        grid.add(l).pad(6f);
    }

    private void addRow(Table grid, int idx, Cluster c) {
        float n = Math.max(c.count, 1);
        Color avg = new Color(c.colorRSum / n, c.colorGSum / n, c.colorBSum / n, 1f);
        Pixmap pm = new Pixmap(24, 24, Pixmap.Format.RGBA8888);
        pm.setColor(avg); pm.fill();
        Texture tex = new Texture(pm); pm.dispose();
        swatchTextures.add(tex);
        grid.add(new Image(new TextureRegionDrawable(tex))).pad(6f).size(24f, 24f);

        addCell(grid, "#" + idx);
        addCell(grid, Integer.toString(c.count));
        addCell(grid, String.format("%.1f", c.genSum / n));
        addCell(grid, String.format("%.2f", c.energySum / n));
        addCell(grid, String.format("%.2f", c.healthSum / n));
        addCell(grid, String.format("%.0fs", c.ageSum / n));
        grid.row();
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0.05f, 0.06f, 0.10f, 1f);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
        simulation.setPaused(wasSimulationPaused);
    }

    private void disposeSwatches() {
        for (Texture t : swatchTextures) {
            try { t.dispose(); } catch (Throwable ignored) {}
        }
        swatchTextures.clear();
    }

    @Override
    public void dispose() {
        super.dispose();
        hide();
        disposeSwatches();
        stage.dispose();
    }
}
