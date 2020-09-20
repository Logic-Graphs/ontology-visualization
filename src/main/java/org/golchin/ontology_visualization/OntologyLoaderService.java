package org.golchin.ontology_visualization;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import lombok.AllArgsConstructor;
import org.golchin.ontology_visualization.metrics.GraphMetric;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;

@AllArgsConstructor
public class OntologyLoaderService extends Service<EvaluatedGraph> {
    private final String url;
    private final GraphMetric metric;
    private final Collection<? extends OntologyToGraphConverter> converters;
    private final OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

    @Override
    protected Task<EvaluatedGraph> createTask() {
        return new Task<EvaluatedGraph>() {
            @Override
            protected EvaluatedGraph call() throws Exception {
                try {
                    new URL(url);
                } catch (MalformedURLException e) {
                    throw new IllegalArgumentException("Invalid URL: '" + url + '\'', e);
                }
                IRI documentIRI = IRI.create(url);
                OWLOntology ontology = manager.loadOntologyFromOntologyDocument(documentIRI);
                return new GraphChooser(ontology, converters, metric).choose();
            }
        };
    }
}
