package org.fastsearch;

import javafx.concurrent.Task;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class SearchEngine {
    private final SearchConfig config;
    private final List<PathMatcher> excludeMatchers;
    private ForkJoinPool forkJoinPool;
    private Task<?> searchTask;

    public SearchEngine(SearchConfig config) {
        this.config = config;
        this.excludeMatchers = new ArrayList<>();
        for (String pattern : config.getExcludePatterns()) {
            try {
                excludeMatchers.add(FileSystems.getDefault().getPathMatcher("glob:**/" + pattern));
            } catch (Exception e) {
                System.err.println("Invalid exclude pattern: " + pattern);
            }
        }
    }

    public void setSearchTask(Task<?> searchTask) {
        this.searchTask = searchTask;
    }

    public void cancelSearch() {
        if (searchTask != null && !searchTask.isDone()) {
            searchTask.cancel(true);
        }
        if (forkJoinPool != null && !forkJoinPool.isShutdown()) {
            forkJoinPool.shutdownNow();
        }
    }

    public void searchFilenameRealtime(String query, String extension, String customFolder, SearchFilters filters,
                                       int maxResults, boolean isCaseSensitive, Consumer<FileResult> resultCallback) {
        forkJoinPool = new ForkJoinPool();
        Set<String> searchRoots = getSearchRoots(customFolder);
        String pattern = buildPattern(query);
        Pattern regex = Pattern.compile(pattern, isCaseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
        Collection<FileResult> allResults = new ConcurrentLinkedQueue<>();

        SearchTask task = new SearchTask(searchRoots, regex, extension, filters, maxResults, resultCallback, allResults, true);
        forkJoinPool.invoke(task);
    }

    public void searchContentRealtime(String text, String extension, String customFolder,
                                      SearchFilters filters, int maxResults, boolean isCaseSensitive,
                                      boolean isRegex, Consumer<FileResult> resultCallback) {
        forkJoinPool = new ForkJoinPool();
        Set<String> searchRoots = getSearchRoots(customFolder);
        String patternString = isRegex ? text : Pattern.quote(text);
        int flags = isCaseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
        Pattern contentPattern = Pattern.compile(patternString, flags);
        Collection<FileResult> allResults = new ConcurrentLinkedQueue<>();

        SearchTask task = new SearchTask(searchRoots, contentPattern, extension, filters, maxResults, resultCallback, allResults, false);
        forkJoinPool.invoke(task);
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

    private boolean searchInFile(Path file, Pattern pattern) {
        try (FileChannel channel = FileChannel.open(file, StandardOpenOption.READ)) {
            if (channel.size() == 0) return false;
            java.nio.MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
            return pattern.matcher(java.nio.charset.StandardCharsets.UTF_8.decode(buffer)).find();
        } catch (Exception e) {
            // File not readable or binary
        }
        return false;
    }

    private boolean shouldExclude(Path path) {
        for (PathMatcher matcher : excludeMatchers) {
            if (matcher.matches(path)) {
                return true;
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

    private class SearchTask extends RecursiveAction {
        private final Collection<String> roots;
        private final Pattern pattern;
        private final String extension;
        private final SearchFilters filters;
        private final int maxResults;
        private final Consumer<FileResult> resultCallback;
        private final Collection<FileResult> allResults;
        private final boolean isFilenameSearch;

        SearchTask(Collection<String> roots, Pattern pattern, String extension, SearchFilters filters, int maxResults,
                   Consumer<FileResult> resultCallback, Collection<FileResult> allResults, boolean isFilenameSearch) {
            this.roots = roots;
            this.pattern = pattern;
            this.extension = extension;
            this.filters = filters;
            this.maxResults = maxResults;
            this.resultCallback = resultCallback;
            this.allResults = allResults;
            this.isFilenameSearch = isFilenameSearch;
        }

        @Override
        protected void compute() {
            if (Thread.currentThread().isInterrupted() || isSearchCancelled()) {
                return;
            }
            List<SearchTask> tasks = new ArrayList<>();
            for (String root : roots) {
                File rootDir = new File(root);
                if (rootDir.exists() && rootDir.isDirectory()) {
                    try (DirectoryStream<Path> stream = Files.newDirectoryStream(rootDir.toPath())) {
                        for (Path path : stream) {
                            if (Thread.currentThread().isInterrupted() || isSearchCancelled() || allResults.size() >= maxResults) {
                                return;
                            }

                            if (Files.isDirectory(path)) {
                                if (!shouldExclude(path)) {
                                    SearchTask task = new SearchTask(Collections.singleton(path.toString()), pattern, extension, filters, maxResults, resultCallback, allResults, isFilenameSearch);
                                    tasks.add(task);
                                }
                            } else {
                                processFile(path);
                            }
                        }
                    } catch (IOException e) {
                        if (!isSearchCancelled()) {
                            System.err.println("Error reading directory: " + root);
                        }
                    }
                }
            }
            invokeAll(tasks);
        }

        private void processFile(Path file) {
            if (Thread.currentThread().isInterrupted() || isSearchCancelled() || allResults.size() >= maxResults) {
                return;
            }

            if (shouldExclude(file)) return;

            if (extension != null && !extension.isEmpty()) {
                if (!file.getFileName().toString().toLowerCase().endsWith("." + extension.toLowerCase())) {
                    return;
                }
            }

            if (isFilenameSearch) {
                if (pattern.matcher(file.getFileName().toString()).find()) {
                    addResult(file);
                }
            } else {
                if (isTextFile(file) && searchInFile(file, pattern)) {
                    addResult(file);
                }
            }
        }

        public boolean isSearchCancelled() {
            return searchTask != null && searchTask.isCancelled();
        }

        private void addResult(Path file) {
            try {
                FileResult result = new FileResult(file.toString());
                if (filters.matches(result)) {
                    allResults.add(result);
                    resultCallback.accept(result);
                }
            } catch (Exception e) {
                if (!isSearchCancelled()) {
                    System.err.println("Error processing " + file + ": " + e.getMessage());
                }
            }
        }
    }
}
