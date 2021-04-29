package org.golchin.ontology_visualization.metrics;

import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;

import java.awt.geom.Point2D;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MetricUtils {
    public static double getMeanLength(Graph graph, Function<Node, Point2D> vertexToPoint) {
        return graph.edges().map(edge -> {
            Point2D sourcePoint = vertexToPoint.apply(edge.getSourceNode());
            Point2D destPoint = vertexToPoint.apply(edge.getTargetNode());
            return sourcePoint.distance(destPoint);
        })
                .collect(Collectors.averagingDouble(x -> x));
    }
}
