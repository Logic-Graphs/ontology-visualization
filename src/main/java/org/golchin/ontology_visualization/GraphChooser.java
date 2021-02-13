package org.golchin.ontology_visualization;

import javafx.util.Pair;
import lombok.AllArgsConstructor;
import org.golchin.ontology_visualization.metrics.GraphMetric;
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.MultiGraph;
import org.semanticweb.owlapi.model.OWLOntology;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import static org.golchin.ontology_visualization.Util.measureTimeMillis;

@AllArgsConstructor
public class GraphChooser {
    private final OWLOntology ontology;
    private final Collection<? extends OntologyToGraphConverter> converters;
    private final GraphSimplifier simplifier;
    private final GraphMetric metric;

    public EvaluatedGraph choose() {
        Double bestMetricValue = null;
        Graph bestGraph = null;
        Map<OntologyToGraphConverter, Double> metricValuesByConverters = new HashMap<>();
        Comparator<Double> comparator = metric.getComparator();
        OntologyToGraphConverter bestConverter = null;
        double sumGraphConversionTime = 0.0;
        double sumMetricComputationTime = 0.0;
        for (OntologyToGraphConverter converter : converters) {
            Pair<MultiGraph, Long> graphWithTime = measureTimeMillis(() -> {
                MultiGraph g = converter.convert(ontology);
                simplifier.simplify(g);
                return g;
            });
            Graph graph = graphWithTime.getKey();
            sumGraphConversionTime += graphWithTime.getValue();
            Pair<Double, Long> metricWithTime = measureTimeMillis(() -> metric.calculate(graph));
            double value = metricWithTime.getKey();
            sumMetricComputationTime += metricWithTime.getValue();
            if (bestMetricValue == null || comparator.compare(bestMetricValue, value) < 0) {
                bestMetricValue = value;
                bestGraph = graph;
                bestConverter = converter;
            }
            metricValuesByConverters.put(converter, value);
        }
        int nConverters = converters.size();
        return new EvaluatedGraph(bestGraph,
                bestMetricValue,
                bestConverter,
                metricValuesByConverters,
                sumGraphConversionTime / nConverters,
                sumMetricComputationTime / nConverters);
    }
}
