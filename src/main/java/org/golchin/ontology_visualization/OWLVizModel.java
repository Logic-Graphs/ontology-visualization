package org.golchin.ontology_visualization;

import org.graphstream.graph.implementations.MultiGraph;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;

public class OWLVizModel extends OntologyGraphModelBase {
    public OWLVizModel(OWLOntology ontology) {
        super(ontology);
    }

    private void loadChildren(OWLEntity entityOfInterest) {
        if (!(entityOfInterest instanceof OWLClass)) {
            return;
        }

        OWLClass clsOfInterest = (OWLClass) entityOfInterest;

        for (OWLClass childCls : this.provider.getChildren(clsOfInterest)) {
            createEdge(childCls, clsOfInterest, "is-a");
        }
    }

    @Override
    public MultiGraph getGraph() {
        for (OWLEntity owlEntity : ontology.getSignature()) {
            loadChildren(owlEntity);
        }
        return graph;
    }
}
