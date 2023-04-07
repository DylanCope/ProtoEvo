package com.protoevo.ui;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.protoevo.env.Environment;
import com.protoevo.settings.EnvironmentSettings;
import com.protoevo.ui.rendering.EnvironmentRenderer;

public class DefaultBackgroundGenerator extends Game {

    private final static String bgFilename = "bg.png";

    @Override
    public void create() {
        OrthographicCamera camera = new OrthographicCamera();
        camera.setToOrtho(
                false, Environment.settings.world.radius.get(),
                Environment.settings.world.radius.get());
        camera.position.set(0, 0, 0);
        camera.zoom = 2f;

        EnvironmentSettings settings = createBgEnvSettings();
        Environment environment = new Environment(settings);
        environment.initialise();

        EnvironmentRenderer environmentRenderer = new EnvironmentRenderer(camera, environment, null);

        System.out.println("Progressing simulation for ~10 seconds...");
        for (int i = 0; i < 10000; i++) {
            environment.update(settings.simulationUpdateDelta.get());
        }

        System.out.println("Creating render...");

        FrameBuffer fbo = new FrameBuffer(Pixmap.Format.RGBA8888,
                2048 * 2, 2048 * 2, false);

        ShaderProgram.pedantic = false;
        camera.update();
        FrameBufferManager fboManager = FrameBufferManager.getInstance();
        fboManager.begin(fbo);
        environmentRenderer.render(0);
        Pixmap pixmap = Pixmap.createFromFrameBuffer(0, 0, fbo.getWidth(), fbo.getHeight());
        pixmap.setBlending(Pixmap.Blending.None);
        fboManager.end();
        PixmapIO.writePNG(Gdx.files.local(bgFilename), pixmap);
        Gdx.app.exit();
    }

    public static void main(String[] args) {
        new Lwjgl3Application(new DefaultBackgroundGenerator(), new Lwjgl3ApplicationConfiguration());
    }

    public static EnvironmentSettings createBgEnvSettings() {
        EnvironmentSettings settings = EnvironmentSettings.createDefault();
        settings.world.seed.set(3L);
//        settings.world.radius.set(3f);
//        settings.world.numRingClusters.set(2);
//        settings.world.rockGenerationIterations.set(100);
//        settings.world.numInitialProtozoa.set(100);
//        settings.world.numInitialPlantPellets.set(500);
//        settings.world.generateLightNoiseTexture.set(false);
//        settings.world.bakeRockLights.set(false);
//        settings.misc.maxProtozoa.set(500);
//        settings.misc.maxPlants.set(2000);
//        settings.chemicalFieldResolution.set(512);
//        settings.lightMapResolution.set(64);
//        settings.world.populationClusterRadius.set(settings.world.radius.get());
        return settings;
    }
}
