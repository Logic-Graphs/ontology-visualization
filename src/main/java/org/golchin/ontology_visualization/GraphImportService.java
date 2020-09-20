package org.golchin.ontology_visualization;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import lombok.Setter;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.stream.file.FileSource;
import org.graphstream.stream.file.FileSourceDOT;

import java.util.concurrent.atomic.AtomicLong;

public class GraphImportService extends Service<Graph> {
    @Setter
    private String fileName;
    private final AtomicLong counter = new AtomicLong();

    @Override
    protected Task<Graph> createTask() {
        return new Task<Graph>() {
            @Override
            protected Graph call() throws Exception {
                MultiGraph graph = new MultiGraph("g");
                FileSource fileSource = new FileSourceDOT();
                fileSource.addSink(graph);
                try {
                    fileSource.readAll(fileName);
                } finally {
                    fileSource.removeSink(graph);
                }
                graph.nodes().forEach(node -> node.setAttribute("label", node.getId()));
                graph.edges().forEach(edge -> copyEdge(graph, edge));
                return graph;
            }
        };
    }

    private void copyEdge(Graph graph, Edge edge) {
        Edge newEdge = graph.addEdge("edge_" + counter.getAndIncrement(), edge.getSourceNode(), edge.getTargetNode(), true);
        newEdge.setAttribute("label", edge.getAttribute("label"));
        graph.removeEdge(edge);
    }
}
