package org.golchin.ontology_visualization.metrics.layout;

import com.google.common.collect.Sets;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;

import java.util.Set;
import java.util.stream.Collectors;

public class MeanJaccardIndex implements GraphSimilarity {
    @Override
    public double measure(Graph first, Graph second) {
        return first.nodes()
                .collect(Collectors.averagingDouble(n -> calculateJaccardIndex(n, second)));
    }

    private double calculateJaccardIndex(Node node, Graph second) {
        Node nodeInSecondGraph = second.getNode(node.getId());
        Set<String> neighbors = node.leavingEdges()
                .map(Edge::getTargetNode)
                .map(Node::toString)
                .collect(Collectors.toSet());
        Set<String> neighborsInSecondGraph = nodeInSecondGraph.leavingEdges()
                .map(Edge::getTargetNode)
                .map(Node::toString)
                .collect(Collectors.toSet());
        if (neighbors.isEmpty() || neighborsInSecondGraph.isEmpty())
            return 1;
        return (double) Sets.intersection(neighbors, neighborsInSecondGraph).size() /
                Sets.union(neighbors, neighborsInSecondGraph).size();
    }
}
