package com.protoevo;

import com.badlogic.gdx.Gdx;
import com.protoevo.core.ApplicationManager;

// Please note that on macOS your application needs to be started with the -XstartOnFirstThread JVM argument
public class DesktopLauncher {

	public static void main(String[] arg) {
		System.out.println("Current JVM version: " + System.getProperty("java.version"));
		new ApplicationManager().launch();
	}
}
