package org.golchin.ontology_visualization.metrics;

import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import weka.core.matrix.Matrix;
import weka.core.matrix.SingularValueDecomposition;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

public class AdjacencyMatrixEnergy implements GraphMetric {
    private Matrix getAdjacencyMatrix(Graph graph) {
        List<Node> nodes = graph.nodes().collect(Collectors.toList());
        int nodeCount = graph.getNodeCount();
        double[][] matrix = new double[nodeCount][nodeCount];
        for (int i = 0; i < nodes.size(); i++) {
            for (int j = 0; j < nodes.size(); j++) {
                if (i != j) {
                    matrix[i][j] = nodes.get(i).hasEdgeToward(nodes.get(j)) ? 1 : 0;
                }
            }
        }
        return new Matrix(matrix);
    }

    @Override
    public double calculate(Graph graph) {
        Matrix adjacencyMatrix = getAdjacencyMatrix(graph);
        SingularValueDecomposition svd = new SingularValueDecomposition(adjacencyMatrix);
        double[] singularValues = svd.getSingularValues();
        return DoubleStream.of(singularValues).sum();
    }

    @Override
    public Comparator<Double> getComparator() {
        return Comparator.<Double>naturalOrder().reversed();
    }
}
