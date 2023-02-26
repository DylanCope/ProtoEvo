package com.protoevo;

import com.protoevo.core.ApplicationManager;

import java.util.Map;

import static com.protoevo.utils.Utils.parseArgs;

// Please note that on macOS your application needs to be started with the -XstartOnFirstThread JVM argument
public class DesktopLauncher {

	public static void main(String[] args) {
		System.out.println("Current JVM version: " + System.getProperty("java.version"));
		Map<String, String> argsMap = parseArgs(args);

		ApplicationManager app = new ApplicationManager();

		if (argsMap.containsKey("headless"))
			app.switchToHeadlessMode();

		app.launch();
	}
}
