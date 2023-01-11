package com.protoevo.playground;

import com.aparapi.Kernel;
import com.aparapi.Range;
import com.aparapi.internal.opencl.OpenCLPlatform;

import java.util.stream.IntStream;


public class TestAparapi {

    private static final int SIZE = 1024 * 1024;
    private static final int PASSES = 100;
    private static final int CONV_RADIUS = 1;
    private static final int CONV_SIZE = 2*CONV_RADIUS + 1;
    private final float[] input = new float[SIZE];
    private final float[] input2 = new float[SIZE];
    private final float[] convKernel = new float[CONV_SIZE];
    private final float[] result = new float[SIZE];

    private void setInputs() {
        for (int i = 0; i < SIZE; i++) {
            input[i] = i % 2 == 0 ? 1.0f : 0.0f;
            input2[i] = i % 2 == 0 ? 1.0f : 0.0f;
        }

        for (int i = 0; i < CONV_SIZE; i++) {
            convKernel[i] = 1.0f;
        }
    }

    private void aparapiConvolve() {

        OpenCLPlatform.getUncachedOpenCLPlatforms().forEach(p -> {
            System.out.println("Platform: " + p.getName());
            p.getOpenCLDevices().forEach(d -> {
                System.out.println("Device: " + d.getName());
            });
        });

        setInputs();

        Kernel kernel = new Kernel() {
            @Override
            public void run() {
                int i = CONV_RADIUS + getGlobalId();
                result[i] = 0;
                for (int j = 0; j < CONV_SIZE; j++) {
                    result[i] += input[i + j - CONV_RADIUS] * convKernel[j];
                }
            }
        };

        System.out.println("Target Device: " + kernel.getTargetDevice() + "\n"
                + "Max Work Group Size: " + kernel.getTargetDevice().getMaxWorkGroupSize() + "\n"
                + "Type: " + kernel.getTargetDevice().getType() + "\n"
                + "Is Running OpenCL: " + kernel.isRunningCL());

        float time = System.nanoTime();
        kernel.execute(Range.create(SIZE - CONV_RADIUS), PASSES);
        time = System.nanoTime() - time;

        System.out.println("Total Run Time: " + time / 1000000 + " ms ("
                + kernel.getExecutionTime() + " ms spent executing)");

        if (kernel.getProfileInfo() != null) {
            System.out.println("Profile Info:");
            kernel.getProfileInfo()
                    .forEach(v -> System.out.println(v.getLabel() + ": " + v));
        }

        printResults();
        kernel.dispose();
    }

    private void parallelStreamConvolve() {
        setInputs();

        float time = System.nanoTime();
        for (int pass = 0; pass < PASSES; pass++)
            IntStream.range(CONV_RADIUS, SIZE - CONV_RADIUS).parallel().forEach(i -> {
                result[i] = 0;
                for (int j = 0; j < CONV_SIZE; j++) {
                    result[i] += input2[i + j - CONV_RADIUS] * convKernel[j];
                }
            });
        time = System.nanoTime() - time;

        System.out.println("Total Run Time: " + time / 1000000 + " ms");
        printResults();
    }

    private void printLongArray(float[] arr) {
        int printCount = 10;
        for (int i = 0; i < printCount; i++) {
            System.out.print(arr[i] + ", ");
        }
        System.out.print(" ...  ");
        for (int i = arr.length - printCount; i < arr.length; i++) {
            System.out.print(arr[i] + ", ");
        }
        System.out.println();
    }

    private void printResults() {
        System.out.println("Input:");
        printLongArray(input);
        System.out.println("Result:");
        printLongArray(result);
    }

    public TestAparapi() {
        System.out.println("Aparapi Convolution Test");
        aparapiConvolve();
        System.out.println();
        System.out.println("Parallel Stream Convolution Test");
        parallelStreamConvolve();
    }

    public static void main(String[] args) {
//        CL.create();
//        CLPlatform platform = CLPlatform.getPlatforms().get(0);
//        List<CLDevice> devices = platform.getDevices(CL_DEVICE_TYPE_GPU);

        new TestAparapi();
    }
}
