package org.golchin.ontology_visualization.metrics;

import org.graphstream.graph.Node;

public class DegreeEntropyMetric extends EntropyMetric<Integer> {
    @Override
    protected Integer classifyNode(Node node) {
        return node.getDegree();
    }
}
