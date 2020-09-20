package org.golchin.ontology_visualization.metrics;

import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;

import java.util.Comparator;

import static java.util.stream.Collectors.groupingBy;

public abstract class EntropyMetric<T> implements GraphMetric {
    protected abstract T classifyNode(Node node);

    @Override
    public double calculate(Graph graph) {
        int nodeCount = graph.getNodeCount();
        return graph.nodes()
                .collect(groupingBy(this::classifyNode))
                .values()
                .stream()
                .mapToDouble(nodes -> {
                    double p = ((double) nodes.size()) / nodeCount;
                    return -p * Math.log(p);
                })
                // entropy
                .sum();
    }

    @Override
    public Comparator<Double> getComparator() {
        return Comparator.<Double>naturalOrder().reversed();
    }
}
