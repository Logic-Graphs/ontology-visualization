package org.golchin.ontology_visualization;

import com.google.common.collect.ImmutableMap;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.StringConverter;
import org.apache.log4j.Logger;
import org.golchin.ontology_visualization.metrics.*;
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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;

public class VisualizationController {
    private static final Logger LOGGER = Logger.getLogger(VisualizationController.class);
    public static final List<Supplier<Layout>> POSSIBLE_LAYOUTS =
            Arrays.asList(LinLog::new, SpringBox::new);
    public static final Map<String, Supplier<Layout>> POSSIBLE_LAYOUTS_BY_NAME =
            POSSIBLE_LAYOUTS.stream().collect(toMap((Supplier<Layout> l) -> l.get().getLayoutAlgorithmName(), l -> l));

    public static final Map<String, LayoutMetric> METRICS_BY_NAME = new LinkedHashMap<>();
    public static final String EDGE_STYLESHEET = "edge { text-visibility-mode: hidden; text-visibility: 0.5;  }";
    public static final String NODE_STYLESHEET = "node { size-mode: fit; text-alignment: center; fill-color: rgb(170, 204, 255); shape: box; text-offset: 5, -2; }";
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
            "Baimuratov et al.", new BaimuratovMetric(),
            "Hosoya entropy", new HosoyaEntropyMetric(),
            "Adjacency matrix energy", new AdjacencyMatrixEnergy());
    private static final Map<String, OntologyToGraphConverter> CONVERTERS_BY_NAME = ImmutableMap.of(
            "Ontograf", new OntografConverter(),
            "OWLViz", new OWLVizConverter());

    private final SimpleObjectProperty<Graph> graph = new SimpleObjectProperty<>();

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

    private final GraphImportFromDOTService importService = new GraphImportFromDOTService();

    private final FileChooser fileChooser = new FileChooser();

    private final GraphExportToImageService exportToImageService = new GraphExportToImageService();
    
    private final GraphExportToDOTService exportToDOTService = new GraphExportToDOTService();

    private final FileChooser imageFileChooser = new FileChooser();

    private final FileChooser ontologyFileChooser = new FileChooser();

    private Graph layoutGraph;

    private final GraphSaver graphSaver = new GraphSaver();

    private final SavedGraphImportService graphImporterService = new SavedGraphImportService();

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

    private final BooleanBinding graphExists = new BooleanBinding() {

        {
            super.bind(graph);
        }

        @Override
        protected boolean computeValue() {
            return graph.get() != null;
        }
    };

    public Graph getGraph() {
        return graph.get();
    }

    public void setGraph(Graph graph) {
        this.graph.set(graph);
    }

    public SimpleObjectProperty<Graph> graphProperty() {
        return graph;
    }

    public final boolean isGraphExists() {
        return graphExists.get();
    }

    public BooleanBinding graphExistsProperty() {
        return graphExists;
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
        ontologyFileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("OWL ontology", "*.owl")
        );
        ToggleGroup toggleGroup = new ToggleGroup();
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

    private Window getWindow() {
        return log.getScene().getWindow();
    }

    @FXML
    public void exportToImage() {
        Runnable exportAction = () -> {
            layoutGraph.setAttribute("ui.stylesheet", NODE_STYLESHEET);
            doExport(exportToImageService, imageFileChooser, layoutGraph);
        };
        if (layoutGraph == null) {
            chooseLayoutAndVisualize(getGraph(), g -> exportAction.run());
        } else {
            exportAction.run();
        }
    }


    public void exportGraph() {
        doExport(exportToDOTService, fileChooser, getGraph());
    }

    private void doExport(GraphExportService service, FileChooser chooser, Graph graph) {
        File file = chooser.showSaveDialog(getWindow());
        if (file == null) {
            return;
        }
        service.setFileName(file.getPath());
        service.setGraph(graph);
        service.setOnSucceeded(event ->
                new Alert(Alert.AlertType.INFORMATION, "Visualization successfully exported").show());
        service.setOnFailed(event ->
                new Alert(Alert.AlertType.ERROR, "Export failed").show());
        service.restart();
    }

    @FXML
    public void importGraph() {
        File file = fileChooser.showOpenDialog(getWindow());
        if (file == null) {
            return;
        }
        importService.setFileName(file.toString());
        importService.setOnSucceeded(event ->
                setGraph((Graph) event.getSource().getValue()));
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
    public void importOntologyFromFile() {
        File file = ontologyFileChooser.showOpenDialog(getWindow());
        if (file != null) {
            try {
                URL url = file.toURI().toURL();
                importOntology(url.toString());
            } catch (MalformedURLException e) {
                LOGGER.error("Unexpected import error", e);
            }
        }
    }

    @FXML
    public void importOntology() {
        importOntology(url.getText());
    }

    private ConversionSettings getConversionSettings(String graphMetricName) {
        if (chooseParametersButton.isSelected()) {
            return new ConversionSettings(null, graphMetricName);
        }
        OntologyToGraphConverter converter = converterChoiceBox.getValue();
        return new ConversionSettings(converter.toString(), null);
    }

    public void importOntology(String url) {
        layoutGraph = null;
        log.setText("");
        String graphMetricName = graphMetricChoiceBox.getSelectionModel().selectedItemProperty().getValue();
        ConversionSettings settings = getConversionSettings(graphMetricName);
        graphImporterService.setConversionSettings(settings);
        graphImporterService.setOntologyIri(url);
        graphImporterService.setOnSucceeded(event -> {
            Graph graph = (Graph) event.getSource().getValue();
            if (graph == null) {
                importOntology(url, graphMetricName, settings);
            } else {
                appendToLog("Loaded previously saved graph");
                setGraph(graph);
            }
        });
        graphImporterService.setOnFailed(event -> importOntology(url, graphMetricName, settings));
        graphImporterService.restart();
    }

    private void importOntology(String url, String graphMetricName, ConversionSettings settings) {
        GraphMetric graphMetric = GRAPH_METRICS_BY_NAME.get(graphMetricName);
        int degree = minDegree.getValue() == null ? 0 : minDegree.getValue();
        NodeRemovingGraphSimplifier simplifier = new NodeRemovingGraphSimplifier(degree);
        Collection<OntologyToGraphConverter> converters = new ArrayList<>(CONVERTERS_BY_NAME.values());
        OntologyLoaderService service = new OntologyLoaderService(url);
        service.setOnSucceeded(e -> {
            OWLOntology ontology = (OWLOntology) e.getSource().getValue();
            if (chooseParametersButton.isSelected()) {
                appendToLog("Choosing best graph representation with metric " + graphMetricName + "...");
                Task<EvaluatedGraph> task = new Task<EvaluatedGraph>() {
                    @Override
                    protected EvaluatedGraph call() {
                        return new GraphChooser(ontology, converters, simplifier, graphMetric).choose();
                    }
                };
                executorService.submit(task);
                task.setOnSucceeded(event -> {
                    EvaluatedGraph evaluatedGraph = (EvaluatedGraph) event.getSource().getValue();
                    logParameterChoice(evaluatedGraph);
                    Graph graph = evaluatedGraph.getGraph();
                    graphSaver.saveGraph(graph, url, settings);
                    setGraph(graph);
                });
            } else {
                OntologyToGraphConverter converter = converterChoiceBox.getValue();
                ConversionTask task = new ConversionTask(ontology, converter, simplifier);
                executorService.submit(task);
                task.setOnSucceeded(event -> {
                    Graph graph = (Graph) event.getSource().getValue();
                    setGraph(graph);
                    graphSaver.saveGraph(graph, url, settings);
                });
            }
        });
        service.setOnFailed(e -> log.setText(e.getSource().getException().getMessage()));
        service.start();
    }

    @FXML
    public void visualize() {
        if (getGraph() != null) {
            chooseLayoutAndVisualize(getGraph(), this::visualize);
        }
    }

    private void logParameterChoice(EvaluatedGraph evaluatedGraph) {
        Map<OntologyToGraphConverter, Double> metricValuesByConverters = evaluatedGraph.getMetricValuesByConverters();
        for (Map.Entry<OntologyToGraphConverter, Double> entry : metricValuesByConverters.entrySet()) {
            OntologyToGraphConverter converter = entry.getKey();
            appendToLog(String.format("Value of metric with converter %s: %.3f",
                    converter,
                    entry.getValue()));
        }
        OntologyToGraphConverter bestConverter = evaluatedGraph.getBestConverter();
        appendToLog("Chose " + bestConverter);
    }

    private void chooseLayoutAndVisualize(Graph graph, Consumer<Graph> action) {
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
                action.accept(layoutGraph);
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
            String name = evaluatedLayout.getLayoutName();
            String summary = evaluatedLayout.getVariants().entrySet()
                    .stream()
                    .map(metricNameToValue -> "Average value of metric for " + metricNameToValue.getKey() +
                                    ": " + metricNameToValue.getValue())
                    .collect(joining("\n", "", "\nChose " + name));
            appendToLog(summary);
            layoutGraph = evaluatedLayout.getBestLayout();
            action.accept(layoutGraph);
        });
        layoutChooserService.setOnFailed(workerStateEvent ->
                log.setText(workerStateEvent.getSource().getException().getMessage()));
    }

    private void visualize(Graph layoutGraph) {
        layoutGraph.setAttribute("ui.stylesheet", STYLESHEET);
        FxViewer fxViewer = new FxViewer(layoutGraph, Viewer.ThreadingModel.GRAPH_IN_GUI_THREAD);
        FxViewPanel panel = (FxViewPanel) fxViewer.addDefaultView(false);
        View view = fxViewer.getDefaultView();
        Camera camera = view.getCamera();

        Text text = new Text("");
        text.setWrappingWidth(180.);
        TextFlow textFlow = new TextFlow();
        panel.setMouseManager(new PanningMouseManager(panel, textFlow, camera, layoutGraph));
        panel.setOnScroll(event -> {
            double delta = 0.05;
            if (event.getDeltaY() > 0) {
                delta = -delta;
            }
            camera.setViewPercent(camera.getViewPercent() + delta);
        });

        Stage stage = new Stage();
        VBox textVBox = new VBox(textFlow);
        textVBox.setPadding(new Insets(10));
        Button hide = new Button("H");
        BorderPane borderPane = new BorderPane();
        borderPane.setLeft(panel);
        borderPane.setRight(textVBox);
        GridPane pane = new GridPane();
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(80);
        ColumnConstraints col3 = new ColumnConstraints();
        col3.setPercentWidth(20);
        pane.getColumnConstraints().addAll(col1, col3);
        RowConstraints rowConstraints = new RowConstraints();
        rowConstraints.setVgrow(Priority.ALWAYS);
        pane.getRowConstraints().addAll(rowConstraints);
        pane.add(panel, 0, 0);
        pane.add(textVBox, 1, 0);

        hide.setOnAction(event -> {
            textVBox.setVisible(!textVBox.isVisible());
            textVBox.setManaged(!textVBox.isManaged());
        });
        stage.setScene(new Scene(pane));
        stage.show();
    }

    private void appendToLog(String line) {
        log.setText(log.getText() + line + "\n");
    }

    static class ConversionTask extends Task<Graph> {
        private final OWLOntology ontology;
        private final OntologyToGraphConverter converter;
        private final GraphSimplifier simplifier;

        ConversionTask(OWLOntology ontology, OntologyToGraphConverter converter, GraphSimplifier simplifier) {
            this.ontology = ontology;
            this.converter = converter;
            this.simplifier = simplifier;
        }

        @Override
        protected Graph call() {
            MultiGraph multiGraph = converter.convert(ontology);
            simplifier.simplify(multiGraph);
            return multiGraph;
        }
    }
}
