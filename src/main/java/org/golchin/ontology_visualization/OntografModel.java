package org.golchin.ontology_visualization;

import org.graphstream.graph.implementations.MultiGraph;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.search.EntitySearcher;
import uk.ac.manchester.cs.owl.owlapi.OWLClassImpl;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Adapted from sources of Ontograf Protege plugin: org.protege.ontograf.common.ProtegeGraphModel.
 */
public class OntografModel extends OntologyGraphModelBase {
    /**
     * Protege specific relationships
     */
    protected static final String DIRECT_SUBCLASS_SLOT_TYPE = "has subclass";
    protected static final String DIRECT_INDIVIDUAL_SLOT_TYPE = "has individual";
    protected static final String SUFFIX_DOMAIN_RANGE = " (Domain>Range)";
    protected static final String SUB_CLASS_SOME_VALUE_OF = "(Subclass some)";
    protected static final String SUB_CLASS_ALL_VALUES = "(Subclass all)";
    protected static final String EQUIVALENT_CLASS_SOME_VALUE_OF = "(Equivalent class some)";
    protected static final String EQUIVALENT_CLASS_ALL_VALUES = "(Equivalent class all)";

    /**
     * Protege specific node types
     */
    protected static final String UNKNOWN_ART_TYPE = "unknown";
    protected static final String CLASS_ART_TYPE = "class";
    protected static final String INDIVIDUAL_ART_TYPE = "individual";
    public static final OWLClassImpl THING = new OWLClassImpl(IRI.create("http://www.w3.org/2002/07/owl#Thing"));

    public OntografModel(OWLOntology ontology) {
        super(ontology);
    }

    private void loadParents(OWLEntity entityOfInterest) {
        if (!(entityOfInterest instanceof OWLClass)) {
            return;
        }

        OWLClass clsOfInterest = (OWLClass) entityOfInterest;

        for (OWLClass parentCls : this.provider.getParents(clsOfInterest)) {
            createEdge(parentCls, clsOfInterest, DIRECT_SUBCLASS_SLOT_TYPE);
        }

    }

    private void createDomainRangeRels(Set<OWLEntity> domains, Set<OWLEntity> ranges, OWLObjectProperty property) {
        // make relationships between all named classes in the domain and all named classes in the range
        for (OWLEntity domainClass : domains) {
            for (OWLEntity rangeClass : ranges) {
                String label = property.getIRI().getRemainder().orNull() + SUFFIX_DOMAIN_RANGE;
                createEdge(domainClass, rangeClass, label);
            }
        }
    }

    private void createDomainRangeRels(OWLOntology owlOntology) {
        Set<OWLObjectProperty> properties = owlOntology.getObjectPropertiesInSignature();

        for (OWLObjectProperty property : properties) {
            for (OWLObjectProperty owlObjectProperty : property.getObjectPropertiesInSignature()) {
                Collection<OWLClassExpression> domainVals = EntitySearcher
                        .getDomains(owlObjectProperty, owlOntology);
                Collection<OWLClassExpression> rangeVals = EntitySearcher
                        .getRanges(owlObjectProperty, owlOntology);

                if (domainVals.isEmpty() && !rangeVals.isEmpty()) {
                    domainVals.add(THING);
                } else if (rangeVals.isEmpty() && !domainVals.isEmpty()) {
                    rangeVals.add(THING);
                }

                Set<OWLEntity> domains = getOWLClasses(domainVals);
                Set<OWLEntity> ranges = getOWLClasses(rangeVals);

                createDomainRangeRels(domains, ranges, owlObjectProperty);
            }
        }
    }

    private Set<OWLEntity> getOWLClasses(
            Collection<OWLClassExpression> owlExpressions) {
        Set<OWLEntity> domains = new HashSet<>();
        for (OWLClassExpression expression : owlExpressions) {
            if (expression instanceof OWLClass) {
                domains.add((OWLClass) expression);
            }
        }

        return domains;
    }

    private void findIncomingIndividualRelationships(OWLEntity entityOfInterest) {
        if (!(entityOfInterest instanceof OWLNamedIndividual))
            return;

        OWLNamedIndividual destIndiv = ((OWLNamedIndividual) entityOfInterest);
        for (OWLClassExpression refNode : EntitySearcher.getTypes(destIndiv, ontology)) {
            if (refNode instanceof OWLClass) {
                OWLClass clsOwner = (OWLClass) refNode;
                createEdge(clsOwner, destIndiv, DIRECT_INDIVIDUAL_SLOT_TYPE);
            }
        }
    }

    private void unreifyRelationInstances(OWLEntity entity) {
        if (entity instanceof OWLNamedIndividual) {
            OWLNamedIndividual individual = (OWLNamedIndividual) entity;
            for (Map.Entry<OWLObjectPropertyExpression, Collection<OWLIndividual>> entry : EntitySearcher
                    .getObjectPropertyValues(individual, ontology).asMap()
                    .entrySet()) {
                for (OWLIndividual refIndividual : entry.getValue()) {
                    createEdge(individual, (OWLNamedIndividual) refIndividual, entry.getKey().toString());
                }
            }
        }
    }

    private void findIncomingConditionsRelationships(OWLEntity entityOfInterest) {
        if (!(entityOfInterest instanceof OWLClass)) {
            return;
        }

        OWLClass owlClass = (OWLClass) entityOfInterest;
        Collection<OWLAxiom> axioms = EntitySearcher.getReferencingAxioms(owlClass, ontology, true);
        for (OWLAxiom axiom : axioms) {
            if (axiom.getAxiomType().equals(AxiomType.SUBCLASS_OF)) {
                OWLSubClassOfAxiom subClassAxiom = (OWLSubClassOfAxiom) axiom;
                OWLClassExpression subClassExpression = subClassAxiom.getSubClass();

                if (subClassExpression instanceof OWLClass) {
                    OWLClassExpression superClassExpression = subClassAxiom.getSuperClass();
                    if (superClassExpression instanceof OWLQuantifiedRestriction) {
                        OWLQuantifiedRestriction<?> restriction = (OWLQuantifiedRestriction<?>) superClassExpression;
                        if (restriction.getFiller() instanceof OWLClass) {
                            String relType = getOWLObjectLabel(restriction.getProperty());
                            if (restriction instanceof OWLObjectSomeValuesFrom) {
                                relType += SUB_CLASS_SOME_VALUE_OF;
                            } else {
                                relType += SUB_CLASS_ALL_VALUES;
                            }

                            OWLEntity source = (OWLClass) subClassExpression;
                            OWLEntity target = (OWLClass) restriction.getFiller();

                            createEdge(source, target, relType);
                        }
                    }
                }
            }
        }
    }

    private void findOutgoingConditionsRelationships(OWLEntity entityOfInterest) {
        if (!(entityOfInterest instanceof OWLClass)) {
            return;
        }

        OWLClass owlClass = (OWLClass) entityOfInterest;

        convertOWLClassExpressionsToArcs(owlClass,
                EntitySearcher.getSuperClasses(owlClass, ontology),
                true);

        convertOWLClassExpressionsToArcs(owlClass,
                EntitySearcher.getEquivalentClasses(owlClass, ontology),
                false);
    }

    private void convertOWLClassExpressionsToArcs(OWLClass owlClass,
                                                  Collection<OWLClassExpression> expressions,
                                                  boolean isSubclass) {
        for (OWLClassExpression expression : expressions) {
            if (expression.getClassExpressionType().equals(ClassExpressionType.OBJECT_SOME_VALUES_FROM)
                    || expression.getClassExpressionType().equals(ClassExpressionType.OBJECT_ALL_VALUES_FROM)) {
                convertOWLClassExpressionToArcs(owlClass, expression, isSubclass);
            } else if (expression.getClassExpressionType().equals(ClassExpressionType.OBJECT_INTERSECTION_OF)) {
                for (OWLClassExpression e : expression.asConjunctSet()) {
                    convertOWLClassExpressionToArcs(owlClass, e, isSubclass);
                }
            }
        }
    }

    private void convertOWLClassExpressionToArcs(OWLClass owlClass,
                                                 OWLClassExpression expression,
                                                 boolean isSubClass) {
        for (OWLClassExpression e : expression.asConjunctSet()) {
            if (e instanceof OWLQuantifiedRestriction) {
                OWLQuantifiedRestriction<?> restriction = (OWLQuantifiedRestriction<?>) e;
                if (restriction.getFiller() instanceof OWLClass) {
                    String relType = getOWLObjectLabel(restriction.getProperty());
                    if (isSubClass) {
                        if (restriction instanceof OWLObjectSomeValuesFrom) {
                            relType += SUB_CLASS_SOME_VALUE_OF;
                        } else {
                            relType += SUB_CLASS_ALL_VALUES;
                        }
                    } else {
                        if (restriction instanceof OWLObjectSomeValuesFrom) {
                            relType += EQUIVALENT_CLASS_SOME_VALUE_OF;
                        } else {
                            relType += EQUIVALENT_CLASS_ALL_VALUES;
                        }
                    }

                    createEdge(owlClass, (OWLClass) restriction.getFiller(), relType);
                }
            }
        }
    }


    @Override
    public MultiGraph getGraph() {
        for (OWLEntity owlEntity : ontology.getSignature()) {
            loadParents(owlEntity);
            findIncomingIndividualRelationships(owlEntity);
            unreifyRelationInstances(owlEntity);
            findIncomingConditionsRelationships(owlEntity);
            findOutgoingConditionsRelationships(owlEntity);
        }
        createDomainRangeRels(ontology);
        return graph;
    }
}
