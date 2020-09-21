package org.golchin.ontology_visualization;

import javafx.concurrent.Task;
import org.graphstream.graph.Node;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class GraphExportToDOTService extends GraphExportService {

    @Override
    protected Task<Void> createTask() {
        return new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(fileName))) {
                    writer.write("digraph {");
                    writer.newLine();
                    graph.nodes()
                            .forEach(node -> {
                                try {
                                    writer.write(String.format("\"%s\" [label=\"%s\"];",
                                            node.getId(),
                                            node.getAttribute("label")));
                                    writer.newLine();
                                } catch (IOException e) {
                                    throw new UncheckedIOException(e);
                                }
                            });
                    graph.edges()
                            .forEach(edge -> {
                                Node sourceNode = edge.getSourceNode();
                                Node targetNode = edge.getTargetNode();
                                Object label = edge.getAttribute("label");
                                try {
                                    writer.write(String.format("\"%s\" -> \"%s\" [label=\"%s\"];",
                                            sourceNode,
                                            targetNode,
                                            label));
                                    writer.newLine();
                                } catch (IOException e) {
                                    throw new UncheckedIOException(e);
                                }
                            });
                    writer.write("}");
                    writer.newLine();
                }
                return null;
            }
        };
    }
}
