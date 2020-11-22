package org.golchin.ontology_visualization;

import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.search.EntitySearcher;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.stream.Collectors.*;

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
        node.setAttribute(attribute, ids);
    }

    public static void addNodeIfNecessary(Graph graph, String nodeId, String label, Map<IRI, List<String>> annotations) {
        if (graph.getNode(nodeId) == null) {
            Node node = graph.addNode(nodeId);
            Object oldLabel = node.getAttribute("label");
            if (oldLabel != null) {
                label = oldLabel + ", " + label;
            }
            node.setAttribute("label", label);
            node.setAttribute("annotations", annotations);
            // fixme always 1 element in ids list
            addToAttribute(node, "ids", nodeId);
        }
    }

    public static Map<IRI, List<String>> getAnnotationValues(OWLOntology ontology, OWLObject owlObject) {
        if (owlObject instanceof OWLEntity) {
            return EntitySearcher.getAnnotations((OWLEntity) owlObject, ontology)
                    .stream()
                    .collect(groupingBy(annotation -> annotation.getProperty().getIRI(),
                            mapping(annotation -> annotation.annotationValue().toString(), toList())));
        }
        return Collections.emptyMap();
    }

    public static String getAnnotationValue(OWLOntology ontology, OWLObject owlObject, OWLAnnotationProperty property) {
        if (owlObject instanceof OWLEntity) {
            Collection<OWLAnnotation> annotations = EntitySearcher.getAnnotations((OWLEntity) owlObject, ontology, property);
            if (annotations.isEmpty())
                return null;
            OWLAnnotation annotation = annotations.iterator().next();
            String stringValue = annotation.annotationValue().toString();
            return unquote(stringValue);
        }
        return null;
    }

    public static String getLabel(OWLOntology ontology, OWLObject owlObject) {
        OWLDataFactory owlDataFactory = ontology.getOWLOntologyManager().getOWLDataFactory();
        return getAnnotationValue(ontology, owlObject, owlDataFactory.getRDFSLabel());
    }

    public static String getEdgeId(String sourceId, String targetId, String label) {
        return String.format("%s->%s->%s", sourceId, label, targetId);
    }

    public static String unquote(String text) {
        text = text.trim();
        if (text.length() > 1 && text.startsWith("\"") && text.endsWith("\""))
            return text.substring(1, text.length() - 1);
        return text;
    }
}
