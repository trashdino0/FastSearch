package org.fastsearch;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// ============================================
// SEARCH CONFIG
// ============================================
public class SearchConfig {
    private static final String CONFIG_FILE = System.getProperty("user.home") + "/.fastsearch.json";

    private int maxResults = 1000;
    private List<String> excludePatterns = new ArrayList<>(Arrays.asList(
            "node_modules", ".git", "__pycache__", "*.tmp"
    ));
    private List<String> extraFolders = new ArrayList<>();
    private List<String> textExtensions = new ArrayList<>(Arrays.asList(
            ".txt", ".log", ".md", ".py", ".java", ".js", ".ts", ".jsx", ".tsx",
            ".html", ".css", ".xml", ".json", ".yaml", ".yml", ".ini", ".conf",
            ".c", ".cpp", ".h", ".hpp", ".cs", ".go", ".rs", ".rb", ".php",
            ".sh", ".bat", ".sql", ".properties", ".gradle", ".maven"
    ));
    private List<SearchHistory> history = new ArrayList<>();
    private String theme = "Light";
    private int statusPathDepth = 4;

    public static SearchConfig load() {
        File configFile = new File(CONFIG_FILE);
        if (!configFile.exists()) {
            SearchConfig config = new SearchConfig();
            config.save();
            return config;
        }

        try (Reader reader = new FileReader(configFile)) {
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                    .create();
            SearchConfig config = gson.fromJson(reader, SearchConfig.class);
            if (config == null) {
                System.err.println("Config file was empty or invalid, creating default config.");
                return new SearchConfig();
            }
            return config;
        } catch (Exception e) {
            System.err.println("Failed to load config: " + e.getMessage());
            return new SearchConfig();
        }
    }

    public void save() {
        try (Writer writer = new FileWriter(CONFIG_FILE)) {
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                    .setPrettyPrinting()
                    .create();
            gson.toJson(this, writer);
        } catch (Exception e) {
            System.err.println("Failed to save config: " + e.getMessage());
        }
    }

    public void addToHistory(String mode, String query, String extension, int resultsCount) {
        history.addFirst(new SearchHistory(mode, query, extension, resultsCount));
        if (history.size() > 20) {
            history = history.subList(0, 20);
        }
        save();
    }

    // Getters and setters
    public int getMaxResults() {
        return maxResults;
    }

    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }

    public List<String> getExcludePatterns() {
        return excludePatterns;
    }

    public void setExcludePatterns(List<String> excludePatterns) {
        this.excludePatterns = excludePatterns;
    }

    public List<String> getExtraFolders() {
        return extraFolders;
    }

    public void setExtraFolders(List<String> extraFolders) {
        this.extraFolders = extraFolders;
    }

    public List<String> getTextExtensions() {
        return textExtensions;
    }

    public void setTextExtensions(List<String> textExtensions) {
        this.textExtensions = textExtensions;
    }

    public List<SearchHistory> getHistory() {
        return history;
    }

    public void setHistory(List<SearchHistory> history) {
        this.history = history;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public int getStatusPathDepth() {
        return statusPathDepth;
    }

    public void setStatusPathDepth(int statusPathDepth) {
        this.statusPathDepth = statusPathDepth;
    }

    // ============================================
    // SEARCH HISTORY
    // ============================================
    public static class SearchHistory {
        private final String mode;
        private final String query;
        private final String extension;
        private final LocalDateTime timestamp;
        private final int resultsCount;

        public SearchHistory(String mode, String query, String extension, int resultsCount) {
            this.mode = mode;
            this.query = query;
            this.extension = extension;
            this.timestamp = LocalDateTime.now();
            this.resultsCount = resultsCount;
        }

        public String getDisplayText() {
            String ext = extension != null && !extension.isEmpty() ? " (." + extension + ")" : "";
            return String.format("[%s] %s: %s%s - %d results",
                    timestamp.format(DateTimeFormatter.ofPattern("MM-dd HH:mm")),
                    mode, query, ext, resultsCount);
        }

        public String getMode() {
            return mode;
        }

        public String getQuery() {
            return query;
        }

        public String getExtension() {
            return extension;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public int getResultsCount() {
            return resultsCount;
        }
    }
}