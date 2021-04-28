package org.golchin.ontology_visualization;

import lombok.Getter;
import org.graphstream.graph.Graph;

import java.util.Map;

@Getter
public class EvaluatedLayout {
    private final String layoutName;
    private final Graph bestLayout;
    private final double bestAesthetics;
    private final double averageAesthetics;
    private final Map<String, LayoutVariant> variants;
    private final LayoutVariant bestVariant;

    public EvaluatedLayout(Map<String, LayoutVariant> variants, LayoutVariant bestVariant) {
        this.variants = variants;
        this.bestVariant = bestVariant;
        layoutName = bestVariant.getLayoutName();
        bestLayout = bestVariant.getLayout();
        bestAesthetics = bestVariant.getBestMetricValue();
        averageAesthetics = bestVariant.getAverageMetricValue();
    }
}
