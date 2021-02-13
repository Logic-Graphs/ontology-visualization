package org.golchin.ontology_visualization;

import javafx.util.Pair;

import java.util.function.Supplier;

public class Util {
    public static <T> Pair<T, Long> measureTimeMillis(Supplier<T> action) {
        long begin = System.nanoTime();
        T result = action.get();
        long time = (System.nanoTime() - begin) / 1000;
        return new Pair<>(result, time);
    }
}
