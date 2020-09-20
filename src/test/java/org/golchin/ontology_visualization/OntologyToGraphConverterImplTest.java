package org.golchin.ontology_visualization;

import com.google.common.collect.Sets;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class OntologyToGraphConverterImplTest {
    private OntologyToGraphConverter converter;
    private final OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
    private final URL url = OntologyToGraphConverterImplTest.class.getResource("/foaf.rdf");
    private OWLOntology ontology;
    private Set<String> expectedIds;

    @BeforeEach
    void setUp() throws Exception {
        ontology = manager.loadOntology(IRI.create(url));
        Set<OWLClass> classesInSignature = ontology.getClassesInSignature();
        expectedIds = classesInSignature.stream().map(OWLClass::toStringID)
                .collect(Collectors.toSet());
    }

    @Test
    void convert() {
        converter = new OntologyToGraphConverterImpl();
        Set<String> actualIds = getActualIds();
        long literalCount = actualIds.stream()
                .filter(id -> id.contains("Literal"))
                .count();
        assertTrue(literalCount > 1);
    }

    @Test
    void singleLiteral() {
        converter = new OntologyToGraphConverterImpl(0, true, false);
        Set<String> actualIds = getActualIds();
        assertEquals(Collections.singleton("http://www.w3.org/2000/01/rdf-schema#Literal"),
                Sets.difference(actualIds, expectedIds));
    }

    @Test
    void equivalenceEdges() {
        converter = new OntologyToGraphConverterImpl(0, false, true);
        MultiGraph graph = converter.convert(ontology);
        long equivalentEdgesCount = graph.edges()
                .filter(edge -> Objects.equals(edge.getAttribute("label"), "equivalent"))
                .peek(edge -> assertFalse(edge.isDirected()))
                .count();
        assertTrue(equivalentEdgesCount > 0);
    }

    private Set<String> getActualIds() {
        MultiGraph graph = converter.convert(ontology);
        Set<String> actualIds = new HashSet<>();
        for (Node node : graph) {
            Object label = node.getAttribute("label");
            assertTrue(label instanceof String && !((String) label).isEmpty());
            Object ids = node.getAttribute("ids");
            assertTrue(ids instanceof Collection && !((Collection<String>) ids).isEmpty());
            actualIds.addAll((Collection<? extends String>) ids);
        }
        assertTrue(actualIds.containsAll(expectedIds));
        return actualIds;
    }
}