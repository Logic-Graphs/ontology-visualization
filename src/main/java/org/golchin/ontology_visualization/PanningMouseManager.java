package org.golchin.ontology_visualization;

import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.Hyperlink;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Border;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.ui.fx_viewer.FxViewPanel;
import org.graphstream.ui.fx_viewer.util.FxMouseOverMouseManager;
import org.graphstream.ui.geom.Point3;
import org.graphstream.ui.graphicGraph.GraphicElement;
import org.graphstream.ui.graphicGraph.GraphicGraph;
import org.graphstream.ui.graphicGraph.GraphicNode;
import org.graphstream.ui.view.View;
import org.graphstream.ui.view.camera.Camera;
import org.graphstream.ui.view.util.InteractiveElement;
import org.semanticweb.owlapi.model.IRI;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static javafx.scene.input.MouseEvent.*;

public class PanningMouseManager extends FxMouseOverMouseManager {
    private static final BrowsingService BROWSING_SERVICE = new BrowsingService();
    private final FxViewPanel panel;
    private final TextFlow text;
    private final Camera camera;
    private final Graph graph;
    private final Random random = new Random();

    public PanningMouseManager(FxViewPanel panel, TextFlow text, Camera camera, Graph graph) {
        this.panel = panel;
        this.text = text;
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
        Map<IRI, List<String>> graphAnnotations = (Map<IRI, List<String>>) this.graph.getAttribute("annotations");
        String titleAndDescription = graphAnnotations == null ? "" : createTitleAndDescription(graphAnnotations);
        Text titleAndDescriptionText = new Text(titleAndDescription);
        this.text.getChildren().add(titleAndDescriptionText);
        panel.addListener(MOUSE_PRESSED,
                (EventHandler<MouseEvent>) e -> {
                    curPoint = new Point3(e.getX(), e.getY(), 0);
                    EnumSet<InteractiveElement> interactiveElements = EnumSet.of(InteractiveElement.NODE);
                    List<GraphicElement> elements =
                            new ArrayList<>(panel.allGraphicElementsIn(interactiveElements, e.getX() - 1, e.getY() - 1,
                                    e.getX() + 1, e.getY() + 1));
                    if (!elements.isEmpty()) {
                        GraphicNode graphicNode = ((GraphicNode) elements.iterator().next());
                        Node node = this.graph.getNode(graphicNode.getId());
                        Map<IRI, List<String>> annotations = (Map<IRI, List<String>>) node.getAttribute("annotations");
                        if (annotations != null) {
                            this.text.getChildren().clear();
                            this.text.getChildren().add(titleAndDescriptionText);
                            Hyperlink iri = createBrowserLink(node.getId(), node.getId());
                            this.text.getChildren().add(iri);
                            this.text.getChildren().add(new Text("\n"));
                            this.text.getChildren().addAll(createTextFromAnnotations(annotations));
                        }
                    }
                });
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

    private String createTitleAndDescription(Map<IRI, List<String>> annotations) {
        StringBuilder stringBuilder = new StringBuilder();
        addAnnotationValue(annotations, stringBuilder, "http://purl.org/dc/elements/1.1/title");
        addAnnotationValue(annotations, stringBuilder, "http://purl.org/dc/elements/1.1/description");
        return stringBuilder.toString();
    }

    private void addAnnotationValue(Map<IRI, List<String>> annotations, StringBuilder stringBuilder, String s) {
        List<String> values = annotations.get(IRI.create(s));
        if (values != null && !values.isEmpty()) {
            String value = ConversionUtils.unquote(values.iterator().next());
            stringBuilder.append(value)
                    .append("\n\n");
        }
    }

    static Hyperlink createBrowserLink(String text, String url) {
        Hyperlink hyperlink = new Hyperlink(text);
        hyperlink.addEventHandler(MOUSE_CLICKED, event -> {
            BROWSING_SERVICE.setUrl(url);
            BROWSING_SERVICE.restart();
        });
        hyperlink.setBorder(Border.EMPTY);
        hyperlink.setPadding(Insets.EMPTY);
        return hyperlink;
    }

    private Collection<? extends javafx.scene.Node> createTextFromAnnotations(Map<IRI, List<String>> annotations) {
        return annotations.entrySet().stream()
                .flatMap(iriToValues -> {
                    IRI iri = iriToValues.getKey();
                    String url = iri.toString();
                    String value = iriToValues.getValue().iterator().next();
                    return Stream.of(
                            createBrowserLink(iri.getRemainder().or(url), url),
                            new Text(": "),
                            createLinkOrText(ConversionUtils.unquote(value)),
                            new Text("\n"));
                })
                .collect(Collectors.toList());
    }

    private javafx.scene.Node createLinkOrText(String text) {
        if (isValidURL(text)) {
            return createBrowserLink(text, text);
        }
        return new Text(text);
    }

    private boolean isValidURL(String text) {
        try {
            new URL(text);
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }
}
