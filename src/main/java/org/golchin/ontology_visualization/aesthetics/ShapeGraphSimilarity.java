package org.golchin.ontology_visualization.aesthetics;

import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import weka.core.*;
import weka.core.neighboursearch.KDTree;

import java.awt.geom.Point2D;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ShapeGraphSimilarity implements Aesthetics {
    private final int k;

    public ShapeGraphSimilarity(int k) {
        this.k = k;
    }

    @Override
    public double calculate(Graph graph, Function<Node, Point2D> vertexToPoint) {
        ArrayList<Attribute> attrs = new ArrayList<>(
                Arrays.asList(new Attribute("x"), new Attribute("y")));
        Instances instances = new Instances("points", attrs, graph.getNodeCount());
        List<Node> nodeList = graph.nodes().collect(Collectors.toList());
        Map<Node, Instance> nodesToInstances = new HashMap<>();
        Comparator<Instance> comparator = Comparator.comparing((Instance instance) -> instance.value(0))
                .thenComparing((Instance instance) -> instance.value(1));
        Map<Instance, Node> instancesToNodes = new TreeMap<>(comparator);
        SingleGraph shapeGraph = new SingleGraph("shape_graph");
        for (Node node : nodeList) {
            Point2D point2D = vertexToPoint.apply(node);
            DenseInstance instance = new DenseInstance(1.0, new double[]{point2D.getX(), point2D.getY()});
            instances.add(instance);
            instance.setDataset(instances);
            nodesToInstances.put(node, instance);
            instancesToNodes.put(instance, node);
            shapeGraph.addNode(node.getId());
        }
        KDTree kdTree = new KDTree();
        try {
            kdTree.setInstances(instances);
            kdTree.setDistanceFunction(new EuclideanDistance(instances));
        } catch (Exception ignore) {
        }
        for (Node node : nodeList) {
            Instance instance = nodesToInstances.get(node);
            try {
                Instances neighbours = kdTree.kNearestNeighbours(instance, k + 1);
                neighbours.stream()
                        .map(instancesToNodes::get)
                        .filter(n -> !Objects.equals(n.getId(), node.getId()))
                        .filter(n -> shapeGraph.getNode(node.getId()).getEdgeBetween(n.getId()) == null)
                        .forEach(n -> shapeGraph.addEdge(UUID.randomUUID().toString(),
                                node.getId(), n.getId()));
            } catch (Exception ignored) {
            }
        }
        return new MeanJaccardIndex().measure(graph, shapeGraph);
    }

    @Override
    public Comparator<Double> getComparator() {
        return Comparator.naturalOrder();
    }
}
