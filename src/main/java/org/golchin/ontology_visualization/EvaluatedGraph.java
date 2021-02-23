package org.golchin.ontology_visualization;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.graphstream.graph.Graph;

import java.util.Map;

@AllArgsConstructor
@Getter
public class EvaluatedGraph {
    private final Graph graph;
    private final double bestMetricValue;
    private final OntologyToGraphConverter bestConverter;
    private final Map<OntologyToGraphConverter, Double> metricValuesByConverters;
}
