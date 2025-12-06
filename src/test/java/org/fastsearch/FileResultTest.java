package org.fastsearch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class FileResultTest {

    @TempDir
    Path tempDir;

    private Path testFile;

    @BeforeEach
    void setUp() throws Exception {
        testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "test content");
    }

    @Test
    void testFileResultName() {
        FileResult result = new FileResult(testFile.toString());
        assertEquals("test.txt", result.getName());
    }

    @Test
    void testFileResultPath() {
        FileResult result = new FileResult(testFile.toString());
        assertEquals(testFile.toString(), result.getPath());
    }

    @Test
    void testFileResultType() {
        FileResult result = new FileResult(testFile.toString());
        assertEquals("Text", result.getType());
    }

    @Test
    void testFileResultSize() {
        FileResult result = new FileResult(testFile.toString());
        assertTrue(result.getSize() > 0);
    }

    @Test
    void testFileResultSizeFormatted() {
        FileResult result = new FileResult(testFile.toString());
        String formatted = result.getSizeFormatted();
        assertNotNull(formatted);
        assertFalse(formatted.isEmpty());
    }

    @Test
    void testFileResultModified() {
        FileResult result = new FileResult(testFile.toString());
        LocalDateTime modified = result.getModified();
        assertNotNull(modified);
    }

    @Test
    void testFileResultModifiedFormatted() {
        FileResult result = new FileResult(testFile.toString());
        String formatted = result.getModifiedFormatted();
        assertNotNull(formatted);
        // Should be in format yyyy-MM-dd HH:mm
        assertTrue(formatted.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}"));
    }

    @Test
    void testFileTypeDetection() throws Exception {
        // Test Java file
        Path javaFile = tempDir.resolve("Test.java");
        Files.writeString(javaFile, "public class Test {}");
        FileResult javaResult = new FileResult(javaFile.toString());
        assertEquals("Java", javaResult.getType());

        // Test Python file
        Path pythonFile = tempDir.resolve("script.py");
        Files.writeString(pythonFile, "print('hello')");
        FileResult pythonResult = new FileResult(pythonFile.toString());
        assertEquals("Python", pythonResult.getType());

        // Test JSON file
        Path jsonFile = tempDir.resolve("config.json");
        Files.writeString(jsonFile, "{}");
        FileResult jsonResult = new FileResult(jsonFile.toString());
        assertEquals("JSON", jsonResult.getType());

        // Test HTML file
        Path htmlFile = tempDir.resolve("index.html");
        Files.writeString(htmlFile, "<html></html>");
        FileResult htmlResult = new FileResult(htmlFile.toString());
        assertEquals("HTML", htmlResult.getType());
    }

    @Test
    void testSizeFormattingBytes() throws Exception {
        // Create a small file
        Path smallFile = tempDir.resolve("small.txt");
        Files.write(smallFile, new byte[512]);
        FileResult result = new FileResult(smallFile.toString());
        assertTrue(result.getSizeFormatted().matches("\\d+ B"));
    }
    @Test
    void testSizeFormattingKB() throws Exception {
        // Create a 10 KB file
        Path mediumFile = tempDir.resolve("medium.txt");
        Files.write(mediumFile, new byte[10240]);
        FileResult result = new FileResult(mediumFile.toString());
        assertTrue(result.getSizeFormatted().contains("KB"));
    }

    @Test
    void testFileWithoutExtension() throws Exception {
        Path noExtFile = tempDir.resolve("README");
        Files.writeString(noExtFile, "readme content");
        FileResult result = new FileResult(noExtFile.toString());
        assertEquals("File", result.getType());
    }

    @Test
    void testFileResultProperties() {
        FileResult result = new FileResult(testFile.toString());
        
        // Test that properties can be accessed
        assertNotNull(result.nameProperty());
        assertNotNull(result.pathProperty());
        assertNotNull(result.sizeProperty());
        assertNotNull(result.typeProperty());
        assertNotNull(result.modifiedProperty());
        assertNotNull(result.sizeFormattedProperty());
        assertNotNull(result.modifiedFormattedProperty());
    }

    @Test
    void testNonExistentFile() {
        Path nonExistentPath = tempDir.resolve("nonexistent.txt");
        FileResult result = new FileResult(nonExistentPath.toString());
        
        assertEquals("nonexistent.txt", result.getName());
        assertEquals(0, result.getSize());
        assertEquals("0 B", result.getSizeFormatted());
        assertEquals("Unknown", result.getModifiedFormatted());
    }

    @Test
    void testDifferentFileTypes() throws Exception {
        // Create various file types and test type detection
        String[] extensions = {"pdf", "docx", "xlsx", "mp3", "mp4", "zip"};
        String[] expectedTypes = {"PDF", "Word", "Excel", "Audio", "Video", "Archive"};

        for (int i = 0; i < extensions.length; i++) {
            Path file = tempDir.resolve("test." + extensions[i]);
            Files.writeString(file, "test");
            FileResult result = new FileResult(file.toString());
            assertEquals(expectedTypes[i], result.getType(), "Type mismatch for extension: " + extensions[i]);
        }
    }
}
