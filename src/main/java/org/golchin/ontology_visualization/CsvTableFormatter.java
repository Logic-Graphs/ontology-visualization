package org.golchin.ontology_visualization;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class CsvTableFormatter extends TableFormatter {
    protected CsvTableFormatter(Table table, Path file) throws IOException {
        super(table, file);
    }

    private String commaSeparated(List<String> values) {
        return String.join(",", values);
    }

    @Override
    protected void writeRow(List<String> row) throws IOException {
        writeLine(commaSeparated(row));
    }

    @Override
    protected void writeHeader(List<String> header) throws IOException {
        writeLine(commaSeparated(header));
    }
}
