# 🚀 FastSearch

[![Java](https://img.shields.io/badge/Java-8%2B-orange?logo=openjdk)](https://www.java.com)
[![License](https://img.shields.io/badge/License-MIT-blue)](https://opensource.org/licenses/MIT)
[![Status](https://img.shields.io/badge/Status-Active-success)](https://github.com/trashdino0/FastSearch)

A high-performance, multi-threaded Java utility for recursively scanning directories to find files by keyword, with built-in support for [Discord webhook](https://support.discord.com/hc/en-us/articles/228383668-Intro-to-Webhooks) notifications.

## 🌟 Key Features

* **Multi-threaded Scanning**: Uses a `ThreadPoolExecutor` to scan multiple directories in parallel, dramatically speeding up searches on large drives.
* **Recursive Search**: Dives deep into all subfolders from a given starting point.
* **Discord Webhook Integration**: Automatically sends a list of found files directly to a Discord channel, perfect for remote monitoring or logging.
* **Performance Metrics**: Logs the total search time in milliseconds to the console.
* **Lightweight**: No external dependencies. Runs on pure Java 8+.

## ⚙️ How It Works

1.  **Initialization**: A fixed-size thread pool is created.
2.  **Recursive Tasks**: The search begins at the specified `directoryPath`. When it encounters a new subdirectory, it submits a new search task for that directory to the thread pool.
3.  **Parallel Execution**: The thread pool executes these tasks concurrently, splitting the workload.
4.  **Thread-Safe Collection**: All found file paths are added to a synchronized `List` to prevent race conditions.
5.  **Completion**: The main thread waits for all search tasks to complete.
6.  **Reporting**: If any files are found, the results are formatted into a single message and sent as a JSON payload to the specified Discord webhook URL.

## 🏁 Getting Started

### Prerequisites

* [Java 8 (or newer) JDK](https://www.oracle.com/java/technologies/downloads/)
* [Git](https://git-scm.com/downloads)

### 1. Clone the Repository

```bash
git clone [https://github.com/trashdino0/FastSearch.git](https://github.com/trashdino0/FastSearch.git)
cd FastSearch
```

### 2. Configure the Search

Before you can run the tool, you **must** edit the hardcoded values in `FastSearch/src/me/trashdino/fastsearch/Main.java`.

```java
// Open FastSearch/src/me/trashdino/fastsearch/Main.java

public class Main {
    public static void main(String[] args) throws InterruptedException {

        // ...
        
        // --- ⬇️ CONFIGURE THESE VALUES ⬇️ ---

        // 1. Set the starting directory for the search (e.g., "C:/" or "/home/user")
        String directoryPath = "C:/"; 

        // 2. Set the keyword to search for (case-insensitive)
        String keyword = "killaura"; 

        // 3. Paste your Discord Webhook URL here
        String webhookUrl = "YOUR_WEBHOOK_URL"; 

        // 4. (Optional) Adjust the number of threads for scanning
        int maxThreads = 10; 

        // --- ⬆️ END OF CONFIGURATION ⬆️ ---
        
        // ...
    }
}
```

### 3. Compile and Run

The project does not currently use a build tool like Maven or Gradle. You can compile and run it directly from the terminal.

```bash
# 1. Navigate to the source directory
cd FastSearch/src

# 2. Compile the Java file
javac me/trashdino/fastsearch/Main.java

# 3. Run the compiled class
java me.trashdino.fastsearch.Main
```

### Example Output

**Console:**
```
Searching for files containing 'killaura' in C:/...
Search completed in 45720ms
Found 3 files. Sending to webhook...
Webhook sent successfully!
```

**Discord Channel:**
> **FastSearch Results:**
> Found 3 file(s) containing 'killaura':
> - `C:\Users\Admin\Documents\old-plugins\killaura.jar`
> - `C:\Windows\Prefetch\KILLAURA.EXE-A30D987C.pf`
> - `C:\Java\Projects\killaura-detector\README.md`

## 🚧 Future Improvements (To-Do)

This project is a great proof-of-concept. Here are some planned features to make it more flexible:

* [ ] **Command-Line Arguments**: Accept the directory path, keyword, and webhook URL as arguments instead of hardcoding them.
* [ ] **Configuration File**: Use a `.properties` or `config.json` file for settings.
* [ ] **Wildcard/Regex Support**: Allow more complex search patterns beyond a simple keyword.
* [ ] **Executable JAR**: Package the tool as a runnable `.jar` file for easier distribution.

## 🤝 Contributing

Pull requests are welcome! If you'd like to help with the "To-Do" list or have other improvements, feel free to fork the repo and submit a PR.

1.  Fork the Project
2.  Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3.  Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4.  Push to the Branch (`git push origin feature/AmazingFeature`)
5.  Open a Pull Request

## 📜 License

Distributed under the MIT License. See the badge at the top for more information.
