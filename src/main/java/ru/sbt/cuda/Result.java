package ru.sbt.cuda;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by vikont on 19.12.16.
 */
public class Result {
    private final long simpleTime;
    private final long cudaTime;

    public Result(long simpleTime, long cudaTime) {
        this.simpleTime = simpleTime;
        this.cudaTime = cudaTime;
    }

    public long getSimpleTime() {
        return simpleTime;
    }

    public long getCudaTime() {
        return cudaTime;
    }

    static Result med(Iterable<Result> results){
        ArrayList<Long> simpleTimes = new ArrayList<>();
        ArrayList<Long> cudaTimes = new ArrayList<>();
        int med = 0;
        for (Result result : results){
            simpleTimes.add(result.simpleTime);
            cudaTimes.add(result.cudaTime);
            med++;
        }
        med = med / 2;
        Collections.sort(simpleTimes);
        Collections.sort(cudaTimes);
        return new Result(simpleTimes.get(med), cudaTimes.get(med));
    }
}
