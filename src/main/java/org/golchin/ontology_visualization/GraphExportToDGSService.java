package org.golchin.ontology_visualization;

import javafx.concurrent.Task;
import org.graphstream.stream.file.FileSink;
import org.graphstream.stream.file.FileSinkDGS;

public class GraphExportToDGSService extends GraphExportService {
    private final FileSink fileSink;

    protected GraphExportToDGSService() {
        fileSink = new FileSinkDGS();
    }

    @Override
    protected Task<Void> createTask() {
        return new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                fileSink.writeAll(graph, fileName);
                return null;
            }
        };
    }
}
