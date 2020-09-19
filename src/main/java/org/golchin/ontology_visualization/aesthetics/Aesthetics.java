package org.golchin.ontology_visualization.aesthetics;

import org.golchin.ontology_visualization.QualityMetric;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;

import java.awt.geom.Point2D;
import java.util.function.Function;

public interface Aesthetics extends QualityMetric {
    double calculate(Graph graph, Function<Node, Point2D> vertexToPoint);
}
