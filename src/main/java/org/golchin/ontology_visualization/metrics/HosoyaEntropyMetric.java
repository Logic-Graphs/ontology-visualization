package org.golchin.ontology_visualization.metrics;

import org.graphstream.algorithm.APSP;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class HosoyaEntropyMetric extends EntropyMetric<Map<Integer, Integer>> {
    @Override
    protected Map<Integer, Integer> classifyNode(Node node) {
        Map<Integer, Integer> profile = new HashMap<>();
        APSP.APSPInfo apspInfo = (APSP.APSPInfo) node.getAttribute("APSPInfo");
        node.getGraph().nodes().forEach(otherNode -> {
            if (node != otherNode) {
                profile.merge((int) apspInfo.getLengthTo(otherNode.getId()), 1, Integer::sum);
            }
        });
        return profile;
    }

    @Override
    public double calculate(Graph graph) {
        APSP apsp = new APSP(graph);
        apsp.compute();
        return super.calculate(graph);
    }

    @Override
    public Comparator<Double> getComparator() {
        return Comparator.<Double>naturalOrder().reversed();
    }
}
