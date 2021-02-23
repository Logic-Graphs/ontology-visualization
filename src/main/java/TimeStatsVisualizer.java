import com.google.common.math.StatsAccumulator;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.StatisticalLineAndShapeRenderer;
import org.jfree.data.statistics.DefaultStatisticalCategoryDataset;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public class TimeStatsVisualizer {
    private final JFreeChart chart;

    public TimeStatsVisualizer(Map<String, StatsAccumulator> statsByMetric, String timeLabel) {
        DefaultStatisticalCategoryDataset dataset = new DefaultStatisticalCategoryDataset();
        for (Map.Entry<String, StatsAccumulator> metricToStats : statsByMetric.entrySet()) {
            StatsAccumulator stats = metricToStats.getValue();
            dataset.add(stats.mean(), stats.populationStandardDeviation(), "", metricToStats.getKey());
        }
        chart = ChartFactory.createLineChart("",
                null,
                timeLabel,
                dataset,
                PlotOrientation.HORIZONTAL,
                false,
                true,
                true);
        StatisticalLineAndShapeRenderer statisticalRenderer =
                new StatisticalLineAndShapeRenderer(false, false);
        CategoryPlot categoryPlot = chart.getCategoryPlot();
        categoryPlot.setRenderer(statisticalRenderer);
        categoryPlot.getDomainAxis().setMaximumCategoryLabelLines(10);
        categoryPlot.setBackgroundPaint(Color.WHITE);
        categoryPlot.setRangeGridlinePaint(Color.BLACK);
    }

    public void writeToFile(Path file, int width, int height) throws IOException {
        ChartUtils.saveChartAsPNG(file.toFile(), chart, width, height);
    }
}
