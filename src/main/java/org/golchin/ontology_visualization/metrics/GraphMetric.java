package org.golchin.ontology_visualization.metrics;

import org.golchin.ontology_visualization.QualityMetric;
import org.graphstream.graph.Graph;

/**
 * A quality metric of a graph.
 * Unlike {@link org.golchin.ontology_visualization.aesthetics.Aesthetics} it takes into account only connections, not layout.
 */
public interface GraphMetric extends QualityMetric {
    double calculate(Graph graph);
}
