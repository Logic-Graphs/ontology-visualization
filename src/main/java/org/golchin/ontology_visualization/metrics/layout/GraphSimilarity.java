package org.golchin.ontology_visualization.metrics.layout;

import org.graphstream.graph.Graph;

public interface GraphSimilarity {
    double measure(Graph first, Graph second);
}
