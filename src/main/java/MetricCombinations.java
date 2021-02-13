import com.google.common.collect.ImmutableMap;
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
        Map<String, GraphMetric> graphMetrics = ImmutableMap.of(
                "Информационная метрика", new BaimuratovMetric(),
                "Графовая энтропия на основе разбиения по степени вершины", new DegreeEntropyMetric(),
                "Графовая энтропия Хосойи", new HosoyaEntropyMetric(),
                "Энергия матрицы смежности", new AdjacencyMatrixEnergy()
        );
        Map<String, LayoutMetric> layoutMetrics = ImmutableMap.of(
                "Стандартное отклонение длин ребер", new EdgeLengthStd(),
                "Минимальный угол между ребрами из одной вершины", new NodeAngleResolution(),
                "Минимальный угол при пересечении ребер", new CrossingAngleResolution(),
                "Количество пересечений ребер", new NumberOfCrossings(),
                "Мера сходства с графом формы", new ShapeGraphSimilarity(5)
        );
        for (Map.Entry<String, String> entry : ONTOLOGY_URLS_BY_ID.entrySet()) {
            String url = entry.getValue();
            System.out.println(url);
            String ontologyName = entry.getKey();
            Files.createDirectories(Paths.get(ontologyName));
            try {
                OWLOntology ontology = OWLManager.createOWLOntologyManager().loadOntology(IRI.create(url));
                Set<EvaluatedGraph> chosenGraphs = new HashSet<>();
                for (Map.Entry<String, GraphMetric> graphMetric : graphMetrics.entrySet()) {
                    System.out.println("Graph metric: " + graphMetric.getKey());
                    GraphChooser graphChooser = new GraphChooser(ontology, CONVERTERS, GRAPH_SIMPLIFIER, graphMetric.getValue());
                    EvaluatedGraph evaluatedGraph = graphChooser.choose();
                    OntologyToGraphConverter bestConverter = evaluatedGraph.getBestConverter();
                    System.out.println("Chosen converter: " + bestConverter);
                    chosenGraphs.add(evaluatedGraph);
                }
                for (EvaluatedGraph evaluatedGraph : chosenGraphs) {
                    OntologyToGraphConverter bestConverter = evaluatedGraph.getBestConverter();
                    for (Map.Entry<String, LayoutMetric> namedLayoutMetric : layoutMetrics.entrySet()) {
                        String layoutMetricName = namedLayoutMetric.getKey();
                        System.out.println("Layout metric: " + layoutMetricName);
                        List<EvaluatedLayout> layoutMetricTrials = new ArrayList<>();
                        for (int i = 0; i < nTrials; i++) {
                            EvaluatedLayout evaluatedLayout = chooseLayout(evaluatedGraph, namedLayoutMetric.getValue());
                            GraphExportToImageService exportToImageService = new GraphExportToImageService();
                            exportToImageService.setGraph(evaluatedGraph.getGraph());
                            exportToImageService.start();
                            String layoutName = evaluatedLayout.getLayoutName();
                            String fileName = String.format("img_%s_%s.png", bestConverter, layoutName);
                            exportToImageService.setFileName(Paths.get(ontologyName, fileName).toString());
                            layoutMetricTrials.add(evaluatedLayout);
                        }
                        LayoutMetricExperiment experiment = new LayoutMetricExperiment(namedLayoutMetric.getValue(), layoutMetricTrials);
                        System.out.println(experiment);
                    }
                }
            } catch (OWLOntologyCreationException e) {
                e.printStackTrace();
            }
        }
    }
}
