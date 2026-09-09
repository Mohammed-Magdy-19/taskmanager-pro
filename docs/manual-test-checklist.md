# Smart Task Manager Pro — Manual Full-Flow Regression Test Script

This checklist defines the complete end-to-end manual regression procedure for v1.0.0. Follow each step in order from a clean state.

---

### Pre-conditions & Setup
- Java 21+ Runtime installed.
- Terminal / command line access at the application root directory.

---

### Step 1: Clean Install Simulation
1. Ensure the application is terminated.
2. In the application working directory, delete:
   - `config.properties`
   - Any SQLite database file (`*.db`, `tasks.db`)
   - The entire `backups/` directory (if present)
3. **Expected Result:** The working directory has no persistent database or config files, simulating a clean first run on a fresh machine.

---

### Step 2: First-Run Wizard Appearance
1. Launch the application via `java -jar target/taskmanager-1.0.0.jar` (or F5 / launcher).
2. **Expected Result:**
   - The **Configuration Wizard** window appears immediately.
   - The window is non-resizable / fixed-size (approx. 620 × 460 px).
   - Theme styling matches AppTheme Warm Editorial Light palette (warm cream background `#FFFBF5`, dark charcoal text `#2D3142`, terracotta accent `#CE3C2B`).
   - Contrast is crisp, typography uses clean SansSerif/Segoe UI hierarchy, and step progress indicators are clearly visible.

---

### Step 3: Complete Wizard with Non-Default Values
1. On Step 1 (Welcome / Profile):
   - Enter User Name: `Release Tester`
   - Enter Email: `tester@example.com`
   - Click **Next**.
2. On Step 2 (Database & Backup Path):
   - Change Database File Path: `custom_release_tasks.db`
   - Change Backup Directory: `custom_backups`
   - Click **Next**.
3. On Step 3 (Preferences & Theming):
   - Select Theme: **Dark**
   - Toggle Auto-Backup: **Enabled**
   - Change Backup Frequency: **12 Hours**
   - Toggle Desktop Notifications: **Enabled**
4. Click **Finish**.
5. Open the generated `config.properties` in a text editor.
6. **Expected Result:**
   - `config.properties` exists and reflects the exact configured non-default values:
     ```properties
     app.theme=Dark
     db.path=custom_release_tasks.db
     backup.dir=custom_backups
     backup.auto.enabled=true
     backup.frequency=12
     notifications.enabled=true
     user.name=Release Tester
     user.email=tester@example.com
     ```
   - Wizard window closes cleanly without lingering threads.

---

### Step 4: Main Window Launch & Theming
1. Observe the main application window opening automatically after wizard completion.
2. **Expected Result:**
   - Main window opens maximized (`ExtendedState = MAXIMIZED_BOTH`).
   - Dark theme is active immediately without manual theme switching.
   - Header shows application title and status.
   - Resize or restore window down: UI smoothly scales, table columns maintain proportions, no text or button clipping occurs.

---

### Step 5: Task Population & Live Statistics
1. Click **+ New Task** button (or press `Ctrl+N`).
2. Add Task 1:
   - Title: `Near Future Reminder Task`
   - Due Date: Set to 2 minutes from current local time (`YYYY-MM-DD HH:MM`).
   - Priority: `HIGH`
   - Status: `PENDING`
   - Click **Save**.
3. Add Task 2:
   - Title: `Overdue Followup Item`
   - Due Date: Select yesterday's date (or earlier).
   - **Expected Result:** Validation warning badge/inline error appears: *"Task due date cannot be in the past"*. Save is blocked.
   - Correct Due Date to empty (leave blank).
   - Priority: `LOW`
   - Status: `IN_PROGRESS`
   - Click **Save**.
4. Add Task 3:
   - Title: `Completed Milestone`
   - Due Date: None.
   - Priority: `MEDIUM`
   - Status: `COMPLETED`
   - Click **Save**.
5. **Expected Result:**
   - All valid tasks appear in the main table.
   - Status and Priority pill badges render with distinct, high-contrast colors:
     - High Priority: Red pill badge.
     - In Progress: Blue pill badge.
     - Completed: Green pill badge.
   - Top Dashboard stats update live:
     - Total: 3
     - Pending: 1
     - In Progress: 1
     - Completed: 1

---

### Step 6: Desktop Reminder Notification
1. Keep the app open and wait until the 2-minute mark for `Near Future Reminder Task`.
2. **Expected Result:**
   - A desktop notification appears via Windows SystemTray / toast dialog.
   - The notification displays the title `Near Future Reminder Task` and due date.
   - No exceptions or errors in console.

---

### Step 7: Cancel-Before-Reschedule Verification
1. Double-click or select `Near Future Reminder Task` and click **Edit**.
2. Change its Due Date to 10 minutes further into the future.
3. Click **Save**.
4. **Expected Result:**
   - The task's due date updates in the table.
   - The old reminder is cancelled immediately.
   - No duplicate or premature notification fires at the original 2-minute time.
   - Only a single reminder will fire at the updated 10-minute timestamp.

---

### Step 8: Search and Filter Combinations (AND Logic)
1. In the search box, type `Task` (matches Task 1).
   - **Expected Result:** Table displays Task 1.
2. Clear search box.
3. In Status filter, select `Completed`.
   - **Expected Result:** Only Task 3 (`Completed Milestone`) displays.
4. Set Priority filter to `High Priority` while keeping search or status filter active.
   - **Expected Result:** Filters combine via strict AND logic. If no task matches both, the friendly Empty State banner appears ("No matching tasks found").
5. Reset search and filters to `All`.
   - **Expected Result:** Full list of 3 tasks reappears instantly.

---

### Step 9: Backup & Selective Restore (Upsert Behavior)
1. Click **Settings** -> navigate to Backup section.
2. Click **Backup Now**.
3. **Expected Result:**
   - A success dialog appears.
   - A `.ser` file appears in `custom_backups/` directory (e.g. `backup_*.ser`).
4. Select `Completed Milestone` in the task table and click **Delete** (confirm prompt).
   - **Expected Result:** Task is removed; total count is 2.
5. In Settings, click **Restore Backup** and select the `.ser` file generated in step 2.
6. **Expected Result:**
   - Restore completes with confirmation dialog.
   - `Completed Milestone` reappears in the table.
   - The other 2 tasks remain intact with their existing status and IDs (upsert/merge behavior, no duplicate rows).

---

### Step 10: Settings Persistence Without Restart
1. Open **Settings**.
2. Change theme from **Dark** to **Light**.
3. Change Auto-Backup frequency to **24 Hours**.
4. Click **Save**.
5. **Expected Result:**
   - Theme switches dynamically to Warm Editorial Light without requiring an app restart.
   - Check `config.properties`: `app.theme=Light` and `backup.frequency=24` are persisted.

---

### Step 11: Resilience to File / DB Error
1. While the application is running, open the file explorer or terminal and rename `custom_release_tasks.db` to `custom_release_tasks.db.locked`.
2. In the app, click **+ New Task**, enter Title: `Resilience Test`, and click **Save**.
3. **Expected Result:**
   - The application does **not** crash or freeze.
   - A user-friendly error dialog appears indicating database write failure.
   - Rename `custom_release_tasks.db.locked` back to `custom_release_tasks.db`.
   - Click **Save** again: task is saved successfully.

---

### Step 12: Application Relaunch & State Continuity
1. Close the application.
2. Launch the application again: `java -jar target/taskmanager-1.0.0.jar`.
3. **Expected Result:**
   - The **Configuration Wizard does NOT reappear** (first-run check correctly identifies existing config).
   - The main window launches directly with the saved Light theme.
   - All tasks and state from prior sessions are preserved in the table.
   - Dashboard stats accurately match saved tasks.
