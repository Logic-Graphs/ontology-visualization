package org.golchin.ontology_visualization;

import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.model.OWLObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class ConversionUtils {
    private static final AtomicLong EDGE_COUNTER = new AtomicLong();

    public static String getEdgeId() {
        return "edge_" + EDGE_COUNTER.getAndIncrement();
    }

    public static void addToAttribute(Node node, String attribute, String element) {
        List<String> ids = (List<String>) node.getAttribute(attribute);
        if (ids == null) {
            ids = new ArrayList<>();
        }
        ids.add(element);
        node.setAttribute("ids", ids);
    }

    public static void addNodeIfNecessary(Graph graph, String nodeId, String label) {
        if (graph.getNode(nodeId) == null) {
            Node node = graph.addNode(nodeId);
            Object oldLabel = node.getAttribute("label");
            if (oldLabel != null) {
                label = oldLabel + ", " + label;
            }
            node.setAttribute("label", label);
            addToAttribute(node, "ids", nodeId);
        }
    }

    public static String getOWLObjectLabel(OWLObject owlObject) {
        if (owlObject instanceof OWLNamedObject) {
            return ((OWLNamedObject) owlObject).getIRI().getRemainder().orNull();
        }
        return owlObject.toString();
    }

    public static String getEdgeId(String sourceId, String targetId, String label) {
        return String.format("%s->%s->%s", sourceId, label, targetId);
    }
}
