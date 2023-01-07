package com.protoevo;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.github.javafaker.App;
import com.protoevo.core.Application;
import com.protoevo.utils.DebugMode;

// Please note that on macOS your application needs to be started with the -XstartOnFirstThread JVM argument
public class DesktopLauncher {
	public static void main (String[] arg) {
		Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
		config.setForegroundFPS(60);
		boolean isDebug = java.lang.management.ManagementFactory.getRuntimeMXBean().
				getInputArguments().toString().contains("-agentlib:jdwp");

		isDebug = true;

		if (isDebug) {
			config.setWindowedMode(1920, 1080);
			DebugMode.setMode(DebugMode.SIMPLE_INFO);
		} else {
			config.setFullscreenMode(Lwjgl3ApplicationConfiguration.getDisplayMode());
		}
		config.useVsync(true);
		config.setTitle("ProtoEvo");
		new Lwjgl3Application(new Application(), config);
	}
}
