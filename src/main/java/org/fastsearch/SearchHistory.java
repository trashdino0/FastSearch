package org.fastsearch;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


// ============================================
// SEARCH HISTORY
// ============================================
class SearchHistory {
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

    // Getters
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
