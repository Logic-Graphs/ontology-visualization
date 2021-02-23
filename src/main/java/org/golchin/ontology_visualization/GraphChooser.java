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
    private final GraphSimplifier simplifier;
    private final GraphMetric metric;

    public EvaluatedGraph choose() {
        Double bestMetricValue = null;
        Graph bestGraph = null;
        Map<OntologyToGraphConverter, Double> metricValuesByConverters = new HashMap<>();
        Comparator<Double> comparator = metric.getComparator();
        OntologyToGraphConverter bestConverter = null;
        for (OntologyToGraphConverter converter : converters) {
            MultiGraph graph = converter.convert(ontology);
            simplifier.simplify(graph);
            double value = metric.calculate(graph);
            if (bestMetricValue == null || comparator.compare(bestMetricValue, value) < 0) {
                bestMetricValue = value;
                bestGraph = graph;
                bestConverter = converter;
            }
            metricValuesByConverters.put(converter, value);
        }
        return new EvaluatedGraph(bestGraph,
                bestMetricValue,
                bestConverter,
                metricValuesByConverters);
    }
}
