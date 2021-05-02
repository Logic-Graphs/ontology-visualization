package org.golchin.ontology_visualization;

import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.stream.GraphReplay;

import java.util.List;
import java.util.stream.Collectors;

public class CircularLayout extends LayoutMethod {
    public CircularLayout() {
        super(true, "Circular");
    }

    public void circle(List<Node> nodes, double r, double left, double top) {
        int nodeCount = nodes.size();
        double phi = 2 * Math.PI / nodeCount;

        for (int i = 0; i < nodeCount; i++) {
            setNodeLocation(nodes.get(i),
                    left + r + r * Math.sin(i * phi),
                    top + r + r * Math.cos(i * phi));
        }
    }

    public Graph layoutGraph(Graph graph) {
        MultiGraph copy = new MultiGraph("mg");
        GraphReplay replay = new GraphReplay("gr");
        replay.addSink(copy);
        replay.replay(graph);
        circle(copy.nodes().collect(Collectors.toList()), 1.0, 0.0, 0.0);
        return copy;
    }
}
