import com.google.common.collect.ImmutableMap;
import javafx.application.Application;
import javafx.stage.Stage;
import org.apache.log4j.Logger;
import org.golchin.ontology_visualization.*;
import org.golchin.ontology_visualization.metrics.BaimuratovMetric;
import org.golchin.ontology_visualization.metrics.DegreeEntropyMetric;
import org.golchin.ontology_visualization.metrics.GraphMetric;
import org.golchin.ontology_visualization.metrics.layout.LayoutMetric;
import org.graphstream.graph.Graph;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MetricCombinations extends Application {
    static Map<String, String> urlById =
            ImmutableMap.of(
                    "foaf", "http://xmlns.com/foaf/spec/index.rdf",
                    "DOCO", "https://svn.code.sf.net/p/sempublishing/code/DoCO/2014-05-14_doco-1_2.owl",
//                    "SIOC", "http://rdfs.org/sioc/ns",
                    "goodRelations", "http://purl.org/goodrelations/v1.owl",
                    "OntoViBe", "http://ontovibe.visualdataweb.org/",
                    "Personas", "http://blankdots.com/open/personasonto.owl");
    static Logger logger = Logger.getLogger(MetricCombinations.class);

    public static void main(String[] args) throws IOException {
        launch(args);
    }

    private static void visualize(OWLOntology ontology, String id, GraphMetric graphMetric, LayoutMetric layoutMetric, List<String> lines) {
        Collection<? extends OntologyToGraphConverter> converters =
                VisualizationController.getConvertersWithParameterCombinations(VisualizationController.PARAMETERS, 0);
        EvaluatedGraph evaluatedGraph = new GraphChooser(ontology, converters, graphMetric).choose();
        Graph graph = evaluatedGraph.getGraph();
        EvaluatedLayout evaluatedLayout = new LayoutChooser(graph, VisualizationController.POSSIBLE_LAYOUTS, 5, layoutMetric)
                .chooseLayout();
        Graph layoutGraph = evaluatedLayout.getBestLayout();
        String graphMetricName = graphMetric.getClass().getSimpleName();
        String layoutMetricName = layoutMetric.getClass().getSimpleName();
        String parameterValues = evaluatedGraph.getBestParameters().values().stream().map(Object::toString).collect(Collectors.joining(","));
        String parameterNames = evaluatedGraph.getBestParameters().keySet().stream().map(Object::toString).collect(Collectors.joining(","));
        String line = String.format("%s,%s,%s,%s,%s", id, graphMetricName, layoutMetricName,
                parameterValues, evaluatedLayout.getName());
        String header = "id,graphMetricName,layoutMetricName," + parameterNames + ",layoutAlgorithm";
        if (lines.isEmpty())
            lines.add(header);
        lines.add(line);
        GraphExportToImageService exportToImageService = new GraphExportToImageService();
        exportToImageService.start();
        exportToImageService.setGraph(layoutGraph);
        String fileName = String.format("img_%s_%s.png", graphMetricName, layoutMetricName);
        exportToImageService.setFileName(Paths.get(id, fileName).toString());
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, String> entry : urlById.entrySet()) {
            String url = entry.getValue();
            System.out.println(url);
            Files.createDirectories(Paths.get(entry.getKey()));
            try {
                OWLOntology ontology = OWLManager.createOWLOntologyManager().loadOntology(IRI.create(url));
                Stream.of(new BaimuratovMetric(), new DegreeEntropyMetric())
                        .forEach(graphMetric -> {
                            for (LayoutMetric layoutMetric : VisualizationController.METRICS_BY_NAME.values()) {
                                visualize(ontology, entry.getKey(), graphMetric, layoutMetric, lines);
                            }
                        });
            } catch (OWLOntologyCreationException e) {
                e.printStackTrace();
            }
        }
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get("dump.csv"))) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        }
    }
}
