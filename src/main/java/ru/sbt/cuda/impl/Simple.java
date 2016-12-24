package ru.sbt.cuda.impl;

import ru.sbt.cuda.interfaces.ILife;

import java.util.Objects;

/**
 * Created by vikont on 19.12.16.
 */
public class Simple implements ILife {
    @Override
    public byte[][] calculate(byte[][] start, int iterations) {
        if (Objects.isNull(start)) throw new IllegalArgumentException("Input array is null");
        int width = start.length;
        int height = start[0].length;
        if ((width == 0) || (height == 0)) throw new IllegalArgumentException("Input array incorrect size");
        return calculate(start, iterations, width, height);
    }

    private byte[][] calculate(byte[][] start, int iterations, int width, int height){
        byte[][] first = new byte[width][height];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                first[i][j] = start[i][j];
            }
        }
        byte[][] second = new byte[width][height];
        byte[][] tmp;
        for (int i = 0; i < iterations; i++) {
            iterate(first, second, width, height);
            tmp = first;
            first = second;
            second = tmp;
        }
        return first;
    }

    private void iterate(byte[][] first, byte[][] second, int width, int height) {
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                int right = (j + 1) % width;
                int left = (j + width - 1) % width;

                int top = (i + height - 1) % height;
                int down = (i + 1) % height;

                int aliveCells =    first[top][left]  + first[top][j]   + first[top][right] +
                                    first[i][left]                      + first[i][right] +
                                    first[down][left] + first[down][j]  + first[down][right];
                second[i][j] = ((aliveCells == 3) || ((aliveCells == 2) && (first[i][j] == 1))) ? (byte)1 : (byte)0;
            }
        }
    }


}
