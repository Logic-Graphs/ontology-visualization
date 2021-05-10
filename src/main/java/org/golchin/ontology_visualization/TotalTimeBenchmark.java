package org.golchin.ontology_visualization;

import com.google.common.collect.ImmutableMap;
import org.golchin.ontology_visualization.metrics.GraphMetric;
import org.golchin.ontology_visualization.metrics.layout.LayoutMetric;
import org.graphstream.ui.javafx.util.FxFileSinkImages;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
public class TotalTimeBenchmark {
    public static final NodeRemovingGraphSimplifier GRAPH_SIMPLIFIER = new NodeRemovingGraphSimplifier(0);
    private static final Map<String, String> ONTOLOGY_URLS_BY_ID = MetricCombinations.ONTOLOGY_URLS_BY_ID;

    private static OWLOntology loadOntology(String url) {
        IRI iri = IRI.create(url);
        try {
            return OWLManager.createOWLOntologyManager().loadOntology(iri);
        } catch (OWLOntologyCreationException e) {
            throw new RuntimeException(e);
        }
    }

    public static final List<OntologyToGraphConverter> CONVERTERS = MetricCombinations.CONVERTERS;
    public static final ImmutableMap<String, GraphMetric> GRAPH_METRICS = MetricCombinations.GRAPH_METRICS;
    public static final ImmutableMap<String, LayoutMetric> LAYOUT_METRICS = MetricCombinations.LAYOUT_METRICS;
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
        Options options = new OptionsBuilder()
                .param("ontologyId", ONTOLOGY_URLS_BY_ID.keySet().toArray(new String[0]))
                .param("graphMetricName", GRAPH_METRICS.keySet().toArray(new String[0]))
                .param("layoutMetricName", LAYOUT_METRICS.keySet().toArray(new String[0]))
                .resultFormat(ResultFormatType.CSV)
                .result("jmh-result-total_" + LocalDateTime.now() + ".csv")
                .warmupTime(TimeValue.seconds(5))
                .measurementTime(TimeValue.seconds(5))
                .timeUnit(TimeUnit.SECONDS)
                .build();
        new Runner(options).run();
    }
}
