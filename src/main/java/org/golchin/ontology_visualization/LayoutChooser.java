package org.golchin.ontology_visualization;

import lombok.AllArgsConstructor;
import org.golchin.ontology_visualization.metrics.layout.LayoutMetric;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.ui.layout.Layout;

import java.awt.geom.Point2D;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

@AllArgsConstructor
public class LayoutChooser {
    public static final Function<Node, Point2D> NODE_POSITION_GETTER = n -> {
        Object xy = n.getAttribute("xyz");
        if (xy instanceof Object[]) {
            Object[] objects = (Object[]) xy;
            return new Point2D.Double(((Double) objects[0]), ((Double) objects[1]));
        }
        double[] doubles = (double[]) xy;
        double x = doubles[0];
        double y = doubles[1];
        return new Point2D.Double(x, y);
    };
    private final Graph graph;
    private final List<Supplier<Layout>> possibleLayouts;
    private final int nTrials;
    private final LayoutMetric layoutMetric;

    public static Graph layoutGraph(Graph graph, Layout layout) {
        MultiGraph copy = new MultiGraph(UUID.randomUUID().toString());
        layout.setStabilizationLimit(0.9);
        copy.addSink(layout);
        layout.addAttributeSink(copy);
        graph.nodes().forEach(node -> {
            Node n = copy.addNode(node.getId());
            Object label = node.getAttribute("label");
            n.setAttribute("label", label);
        });
        graph.edges().forEach(edge -> {
            Node source = copy.getNode(edge.getNode0().getId());
            Node target = copy.getNode(edge.getNode1().getId());
            Edge e = copy.addEdge(edge.getId(), source, target, true);
            Object label = edge.getAttribute("label");
            if (label instanceof String)
                label = ((String) label).toLowerCase();
            e.setAttribute("label", label);
        });
        do {
            layout.compute();
        } while (layout.getStabilization() < layout.getStabilizationLimit());
        return copy;
    }

    public EvaluatedLayout chooseLayout() {
        List<EvaluatedLayout> evaluatedLayouts = new ArrayList<>();
        Comparator<Double> comparator = layoutMetric.getComparator();
        Map<String, Double> variants = new LinkedHashMap<>();
        for (Supplier<Layout> possibleLayoutSupplier : possibleLayouts) {
            Graph bestLayout = null;
            Double bestMetric = null;
            double sumMetrics = 0;
            String layoutName = null;
            for (int i = 0; i < nTrials; i++) {
                Layout layout = possibleLayoutSupplier.get();
                layoutName = layout.getLayoutAlgorithmName();
                Graph layoutGraph = layoutGraph(graph, layout);
                double curMetric = layoutMetric.calculate(layoutGraph, NODE_POSITION_GETTER);
                sumMetrics += curMetric;
                if (bestMetric == null || comparator.compare(bestMetric, curMetric) < 0) {
                    bestLayout = layoutGraph;
                    bestMetric = curMetric;
                }
            }
            double average = sumMetrics / nTrials;
            variants.put(layoutName, average);
            EvaluatedLayout evaluatedLayout =
                    new EvaluatedLayout(layoutName, bestLayout, bestMetric, average, variants);
            evaluatedLayouts.add(evaluatedLayout);
        }
        return Collections.max(evaluatedLayouts, Comparator.comparing(EvaluatedLayout::getAverageAesthetics, comparator));
    }
}
