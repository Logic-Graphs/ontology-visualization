package org.golchin.ontology_visualization;

import com.google.common.collect.ImmutableMap;
import org.golchin.ontology_visualization.metrics.*;
import org.golchin.ontology_visualization.metrics.layout.*;
import org.graphstream.ui.javafx.util.FxFileSinkImages;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
public class GraphMetricsBenchmark {
    public static final NodeRemovingGraphSimplifier GRAPH_SIMPLIFIER = new NodeRemovingGraphSimplifier(0);
    private static final Map<String, String> ONTOLOGY_URLS_BY_ID =
            ImmutableMap.<String, String>builder()
                    .put("Dublin Core", "file:///home/roman/Downloads/dublin_core_terms.rdf")
                    .put("FOAF", "file:///home/roman/Downloads/index.rdf")
                    .put("SIOC", "file:///home/roman/Downloads/ns.rdf")
                    .put("Good Relations", "file:///home/roman/Downloads/v1.owl")
                    .put("MarineTLO", "file:///home/roman/Downloads/marinetlo.owl")
                    .build();

    private static OWLOntology loadOntology(String url) {
        IRI iri = IRI.create(url);
        try {
            return OWLManager.createOWLOntologyManager().loadOntology(iri);
        } catch (OWLOntologyCreationException e) {
            throw new RuntimeException(e);
        }
    }

    public static final List<OntologyToGraphConverter> CONVERTERS = Arrays.asList(new OWLVizConverter(), new OntografConverter());
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
    @Param({"Графовая энтропия Хосойи"})
    private static String graphMetricName;

    @Param({"Стандартное отклонение длин ребер"})
    private static String layoutMetricName;

    @Param("FOAF")
    private static String ontologyId;

    @State(Scope.Benchmark)
    public static class BenchmarkState {
        private String ontologyUrl;
        private GraphMetric graphMetric;
        private LayoutMetric layoutMetric;

        static {
            Logger root = Logger.getLogger("");
            root.setLevel(java.util.logging.Level.SEVERE);
        }

        @Setup(Level.Trial)
        public void setUp() {
            ontologyUrl = ONTOLOGY_URLS_BY_ID.get(ontologyId);
            graphMetric = GRAPH_METRICS.get(graphMetricName);
            layoutMetric = LAYOUT_METRICS.get(layoutMetricName);
        }

    }

    @Benchmark
    public void chooseConverter(BenchmarkState state) throws IOException {
        OWLOntology ontology = loadOntology(state.ontologyUrl);
        GraphChooser graphChooser = new GraphChooser(ontology, CONVERTERS, GRAPH_SIMPLIFIER, state.graphMetric);
        EvaluatedGraph evaluatedGraph = graphChooser.choose();
        LayoutChooser layoutChooser = new LayoutChooser(evaluatedGraph.getGraph(), VisualizationController.POSSIBLE_LAYOUTS, 20, state.layoutMetric);
        EvaluatedLayout evaluatedLayout = layoutChooser.chooseLayout();
        FxFileSinkImages sink = new FxFileSinkImages();
        sink.writeAll(evaluatedLayout.getBestLayout(), "layout.png");
    }

    public static void main(String[] args) throws RunnerException, FileNotFoundException {
        try (PrintStream out = new PrintStream("perf_" + LocalDateTime.now() + ".txt")) {
            System.setOut(out);
            Options options = new OptionsBuilder()
                    .param("ontologyId", ONTOLOGY_URLS_BY_ID.keySet().toArray(new String[0]))
                    .param("graphMetricName", GRAPH_METRICS.keySet().toArray(new String[0]))
                    .param("layoutMetricName", LAYOUT_METRICS.keySet().toArray(new String[0]))
                    .build();
            new Runner(options).run();
        }
    }
}
