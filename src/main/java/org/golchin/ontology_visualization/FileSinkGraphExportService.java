package org.golchin.ontology_visualization;

import javafx.concurrent.Task;
import org.graphstream.stream.file.FileSink;

public abstract class FileSinkGraphExportService extends GraphExportService {
    private final FileSink fileSink;

    protected FileSinkGraphExportService() {
        fileSink = createSink();
    }

    protected abstract FileSink createSink();

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
