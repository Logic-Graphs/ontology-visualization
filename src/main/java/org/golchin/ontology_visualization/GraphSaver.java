package org.golchin.ontology_visualization;

import org.apache.log4j.Logger;
import org.graphstream.graph.Graph;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class GraphSaver {
    private static final Logger LOGGER = Logger.getLogger(GraphSaver.class);
    private final GraphExportToDOTService graphExportService = new GraphExportToDOTService();
    private final GraphAnnotationsMapper graphAnnotationsMapper = new GraphAnnotationsMapper();

    public static Path getPath(String ontologyIri, ConversionSettings settings) {
        StringBuilder builder = new StringBuilder();
        if (settings.getMetric() != null) {
            builder.append("metric='").append(settings.getMetric()).append('\'');
        }
        if (settings.getPredefinedConverter() != null) {
            builder.append("converter='").append(settings.getPredefinedConverter()).append('\'');
        }
        String settingsFolder = builder.toString();
        String ontologyFolder = ontologyIri.replace('/', '|');
        Path homeDirectory = getHomeDirectory();
        if (homeDirectory == null) {
            return null;
        }
        Path appDirectory = homeDirectory.resolve(".ontology-viz");
        Path relativeGraphPath = Paths.get(ontologyFolder, settingsFolder, "graph");
        return appDirectory.resolve(relativeGraphPath);
    }

    public static Path getHomeDirectory() {
        String homeDir = System.getProperty("user.home");
        if (homeDir == null) {
            return null;
        }
        return Paths.get(homeDir);
    }

    public void saveGraph(Graph graph, String ontologyIri, ConversionSettings settings) {
        graphExportService.setGraph(graph);
        Path dotFilePath = getPath(ontologyIri, settings);
        if (dotFilePath == null) {
            return;
        }
        Path dir = dotFilePath.getParent();
        try {
            Files.createDirectories(dir);
            graphExportService.setFileName(dotFilePath.toString());
            graphExportService.restart();
            graphAnnotationsMapper.saveAnnotations(dir.toString(), graph);
        } catch (IOException e) {
            LOGGER.error("Failed to create graph directory " + dir);
        }
    }
}
