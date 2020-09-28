package org.golchin.ontology_visualization;

import javafx.concurrent.Task;
import org.graphstream.stream.file.FileSinkImages;
import org.graphstream.ui.javafx.util.FxFileSinkImages;

public class GraphExportToImageService extends GraphExportService {
    private final FileSinkImages fileSink;

    public GraphExportToImageService() {
        fileSink = new FxFileSinkImages();
        fileSink.setLayoutPolicy(FileSinkImages.LayoutPolicy.NO_LAYOUT);
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
