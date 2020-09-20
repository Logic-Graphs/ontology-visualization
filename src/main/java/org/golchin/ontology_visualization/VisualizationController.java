package org.golchin.ontology_visualization;

import com.google.common.collect.ImmutableMap;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.golchin.ontology_visualization.metrics.BaimuratovMetric;
import org.golchin.ontology_visualization.metrics.DegreeEntropyMetric;
import org.golchin.ontology_visualization.metrics.GraphMetric;
import org.golchin.ontology_visualization.metrics.layout.*;
import org.graphstream.graph.Graph;
import org.graphstream.ui.fx_viewer.FxViewPanel;
import org.graphstream.ui.fx_viewer.FxViewer;
import org.graphstream.ui.layout.Layout;
import org.graphstream.ui.layout.springbox.implementations.LinLog;
import org.graphstream.ui.layout.springbox.implementations.SpringBox;
import org.graphstream.ui.view.View;
import org.graphstream.ui.view.Viewer;
import org.graphstream.ui.view.camera.Camera;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;

public class VisualizationController {
    public static final List<Supplier<Layout>> POSSIBLE_LAYOUTS =
            Arrays.asList(LinLog::new, SpringBox::new);

    public static final Map<String, LayoutMetric> METRICS_BY_NAME = new LinkedHashMap<>();
    private static final String STYLESHEET = "edge { text-visibility-mode: hidden; text-visibility: 0.5;  }" +
            " node { size-mode: fit; text-alignment: center; fill-color: green; shape: box; }";

    static {
        METRICS_BY_NAME.put("Number of crossings", new NumberOfCrossings());
        METRICS_BY_NAME.put("Crossings angle resolution", new CrossingAngleResolution());
        METRICS_BY_NAME.put("Node angle resolution", new NodeAngleResolution());
        METRICS_BY_NAME.put("Edge length standard deviation", new EdgeLengthStd());
        METRICS_BY_NAME.put("k-nearest neighbors shape graph similarity", new ShapeGraphSimilarity(5));
    }

    private static final Map<String, GraphMetric> GRAPH_METRICS_BY_NAME = ImmutableMap.of(
            "Entropy", new DegreeEntropyMetric(),
            "Baimuratov et al.", new BaimuratovMetric());

    @FXML
    private TextField url;

    @FXML
    private ChoiceBox<String> graphMetricChoiceBox;

    @FXML
    private ChoiceBox<String> metricChoiceBox;

    @FXML
    private TextArea log;

    @FXML
    private Spinner<Integer> minDegree;

    @FXML
    public void initialize() {
        url.setText("http://xmlns.com/foaf/spec/index.rdf");
        minDegree.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100));
        metricChoiceBox.getItems().addAll(METRICS_BY_NAME.keySet());
        metricChoiceBox.getSelectionModel().select(0);
        graphMetricChoiceBox.getItems().addAll(GRAPH_METRICS_BY_NAME.keySet());
        graphMetricChoiceBox.getSelectionModel().select(0);
    }

    private static Collection<? extends OntologyToGraphConverter> getConvertersWithParameterCombinations(List<Parameter<?>> parameters, int degree) {
        return Parameter.getParameterCombinations(parameters)
                .stream()
                .map(parametersMap -> new OntologyToGraphConverterImpl(degree, parametersMap))
                .collect(Collectors.toList());
    }

    @FXML
    public void visualize() {
        log.setText("");
        int degree = minDegree.getValue() == null ? 0 : minDegree.getValue();
        List<Parameter<?>> parameters = Arrays.asList(OntologyToGraphConverterImpl.MERGE_EQUIVALENT, OntologyToGraphConverterImpl.MULTIPLY_DATATYPES);
        Collection<? extends OntologyToGraphConverter> converters =
                getConvertersWithParameterCombinations(parameters, degree);
        String graphMetricName = graphMetricChoiceBox.getSelectionModel().selectedItemProperty().getValue();
        GraphMetric graphMetric = GRAPH_METRICS_BY_NAME.get(graphMetricName);
        OntologyLoaderService service = new OntologyLoaderService(url.getText(), graphMetric, converters);
        service.setOnSucceeded(e -> {
            EvaluatedGraph evaluatedGraph = (EvaluatedGraph) e.getSource().getValue();
            Graph graph = evaluatedGraph.getGraph();
            Map<Map<String, Object>, Double> metricValuesByParameters = evaluatedGraph.getMetricValuesByParameters();
            for (Map.Entry<Map<String, Object>, Double> entry : metricValuesByParameters.entrySet()) {
                appendToLog("Value of metric with parameters " + entry.getKey() +
                        ": " + entry.getValue());
            }
            appendToLog("Chose " + evaluatedGraph.getBestParameters());
            String metricName = metricChoiceBox.getSelectionModel().selectedItemProperty().getValue();
            LayoutMetric layoutMetric = METRICS_BY_NAME.get(metricName);

            LayoutChooser layoutChooser = new LayoutChooser(graph, POSSIBLE_LAYOUTS, 5, layoutMetric);
            LayoutChooserService layoutChooserService = new LayoutChooserService(layoutChooser);
            layoutChooserService.start();
            appendToLog("Choosing best layout with metric " + metricName + "...");
            layoutChooserService.setOnSucceeded(stateEvent -> {
                EvaluatedLayout evaluatedLayout = (EvaluatedLayout) stateEvent.getSource().getValue();
                String name = evaluatedLayout.getName();
                String summary = evaluatedLayout.getVariants().entrySet()
                        .stream()
                        .map(metricNameToValue -> "Average value of metric for " + metricNameToValue.getKey() +
                                        ": " + metricNameToValue.getValue())
                        .collect(joining("\n", "", "\nChose " + name));
                appendToLog(summary);
                Graph layoutGraph = evaluatedLayout.getBestLayout();
                visualize(layoutGraph);
            });
            layoutChooserService.setOnFailed(workerStateEvent ->
                    log.setText(workerStateEvent.getSource().getException().getMessage()));
        });
        service.setOnFailed(e -> log.setText(e.getSource().getException().getMessage()));
        service.start();
        appendToLog("Choosing best graph representation with metric " + graphMetricName + "...");
    }

    private void visualize(Graph layoutGraph) {
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
    }

    private void appendToLog(String line) {
        log.setText(log.getText() + line + "\n");
    }
}
