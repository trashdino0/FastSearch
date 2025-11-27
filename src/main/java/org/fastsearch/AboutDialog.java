package org.fastsearch;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * About Dialog - Shows application information
 */
public class AboutDialog extends Dialog<Void> {
    private final SearchConfig config;

    public AboutDialog(SearchConfig config) {
        this.config = config;
        setTitle("About Fast Search");
        setHeaderText("Fast File & Content Search Tool");

        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.setAlignment(Pos.CENTER);

        Label versionLabel = new Label("Version 1.0.0");
        versionLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        Label descLabel = getLabel();

        Label authorLabel = new Label("Created by TrashDino");
        authorLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: gray;");

        Hyperlink githubLink = new Hyperlink("View on GitHub");
        githubLink.setOnAction(e -> {
            try {
                java.awt.Desktop.getDesktop().browse(
                        new java.net.URI("https://github.com/trashdino0/FastSearch")
                );
            } catch (Exception ex) {
                System.err.println("Could not open browser: " + ex.getMessage());
            }
        });

        content.getChildren().addAll(versionLabel, descLabel, githubLink, authorLabel);

        getDialogPane().setContent(content);
        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        // Apply theme to dialog pane directly in constructor
        String stylesheet = "styles-dark.css";
        if ("Light".equalsIgnoreCase(config.getTheme())) {
            stylesheet = "styles-light.css";
        }
        try {
            String css = FastSearchApp.class.getResource(stylesheet).toExternalForm();
            getDialogPane().getStylesheets().add(css);
        } catch (Exception e) {
            System.err.println("Could not load stylesheet for AboutDialog: " + stylesheet);
            e.printStackTrace();
        }
    }

    private static Label getLabel() {
        Label descLabel = new Label(
                """
                        A powerful file and content search tool
                        with advanced filtering and history tracking.
                        
                        Features:
                        • Real-time search results
                        • Deep folder recursion
                        • Content search in text files
                        • Advanced filtering options
                        • Search history tracking"""
        );
        descLabel.setStyle("-fx-text-alignment: center;");
        return descLabel;
    }
}