package com.protoevo.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.protoevo.settings.Parameter;
import com.protoevo.settings.Settings;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ApplicationSettings extends Settings {

    public final Parameter<String> displayMode = new Parameter<>(
            "Display Mode",
            "Application display mode.",
            "Borderless Window"
    ).setOptions("Fullscreen", "Borderless Window", "Windowed");

    public final Parameter<Integer> windowWidth = new Parameter<>(
            "Window Width",
            "Application width, if windowed.",
            1920
    );

    public final Parameter<Integer> windowHeight = new Parameter<>(
            "Window Height",
            "Application height, if windowed.",
            1080
    );

    public final Parameter<Integer> fpsCap = new Parameter<>(
            "Max FPS",
            "Maximum frames per second.",
            60
    );
    public final Parameter<Integer> msaaSamples = new Parameter<>(
            "Anti-Aliasing Samples",
            "",
            16
    );

    public ApplicationSettings() {
        super("Application");
    }

    public static ApplicationSettings load() {
        ApplicationSettings settings;
        Path settingsPath = Paths.get("settings.dat");
        if (Files.exists(settingsPath)) {
            try {
                ObjectInputStream inputStream = new ObjectInputStream(
                        Files.newInputStream(settingsPath)
                );
                settings = (ApplicationSettings) inputStream.readObject();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            settings = new ApplicationSettings();
            settings.collectParameters();
        }
        return settings;
    }

    public void save() {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream("settings.dat");
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(this);
            objectOutputStream.flush();
            objectOutputStream.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
