package org.golchin.ontology_visualization;

import javafx.concurrent.Service;
import javafx.concurrent.Task;

public class LayoutChooserService extends Service<EvaluatedLayout> {
    private final LayoutChooser layoutChooser;

    public LayoutChooserService(LayoutChooser layoutChooser) {
        this.layoutChooser = layoutChooser;
    }

    @Override
    protected Task<EvaluatedLayout> createTask() {
        return new Task<EvaluatedLayout>() {
            @Override
            protected EvaluatedLayout call() {
                return layoutChooser.chooseLayout();
            }
        };
    }
}
