package org.golchin.ontology_visualization;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import lombok.Setter;
import org.graphstream.graph.Graph;
import org.graphstream.stream.file.FileSinkImages;
import org.graphstream.ui.javafx.util.FxFileSinkImages;

public class GraphExportToImageService extends Service<Void> {
    private final FileSinkImages fileSinkImages = new FxFileSinkImages();
    @Setter
    private Graph graph;
    @Setter
    private String fileName;

    public GraphExportToImageService() {
        fileSinkImages.setLayoutPolicy(FileSinkImages.LayoutPolicy.NO_LAYOUT);
    }


    @Override
    protected Task<Void> createTask() {
        return new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                fileSinkImages.writeAll(graph, fileName);
                return null;
            }
        };
    }
}
