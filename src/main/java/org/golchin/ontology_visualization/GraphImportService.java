package org.golchin.ontology_visualization;

import guru.nidi.graphviz.attribute.Named;
import guru.nidi.graphviz.model.Link;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.model.MutableNode;
import guru.nidi.graphviz.parse.Parser;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import lombok.Setter;
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.MultiGraph;

import java.io.File;
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
                MutableGraph mutableGraph = new Parser().read(new File(fileName));
                MultiGraph graph = new MultiGraph("g");
                for (MutableNode node : mutableGraph.nodes()) {
                    Object label = node.get("label");
                    graph.addNode(node.name().toString()).setAttribute("label", label);
                }
                for (Link link : mutableGraph.edges()) {
                    Object label = link.get("label");
                    Named from = link.from();
                    Named to = link.to();
                    graph.addEdge("edge_" + counter.getAndIncrement(), from.name().toString(), to.name().toString())
                            .setAttribute("label", label);
                }
                return graph;
            }
        };
    }

}
