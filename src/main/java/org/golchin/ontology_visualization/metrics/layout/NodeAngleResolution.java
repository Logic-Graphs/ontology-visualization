package org.golchin.ontology_visualization.metrics.layout;

import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;

import java.awt.geom.Point2D;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.Math.PI;

public class NodeAngleResolution implements LayoutMetric {
    @Override
    public double calculate(Graph graph, Function<Node, Point2D> vertexToPoint) {
        double minAngle = Double.POSITIVE_INFINITY;
        for (Node node : graph) {
            Point2D nodePoint = vertexToPoint.apply(node);
            List<Double> angles = node.neighborNodes()
                    .filter(neighbor -> neighbor != node)
                    .distinct()
                    .map(neighbor -> {
                        Point2D neighborPoint = vertexToPoint.apply(neighbor);
                        return Math.atan2(neighborPoint.getY() - nodePoint.getY(), neighborPoint.getX() - nodePoint.getX());
                    })
                    .map(NodeAngleResolution::toCanonicalAngle)
                    .sorted()
                    .collect(Collectors.toList());
            for (int i = 1; i < angles.size(); i++) {
                Double curAngle = angles.get(i);
                Double prevAngle = angles.get(i - 1);
                double difference = curAngle - prevAngle;
                if (minAngle > difference)
                    minAngle = difference;
            }
            if (angles.size() > 1) {
                double angleDifference = 2 * PI + angles.get(0) - angles.get(angles.size() - 1);
                if (minAngle > angleDifference)
                    minAngle = angleDifference;
            }
        }
        return minAngle * 180 / PI;
    }

    @Override
    public Comparator<Double> getComparator() {
        return Comparator.naturalOrder();
    }

    static double toCanonicalAngle(double angle) {
        while (angle < 0) {
            angle += 2 * PI;
        }
        while (angle >= 2 * PI) {
            angle -= 2 * PI;
        }
        return angle;
    }

}
