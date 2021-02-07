package org.golchin.ontology_visualization;

import javafx.concurrent.Service;
import lombok.Setter;
import org.graphstream.graph.Graph;


public abstract class GraphImportService extends Service<Graph> {
    @Setter
    protected String fileName;
}
