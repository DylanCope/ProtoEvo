package com.protoevo;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.Texture;
import com.protoevo.core.Application;
import com.protoevo.settings.RenderSettings;
import com.protoevo.utils.DebugMode;
import com.badlogic.gdx.tools.texturepacker.TexturePacker;

// Please note that on macOS your application needs to be started with the -XstartOnFirstThread JVM argument
public class DesktopLauncher {

//	private static boolean windowed = false;
	private static boolean windowed = true;

	public static void main(String[] arg) {
		Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
		config.setForegroundFPS(60);
		boolean isDebug = java.lang.management.ManagementFactory.getRuntimeMXBean().
				getInputArguments().toString().contains("-agentlib:jdwp");

		if (isDebug)
			DebugMode.setMode(DebugMode.SIMPLE_INFO);
		if (isDebug | windowed) {
			config.setWindowedMode(1920, 1080);
		} else {
			config.setFullscreenMode(Lwjgl3ApplicationConfiguration.getDisplayMode());
		}

//		TexturePacker.Settings settings = new TexturePacker.Settings();
////		settings.maxWidth = 512;
////		settings.maxHeight = 512;
//		settings.bleed = true;
//		settings.bleedIterations = 100;
//		settings.premultiplyAlpha = true;
//		settings.filterMin = Texture.TextureFilter.Linear;
//		settings.filterMag = Texture.TextureFilter.Linear;
////		settings.filterMag = ;
//
//		String[] names = new String[]{"cell"};//, "flagella", "nodes"};
//		for (String name : names)
//			TexturePacker.process(settings, "textures/" + name, "textures", name);

		config.setBackBufferConfig(
				8, 8, 8, 8, 16, 0,
				RenderSettings.msaaSamples); // 8, 8, 8, 8, 16, 0 are default values

		config.useVsync(true);
		config.setTitle("ProtoEvo");
		new Lwjgl3Application(new Application(), config);
	}
}
