package org.golchin.ontology_visualization;

import lombok.AllArgsConstructor;
import org.golchin.ontology_visualization.metrics.GraphMetric;
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.MultiGraph;
import org.semanticweb.owlapi.model.OWLOntology;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
public class GraphChooser {
    private final OWLOntology ontology;
    private final Collection<? extends OntologyToGraphConverter> converters;
    private final GraphMetric metric;

    protected EvaluatedGraph choose() {
        Double bestMetricValue = null;
        Graph bestGraph = null;
        Map<Parameter<?>, Object> bestParameterValues = null;
        Map<Map<Parameter<?>, Object>, Double> metricValuesByParameters = new HashMap<>();
        Comparator<Double> comparator = metric.getComparator();
        for (OntologyToGraphConverter converter : converters) {
            MultiGraph graph = converter.convert(ontology);
            double value = metric.calculate(graph);
            if (bestMetricValue == null || comparator.compare(bestMetricValue, value) < 0) {
                bestMetricValue = value;
                bestGraph = graph;
                bestParameterValues = converter.getParameterValues();
            }
            metricValuesByParameters.put(converter.getParameterValues(), value);
        }
        return new EvaluatedGraph(bestGraph, bestMetricValue, bestParameterValues, metricValuesByParameters);
    }
}
