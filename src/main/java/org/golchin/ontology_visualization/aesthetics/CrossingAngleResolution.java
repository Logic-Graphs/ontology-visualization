package org.golchin.ontology_visualization.aesthetics;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.Comparator;

public class CrossingAngleResolution extends CrossingsAesthetics {

    static double scalarProduct(Point2D v1, Point2D v2) {
        double len1 = v1.distance(0, 0);
        double len2 = v2.distance(0, 0);
        return (v1.getX() * v2.getX() + v1.getY() * v2.getY()) / len1 / len2;
    }

    @Override
    protected double getInitialValue() {
        return Double.POSITIVE_INFINITY;
    }

    @Override
    protected double reduce(double curValue, Line2D firstLine, Line2D secondLine) {
        Point2D v1 = new Point2D.Double(firstLine.getX2() - firstLine.getX1(), firstLine.getY2() - firstLine.getY1());
        Point2D v2 = new Point2D.Double(secondLine.getX2() - secondLine.getX1(), secondLine.getY2() - secondLine.getY1());
        double acos = Math.acos(Math.abs(scalarProduct(v1, v2)));
        return Math.min(curValue, acos);
    }

    @Override
    public Comparator<Double> getComparator() {
        return Comparator.naturalOrder();
    }
}
