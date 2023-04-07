package com.protoevo.utils;

import com.badlogic.gdx.Gdx;
import jcuda.Pointer;
import jcuda.Sizeof;
import jcuda.driver.*;

import java.io.File;
import java.io.IOException;

import static jcuda.driver.JCudaDriver.*;

public class JCudaKernelRunner {

    private CUfunction function;
    private final int blockSizeX, blockSizeY;
    private final String kernelName, functionName;

    public static boolean cudaAvailable() {
        try {
            new JCudaKernelRunner("diffusion");
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    public JCudaKernelRunner(String kernelName) {
        this(kernelName, "kernel", 32, 32);
    }

    public JCudaKernelRunner(String kernelName, String functionName, int blockSizeX, int blockSizeY) {
        this.blockSizeX = blockSizeX;
        this.blockSizeY = blockSizeY;
        this.kernelName = kernelName;
        this.functionName = functionName;

        // Enable exceptions and omit all subsequent error checks
        JCudaDriver.setExceptionsEnabled(true);
        initialise();
    }

    private void initialise() {
        // Initialize the driver and create a context for the first device.
        cuInit(0);
        CUdevice device = new CUdevice();
        cuDeviceGet(device, 0);
        CUcontext context = new CUcontext();
        cuCtxCreate(context, 0, device);

        String kernelPath = "kernels/" + kernelName + ".cu";
        try {
            String ptxFile = JCudaUtils.preparePtxFile(kernelPath);

            // Load the ptx file. Make sure to have compiled the cu files first.
            // e.g.: > nvcc -m64 -ptx kernel.cu
            // This step should now be handled by the preparePtxFile
            CUmodule module = new CUmodule();
            cuModuleLoad(module,  ptxFile);

            // Obtain a function pointer to the function to run.
            function = new CUfunction();
            cuModuleGetFunction(function, module, functionName);

        } catch (IOException e) {
            if (!new File(kernelName).exists())
                throw new RuntimeException("Was unable to compile " + kernelName + ":\n" + e);
        }
    }

    public CUdeviceptr[] getAdditionalParameters() {
        return null;
    }

    public byte[] processImage(byte[] pixels, int w, int h) {
        return processImage(pixels, pixels, w, h, 4);
    }

    public byte[] processImage(byte[] pixels, byte[] result, int w, int h) {
        return processImage(pixels, result, w, h, 4);
    }

    public byte[] processImage(byte[] pixels, byte[] result, int w, int h, int c) {

        // Allocate the device input data, and copy the
        // host input data to the device
        CUdeviceptr devicePixels = new CUdeviceptr();
        cuMemAlloc(devicePixels, (long) pixels.length * Sizeof.BYTE);
        cuMemcpyHtoD(devicePixels, Pointer.to(pixels), (long) pixels.length * Sizeof.BYTE);

        // Allocate device output memory
        CUdeviceptr deviceOutput = new CUdeviceptr();
        cuMemAlloc(deviceOutput, (long) result.length * Sizeof.BYTE);

        Pointer kernelParameters = Pointer.to(
                Pointer.to(new int[]{w}),
                Pointer.to(new int[]{h}),
                Pointer.to(new int[]{c}),
                Pointer.to(devicePixels),
                Pointer.to(deviceOutput)
        );


        // Set up the kernel parameters: A pointer to an array
        // of pointers which point to the actual values.

        // Call the kernel function.
        int gridSizeX = (int) Math.ceil((double) w / blockSizeX);
        int gridSizeY = (int) Math.ceil((double) h / blockSizeY);
        cuLaunchKernel(function,
                gridSizeX, gridSizeY, 1,      // Grid dimension
                blockSizeX, blockSizeY, 1,      // Block dimension
                0, null,               // Shared memory size and stream
                kernelParameters, null // Kernel- and extra parameters
        );

        cuCtxSynchronize();

        // Allocate host output memory and copy the device output
        // to the host.
        cuMemcpyDtoH(Pointer.to(result), deviceOutput, (long) result.length * Sizeof.BYTE);
        cuMemFree(devicePixels);
        cuMemFree(deviceOutput);

        return result;
    }
}
