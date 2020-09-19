package org.golchin.ontology_visualization.metrics.layout;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;

import java.awt.geom.Point2D;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class EdgeLengthStd implements LayoutMetric {
    @Override
    public double calculate(Graph graph, Function<Node, Point2D> vertexToPoint) {
        double sumOfSquares = 0.;
        double sum = 0.;
        List<Edge> edges = graph.edges().collect(Collectors.toList());
        for (Edge edge : edges) {
            Point2D sourcePoint = vertexToPoint.apply(edge.getNode0());
            Point2D targetPoint = vertexToPoint.apply(edge.getNode1());
            double distance = sourcePoint.distance(targetPoint);
            sum += distance;
            sumOfSquares += sourcePoint.distanceSq(targetPoint);
        }
        int edgeCount = edges.size();
        double meanOfSquares = sumOfSquares / edgeCount;
        double mean = sum / edgeCount;
        return Math.sqrt(meanOfSquares - mean * mean);
    }

    @Override
    public Comparator<Double> getComparator() {
        return Comparator.<Double>naturalOrder().reversed();
    }
}
