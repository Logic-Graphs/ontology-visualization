import com.google.common.collect.ImmutableMap;
import com.google.common.math.Stats;
import javafx.application.Application;
import javafx.stage.Stage;
import org.golchin.ontology_visualization.*;
import org.golchin.ontology_visualization.metrics.*;
import org.golchin.ontology_visualization.metrics.layout.*;
import org.graphstream.graph.Graph;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class MetricCombinations extends Application {
    public static final NodeRemovingGraphSimplifier GRAPH_SIMPLIFIER = new NodeRemovingGraphSimplifier(0);
    private static final Map<String, String> ONTOLOGY_URLS_BY_ID =
            ImmutableMap.<String, String>builder()
                    .put("Dublin Core", "https://www.dublincore.org/specifications/dublin-core/dcmi-terms/dublin_core_terms.rdf")
                    .put("FOAF", "http://xmlns.com/foaf/spec/index.rdf")
//                    .put("SIOC", "http://rdfs.org/sioc/ns")
                    .put("SIOC", "file:///home/roman/Downloads/ns.rdf")
                    .put("Good Relations", "http://purl.org/goodrelations/v1.owl")
                    // fixme
//                    .put("The Music Ontology","http://purl.org/ontology/mo/")
                    .put("MarineTLO", "http://www.ics.forth.gr/isl/ontology/content-MTLO/marinetlo.owl")
                    .build();
    public static final List<OntologyToGraphConverter> CONVERTERS = Arrays.asList(new OWLVizConverter(), new OntografConverter());
    public static final String HEADER_METRIC = "Метрика";
    public static final String HEADER_CHOSEN_CONVERTER = "Выбранный способ построения графа";
    public static final String HEADER_METRIC_VALUE = "Значение метрики для ";
    public static final String HEADER_CHOSEN_LAYOUT = "Выбранный способ визуализации";
    public static final ImmutableMap<String, GraphMetric> GRAPH_METRICS = ImmutableMap.of(
            "Информационная метрика", new BaimuratovMetric(),
            "Графовая энтропия на основе разбиения по степени вершины", new DegreeEntropyMetric(),
            "Графовая энтропия Хосойи", new HosoyaEntropyMetric(),
            "Энергия матрицы смежности", new AdjacencyMatrixEnergy()
    );
    public static final ImmutableMap<String, LayoutMetric> LAYOUT_METRICS = ImmutableMap.of(
            "Стандартное отклонение длин ребер", new EdgeLengthStd(),
            "Минимальный угол между ребрами из одной вершины", new NodeAngleResolution(),
            "Минимальный угол при пересечении ребер", new CrossingAngleResolution(),
            "Количество пересечений ребер", new NumberOfCrossings(),
            "Мера сходства с графом формы", new ShapeGraphSimilarity(5)
    );

    public static void main(String[] args) throws IOException {
        launch(args);
    }

    private static EvaluatedLayout chooseLayout(EvaluatedGraph evaluatedGraph,
                                                LayoutMetric layoutMetric) {
        Graph graph = evaluatedGraph.getGraph();
        LayoutChooser layoutChooser = new LayoutChooser(graph, VisualizationController.POSSIBLE_LAYOUTS, 5, layoutMetric);
        return layoutChooser.chooseLayout();
    }

    private static String removeSpaces(String s) {
        return s.replace(" ", "");
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parameters parameters = getParameters();
        String formattedNTrials = parameters.getRaw().get(0);
        int nTrials = Integer.parseInt(formattedNTrials);
        for (Map.Entry<String, String> entry : ONTOLOGY_URLS_BY_ID.entrySet()) {
            String url = entry.getValue();
            System.out.println(url);
            String ontologyName = entry.getKey();
            Path ontologyDir = Files.createDirectories(Paths.get("experiments_out", ontologyName));
            try {
                OWLOntology ontology = OWLManager.createOWLOntologyManager().loadOntology(IRI.create(url));
                Set<EvaluatedGraph> chosenGraphs = new HashSet<>();
                List<String> header = createHeaderForConvertersTable();
                Table convertersTable = new Table(removeSpaces(ontologyName) + "_converter_choice",
                        "Выбор способа построения графа для онтологии " + ontologyName,
                        header,
                        GRAPH_METRICS.size());
                int rowIndex = 0;
                for (Map.Entry<String, GraphMetric> namedGraphMetric : ((Map<String, GraphMetric>) GRAPH_METRICS).entrySet()) {
                    String metricName = namedGraphMetric.getKey();
                    convertersTable.setValue(HEADER_METRIC, rowIndex, metricName);
                    System.out.println("Graph metric: " + metricName);
                    GraphChooser graphChooser = new GraphChooser(ontology, CONVERTERS, GRAPH_SIMPLIFIER, namedGraphMetric.getValue());
                    EvaluatedGraph evaluatedGraph = graphChooser.choose();
                    OntologyToGraphConverter bestConverter = evaluatedGraph.getBestConverter();
                    System.out.println("Chosen converter: " + bestConverter);
                    chosenGraphs.add(evaluatedGraph);
                    for (Map.Entry<OntologyToGraphConverter, Double> converterToMetric : evaluatedGraph.getMetricValuesByConverters().entrySet()) {
                        String formattedValue = String.format("%.3f", converterToMetric.getValue());
                        convertersTable.setValue(HEADER_METRIC_VALUE + converterToMetric.getKey(), rowIndex, formattedValue);
                    }
                    convertersTable.setValue(HEADER_CHOSEN_CONVERTER, rowIndex, String.valueOf(bestConverter));
                    rowIndex++;
                }
                convertersTable.writeToCsv(ontologyDir.resolve("converters.csv"));
                convertersTable.writeToLatex(ontologyDir.resolve("converters.tex"));
                for (EvaluatedGraph evaluatedGraph : chosenGraphs) {
                    OntologyToGraphConverter bestConverter = evaluatedGraph.getBestConverter();
                    experimentWithLayoutMetrics(nTrials, LAYOUT_METRICS, ontologyName, ontologyDir, evaluatedGraph, bestConverter);
                }
            } catch (OWLOntologyCreationException e) {
                e.printStackTrace();
            }
        }
    }

    private void experimentWithLayoutMetrics(int nTrials,
                                             Map<String, LayoutMetric> layoutMetrics,
                                             String ontologyName,
                                             Path ontologyDir,
                                             EvaluatedGraph evaluatedGraph,
                                             OntologyToGraphConverter bestConverter) throws IOException {
        List<String> layoutTableHeader = new ArrayList<>();
        layoutTableHeader.add(HEADER_METRIC);
        for (String layout : VisualizationController.POSSIBLE_LAYOUTS_BY_NAME.keySet()) {
            layoutTableHeader.add(HEADER_METRIC_VALUE + layout);
        }
        layoutTableHeader.add(HEADER_CHOSEN_LAYOUT);
        Table layoutChoiceTable = new Table(removeSpaces(ontologyName) + "_" + bestConverter,
                "Выбор способа визуализации для способа построения " + bestConverter,
                layoutTableHeader,
                layoutMetrics.size());
        int layoutRowIndex = 0;
        Map<String, List<LayoutVariant>> bestLayoutVariantsByConverter = new HashMap<>();
        for (Map.Entry<String, LayoutMetric> namedLayoutMetric : layoutMetrics.entrySet()) {
            String layoutMetricName = namedLayoutMetric.getKey();
            System.out.println("Layout metric: " + layoutMetricName);
            List<EvaluatedLayout> layoutMetricTrials = new ArrayList<>();
            for (int i = 0; i < nTrials; i++) {
                EvaluatedLayout evaluatedLayout = chooseLayout(evaluatedGraph, namedLayoutMetric.getValue());
                layoutMetricTrials.add(evaluatedLayout);
            }
            LayoutMetricExperiment experiment = new LayoutMetricExperiment(namedLayoutMetric.getValue(), layoutMetricTrials);
            for (Map.Entry<String, Stats> e : experiment.getStatsByLayout().entrySet()) {
                String layout = e.getKey();
                Stats stats = e.getValue();
                String formattedMetricValue = String.format("%.3f ± %.3f", stats.mean(), stats.populationVariance());
                layoutChoiceTable.setValue(HEADER_METRIC_VALUE + layout, layoutRowIndex, formattedMetricValue);
            }
            layoutChoiceTable.setValue(HEADER_METRIC, layoutRowIndex, layoutMetricName);
            layoutChoiceTable.setValue(HEADER_CHOSEN_LAYOUT, layoutRowIndex, experiment.getBestLayoutName());
            String bestLayoutName = experiment.getBestLayoutName();
            bestLayoutVariantsByConverter.computeIfAbsent(bestConverter.toString(), c -> new ArrayList<>())
                    .add(experiment.getBestVariantByLayout().get(bestLayoutName));
            layoutRowIndex++;
        }
        bestLayoutVariantsByConverter.forEach((converterName, layoutVariants) -> {
            for (LayoutVariant layoutVariant : layoutVariants) {
                GraphExportToImageService exportToImageService = new GraphExportToImageService();
                exportToImageService.setGraph(layoutVariant.getLayout());
                exportToImageService.start();
                String layoutName = layoutVariant.getLayoutName();
                String fileName = String.format("img_%s_%s.png", bestConverter, layoutName);
                exportToImageService.setFileName(ontologyDir.resolve(fileName).toString());
            }
        });
        String layoutChoiceFileName = bestConverter + "_layouts";
        layoutChoiceTable.writeToCsv(ontologyDir.resolve(layoutChoiceFileName + ".csv"));
        layoutChoiceTable.writeToLatex(ontologyDir.resolve(layoutChoiceFileName + ".tex"));
    }

    private List<String> createHeaderForConvertersTable() {
        List<String> header = new ArrayList<>();
        header.add(HEADER_METRIC);
        for (OntologyToGraphConverter converter : CONVERTERS) {
            header.add(HEADER_METRIC_VALUE + converter);
        }
        header.add(HEADER_CHOSEN_CONVERTER);
        return header;
    }
}
