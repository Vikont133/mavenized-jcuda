package ru.sbt.cuda;

import ru.sbt.cuda.impl.Cuda;
import ru.sbt.cuda.impl.Simple;
import ru.sbt.cuda.interfaces.ILife;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

/**
 * Created by vikont on 19.12.16.
 */
public class Main {

    public static final int EXPERIMENT_COUNT = 10;
    private static final int[] iterations = new int[]{10, 50, 100, 500, 1000, 5000, 10000};
    private static final int[] worldSize = new int[]{16, 32, 64, 128, 256, 512, 1024, 2048};
    private static final ILife cuda = new Cuda();
    private static final ILife simple = new Simple();

    public static Result experiment(int worldSize, int iterations, int count) throws IOException {
        ArrayList<Result> results = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            results.add(experiment(worldSize, iterations));
        }
        return Result.med(results);
    }

    public static Result experiment(int worldSize, int iterations) throws IOException {
        Random random = new Random();
        byte[][] start = new byte[worldSize][worldSize];
        for (int i = 0; i < worldSize; i++) {
            for (int j = 0; j < worldSize; j++) {
                start[i][j] = (byte) (random.nextBoolean() ? 1 : 0);
            }
        }
        long startTime;
        startTime = System.nanoTime();
        cuda.calculate(start, iterations);
        long cudaTime = System.nanoTime() - startTime;
        startTime = System.nanoTime();
        simple.calculate(start, iterations);
        long simpleTime = System.nanoTime() - startTime;
        return new Result(simpleTime, cudaTime);
    }

    public static void main(String[] args) throws IOException {
        System.out.printf("%18s%18s%18s%18s\n", "SIZE", "ITERATIONS", "CUDA", "SIMPLE");
        for (int i = 0; i < iterations.length; i++) {
            for (int j = 0; j < worldSize.length; j++) {
                Result result = experiment(worldSize[j], iterations[i], EXPERIMENT_COUNT);
                System.out.printf("%18d%18d%18d%18d\n", worldSize[j], iterations[i], result.getCudaTime(), result.getSimpleTime());
            }
        }
    }
}
