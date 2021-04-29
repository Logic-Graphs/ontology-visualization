package org.golchin.ontology_visualization;

import com.google.common.math.StatsAccumulator;
import org.apache.log4j.Logger;
import org.golchin.ontology_visualization.metrics.layout.LayoutMetric;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;

import java.awt.geom.Point2D;
import java.util.*;
import java.util.function.Function;

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
    private final Collection<LayoutMethod> possibleLayouts;
    private final int nTrials;
    private final int minTrialsCount;
    private final LayoutMetric layoutMetric;
    private final double changeThreshold;
    private static final Logger LOGGER = Logger.getLogger(LayoutChooser.class);

    public LayoutChooser(Graph graph,
                     Collection<LayoutMethod> possibleLayouts,
                     int nTrials,
                     LayoutMetric layoutMetric) {
        this(graph, possibleLayouts, nTrials, 5, layoutMetric, 0.01);
    }

    public LayoutChooser(Graph graph,
                         Collection<LayoutMethod> possibleLayouts,
                         int nTrials,
                         int minTrialsCount,
                         LayoutMetric layoutMetric,
                         double changeThreshold) {
        this.graph = graph;
        this.possibleLayouts = possibleLayouts;
        this.nTrials = nTrials;
        this.minTrialsCount = minTrialsCount;
        this.layoutMetric = layoutMetric;
        this.changeThreshold = changeThreshold;
    }

    public EvaluatedLayout chooseLayout() {
        Comparator<Double> comparator = layoutMetric.getComparator();
        Map<String, LayoutVariant> variants = new LinkedHashMap<>();
        for (LayoutMethod layoutAdapter : possibleLayouts) {
            Graph bestLayout = null;
            Double bestMetric = null;
            List<Double> metricValues = new ArrayList<>();
            int nTrials = layoutAdapter.isDeterministic() ? 1 : this.nTrials;
            boolean hasConverged = false;
            Double prevMean = null;
            StatsAccumulator meanAccumulator = new StatsAccumulator();
            double meanChangeRatio = Double.POSITIVE_INFINITY;
            String layoutName = layoutAdapter.getLayoutAlgorithmName();
            int i = 0;
            for (; i < nTrials && !hasConverged; i++) {
                Graph layoutGraph = layoutAdapter.layoutGraph(graph);
                double curMetric = layoutMetric.calculate(layoutGraph, NODE_POSITION_GETTER);
                if (bestMetric == null || comparator.compare(bestMetric, curMetric) < 0) {
                    bestLayout = layoutGraph;
                    bestMetric = curMetric;
                }
                metricValues.add(curMetric);
                meanAccumulator.add(curMetric);
                double mean = meanAccumulator.mean();
                if (i + 1 >= minTrialsCount && prevMean != null) {
                    meanChangeRatio = Math.abs(mean - prevMean) / prevMean;
                    if (mean < 1E-9 && prevMean < 1E-9 || meanChangeRatio < changeThreshold) {
                        hasConverged = true;
                    }
                }
                prevMean = mean;
            }
            String metricName = layoutMetric.getClass().getSimpleName();
            if (hasConverged) {
                LOGGER.debug("Converged for layout " + layoutName + ", " + metricName + " after " + i + " iterations");
            } else {
                LOGGER.warn(String.format("Not converged for layout %s, metric %s, current ratio: %.4f", layoutName, metricName, meanChangeRatio));
            }
            variants.put(layoutName, new LayoutVariant(layoutName, bestLayout, bestMetric, metricValues));
        }
        Comparator<LayoutVariant> variantComparator = Comparator.comparing(LayoutVariant::getAverageMetricValue, comparator);
        LayoutVariant bestVariant = Collections.max(variants.values(), variantComparator);
        return new EvaluatedLayout(variants, bestVariant);
    }
}
