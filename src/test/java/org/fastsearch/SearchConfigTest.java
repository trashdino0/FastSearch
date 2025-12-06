package org.fastsearch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SearchConfigTest {
    
    private SearchConfig config;

    @BeforeEach
    void setUp() {
        config = new SearchConfig();
    }

    @Test
    void testDefaultMaxResults() {
        assertEquals(1000, config.getMaxResults());
    }

    @Test
    void testSetValidMaxResults() {
        config.setMaxResults(5000);
        assertEquals(5000, config.getMaxResults());
    }

    @Test
    void testSetInvalidMaxResults() {
        assertThrows(IllegalArgumentException.class, () -> config.setMaxResults(0));
        assertThrows(IllegalArgumentException.class, () -> config.setMaxResults(-1));
    }

    @Test
    void testSetMaxResultsMinimumValue() {
        config.setMaxResults(1);
        assertEquals(1, config.getMaxResults());
    }

    @Test
    void testDefaultStatusPathDepth() {
        assertEquals(4, config.getStatusPathDepth());
    }

    @Test
    void testSetValidStatusPathDepth() {
        config.setStatusPathDepth(8);
        assertEquals(8, config.getStatusPathDepth());
    }

    @Test
    void testSetInvalidStatusPathDepth() {
        assertThrows(IllegalArgumentException.class, () -> config.setStatusPathDepth(0));
        assertThrows(IllegalArgumentException.class, () -> config.setStatusPathDepth(-1));
    }

    @Test
    void testSetStatusPathDepthMinimumValue() {
        config.setStatusPathDepth(1);
        assertEquals(1, config.getStatusPathDepth());
    }

    @Test
    void testDefaultTheme() {
        assertEquals("Light", config.getTheme());
    }

    @Test
    void testSetTheme() {
        config.setTheme("Dark");
        assertEquals("Dark", config.getTheme());
    }

    @Test
    void testDefaultExcludePatterns() {
        List<String> patterns = config.getExcludePatterns();
        assertNotNull(patterns);
        assertFalse(patterns.isEmpty());
        assertTrue(patterns.contains("node_modules"));
        assertTrue(patterns.contains(".git"));
    }

    @Test
    void testSetExcludePatterns() {
        List<String> newPatterns = new ArrayList<>();
        newPatterns.add(".svn");
        newPatterns.add("target");
        config.setExcludePatterns(newPatterns);
        
        List<String> retrieved = config.getExcludePatterns();
        assertEquals(2, retrieved.size());
        assertTrue(retrieved.contains(".svn"));
        assertTrue(retrieved.contains("target"));
    }

    @Test
    void testDefaultTextExtensions() {
        List<String> extensions = config.getTextExtensions();
        assertNotNull(extensions);
        assertFalse(extensions.isEmpty());
        assertTrue(extensions.contains(".java"));
        assertTrue(extensions.contains(".txt"));
        assertTrue(extensions.contains(".json"));
    }

    @Test
    void testSetTextExtensions() {
        List<String> newExtensions = new ArrayList<>();
        newExtensions.add(".custom");
        config.setTextExtensions(newExtensions);
        
        List<String> retrieved = config.getTextExtensions();
        assertEquals(1, retrieved.size());
        assertTrue(retrieved.contains(".custom"));
    }

    @Test
    void testWindowDimensions() {
        assertEquals(1000, config.getWindowWidth());
        assertEquals(700, config.getWindowHeight());
        assertEquals(-1, config.getWindowX());
        assertEquals(-1, config.getWindowY());
    }

    @Test
    void testSetWindowDimensions() {
        config.setWindowWidth(1200);
        config.setWindowHeight(800);
        config.setWindowX(100);
        config.setWindowY(50);
        
        assertEquals(1200, config.getWindowWidth());
        assertEquals(800, config.getWindowHeight());
        assertEquals(100, config.getWindowX());
        assertEquals(50, config.getWindowY());
    }

    @Test
    void testAddToHistory() {
        config.addToHistory("Filename", "test.txt", "txt", 5);
        
        List<SearchConfig.SearchHistory> history = config.getHistory();
        assertFalse(history.isEmpty());
        
        SearchConfig.SearchHistory latest = history.get(0);
        assertEquals("Filename", latest.getMode());
        assertEquals("test.txt", latest.getQuery());
        assertEquals("txt", latest.getExtension());
        assertEquals(5, latest.getResultsCount());
    }

    @Test
    void testHistoryMaxSize() {
        // Add 25 entries, should only keep 20
        for (int i = 0; i < 25; i++) {
            config.addToHistory("Filename", "query" + i, "txt", i);
        }
        
        List<SearchConfig.SearchHistory> history = config.getHistory();
        assertEquals(20, history.size());
    }

    @Test
    void testGetExtraFolders() {
        List<String> folders = config.getExtraFolders();
        assertNotNull(folders);
    }

    @Test
    void testSetExtraFolders() {
        List<String> newFolders = new ArrayList<>();
        newFolders.add("C:\\Users\\Documents");
        newFolders.add("C:\\Users\\Downloads");
        config.setExtraFolders(newFolders);
        
        List<String> retrieved = config.getExtraFolders();
        assertEquals(2, retrieved.size());
        assertTrue(retrieved.contains("C:\\Users\\Documents"));
    }

    @Test
    void testSetHistory() {
        List<SearchConfig.SearchHistory> newHistory = new ArrayList<>();
        config.setHistory(newHistory);
        
        assertTrue(config.getHistory().isEmpty());
    }
}
