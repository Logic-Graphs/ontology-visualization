package org.golchin.ontology_visualization.aesthetics;

import org.graphstream.graph.Graph;

public interface GraphSimilarity {
    double measure(Graph first, Graph second);
}
