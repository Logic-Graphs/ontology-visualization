package org.golchin.ontology_visualization;

import javafx.concurrent.Service;
import lombok.Setter;
import org.graphstream.graph.Graph;

public abstract class GraphExportService extends Service<Void> {
    @Setter
    protected Graph graph;
    @Setter
    protected String fileName;
}
