package org.fastsearch;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;


// ============================================
// SEARCH CONFIG
// ============================================
class SearchConfig {
    private static final String CONFIG_FILE = System.getProperty("user.home") + "/.fastsearch.json";

    private int maxResults = 1000;
    private List<String> excludePatterns = new ArrayList<>(Arrays.asList(
            "node_modules", ".git", "__pycache__", "*.tmp"
    ));
    private List<String> extraFolders = new ArrayList<>();
    private List<SearchHistory> history = new ArrayList<>();

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
                    .setPrettyPrinting()
                    .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                    .create();
            gson.toJson(this, writer);
        } catch (Exception e) {
            System.err.println("Failed to save config: " + e.getMessage());
        }
    }

    public void addToHistory(String mode, String query, String extension, int resultsCount) {
        history.add(0, new SearchHistory(mode, query, extension, resultsCount));
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

    public List<SearchHistory> getHistory() {
        return history;
    }

    public void setHistory(List<SearchHistory> history) {
        this.history = history;
    }
}