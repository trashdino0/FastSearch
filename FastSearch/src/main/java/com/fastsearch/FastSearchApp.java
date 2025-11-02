package com.fastsearch;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Fast File & Content Search Tool - JavaFX UI
 * Similar to the Python version but with a graphical interface
 */
public class FastSearchApp extends Application {

    // UI Components
    private TextField searchField;
    private ComboBox<String> searchModeCombo;
    private TextField extensionField;
    private TableView<FileResult> resultsTable;
    private Label statusLabel;
    private Label timerLabel;
    private ProgressIndicator progressIndicator;
    private Button searchButton;
    private TextField minSizeField;
    private TextField maxSizeField;
    private DatePicker modifiedAfterPicker;
    private CheckBox todayOnlyCheck;
    private Spinner<Integer> maxResultsSpinner;
    private TextField searchFolderField;
    private Button browseFolderButton;

    // Timer tracking
    private long searchStartTime;
    private javafx.animation.Timeline timerTimeline;

    // Data
    private ObservableList<FileResult> searchResults;
    private SearchConfig config;
    private SearchEngine searchEngine;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        config = SearchConfig.load();
        searchEngine = new SearchEngine(config);
        searchResults = FXCollections.observableArrayList();

        primaryStage.setTitle("Fast Search");
        primaryStage.setScene(createScene());
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        primaryStage.show();
    }

    private Scene createScene() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        // Top: Search controls
        root.setTop(createSearchPanel());

        // Center: Results table
        root.setCenter(createResultsPanel());

        // Bottom: Status bar
        root.setBottom(createStatusBar());

        return new Scene(root, 1000, 700);
    }

    private VBox createSearchPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10));
        panel.setStyle("-fx-background-color: #f4f4f4; -fx-background-radius: 5;");

        // Folder selection row
        HBox folderBox = new HBox(10);
        folderBox.setAlignment(Pos.CENTER_LEFT);

        Label folderLabel = new Label("Search in:");
        folderLabel.setPrefWidth(80);

        searchFolderField = new TextField();
        searchFolderField.setPromptText("Select folder to search (or leave empty for common folders)");
        searchFolderField.setEditable(false);
        HBox.setHgrow(searchFolderField, Priority.ALWAYS);

        browseFolderButton = new Button("📁 Browse");
        browseFolderButton.setOnAction(e -> selectSearchFolder());

        Button clearFolderButton = new Button("✕");
        clearFolderButton.setTooltip(new Tooltip("Clear and search in common folders"));
        clearFolderButton.setOnAction(e -> searchFolderField.clear());

        folderBox.getChildren().addAll(folderLabel, searchFolderField, browseFolderButton, clearFolderButton);

        // Search mode and query
        HBox searchBox = new HBox(10);
        searchBox.setAlignment(Pos.CENTER_LEFT);

        Label modeLabel = new Label("Mode:");
        searchModeCombo = new ComboBox<>();
        searchModeCombo.getItems().addAll("Filename", "Content");
        searchModeCombo.setValue("Filename");
        searchModeCombo.setPrefWidth(120);

        Label queryLabel = new Label("Search:");
        searchField = new TextField();
        searchField.setPromptText("Enter filename or text to search...");
        HBox.setHgrow(searchField, Priority.ALWAYS);
        searchField.setOnAction(e -> performSearch());

        Label extLabel = new Label("Extension:");
        extensionField = new TextField();
        extensionField.setPromptText(".txt, .pdf");
        extensionField.setPrefWidth(100);

        searchButton = new Button("Search");
        searchButton.setDefaultButton(true);
        searchButton.setOnAction(e -> performSearch());
        searchButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");

        progressIndicator = new ProgressIndicator();
        progressIndicator.setPrefSize(25, 25);
        progressIndicator.setVisible(false);

        searchBox.getChildren().addAll(
                modeLabel, searchModeCombo,
                queryLabel, searchField,
                extLabel, extensionField,
                searchButton, progressIndicator
        );

        // Filters
        HBox filtersBox = createFiltersPanel();

        panel.getChildren().addAll(folderBox, searchBox, filtersBox);
        return panel;
    }

    private HBox createFiltersPanel() {
        HBox filters = new HBox(15);
        filters.setAlignment(Pos.CENTER_LEFT);

        // Size filters
        Label sizeLabel = new Label("Size:");
        minSizeField = new TextField();
        minSizeField.setPromptText("Min (e.g., 10mb)");
        minSizeField.setPrefWidth(100);

        Label toLabel = new Label("to");
        maxSizeField = new TextField();
        maxSizeField.setPromptText("Max");
        maxSizeField.setPrefWidth(100);

        // Date filter
        Label modLabel = new Label("Modified after:");
        modifiedAfterPicker = new DatePicker();
        modifiedAfterPicker.setPrefWidth(140);

        todayOnlyCheck = new CheckBox("Today only");
        todayOnlyCheck.setOnAction(e -> {
            if (todayOnlyCheck.isSelected()) {
                modifiedAfterPicker.setValue(java.time.LocalDate.now());
            }
        });

        // Max results
        Label maxLabel = new Label("Max results:");
        maxResultsSpinner = new Spinner<>(100, 10000, config.getMaxResults(), 100);
        maxResultsSpinner.setPrefWidth(100);
        maxResultsSpinner.setEditable(true);

        filters.getChildren().addAll(
                sizeLabel, minSizeField, toLabel, maxSizeField,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                modLabel, modifiedAfterPicker, todayOnlyCheck,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                maxLabel, maxResultsSpinner
        );

        return filters;
    }

    private VBox createResultsPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10, 0, 0, 0));

        Label resultsLabel = new Label("Search Results");
        resultsLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        // Results table
        resultsTable = new TableView<>();
        resultsTable.setItems(searchResults);
        resultsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        resultsTable.setPlaceholder(new Label("No results yet. Enter a search query and click Search."));

        TableColumn<FileResult, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(250);
        nameCol.setMinWidth(100);

        TableColumn<FileResult, String> pathCol = new TableColumn<>("Path");
        pathCol.setCellValueFactory(new PropertyValueFactory<>("path"));
        pathCol.setPrefWidth(400);
        pathCol.setMinWidth(200);

        TableColumn<FileResult, String> sizeCol = new TableColumn<>("Size");
        sizeCol.setCellValueFactory(new PropertyValueFactory<>("sizeFormatted"));
        sizeCol.setPrefWidth(100);
        sizeCol.setMinWidth(60);

        TableColumn<FileResult, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        typeCol.setPrefWidth(80);
        typeCol.setMinWidth(60);

        TableColumn<FileResult, String> modifiedCol = new TableColumn<>("Modified");
        modifiedCol.setCellValueFactory(new PropertyValueFactory<>("modifiedFormatted"));
        modifiedCol.setPrefWidth(150);
        modifiedCol.setMinWidth(120);

        resultsTable.getColumns().addAll(nameCol, pathCol, sizeCol, typeCol, modifiedCol);

        // Context menu for results
        ContextMenu contextMenu = createResultContextMenu();
        resultsTable.setContextMenu(contextMenu);
        resultsTable.setRowFactory(tv -> {
            TableRow<FileResult> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    openFile(row.getItem());
                }
            });
            return row;
        });

        // Action buttons
        HBox actionBox = new HBox(10);
        actionBox.setAlignment(Pos.CENTER_LEFT);

        Button openBtn = new Button("Open");
        openBtn.setOnAction(e -> openSelected());

        Button folderBtn = new Button("Open Folder");
        folderBtn.setOnAction(e -> openFolderSelected());

        Button copyBtn = new Button("Copy Path");
        copyBtn.setOnAction(e -> copyPathSelected());

        Button deleteBtn = new Button("Delete");
        deleteBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
        deleteBtn.setOnAction(e -> deleteSelected());

        Button exportBtn = new Button("Export Results");
        exportBtn.setOnAction(e -> exportResults());

        Button clearBtn = new Button("Clear");
        clearBtn.setOnAction(e -> searchResults.clear());

        actionBox.getChildren().addAll(openBtn, folderBtn, copyBtn, deleteBtn, exportBtn, clearBtn);

        VBox.setVgrow(resultsTable, Priority.ALWAYS);
        panel.getChildren().addAll(resultsLabel, resultsTable, actionBox);

        return panel;
    }

    private ContextMenu createResultContextMenu() {
        ContextMenu menu = new ContextMenu();

        MenuItem openItem = new MenuItem("Open");
        openItem.setOnAction(e -> openSelected());

        MenuItem folderItem = new MenuItem("Open Folder");
        folderItem.setOnAction(e -> openFolderSelected());

        MenuItem copyItem = new MenuItem("Copy Path");
        copyItem.setOnAction(e -> copyPathSelected());

        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setOnAction(e -> deleteSelected());

        menu.getItems().addAll(openItem, folderItem, copyItem, new SeparatorMenuItem(), deleteItem);
        return menu;
    }

    private HBox createStatusBar() {
        HBox statusBar = new HBox(10);
        statusBar.setPadding(new Insets(5));
        statusBar.setStyle("-fx-background-color: #e0e0e0;");
        statusBar.setAlignment(Pos.CENTER_LEFT);

        statusLabel = new Label("Ready - Select a folder or leave empty to search common folders");

        timerLabel = new Label("⏱ 0:00");
        timerLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2196F3;");
        timerLabel.setVisible(false);

        Button configBtn = new Button("⚙ Config");
        configBtn.setOnAction(e -> showConfigDialog());

        Button historyBtn = new Button("📋 History");
        historyBtn.setOnAction(e -> showHistoryDialog());

        Button aboutBtn = new Button("ℹ About");
        aboutBtn.setOnAction(e -> showAboutDialog());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        statusBar.getChildren().addAll(statusLabel, timerLabel, spacer, historyBtn, configBtn, aboutBtn);
        return statusBar;
    }

    private void performSearch() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) {
            showAlert("Error", "Please enter a search query", Alert.AlertType.ERROR);
            return;
        }

        String mode = searchModeCombo.getValue();
        String extension = extensionField.getText().trim();
        String customFolder = searchFolderField.getText().trim();

        // Build filters
        SearchFilters filters = new SearchFilters();
        filters.setMinSize(parseSize(minSizeField.getText()));
        filters.setMaxSize(parseSize(maxSizeField.getText()));
        if (modifiedAfterPicker.getValue() != null) {
            filters.setModifiedAfter(modifiedAfterPicker.getValue().atStartOfDay());
        }

        int maxResults = maxResultsSpinner.getValue();

        // Clear previous results
        searchResults.clear();

        // Start timer
        searchStartTime = System.currentTimeMillis();
        startTimer();

        // Disable UI during search
        setSearching(true);

        // Run search in background with real-time updates
        Task<Void> searchTask = new Task<>() {
            @Override
            protected Void call() {
                updateMessage("Searching...");

                try {
                    if (mode.equals("Filename")) {
                        searchEngine.searchFilenameRealtime(query, customFolder, filters, maxResults,
                                result -> addResultToTable(result));
                    } else {
                        searchEngine.searchContentRealtime(query, extension, customFolder, filters, maxResults,
                                result -> addResultToTable(result));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    throw e;
                }

                return null;
            }

            @Override
            protected void succeeded() {
                stopTimer();
                setSearching(false);

                long elapsedSeconds = (System.currentTimeMillis() - searchStartTime) / 1000;
                String folderInfo = customFolder.isEmpty() ? "common folders" : new File(customFolder).getName();
                updateStatus(searchResults.size() + " results found in " + folderInfo +
                        " (" + elapsedSeconds + "s)");

                // Add to history
                config.addToHistory(mode, query, extension, searchResults.size());
            }

            @Override
            protected void failed() {
                stopTimer();
                setSearching(false);
                updateStatus("Search failed");
                Throwable ex = getException();
                ex.printStackTrace();
                showAlert("Error", "Search failed: " + ex.getMessage(), Alert.AlertType.ERROR);
            }

            @Override
            protected void cancelled() {
                stopTimer();
                setSearching(false);
                updateStatus("Search cancelled");
            }
        };

        Thread searchThread = new Thread(searchTask);
        searchThread.setDaemon(true);
        searchThread.start();
    }

    private void addResultToTable(FileResult result) {
        Platform.runLater(() -> {
            searchResults.add(result);
            updateStatus("Found " + searchResults.size() + " files...");
        });
    }

    private void startTimer() {
        timerLabel.setVisible(true);
        timerTimeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), e -> updateTimer())
        );
        timerTimeline.setCycleCount(javafx.animation.Timeline.INDEFINITE);
        timerTimeline.play();
    }

    private void stopTimer() {
        if (timerTimeline != null) {
            timerTimeline.stop();
        }
    }

    private void updateTimer() {
        long elapsedSeconds = (System.currentTimeMillis() - searchStartTime) / 1000;
        long minutes = elapsedSeconds / 60;
        long seconds = elapsedSeconds % 60;
        Platform.runLater(() ->
                timerLabel.setText(String.format("⏱ %d:%02d", minutes, seconds))
        );
    }

    private void setSearching(boolean searching) {
        searchButton.setDisable(searching);
        searchField.setDisable(searching);
        progressIndicator.setVisible(searching);
    }

    private void updateStatus(String message) {
        Platform.runLater(() -> statusLabel.setText(message));
    }

    private void openSelected() {
        FileResult selected = resultsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            openFile(selected);
        }
    }

    private void openFile(FileResult result) {
        try {
            java.awt.Desktop.getDesktop().open(new File(result.getPath()));
        } catch (Exception e) {
            showAlert("Error", "Could not open file: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void openFolderSelected() {
        FileResult selected = resultsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                File folder = new File(selected.getPath()).getParentFile();
                java.awt.Desktop.getDesktop().open(folder);
            } catch (Exception e) {
                showAlert("Error", "Could not open folder: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    private void copyPathSelected() {
        FileResult selected = resultsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
            javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
            content.putString(selected.getPath());
            clipboard.setContent(content);
            updateStatus("Path copied to clipboard");
        }
    }

    private void deleteSelected() {
        FileResult selected = resultsTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete file?");
        confirm.setContentText(selected.getName());

        if (confirm.showAndWait().get() == ButtonType.OK) {
            try {
                Files.delete(Paths.get(selected.getPath()));
                searchResults.remove(selected);
                updateStatus("File deleted");
            } catch (Exception e) {
                showAlert("Error", "Could not delete file: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    private void exportResults() {
        if (searchResults.isEmpty()) {
            showAlert("Info", "No results to export", Alert.AlertType.INFORMATION);
            return;
        }

        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Export Results");
        fileChooser.getExtensionFilters().addAll(
                new javafx.stage.FileChooser.ExtensionFilter("Text Files", "*.txt"),
                new javafx.stage.FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );

        File file = fileChooser.showSaveDialog(resultsTable.getScene().getWindow());
        if (file != null) {
            try (PrintWriter writer = new PrintWriter(file)) {
                if (file.getName().endsWith(".csv")) {
                    writer.println("Name,Path,Size,Type,Modified");
                    for (FileResult r : searchResults) {
                        writer.printf("\"%s\",\"%s\",%d,\"%s\",\"%s\"%n",
                                r.getName(), r.getPath(), r.getSize(), r.getType(), r.getModifiedFormatted());
                    }
                } else {
                    for (FileResult r : searchResults) {
                        writer.println(r.getPath());
                    }
                }
                updateStatus("Exported " + searchResults.size() + " results");
            } catch (Exception e) {
                showAlert("Error", "Export failed: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    private void showConfigDialog() {
        ConfigDialog dialog = new ConfigDialog(config);
        dialog.showAndWait();
        config.save();
        searchEngine = new SearchEngine(config);
    }

    private void showHistoryDialog() {
        HistoryDialog dialog = new HistoryDialog(config);
        Optional<SearchHistory> result = dialog.showAndWait();
        result.ifPresent(history -> {
            searchModeCombo.setValue(history.getMode());
            searchField.setText(history.getQuery());
            if (history.getExtension() != null) {
                extensionField.setText(history.getExtension());
            }
            performSearch();
        });
    }

    private void showAboutDialog() {
        AboutDialog dialog = new AboutDialog();
        dialog.showAndWait();
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void selectSearchFolder() {
        javafx.stage.DirectoryChooser directoryChooser = new javafx.stage.DirectoryChooser();
        directoryChooser.setTitle("Select Folder to Search");

        // Set initial directory if one is already selected
        String currentFolder = searchFolderField.getText().trim();
        if (!currentFolder.isEmpty() && new File(currentFolder).exists()) {
            directoryChooser.setInitialDirectory(new File(currentFolder));
        } else {
            // Default to user home
            directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        }

        File selectedDirectory = directoryChooser.showDialog(searchFolderField.getScene().getWindow());

        if (selectedDirectory != null && selectedDirectory.exists()) {
            searchFolderField.setText(selectedDirectory.getAbsolutePath());
            updateStatus("Search folder set to: " + selectedDirectory.getName());
        }
    }

    private Long parseSize(String sizeStr) {
        if (sizeStr == null || sizeStr.trim().isEmpty()) return null;

        sizeStr = sizeStr.toLowerCase().trim();
        double multiplier = 1;

        if (sizeStr.endsWith("kb")) {
            multiplier = 1024;
            sizeStr = sizeStr.substring(0, sizeStr.length() - 2);
        } else if (sizeStr.endsWith("mb")) {
            multiplier = 1024 * 1024;
            sizeStr = sizeStr.substring(0, sizeStr.length() - 2);
        } else if (sizeStr.endsWith("gb")) {
            multiplier = 1024 * 1024 * 1024;
            sizeStr = sizeStr.substring(0, sizeStr.length() - 2);
        }

        try {
            return (long) (Double.parseDouble(sizeStr.trim()) * multiplier);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}