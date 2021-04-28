package org.golchin.ontology_visualization;

import com.google.common.math.Stats;
import lombok.Getter;
import org.graphstream.graph.Graph;

import java.util.List;

@Getter
public class LayoutVariant {
    private final String layoutName;
    private final Graph layout;
    private final List<Double> allMetricValues;
    private final double bestMetricValue;
    private final double averageMetricValue;

    public LayoutVariant(String layoutName, Graph layout, double bestMetricValue, List<Double> allMetricValues) {
        this.layoutName = layoutName;
        this.layout = layout;
        this.allMetricValues = allMetricValues;
        this.bestMetricValue = bestMetricValue;
        Stats stats = Stats.of(allMetricValues);
        averageMetricValue = stats.mean();
    }
}
