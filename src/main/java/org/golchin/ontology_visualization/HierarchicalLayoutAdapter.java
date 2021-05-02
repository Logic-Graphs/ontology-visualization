package org.golchin.ontology_visualization;

import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.ui.layout.HierarchicalLayout;

import java.util.*;

public class HierarchicalLayoutAdapter extends LayoutAdapter<HierarchicalLayout> {

    public HierarchicalLayoutAdapter() {
        super(HierarchicalLayout::new, true, "Hierarchical Layout");
    }

    private List<String> getRoots(Graph graph) {
        List<String> roots = new ArrayList<>();
        Set<Integer> visited = new HashSet<>();
        int i = 0;
        while (visited.size() < graph.getNodeCount()) {
            Node node = graph.getNode(i);
            if (visited.add(node.getIndex())) {
                roots.add(node.getId());
                Iterator<Node> nodeIterator = node.getBreadthFirstIterator(false);
                while (nodeIterator.hasNext()) {
                    Node next = nodeIterator.next();
                    visited.add(next.getIndex());
                }
            }
            i++;
        }
        return roots;
    }

    @Override
    public void prepareLayout(HierarchicalLayout layout, Graph graph) {
        List<String> roots = getRoots(graph);
        layout.setRoots(roots.toArray(new String[0]));
        super.prepareLayout(layout, graph);
    }
}
