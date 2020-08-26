package org.golchin.ontology_visualization;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import lombok.AllArgsConstructor;
import org.graphstream.graph.Graph;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import java.net.MalformedURLException;
import java.net.URL;

@AllArgsConstructor
public class OntologyLoaderService extends Service<Graph> {
    private final String url;
    private final OntologyToGraphConverter converter;
    private final OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

    @Override
    protected Task<Graph> createTask() {
        return new Task<Graph>() {
            @Override
            protected Graph call() throws Exception {
                try {
                    new URL(url);
                } catch (MalformedURLException e) {
                    throw new IllegalArgumentException("Invalid URL: '" + url + '\'', e);
                }
                IRI documentIRI = IRI.create(url);
                OWLOntology ontology = manager.loadOntologyFromOntologyDocument(documentIRI);
                return converter.convert(ontology);
            }
        };
    }
}
