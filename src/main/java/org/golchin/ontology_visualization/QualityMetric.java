package org.golchin.ontology_visualization;

import java.util.Comparator;

public interface QualityMetric {
    /**
     * @return comparator that tells what value is better,
     * typically {@code Comparator.naturalOrder()} or {@code Comparator.naturalOrder().reversed()}
     */
    Comparator<Double> getComparator();
}
