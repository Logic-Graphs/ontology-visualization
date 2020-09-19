package org.golchin.ontology_visualization.metrics;

import org.golchin.ontology_visualization.QualityMetric;
import org.golchin.ontology_visualization.metrics.layout.LayoutMetric;
import org.graphstream.graph.Graph;

/**
 * A quality metric of a graph.
 * Unlike {@link LayoutMetric} it takes into account only connections, not layout.
 */
public interface GraphMetric extends QualityMetric {
    double calculate(Graph graph);
}
