package com.protoevo.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class JCudaUtils {    /**
     * The extension of the given file name is replaced with "ptx".
     * If the file with the resulting name does not exist, it is
     * compiled from the given file using NVCC. The name of the
     * PTX file is returned.
     *
     * @param cuFileName The name of the .CU file
     * @return The name of the PTX file
     * @throws IOException If an I/O error occurs
     */
    public static String preparePtxFile(String cuFileName) throws IOException
    {
        if (DebugMode.isDebugMode())
            System.out.println("Compiling CUDA kernel...");
        int endIndex = cuFileName.lastIndexOf('.');
        if (endIndex == -1) {
            endIndex = cuFileName.length()-1;
        }

        String ptxFileName = cuFileName.substring(0, endIndex+1) + "ptx";

        File cuFile = new File(cuFileName);
        if (DebugMode.isDebugMode())
            System.out.println(cuFile.getCanonicalPath());
        if (!cuFile.exists()) {
            throw new IOException("Input file not found: " + cuFileName);
        }
        String modelString = "-m" + System.getProperty("sun.arch.data.model");
        String command =
                "nvcc " + modelString + " -ptx " + cuFile.getPath() + " -o " + ptxFileName;

        if (DebugMode.isDebugMode())
            System.out.println(command);
        Process process = Runtime.getRuntime().exec(command);

        String errorMessage =
                new String(toByteArray(process.getErrorStream()));
        String outputMessage =
                new String(toByteArray(process.getInputStream()));

        if (DebugMode.isDebugMode()) {
            System.out.println(outputMessage);
            System.out.println(errorMessage);
        }

        int exitValue = 0;
        try
        {
            exitValue = process.waitFor();
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new IOException(
                    "Interrupted while waiting for nvcc output", e);
        }

        if (exitValue != 0)
        {
            StringBuilder message = new StringBuilder();
            message.append("Could not create .ptx file: \n")
                    .append("Exit code: ").append(exitValue).append("\n")
                    .append(outputMessage).append("\n")
                    .append(errorMessage).append("\n");
            throw new IOException(
                    "Could not create .ptx file: " + message);
        }

        if (DebugMode.isDebugMode())
            System.out.println("Success.");
        return ptxFileName;
    }

    /**
     * Fully reads the given InputStream and returns it as a byte array
     *
     * @param inputStream The input stream to read
     * @return The byte array containing the data from the input stream
     * @throws IOException If an I/O error occurs
     */
    private static byte[] toByteArray(InputStream inputStream)
            throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte buffer[] = new byte[8192];
        while (true)
        {
            int read = inputStream.read(buffer);
            if (read == -1)
            {
                break;
            }
            baos.write(buffer, 0, read);
        }
        return baos.toByteArray();
    }
}
