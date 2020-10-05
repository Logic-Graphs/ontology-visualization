package org.golchin.ontology_visualization;

import com.google.common.collect.ImmutableMap;
import org.graphstream.graph.implementations.MultiGraph;
import org.semanticweb.owlapi.model.OWLOntology;

import java.util.Map;

public class OWLVizConverter implements OntologyToGraphConverter {
    @Override
    public MultiGraph convert(OWLOntology ontology) {
        return new OWLVizModel(ontology).getGraph();
    }

    @Override
    public Map<Parameter<?>, Object> getParameterValues() {
        return ImmutableMap.of(OntologyToGraphConverterImpl.MERGE_EQUIVALENT, false,
                OntologyToGraphConverterImpl.MULTIPLY_DATATYPES, false);
    }

    @Override
    public String toString() {
        return "OWLViz";
    }
}
