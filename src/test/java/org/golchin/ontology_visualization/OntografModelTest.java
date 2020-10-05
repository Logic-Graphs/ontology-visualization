package org.golchin.ontology_visualization;

import com.google.common.collect.ImmutableSet;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import java.net.URL;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class OntografModelTest {
    private OWLOntology loadOntology(String resourceName) throws OWLOntologyCreationException {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        URL url = OntologyToGraphConverterImplTest.class.getResource("/" + resourceName);
        return manager.loadOntology(IRI.create(url));
    }

    @Test
    void getGraph() throws Exception {
        OWLOntology ontology = loadOntology("foaf.rdf");
        MultiGraph graph = new OntografModel(ontology).getGraph();
        Node account = graph.getNode("http://xmlns.com/foaf/0.1/OnlineAccount");
        assertNotNull(account);
        Set<Object> labels = account.leavingEdges()
                .filter(edge -> edge.getAttribute("label").equals("subclass"))
                .map(edge -> edge.getTargetNode().getAttribute("label"))
                .collect(Collectors.toSet());
        assertTrue(labels.containsAll(ImmutableSet.of("OnlineChatAccount", "OnlineEcommerceAccount", "OnlineGamingAccount")));
        Node agent = graph.getNode("http://xmlns.com/foaf/0.1/Agent");
        assertNotNull(agent);
        Edge edge = agent.leavingEdges()
                .filter(e -> ("mbox_sha1sum" + OntografModel.SUFFIX_DOMAIN_RANGE).equals(e.getAttribute("label")))
                .findFirst()
                .orElse(null);
        assertNotNull(edge);
        Node thing = graph.getNode("http://www.w3.org/2002/07/owl#Thing");
        assertEquals(thing, edge.getTargetNode());
    }
}