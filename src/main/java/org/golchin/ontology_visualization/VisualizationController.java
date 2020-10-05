package org.golchin.ontology_visualization;

import com.google.common.collect.ImmutableMap;
import javafx.beans.property.ObjectProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.StringConverter;
import org.golchin.ontology_visualization.metrics.BaimuratovMetric;
import org.golchin.ontology_visualization.metrics.DegreeEntropyMetric;
import org.golchin.ontology_visualization.metrics.GraphMetric;
import org.golchin.ontology_visualization.metrics.layout.*;
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.ui.fx_viewer.FxViewPanel;
import org.graphstream.ui.fx_viewer.FxViewer;
import org.graphstream.ui.layout.Layout;
import org.graphstream.ui.layout.springbox.implementations.LinLog;
import org.graphstream.ui.layout.springbox.implementations.SpringBox;
import org.graphstream.ui.view.View;
import org.graphstream.ui.view.Viewer;
import org.graphstream.ui.view.camera.Camera;
import org.semanticweb.owlapi.model.OWLOntology;

import java.io.File;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;

public class VisualizationController {
    public static final List<Supplier<Layout>> POSSIBLE_LAYOUTS =
            Arrays.asList(LinLog::new, SpringBox::new);
    public static final Map<String, Supplier<Layout>> POSSIBLE_LAYOUTS_BY_NAME =
            POSSIBLE_LAYOUTS.stream().collect(toMap((Supplier<Layout> l) -> l.get().getLayoutAlgorithmName(), l -> l));

    public static final Map<String, LayoutMetric> METRICS_BY_NAME = new LinkedHashMap<>();
    public static final String EDGE_STYLESHEET = "edge { text-visibility-mode: hidden; text-visibility: 0.5;  }";
    public static final String NODE_STYLESHEET = "node { size-mode: fit; text-alignment: center; fill-color: green; shape: box; }";
    private static final String STYLESHEET = EDGE_STYLESHEET + " " + NODE_STYLESHEET;
    public static final List<Parameter<?>> PARAMETERS = Arrays.asList(OntologyToGraphConverterImpl.MERGE_EQUIVALENT, OntologyToGraphConverterImpl.MULTIPLY_DATATYPES);
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
    private static final Map<String, OntologyToGraphConverter> CONVERTERS_BY_NAME = ImmutableMap.of(
            "Ontograf", new OntografConverter(),
            "OWLViz", new OWLVizConverter());

    private Graph graph;

    public final ExecutorService executorService = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r);
        thread.setDaemon(true);
        return thread;
    });

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

    @FXML
    private ScrollPane scrollPane;

    @FXML
    private RadioButton explicitlySetParametersButton;

    @FXML
    private RadioButton chooseParametersButton;

    @FXML
    private ChoiceBox<Layout> layoutAlgorithmChoiceBox;

    @FXML
    private RadioButton usePredefinedAlgorithmButton;

    @FXML
    private RadioButton chooseLayoutAutomaticallyButton;

    @FXML
    private RadioButton usePredefinedConverterButton;

    @FXML
    private ChoiceBox<OntologyToGraphConverter> converterChoiceBox;

    private final Map<Parameter<?>, ObjectProperty<?>> conversionParameterValues = new HashMap<>();

    private static Collection<OntologyToGraphConverter> getConvertersWithParameterCombinations(List<Parameter<?>> parameters, int degree) {
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
        scrollPane.setContent(createParametersForm());
        ToggleGroup toggleGroup = new ToggleGroup();
        explicitlySetParametersButton.setToggleGroup(toggleGroup);
        chooseParametersButton.setToggleGroup(toggleGroup);
        usePredefinedConverterButton.setToggleGroup(toggleGroup);
        converterChoiceBox.getItems().addAll(CONVERTERS_BY_NAME.values());
        converterChoiceBox.getSelectionModel().selectFirst();
        converterChoiceBox.setConverter(new StringConverter<OntologyToGraphConverter>() {
            @Override
            public String toString(OntologyToGraphConverter object) {
                return String.valueOf(object);
            }

            @Override
            public OntologyToGraphConverter fromString(String string) {
                return CONVERTERS_BY_NAME.get(string);
            }
        });
        ToggleGroup layoutToggleGroup = new ToggleGroup();
        usePredefinedAlgorithmButton.setToggleGroup(layoutToggleGroup);
        chooseLayoutAutomaticallyButton.setToggleGroup(layoutToggleGroup);
        layoutAlgorithmChoiceBox.setConverter(new StringConverter<Layout>() {
            @Override
            public String toString(Layout object) {
                return object.getLayoutAlgorithmName();
            }

            @Override
            public Layout fromString(String string) {
                return POSSIBLE_LAYOUTS_BY_NAME.get(string).get();
            }
        });
        for (Supplier<Layout> layout : POSSIBLE_LAYOUTS) {
            layoutAlgorithmChoiceBox.getItems().add(layout.get());
        }
        layoutAlgorithmChoiceBox.getSelectionModel().selectFirst();
        log.setEditable(false);
    }

    private GridPane createParametersForm() {
        GridPane gridPane = new GridPane();
        gridPane.setHgap(20);
        gridPane.setVgap(20);
        for (int i = 0; i < PARAMETERS.size(); i++) {
            Parameter<?> parameter = PARAMETERS.get(i);
            Label label = new Label(parameter.getDescription());
            label.setPadding(new Insets(0, 0, 0, 5));
            gridPane.add(label, 0, i);
            ChoiceBox<Object> choiceBox = new ChoiceBox<>();
            choiceBox.getItems().addAll(parameter.getPossibleValues());
            choiceBox.getSelectionModel().selectFirst();
            gridPane.add(choiceBox, 1, i);
            conversionParameterValues.put(parameter, choiceBox.valueProperty());
        }
        return gridPane;
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
        Collection<OntologyToGraphConverter> converters =
                getConvertersWithParameterCombinations(PARAMETERS, degree);
        converters.addAll(CONVERTERS_BY_NAME.values());
        String graphMetricName = graphMetricChoiceBox.getSelectionModel().selectedItemProperty().getValue();
        GraphMetric graphMetric = GRAPH_METRICS_BY_NAME.get(graphMetricName);
        OntologyLoaderService service = new OntologyLoaderService(url.getText());
        service.setOnSucceeded(e -> {
            OWLOntology ontology = (OWLOntology) e.getSource().getValue();
            if (chooseParametersButton.isSelected()) {
                appendToLog("Choosing best graph representation with metric " + graphMetricName + "...");
                Task<EvaluatedGraph> task = new Task<EvaluatedGraph>() {
                    @Override
                    protected EvaluatedGraph call() {
                        return new GraphChooser(ontology, converters, graphMetric).choose();
                    }
                };
                executorService.submit(task);
                task.setOnSucceeded(event -> {
                    EvaluatedGraph evaluatedGraph = (EvaluatedGraph) event.getSource().getValue();
                    logParameterChoice(evaluatedGraph);
                    graph = evaluatedGraph.getGraph();
                });
            } else if (explicitlySetParametersButton.isSelected()) {
                Task<Graph> task = new Task<Graph>() {
                    @Override
                    protected Graph call() {
                        OntologyToGraphConverter converter =
                                new OntologyToGraphConverterImpl(degree, getParameterValues());
                        MultiGraph multiGraph = converter.convert(ontology);
                        graph = multiGraph;
                        return multiGraph;
                    }
                };
                executorService.submit(task);
            } else {
                OntologyToGraphConverter converter = converterChoiceBox.getValue();
                executorService.submit(new ConversionTask(ontology, converter));
            }
        });
        service.setOnFailed(e -> log.setText(e.getSource().getException().getMessage()));
        service.start();
    }

    private Map<Parameter<?>, Object> getParameterValues() {
        return conversionParameterValues.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, stringObjectPropertyEntry -> stringObjectPropertyEntry.getValue().get()));
    }

    @FXML
    public void visualize() {
        if (graph != null) {
            chooseLayoutAndVisualize(graph);
        }
    }

    private void logParameterChoice(EvaluatedGraph evaluatedGraph) {
        Map<OntologyToGraphConverter, Double> metricValuesByParameters = evaluatedGraph.getMetricValuesByConverters();
        for (Map.Entry<OntologyToGraphConverter, Double> entry : metricValuesByParameters.entrySet()) {
            OntologyToGraphConverter converter = entry.getKey();
            String formattedParameters = formatParameters(converter.getParameterValues());
            appendToLog(String.format("Value of metric with converter %s and parameters %s: %.3f",
                    converter,
                    formattedParameters,
                    entry.getValue()));
        }
        OntologyToGraphConverter bestConverter = evaluatedGraph.getBestConverter();
        appendToLog(String.format("Chose %s with parameters %s",
                bestConverter,
                formatParameters(bestConverter.getParameterValues())));
    }

    private String formatParameters(Map<Parameter<?>, Object> parametersMap) {
        return parametersMap.entrySet()
                .stream()
                .map(parameterToValue ->
                        String.format("'%s':%s", parameterToValue.getKey().getDescription(), parameterToValue.getValue()))
                .collect(joining(",", "{", "}"));
    }

    private void chooseLayoutAndVisualize(Graph graph) {
        if (usePredefinedAlgorithmButton.isSelected()) {
            Task<Graph> task = new Task<Graph>() {
                @Override
                protected Graph call() {
                    return LayoutChooser.layoutGraph(graph, layoutAlgorithmChoiceBox.getValue());
                }
            };
            executorService.submit(task);
            task.setOnSucceeded(event -> {
                layoutGraph = (Graph) event.getSource().getValue();
                visualize(layoutGraph);
            });
            return;
        }
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

    class ConversionTask extends Task<Graph> {
        private final OWLOntology ontology;
        private final OntologyToGraphConverter converter;

        ConversionTask(OWLOntology ontology, OntologyToGraphConverter converter) {
            this.ontology = ontology;
            this.converter = converter;
        }

        @Override
        protected Graph call() {
            MultiGraph multiGraph = converter.convert(ontology);
            graph = multiGraph;
            return multiGraph;
        }
    }
}
