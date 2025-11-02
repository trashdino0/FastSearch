package com.fastsearch;

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
    private Map<String, String> customApps = new HashMap<>();
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
            return gson.fromJson(reader, SearchConfig.class);
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

    public Map<String, String> getCustomApps() {
        return customApps;
    }

    public void setCustomApps(Map<String, String> customApps) {
        this.customApps = customApps;
    }

    public List<SearchHistory> getHistory() {
        return history;
    }

    public void setHistory(List<SearchHistory> history) {
        this.history = history;
    }
}