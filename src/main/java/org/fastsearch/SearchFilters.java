package org.fastsearch;

import java.time.LocalDateTime;

// ============================================
// SEARCH FILTERS
// ============================================
class SearchFilters {
    private Long minSize;
    private Long maxSize;
    private LocalDateTime modifiedAfter;
    private LocalDateTime modifiedBefore;

    public boolean matches(FileResult result) {
        if (minSize != null && result.getSize() < minSize) return false;
        if (maxSize != null && result.getSize() > maxSize) return false;
        if (modifiedAfter != null && result.getModified().isBefore(modifiedAfter)) return false;
        return modifiedBefore == null || !result.getModified().isAfter(modifiedBefore);
    }

    // Setters
    public void setMinSize(Long minSize) {
        this.minSize = minSize;
    }

    public void setMaxSize(Long maxSize) {
        this.maxSize = maxSize;
    }

    public void setModifiedAfter(LocalDateTime modifiedAfter) {
        this.modifiedAfter = modifiedAfter;
    }

    public void setModifiedBefore(LocalDateTime modifiedBefore) {
        this.modifiedBefore = modifiedBefore;
    }
}