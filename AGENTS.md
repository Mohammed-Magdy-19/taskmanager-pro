# Agent Instructions — Task Manager (Java Swing) Project

Paste this into your coding agent's system/project instructions. It governs **how** the agent writes code for this project, not what feature to build next (that's the roadmap).

---

## 0. Token Discipline (read first, applies to every response)
- No restating the task, no "Here's what I'll do" preambles, no summarizing code you just wrote in prose afterward.
- Answer directly: code first, then only a **1–3 line** note if something needs the user's attention (a decision made, a risk, a follow-up needed). No note if nothing needs attention.
- Never regenerate a whole file when a small diff/patch does the job. Prefer targeted edits over full rewrites.
- Don't explain standard Java/Swing/SQL syntax. Only comment on project-specific decisions.
- If a task is ambiguous, make the most reasonable assumption from this doc + the roadmap and state it in one line — don't ask a clarifying question unless truly blocked.

## 1. Architecture (fixed — don't restructure without being asked)
```
config/       → ConfigManager (Singleton), ConfigWizardFrame, wizard/ steps, ConfigurationDialog
model/        → BaseEntity (abstract, Serializable), Task/User/Category, enums/
repository/   → GenericRepository<T>, TaskRepository, DatabaseConnection (Singleton)
service/      → GenericService<T> (abstract), TaskServiceImpl, NotificationServiceImpl,
                BackupManager, SerializationUtil<T>
validation/   → Validator<T>, ValidationResult, TaskValidator, FieldValidators
concurrency/  → ThreadPoolManager (Singleton), ReminderScheduler
event/        → EventBus (Singleton), *Event classes
controller/   → TaskController, DashboardController, SettingsController
view/         → MainFrame, panels, dialogs, theme/ (AppTheme, TranslucentScrollBarUI)
util/         → SwingSafe, Logger
```
**Dependency direction is one-way:** `view → controller → service → repository`. A layer never calls upward or skips a layer (e.g. `view` never touches `repository` directly). If a task seems to require breaking this, flag it in one line instead of doing it silently.

## 2. SOLID — applied concretely to this codebase, not abstractly
- **S — Single Responsibility.** One class, one job. `TaskPanel` renders and forwards UI events only — it never calls `TaskService`/`TaskRepository` directly, `TaskController` does. If a class starts doing two things (e.g. a panel that also validates or persists), split it.
- **O — Open/Closed.** New entity types (e.g. a future `Project`) extend `BaseEntity` and get a new `Repository`/`Service` pair — never add `if (type == X)` branches into `GenericRepository`/`GenericService`. New validation rules go into a new/extended `Validator<T>`, not `if` chains inside services.
- **L — Liskov Substitution.** Any `Validator<T>`, `Repository<T>` implementation, or `GenericService<T>` subclass must be fully substitutable for its base type — no subclass should throw on a case the base contract allows, or silently no-op a method the base guarantees behavior for.
- **I — Interface Segregation.** Keep `TaskService`, `NotificationService`, `Validator<T>` etc. narrow and role-specific. Don't add unrelated methods to an existing interface because it's convenient — create a new small interface instead.
- **D — Dependency Inversion.** `TaskController`/`TaskServiceImpl` depend on interfaces (`TaskService`, `Validator<Task>`), never on concrete Swing classes or SQL directly. Constructor-inject dependencies (pass them in, don't `new` them inside a class) so tests can mock them.

## 3. Design Patterns — where each one belongs (use these, don't invent new ones ad hoc)
| Pattern | Where | Why |
|---|---|---|
| **Singleton** | `ConfigManager`, `DatabaseConnection`, `ThreadPoolManager`, `EventBus` | One instance app-wide; avoid re-creating pools/connections |
| **Generic Repository/DAO** | `GenericRepository<T>` → `TaskRepository` | Shared CRUD contract across entities, SQL specifics isolated per repo |
| **Template Method** | `GenericService<T>` (`create/update/delete` fixed flow, `validateBeforeX`/`afterX` hooks overridden by subclasses) | Shared lifecycle, customizable steps |
| **Observer** | `EventBus` + `*Event` classes; UI panels subscribe, background threads publish | Decouples background threads from Swing components |
| **Strategy** | `Validator<T>` implementations; sort/filter comparators passed into repository queries | Swappable behavior without touching the caller |
| **Factory (lightweight)** | Only if a class needs to construct different concrete types at runtime (e.g. building the right `Step` panel by name) — don't force a Factory where a plain constructor call is clearer | Avoid pattern-for-pattern's-sake |
| **MVC** | `view` / `controller` / `service`+`model` | Whole-app separation of concerns |
| **Builder** | Use only if a class ends up with >4 constructor params (e.g. a future complex `Task` builder) — not needed yet, don't add prematurely | Avoid telescoping constructors |

**Rule:** don't add a pattern that isn't earning its complexity. A pattern used where a simple method would do is itself a maintainability cost — prefer the plainest solution that satisfies SOLID above.

## 4. Code Cleanliness (non-negotiable)
- **No God classes.** If a class exceeds ~200–250 lines or does more than one layer's job, split it.
- **No spaghetti control flow.** Max 2 levels of nested `if`/loop before extracting a private method. Prefer early returns over deep nesting.
- **Method length:** aim under ~25 lines. If a method needs a comment to explain "step 1 / step 2 / step 3," it should probably be 2–3 methods.
- **Naming:** intention-revealing, no abbreviations (`taskRepository` not `tRepo`), booleans read as questions (`isValid`, `hasReminder`).
- **No magic numbers/strings.** Use `AppTheme` constants, enum values, or named constants — not raw `"HIGH"` or `12` scattered in code.
- **Comments — Javadoc on every public class and public method,** explaining *why*, not restating the signature:
  ```java
  /**
   * Persists a task and schedules its reminder. Runs off the EDT — callers
   * must not touch Swing components from the completion callback directly;
   * subscribe to TaskProcessedEvent instead.
   */
  public void createAsync(Task task) { ... }
  ```
  Inline comments only where the *why* isn't obvious from the code itself (e.g. "cancel before reschedule — avoids duplicate reminders on rapid edits").
- **No commented-out dead code** left in commits. No `TODO` without a one-line reason.
- **Consistent exception handling:** never swallow an exception silently (`catch (Exception e) {}`). Always log or rethrow as a domain exception (`ValidationException`, etc.).

## 5. Concurrency & EDT Safety (project-specific, strict)
- Background code (`ThreadPoolManager`, `ScheduledExecutorService` tasks) **never** touches Swing components directly — only reads data and publishes via `EventBus` (which wraps delivery in `SwingUtilities.invokeLater`).
- Shared mutable state only via `ConcurrentHashMap`/`AtomicInteger`/`CopyOnWriteArrayList` — never a plain `HashMap`/`ArrayList` shared across threads.
- All SQLite writes route through `ThreadPoolManager`'s single shared `ExecutorService` — never a raw `new Thread(...)`.
- Use `SwingSafe.assertEDT()` in dev builds at the top of any method mutating a live Swing component, to catch violations early.

## 6. Validation
- Every entity gets one `Validator<T>` used by **both** the UI (inline errors) and the service layer (defense-in-depth) — never duplicate a rule in two places. If a rule changes, it changes in exactly one file.

## 7. Testing Expectations
- Every new `Service` method gets a corresponding unit test (Mockito-mocked repository) before being considered done.
- Every new `Repository` method gets an integration test against a temp SQLite file.
- Don't skip writing the test "to save time" — untested service/repository code is the #1 source of regressions in this project given its concurrency layer.
- **Always include hard/edge-case tests, not just the happy path.** A method isn't "tested" if only the normal-input case is covered. For every new method, also write cases for:
  - **Boundary values** — empty string, blank/whitespace-only string, exactly-at-limit length (e.g. title at 100 chars vs 101), zero, negative numbers, `Integer.MAX_VALUE` where relevant.
  - **Null / missing input** — null entity, null field, null `dueDate`, missing config key with no default.
  - **Invalid/malformed input** — a due date already in the past, an unknown enum-like string, a corrupted or truncated `.ser` backup file, a malformed `.db` path.
  - **Concurrency edge cases** — two rapid edits to the same task (reminder cancel-then-reschedule race), a backup running while a save is in flight, duplicate/rapid `create()` calls for the same logical entity.
  - **State edge cases** — deleting an entity that was already deleted, updating an entity that doesn't exist, restoring a backup into an empty vs. non-empty database, running the wizard finish twice.
  - **Failure paths** — repository throws mid-service-call (verify no partial state / no silent swallow), file I/O failure during serialization, `SQLITE_BUSY`/locked-DB scenario.
  For each hard case, assert the *specific* failure behavior expected (a named exception type, an unchanged persisted state, a `ValidationResult` with the exact field error) — not just "it doesn't crash."

## 8. When Generating Code, Always
1. State which layer(s) you're touching in one line, if not obvious from the file path.
2. Follow the existing method-ordering convention within a class: constructors → public API → private helpers.
3. Match existing naming/style in the file you're editing over introducing a new convention.
4. If a change affects more than one file (e.g. new entity → repository → service → controller → view), touch all of them in the same response — don't leave the codebase in a half-wired state.
5. Flag (in one line) any deviation from this document you believe is justified — don't silently diverge.