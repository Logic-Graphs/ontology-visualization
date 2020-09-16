package org.golchin.ontology_visualization;

import lombok.AllArgsConstructor;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
import org.semanticweb.owlapi.model.*;
import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;
import owltools.graph.OWLQuantifiedProperty;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@AllArgsConstructor
public class OntologyToGraphConverterImpl implements OntologyToGraphConverter {
    private final int minDegree;
    private final AtomicInteger edgeCounter = new AtomicInteger();
    private final AtomicInteger nodeCounter = new AtomicInteger();

    public OntologyToGraphConverterImpl() {
        this(0);
    }

    private void traverseEquivalentClasses(OWLOntology ontology,
                                                  OWLClass initialClass,
                                                  String id,
                                                  Map<OWLObject, EquivalentClassesSet> owlObjectsToIds) {
        if (owlObjectsToIds.containsKey(initialClass))
            return;
        EquivalentClassesSet classesSet = new EquivalentClassesSet(id);
        classesSet.nodeIds.add(id);
        traverseEquivalentClasses(ontology, initialClass, classesSet, owlObjectsToIds);
    }

    private void traverseEquivalentClasses(OWLOntology ontology,
                                           OWLClass initialClass,
                                           EquivalentClassesSet classesSet,
                                           Map<OWLObject, EquivalentClassesSet> owlObjectsToIds) {
        owlObjectsToIds.put(initialClass, classesSet);
        classesSet.nodeIds.add(getOWLObjectId(initialClass));
        for (OWLEquivalentClassesAxiom axiom : ontology.getEquivalentClassesAxioms(initialClass)) {
            for (OWLClass namedClass : axiom.getNamedClasses()) {
                EquivalentClassesSet oldClassesSet = owlObjectsToIds.get(namedClass);
                if (oldClassesSet == null) {
                    traverseEquivalentClasses(ontology, namedClass, classesSet, owlObjectsToIds);
                } else if (!oldClassesSet.representativeId.equals(classesSet.representativeId)) {
                    // merge neighbor class to current one
                    owlObjectsToIds.put(namedClass, classesSet);
                    classesSet.nodeIds.addAll(oldClassesSet.nodeIds);
                }
            }
        }
    }

    @Override
    public MultiGraph convert(OWLOntology ontology) {
        OWLGraphWrapper graphWrapper = new OWLGraphWrapper(ontology);
        MultiGraph graph = new MultiGraph("g");
        Map<OWLObject, EquivalentClassesSet> owlObjectsToIds = new HashMap<>();
        Set<OWLObject> allOWLObjects = graphWrapper.getAllOWLObjects();
        convertClasses(ontology, graph, owlObjectsToIds, allOWLObjects);
        convertDataProperties(ontology, graph, owlObjectsToIds);
        for (OWLObject owlObject : allOWLObjects) {
            if (owlObject instanceof OWLObjectProperty) {
                convertDomainRangeAxioms(ontology, graph, owlObjectsToIds, (OWLObjectProperty) owlObject);
            }
            if (owlObject instanceof OWLEntity) {
                OWLEntity entity = (OWLEntity) owlObject;
                Set<OWLGraphEdge> outgoingEdges = graphWrapper.getOutgoingEdges(owlObject);
                for (OWLGraphEdge outgoingEdge : outgoingEdges) {
                    OWLObject target = outgoingEdge.getTarget();
                    if (target instanceof OWLEntity) {
                        String label = outgoingEdge.getQuantifiedPropertyList()
                                .stream()
                                .map(owlQuantifiedProperty -> {
                                    OWLObjectProperty property = owlQuantifiedProperty.getProperty();
                                    OWLQuantifiedProperty.Quantifier quantifier = owlQuantifiedProperty.getQuantifier();
                                    if (property == null)
                                        return quantifier.toString();
                                    return property.getIRI().getRemainder().orNull() + " " + quantifier;
                                })
                                .collect(Collectors.joining(", "));
                        addEdge(ontology, graph, getEdgeId(), owlObjectsToIds, entity, target, label);
                    }
                }
            }
        }
        for (EquivalentClassesSet classesSet : new HashSet<>(owlObjectsToIds.values())) {
            if (classesSet.nodeIds.size() > 1) {
                mergeEquivalentClasses(graph, classesSet);
            }
        }
        List<Node> nodesToRemove = graph.nodes()
                .filter(n -> n.getDegree() < minDegree)
                .collect(Collectors.toList());
        for (Node node : nodesToRemove) {
            graph.removeNode(node);
        }
        return graph;
    }

    private void convertDomainRangeAxioms(OWLOntology ontology,
                                          MultiGraph graph,
                                          Map<OWLObject, EquivalentClassesSet> owlObjectsToIds,
                                          OWLObjectProperty property) {
        Set<OWLObjectPropertyDomainAxiom> domainAxioms = ontology.getObjectPropertyDomainAxioms(property);
        Set<OWLObjectPropertyRangeAxiom> rangeAxioms = ontology.getObjectPropertyRangeAxioms(property);
        for (OWLObjectPropertyDomainAxiom domainAxiom : domainAxioms) {
            for (OWLObjectPropertyRangeAxiom rangeAxiom : rangeAxioms) {
                OWLClassExpression domain = domainAxiom.getDomain();
                OWLClassExpression range = rangeAxiom.getRange();
                if ((domain instanceof OWLClass) && (range instanceof OWLClass)) {
                    String id = getEdgeId();
                    String label = property.getIRI().getRemainder().orNull();
                    addEdge(ontology, graph, id, owlObjectsToIds, domain.asOWLClass(), range.asOWLClass(), label);
                }
            }
        }
    }

    private void convertDataProperties(OWLOntology ontology, MultiGraph graph, Map<OWLObject, EquivalentClassesSet> owlObjectsToIds) {
        for (OWLDataProperty property : ontology.getDataPropertiesInSignature()) {
            Set<OWLDataPropertyDomainAxiom> domainAxioms = ontology.getDataPropertyDomainAxioms(property);
            Set<OWLDataPropertyRangeAxiom> rangeAxioms = ontology.getDataPropertyRangeAxioms(property);
            for (OWLDataPropertyDomainAxiom domainAxiom : domainAxioms) {
                for (OWLDataPropertyRangeAxiom rangeAxiom : rangeAxioms) {
                    OWLClassExpression domain = domainAxiom.getDomain();
                    OWLDataRange range = rangeAxiom.getRange();
                    String label = property.getIRI().getRemainder().orNull();
                    addEdge(ontology, graph, getEdgeId(), owlObjectsToIds, domain, range, label);
                }
            }
        }
    }

    private void convertClasses(OWLOntology ontology, MultiGraph graph, Map<OWLObject, EquivalentClassesSet> owlObjectsToIds, Set<OWLObject> allOWLObjects) {
        for (OWLObject owlObject : allOWLObjects) {
            if (owlObject instanceof OWLClass) {
                OWLClass owlClass = (OWLClass) owlObject;
                Node node = graph.addNode(owlClass.toStringID());
                String label = owlClass.getIRI().getRemainder().orNull();
                node.setAttribute("label", label);
                addToAttribute(node, "ids", node.getId());
                traverseEquivalentClasses(ontology, owlClass, node.getId(), owlObjectsToIds);
            }
        }
    }

    private String getEdgeId() {
        return "edge_" + edgeCounter.getAndIncrement();
    }

    private void mergeEquivalentClasses(Graph graph, EquivalentClassesSet classesSet) {
        Node theNode = graph.getNode(classesSet.representativeId);
        List<Node> nodesToRemove = new ArrayList<>();
        Set<String> labels = new HashSet<>();
        for (String nodeId : classesSet.nodeIds) {
            Node node = graph.getNode(nodeId);
            Object label = node.getAttribute("label");
            labels.add(String.valueOf(label));
            if (!Objects.equals(nodeId, classesSet.representativeId)) {
                nodesToRemove.add(node);
                node.edges().forEach(edge -> {
                    Edge copy = copyEdge(graph, theNode, nodeId, node, edge);
                    copy.setAttribute("label", edge.getAttribute("label"));
                });
                addToAttribute(theNode, "ids", nodeId);
            }
        }
        for (Node node : nodesToRemove) {
            graph.removeNode(node);
        }
        theNode.setAttribute("label", String.join("\n", labels));
    }

    private Edge copyEdge(Graph graph, Node theNode, String nodeId, Node node, Edge edge) {
        String sourceId = edge.getSourceNode().getId();
        String targetId = edge.getTargetNode().getId();
        String edgeId = getEdgeId();
        if (edge.getSourceNode() == edge.getTargetNode() ||
                sourceId.equals(nodeId) && targetId.equals(nodeId)) {
            return graph.addEdge(edgeId, theNode, theNode, true);
        } else if (edge.getSourceNode() == node) {
            return graph.addEdge(edgeId, theNode, edge.getTargetNode(), true);
        } else {
            return graph.addEdge(edgeId, edge.getSourceNode(), theNode, true);
        }
    }

    static void addToAttribute(Node node, String attribute, String element) {
        List<String> ids = (List<String>) node.getAttribute(attribute);
        if (ids == null) {
            ids = new ArrayList<>();
        }
        ids.add(element);
        node.setAttribute("ids", ids);
    }

    static void addNodeIfNecessary(Graph graph, String nodeId, String label) {
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

    private void addEdge(OWLOntology ontology,
                        Graph graph,
                        String edgeId,
                        Map<OWLObject, EquivalentClassesSet> owlObjectsToIds,
                        OWLObject source,
                        OWLObject target,
                        String label) {
        if (source instanceof OWLClass) {
            traverseEquivalentClasses(ontology, ((OWLClass) source), getOWLObjectId(source), owlObjectsToIds);
        }
        if (target instanceof OWLClass) {
            traverseEquivalentClasses(ontology, ((OWLClass) target), getOWLObjectId(target), owlObjectsToIds);
        }
        String sourceId = getOWLObjectId(source);
        String sourceLabel = getOWLObjectLabel(source);
        String targetId = getOWLObjectId(target);
        String targetLabel = getOWLObjectLabel(target);
        addNodeIfNecessary(graph, sourceId, sourceLabel);
        addNodeIfNecessary(graph, targetId, targetLabel);
        graph.addEdge(edgeId, sourceId, targetId, true)
                .setAttribute("label", label);
    }

    private static String getOWLObjectLabel(OWLObject owlObject) {
        if (owlObject instanceof OWLNamedObject) {
            return ((OWLNamedObject) owlObject).getIRI().getRemainder().orNull();
        }
        return owlObject.toString();
    }

    private String getOWLObjectId(OWLObject owlObject) {
        if (owlObject instanceof OWLNamedObject) {
            String id = ((OWLNamedObject) owlObject).getIRI().toString();
            if (owlObject instanceof OWLDatatype) {
                OWLDatatype datatype = (OWLDatatype) owlObject;
                if (datatype.isBuiltIn()) {
                    return id + "_" + nodeCounter.getAndIncrement();
                }
            }
            return id;
        }
        return owlObject.toString();
    }

    protected static class EquivalentClassesSet {
        private final Set<String> nodeIds = new HashSet<>();
        private final String representativeId;

        public EquivalentClassesSet(String representativeId) {
            this.representativeId = representativeId;
            nodeIds.add(representativeId);
        }
    }
}
