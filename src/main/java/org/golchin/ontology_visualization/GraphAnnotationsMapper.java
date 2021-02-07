package org.golchin.ontology_visualization;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graphstream.graph.Graph;
import org.semanticweb.owlapi.model.IRI;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GraphAnnotationsMapper {
    private static final TypeReference<Map<String, Map<IRI, List<String>>>> TYPE_REFERENCE = new TypeReference<Map<String, Map<IRI, List<String>>>>() {
    };
    public static final String FILE_NAME = "annotations.json";
    public static final String ATTRIBUTE = "annotations";
    private final ObjectMapper objectMapper = new ObjectMapper();

    protected void saveAnnotations(String directory, Graph graph) throws IOException {
        Map<String, Map<IRI, List<String>>> annotationsById = new HashMap<>();
        annotationsById.put("graph", (Map<IRI, List<String>>) graph.getAttribute("annotations"));
        graph.nodes().forEach(node ->
                annotationsById.put(node.getId(), (Map<IRI, List<String>>) node.getAttribute("annotations")));
        objectMapper.writeValue(new File(directory, "annotations.json"), annotationsById);
    }

    protected void restoreAnnotations(String directory, Graph graph) throws IOException {
        File file = new File(directory, FILE_NAME);
        Map<String, Map<IRI, List<String>>> annotationsById = objectMapper.readValue(file, TYPE_REFERENCE);
        graph.setAttribute(ATTRIBUTE, annotationsById.get("graph"));
        graph.nodes().forEach(node ->
                        node.setAttribute(ATTRIBUTE, annotationsById.get(node.getId())));
    }
}
