package org.fastsearch;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class FastSearchApp extends Application {

    private MainWindowController controller;

    public static void main(String[] args) {
        launch(args);
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
