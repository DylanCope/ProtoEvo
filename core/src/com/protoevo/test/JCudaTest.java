package com.protoevo.test;

import com.protoevo.utils.JCudaKernelRunner;
import jcuda.Pointer;
import jcuda.Sizeof;
import jcuda.driver.*;

import javax.imageio.ImageIO;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static jcuda.driver.JCudaDriver.*;

public class JCudaTest {

    private static final int[] convFilter = new int[] {
            1, 2, 1,
            2, 4, 2,
            1, 2, 1
    };

    private void processTestImage() {
        try {
            // Enable exceptions and omit all subsequent error checks
            JCudaDriver.setExceptionsEnabled(true);

            // Create the PTX file by calling the NVCC
            String ptxFileName = "kernels/diffusion.ptx";

            // Initialize the driver and create a context for the first device.
            cuInit(0);
            CUdevice device = new CUdevice();
            cuDeviceGet(device, 0);
            CUcontext context = new CUcontext();
            cuCtxCreate(context, 0, device);

            // Load the ptx file.
            CUmodule module = new CUmodule();
            cuModuleLoad(module, ptxFileName);

            // Obtain a function pointer to the "add" function.
            CUfunction function = new CUfunction();
            cuModuleGetFunction(function, module, "kernel");

            BufferedImage bi = ImageIO.read(new File("test/test.png"));
            int w = bi.getWidth();
            int h = bi.getHeight();
            int c = bi.getColorModel().getNumComponents();
            int[] pixels = new int[w * h * c];
            int[] result = new int[w * h * c];

    //            BufferedImage grayscaleImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    //            Graphics g = grayscaleImg.getGraphics();
    //            g.drawImage(bi, 0, 0, null);
    //            g.dispose();

            pixels = bi.getRaster().getPixels(0, 0, w, h, pixels);

            long start = System.currentTimeMillis();

            int numElements = pixels.length;

            // Allocate the device input data, and copy the
            // host input data to the device
            CUdeviceptr devicePixels = new CUdeviceptr();
            cuMemAlloc(devicePixels, numElements * Sizeof.INT);
            cuMemcpyHtoD(devicePixels, Pointer.to(pixels), numElements * Sizeof.INT);

            // Allocate device output memory
            CUdeviceptr deviceOutput = new CUdeviceptr();
            cuMemAlloc(deviceOutput, numElements * Sizeof.INT);

            // Set up the kernel parameters: A pointer to an array
            // of pointers which point to the actual values.
            Pointer kernelParameters = Pointer.to(
                    Pointer.to(new int[]{w}),
                    Pointer.to(new int[]{h}),
                    Pointer.to(new int[]{c}),
                    Pointer.to(devicePixels),
                    Pointer.to(deviceOutput)
            );

            // Call the kernel function.
            int blockSizeX = 32;
            int blockSizeY = 32;
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
            int hostOutput[] = new int[numElements];
            cuMemcpyDtoH(Pointer.to(hostOutput), deviceOutput, numElements * Sizeof.INT);

            System.out.println("Time " + (System.currentTimeMillis() - start));

            bi.getRaster().setPixels(0, 0, w, h, hostOutput);

            ImageIO.write(bi, "PNG", new File("test/cuda-result.png"));
            cuMemFree(devicePixels);
            cuMemFree(deviceOutput);
        } catch (RuntimeException e) {
            throw e;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void testJCudaKernelRunner() {

        BufferedImage bi;
        try {
            bi = ImageIO.read(new File("test/test.png"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        int w = bi.getWidth();
        int h = bi.getHeight();
        int c = bi.getColorModel().getNumComponents();
        int[] pixels = new int[w * h * c];
        pixels = bi.getRaster().getPixels(0, 0, w, h, pixels);
        int[] result = new int[w * h * c];

        JCudaKernelRunner kernelRunner = new JCudaKernelRunner("diffusion");
//        for (int i = 0; i < 1; i++) {
            kernelRunner.processImage(pixels, result, w, h, c);
//            int[] tmp = pixels;
//            pixels = result;
//            result = tmp;
//        }

        bi.getRaster().setPixels(0, 0, w, h, result);
        try {
            ImageIO.write(bi, "PNG", new File("test/cuda-result2.png"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        JCudaTest tester = new JCudaTest();
//        tester.processTestImage();
        tester.testJCudaKernelRunner();
    }
}
