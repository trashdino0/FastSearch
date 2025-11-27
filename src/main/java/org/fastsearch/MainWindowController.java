package org.fastsearch;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Optional;

public class MainWindowController {

    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> searchModeCombo;
    @FXML
    private TextField extensionField;
    @FXML
    private TableView<FileResult> resultsTable;
    @FXML
    private Label statusLabel;
    @FXML
    private Label timerLabel;
    @FXML
    private ProgressIndicator progressIndicator;
    @FXML
    private Button searchButton;
    @FXML
    private Button stopButton;
    @FXML
    private TextField minSizeField;
    @FXML
    private TextField maxSizeField;
    @FXML
    private DatePicker modifiedAfterPicker;
    @FXML
    private CheckBox todayOnlyCheck;
    @FXML
    private Spinner<Integer> maxResultsSpinner;
    @FXML
    private TextField searchFolderField;
    @FXML
    private Button browseFolderButton;
    @FXML
    private Button clearFolderButton;

    @FXML
    private TableColumn<FileResult, String> nameCol;
    @FXML
    private TableColumn<FileResult, String> pathCol;
    @FXML
    private TableColumn<FileResult, String> sizeCol;
    @FXML
    private TableColumn<FileResult, String> typeCol;
    @FXML
    private TableColumn<FileResult, String> modifiedCol;

    private long searchStartTime;
    private javafx.animation.Timeline timerTimeline;

    private ObservableList<FileResult> searchResults;
    private SearchConfig config;
    private SearchEngine searchEngine;
    private Task<Void> searchTask;

    @FXML
    public void initialize() {
        config = SearchConfig.load();
        searchEngine = new SearchEngine(config);
        searchResults = FXCollections.observableArrayList();

        searchModeCombo.getItems().addAll("Filename", "Content");
        searchModeCombo.setValue("Filename");

        maxResultsSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(100, 10000, config.getMaxResults(), 100));

        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        pathCol.setCellValueFactory(new PropertyValueFactory<>("path"));
        sizeCol.setCellValueFactory(new PropertyValueFactory<>("sizeFormatted"));
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        modifiedCol.setCellValueFactory(new PropertyValueFactory<>("modifiedFormatted"));

        resultsTable.setItems(searchResults);

        todayOnlyCheck.setOnAction(e -> {
            if (todayOnlyCheck.isSelected()) {
                modifiedAfterPicker.setValue(LocalDate.now());
            }
        });

        resultsTable.setRowFactory(tv -> {
            TableRow<FileResult> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    openFile(row.getItem());
                }
            });
            return row;
        });
    }

    public void saveConfig() {
        config.save();
    }

    @FXML
    private void performSearch() {
        if (searchTask != null && searchTask.isRunning()) {
            searchTask.cancel(true);
        }

        String query = searchField.getText().trim();
        if (query.isEmpty()) {
            showAlert("Error", "Please enter a search query", Alert.AlertType.ERROR);
            return;
        }

        String mode = searchModeCombo.getValue();
        String extension = extensionField.getText().trim();
        String customFolder = searchFolderField.getText().trim();

        SearchFilters filters = new SearchFilters();
        filters.setMinSize(parseSize(minSizeField.getText()));
        filters.setMaxSize(parseSize(maxSizeField.getText()));
        if (modifiedAfterPicker.getValue() != null) {
            filters.setModifiedAfter(modifiedAfterPicker.getValue().atStartOfDay());
        }

        int maxResults = maxResultsSpinner.getValue();

        searchResults.clear();

        searchStartTime = System.currentTimeMillis();
        startTimer();

        setSearching(true);

        searchTask = new Task<>() {
            @Override
            protected Void call() {
                updateMessage("Searching...");

                try {
                    if (mode.equals("Filename")) {
                        searchEngine.searchFilenameRealtime(query, extension, customFolder, filters, maxResults,
                                result -> addResultToTable(result));
                    } else {
                        searchEngine.searchContentRealtime(query, extension, customFolder, filters, maxResults,
                                result -> addResultToTable(result));
                    }
                } catch (Exception e) {
                    if (!isCancelled()) {
                        e.printStackTrace();
                        throw e;
                    }
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

                config.addToHistory(mode, query, extension, searchResults.size());
            }

            @Override
            protected void failed() {
                stopTimer();
                setSearching(false);
                updateStatus("Search failed");
                Throwable ex = getException();
                if (ex != null) {
                    ex.printStackTrace();
                    showAlert("Error", "Search failed: " + ex.getMessage(), Alert.AlertType.ERROR);
                }
            }

            @Override
            protected void cancelled() {
                stopTimer();
                setSearching(false);
                updateStatus("Search cancelled");
            }
        };

        searchEngine.setSearchTask(searchTask);
        Thread searchThread = new Thread(searchTask);
        searchThread.setDaemon(true);
        searchThread.start();
    }

    @FXML
    private void stopSearch() {
        if (searchTask != null && searchTask.isRunning()) {
            searchTask.cancel(true);
        }
        if (searchEngine != null) {
            searchEngine.cancelSearch();
        }
        setSearching(false);
    }

    @FXML
    private void clearResults() {
        searchResults.clear();
    }

    @FXML
    private void selectSearchFolder() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Folder to Search");

        String currentFolder = searchFolderField.getText().trim();
        if (!currentFolder.isEmpty() && new File(currentFolder).exists()) {
            directoryChooser.setInitialDirectory(new File(currentFolder));
        } else {
            directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        }

        File selectedDirectory = directoryChooser.showDialog(searchFolderField.getScene().getWindow());

        if (selectedDirectory != null && selectedDirectory.exists()) {
            searchFolderField.setText(selectedDirectory.getAbsolutePath());
            updateStatus("Search folder set to: " + selectedDirectory.getName());
        }
    }

    @FXML
    private void clearSearchFolder() {
        searchFolderField.clear();
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
        stopButton.setVisible(searching);
        progressIndicator.setVisible(searching);
    }

    private void updateStatus(String message) {
        Platform.runLater(() -> statusLabel.setText(message));
    }

    @FXML
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

    @FXML
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

    @FXML
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

    @FXML
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

    @FXML
    private void exportResults() {
        if (searchResults.isEmpty()) {
            showAlert("Info", "No results to export", Alert.AlertType.INFORMATION);
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Results");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Text Files", "*.txt"),
                new FileChooser.ExtensionFilter("CSV Files", "*.csv")
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

    @FXML
    private void showConfigDialog() {
        ConfigDialog dialog = new ConfigDialog(config);
        dialog.showAndWait();
        searchEngine = new SearchEngine(config);
    }

    @FXML
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

    @FXML
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
