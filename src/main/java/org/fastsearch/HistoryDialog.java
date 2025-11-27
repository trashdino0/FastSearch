package org.fastsearch;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Search History Dialog
 */
class HistoryDialog extends Dialog<SearchHistory> {
    private final ListView<SearchHistory> historyList;
    private final SearchConfig config;

    public HistoryDialog(SearchConfig config) {
        this.config = config;
        setTitle("Search History");
        setHeaderText("Recent Searches (double-click to re-run)");

        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.setPrefSize(600, 400);

        historyList = new ListView<>();
        historyList.setItems(FXCollections.observableArrayList(config.getHistory()));
        historyList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(SearchHistory item, boolean empty) {
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
                SearchHistory selected = historyList.getSelectionModel().getSelectedItem();
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
            String css = FastSearchApp.class.getResource(stylesheet).toExternalForm();
            getDialogPane().getStylesheets().add(css);
        } catch (Exception e) {
            System.err.println("Could not load stylesheet for HistoryDialog: " + stylesheet);
            e.printStackTrace();
        }

        setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return historyList.getSelectionModel().getSelectedItem();
            }
            return null;
        });
    }
}
