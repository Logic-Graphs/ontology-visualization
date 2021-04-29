package org.golchin.ontology_visualization;

import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.stream.GraphReplay;
import org.graphstream.ui.layout.Layout;

import java.util.function.Supplier;

public class LayoutAdapter<T extends Layout> extends LayoutMethod {
    private final Supplier<T> layoutSupplier;

    public LayoutAdapter(Supplier<T> layoutSupplier, String layoutAlgorithmName) {
        this(layoutSupplier, false, layoutAlgorithmName);
    }

    public LayoutAdapter(Supplier<T> layoutSupplier, boolean isDeterministic, String layoutAlgorithmName) {
        super(isDeterministic, layoutAlgorithmName);
        this.layoutSupplier = layoutSupplier;
    }

    public void prepareLayout(T layout, Graph graph) {
        layout.setStabilizationLimit(0.9);
    }

    public Graph layoutGraph(Graph graph) {
        T layout = layoutSupplier.get();
        prepareLayout(layout, graph);
        MultiGraph copy = new MultiGraph("mg");
        copy.addSink(layout);
        layout.addAttributeSink(copy);
        GraphReplay replay = new GraphReplay("gr");
        replay.addSink(copy);
        replay.replay(graph);
        do {
            layout.compute();
        } while (layout.getStabilization() < layout.getStabilizationLimit());
        return copy;
    }

}
