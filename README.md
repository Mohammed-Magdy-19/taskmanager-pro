# Smart Task Manager Pro

A high-performance, multithreaded desktop task and productivity management application built with **Java Swing**, **FlatLaf**, and **SQLite**. Designed with strict MVC separation, SOLID architectural principles, and a Warm Editorial design system.

---

## Key Features

- **Dynamic Task Dashboard & Real-Time Filtering**:
  Live dashboard cards displaying task distribution across status and priority, responsive data table with custom status pill badges, debounced search across titles and descriptions, and combined multi-field filtering.
- **Multithreaded Background Architecture & Reminders**:
  Dedicated background thread pool for all persistence operations off the Event Dispatch Thread (EDT). Precision reminder scheduler using the cancel-before-reschedule pattern with system tray alerts and fallback toast notifications.
- **Resilient Persistence & Backup Engine**:
  Embedded SQLite engine running with WAL (Write-Ahead Logging), 5000ms busy timeout, and foreign key enforcement. Complete snapshot backup and restore capabilities with automatic conflict-free upsert merge strategies.
- **First-Run Configuration Wizard & Dual-Theming**:
  Step-by-step onboarding wizard for profile configuration, storage path selection, and theme preferences (Warm Editorial Light and Dark modes) that switch dynamically without requiring an application restart.

---

## Toolchain & Build Setup

### Prerequisites
- **JDK 21+** (Built and validated on **Oracle JDK 26.0.1**).
- **Apache Maven 3.6.3+**.
- *(Optional for building Windows installers)*: **WiX Toolset v4/v5** (`dotnet tool install --global wix`).

> **Note on JDK 26 & Release 21 Toolchain**:  
> The project compiles bytecode with `<maven.compiler.release>21</maven.compiler.release>` to ensure long-term stability and binary compatibility with libraries (`sqlite-jdbc`, `flatlaf`). Running under JDK 26 provides modern JIT compiler optimizations. When running `jpackage`, it bundles a trimmed runtime based on the installed JDK (JDK 26), delivering zero-dependency native execution without requiring Java to be installed on the client machine.

---

## Building the Application

To compile all sources, execute the full test suite (174 unit and integration tests), and generate the standalone fat JAR:

```bash
mvn clean package
```

The build produces:
- `target/taskmanager-1.0.0.jar` (Executable fat JAR containing all bundled dependencies).

---

## Running the Application

### Option 1: Standalone JAR
Run directly using any Java 21+ runtime:

```bash
java -jar target/taskmanager-1.0.0.jar
```

*On the very first launch in a clean directory, the Configuration Wizard will automatically appear to initialize `config.properties` and the database.*

### Option 2: Native Windows Installer (`.exe`)
To package the native Windows installer with bundled private runtime:

```powershell
jpackage --type exe `
  --name "Smart Task Manager Pro" `
  --app-version 1.0.0 `
  --vendor "Mohammed Magdy" `
  --input target `
  --main-jar taskmanager-1.0.0.jar `
  --main-class com.taskmanager.MainApp `
  --win-dir-chooser `
  --win-menu `
  --win-shortcut `
  --dest dist
```

Output:
- `dist/Smart Task Manager Pro-1.0.0.exe` (Self-contained native Windows setup executable).

---

## Quality & Testing

The application includes comprehensive test coverage across layers:
- **Unit Tests**: Mockito-verified service layer interactions (`TaskServiceImplTest`), notification dispatching (`NotificationServiceImplTest`), validation boundary checks (`TaskValidatorTest`), and thread pool management (`ThreadPoolManagerTest`).
- **Integration Tests**: Real SQLite file regression testing (`TaskRepositoryIntegrationTest`, `DatabaseConnectionTest`) asserting pragma enforcement (WAL, busy_timeout, foreign_keys).
- **End-to-End Tests**: Full stack tests (`TaskServiceEndToEndIntegrationTest`) verifying Service $\rightarrow$ Repository $\rightarrow$ SQLite round-trips without test doubles.
- **Concurrency Tests**: Multi-threaded stress loops (`ConcurrencyStressTest`, `BackupConcurrencyTest`, `SearchFilterTest`) verifying EDT safety and race condition immunity.
- **Manual Verification**: Detailed 12-step regression script available in [`docs/manual-test-checklist.md`](docs/manual-test-checklist.md).
