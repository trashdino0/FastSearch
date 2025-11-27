package org.fastsearch;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.util.Optional;

/**
 * Configuration Dialog
 */
class ConfigDialog extends Dialog<Void> {
    private final SearchConfig config;
    private final ComboBox<String> themeCombo;
    private final Spinner<Integer> maxResultsSpinner;
    private final ListView<String> excludeList;
    private final ListView<String> foldersList;

    public ConfigDialog(SearchConfig config) {
        this.config = config;

        setTitle("Configuration");
        setHeaderText("Search Configuration");

        // Create UI
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setPrefWidth(500);

        // Theme
        HBox themeBox = new HBox(10);
        themeBox.setAlignment(Pos.CENTER_LEFT);
        Label themeLabel = new Label("Theme:");
        themeLabel.setPrefWidth(120);
        themeCombo = new ComboBox<>();
        themeCombo.setItems(FXCollections.observableArrayList("Light", "Dark"));
        themeCombo.setValue(config.getTheme());
        themeCombo.setPrefWidth(150);
        themeBox.getChildren().addAll(themeLabel, themeCombo);

        // Max Results
        HBox maxResultsBox = new HBox(10);
        maxResultsBox.setAlignment(Pos.CENTER_LEFT);
        Label maxLabel = new Label("Max Results:");
        maxLabel.setPrefWidth(120);
        maxResultsSpinner = new Spinner<>(100, 10000, config.getMaxResults(), 100);
        maxResultsSpinner.setEditable(true);
        maxResultsSpinner.setPrefWidth(150);
        maxResultsBox.getChildren().addAll(maxLabel, maxResultsSpinner);

        // Exclude Patterns
        Label excludeLabel = new Label("Exclude Patterns:");
        excludeLabel.setStyle("-fx-font-weight: bold;");

        excludeList = new ListView<>();
        excludeList.setItems(FXCollections.observableArrayList(config.getExcludePatterns()));
        excludeList.setPrefHeight(100);

        HBox excludeButtons = new HBox(5);
        Button addExcludeBtn = new Button("Add");
        addExcludeBtn.setOnAction(e -> addExcludePattern());
        Button removeExcludeBtn = new Button("Remove");
        removeExcludeBtn.setOnAction(e -> removeExcludePattern());
        excludeButtons.getChildren().addAll(addExcludeBtn, removeExcludeBtn);

        // Search Folders
        Label foldersLabel = new Label("Search Folders:");
        foldersLabel.setStyle("-fx-font-weight: bold;");

        foldersList = new ListView<>();
        foldersList.setItems(FXCollections.observableArrayList(config.getExtraFolders()));
        foldersList.setPrefHeight(100);

        HBox foldersButtons = new HBox(5);
        Button addFolderBtn = new Button("Add");
        addFolderBtn.setOnAction(e -> addFolder());
        Button removeFolderBtn = new Button("Remove");
        removeFolderBtn.setOnAction(e -> removeFolder());
        foldersButtons.getChildren().addAll(addFolderBtn, removeFolderBtn);

        content.getChildren().addAll(
                themeBox,
                maxResultsBox,
                new Separator(),
                excludeLabel, excludeList, excludeButtons,
                new Separator(),
                foldersLabel, foldersList, foldersButtons
        );

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
            System.err.println("Could not load stylesheet for ConfigDialog: " + stylesheet);
            e.printStackTrace();
        }

        // Save on OK
        setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                saveConfig();
            }
            return null;
        });
    }

    private void addExcludePattern() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Exclude Pattern");
        dialog.setHeaderText("Enter pattern to exclude");
        dialog.setContentText("Pattern (e.g., *.tmp, node_modules):");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(pattern -> {
            if (!pattern.trim().isEmpty()) {
                excludeList.getItems().add(pattern.trim());
            }
        });
    }

    private void removeExcludePattern() {
        String selected = excludeList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            excludeList.getItems().remove(selected);
        }
    }

    private void addFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Folder to Search");
        File folder = chooser.showDialog(getDialogPane().getScene().getWindow());

        if (folder != null && folder.exists()) {
            String path = folder.getAbsolutePath();
            if (!foldersList.getItems().contains(path)) {
                foldersList.getItems().add(path);
            }
        }
    }

    private void removeFolder() {
        String selected = foldersList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            foldersList.getItems().remove(selected);
        }
    }

    private void saveConfig() {
        config.setTheme(themeCombo.getValue());
        config.setMaxResults(maxResultsSpinner.getValue());
        config.setExcludePatterns(new java.util.ArrayList<>(excludeList.getItems()));
        config.setExtraFolders(new java.util.ArrayList<>(foldersList.getItems()));
        config.save();
    }
}

