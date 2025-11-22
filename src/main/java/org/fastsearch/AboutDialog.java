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
    public AboutDialog() {
        setTitle("About Fast Search");
        setHeaderText("Fast File & Content Search Tool");

        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.setAlignment(Pos.CENTER);

        Label versionLabel = new Label("Version 1.0.0");
        versionLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        Label descLabel = new Label(
                "A powerful file and content search tool\n" +
                        "with advanced filtering and history tracking.\n\n" +
                        "Features:\n" +
                        "• Real-time search results\n" +
                        "• Deep folder recursion\n" +
                        "• Content search in text files\n" +
                        "• Advanced filtering options\n" +
                        "• Search history tracking"
        );
        descLabel.setStyle("-fx-text-alignment: center;");

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
    }
}