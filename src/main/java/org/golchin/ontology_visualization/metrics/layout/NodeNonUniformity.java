package org.golchin.ontology_visualization.metrics.layout;

import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;

import java.awt.geom.Point2D;
import java.util.Comparator;
import java.util.function.Function;

import static org.golchin.ontology_visualization.metrics.MetricUtils.getMeanLength;

public class NodeNonUniformity implements LayoutMetric {
    @Override
    public double calculate(Graph graph, Function<Node, Point2D> vertexToPoint) {
        double meanLength = getMeanLength(graph, vertexToPoint);
        double total = 0.;
        for (Node node : graph) {
            for (Node otherNode : graph) {
                if (node != otherNode) {
                    Point2D point = vertexToPoint.apply(node);
                    Point2D otherPoint = vertexToPoint.apply(otherNode);
                    double distance = point.distance(otherPoint);
                    distance /= meanLength;
                    total += 1 / distance / distance;
                }
            }
        }
        return total;
    }

    @Override
    public Comparator<Double> getComparator() {
        return Comparator.<Double>naturalOrder().reversed();
    }

}
