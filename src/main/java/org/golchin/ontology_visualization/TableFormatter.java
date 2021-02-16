package org.golchin.ontology_visualization;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public abstract class TableFormatter {
    protected final Table table;
    private final BufferedWriter writer;

    protected TableFormatter(Table table, Path file) throws IOException {
        this.table = table;
        writer = Files.newBufferedWriter(file);
    }

    protected void writeLine(String line) throws IOException {
        writer.write(line);
        writer.newLine();
    }

    protected void writePreamble() throws IOException {

    }

    protected void writeFooter() throws IOException {

    }

    protected abstract void writeRow(List<String> row) throws IOException;

    protected abstract void writeHeader(List<String> header) throws IOException;

    public void writeToFile() throws IOException {
        writePreamble();
        writeHeader(table.getHeader());
        for (List<String> row : table.getValues()) {
            writeRow(row);
        }
        writeFooter();
        writer.close();
    }
}
