package org.golchin.ontology_visualization;

import javafx.util.Pair;

import java.util.function.Supplier;

public class Util {
    public static <T> Pair<T, Double> measureTimeMillis(Supplier<T> action) {
        long begin = System.nanoTime();
        T result = action.get();
        double time = (double) (System.nanoTime() - begin) / 1_000_000;
        return new Pair<>(result, time);
    }
}
