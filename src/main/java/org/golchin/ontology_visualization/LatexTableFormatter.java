package org.golchin.ontology_visualization;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class LatexTableFormatter extends TableFormatter {

    protected LatexTableFormatter(Table table, Path file) throws IOException {
        super(table, file);
    }

    @Override
    protected void writePreamble() throws IOException {
        writeLine("\\begin{table}[!htb]");
        writeLine("\\caption{" + table.getCaption() + "}");
        writeLine("\\label{tab:" + table.getLabel() + "}");
        writeLine("\\centering");
        writeLine("\\begin{tabularx}{\\textwidth}{|*{" + table.getNColumns() +
                "}{>{\\centering\\arraybackslash}X|}}\\hline");
    }

    private String serializeRow(List<String> row) {
        return row.stream().collect(Collectors.joining(" & ", "", "\\\\\\hline"));
    }

    @Override
    protected void writeRow(List<String> row) throws IOException {
        writeLine(serializeRow(row));
    }

    @Override
    protected void writeHeader(List<String> header) throws IOException {
        writeLine(serializeRow(header));
    }

    @Override
    protected void writeFooter() throws IOException {
        writeLine("\\end{tabularx}");
        writeLine("\\end{table}");
    }
}
