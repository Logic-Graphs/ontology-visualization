package org.golchin.ontology_visualization;

import org.graphstream.graph.implementations.MultiGraph;
import org.semanticweb.owlapi.model.OWLOntology;

public class OWLVizConverter implements OntologyToGraphConverter {
    @Override
    public MultiGraph convert(OWLOntology ontology) {
        return new OWLVizModel(ontology).getGraph();
    }

    @Override
    public String toString() {
        return "OWLViz";
    }
}
