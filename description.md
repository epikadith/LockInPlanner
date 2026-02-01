# LockInPlanner

## Functionality
**LockInPlanner** is a productivity application designed to help users manage their daily schedule effectively.

*   **Timeline View**: A powerful vertical 24-hour timeline that visualizes tasks with minute-level precision.
    *   **Minute Precision**: Tasks are rendered with accurate vertical positioning and height representing their exact start and end times (down to the minute).
    *   **Fractional Rendering**: A task starting at 4:30 PM visually begins halfway through the 4:00 PM slot.
    *   **Side-by-Side Tasks**: Overlapping tasks are intelligently arranged side-by-side to avoid visual clutter.
    *   **Task Editing**: Users can tap any task to edit its details directly.

*   **Checklists**: A dedicated screen for managing lists (to-do, grocery, etc.).
    *   **Organization**: Create multiple checklists with titles, color-coding, and visual progress bars.
    *   **Objectives**: Add checkbox items to each list.
    *   **Smart Sorting**:
        *   **Items**: Checked items automatically move to the bottom.
        *   **Lists**: Completed checklists sink to the bottom of the grid.
    *   **Manual Reordering**: "uncompleted" items can be reordered using Up/Down controls.
    *   **Grid Layout**: Toggle between 1, 2, or 3 columns for viewing lists.
    *   **Progress Tracking**: Visual progress bar shows completion percentage for each list.

*   **Task Creation (TaskBuilder)**:
    *   **Detailed Input**: Name, description, time range (hour & minute), date, and color.
    *   **Repeatability Options**:
        *   **Single**: One-time task.
        *   **Daily**: Repeats every day.
        *   **Custom**: Repeats on specific selected days (e.g., Mon, Wed, Fri).
    *   **Scrollable UI**: Optimized for all screen sizes.

*   **Calendar View**:
    *   **Month Grid**: Displays tasks for the entire month with "Show Daily" toggles.
    *   **Smart Visualization**: Correctly displays "Custom" tasks only on their active days.
    *   **Day View**: Detailed popup showing tasks for a selected date with edit capabilities.

*   **Settings & Data Management**:
    *   **Import/Export**: Robust JSON import/export functionality with **Strict Schema Validation**.
        *   Rejects corrupted files (e.g., missing fields, unknown keys).
        *   Prevents logic errors like "Zero Duration" tasks.
    *   **Data Cleanup**: Options to delete all tasks or checklists.

## Technical Implementation

### Tech Stack
*   **Language**: Kotlin
*   **UI Framework**: Jetpack Compose (Material3)
*   **Architecture**: MVVM (Model-View-ViewModel)
*   **Database**: Room (SQLite) with KSP.
*   **Asynchronous**: Kotlin Coroutines & Flow.

### Key Components

1.  **`TimelineScreen.kt`**:
    *   Renders the 24-hour timeline.
    *   **Custom Layout Logic**: Calculates fractional offsets based on task minutes to render accurate `Box` heights and positions within hour rows.

2.  **`ChecklistsScreen.kt` & `ListViewPopup.kt`**:
    *   Manages the grid of checklists.
    *   **Reordering Logic**: Implements robust list re-indexing (swapping `order` fields) to persist manual item ordering.
    *   **Reactive UI**: Updates in real-time as database changes occur.

3.  **`DataImportManager.kt`**:
    *   **Strict Validation**: Manually parses `JsonElement` trees to verify schema integrity before deserialization.
    *   **Error Handling**: Provides detailed user feedback for validation failures (e.g., "Missing required field 'text'").

4.  **`TaskBuilder.kt`**:
    *   A robust, scrollable dialog for creating/editing tasks.
    *   Handles minute-precise time selection and complex repeatability modes.

5.  **Data Layer**:
    *   **Entities**: `TaskEntity`, `ChecklistEntity`, `ObjectiveEntity`.
    *   **DAOs**: `TaskDao`, `ChecklistDao`.
    *   **Repository**: `TaskRepository`, `ChecklistRepository`.

6.  **Navigation**:
    *   Adaptive navigation suite integrating Timeline, Calendar, Checklists, and Settings screens.
