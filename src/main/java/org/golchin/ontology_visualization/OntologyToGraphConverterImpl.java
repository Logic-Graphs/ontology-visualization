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

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@AllArgsConstructor
public class OntologyToGraphConverterImpl implements OntologyToGraphConverter {
    private final int minDegree;

    public OntologyToGraphConverterImpl() {
        this(0);
    }

    @Override
    public MultiGraph convert(OWLOntology ontology) {
        OWLGraphWrapper graphWrapper = new OWLGraphWrapper(ontology);
        MultiGraph graph = new MultiGraph("g");
        Set<OWLObject> allOWLObjects = graphWrapper.getAllOWLObjects();
        for (OWLObject owlObject : allOWLObjects) {
            if (owlObject instanceof OWLClass) {
                OWLEntity entity = (OWLEntity) owlObject;
                Node node = graph.addNode(entity.toStringID());
                node.setAttribute("label", entity.getIRI().getRemainder().orNull());
            }
        }
        int i = 0;
        for (OWLDataProperty property : ontology.getDataPropertiesInSignature()) {
            Set<OWLDataPropertyDomainAxiom> domainAxioms = ontology.getDataPropertyDomainAxioms(property);
            Set<OWLDataPropertyRangeAxiom> rangeAxioms = ontology.getDataPropertyRangeAxioms(property);
            if (!domainAxioms.isEmpty() && !rangeAxioms.isEmpty()) {
                OWLClassExpression domain = domainAxioms.iterator().next().getDomain();
                OWLDataRange range = rangeAxioms.iterator().next().getRange();
                String label = property.getIRI().getRemainder().orNull();
                addEdgeIfNecessary(graph, "edge_" + i++, domain.asOWLClass().toStringID(), range.toString(), label);
            }
        }
        for (OWLObject owlObject : allOWLObjects) {
            if (owlObject instanceof OWLObjectProperty) {
                OWLObjectProperty property = (OWLObjectProperty) owlObject;
                Set<OWLObjectPropertyDomainAxiom> domainAxioms = ontology.getObjectPropertyDomainAxioms(property);
                Set<OWLObjectPropertyRangeAxiom> rangeAxioms = ontology.getObjectPropertyRangeAxioms(property);
                if (!domainAxioms.isEmpty() && !rangeAxioms.isEmpty()) {
                    OWLClassExpression domain = domainAxioms.iterator().next().getDomain();
                    OWLClassExpression range = rangeAxioms.iterator().next().getRange();
                    if ((domain instanceof OWLClass) && (range instanceof OWLClass)) {
                        String domainId = domain.asOWLClass().toStringID();
                        String rangeId = range.asOWLClass().toStringID();
                        addNodeIfNecessary(graph, domainId);
                        addNodeIfNecessary(graph, rangeId);
                        Edge edge = graph.addEdge("edge_" + i++, domainId, rangeId, true);
                        edge.setAttribute("label", property.getIRI().getRemainder().orNull());
                    }
                }
            }
            if (owlObject instanceof OWLEntity) {
                OWLEntity entity = (OWLEntity) owlObject;
                String sourceVertex = entity.toStringID();
                Set<OWLGraphEdge> outgoingEdges = graphWrapper.getOutgoingEdges(owlObject);
                for (OWLGraphEdge outgoingEdge : outgoingEdges) {
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
                    OWLObject target = outgoingEdge.getTarget();
                    if (target instanceof OWLEntity) {
                        String targetVertex = ((OWLEntity) target).toStringID();
                        addEdgeIfNecessary(graph, "edge_" + i++, sourceVertex, targetVertex, label);
                    }
                }
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

    static void addNodeIfNecessary(Graph graph, String nodeId) {
        if (graph.getNode(nodeId) == null) {
            graph.addNode(nodeId);
        }
    }

    static void addEdgeIfNecessary(Graph graph, String edgeId, String source, String target, String label) {
        addNodeIfNecessary(graph, source);
        addNodeIfNecessary(graph, target);
        graph.addEdge(edgeId, source, target, true)
                .setAttribute("label", label);
    }
}
