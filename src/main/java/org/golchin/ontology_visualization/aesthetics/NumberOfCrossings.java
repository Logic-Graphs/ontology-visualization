package org.golchin.ontology_visualization.aesthetics;

import java.awt.geom.Line2D;

public class NumberOfCrossings extends CrossingsAesthetics {
    @Override
    protected double getInitialValue() {
        return 0;
    }

    @Override
    protected double reduce(double curValue, Line2D firstLine, Line2D secondLine) {
        return curValue + 1;
    }
}
