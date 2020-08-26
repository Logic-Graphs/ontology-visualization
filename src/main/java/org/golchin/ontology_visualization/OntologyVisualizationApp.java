package org.golchin.ontology_visualization;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class OntologyVisualizationApp extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(OntologyVisualizationApp.class.getResource("/" + fxml + ".fxml"));
        return fxmlLoader.load();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent form = loadFXML("form");
        Scene scene = new Scene(form);
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
