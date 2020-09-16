package org.golchin.ontology_visualization;

import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OntologyToGraphConverterImplTest {
    private final OntologyToGraphConverter converter = new OntologyToGraphConverterImpl();
    private final OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

    @Test
    void convert() throws OWLOntologyCreationException {
        URL url = OntologyToGraphConverterImplTest.class.getResource("/foaf.rdf");
        OWLOntology ontology = manager.loadOntology(IRI.create(url));
        MultiGraph graph = converter.convert(ontology);
        ontology.getSignature();
        Set<OWLClass> classesInSignature = ontology.getClassesInSignature();
        Set<String> expectedIds = classesInSignature.stream().map(OWLClass::toStringID)
                .collect(Collectors.toSet());
        Set<String> actualIds = new HashSet<>();
        for (Node node : graph) {
            Object label = node.getAttribute("label");
            assertTrue(label instanceof String && !((String) label).isEmpty());
            Object ids = node.getAttribute("ids");
            assertTrue(ids instanceof Collection && !((Collection<String>) ids).isEmpty());
            actualIds.addAll((Collection<? extends String>) ids);
        }
        assertTrue(actualIds.containsAll(expectedIds));
    }
}