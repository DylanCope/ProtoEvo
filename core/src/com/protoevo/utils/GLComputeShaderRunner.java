package com.protoevo.utils;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.opengl.GL43C.*;

import org.lwjgl.BufferUtils;

import com.protoevo.core.ApplicationManager;


public class GLComputeShaderRunner {
    static final int FILTER_SIZE = 3;

    private final int blockSizeX, blockSizeY;
    private final String kernelName;
    private int program, computeShader;
    private long window;
    private boolean initialized = false;
    private ByteBuffer inputBuffer = BufferUtils.createByteBuffer(1024*1024*4);
    private ByteBuffer outputBuffer = BufferUtils.createByteBuffer(1024*1024*4);

    /* OpenGL resources */
    private int[] textures = new int[2];

    public GLComputeShaderRunner(String kernelName) {
        this(kernelName, "kernel", 8, 8);
    }

    public GLComputeShaderRunner(String kernelName, String functionName, int blockSizeX, int blockSizeY) {
        System.out.println("Creating GLComputeShaderRunner");
        this.blockSizeX = blockSizeX;
        this.blockSizeY = blockSizeY;
        this.kernelName = kernelName;
    }

    private void initialise() {
        if (initialized)
            return;

        int error = 0;
        // Create the compute shader
        if (!glfwInit())
            throw new AssertionError("Failed to initialize GLFW");

        window = ApplicationManager.window;
        if (window == NULL)
            throw new AssertionError("Failed to create GLFW window");
        glfwMakeContextCurrent(window);

        computeShader = glCreateShader(GL_COMPUTE_SHADER);

        // Load the shader source code
        String shaderSource = null;
        try {
            shaderSource = new String(Files.readAllBytes(Paths.get("shaders/compute/" + kernelName + ".cs.glsl")), StandardCharsets.UTF_8);
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
        if (!initialized) {
            if (ApplicationManager.window == NULL) {
                System.out.println("Window is null");
                return result;
            } else {
                initialise();
            }
        }
        glBindImageTexture(0, textures[0], 0, false, 0, GL_READ_WRITE, GL_RGBA8UI);
        glBindImageTexture(1, textures[1], 0, false, 0, GL_READ_WRITE, GL_RGBA8UI);

        // Bind the program and set the input and output buffer bindings
        glUseProgram(program);

        // Set the kernel parameters
        glUniform1i(glGetUniformLocation(program, "width"), w);
        glUniform1i(glGetUniformLocation(program, "height"), h);
        glUniform1i(glGetUniformLocation(program, "channels"), c);

        // Copy the input to the input buffer and then to shader
        inputBuffer.position(0);
        inputBuffer.put(pixels);
        inputBuffer.position(0);

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
        outputBuffer.position(0);
        glGetTexImage(GL_TEXTURE_2D, 0, GL_RGBA_INTEGER, GL_UNSIGNED_BYTE, outputBuffer);
        glBindTexture(GL_TEXTURE_2D, 0);
        outputBuffer.get(result);

        return result;
    }
}
