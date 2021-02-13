package org.golchin.ontology_visualization;

import lombok.AllArgsConstructor;
import org.golchin.ontology_visualization.metrics.layout.LayoutMetric;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.stream.GraphReplay;
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
        GraphReplay replay = new GraphReplay("gr");
        replay.addSink(copy);
        replay.replay(graph);
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
            List<Double> metricValues = new ArrayList<>();
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
                metricValues.add(curMetric);
            }
            double average = sumMetrics / nTrials;
            variants.put(layoutName, average);
            EvaluatedLayout evaluatedLayout =
                    new EvaluatedLayout(layoutName, bestLayout, metricValues, bestMetric, average, variants);
            evaluatedLayouts.add(evaluatedLayout);
        }
        return Collections.max(evaluatedLayouts, Comparator.comparing(EvaluatedLayout::getAverageAesthetics, comparator));
    }
}
