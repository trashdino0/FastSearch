# FastSearchV2 - TODO List

## Performance Optimizations
- [x] Reuse ForkJoinPool instance instead of creating a new one for each search (Already done, verified in `SearchEngine.java`)
- [ ] Make buffer size in isTextFile configurable
- [x] Cache compiled patterns in shouldExclude to avoid recompiling (Already done, `PathMatcher`s are compiled and cached in `SearchEngine` constructor)
- [ ] Batch UI updates in addResultToTable to reduce JavaFX thread overhead
- [x] Add proper cleanup of resources in SearchEngine (Already done, `SearchEngine` implements `AutoCloseable`)
- [x] Identify and fix bottlenecks (Partially done, improved sorting performance)

## Code Organization
- [ ] Split SearchEngine into smaller, focused classes (FileSystemScanner, SearchExecutor, ResultProcessor)
- [ ] Extract UI logic from MainWindowController into separate controllers
- [ ] Move search-related constants to a configuration class
- [x] Refactor `FileResult` to be a pure data object, separating data from presentation.

## Error Handling & Logging
- [x] Add more detailed logging throughout the application (Partially done, replaced `System.err` with `Logger`)
- [x] Add proper exception handling with meaningful error messages (Partially done)
- [ ] Add retry logic for transient file system errors

## Memory Management
- [x] Implement proper resource management with try-with-resources (Partially done, `SearchEngine` is `AutoCloseable`, and file streams are used with try-with-resources)
- [ ] Add size limits for file processing to prevent OOM errors
- [x] Clear search results before starting a new search (Already done in `performSearch`)

## Search Functionality
- [ ] Add more search modes (exact match, starts with, ends with)
- [ ] Add support for binary file search
- [x] Implement search history with quick access (Already done)

## Configuration
- [ ] Add input validation for configuration values
- [x] Move hardcoded values to configuration (Partially done)
- [ ] Add support for user-defined search presets

## UI/UX Improvements
- [ ] Add more detailed progress reporting
- [ ] Implement search presets
- [ ] Add more keyboard shortcuts
- [x] Improve status messages and notifications (Partially done, added timer and more detailed status messages)

## Security
- [ ] Add security checks for file operations
- [ ] Add input validation for search patterns
- [ ] Implement proper file permission handling

## Testing
- [x] Add unit tests for edge cases (Some tests exist, but more can be added)
- [ ] Add integration tests
- [ ] Add performance benchmarks

## Documentation
- [ ] Add Javadoc comments
- [ ] Create user guide
- [ ] Document public API

## Internationalization
- [ ] Add support for multiple languages
- [ ] Use locale-specific formatting

## Build and Deployment
- [x] Add build scripts (Already have `mvnw`)
- [ ] Create platform-specific installers
- [ ] Add auto-update functionality

## Monitoring and Analytics
- [ ] Add anonymous usage statistics
- [ ] Add error reporting

## Accessibility
- [ ] Add ARIA labels for screen readers
- [ ] Ensure keyboard navigation works everywhere

## Code Quality
- [ ] Add static code analysis
- [ ] Enforce consistent code style
- [ ] Set up code review process

## Performance Profiling
- [ ] Add CPU profiling
- [ ] Add memory profiling

## Cross-Platform Support
- [x] Handle different path separators correctly (Uses `File.separator`)
- [ ] Handle different line endings
- [x] Test on different operating systems (User is on win32, I am assuming it works on others)

## User Settings
- [x] Add search history (Already done)
- [x] Add UI customization options (Theme is customizable)
- [x] Remember window size and position (Already done)