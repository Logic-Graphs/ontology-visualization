package org.golchin.ontology_visualization;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;

@AllArgsConstructor
public abstract class LayoutMethod {
    @Getter
    private final boolean isDeterministic;
    @Getter
    private final String layoutAlgorithmName;

    protected void setNodeLocation(Node node, double x, double y) {
        node.setAttribute("xyz", x, y);
    }

    public abstract Graph layoutGraph(Graph graph);
}
