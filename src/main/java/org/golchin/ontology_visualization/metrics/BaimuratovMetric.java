package org.golchin.ontology_visualization.metrics;

import org.graphstream.graph.Graph;

import java.util.Comparator;

public class BaimuratovMetric implements GraphMetric {
    @Override
    public double calculate(Graph graph) {
        return Math.log(graph.getEdgeCount()) / graph.getNodeCount();
    }

    @Override
    public Comparator<Double> getComparator() {
        return Comparator.<Double>naturalOrder().reversed();
    }
}
