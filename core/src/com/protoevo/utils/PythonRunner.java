package com.protoevo.utils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class PythonRunner {

    public static void runPython(String script, String args, boolean blocking) {
        try {
            // Create the process builder
            ProcessBuilder pb = new ProcessBuilder("python -m", script, args);
            pb.redirectErrorStream(true); // Redirect the error stream to the input stream

            // Start the process
            Process process = pb.start();

            // Read the output
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            if (blocking) {
                // Wait for the process to finish
                process.waitFor();
            }

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void runPython(String script, String args) {
        runPython(script, args, false);
    }
}
