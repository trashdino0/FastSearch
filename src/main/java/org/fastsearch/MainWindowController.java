package org.fastsearch;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.tika.Tika;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainWindowController {
    private static final Logger logger = Logger.getLogger(MainWindowController.class.getName());


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
    private CheckBox caseSensitiveCheck;
    @FXML
    private CheckBox regexCheck;

    @FXML
    private TableColumn<FileResult, String> nameCol;
    @FXML
    private TableColumn<FileResult, String> pathCol;
    @FXML
    private TableColumn<FileResult, Long> sizeCol;
    @FXML
    private TableColumn<FileResult, String> typeCol;
    @FXML
    private TableColumn<FileResult, LocalDateTime> modifiedCol;
    @FXML
    private VBox previewVBox;

    private long searchStartTime;
    private javafx.animation.Timeline timerTimeline;

    private ObservableList<FileResult> searchResults;
    private SearchConfig config;
    private SearchEngine searchEngine;
    private Task<Void> searchTask;
    private final Tika tika = new Tika();
    private PauseTransition previewDelay;
    private Task<?> previewTask;


    @FXML
    public void initialize() {
        config = SearchConfig.load();
        searchEngine = new SearchEngine(config);
        searchResults = FXCollections.observableArrayList();

        // Apply theme on startup
        Platform.runLater(() -> FastSearchApp.applyTheme(searchField.getScene(), config.getTheme()));

        searchModeCombo.getItems().addAll("Filename", "Content");
        searchModeCombo.setValue("Filename");

        maxResultsSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(100, 10000, config.getMaxResults(), 100));

        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        pathCol.setCellValueFactory(new PropertyValueFactory<>("path"));
        sizeCol.setCellValueFactory(new PropertyValueFactory<>("size"));
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        modifiedCol.setCellValueFactory(new PropertyValueFactory<>("modified"));

        // Enable sorting
        nameCol.setSortable(true);
        pathCol.setSortable(true);
        typeCol.setSortable(true);
        sizeCol.setSortable(true);
        modifiedCol.setSortable(true);

        sizeCol.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Long size, boolean empty) {
                super.updateItem(size, empty);
                if (empty || size == null) {
                    setText(null);
                } else {
                    setText(formatSize(size));
                }
            }
        });

        modifiedCol.setCellFactory(tc -> new TableCell<>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            @Override
            protected void updateItem(LocalDateTime modified, boolean empty) {
                super.updateItem(modified, empty);
                if (empty || modified == null) {
                    setText(null);
                } else {
                    setText(formatter.format(modified));
                }
            }
        });

        resultsTable.setItems(searchResults);

        previewDelay = new PauseTransition(Duration.millis(200));
        resultsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            previewDelay.stop();
            if (newSelection != null) {
                previewDelay.setOnFinished(e -> updatePreview(newSelection));
                previewDelay.playFromStart();
            }
        });

        // Allow multiple column sorting
        resultsTable.getSortOrder().addAll(nameCol); // Default sort by name

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

        searchModeCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if ("Filename".equals(newVal)) {
                regexCheck.setSelected(false);
                regexCheck.setDisable(true);
            } else {
                regexCheck.setDisable(false);
            }
        });
        regexCheck.setDisable("Filename".equals(searchModeCombo.getValue()));
    }

    private void updatePreview(FileResult fileResult) {
        if (previewTask != null && previewTask.isRunning()) {
            previewTask.cancel();
        }

        previewVBox.getChildren().clear();
        Label titleLabel = new Label("Preview");
        titleLabel.getStyleClass().add("results-label");
        previewVBox.getChildren().add(titleLabel);

        if (fileResult == null) {
            previewVBox.getChildren().add(new Label("Select a file to see a preview."));
            return;
        }

        previewTask = new Task<>() {
            @Override
            protected Object call() throws Exception {
                File file = new File(fileResult.getPath());
                String mimeType = tika.detect(file);
                String searchTerm = searchField.getText();
                return new PreviewData(fileResult, mimeType, searchTerm);
            }

            @Override
            protected void succeeded() {
                PreviewData data = (PreviewData) getValue();
                String mimeType = data.mimeType();
                if (mimeType == null) {
                    showUnsupportedPreview(fileResult, "Unknown file type");
                    return;
                }

                if (mimeType.startsWith("image/")) {
                    showImageViewer(fileResult);
                } else if (mimeType.equals("application/pdf")) {
                    showPdfViewer(fileResult);
                } else if (mimeType.startsWith("text/") || mimeType.contains("xml") || mimeType.contains("json") || mimeType.contains("javascript")) {
                    showCodeViewer(fileResult, ((PreviewData) getValue()).searchTerm);
                } else {
                    showUnsupportedPreview(fileResult, "Preview not available for this file type: " + mimeType);
                }
            }

            @Override
            protected void failed() {
                showUnsupportedPreview(fileResult, "Failed to load preview.");
                getException().printStackTrace();
            }
        };

        Thread previewThread = new Thread(previewTask);
        previewThread.setDaemon(true);
        previewThread.start();
    }

    private void showImageViewer(FileResult fileResult) {
        previewTask = new Task<Image>() {
            @Override
            protected Image call() throws Exception {
                try (InputStream is = Files.newInputStream(Paths.get(fileResult.getPath()))) {
                    return new Image(is);
                }
            }

            @Override
            protected void succeeded() {
                Image image = getValue();
                ImageView imageView = new ImageView(image);
                imageView.setPreserveRatio(true);
                imageView.setFitWidth(previewVBox.getWidth() - 20); // padding
                previewVBox.widthProperty().addListener((obs, oldVal, newVal) -> {
                    imageView.setFitWidth(newVal.doubleValue() - 20);
                });
                setPreviewContent(fileResult, imageView);
            }

            @Override
            protected void failed() {
                showUnsupportedPreview(fileResult, "Failed to load image.");
            }
        };
        Thread thread = new Thread(previewTask);
        thread.setDaemon(true);
        thread.start();
    }

    private void showPdfViewer(FileResult fileResult) {
        previewTask = new Task<Image>() {
            @Override
            protected Image call() throws Exception {
                try (PDDocument document = Loader.loadPDF(new File(fileResult.getPath()))) {
                    PDFRenderer renderer = new PDFRenderer(document);
                    BufferedImage bufferedImage = renderer.renderImageWithDPI(0, 96); // 0 is first page, 96 DPI
                    return SwingFXUtils.toFXImage(bufferedImage, null);
                }
            }
            @Override
            protected void succeeded() {
                Image image = getValue();
                ImageView imageView = new ImageView(image);
                imageView.setPreserveRatio(true);
                imageView.setFitWidth(previewVBox.getWidth() - 20);
                previewVBox.widthProperty().addListener((obs, oldVal, newVal) -> {
                    imageView.setFitWidth(newVal.doubleValue() - 20);
                });
                setPreviewContent(fileResult, imageView);
            }
            @Override
            protected void failed() {
                showUnsupportedPreview(fileResult, "Failed to load PDF preview.");
                getException().printStackTrace();
            }
        };
        Thread thread = new Thread(previewTask);
        thread.setDaemon(true);
        thread.start();
    }

    private void showCodeViewer(FileResult fileResult, String searchTerm) {
        previewTask = new Task<String>() {
            @Override
            protected String call() throws Exception {
                File file = new File(fileResult.getPath());
                if (file.length() > 10 * 1024 * 1024) { // 10MB limit
                    return "[File too large for preview]";
                }
                return Files.readString(file.toPath());
            }

            @Override
            protected void succeeded() {
                String content = getValue();
                CodeArea codeArea = new CodeArea(content);
                codeArea.setEditable(false);
                
                String stylesheet = FastSearchApp.getStylesheet(config.getTheme());
                if (stylesheet != null) {
                    codeArea.getStylesheets().add(stylesheet);
                }
                codeArea.getStyleClass().add("code-area");

                String language = getLanguage(fileResult.getName());
                
                // Syntax highlighting
                codeArea.setStyleSpans(0, computeSyntaxHighlighting(codeArea.getText(), language));

                // Search term highlighting and scrolling
                if (searchTerm != null && !searchTerm.isEmpty()) {
                    Pattern pattern = Pattern.compile(Pattern.quote(searchTerm), Pattern.CASE_INSENSITIVE);
                    Matcher matcher = pattern.matcher(codeArea.getText());
                    boolean firstMatch = true;
                    while (matcher.find()) {
                        if (firstMatch) {
                            final int start = matcher.start();
                            Platform.runLater(() -> {
                                codeArea.moveTo(start);
                                codeArea.requestFollowCaret();
                            });
                            firstMatch = false;
                        }
                        codeArea.setStyle(matcher.start(), matcher.end(), Collections.singleton("search-term"));
                    }
                }
                
                VirtualizedScrollPane<CodeArea> scrollPane = new VirtualizedScrollPane<>(codeArea);
                VBox.setVgrow(scrollPane, Priority.ALWAYS);
                setPreviewContent(fileResult, scrollPane);
            }

            @Override
            protected void failed() {
                showUnsupportedPreview(fileResult, "Failed to load text content.");
            }
        };
        Thread thread = new Thread(previewTask);
        thread.setDaemon(true);
        thread.start();
    }

    private String getLanguage(String filename) {
        String extension = "";
        int i = filename.lastIndexOf('.');
        if (i > 0) {
            extension = filename.substring(i + 1);
        }
        return switch (extension) {
            case "java" -> "java";
            case "py" -> "python";
            case "js" -> "javascript";
            case "c", "h" -> "c";
            case "cpp", "hpp", "cxx", "hxx" -> "cpp";
            case "cs" -> "csharp";
            default -> "java"; // Default to java for now
        };
    }

    private void showUnsupportedPreview(FileResult fileResult, String message) {
        Label label = new Label(message);
        setPreviewContent(fileResult, label);
    }

    private void setPreviewContent(FileResult fileResult, javafx.scene.Node contentNode) {
        previewVBox.getChildren().clear();
        Label titleLabel = new Label("Preview");
        titleLabel.getStyleClass().add("results-label");
        previewVBox.getChildren().add(titleLabel);

        VBox metadataBox = new VBox();
        metadataBox.setSpacing(2);
        metadataBox.getChildren().add(new Label("Name: " + fileResult.getName()));
        metadataBox.getChildren().add(new Label("Path: " + fileResult.getPath()));
        metadataBox.getChildren().add(new Label("Size: " + formatSize(fileResult.getSize())));
        metadataBox.getChildren().add(new Label("Modified: " + fileResult.getModified().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
        previewVBox.getChildren().add(metadataBox);
        previewVBox.getChildren().add(contentNode);
    }

    private record PreviewData(FileResult fileResult, String mimeType, String searchTerm) {}

    private static final String[] JAVA_KEYWORDS = new String[] {
            "abstract", "assert", "boolean", "break", "byte",
            "case", "catch", "char", "class", "const",
            "continue", "default", "do", "double", "else",
            "enum", "extends", "final", "finally", "float",
            "for", "goto", "if", "implements", "import",
            "instanceof", "int", "interface", "long", "native",
            "new", "package", "private", "protected", "public",
            "return", "short", "static", "strictfp", "super",
            "switch", "synchronized", "this", "throw", "throws",
            "transient", "try", "void", "volatile", "while"
    };

    private static final String[] PYTHON_KEYWORDS = new String[] {
            "False", "None", "True", "and", "as", "assert", "async", "await", "break",
            "class", "continue", "def", "del", "elif", "else", "except", "finally",
            "for", "from", "global", "if", "import", "in", "is", "lambda", "nonlocal",
            "not", "or", "pass", "raise", "return", "try", "while", "with", "yield"
    };

    private static final String[] JS_KEYWORDS = new String[] {
            "break", "case", "catch", "class", "const", "continue", "debugger", "default",
            "delete", "do", "else", "export", "extends", "finally", "for", "function",
            "if", "import", "in", "instanceof", "new", "return", "super", "switch",
            "this", "throw", "try", "typeof", "var", "void", "while", "with", "yield",
            "let", "static", "implements", "interface", "package", "private", "protected", "public"
    };

    private static final String[] C_KEYWORDS = new String[] {
            "auto", "break", "case", "char", "const", "continue", "default", "do",
            "double", "else", "enum", "extern", "float", "for", "goto", "if",
            "int", "long", "register", "return", "short", "signed", "sizeof", "static",
            "struct", "switch", "typedef", "union", "unsigned", "void", "volatile", "while"
    };

    private static final String[] CPP_KEYWORDS = new String[] {
            "auto", "break", "case", "char", "const", "continue", "default", "do",
            "double", "else", "enum", "extern", "float", "for", "goto", "if",
            "int", "long", "register", "return", "short", "signed", "sizeof", "static",
            "struct", "switch", "typedef", "union", "unsigned", "void", "volatile", "while",
            "asm", "bool", "catch", "class", "const_cast", "delete", "dynamic_cast",
            "explicit", "false", "friend", "inline", "mutable", "namespace", "new",
            "operator", "private", "protected", "public", "reinterpret_cast", "static_cast",
            "template", "this", "throw", "true", "try", "typeid", "typename", "using", "virtual"
    };

    private static final String[] CSHARP_KEYWORDS = new String[] {
            "abstract", "as", "base", "bool", "break", "byte", "case", "catch", "char",
            "checked", "class", "const", "continue", "decimal", "default", "delegate",
            "do", "double", "else", "enum", "event", "explicit", "extern", "false",
            "finally", "fixed", "float", "for", "foreach", "goto", "if", "implicit",
            "in", "int", "interface", "internal", "is", "lock", "long", "namespace",
            "new", "null", "object", "operator", "out", "override", "params", "private",
            "protected", "public", "readonly", "ref", "return", "sbyte", "sealed",
            "short", "sizeof", "stackalloc", "static", "string", "struct", "switch",
            "this", "throw", "true", "try", "typeof", "uint", "ulong", "unchecked",
            "unsafe", "ushort", "using", "virtual", "void", "volatile", "while"
    };

    private static String getKeywordsPattern(String language) {
        return switch (language) {
            case "java" -> "\\b(" + String.join("|", JAVA_KEYWORDS) + ")\\b";
            case "python" -> "\\b(" + String.join("|", PYTHON_KEYWORDS) + ")\\b";
            case "javascript" -> "\\b(" + String.join("|", JS_KEYWORDS) + ")\\b";
            case "c" -> "\\b(" + String.join("|", C_KEYWORDS) + ")\\b";
            case "cpp" -> "\\b(" + String.join("|", CPP_KEYWORDS) + ")\\b";
            case "csharp" -> "\\b(" + String.join("|", CSHARP_KEYWORDS) + ")\\b";
            default -> "";
        };
    }

    private static final String PAREN_PATTERN = "\\(|\\)";
    private static final String BRACE_PATTERN = "\\{|\\}";
    private static final String BRACKET_PATTERN = "\\[|\\]";
    private static final String SEMICOLON_PATTERN = "\\;";
    private static final String STRING_PATTERN = "\"([^\"\\\\]|\\\\.)*\"";
    private static final String COMMENT_PATTERN = "//[^\n]*" + "|" + "/\\*(.|\\R)*?\\*/";
    private static final String ANNOTATION_PATTERN = "@[\\w]+";
    private static final String NUMBER_PATTERN = "\\b\\d+[dDlLfF]?\\b";


    private static Pattern buildSyntaxPattern(String language) {
        return Pattern.compile(
                "(?<KEYWORD>" + getKeywordsPattern(language) + ")"
                        + "|(?<PAREN>" + PAREN_PATTERN + ")"
                        + "|(?<BRACE>" + BRACE_PATTERN + ")"
                        + "|(?<BRACKET>" + BRACKET_PATTERN + ")"
                        + "|(?<SEMICOLON>" + SEMICOLON_PATTERN + ")"
                        + "|(?<STRING>" + STRING_PATTERN + ")"
                        + "|(?<COMMENT>" + COMMENT_PATTERN + ")"
                        + "|(?<ANNOTATION>" + ANNOTATION_PATTERN + ")"
                        + "|(?<NUMBER>" + NUMBER_PATTERN + ")"
        );
    }

    private static StyleSpans<Collection<String>> computeSyntaxHighlighting(String text, String language) {
        Pattern PATTERN = buildSyntaxPattern(language);
        Matcher matcher = PATTERN.matcher(text);
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder
                = new StyleSpansBuilder<>();
        while(matcher.find()) {
            String styleClass =
                    matcher.group("KEYWORD") != null ? "keyword" :
                    matcher.group("PAREN") != null ? "paren" :
                    matcher.group("BRACE") != null ? "brace" :
                    matcher.group("BRACKET") != null ? "bracket" :
                    matcher.group("SEMICOLON") != null ? "semicolon" :
                    matcher.group("STRING") != null ? "string" :
                    matcher.group("COMMENT") != null ? "comment" :
                    matcher.group("ANNOTATION") != null ? "annotation" :
                    matcher.group("NUMBER") != null ? "number" :
                    null; /* never happens */ assert styleClass != null;
            spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
            spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastKwEnd = matcher.end();
        }
        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        return spansBuilder.create();
    }

    public void saveConfig() {
        config.save();
    }

    @FXML
    private void performSearch() {
        // Cancel any running search
        if (searchTask != null && searchTask.isRunning()) {
            searchTask.cancel(true);
        }

        // Clear previous results
        searchResults.clear();
        resultsTable.getSortOrder().clear();
        resultsTable.getSortOrder().add(nameCol); // Reset default sort

        // Reset status
        updateStatus("Searching...");

        String query = searchField.getText().trim();
        if (query.isEmpty()) {
            showAlert("Error", "Please enter a search query", Alert.AlertType.ERROR);
            updateStatus("Error: No search query provided");
            return;
        }

        String mode = searchModeCombo.getValue();
        String extension = extensionField.getText().trim();
        String customFolder = searchFolderField.getText().trim();
        boolean isCaseSensitive = caseSensitiveCheck.isSelected();
        boolean isRegex = !regexCheck.isDisabled() && regexCheck.isSelected();

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
                Consumer<String> statusCallback = (status) -> {
                    String truncatedPath = truncatePath(status, config.getStatusPathDepth());
                    Platform.runLater(() -> updateStatus(truncatedPath));
                };

                try {
                    if (mode.equals("Filename")) {
                        searchEngine.searchFilenameRealtime(query, extension, customFolder, filters, maxResults,
                                isCaseSensitive, result -> addResultToTable(result), statusCallback);
                    } else {
                        searchEngine.searchContentRealtime(query, extension, customFolder, filters, maxResults,
                                isCaseSensitive, isRegex, result -> addResultToTable(result), statusCallback);
                    }
                } catch (Exception e) {
                    if (!isCancelled()) {
                        logger.log(Level.SEVERE, "Error during search operation", e);
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
                    logger.log(Level.SEVERE, "Search failed", ex);
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

    private String truncatePath(String path, int depth) {
        if (path == null) return "";
        String[] parts = path.split("[/\\\\]");
        if (parts.length <= depth) {
            return path;
        }
        StringJoiner joiner = new StringJoiner(File.separator);
        for (int i = 0; i < depth; i++) {
            joiner.add(parts[i]);
        }
        joiner.add("...");
        return joiner.toString();
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

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
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
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                    for (FileResult r : searchResults) {
                        writer.printf("\"%s\",\"%s\",%d,\"%s\",\"%s\"%n",
                                r.getName(), r.getPath(), r.getSize(), r.getType(), formatter.format(r.getModified()));
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
        // Apply theme to main window, just in case it was changed in config
        FastSearchApp.applyTheme(searchField.getScene(), config.getTheme());

        // Also update preview if it's open
        if (previewVBox.getChildren().size() > 1 && previewVBox.getChildren().get(1) instanceof VirtualizedScrollPane) {
            @SuppressWarnings("unchecked")
            VirtualizedScrollPane<CodeArea> scrollPane = (VirtualizedScrollPane<CodeArea>) previewVBox.getChildren().get(1);
            CodeArea codeArea = scrollPane.getContent();
            String stylesheet = FastSearchApp.getStylesheet(config.getTheme());
            if (stylesheet != null) {
                codeArea.getStylesheets().clear();
                codeArea.getStylesheets().add(stylesheet);
            }
        }
    }

    @FXML
    private void showHistoryDialog() {
        HistoryDialog dialog = new HistoryDialog(config);
        Optional<SearchConfig.SearchHistory> result = dialog.showAndWait();
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
        AboutDialog dialog = new AboutDialog(config);
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

        String substring = sizeStr.substring(0, sizeStr.length() - 2);
        if (sizeStr.endsWith("kb")) {
            multiplier = 1024;
            sizeStr = substring;
        } else if (sizeStr.endsWith("mb")) {
            multiplier = 1024 * 1024;
            sizeStr = substring;
        } else if (sizeStr.endsWith("gb")) {
            multiplier = 1024 * 1024 * 1024;
            sizeStr = substring;
        }

        try {
            return (long) (Double.parseDouble(sizeStr.trim()) * multiplier);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String formatSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024.0));
        return String.format("%.2f GB", size / (1024.0 * 1024.0 * 1024.0));
    }
}

