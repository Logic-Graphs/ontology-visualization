package org.golchin.ontology_visualization;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.graphstream.graph.Graph;

import java.util.List;
import java.util.Map;

@AllArgsConstructor
@Getter
public class EvaluatedLayout {
    private final String layoutName;
    private final Graph bestLayout;
    private final List<Double> allMetricValues;
    private final double bestAesthetics;
    private final double averageAesthetics;
    private final Map<String, Double> variants;
}
