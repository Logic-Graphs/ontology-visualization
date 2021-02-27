package org.golchin.ontology_visualization;

import lombok.AllArgsConstructor;
import org.golchin.ontology_visualization.metrics.layout.LayoutMetric;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;

import java.awt.geom.Point2D;
import java.util.*;
import java.util.function.Function;

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
    private final Collection<LayoutAdapter<?>> possibleLayouts;
    private final int nTrials;
    private final LayoutMetric layoutMetric;

    public EvaluatedLayout chooseLayout() {
        Comparator<Double> comparator = layoutMetric.getComparator();
        Map<String, LayoutVariant> variants = new LinkedHashMap<>();
        for (LayoutAdapter<?> layoutAdapter : possibleLayouts) {
            Graph bestLayout = null;
            Double bestMetric = null;
            String layoutName = null;
            List<Double> metricValues = new ArrayList<>();
            int nTrials = layoutAdapter.isDeterministic() ? 1 : this.nTrials;
            for (int i = 0; i < nTrials; i++) {
                layoutName = layoutAdapter.getLayoutAlgorithmName();
                Graph layoutGraph = layoutAdapter.layoutGraph(graph);
                double curMetric = layoutMetric.calculate(layoutGraph, NODE_POSITION_GETTER);
                if (bestMetric == null || comparator.compare(bestMetric, curMetric) < 0) {
                    bestLayout = layoutGraph;
                    bestMetric = curMetric;
                }
                metricValues.add(curMetric);
            }
            variants.put(layoutName, new LayoutVariant(layoutName, bestLayout, bestMetric, metricValues));
        }
        Comparator<LayoutVariant> variantComparator = Comparator.comparing(LayoutVariant::getAverageMetricValue, comparator);
        LayoutVariant bestVariant = Collections.max(variants.values(), variantComparator);
        return new EvaluatedLayout(variants, bestVariant);
    }
}
