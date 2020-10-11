package org.golchin.ontology_visualization;

import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;

import java.util.List;
import java.util.stream.Collectors;

public class NodeRemovingGraphSimplifier implements GraphSimplifier {
    private final int degree;

    public NodeRemovingGraphSimplifier(int degree) {
        this.degree = degree;
    }

    @Override
    public void simplify(Graph graph) {
        List<Node> nodesToRemove = graph.nodes()
                .filter(n -> n.getDegree() < degree)
                .collect(Collectors.toList());
        for (Node node : nodesToRemove) {
            graph.removeNode(node);
        }
    }
}
