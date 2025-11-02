package com.fastsearch;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Search Engine - Handles filename and content searches with real-time updates
 */
public class SearchEngine {
    private final SearchConfig config;

    public SearchEngine(SearchConfig config) {
        this.config = config;
    }

    /**
     * Search for files by filename pattern with real-time updates
     */
    public void searchFilenameRealtime(String query, String customFolder, SearchFilters filters,
                                       int maxResults, Consumer<FileResult> resultCallback) {
        Set<String> searchRoots = getSearchRoots(customFolder);
        String pattern = buildPattern(query);
        Pattern regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);

        List<FileResult> allResults = Collections.synchronizedList(new ArrayList<>());

        for (String root : searchRoots) {
            if (allResults.size() >= maxResults) break;
            searchInDirectoryRealtime(new File(root), regex, filters, allResults, maxResults, resultCallback);
        }
    }

    /**
     * Search for text content within files with real-time updates
     */
    public void searchContentRealtime(String text, String extension, String customFolder,
                                      SearchFilters filters, int maxResults, Consumer<FileResult> resultCallback) {
        Set<String> searchRoots = getSearchRoots(customFolder);
        Pattern contentPattern = Pattern.compile(Pattern.quote(text), Pattern.CASE_INSENSITIVE);

        List<FileResult> allResults = Collections.synchronizedList(new ArrayList<>());

        for (String root : searchRoots) {
            if (allResults.size() >= maxResults) break;
            searchContentInDirectoryRealtime(new File(root), contentPattern, extension, filters,
                    allResults, maxResults, resultCallback);
        }
    }

    private void searchInDirectoryRealtime(File dir, Pattern pattern, SearchFilters filters,
                                           List<FileResult> allResults, int maxResults,
                                           Consumer<FileResult> resultCallback) {
        if (!dir.exists() || !dir.isDirectory() || allResults.size() >= maxResults) {
            return;
        }

        try {
            Files.walkFileTree(dir.toPath(), EnumSet.noneOf(FileVisitOption.class), Integer.MAX_VALUE,
                    new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                            if (allResults.size() >= maxResults) {
                                return FileVisitResult.TERMINATE;
                            }

                            String filename = file.getFileName().toString();

                            if (pattern.matcher(filename).find() && !shouldExclude(file.toString())) {
                                try {
                                    FileResult result = new FileResult(file.toString());
                                    if (filters.matches(result)) {
                                        allResults.add(result);
                                        resultCallback.accept(result);
                                        System.out.println("Found: " + file);
                                    }
                                } catch (Exception e) {
                                    System.err.println("Error processing " + file + ": " + e.getMessage());
                                }
                            }

                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                            if (allResults.size() >= maxResults) {
                                return FileVisitResult.TERMINATE;
                            }
                            if (shouldExclude(dir.toString())) {
                                return FileVisitResult.SKIP_SUBTREE;
                            }
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFileFailed(Path file, IOException exc) {
                            return FileVisitResult.CONTINUE;
                        }
                    });
        } catch (Exception e) {
            System.err.println("Error searching " + dir + ": " + e.getMessage());
        }
    }

    private void searchContentInDirectoryRealtime(File dir, Pattern contentPattern, String extension,
                                                  SearchFilters filters, List<FileResult> allResults,
                                                  int maxResults, Consumer<FileResult> resultCallback) {
        if (!dir.exists() || !dir.isDirectory() || allResults.size() >= maxResults) {
            return;
        }

        try {
            Files.walkFileTree(dir.toPath(), EnumSet.noneOf(FileVisitOption.class), Integer.MAX_VALUE,
                    new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                            if (allResults.size() >= maxResults) {
                                return FileVisitResult.TERMINATE;
                            }

                            if (extension != null && !extension.isEmpty()) {
                                String filename = file.getFileName().toString();
                                if (!filename.toLowerCase().endsWith("." + extension.toLowerCase())) {
                                    return FileVisitResult.CONTINUE;
                                }
                            }

                            if (isTextFile(file) && !shouldExclude(file.toString())) {
                                if (searchInFile(file, contentPattern)) {
                                    try {
                                        FileResult result = new FileResult(file.toString());
                                        if (filters.matches(result)) {
                                            allResults.add(result);
                                            resultCallback.accept(result);
                                            System.out.println("Found content in: " + file);
                                        }
                                    } catch (Exception e) {
                                        System.err.println("Error processing " + file + ": " + e.getMessage());
                                    }
                                }
                            }

                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                            if (allResults.size() >= maxResults) {
                                return FileVisitResult.TERMINATE;
                            }
                            if (shouldExclude(dir.toString())) {
                                return FileVisitResult.SKIP_SUBTREE;
                            }
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFileFailed(Path file, IOException exc) {
                            return FileVisitResult.CONTINUE;
                        }
                    });
        } catch (Exception e) {
            System.err.println("Error searching content in " + dir + ": " + e.getMessage());
        }
    }

    private boolean searchInFile(Path file, Pattern pattern) {
        try (BufferedReader reader = Files.newBufferedReader(file)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (pattern.matcher(line).find()) {
                    return true;
                }
            }
        } catch (Exception e) {
            // File not readable or binary
        }
        return false;
    }

    private boolean isTextFile(Path file) {
        String name = file.getFileName().toString().toLowerCase();
        String[] textExtensions = {
                ".txt", ".log", ".md", ".py", ".java", ".js", ".ts", ".jsx", ".tsx",
                ".html", ".css", ".xml", ".json", ".yaml", ".yml", ".ini", ".conf",
                ".c", ".cpp", ".h", ".hpp", ".cs", ".go", ".rs", ".rb", ".php",
                ".sh", ".bat", ".sql", ".properties", ".gradle", ".maven"
        };

        for (String ext : textExtensions) {
            if (name.endsWith(ext)) {
                return true;
            }
        }

        try {
            return Files.size(file) < 10 * 1024 * 1024;
        } catch (IOException e) {
            return false;
        }
    }

    private boolean shouldExclude(String path) {
        String lowerPath = path.toLowerCase();
        for (String pattern : config.getExcludePatterns()) {
            String lowerPattern = pattern.toLowerCase().replace("*", "");

            String[] pathParts = path.replace("\\", "/").split("/");
            for (String part : pathParts) {
                if (part.toLowerCase().contains(lowerPattern)) {
                    return true;
                }
            }
        }
        return false;
    }

    private String buildPattern(String query) {
        if (query.contains("*") || query.contains("?")) {
            return query.replace(".", "\\.")
                    .replace("*", ".*")
                    .replace("?", ".");
        }
        return ".*" + Pattern.quote(query) + ".*";
    }

    private Set<String> getSearchRoots(String customFolder) {
        Set<String> roots = new LinkedHashSet<>();

        if (customFolder != null && !customFolder.isEmpty()) {
            File customDir = new File(customFolder);
            if (customDir.exists() && customDir.isDirectory()) {
                roots.add(customDir.getAbsolutePath());
                return roots;
            }
        }

        String userHome = System.getProperty("user.home");
        String[] commonFolders = {
                "Desktop", "Documents", "Downloads", "Pictures", "Videos", "Music"
        };

        for (String folder : commonFolders) {
            File dir = new File(userHome, folder);
            if (dir.exists() && dir.isDirectory()) {
                roots.add(dir.getAbsolutePath());
            }
        }

        for (String extra : config.getExtraFolders()) {
            File dir = new File(extra);
            if (dir.exists() && dir.isDirectory()) {
                roots.add(dir.getAbsolutePath());
            }
        }

        if (roots.isEmpty()) {
            roots.add(userHome);
        }

        return roots;
    }
}