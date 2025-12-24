package org.fastsearch;

import javafx.beans.property.*;

import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
        } else {
            this.size = new SimpleLongProperty(0);
            this.modified = new SimpleObjectProperty<>(LocalDateTime.now());
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
}