package com.protoevo.test.sdfdemo;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.protoevo.core.ApplicationManager;
import com.protoevo.ui.GraphicsAdapter;
import com.protoevo.utils.DebugMode;

import static com.protoevo.core.ApplicationManager.launchedWithDebug;

public class SDFDemo extends Game {

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
//        config.setForegroundFPS(60);
        boolean isDebug = launchedWithDebug();

        if (isDebug)
            DebugMode.setMode(DebugMode.SIMPLE_INFO);

        config.setWindowedMode(1024, 1024);
        config.setBackBufferConfig(
                8, 8, 8, 8, 16, 0,
                ApplicationManager.settings.msaaSamples.get()); // 8, 8, 8, 8, 16, 0 are default values

        config.useVsync(true);
        new Lwjgl3Application(new SDFDemo(), config);
    }

    @Override
    public void create() {
        // TODO Auto-generated method stub
        setScreen(new SDFCellDemoScreen());
    }
}
