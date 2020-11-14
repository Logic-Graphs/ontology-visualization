package org.golchin.ontology_visualization.metrics;

import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.SingleGraph;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class HosoyaEntropyMetricTest {
    private final HosoyaEntropyMetric metric = new HosoyaEntropyMetric();

    private static Stream<Arguments> params() {
        Graph threeNodeCycle = new SingleGraph("3-cycle");
        SingleGraph graph = new SingleGraph("g");
        threeNodeCycle.addSink(graph);
        threeNodeCycle.addNode("1");
        threeNodeCycle.addNode("2");
        threeNodeCycle.addNode("3");
        threeNodeCycle.addEdge("1", "1", "2", true);
        threeNodeCycle.addEdge("2", "2", "3", true);
        threeNodeCycle.addEdge("3", "3", "1", true);
        graph.removeEdge("3");
        graph.addEdge("3", "1", "3", true);

        SingleGraph emptyGraph = new SingleGraph("empty");
        emptyGraph.addNode("a");
        emptyGraph.addNode("b");
        return Stream.of(
                arguments(threeNodeCycle, 0.0),
                arguments(graph, Math.log(3.0)),
                arguments(emptyGraph, 0.0));
    }

    @ParameterizedTest
    @MethodSource("params")
    void calculate(Graph graph, double expectedValue) {
        assertEquals(expectedValue, metric.calculate(graph), 1e-6);
    }
}