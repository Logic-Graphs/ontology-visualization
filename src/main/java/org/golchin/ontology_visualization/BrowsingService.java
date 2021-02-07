package org.golchin.ontology_visualization;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import org.apache.log4j.Logger;

import java.awt.*;
import java.io.IOException;
import java.net.URI;

public class BrowsingService extends Service<Void> {
    private static final Logger LOGGER = Logger.getLogger(PanningMouseManager.class);
    private String url;

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    protected Task<Void> createTask() {
        return new Task<Void>() {
            @Override
            protected Void call() {
                try {
                    Desktop.getDesktop().browse(URI.create(url));
                } catch (IOException e) {
                    LOGGER.error("Failed to load page", e);
                }
                return null;
            }
        };
    }
}
