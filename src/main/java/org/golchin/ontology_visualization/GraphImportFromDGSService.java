package org.golchin.ontology_visualization;

import javafx.concurrent.Task;
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.stream.file.FileSource;
import org.graphstream.stream.file.FileSourceDGS;

public class GraphImportFromDGSService extends GraphImportService {
    private final FileSource source = new FileSourceDGS();

    @Override
    protected Task<Graph> createTask() {
        return new Task<Graph>() {
            @Override
            protected Graph call() throws Exception {
                MultiGraph graph = new MultiGraph("mg");
                source.addSink(graph);
                try {
                    source.readAll(fileName);
                } finally {
                    source.removeSink(graph);
                }
                return graph;
            }
        };
    }
}
