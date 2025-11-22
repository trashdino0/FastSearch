package org.fastsearch;

import javafx.beans.property.*;

import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * File Result Model - Represents a search result file with JavaFX properties
 */
public class FileResult {
    private final StringProperty name;
    private final StringProperty path;
    private final LongProperty size;
    private final StringProperty type;
    private final ObjectProperty<LocalDateTime> modified;
    private final StringProperty sizeFormatted;
    private final StringProperty modifiedFormatted;

    public FileResult(String path) {
        File file = new File(path);

        this.name = new SimpleStringProperty(file.getName());
        this.path = new SimpleStringProperty(path);
        this.type = new SimpleStringProperty(getFileType(path));

        if (file.exists()) {
            this.size = new SimpleLongProperty(file.length());
            LocalDateTime modTime = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(file.lastModified()),
                    ZoneId.systemDefault()
            );
            this.modified = new SimpleObjectProperty<>(modTime);
            this.sizeFormatted = new SimpleStringProperty(formatSize(file.length()));
            this.modifiedFormatted = new SimpleStringProperty(
                    modTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
            );
        } else {
            this.size = new SimpleLongProperty(0);
            this.modified = new SimpleObjectProperty<>(LocalDateTime.now());
            this.sizeFormatted = new SimpleStringProperty("0 B");
            this.modifiedFormatted = new SimpleStringProperty("Unknown");
        }
    }

    private String getFileType(String path) {
        String ext = "";
        int i = path.lastIndexOf('.');
        if (i > 0) {
            ext = path.substring(i + 1).toUpperCase();
        }

        Map<String, String> typeMap = Map.ofEntries(
                Map.entry("PDF", "PDF"),
                Map.entry("DOCX", "Word"),
                Map.entry("DOC", "Word"),
                Map.entry("XLSX", "Excel"),
                Map.entry("XLS", "Excel"),
                Map.entry("PPTX", "PowerPoint"),
                Map.entry("TXT", "Text"),
                Map.entry("JPG", "Image"),
                Map.entry("JPEG", "Image"),
                Map.entry("PNG", "Image"),
                Map.entry("GIF", "Image"),
                Map.entry("MP4", "Video"),
                Map.entry("AVI", "Video"),
                Map.entry("MP3", "Audio"),
                Map.entry("WAV", "Audio"),
                Map.entry("ZIP", "Archive"),
                Map.entry("RAR", "Archive"),
                Map.entry("PY", "Python"),
                Map.entry("JAVA", "Java"),
                Map.entry("JS", "JavaScript"),
                Map.entry("HTML", "HTML"),
                Map.entry("CSS", "CSS")
        );

        return typeMap.getOrDefault(ext, ext.isEmpty() ? "File" : ext);
    }

    private String formatSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024));
        return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
    }

    // JavaFX Property getters (REQUIRED for TableView)
    public StringProperty nameProperty() {
        return name;
    }

    public StringProperty pathProperty() {
        return path;
    }

    public LongProperty sizeProperty() {
        return size;
    }

    public StringProperty typeProperty() {
        return type;
    }

    public ObjectProperty<LocalDateTime> modifiedProperty() {
        return modified;
    }

    public StringProperty sizeFormattedProperty() {
        return sizeFormatted;
    }

    public StringProperty modifiedFormattedProperty() {
        return modifiedFormatted;
    }

    // Standard getters
    public String getName() {
        return name.get();
    }

    // Setters (optional)
    public void setName(String name) {
        this.name.set(name);
    }

    public String getPath() {
        return path.get();
    }

    public void setPath(String path) {
        this.path.set(path);
    }

    public long getSize() {
        return size.get();
    }

    public void setSize(long size) {
        this.size.set(size);
    }

    public String getType() {
        return type.get();
    }

    public void setType(String type) {
        this.type.set(type);
    }

    public LocalDateTime getModified() {
        return modified.get();
    }

    public void setModified(LocalDateTime modified) {
        this.modified.set(modified);
    }

    public String getSizeFormatted() {
        return sizeFormatted.get();
    }

    public String getModifiedFormatted() {
        return modifiedFormatted.get();
    }
}