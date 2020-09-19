package org.golchin.ontology_visualization;

import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.golchin.ontology_visualization.aesthetics.*;
import org.graphstream.graph.Graph;
import org.graphstream.ui.fx_viewer.FxViewPanel;
import org.graphstream.ui.fx_viewer.FxViewer;
import org.graphstream.ui.layout.Layout;
import org.graphstream.ui.layout.springbox.implementations.LinLog;
import org.graphstream.ui.layout.springbox.implementations.SpringBox;
import org.graphstream.ui.view.View;
import org.graphstream.ui.view.Viewer;
import org.graphstream.ui.view.camera.Camera;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class VisualizationController {
    public static final List<Supplier<Layout>> POSSIBLE_LAYOUTS =
            Arrays.asList(LinLog::new, SpringBox::new);

    public static final Map<String, Aesthetics> METRICS_BY_NAME = new LinkedHashMap<>();
    private static final String STYLESHEET = "edge { text-visibility-mode: hidden; text-visibility: 0.5;  }" +
            " node { size-mode: fit; text-alignment: center; fill-color: green; shape: box; }";

    static {
        METRICS_BY_NAME.put("Number of crossings", new NumberOfCrossings());
        METRICS_BY_NAME.put("Crossings angle resolution", new CrossingAngleResolution());
        METRICS_BY_NAME.put("Node angle resolution", new NodeAngleResolution());
        METRICS_BY_NAME.put("Edge length standard deviation", new EdgeLengthStd());
        METRICS_BY_NAME.put("k-nearest neighbors shape graph similarity", new ShapeGraphSimilarity(5));
    }

    @FXML
    private TextField url;

    @FXML
    private ChoiceBox<String> metricChoiceBox;

    @FXML
    private Label log;

    @FXML
    private Spinner<Integer> minDegree;

    @FXML
    public void initialize() {
        url.setText("http://xmlns.com/foaf/spec/index.rdf");
        minDegree.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100));
        metricChoiceBox.getItems().addAll(VisualizationController.METRICS_BY_NAME.keySet());
        metricChoiceBox.getSelectionModel().select(0);
    }

    @FXML
    public void visualize() {
        log.setText("");
        int degree = minDegree.getValue() == null ? 0 : minDegree.getValue();
        OntologyToGraphConverterImpl converter = new OntologyToGraphConverterImpl(degree);
        OntologyLoaderService service = new OntologyLoaderService(url.getText(), converter);
        service.setOnSucceeded(e -> {
            Graph graph = (Graph) e.getSource().getValue();
            String metricName = metricChoiceBox.getSelectionModel().selectedItemProperty().getValue();
            Aesthetics aesthetics = METRICS_BY_NAME.get(metricName);

            LayoutChooser layoutChooser = new LayoutChooser(graph, POSSIBLE_LAYOUTS, 5, aesthetics);
            LayoutChooserService layoutChooserService = new LayoutChooserService(layoutChooser);
            layoutChooserService.start();
            layoutChooserService.setOnSucceeded(stateEvent -> {
                EvaluatedLayout evaluatedLayout = (EvaluatedLayout) stateEvent.getSource().getValue();
                String name = evaluatedLayout.getName();
                String summary = evaluatedLayout.getVariants().entrySet()
                        .stream()
                        .map(metricNameToValue -> metricNameToValue.getKey() + " with average " + metricNameToValue.getValue())
                        .collect(Collectors.joining("\n", "", "\nChose " + name));
                log.setText(summary);
                Graph layoutGraph = evaluatedLayout.getBestLayout();
                layoutGraph.setAttribute("ui.stylesheet", STYLESHEET);
                FxViewer fxViewer = new FxViewer(layoutGraph, Viewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);
                FxViewPanel panel = (FxViewPanel) fxViewer.addDefaultView(false);
                View view = fxViewer.getDefaultView();
                Camera camera = view.getCamera();

                panel.setMouseManager(new PanningMouseManager(panel, camera, layoutGraph));
                panel.setOnScroll(event -> {
                    double delta = 0.05;
                    if (event.getDeltaY() > 0)
                        delta = -delta;
                    camera.setViewPercent(camera.getViewPercent() + delta);
                });

                Stage stage = new Stage();
                stage.setScene(new Scene(panel));
                stage.show();
            });
            layoutChooserService.setOnFailed(workerStateEvent ->
                    log.setText(workerStateEvent.getSource().getException().getMessage()));
        });
        service.setOnFailed(e -> log.setText(e.getSource().getException().getMessage()));
        service.restart();

    }
}
