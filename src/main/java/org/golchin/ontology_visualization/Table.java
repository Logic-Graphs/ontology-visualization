package org.golchin.ontology_visualization;

import lombok.Getter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class Table {
    private final String label;
    private final String caption;
    private final List<String> header;
    private final Map<String, Integer> indices;
    private final int nRows;
    private final int nColumns;
    private final List<List<String>> values;

    public Table(String label, String caption, List<String> header, int nRows) {
        this.label = label;
        this.caption = caption;
        this.header = header;
        indices = getIndices(header);
        this.nRows = nRows;
        nColumns = header.size();
        this.values = new ArrayList<>();
        for (int i = 0; i < nRows; i++) {
            List<String> row = new ArrayList<>(nColumns);
            for (int j = 0; j < nColumns; j++) {
                row.add(null);
            }
            values.add(row);
        }
    }

    private static <T> Map<T, Integer> getIndices(List<T> list) {
        Map<T, Integer> result = new HashMap<>();
        for (int i = 0; i < list.size(); i++) {
            result.put(list.get(i), i);
        }
        return result;
    }

    public void setValue(String column, int rowIndex, String value) {
        int columnIndex = indices.get(column);
        values.get(rowIndex).set(columnIndex, value);
    }

    public void writeToCsv(Path file) throws IOException {
        new CsvTableFormatter(this, file).writeToFile();
    }

    public void writeToLatex(Path file) throws IOException {
        new LatexTableFormatter(this, file).writeToFile();
    }
}
