package org.golchin.ontology_visualization;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import lombok.Setter;
import org.apache.log4j.Logger;
import org.graphstream.graph.Graph;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;

public class SavedGraphImportService extends Service<Graph> {
    private static final Logger LOGGER = Logger.getLogger(SavedGraphImportService.class);
    private final GraphImportFromDOTService service = new GraphImportFromDOTService();
    private final GraphAnnotationsMapper graphAnnotationsMapper = new GraphAnnotationsMapper();
    @Setter
    private String ontologyIri;
    @Setter
    private ConversionSettings conversionSettings;

    @Override
    protected Task<Graph> createTask() {
        return new Task<Graph>() {
            @Override
            protected Graph call() {
                Path path = GraphSaver.getPath(ontologyIri, conversionSettings);
                if (path == null || !Files.exists(path)) {
                    return null;
                }
                service.setFileName(path.toString());
                try {
                    Task<Graph> task = service.createTask();
                    task.run();
                    Graph graph = task.get();
                    try {
                        graphAnnotationsMapper.restoreAnnotations(path.getParent().toString(), graph);
                    } catch (IOException e) {
                        LOGGER.error("Failed to import graph annotations", e);
                    }
                    return graph;
                } catch (InterruptedException | ExecutionException e) {
                    LOGGER.error("Failed to import graph", e);
                }
                return null;
            }
        };
    }
}
