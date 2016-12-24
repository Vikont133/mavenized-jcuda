package ru.sbt.cuda.impl;

import jcuda.Pointer;
import jcuda.Sizeof;
import jcuda.driver.*;
import ru.sbt.cuda.interfaces.ILife;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import static jcuda.driver.JCudaDriver.*;

/**
 * Created by vikont on 18.12.16.
 */
public class Cuda implements ILife {

    private static final int BLOCK_WIDTH = 16;
    private static final int BLOCK_HEIGHT = 16;
    private static final int MAX_CELLS = 2048 * 2048;

    private static final String CU_FUNCTION_NAME = "lifeStep";
    private static final String CU_FILE_NAME = "LifeStep.cu";


    private CUmodule cuModule;
    private CUfunction cuFunction;

    public Cuda(){
        JCudaDriver.setExceptionsEnabled(true);

        // Create the PTX file by calling the NVCC
        String ptxFileName = null;
        try {
            ptxFileName = preparePtxFile(Cuda.class.getClassLoader().getResource(CU_FILE_NAME).getFile());
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Initialize the driver and create a context for the first device.
        cuInit(0);
        CUdevice device = new CUdevice();
        cuDeviceGet(device, 0);
        CUcontext context = new CUcontext();
        cuCtxCreate(context, 0, device);

        // Load the ptx file.
        cuModule = new CUmodule();
        cuModuleLoad(cuModule, ptxFileName);

        // Obtain a function
        cuFunction = new CUfunction();
        cuModuleGetFunction(cuFunction, cuModule, CU_FUNCTION_NAME);
    }

    public byte[][] calculate(byte[][] start, int iterations){
        if (Objects.isNull(start)) throw new IllegalArgumentException("Input array is null");
        int width = start.length;
        int height = start[0].length;
        if ((width == 0) || (height == 0)) throw new IllegalArgumentException("Input array incorrect size");
        if (width % BLOCK_WIDTH != 0) throw new IllegalArgumentException("Input array incorrect size");
        if (height % BLOCK_HEIGHT != 0) throw new IllegalArgumentException("Input array incorrect size");
        if (width * height > MAX_CELLS) throw new IllegalArgumentException("Input array incorrect size");
        return calculate(start, iterations, width, height);
    }

    private byte[][] calculate(byte[][] start, int iterations, int width, int height){
        CUdeviceptr pointersIn[] = new CUdeviceptr[width];
        for (int i = 0; i < width; i++) {
            pointersIn[i] = new CUdeviceptr();
            cuMemAlloc(pointersIn[i], height * Sizeof.BYTE);
            cuMemcpyHtoD(pointersIn[i], Pointer.to(start[i]), height * Sizeof.BYTE);
        }

        CUdeviceptr input = new CUdeviceptr();
        cuMemAlloc(input, width * Sizeof.POINTER);
        cuMemcpyHtoD(input, Pointer.to(pointersIn), width * Sizeof.POINTER);

        Pointer kernelParameters = Pointer.to(
                Pointer.to(input),
                Pointer.to(new int[]{width}),
                Pointer.to(new int[]{height})
        );

        for (int i = 0; i < iterations; i++) {
            cuLaunchKernel(cuFunction,
                    width / BLOCK_WIDTH,  height / BLOCK_HEIGHT, 1,      // Grid dimension
                    BLOCK_WIDTH, BLOCK_HEIGHT, 1,      // Block dimension
                    0, null,               // Shared memory size and stream
                    kernelParameters, null // Kernel- and extra parameters
            );
            cuCtxSynchronize();
        }

        byte[][] end = new byte[width][height];
        for (int i = 0; i < width; i++) {
            cuMemcpyDtoH(Pointer.to(end[i]), pointersIn[i], height * Sizeof.BYTE);
        }

        for (int i = 0; i < width; i++) {
            cuMemFree(pointersIn[i]);
        }
        cuMemFree(input);
        return end;
    }

    /**
     * The extension of the given file name is replaced with "ptx".
     * If the file with the resulting name does not exist, it is
     * compiled from the given file using NVCC. The name of the
     * PTX file is returned.
     *
     * @param cuFileName The name of the .CU file
     * @return The name of the PTX file
     * @throws IOException If an I/O error occurs
     */
    private static String preparePtxFile(String cuFileName) throws IOException
    {
        int endIndex = cuFileName.lastIndexOf('.');
        if (endIndex == -1)
        {
            endIndex = cuFileName.length()-1;
        }
        String ptxFileName = cuFileName.substring(0, endIndex+1)+"ptx";
        File ptxFile = new File(ptxFileName);
        if (ptxFile.exists())
        {
            return ptxFileName;
        }

        File cuFile = new File(cuFileName);
        if (!cuFile.exists())
        {
            throw new IOException("Input file not found: "+cuFileName);
        }
        String modelString = "-m" + System.getProperty("sun.arch.data.model");
        String command =
                "nvcc " + modelString + " -ptx "+
                        cuFile.getPath()+" -o "+ptxFileName;

        System.out.println("Executing\n"+command);
        Process process = Runtime.getRuntime().exec(command);

        String errorMessage =
                new String(toByteArray(process.getErrorStream()));
        String outputMessage =
                new String(toByteArray(process.getInputStream()));
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
            System.out.println("nvcc process exitValue "+exitValue);
            System.out.println("errorMessage:\n"+errorMessage);
            System.out.println("outputMessage:\n"+outputMessage);
            throw new IOException(
                    "Could not create .ptx file: "+errorMessage);
        }

        System.out.println("Finished creating PTX file");
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

