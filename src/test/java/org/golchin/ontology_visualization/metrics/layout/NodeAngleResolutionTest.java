package org.golchin.ontology_visualization.metrics.layout;

import org.golchin.ontology_visualization.LayoutChooser;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
import org.junit.jupiter.api.Test;

import static java.lang.Math.PI;
import static org.junit.jupiter.api.Assertions.assertEquals;

class NodeAngleResolutionTest {
    @Test
    void obtuseAngle() {
        MultiGraph g = new MultiGraph("g");
        Node node0 = g.addNode("0");
        Node node1 = g.addNode("1");
        Node node2 = g.addNode("2");
        Node node3 = g.addNode("3");
        g.addEdge("1", "0", "1");
        g.addEdge("2", "0", "2");
        g.addEdge("3", "0", "3");
        node0.setAttribute("xyz", 0.0, 0.0);
        node1.setAttribute("xyz", 0.0, 1.0);
        double angle = PI / 6;
        node2.setAttribute("xyz", -Math.cos(angle), -Math.sin(angle));
        node3.setAttribute("xyz", Math.cos(angle), -Math.sin(angle));
        double result = new NodeAngleResolution().calculate(g, LayoutChooser.NODE_POSITION_GETTER);
        assertEquals(120.0, result, 1e-6);
    }
}