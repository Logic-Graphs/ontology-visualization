import com.google.common.collect.ImmutableMap;
import com.google.common.math.Stats;
import com.google.common.math.StatsAccumulator;
import javafx.application.Platform;
import javafx.util.Pair;
import org.golchin.ontology_visualization.*;
import org.golchin.ontology_visualization.metrics.*;
import org.golchin.ontology_visualization.metrics.layout.*;
import org.graphstream.graph.Graph;
import org.graphstream.ui.javafx.util.FxFileSinkImages;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@SuppressWarnings("UnstableApiUsage")
public class MetricCombinations {
    public static final NodeRemovingGraphSimplifier GRAPH_SIMPLIFIER = new NodeRemovingGraphSimplifier(0);
    private static final Map<String, String> ONTOLOGY_URLS_BY_ID =
            ImmutableMap.<String, String>builder()
                    .put("Dublin Core", "file:///home/roman/Downloads/dublin_core_terms.rdf")
                    .put("FOAF", "file:///home/roman/Downloads/index.rdf")
                    .put("SIOC", "file:///home/roman/Downloads/ns.rdf")
                    .put("Good Relations", "file:///home/roman/Downloads/v1.owl")
                    .put("MarineTLO", "file:///home/roman/Downloads/marinetlo.owl")
                    .build();
    public static final List<OntologyToGraphConverter> CONVERTERS =
            Arrays.asList(new OWLVizConverter(), new OntografConverter());
    public static final String HEADER_METRIC = "Метрика";
    public static final String HEADER_CHOSEN_CONVERTER = "Выбранный способ построения графа";
    public static final String HEADER_METRIC_VALUE = "Значение метрики для ";
    public static final String HEADER_CHOSEN_LAYOUT = "Выбранный алгоритм визуализации";
    public static final ImmutableMap<String, GraphMetric> GRAPH_METRICS = ImmutableMap.of(
            "Информационная метрика", new BaimuratovMetric(),
            "Графовая энтропия на основе разбиения по степени вершины", new DegreeEntropyMetric(),
            "Графовая энтропия Хосойи", new HosoyaEntropyMetric(),
            "Энергия матрицы смежности", new AdjacencyMatrixEnergy()
    );
    public static final ImmutableMap<String, LayoutMetric> LAYOUT_METRICS = ImmutableMap.<String, LayoutMetric>builder()
            .put("Стандартное отклонение длин ребер", new EdgeLengthStd())
            .put("Минимальный угол между ребрами из одной вершины", new NodeAngleResolution())
            .put("Минимальный угол при пересечении ребер", new CrossingAngleResolution())
            .put("Количество пересечений ребер", new NumberOfCrossings())
            .put("Мера сходства с графом формы", new ShapeGraphSimilarity(5))
            .put("Неравномерность распределения вершин", new NodeNonUniformity())
            .build();
    public static final String STATS_CHART_TIME_LABEL = "Время вычисления, мс";
    private static final List<String> CONVERTERS_TABLE_HEADER = createConvertersTableHeader();
    private static final List<String> LAYOUTS_TABLE_HEADER = createLayoutsTableHeader();

    private static EvaluatedLayout chooseLayout(EvaluatedGraph evaluatedGraph,
                                                LayoutMetric layoutMetric) {
        Graph graph = evaluatedGraph.getGraph();
        LayoutChooser layoutChooser = new LayoutChooser(graph,
                VisualizationController.POSSIBLE_LAYOUTS,
                20,
                5,
                layoutMetric,
                0.05);
        return layoutChooser.chooseLayout();
    }

    private static String removeSpaces(String s) {
        return s.replace(" ", "");
    }

    public static void main(String[] args) throws IOException {
        List<String> latexLines = new ArrayList<>();
        int nTrials = Integer.parseInt(args[0]);
        for (Map.Entry<String, String> entry : ONTOLOGY_URLS_BY_ID.entrySet()) {
            String url = entry.getValue();
            System.out.println(url);
            String ontologyName = entry.getKey();
            Path ontologyDir = Files.createDirectories(Paths.get("experiments_out", ontologyName));
            try {
                Map<String, StatsAccumulator> timeStatsByGraphMetric = new HashMap<>();
                Map<String, StatsAccumulator> timeStatsByLayoutMetric = new HashMap<>();
                IRI ontologyIRI = IRI.create(url);
                OWLOntology ontology = OWLManager.createOWLOntologyManager().loadOntology(ontologyIRI);
                Table convertersTable = new Table(removeSpaces(ontologyName) + "_converter_choice",
                        "Выбор способа построения графа для онтологии " + ontologyName,
                        CONVERTERS_TABLE_HEADER,
                        GRAPH_METRICS.size());
                Map<String, Map<String, List<EvaluatedLayout>>> resultsByMetrics = new HashMap<>();
                Map<String, OntologyToGraphConverter> bestConvertersByMetric = new HashMap<>();
                for (int i = 0; i < nTrials; i++) {
                    doTrial(timeStatsByGraphMetric,
                            timeStatsByLayoutMetric,
                            ontology,
                            convertersTable,
                            resultsByMetrics,
                            bestConvertersByMetric,
                            ontologyDir);
                }
                convertersTable.writeToCsv(ontologyDir.resolve("converters.csv"));
                convertersTable.writeToLatex(ontologyDir.resolve("converters.tex"));

                writeResultsToTables(ontologyName, ontologyDir, resultsByMetrics, bestConvertersByMetric);
                writeTimeStats(ontologyDir, timeStatsByGraphMetric, timeStatsByLayoutMetric);
            } catch (OWLOntologyCreationException e) {
                e.printStackTrace();
            }
            Files.list(ontologyDir)
                    .filter(path -> path.getFileName().toString().endsWith(".tex"))
                    .flatMap(path -> {
                        try {
                            return Files.lines(path);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .forEach(latexLines::add);
        }
        Platform.exit();
        Path tablesFile = Paths.get("all_tables.tex");
        try (BufferedWriter writer = Files.newBufferedWriter(tablesFile)) {
            for (String line : latexLines) {
                writer.write(line);
                writer.newLine();
            }
        }
    }

    private static void writeTimeStats(Path ontologyDir,
                                       Map<String, StatsAccumulator> timeStatsByGraphMetric,
                                       Map<String, StatsAccumulator> timeStatsByLayoutMetric) throws IOException {
        System.out.println("time stats");
        timeStatsByGraphMetric.forEach((metric, stats) ->
                System.out.println(metric + " " + stats.mean() + " ± " + stats.populationStandardDeviation()));
        TimeStatsVisualizer visualizer = new TimeStatsVisualizer(timeStatsByGraphMetric, STATS_CHART_TIME_LABEL);
        visualizer.writeToFile(ontologyDir.resolve("graph_metrics_time_chart.png"), 800, 600);
        System.out.println("layout time stats");
        timeStatsByLayoutMetric.forEach((metric, stats) ->
                System.out.println(metric + " " + stats.mean() + " ± " + stats.populationStandardDeviation()));
        Path chartPath = ontologyDir.resolve("layout_metrics_time_chart.png");
        TimeStatsVisualizer layoutStatsVisualizer = new TimeStatsVisualizer(timeStatsByLayoutMetric, STATS_CHART_TIME_LABEL);
        layoutStatsVisualizer.writeToFile(chartPath, 800, 600);
    }

    private static void doTrial(Map<String, StatsAccumulator> timeStatsByGraphMetric, Map<String, StatsAccumulator> timeStatsByLayoutMetric, OWLOntology ontology, Table convertersTable, Map<String, Map<String, List<EvaluatedLayout>>> experimentsByMetricCombinations, Map<String, OntologyToGraphConverter> bestConvertersByMetric, Path ontologyDir) throws IOException {
        int rowIndex = 0;
        for (Map.Entry<String, GraphMetric> namedGraphMetric : GRAPH_METRICS.entrySet()) {
            String graphMetricName = namedGraphMetric.getKey();
            convertersTable.setValue(HEADER_METRIC, rowIndex, graphMetricName);
            System.out.println("Graph metric: " + graphMetricName);
            GraphChooser graphChooser = new GraphChooser(ontology, CONVERTERS, GRAPH_SIMPLIFIER, namedGraphMetric.getValue());
            Pair<EvaluatedGraph, Double> graphWithTime = Util.measureTimeMillis(graphChooser::choose);
            EvaluatedGraph evaluatedGraph = graphWithTime.getKey();
            timeStatsByGraphMetric.computeIfAbsent(graphMetricName, n -> new StatsAccumulator())
                    .add(graphWithTime.getValue());
            OntologyToGraphConverter bestConverter = evaluatedGraph.getBestConverter();
            bestConvertersByMetric.put(graphMetricName, bestConverter);
            System.out.println("Chosen converter: " + bestConverter);
            for (Map.Entry<OntologyToGraphConverter, Double> converterToMetric : evaluatedGraph.getMetricValuesByConverters().entrySet()) {
                String formattedValue = formatDouble(converterToMetric.getValue());
                convertersTable.setValue(HEADER_METRIC_VALUE + converterToMetric.getKey(), rowIndex, formattedValue);
            }
            convertersTable.setValue(HEADER_CHOSEN_CONVERTER, rowIndex, String.valueOf(bestConverter));
            for (Map.Entry<String, LayoutMetric> namedLayoutMetric : LAYOUT_METRICS.entrySet()) {
                String layoutMetricName = namedLayoutMetric.getKey();
                System.out.println("Layout metric: " + layoutMetricName);
                LayoutMetric layoutMetric = namedLayoutMetric.getValue();
                Pair<EvaluatedLayout, Double> layoutWithTime =
                        Util.measureTimeMillis(() -> chooseLayout(evaluatedGraph, layoutMetric));
                EvaluatedLayout evaluatedLayout = layoutWithTime.getKey();
                experimentsByMetricCombinations.computeIfAbsent(graphMetricName, m -> new HashMap<>())
                        .computeIfAbsent(layoutMetricName, m -> new ArrayList<>())
                        .add(evaluatedLayout);
                timeStatsByLayoutMetric.computeIfAbsent(layoutMetricName, m -> new StatsAccumulator())
                        .add(layoutWithTime.getValue());
                String ontologyName = ontologyDir.getFileName().toString();
                String fileName = String.format("%s_%s_%s.png", ontologyName, bestConverter, evaluatedLayout.getLayoutName());
                Graph graph = layoutWithTime.getKey().getBestLayout();
                graph.setAttribute("ui.stylesheet", VisualizationController.GRAPH_STYLESHEET);
                String filePath = ontologyDir.resolve(fileName).toString();
                new FxFileSinkImages().writeAll(graph, filePath);
            }
            rowIndex++;
        }
    }

    private static void writeResultsToTables(String ontologyName,
                                             Path ontologyDir,
                                             Map<String, Map<String, List<EvaluatedLayout>>> resultsByMetrics,
                                             Map<String, OntologyToGraphConverter> bestConvertersByMetric) throws IOException {
        for (Map.Entry<String, Map<String, List<EvaluatedLayout>>> graphMetricEntry : resultsByMetrics.entrySet()) {
            Map<String, List<EvaluatedLayout>> experimentsByLayoutMetrics = graphMetricEntry.getValue();
            OntologyToGraphConverter bestConverter = bestConvertersByMetric.get(graphMetricEntry.getKey());
            Table layoutChoiceTable = new Table(removeSpaces(ontologyName) + "_" + bestConverter,
                    "Выбор алгоритма визуализации " + ontologyName + " для способа построения " + bestConverter,
                    LAYOUTS_TABLE_HEADER,
                    LAYOUT_METRICS.size());
            int layoutRowIndex = 0;
            for (Map.Entry<String, List<EvaluatedLayout>> layoutMetricToTrials : experimentsByLayoutMetrics.entrySet()) {
                String layoutMetricName = layoutMetricToTrials.getKey();
                List<EvaluatedLayout> layoutMetricTrials = layoutMetricToTrials.getValue();
                LayoutMetricExperiment experiment =
                        new LayoutMetricExperiment(LAYOUT_METRICS.get(layoutMetricName), layoutMetricTrials);
                for (Map.Entry<String, Stats> e : experiment.getStatsByLayout().entrySet()) {
                    String layout = e.getKey();
                    Stats stats = e.getValue();
                    double standardDeviation = stats.populationStandardDeviation();
                    String formattedMetricValue;
                    double mean = stats.mean();
                    String formattedMean = formatDouble(mean);
                    if (standardDeviation > 0) {
                        formattedMetricValue = formattedMean + " \\pm " + formatDouble(standardDeviation);
                    } else {
                        formattedMetricValue = formattedMean;
                    }
                    layoutChoiceTable.setValue(HEADER_METRIC_VALUE + layout, layoutRowIndex, "$ " + formattedMetricValue + " $");
                }
                layoutChoiceTable.setValue(HEADER_METRIC, layoutRowIndex, layoutMetricName);
                layoutChoiceTable.setValue(HEADER_CHOSEN_LAYOUT, layoutRowIndex, experiment.getBestLayoutName());
                layoutRowIndex++;
            }
            String layoutChoiceFileName = bestConverter + "_layouts";
            layoutChoiceTable.writeToCsv(ontologyDir.resolve(layoutChoiceFileName + ".csv"));
            layoutChoiceTable.writeToLatex(ontologyDir.resolve(layoutChoiceFileName + ".tex"));
        }
    }

    static String latexScientificNotation(Number number) {
        String result = number.toString();
        int eIndex = result.indexOf("E");
        if (eIndex < 0) {
            return result;
        }
        String coefficient = result.substring(0, eIndex);
        return coefficient + " \\times 10^{" + result.substring(eIndex + 1) + "}";
    }

    private static String formatDouble(double d) {
        double rounded = roundDoubleUpToNPlaces(d, 3);
        return latexScientificNotation(rounded);
    }

    private static double roundDoubleUpToNPlaces(double d, int n) {
        n -= Math.floor(Math.log10(d)) + 1;
        double power = Math.pow(10, n);
        return (double) Math.round(d * power) / power;
    }

    private static List<String> createLayoutsTableHeader() {
        List<String> layoutTableHeader = new ArrayList<>();
        layoutTableHeader.add(HEADER_METRIC);
        for (String layout : VisualizationController.POSSIBLE_LAYOUTS_BY_NAME.keySet()) {
            layoutTableHeader.add(HEADER_METRIC_VALUE + layout);
        }
        layoutTableHeader.add(HEADER_CHOSEN_LAYOUT);
        return layoutTableHeader;
    }

    private static List<String> createConvertersTableHeader() {
        List<String> header = new ArrayList<>();
        header.add(HEADER_METRIC);
        for (OntologyToGraphConverter converter : CONVERTERS) {
            header.add(HEADER_METRIC_VALUE + converter);
        }
        header.add(HEADER_CHOSEN_CONVERTER);
        return header;
    }
}
