package org.golchin.ontology_visualization;

import org.graphstream.stream.file.FileSink;
import org.graphstream.stream.file.FileSinkImages;
import org.graphstream.ui.javafx.util.FxFileSinkImages;

public class GraphExportToImageService extends FileSinkGraphExportService {

    @Override
    protected FileSink createSink() {
        FileSinkImages fileSink = new FxFileSinkImages();
        fileSink.setLayoutPolicy(FileSinkImages.LayoutPolicy.NO_LAYOUT);
        return fileSink;
    }
}
