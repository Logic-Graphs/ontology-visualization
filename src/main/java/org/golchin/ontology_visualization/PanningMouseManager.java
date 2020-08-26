package org.golchin.ontology_visualization;

import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import org.graphstream.graph.Graph;
import org.graphstream.ui.fx_viewer.FxViewPanel;
import org.graphstream.ui.fx_viewer.util.FxMouseOverMouseManager;
import org.graphstream.ui.geom.Point3;
import org.graphstream.ui.graphicGraph.GraphicElement;
import org.graphstream.ui.graphicGraph.GraphicGraph;
import org.graphstream.ui.view.View;
import org.graphstream.ui.view.camera.Camera;
import org.graphstream.ui.view.util.InteractiveElement;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;

import static javafx.scene.input.MouseEvent.*;

public class PanningMouseManager extends FxMouseOverMouseManager {
    private final FxViewPanel panel;
    private final Camera camera;
    private final Graph graph;
    private final Random random = new Random();

    public PanningMouseManager(FxViewPanel panel, Camera camera, Graph graph) {
        this.panel = panel;
        this.camera = camera;
        this.graph = graph;
    }

    private Point3 curPoint;
    
    @Override
    public void init(GraphicGraph graph, View view) {
        super.init(graph, view);
        release();
        panel.addListener(MOUSE_DRAGGED,
                (EventHandler<MouseEvent>) (e -> {
                    Point3 nextPointGu = camera.transformPxToGu(e.getX(), e.getY());
                    Point3 curPointGu = camera.transformPxToGu(curPoint.x, curPoint.y);
                    Point3 deltaGu = new Point3(nextPointGu.x - curPointGu.x, nextPointGu.y - curPointGu.y);
                    Point3 viewCenter = camera.getViewCenter();
                    camera.setViewCenter(viewCenter.x - deltaGu.x, viewCenter.y - deltaGu.y, viewCenter.z);
                    curPoint = new Point3(e.getX(), e.getY(), 0);
                }));
        panel.addListener(MOUSE_PRESSED,
                (EventHandler<MouseEvent>) (e -> curPoint = new Point3(e.getX(), e.getY(), 0)));
        final GraphicElement[] prevElement = {null};
        panel.addListener(MOUSE_MOVED, (EventHandler<MouseEvent>) (e -> {
            if (prevElement[0] != null) {
                String id = prevElement[0].getId();
                String stylesheet = String.format("edge#%s { text-visibility-mode: hidden; fill-color: black; }", id);
                graph.setAttribute("ui.stylesheet", stylesheet);
            }
            EnumSet<InteractiveElement> types = EnumSet.of(InteractiveElement.EDGE);
            int halfLength = 10;
            List<GraphicElement> elements =
                    new ArrayList<>(panel.allGraphicElementsIn(types, e.getX() - halfLength, e.getY() - halfLength,
                            e.getX() + halfLength, e.getY() + halfLength));
            if (!elements.isEmpty()) {
                GraphicElement chosenElement = elements.get(random.nextInt(elements.size()));
                String stylesheet = String.format("edge#%s { text-visibility-mode: normal;  text-alignment: along; fill-color: blue; }", chosenElement.getId());
                graph.setAttribute("ui.stylesheet", stylesheet);
                prevElement[0] = chosenElement;
            }
        }));
    }

    @Override
    protected void mouseOverElement(GraphicElement element) {
        super.mouseOverElement(element);
    }
}
