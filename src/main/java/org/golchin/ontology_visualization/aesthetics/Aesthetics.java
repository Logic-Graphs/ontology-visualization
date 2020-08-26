package org.golchin.ontology_visualization.aesthetics;

import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;

import java.awt.geom.Point2D;
import java.util.Comparator;
import java.util.function.Function;

public interface Aesthetics {
    double calculate(Graph graph, Function<Node, Point2D> vertexToPoint);

    Comparator<Double> getComparator();
}
