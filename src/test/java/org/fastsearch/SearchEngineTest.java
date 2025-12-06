package org.fastsearch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

public class SearchEngineTest {

    @TempDir
    Path tempDir;

    private SearchConfig config;
    private SearchEngine searchEngine;

    @BeforeEach
    void setUp() throws IOException {
        config = new SearchConfig();
        // Clear exclude patterns before each test
        config.getExcludePatterns().clear();
        searchEngine = new SearchEngine(config);

        // Create a diverse set of test files
        Files.writeString(tempDir.resolve("testFile1.txt"), "Hello World from test file 1.");
        Files.writeString(tempDir.resolve("TESTFILE_UPPER.txt"), "Case sensitive test.");
        Files.writeString(tempDir.resolve("testFile2.log"), "Another file with hello content.");

        // For size filters
        Files.write(tempDir.resolve("small.txt"), new byte[100]); // 100 bytes
        Files.write(tempDir.resolve("large.log"), new byte[2048]); // 2 KB

        // For date filters and subdirectories
        Files.createDirectory(tempDir.resolve("subdir"));
        Files.writeString(tempDir.resolve("subdir/testFile3.txt"), "This is a file in a subdirectory.");

        // For exclusion
        Files.createDirectory(tempDir.resolve("excluded_dir"));
        Files.writeString(tempDir.resolve("excluded_dir/excluded_file.txt"), "This file should be excluded.");
        Files.writeString(tempDir.resolve("file_to_exclude.tmp"), "This is a temporary file to be excluded.");
    }

    private List<FileResult> runFilenameSearch(String query, String extension, String folder, SearchFilters filters, int maxResults, boolean caseSensitive) {
        List<FileResult> results = new CopyOnWriteArrayList<>();
        searchEngine.searchFilenameRealtime(query, extension, folder, filters, maxResults, caseSensitive, results::add, null);
        return results;
    }

    private List<FileResult> runContentSearch(String query, String extension, String folder, SearchFilters filters, int maxResults, boolean caseSensitive, boolean isRegex) {
        List<FileResult> results = new CopyOnWriteArrayList<>();
        searchEngine.searchContentRealtime(query, extension, folder, filters, maxResults, caseSensitive, isRegex, results::add, null);
        return results;
    }

    @Test
    void testFilenameSearch() {
        List<FileResult> results = runFilenameSearch("testFile1", null, tempDir.toString(), new SearchFilters(), 10, false);
        assertEquals(1, results.size());
        assertEquals("testFile1.txt", results.get(0).getName());
    }

    @Test
    void testFilenameCaseSensitiveSearch() {
        List<FileResult> results = runFilenameSearch("testfile", null, tempDir.toString(), new SearchFilters(), 10, true);
        assertEquals(0, results.size());

        results = runFilenameSearch("TESTFILE_UPPER", null, tempDir.toString(), new SearchFilters(), 10, true);
        assertEquals(1, results.size());
        assertEquals("TESTFILE_UPPER.txt", results.get(0).getName());
    }

    @Test
    void testContentSearch() {
        List<FileResult> results = runContentSearch("hello", null, tempDir.toString(), new SearchFilters(), 10, false, false);
        assertEquals(2, results.size());
        assertTrue(results.stream().anyMatch(r -> r.getName().equals("testFile1.txt")));
        assertTrue(results.stream().anyMatch(r -> r.getName().equals("testFile2.log")));
    }

    @Test
    void testContentCaseSensitiveSearch() {
        List<FileResult> results = runContentSearch("Hello", null, tempDir.toString(), new SearchFilters(), 10, true, false);
        assertEquals(1, results.size());
        assertEquals("testFile1.txt", results.get(0).getName());
    }
    
    @Test
    void testContentRegexSearch() {
        // Search for "Hello" or "hello"
        List<FileResult> results = runContentSearch("[Hh]ello", null, tempDir.toString(), new SearchFilters(), 10, false, true);
        assertEquals(2, results.size());
    
        // Search for a word starting with 'sub'
        results = runContentSearch("\\bsub.*", null, tempDir.toString(), new SearchFilters(), 10, false, true);
        assertEquals(1, results.size());
        assertEquals("testFile3.txt", results.get(0).getName());
    }

    @Test
    void testExcludeDir() {
        config.getExcludePatterns().add("excluded_dir");
        searchEngine = new SearchEngine(config);
        List<FileResult> results = runContentSearch("This", null, tempDir.toString(), new SearchFilters(), 10, false, false);
        // Should find testFile3.txt and file_to_exclude.tmp, but not excluded_file.txt
        assertEquals(2, results.size());
        assertFalse(results.stream().anyMatch(r -> r.getName().equals("excluded_file.txt")));
    }
    
    @Test
    void testExcludeFilePattern() {
        config.getExcludePatterns().add("*.tmp");
        searchEngine = new SearchEngine(config);
        List<FileResult> results = runContentSearch("This", null, tempDir.toString(), new SearchFilters(), 10, false, false);
        // Should find testFile3.txt and excluded_file.txt, but not file_to_exclude.tmp
        assertEquals(2, results.size());
        assertFalse(results.stream().anyMatch(r -> r.getName().equals("file_to_exclude.tmp")));
    }

    @Test
    void testMinSizeFilter() {
        SearchFilters filters = new SearchFilters();
        filters.setMinSize(1024L); // 1 KB
        List<FileResult> results = runFilenameSearch("large", null, tempDir.toString(), filters, 10, false);
        assertEquals(1, results.size());
        assertEquals("large.log", results.get(0).getName());
    }

    @Test
    void testMaxSizeFilter() {
        SearchFilters filters = new SearchFilters();
        filters.setMaxSize(500L); // 500 bytes
        List<FileResult> results = runFilenameSearch("small", null, tempDir.toString(), filters, 10, false);
        assertEquals(1, results.size());
        assertEquals("small.txt", results.get(0).getName());
    }
    
    @Test
    void testDateFilter() {
        SearchFilters filters = new SearchFilters();
        filters.setModifiedAfter(LocalDateTime.of(2020, 1, 1, 0, 0));
        List<FileResult> results = runFilenameSearch("test", null, tempDir.toString(), filters, 10, false);
        // All files were created now, so all should match
        assertEquals(4, results.size());
    
        filters.setModifiedAfter(LocalDateTime.of(2030, 1, 1, 0, 0));
        results = runFilenameSearch("test", null, tempDir.toString(), filters, 10, false);
        assertEquals(0, results.size());
    }

    @Test
    void testExtensionFilter() {
        List<FileResult> results = runFilenameSearch("test", "log", tempDir.toString(), new SearchFilters(), 10, false);
        assertEquals(1, results.size());
        assertEquals("testFile2.log", results.get(0).getName());
    }

    @Test
    void testEmptyQuery() {
        // Empty query pattern should match all files (matches ".*" pattern)
        List<FileResult> results = runFilenameSearch("", null, tempDir.toString(), new SearchFilters(), 10, false);
        // Empty pattern matches everything, so we expect multiple results
        assertTrue(results.size() > 0);
    }

    @Test
    void testNoResults() {
        List<FileResult> results = runContentSearch("nonexistent_string_xyz", null, tempDir.toString(), new SearchFilters(), 10, false, false);
        assertEquals(0, results.size());
    }
    
    @Test
    void testSearchInEmptyDirectory() throws IOException {
        Path emptyDir = Files.createDirectory(tempDir.resolve("empty_dir"));
        List<FileResult> results = runFilenameSearch("any", null, emptyDir.toString(), new SearchFilters(), 10, false);
        assertEquals(0, results.size());
    }
    
    @Test
    void testDirectoryNotFound() {
        List<FileResult> results = runFilenameSearch("any", null, tempDir.toString() + "/nonexistent_dir", new SearchFilters(), 10, false);
        // Verify that non-existent directory returns empty results
        assertEquals(0, results.size());
    }
    
    @Test
    void testMaxResults() {
        List<FileResult> results = runContentSearch("file", null, tempDir.toString(), new SearchFilters(), 2, false, false);
        assertEquals(2, results.size());
    }
}