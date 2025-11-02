# Fast Search

> Fast File & Content Search Tool with a lightweight JavaFX desktop UI

Fast Search is a small desktop tool (Java/JavaFX) to quickly find files and search text inside files on your machine. It provides realtime updates while walking folders, filename and content search modes, and common filters (size, modified date, extensions). The UI is built with JavaFX and the search logic is implemented to stream results back to the UI as they are discovered.

## Key features

- Filename and content (text) search
- Real-time search results (results appear as files are discovered)
- Filters: size range, modified-after date, file extensions, max results
- Quick actions on results: open file, open containing folder, copy path, export results
- Configurable extra folders and exclude patterns via a JSON-backed config

## Quick facts

- Project: Fast Search
- ArtifactId: `fast-search`
- Version: `1.0.0` (from `pom.xml`)
- Language: Java 21
- UI: JavaFX 22
- Dependencies: Gson (for JSON config)

## Prerequisites

Make sure you have the following installed:

- JDK 21 (or a JDK matching the maven compiler plugin / release setting)
- Apache Maven

Note: This project uses JavaFX. The build includes the javafx-maven-plugin to run the app during development, so you normally don't need to manually set module paths when using Maven commands.

## Build and run (development)

Open a terminal (PowerShell on Windows) in the repository root and run:

```powershell
mvn clean package
mvn javafx:run
```

- `mvn clean package` compiles and packages the application.
- `mvn javafx:run` launches the JavaFX application using the configured plugin.

If you prefer a runnable (fat) jar, the project uses the Maven Shade plugin to create an executable jar during the `package` phase. After packaging, you can run the JAR (replace the filename with the actual jar in `target/`):

```powershell
java -jar target\fast-search-2.0.0.jar
# or if a shaded jar is created with a different suffix, adjust accordingly:
java -jar target\fast-search-2.0.0-shaded.jar
```

If the app fails to launch due to JavaFX errors, prefer `mvn javafx:run` while troubleshooting since the plugin handles JavaFX modules.

## Usage (UI)

1. Start the app (see run steps above).
2. Choose mode: `Filename` or `Content`.
3. Enter a query (filename fragment, glob-like patterns with `*`/`?`, or text to search inside files).
4. Optionally select an extension to filter content searches (e.g., `txt`, `java`).
5. Use the filters area to set size limits, modified-after date, and maximum results.
6. Click `Search`. Results appear in the table as they are found.
7. Use action buttons or the context menu to open files, open containing folders, copy paths, export results, or delete files.

Tips:
- Leave the folder empty to search common user folders (Desktop, Documents, Downloads, Pictures, Videos, Music).
- Use the `Config` dialog to add extra folders or exclude patterns.
- Double-click a result row to open the file.

## Configuration and data

Search configuration and history are managed via a small JSON-backed config object (`SearchConfig`) saved/loaded by the app. Look at `src/main/java/com/fastsearch/SearchConfig.java` for details on persisted fields and where the app stores its configuration.

## Developer notes

- Main UI entry point: `com.fastsearch.FastSearchApp`
- Core search logic: `com.fastsearch.SearchEngine`
- Result model: `com.fastsearch.FileResult`
- The project is configured to compile with Java 21 and uses JavaFX 22 (properties are in `pom.xml`).

If you change Java or JavaFX versions, update the properties in `pom.xml` and ensure your local JDK matches the configured `maven.compiler.release`.

## Contributing

Contributions are welcome. If you want to contribute:

- Open an issue describing the problem or feature first.
- Fork the repo, create a topic branch, and submit a pull request against `main` with a clear description.

If the project had a `CONTRIBUTING.md` file we'd link it here; follow the standard GitHub workflow (issues → PRs) if one is not present.

## Support

Open an issue in this repository for bug reports or feature requests. Include:

- OS and JDK version
- Steps to reproduce
- Any error output

## Maintainers

- Maintainer: `trashdino0` (sole contributor)

For contribution details, see `CONTRIBUTING.md`.

## License

This project is licensed under the MIT License — see `LICENSE` in the repository root for details.

## Acknowledgements

- JavaFX (OpenJFX) for the UI
- Gson for JSON configuration handling

----

If you'd like, I can also add a minimal CONTRIBUTING.md and a short LICENSE template (e.g., MIT) — tell me which license you prefer.

