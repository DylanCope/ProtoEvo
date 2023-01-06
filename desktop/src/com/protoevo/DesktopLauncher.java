package com.protoevo;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.protoevo.core.Application;

// Please note that on macOS your application needs to be started with the -XstartOnFirstThread JVM argument
public class DesktopLauncher {
	public static void main (String[] arg) {
		Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
		config.setForegroundFPS(60);
//		config.setWindowedMode(1920, 1080);
		config.setFullscreenMode(Lwjgl3ApplicationConfiguration.getDisplayMode());
		config.useVsync(true);
		config.setTitle("ProtoEvo");
		new Lwjgl3Application(new Application(), config);
	}
}
