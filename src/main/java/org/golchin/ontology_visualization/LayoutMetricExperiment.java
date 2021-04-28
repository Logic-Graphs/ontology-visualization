package org.golchin.ontology_visualization;

import com.google.common.math.Stats;
import lombok.Getter;
import org.golchin.ontology_visualization.metrics.layout.LayoutMetric;

import java.util.*;

public class LayoutMetricExperiment {
    @Getter
    private final Map<String, Integer> bestLayoutAlgorithmNames;
    // inner list contains runs with different random seeds within a trial
    private final Map<String, List<Double>> metricValuesByLayout;
    @Getter
    private final Map<String, Stats> statsByLayout;
    @Getter
    private final String bestLayoutName;
    @Getter
    private Map<String, LayoutVariant> bestVariantByLayout;

    public LayoutMetricExperiment(LayoutMetric layoutMetric, List<EvaluatedLayout> trials) {
        bestLayoutAlgorithmNames = new HashMap<>();
        metricValuesByLayout = new HashMap<>();
        bestVariantByLayout = new HashMap<>();
        Comparator<Double> comparator = layoutMetric.getComparator();
        for (EvaluatedLayout trial : trials) {
            String bestLayoutName = trial.getLayoutName();
            bestLayoutAlgorithmNames.merge(bestLayoutName, 1, Integer::sum);
            trial.getVariants().forEach((layoutName, layoutVariant) -> {
                LayoutVariant bestVariant = bestVariantByLayout.get(layoutName);
                if (bestVariant == null || comparator.compare(layoutVariant.getAverageMetricValue(), bestVariant.getAverageMetricValue()) > 0) {
                    bestVariantByLayout.put(layoutName, layoutVariant);
                }
                metricValuesByLayout.computeIfAbsent(layoutName, __ -> new ArrayList<>())
                        .addAll(layoutVariant.getAllMetricValues());
            });
        }
        statsByLayout = new HashMap<>();
        metricValuesByLayout.forEach((layout, metricValues) -> statsByLayout.put(layout, Stats.of(metricValues)));
        bestLayoutName = Collections.max(bestLayoutAlgorithmNames.entrySet(), Map.Entry.comparingByValue())
                .getKey();
    }
}
