<div align="center">

# FastSearch

**A high-performance, desktop file and content search tool built with JavaFX.**

</div>

<p align="center">
  <img alt="Java Version" src="https://img.shields.io/badge/java-21-blue.svg">
  <img alt="License" src="https://img.shields.io/github/license/your-username/FastSearchV2?style=flat-square&label=license">
  <img alt="Build Status" src="https://img.shields.io/github/actions/workflow/status/your-username/FastSearchV2/build.yml?branch=main&style=flat-square&label=build">
</p>

<p align="center">
  <a href="#about-the-project">About</a> •
  <a href="#key-features">Features</a> •
  <a href="#getting-started">Getting Started</a> •
  <a href="#usage">Usage</a> •
  <a href="#contributing">Contributing</a> •
  <a href="#license">License</a>
</p>

---

## About The Project

**FastSearch** is a desktop utility designed to provide a blazing-fast file content search. Unlike standard file finders that only match filenames, FastSearch digs into the contents of files, making it easy to find code snippets, logs, or documents based on what's inside them. The clean and intuitive UI, built with JavaFX, ensures a smooth user experience.

<p align="center">
  <img src="https://raw.githubusercontent.com/user/repo/main/screenshot.png" alt="screenshot" width="700"/>
  <br>
  <em>Note: You should replace the link above with an actual screenshot of your application.</em>
</p>

### Key Features

- **Multi-threaded Content Search:** Leverages multiple CPU cores to scan directories and files in parallel.
- **Responsive UI:** A non-blocking, fluid user interface built on the modern JavaFX platform.
- **Customizable Search Filters:** Refine your searches with filters for file types, case sensitivity, and more.
- **Search History:** Keeps track of your previous searches for easy access.
- **Configurable:** Save your search settings and preferences with ease.
- **Standalone Package:** Distributed as a single `.jar` file with no external dependencies required to run.

### Tech Stack

- **Java 21:** Leverages the latest features and performance improvements from Java.
- **JavaFX 22:** Powers the modern, clean, and responsive user interface.
- **Maven:** Manages the project's build lifecycle and dependencies.
- **Gson:** Used for serialization and deserialization of search history and configuration.

---

## Getting Started

To get a local copy up and running, follow these steps.

### Prerequisites

- **Java Development Kit (JDK) 21** or later. You can download it from [Adoptium](https://adoptium.net/).

### Build

1.  **Clone the repository:**
    ```sh
    git clone https://github.com/your-username/FastSearchV2.git
    cd FastSearchV2
    ```
    *Remember to replace `your-username` with your GitHub username.*

2.  **Build with the Maven Wrapper:**

    On Windows:
    ```sh
    mvnw.cmd clean package
    ```

    On macOS/Linux:
    ```sh
    ./mvnw clean package
    ```

    This command will compile, test, and package the application into a single executable JAR file at `target/FastSearch-1.0.0.jar`.

---

## Usage

Once you have built the application, you can run it from the command line:

```sh
java -jar target/FastSearch-1.0.0.jar
```

The main UI will appear.

1.  Enter your search term in the **"Search"** field.
2.  Click **"Browse"** to select the base directory where you want to start the search.
3.  Configure additional filters (e.g., `*.java, *.txt`) if needed.
4.  Click the **"Search"** button to begin.
5.  Results will populate in the table as they are found. Double-click a result to open the file.

---

## Contributing

Contributions are what make the open source community such an amazing place to learn, inspire, and create. Any contributions you make are **greatly appreciated**.

If you have a suggestion that would make this better, please fork the repo and create a pull request. You can also simply open an issue with the tag "enhancement".

1.  Fork the Project
2.  Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3.  Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4.  Push to the Branch (`git push origin feature/AmazingFeature`)
5.  Open a Pull Request

---

## License

Distributed under the MIT License. See `LICENSE` for more information.
