package org.golchin.ontology_visualization;

import com.google.common.collect.ImmutableMap;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
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

import java.io.File;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;

public class VisualizationController {
    public static final List<Supplier<Layout>> POSSIBLE_LAYOUTS =
            Arrays.asList(LinLog::new, SpringBox::new);

    public static final Map<String, LayoutMetric> METRICS_BY_NAME = new LinkedHashMap<>();
    public static final String EDGE_STYLESHEET = "edge { text-visibility-mode: hidden; text-visibility: 0.5;  }";
    public static final String NODE_STYLESHEET = "node { size-mode: fit; text-alignment: center; fill-color: green; shape: box; }";
    private static final String STYLESHEET = EDGE_STYLESHEET + " " + NODE_STYLESHEET;

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
    private Graph graph;

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

    private final GraphImportService importService = new GraphImportService();

    private final FileChooser fileChooser = new FileChooser();

    private final GraphExportToImageService exportToImageService = new GraphExportToImageService();
    
    private final GraphExportToDOTService exportToDOTService = new GraphExportToDOTService();

    private final FileChooser imageFileChooser = new FileChooser();

    private Graph layoutGraph;

    private static Collection<? extends OntologyToGraphConverter> getConvertersWithParameterCombinations(List<Parameter<?>> parameters, int degree) {
        return Parameter.getParameterCombinations(parameters)
                .stream()
                .map(parametersMap -> new OntologyToGraphConverterImpl(degree, parametersMap))
                .collect(Collectors.toList());
    }

    @FXML
    public void initialize() {
        url.setText("http://xmlns.com/foaf/spec/index.rdf");
        minDegree.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100));
        metricChoiceBox.getItems().addAll(METRICS_BY_NAME.keySet());
        metricChoiceBox.getSelectionModel().select(0);
        graphMetricChoiceBox.getItems().addAll(GRAPH_METRICS_BY_NAME.keySet());
        graphMetricChoiceBox.getSelectionModel().select(0);
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Graphviz format", "*.dot"));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Any file", "*"));
        imageFileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PNG image", "*.png"),
                new FileChooser.ExtensionFilter("JPG image", "*.jpg"),
                new FileChooser.ExtensionFilter("BMP image", "*.bmp")
        );
    }

    @FXML
    public void exportToImage() {
        export(exportToImageService, imageFileChooser);
    }


    public void exportGraph() {
        export(exportToDOTService, fileChooser);
    }

    private void export(GraphExportService service, FileChooser chooser) {
        if (layoutGraph == null) {
            // fixme maybe use SimpleObjectProperty for graph?
            new Alert(Alert.AlertType.ERROR, "No layout has been computed").show();
            return;
        }
        layoutGraph.setAttribute("ui.stylesheet", NODE_STYLESHEET);
        File file = chooser.showSaveDialog(null);
        service.setFileName(file.getPath());
        service.setGraph(layoutGraph);
        service.setOnSucceeded(event ->
                new Alert(Alert.AlertType.INFORMATION, "Visualization successfully exported").show());
        service.setOnFailed(event ->
                new Alert(Alert.AlertType.ERROR, "Export failed").show());
        service.restart();
    }

    @FXML
    public void importGraph() {
        Window window = log.getScene().getWindow();
        File file = fileChooser.showOpenDialog(window);
        importService.setFileName(file.toString());
        importService.setOnSucceeded(event ->
                graph = (Graph) event.getSource().getValue());
        importService.setOnFailed(event -> {
            Throwable exception = event.getSource().getException();
            if (exception != null) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Not a valid DOT file");
                alert.show();
            }
        });
        importService.restart();
    }

    @FXML
    public void importOntology() {
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
            logParameterChoice(evaluatedGraph);
            graph = evaluatedGraph.getGraph();
        });
        service.setOnFailed(e -> log.setText(e.getSource().getException().getMessage()));
        service.start();
        appendToLog("Choosing best graph representation with metric " + graphMetricName + "...");
    }

    @FXML
    public void visualize() {
        if (graph != null) {
            chooseLayoutAndVisualize(graph, true);
        }
    }

    private void logParameterChoice(EvaluatedGraph evaluatedGraph) {
        Map<Map<String, Object>, Double> metricValuesByParameters = evaluatedGraph.getMetricValuesByParameters();
        for (Map.Entry<Map<String, Object>, Double> entry : metricValuesByParameters.entrySet()) {
            appendToLog("Value of metric with parameters " + entry.getKey() +
                    ": " + entry.getValue());
        }
        appendToLog("Chose " + evaluatedGraph.getBestParameters());
    }

    private void chooseLayoutAndVisualize(Graph graph, boolean shouldVisualize) {
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
            layoutGraph = evaluatedLayout.getBestLayout();
            if (shouldVisualize)
                visualize(layoutGraph);
        });
        layoutChooserService.setOnFailed(workerStateEvent ->
                log.setText(workerStateEvent.getSource().getException().getMessage()));
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
