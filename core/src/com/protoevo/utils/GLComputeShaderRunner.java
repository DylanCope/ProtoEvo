package com.protoevo.utils;

import com.badlogic.gdx.Gdx;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL;

import java.nio.ByteBuffer;

import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.opengl.GL43C.*;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.glfw.GLFW.*;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;


public class GLComputeShaderRunner {
    /**
     * A simple thread pool for executing tasks on a separate thread.
     * Once simulation runs on a different thread than the main/LibGDX thread, this should be
     * removed and run directly on the simulation thread.
     */
    private class GLThread {
        private final ExecutorService executor = Executors.newSingleThreadExecutor();

        public void execute(Runnable task) {
            FutureTask<?> futureTask = new FutureTask<>(task, null);
            executor.execute(futureTask);
            try {
                futureTask.get();
            } catch (InterruptedException e) {
                System.out.println("Error executing task: " + e);
            } catch (ExecutionException e){
                System.out.println("Error executing task: " + e);
            }
        }

        public void shutdown() {
            executor.shutdown();
        }
    }

    static final int FILTER_SIZE = 3;

    private final int blockSizeX, blockSizeY;
    private final String kernelName;
    private long window;
    private boolean initialized = false;
    private ByteBuffer inputBuffer = BufferUtils.createByteBuffer(1024*1024*4);
    private ByteBuffer outputBuffer = BufferUtils.createByteBuffer(1024*1024*4);
    private GLThread glThread = new GLThread();

    /* OpenGL resources */
    private int program, computeShader;
    private int[] textures = new int[2];

    public GLComputeShaderRunner(String kernelName) {
        this(kernelName, "kernel", 8, 8);
    }

    public GLComputeShaderRunner(String kernelName, String functionName, int blockSizeX, int blockSizeY) {
        System.out.println("Creating GLComputeShaderRunner");
        this.blockSizeX = blockSizeX;
        this.blockSizeY = blockSizeY;
        this.kernelName = kernelName;

        glThread.execute(() -> {
            initialise();
        });
    }

    private void initialise() {
        if (initialized)
            return;

        if (!glfwInit())
            throw new AssertionError("Failed to initialize GLFW");


        int error = 0;

        // Create opengl context
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_MAXIMIZED, GLFW_TRUE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        window = glfwCreateWindow(1, 1, "hidden_window", NULL, NULL);

        System.out.println("Window: " + window);

        if (window == NULL)
            throw new AssertionError("Failed to create GLFW window");
        glfwMakeContextCurrent(window);
        GL.createCapabilities();

        computeShader = glCreateShader(GL_COMPUTE_SHADER);

        // Load the shader source code
        String shaderSource;
        try {
            shaderSource = Gdx.files.internal("shaders/compute/" + kernelName + ".cs.glsl").readString();
//            shaderSource = new String(Files.readAllBytes(Paths.get("shaders/compute/" + kernelName + ".cs.glsl")), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Was unable to load " + kernelName + ":\n" + e);
        }

        // Compile the compute shader
        glShaderSource(computeShader, shaderSource);
        glCompileShader(computeShader);
        if (glGetShaderi(computeShader, GL_COMPILE_STATUS) != GL_TRUE) {
            throw new RuntimeException("Failed to compile compute shader:\n" + glGetShaderInfoLog(computeShader));
        }

        // Create the program and attach the compute shader
        program = glCreateProgram();
        error = glGetError();
        if (error != GL_NO_ERROR) {
            System.out.println("Error creating GL program: " + error);
        }

        glAttachShader(program, computeShader);

        // Link the program
        glLinkProgram(program);
        if (glGetProgrami(program, GL_LINK_STATUS) != GL_TRUE) {
            throw new RuntimeException("Failed to link program:\n" + glGetProgramInfoLog(program));
        }

        glUseProgram(program);

        // Delete the compute shader
        glDeleteShader(computeShader);

        // Create output texture
        textures[0] = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textures[0]);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        final int texSize = 1024; // Was 2048
        glTexStorage2D(GL_TEXTURE_2D, 1, GL_RGBA8UI, texSize, texSize);

        // Create input texture
        textures[1] = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textures[1]);
        glBindImageTexture(1, textures[1], 0, false, 0, GL_READ_WRITE, GL_RGBA8UI);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        ByteBuffer inputBuffer = BufferUtils.createByteBuffer(texSize*texSize*4);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8I, texSize, texSize, 0, GL_RGBA_INTEGER, GL_BYTE, inputBuffer);

        error = glGetError();
        if (error != GL_NO_ERROR) {
            System.out.println("GL Compute Shader Runner Init Error: " + error);
        }

        initialized = true;
    }

    public byte[] processImage(byte[] pixels, int w, int h) {
        return processImage(pixels, pixels, w, h, 4);
    }

    public byte[] processImage(byte[] pixels, byte[] result, int w, int h) {
        return processImage(pixels, result, w, h, 4);
    }


    public byte[] processImage(byte[] pixels, byte[] result, int w, int h, int c) {
        glThread.execute(() -> {
            processImageJob(pixels, result, w, h, c);
        });

        return result;
    }

    public byte[] processImageJob(byte[] pixels, byte[] result, int w, int h, int c) {
        glBindImageTexture(0, textures[0], 0, false, 0, GL_READ_WRITE, GL_RGBA8UI);
        glBindImageTexture(1, textures[1], 0, false, 0, GL_READ_WRITE, GL_RGBA8UI);

        // Bind the program and set the input and output buffer bindings
        glUseProgram(program);

        // Set the kernel parameters
        glUniform1i(glGetUniformLocation(program, "width"), w);
        glUniform1i(glGetUniformLocation(program, "height"), h);
        glUniform1i(glGetUniformLocation(program, "channels"), c);

        // Copy the input to the input buffer and then to shader
        ((java.nio.Buffer) inputBuffer).rewind();
        inputBuffer.put(pixels);
        ((java.nio.Buffer) inputBuffer).rewind();

        glBindTexture(GL_TEXTURE_2D, textures[1]);
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, w, h, GL_RGBA_INTEGER, GL_BYTE, inputBuffer);
        glBindTexture(GL_TEXTURE_2D, 0);

        // Dispatch the compute shader
        int gridSizeX = (int) Math.ceil((double) w / blockSizeX);
        int gridSizeY = (int) Math.ceil((double) h / blockSizeY);

        // Compute runs in ~0ms
        glDispatchCompute(gridSizeX, gridSizeY, 1);

        // Wait for the compute shader to finish
        glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);

        // Get the output from shader to outputBuffer and then to result array
        glBindTexture(GL_TEXTURE_2D, textures[0]);
        glBindImageTexture(0, textures[0], 0, false, 0, GL_READ_ONLY, GL_RGBA8UI);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        ((java.nio.Buffer) outputBuffer).rewind();
        glGetTexImage(GL_TEXTURE_2D, 0, GL_RGBA_INTEGER, GL_UNSIGNED_BYTE, outputBuffer);
        glBindTexture(GL_TEXTURE_2D, 0);
        outputBuffer.get(result);

        return result;
    }
}
