package org.golchin.ontology_visualization;

import com.mxgraph.layout.mxGraphLayout;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.model.mxICell;
import com.mxgraph.view.mxGraph;
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.stream.GraphReplay;
import org.jgrapht.ext.JGraphXAdapter;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedPseudograph;

import java.awt.geom.Rectangle2D;
import java.util.function.Function;

public class MxLayoutMethod extends LayoutMethod {
    private final Function<mxGraph, mxGraphLayout> graphLayoutConstructor;

    public MxLayoutMethod(boolean isDeterministic, String layoutAlgorithmName, Function<mxGraph, mxGraphLayout> graphLayoutConstructor) {
        super(isDeterministic, layoutAlgorithmName);
        this.graphLayoutConstructor = graphLayoutConstructor;
    }

    static void setNodePositions(MultiGraph graph, JGraphXAdapter<String, DefaultEdge> g) {
        for (mxICell mxICell : g.getCellToVertexMap().keySet()) {
            mxGeometry geometry = mxICell.getGeometry();
            Rectangle2D rectangle2D = geometry.getRectangle().getBounds2D();
            double centerX = rectangle2D.getCenterX();
            double centerY = rectangle2D.getCenterY();
            String value = (String) mxICell.getValue();
            graph.getNode(value).setAttribute("xyz", centerX, centerY);
        }
    }

    static DirectedPseudograph<String, DefaultEdge> toJGraphTGraph(MultiGraph multiGraph) {
        DirectedPseudograph<String, DefaultEdge> result = new DirectedPseudograph<>(DefaultEdge.class);
        multiGraph.nodes().forEach(node -> result.addVertex(node.getId()));
        multiGraph.edges().forEach(edge -> result.addEdge(edge.getSourceNode().getId(), edge.getTargetNode().getId()));

        return result;
    }


    @Override
    public Graph layoutGraph(Graph graph) {
        MultiGraph copy = new MultiGraph("mg");
        GraphReplay replay = new GraphReplay("gr");
        replay.addSink(copy);
        replay.replay(graph);
        DirectedPseudograph<String, DefaultEdge> pseudograph = toJGraphTGraph(copy);
        JGraphXAdapter<String, DefaultEdge> graphXAdapter = new JGraphXAdapter<>(pseudograph);
        mxGraphLayout layout = graphLayoutConstructor.apply(graphXAdapter);
        layout.execute(graphXAdapter.getDefaultParent());
        setNodePositions(copy, graphXAdapter);
        return copy;
    }
}
