package org.fastsearch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class SearchFiltersTest {

    @TempDir
    Path tempDir;

    private SearchFilters filters;
    private FileResult testResult;

    @BeforeEach
    void setUp() throws Exception {
        filters = new SearchFilters();
        
        // Create a test file
        Path testFile = tempDir.resolve("test.txt");
        Files.write(testFile, new byte[5000]); // 5 KB file
        testResult = new FileResult(testFile.toString());
    }

    @Test
    void testNoFiltersApplied() {
        assertTrue(filters.matches(testResult));
    }

    @Test
    void testMinSizeFilterPass() {
        filters.setMinSize(1000L); // 1 KB
        assertTrue(filters.matches(testResult)); // 5 KB should pass
    }

    @Test
    void testMinSizeFilterFail() {
        filters.setMinSize(10000L); // 10 KB
        assertFalse(filters.matches(testResult)); // 5 KB should fail
    }

    @Test
    void testMaxSizeFilterPass() {
        filters.setMaxSize(10000L); // 10 KB
        assertTrue(filters.matches(testResult)); // 5 KB should pass
    }

    @Test
    void testMaxSizeFilterFail() {
        filters.setMaxSize(1000L); // 1 KB
        assertFalse(filters.matches(testResult)); // 5 KB should fail
    }

    @Test
    void testMinAndMaxSizeFilter() {
        filters.setMinSize(1000L);  // 1 KB
        filters.setMaxSize(10000L); // 10 KB
        assertTrue(filters.matches(testResult)); // 5 KB should pass
    }

    @Test
    void testMinAndMaxSizeFilterOutOfRange() {
        filters.setMinSize(1000L);  // 1 KB
        filters.setMaxSize(3000L);  // 3 KB
        assertFalse(filters.matches(testResult)); // 5 KB should fail (too large)
    }

    @Test
    void testModifiedAfterFilterPass() {
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        filters.setModifiedAfter(yesterday);
        assertTrue(filters.matches(testResult)); // Recent file should pass
    }

    @Test
    void testModifiedAfterFilterFail() {
        LocalDateTime future = LocalDateTime.now().plusDays(1);
        filters.setModifiedAfter(future);
        assertFalse(filters.matches(testResult)); // Recent file should fail
    }

    @Test
    void testModifiedBeforeFilterPass() {
        LocalDateTime future = LocalDateTime.now().plusDays(1);
        filters.setModifiedBefore(future);
        assertTrue(filters.matches(testResult)); // Recent file should pass
    }

    @Test
    void testModifiedBeforeFilterFail() {
        LocalDateTime past = LocalDateTime.now().minusDays(1);
        filters.setModifiedBefore(past);
        assertFalse(filters.matches(testResult)); // Recent file should fail
    }

    @Test
    void testCombinedFilters() {
        filters.setMinSize(1000L);
        filters.setMaxSize(10000L);
        filters.setModifiedAfter(LocalDateTime.now().minusDays(1));
        
        assertTrue(filters.matches(testResult));
    }

    @Test
    void testCombinedFiltersSomeFail() {
        filters.setMinSize(1000L);
        filters.setMaxSize(3000L); // Too small
        filters.setModifiedAfter(LocalDateTime.now().minusDays(1));
        
        assertFalse(filters.matches(testResult));
    }

    @Test
    void testSetMinSize() {
        assertDoesNotThrow(() -> filters.setMinSize(5000L));
        assertTrue(filters.matches(testResult)); // 5KB file should match 5KB min
        
        filters.setMinSize(5001L);
        assertFalse(filters.matches(testResult)); // 5KB file should not match 5001B min
    }
    @Test
    void testSetMaxSize() {
        assertDoesNotThrow(() -> filters.setMaxSize(10000L));
    }

    @Test
    void testSetModifiedAfter() {
        LocalDateTime dateTime = LocalDateTime.now();
        assertDoesNotThrow(() -> filters.setModifiedAfter(dateTime));
    }

    @Test
    void testSetModifiedBefore() {
        LocalDateTime dateTime = LocalDateTime.now();
        assertDoesNotThrow(() -> filters.setModifiedBefore(dateTime));
    }

    @Test
    void testNullMinSize() {
        filters.setMinSize(null);
        assertTrue(filters.matches(testResult));
    }

    @Test
    void testNullMaxSize() {
        filters.setMaxSize(null);
        assertTrue(filters.matches(testResult));
    }

    @Test
    void testNullModifiedAfter() {
        filters.setModifiedAfter(null);
        assertTrue(filters.matches(testResult));
    }

    @Test
    void testNullModifiedBefore() {
        filters.setModifiedBefore(null);
        assertTrue(filters.matches(testResult));
    }

    @Test
    void testZeroSizeFile() throws Exception {
        Path emptyFile = tempDir.resolve("empty.txt");
        Files.write(emptyFile, new byte[0]);
        FileResult emptyResult = new FileResult(emptyFile.toString());
        
        assertTrue(filters.matches(emptyResult));
        
        filters.setMinSize(1L);
        assertFalse(filters.matches(emptyResult));
    }

    @Test
    void testLargeSize() throws Exception {
        Path largeFile = tempDir.resolve("large.txt");
        Files.write(largeFile, new byte[1024 * 1024]); // 1 MB
        FileResult largeResult = new FileResult(largeFile.toString());
        
        filters.setMaxSize(512 * 1024L); // 512 KB
        assertFalse(filters.matches(largeResult));
        
        filters.setMaxSize(2 * 1024 * 1024L); // 2 MB
        assertTrue(filters.matches(largeResult));
    }

    @Test
    void testExactSizeMatch() {
        long exactSize = testResult.getSize();
        filters.setMinSize(exactSize);
        filters.setMaxSize(exactSize);
        assertTrue(filters.matches(testResult));
    }

    @Test
    void testModifiedTodayFilter() throws Exception {
        Path todayFile = tempDir.resolve("today.txt");
        Files.writeString(todayFile, "content");
        FileResult todayResult = new FileResult(todayFile.toString());
        
        LocalDateTime startOfToday = LocalDateTime.now()
                .withHour(0).withMinute(0).withSecond(0).withNano(0);
        
        filters.setModifiedAfter(startOfToday);
        assertTrue(filters.matches(todayResult));
    }
}
