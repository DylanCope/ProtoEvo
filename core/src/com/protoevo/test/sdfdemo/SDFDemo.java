package com.protoevo.test.sdfdemo;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.protoevo.ui.GraphicsAdapter;
import com.protoevo.utils.DebugMode;

import static com.protoevo.core.ApplicationManager.launchedWithDebug;

public class SDFDemo extends Game {

    public static void main(String[] args) {
        boolean headless = false;
        boolean windowed = true;

        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setForegroundFPS(60);
        boolean isDebug = launchedWithDebug();

        if (isDebug)
            DebugMode.setMode(DebugMode.SIMPLE_INFO);

        if (isDebug | windowed) {
            config.setWindowedMode(1920, 1080);
//        } else if (borderlessWindowed) {
//            Graphics.DisplayMode displayMode = Gdx.graphics.getDisplayMode();
//            Gdx.graphics.setUndecorated(true);
//            Gdx.graphics.setWindowedMode(displayMode.width, displayMode.height);
        } else {
            config.setFullscreenMode(Lwjgl3ApplicationConfiguration.getDisplayMode());
        }

        config.setBackBufferConfig(
                8, 8, 8, 8, 16, 0,
                GraphicsAdapter.settings.msaaSamples.get()); // 8, 8, 8, 8, 16, 0 are default values

//        config.setWindowIcon(Files.FileType.Internal, "app-icon.png");

        config.useVsync(true);
//        config.setTitle("ProtoEvo");

        new Lwjgl3Application(new SDFDemo(), config);
    }

    @Override
    public void create() {
        // TODO Auto-generated method stub
        setScreen(new SDFDemoScreen());
    }
}
