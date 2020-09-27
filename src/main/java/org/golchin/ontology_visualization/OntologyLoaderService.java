package org.golchin.ontology_visualization;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import lombok.AllArgsConstructor;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import java.net.MalformedURLException;
import java.net.URL;

@AllArgsConstructor
public class OntologyLoaderService extends Service<OWLOntology> {
    private final String url;
    private final OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

    @Override
    protected Task<OWLOntology> createTask() {
        return new Task<OWLOntology>() {
            @Override
            protected OWLOntology call() throws Exception {
                try {
                    new URL(url);
                } catch (MalformedURLException e) {
                    throw new IllegalArgumentException("Invalid URL: '" + url + '\'', e);
                }
                IRI documentIRI = IRI.create(url);
                return manager.loadOntologyFromOntologyDocument(documentIRI);
            }
        };
    }
}
