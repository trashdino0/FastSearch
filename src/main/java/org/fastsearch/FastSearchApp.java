package org.fastsearch;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FastSearchApp extends Application {
    private static final Logger logger = Logger.getLogger(FastSearchApp.class.getName());

    private MainWindowController controller;

    public static void main(String[] args) {
        launch(args);
    }

    public static void applyTheme(Scene scene, String theme) {
        logger.log(Level.FINE, "Applying theme: {0}", theme);
        String stylesheet = "styles-dark.css";
        if ("Light".equalsIgnoreCase(theme)) {
            stylesheet = "styles-light.css";
        }
        logger.log(Level.FINE, "Loading stylesheet: {0}", stylesheet);

        try {
            java.net.URL resource = FastSearchApp.class.getResource(stylesheet);
            if (resource == null) {
                logger.log(Level.WARNING, "Could not find stylesheet: {0}", stylesheet);
                return;
            }
            String css = resource.toExternalForm();
            if (scene != null) {
                logger.fine("Scene is not null. Clearing and adding stylesheet.");
                scene.getStylesheets().clear();
                scene.getStylesheets().add(css);
                logger.fine("Stylesheet applied successfully");
            } else {
                logger.warning("Cannot apply stylesheet: scene is null");
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error loading stylesheet: " + stylesheet, e);
        }
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("MainWindow.fxml"));
        Parent root = loader.load();
        controller = loader.getController();

        primaryStage.setTitle("Fast Search");
        primaryStage.setScene(new Scene(root, 1000, 700));
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        primaryStage.show();
    }

    @Override
    public void stop() {
        if (controller != null) {
            controller.saveConfig();
        }
    }
}
