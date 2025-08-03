## 3. Recent Changes and Bug Fixes

This section details the significant updates and troubleshooting steps performed to enhance the application's user interface and resolve critical runtime issues.

### 3.1. UI Enhancements: Icon-Based Login

The login screen's user type selection has been modernized to improve user experience. The previous `ComboBox` (dropdown menu) was replaced with a more intuitive and visually appealing icon-based selection screen.

**Key Changes:**

*   **User Interface (`loginpanel.fxml`):** The FXML file was updated to include a new `VBox` for user type selection, containing clickable `SVGPath` icons for Admin, Faculty, and Student roles. The original login form is now hidden by default and only appears after a user type is selected.
*   **Controller Logic (`LoginController.java`):** The controller was rewritten to manage the new UI. Event handlers were added for the icon clicks (`handleAdminLoginSelect`, `handleFacultyLoginSelect`, `handleStudentLoginSelect`) and a back button (`handleBackButtonClick`) to toggle visibility between the selection screen and the login form.

This change provides a cleaner, more modern interface for the user's first interaction with the application.

### 3.2. Environment and Configuration Fixes

A significant portion of the work involved diagnosing and fixing deep-seated environment and build configuration problems that prevented the application from running.

**The core issues were:**

1.  **Missing Java Runtime:** The system could not execute the application because a Java Development Kit (JDK) was not installed or configured correctly in the system's path.
2.  **Version Incompatibility:** The project was configured to use JavaFX SDK 24.0.2, which is built for Java 22, while the newly installed JDK was version 21. This mismatch caused an `UnsupportedClassVersionError`.

**Resolution Steps:**

1.  **Installed JDK 21:** A compatible JDK (version 21 for Apple Silicon) was installed to provide the necessary Java runtime.
2.  **Downloaded JavaFX SDK 21:** The correct version of the JavaFX SDK (version 21) was downloaded to match the installed JDK.
3.  **Updated Project Properties (`nbproject/project.properties`):** The project's configuration file was modified to:
    *   Point to the new `javafx-sdk-21` directory.
    *   Update the `javac.classpath` to include the new JavaFX 21 JAR files.
    *   Change the `run.jvmargs` to use the correct module path.
    *   Set the `javac.source` and `javac.target` to `21` to ensure the code compiles for the correct Java version.

### 3.3. Runtime Error Resolution

After resolving the configuration issues, a final runtime error occurred:

*   **`javafx.fxml.LoadException`:** The application failed to start because the `loginpanel.fxml` file was missing necessary imports for UI components (`HBox`, `Label`, and `SVGPath`) that were added during the UI enhancement phase.

**Resolution:**

The missing `<?import ...>` statements were added to the top of `loginpanel.fxml`, allowing the FXML loader to correctly construct the scene.

### 3.4. Current Status

Following these changes, the application now builds and runs successfully. All version conflicts and runtime errors have been resolved, and the new icon-based login UI is fully functional.

### 3.5. Build and Run Commands

To build and run the application from the command line, use the following commands from the project's root directory:

**Build:**
```shell
ant
```

**Run:**
```shell
/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home/bin/java --module-path /Users/fil/Fil/labsession-main/javafx-sdk-21/lib --add-modules javafx.controls,javafx.fxml -cp "/Users/fil/Fil/labsession-main/build/classes:/Users/fil/Fil/labsession-main/src/mysql-connector-j-8.1.0.jar" main.MainApp
```
