package com.protoevo.utils;

import jcuda.Pointer;
import jcuda.Sizeof;
import jcuda.driver.*;

import java.io.IOException;

import static jcuda.driver.JCudaDriver.*;

public class JCudaKernelRunner {

    private final CUfunction function;
    private final int blockSizeX, blockSizeY;

    public JCudaKernelRunner(String kernelName) {
        this(kernelName, "kernel", 32, 32);
    }

    public JCudaKernelRunner(String kernelName, String functionName, int blockSizeX, int blockSizeY) {
        this.blockSizeX = blockSizeX;
        this.blockSizeY = blockSizeY;

        // Enable exceptions and omit all subsequent error checks
        JCudaDriver.setExceptionsEnabled(true);

        // Initialize the driver and create a context for the first device.
        cuInit(0);
        CUdevice device = new CUdevice();
        cuDeviceGet(device, 0);
        CUcontext context = new CUcontext();
        cuCtxCreate(context, 0, device);
        try {
            String ptxFile = JCudaUtils.preparePtxFile("kernels/" + kernelName + ".cu");

            // Load the ptx file. Make sure to have compiled the cu files first.
            // e.g.: > nvcc -m64 -ptx kernel.cu
            // This step should now be handled by the preparePtxFile
            CUmodule module = new CUmodule();
            cuModuleLoad(module,  ptxFile);

            // Obtain a function pointer to the function to run.
            function = new CUfunction();
            cuModuleGetFunction(function, module, functionName);

        } catch (IOException e) {
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

//        CUdeviceptr[] additionalParameters = getAdditionalParameters();
//        boolean hasAdditionalParameters = additionalParameters != null && additionalParameters.length > 0;

        int nBaseParams = 5;
//        int nParams = hasAdditionalParameters ? nBaseParams + additionalParameters.length : nBaseParams;
//        Pointer[] parameters = new Pointer[5];
//        parameters[0] = Pointer.to(Pointer.to(new int[]{w}));
//        parameters[1] = Pointer.to(Pointer.to(new int[]{h}));
//        parameters[2] = Pointer.to(Pointer.to(new int[]{c}));
//        parameters[3] = Pointer.to(devicePixels);
//        parameters[4] = Pointer.to(deviceOutput);
        Pointer kernelParameters = Pointer.to(
                Pointer.to(new int[]{w}),
                Pointer.to(new int[]{h}),
                Pointer.to(new int[]{c}),
                Pointer.to(devicePixels),
                Pointer.to(deviceOutput)
        );


//        if (hasAdditionalParameters) {
//            for (CUdeviceptr additionalParameter : additionalParameters) {
//                cuMemAlloc(additionalParameter, (long) pixels.length * Sizeof.INT);
//            }
//            for (int i = 0; i < additionalParameters.length; i++) {
//                parameters[i + nBaseParams] = Pointer.to(additionalParameters[i]);
//            }
//        }

        // Set up the kernel parameters: A pointer to an array
        // of pointers which point to the actual values.
//        Pointer kernelParameters = Pointer.to(parameters);

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
//        int[] hostOutput = new int[result.length];
        cuMemcpyDtoH(Pointer.to(result), deviceOutput, (long) result.length * Sizeof.BYTE);
        cuMemFree(devicePixels);
        cuMemFree(deviceOutput);

        return result;
    }
}
