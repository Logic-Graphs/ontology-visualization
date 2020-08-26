package org.golchin.ontology_visualization.aesthetics;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class CrossingsAesthetics implements Aesthetics {
    @Override
    public double calculate(Graph graph, Function<Node, Point2D> vertexToPoint) {
        List<Edge> edgeList = graph.edges().collect(Collectors.toList());
        Map<Edge, Line2D> edgeToLine = graph.edges()
                .collect(Collectors.toMap(Function.identity(), edge -> {
                    Point2D sourcePoint = vertexToPoint.apply(edge.getSourceNode());
                    Point2D targetPoint = vertexToPoint.apply(edge.getTargetNode());
                    return new Line2D.Double(sourcePoint, targetPoint);
                }));
        double crossings = getInitialValue();
        for (int i = 0; i < edgeList.size(); i++) {
            Line2D firstLine = edgeToLine.get(edgeList.get(i));
            for (int j = 0; j < i; j++) {
                if (!haveCommonVertices(edgeList.get(i), edgeList.get(j))) {
                    Line2D secondLine = edgeToLine.get(edgeList.get(j));
                    if (firstLine.intersectsLine(secondLine)) {
                        crossings = reduce(crossings, firstLine, secondLine);
                    }
                }
            }
        }
        return crossings;
    }

    protected abstract double getInitialValue();

    protected abstract double reduce(double curValue, Line2D firstLine, Line2D secondLine);

    @Override
    public Comparator<Double> getComparator() {
        return Comparator.<Double>naturalOrder().reversed();
    }

    boolean haveCommonVertices(Edge e1, Edge e2) {
        return e1.getSourceNode() == e2.getSourceNode() || e1.getSourceNode() == e2.getTargetNode() ||
                e1.getTargetNode() == e2.getSourceNode() || e1.getTargetNode() == e2.getTargetNode();
    }
}
