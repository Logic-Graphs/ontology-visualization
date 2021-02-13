package org.golchin.ontology_visualization;

import com.google.common.math.Stats;
import org.golchin.ontology_visualization.metrics.layout.LayoutMetric;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LayoutMetricExperiment {
    private final LayoutMetric layoutMetric;
    private final List<EvaluatedLayout> trials;
    private final Set<String> bestLayoutAlgorithmNames;
    // inner list contains runs with different random seeds within an attempt
    private final List<List<Double>> metricValues;

    public LayoutMetricExperiment(LayoutMetric layoutMetric, List<EvaluatedLayout> trials) {
        this.layoutMetric = layoutMetric;
        this.trials = trials;
        bestLayoutAlgorithmNames = new HashSet<>();
        metricValues = new ArrayList<>();
        for (EvaluatedLayout trial : trials) {
            bestLayoutAlgorithmNames.add(trial.getLayoutName());
            metricValues.add(trial.getAllMetricValues());
        }
    }

    @Override
    public String toString() {
        Stats stats = Stats.of(metricValues.stream().flatMapToDouble(doubles -> doubles.stream().mapToDouble(x -> x)));
        return String.format("bestLayoutAlgorithmNames=%s, metricValue=%.3f ± %.3f",
                bestLayoutAlgorithmNames, stats.mean(), stats.populationVariance());
    }
}
