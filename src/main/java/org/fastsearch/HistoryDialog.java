package org.fastsearch;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Search History Dialog
 */
public class HistoryDialog extends Dialog<SearchConfig.SearchHistory> {
    private static final Logger logger = Logger.getLogger(HistoryDialog.class.getName());
    private final ListView<SearchConfig.SearchHistory> historyList;

    public HistoryDialog(SearchConfig config) {
        setTitle("Search History");
        setHeaderText("Recent Searches (double-click to re-run)");

        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.setPrefSize(600, 400);

        historyList = new ListView<>();
        historyList.setItems(FXCollections.observableArrayList(config.getHistory()));
        historyList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(SearchConfig.SearchHistory item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getDisplayText());
                }
            }
        });

        // Double-click to select
        historyList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                SearchConfig.SearchHistory selected = historyList.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    setResult(selected);
                    close();
                }
            }
        });

        VBox.setVgrow(historyList, Priority.ALWAYS);

        Label infoLabel = new Label("Click OK to re-run selected search");
        infoLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: gray;");

        content.getChildren().addAll(historyList, infoLabel);

        getDialogPane().setContent(content);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Apply theme to dialog pane directly in constructor
        String stylesheet = "styles-dark.css";
        if ("Light".equalsIgnoreCase(config.getTheme())) {
            stylesheet = "styles-light.css";
        }
        try {
            URL resource = FastSearchApp.class.getResource(stylesheet);
            if (resource != null) {
                String css = resource.toExternalForm();
                getDialogPane().getStylesheets().add(css);
            } else {
                logger.log(Level.WARNING, "Could not find stylesheet: {0}", stylesheet);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error loading stylesheet: " + stylesheet, e);
        }

        setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return historyList.getSelectionModel().getSelectedItem();
            }
            return null;
        });
    }
}
