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

    public static String formatUpToNPlaces(double d, int n) {
        if (n < 0) n = 0;
        String format = String.format("%." + n + "f", d);
        return stripTrailingZeros(format);
    }

    public static String stripTrailingZeros(String format) {
        int dotIndex = format.indexOf('.');
        if (dotIndex < 0)
            return format;
        int rightmostNonZeroIndex = format.length() - 1;
        while (rightmostNonZeroIndex > dotIndex && format.charAt(rightmostNonZeroIndex) == '0') {
            rightmostNonZeroIndex--;
        }
        if (format.charAt(rightmostNonZeroIndex) == '.')
            rightmostNonZeroIndex--;
        return format.substring(0, rightmostNonZeroIndex + 1);
    }
}
