/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package App.Controllers;

import App.DatabaseConnection;
import App.Models.PerformanceView;
import App.Models.TaskSubjectView;
import App.Models.subject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;

/**
 *
 * @author Blanc
 */
public class sessionController {
    String instructorID = "";
    DatabaseConnection dc = new DatabaseConnection();
    public void initialize()throws Exception{
        dc.connect();
        File read = new File("instructorID.txt");
        Scanner sc = new Scanner(read);
        while(sc.hasNextLine()){
            instructorID=sc.nextLine();
            System.out.println(instructorID);
        }
        Statement statement = dc.con.createStatement();
        statement.executeUpdate("UPDATE `faculty` SET status= 'Active' WHERE facultyID = '"+instructorID+"'");
        
        loadSubTable();
        loadTasksToTable();
    }
    
    @FXML
    public void logOut()throws Exception{
        Statement statement = dc.con.createStatement();
        statement.executeUpdate("UPDATE `faculty` SET status= 'Inactive' WHERE facultyID = '"+instructorID+"'");
    }
    
    @FXML private TableView<PerformanceView> studentPass;
    @FXML private TableColumn<PerformanceView, String> studName, fileName, studentScore,dateSubmitted;
    @FXML private TableColumn<PerformanceView, Void> message, studentActions;

    public void loadStudentPassesTable() throws Exception {
        performanceList.clear();

        studName.setCellValueFactory(cellData -> cellData.getValue().studentNameProperty());
        fileName.setCellValueFactory(cellData -> cellData.getValue().fileNameProperty());
        studentScore.setCellValueFactory(cellData -> cellData.getValue().scoreProperty());
        dateSubmitted.setCellValueFactory(cellData -> cellData.getValue().dateSubProperty());

        // Setup message column with VIEW button
        message.setCellFactory(col -> new TableCell<>() {
            private final Button viewBtn = new Button("VIEW");

            {
                viewBtn.setOnAction(e -> {
                    PerformanceView performance = getTableView().getItems().get(getIndex());
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Message");
                    alert.setHeaderText("Message: ");
                    alert.setContentText(performance.getMessage());
                    alert.showAndWait();
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : viewBtn);
            }
        });

        // Setup studentActions column with DOWNLOAD and SCORE buttons
        studentActions.setCellFactory(col -> new TableCell<>() {
            private final Button downloadBtn = new Button("Download");
            private final Button scoreBtn = new Button("Score");
            private final HBox container = new HBox(10, downloadBtn, scoreBtn);

            {
                downloadBtn.setOnAction(e -> {
                    PerformanceView performance = getTableView().getItems().get(getIndex());

                    try {
                        PreparedStatement ps = dc.con.prepareStatement(
                            "SELECT file FROM performance WHERE id = ?"
                        );
                        ps.setInt(1, Integer.parseInt(performance.getId()));  // assuming you have an ID getter

                        ResultSet rs = ps.executeQuery();
                        if (rs.next()) {
                            Blob blob = rs.getBlob("file");
                            InputStream is = blob.getBinaryStream();

                            FileChooser chooser = new FileChooser();
                            chooser.setTitle("Save File");
                            chooser.setInitialFileName(performance.getFile());
                            File saveFile = chooser.showSaveDialog(null);

                            if (saveFile != null) {
                                Files.copy(is, saveFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                            }

                            is.close();
                        }
                        rs.close();
                        ps.close();

                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
                scoreBtn.setOnAction(e -> {
                    PerformanceView performance = getTableView().getItems().get(getIndex());
                    TextInputDialog dialog = new TextInputDialog();
                    dialog.setTitle("Enter Score");
                    dialog.setHeaderText("Score for " + performance.getStudentName());
                    dialog.setContentText("Score:");

                    dialog.showAndWait().ifPresent(score -> {
                        try {
                            PreparedStatement ps = dc.con.prepareStatement("UPDATE performance SET score = ? WHERE id = ?");
                            ps.setString(1, score);
                            ps.setString(2, performance.getId());
                            ps.executeUpdate();
                            ps.close();
                            loadPerformanceData();
                            studentPass.refresh();
                        } catch (SQLException ex) {
                            new Alert(Alert.AlertType.ERROR, "Failed to update score: " + ex.getMessage()).showAndWait();
                        }
                    });
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : container);
            }
        });

        // Load data into table
        loadPerformanceData();
        studentPass.setItems(performanceList);
    }

    @FXML private Label totalPass;
    String taskId = "";
    int count = 0;
    ObservableList<PerformanceView> performanceList = FXCollections.observableArrayList();
    public void loadPerformanceData() throws SQLException {
        performanceList.clear();
        count = 0;
        String sql = """
            SELECT p.id, CONCAT(s.firstname, ' ', s.lastname) AS studentName, p.file, p.message, p.score, p.file_path, p.date_sub
            FROM performance p
            JOIN student s ON p.student_id = s.studentID WHERE p.task_id='"""+taskId+"' ORDER BY p.date_sub desc";

        PreparedStatement ps = dc.con.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            performanceList.add(new PerformanceView(
                rs.getString("id"),
                rs.getString("studentName"),
                rs.getString("file"),
                rs.getString("message"),
                rs.getString("score"),
                rs.getString("file_path"),
                rs.getString("date_sub")
            ));
            count++;
        }
        totalPass.setText(""+count);

        rs.close();
        ps.close();
    }
    
    @FXML private TableView<TaskSubjectView> taskTable;
    @FXML private TableColumn<TaskSubjectView, String> task, taskDesc, subCodeCol, subDesc, dur, dateSub;
    @FXML private TableColumn<TaskSubjectView, Void> taskActions;
    public void loadTasksToTable()throws Exception{
        task.setCellValueFactory(data -> data.getValue().taskProperty());
        taskDesc.setCellValueFactory(data -> data.getValue().taskDescriptionProperty());
        subCodeCol.setCellValueFactory(data -> data.getValue().subjectCodeProperty());
        subDesc.setCellValueFactory(data -> data.getValue().subjectDescriptionProperty());
        dur.setCellValueFactory(data -> data.getValue().durationProperty());
        dateSub.setCellValueFactory(cellData -> cellData.getValue().dateSubProperty());
        taskSubjectList.clear();
        loadTaskSubjectData();
        taskTable.setItems(taskSubjectList);
        
        taskActions.setCellFactory(col -> new TableCell<>() {
            private final Button toggleButton = new Button();
            private final Button viewButton = new Button("VIEW");
            private final HBox buttonBox = new HBox(5, toggleButton, viewButton);

            {
                toggleButton.setOnAction(e -> {
                    TaskSubjectView task = getTableView().getItems().get(getIndex());
                    try {
                        toggleTaskStatus(task); // Toggle status and update DB
                        loadTasksToTable();     // Refresh table after update
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });

                viewButton.setOnAction(e -> {
                    TaskSubjectView task = getTableView().getItems().get(getIndex());
                    try {
                        taskId = task.getTaskId();
                        loadStudentPassesTable();
                        taskPane.setVisible(false);
                        studentPassPane.setVisible(true);
                    } catch (Exception ex) {
                        Logger.getLogger(sessionController.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    TaskSubjectView task = getTableView().getItems().get(getIndex());
                    toggleButton.setText("Pending".equalsIgnoreCase(task.getStatus()) ? "CLOSE" : "OPEN");
                    setGraphic(buttonBox);
                }
            }
        });
    }
    public void toggleTaskStatus(TaskSubjectView task) throws SQLException {
        String newStatus = "Pending".equalsIgnoreCase(task.getStatus()) ? "Done" : "Pending";

        String sql = "UPDATE tasks SET status = ? WHERE task_id = ?";
        PreparedStatement ps = dc.con.prepareStatement(sql);
        ps.setString(1, newStatus);
        ps.setString(2, task.getTaskId());
        ps.executeUpdate();
        ps.close();

        // Optional: update the model directly to avoid refetching
        task.setStatus(newStatus);
    }


    ObservableList<TaskSubjectView> taskSubjectList = FXCollections.observableArrayList();
    public void loadTaskSubjectData() throws Exception {
        taskSubjectList.clear(); // Clear any existing data

        String sql = """
            SELECT 
                t.task_id, 
                t.task, 
                t.description AS task_description, 
                s.code AS subject_code, 
                s.description AS subject_description, 
                t.duration,
                t.status,
                t.date_sub
            FROM tasks t
            JOIN subject s ON t.subject_id = s.id
            WHERE t.instructor_id = '"""+instructorID+"' ORDER BY t.date_sub desc";

        PreparedStatement ps = dc.con.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            taskSubjectList.add(new TaskSubjectView(
                rs.getString("task_id"),
                rs.getString("task"),
                rs.getString("task_description"),
                rs.getString("subject_code"),
                rs.getString("subject_description"),
                rs.getString("duration"),
                rs.getString("status"),
                rs.getString("date_sub")
            ));
        }

        rs.close();
        ps.close();
    }


    @FXML private TableView<subject> subjectTable;
    @FXML private TableColumn<subject, String> subCode, subName, subSem, subYear;
    @FXML private TableColumn<subject, Void> createSession, manageSubject;
    ObservableList<subject> subjects = FXCollections.observableArrayList();
    public void loadSubTable() throws Exception {
        subjects.clear();
        getSubjects();

        subCode.setCellValueFactory(cellData -> cellData.getValue().codeProperty());
        subName.setCellValueFactory(cellData -> cellData.getValue().descriptionProperty());
        subSem.setCellValueFactory(cellData -> cellData.getValue().semProperty());
        subYear.setCellValueFactory(cellData -> cellData.getValue().yearProperty());
        subjectTable.setItems(subjects);

        // Create button (no action)
        createSession.setCellFactory(col -> new TableCell<>() {
            private final Button createBtn = new Button("Create");

            {
                createBtn.setOnAction(event -> {
                    subject selected = getTableView().getItems().get(getIndex());
                    if (selected == null) return;

                    Dialog<ButtonType> dialog = new Dialog<>();
                    dialog.setTitle("Create Session");

                    // Input fields
                    TextField taskField = new TextField();
                    TextField durationField = new TextField();
                    TextField taskCode = new TextField();
                    TextArea description = new TextArea();
                    description.setWrapText(true);

                    // ✅ NEW: Semester ComboBox
                    ComboBox<String> semCombo = new ComboBox<>();
                    semCombo.getItems().addAll("1", "2");
                    semCombo.setValue("1");

                    // ✅ NEW: School Year ComboBox
                    ComboBox<String> schoolYearCombo = new ComboBox<>();
                    int currentYear = LocalDate.now().getYear();
                    for (int i = -1; i <= 3; i++) {
                        int start = currentYear + i;
                        schoolYearCombo.getItems().add(start + "-" + (start + 1));
                    }
                    schoolYearCombo.setValue(currentYear + "-" + (currentYear + 1));

                    // Layout
                    GridPane grid = new GridPane();
                    grid.setHgap(10);
                    grid.setVgap(10);
                    grid.addRow(0, new Label("Task:"), taskField);
                    grid.addRow(1, new Label("Description:"), description);
                    grid.addRow(2, new Label("Duration:"), durationField);
                    grid.addRow(3, new Label("CODE:"), taskCode);
                    grid.addRow(4, new Label("Semester:"), semCombo);
                    grid.addRow(5, new Label("School Year:"), schoolYearCombo);

                    dialog.getDialogPane().setContent(grid);
                    dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
                    Optional<ButtonType> result = dialog.showAndWait();

                    if (result.isPresent() && result.get() == ButtonType.OK) {
                        String task = taskField.getText().trim();
                        String duration = durationField.getText().trim();
                        String task_code = taskCode.getText().trim();
                        String descriptions = description.getText().trim();
                        String sem = semCombo.getValue();
                        String schoolYear = schoolYearCombo.getValue();

                        if (task.isEmpty() || duration.isEmpty() || sem == null || schoolYear == null) {
                            showAlert("Task, Duration, Semester, and School Year are required!");
                            return;
                        }

                        if (!isValidDuration(duration)) {
                            showAlert("Invalid duration! Use whole hours (e.g., 1, 2) or H:MM (e.g., 1:30). Max 24 hours.");
                            return;
                        }

                        try {
                            PreparedStatement ps = dc.con.prepareStatement(
                                "INSERT INTO tasks (task, status, subject_id, duration, instructor_id, task_code, description, sem, school_year) " +
                                "VALUES (?, 'Pending', ?, ?, ?, ?, ?, ?, ?)"
                            );
                            ps.setString(1, task);
                            ps.setString(2, selected.getId());
                            ps.setString(3, duration);
                            ps.setString(4, instructorID);
                            ps.setString(5, task_code);
                            ps.setString(6, descriptions);
                            ps.setString(7, sem);
                            ps.setString(8, schoolYear);

                            int rows = ps.executeUpdate();
                            if (rows > 0) {
                                Alert success = new Alert(Alert.AlertType.INFORMATION);
                                success.setTitle("Success");
                                success.setHeaderText("Session created successfully!");
                                success.showAndWait();
                            }

                            ps.close();
                        } catch (SQLException e) {
                            e.printStackTrace();
                            Alert error = new Alert(Alert.AlertType.ERROR);
                            error.setTitle("Error");
                            error.setHeaderText("Failed to create session.");
                            error.setContentText(e.getMessage());
                            error.showAndWait();
                        }
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : createBtn);
            }
        });



        // Manage buttons: Update and Delete
        manageSubject.setCellFactory(col -> new TableCell<>() {
            private final Button updateBtn = new Button("Update");
            private final Button deleteBtn = new Button("Delete");
            private final HBox hbox = new HBox(5, updateBtn, deleteBtn);

            {
                updateBtn.setOnAction(event -> {
                    subject selected = getTableView().getItems().get(getIndex());

                    TextInputDialog dialog = new TextInputDialog(selected.getDescription());
                    dialog.setTitle("Update Subject");
                    dialog.setHeaderText("Update description for: " + selected.getCode());
                    dialog.setContentText("New Description:");

                    dialog.showAndWait().ifPresent(newDesc -> {
                        try {
                            PreparedStatement ps = dc.con.prepareStatement(
                                "UPDATE subject SET description = ? WHERE id = ?"
                            );
                            ps.setString(1, newDesc);
                            ps.setString(2, selected.getId());

                            int rowsUpdated = ps.executeUpdate();
                            if (rowsUpdated > 0) {
                                selected.setDescription(newDesc);
                                subjectTable.refresh();
                            }

                            ps.close();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    });
                });

                deleteBtn.setOnAction(event -> {
                    subject selected = getTableView().getItems().get(getIndex());

                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Confirm Deletion");
                    confirm.setHeaderText("Are you sure you want to delete subject: " + selected.getCode() + "?");

                    confirm.showAndWait().ifPresent(response -> {
                        if (response == ButtonType.OK) {
                            try {
                                PreparedStatement ps = dc.con.prepareStatement(
                                    "DELETE FROM subject WHERE id = ?"
                                );
                                ps.setString(1, selected.getId());

                                int rowsDeleted = ps.executeUpdate();
                                if (rowsDeleted > 0) {
                                    subjects.remove(selected);
                                }

                                ps.close();
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : hbox);
            }
        });
    }
    
    // ✅ Helper to show warnings
    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message);
        alert.showAndWait();
    }

    // ✅ Validate duration input: whole number (1-24) or H:MM format (0-24 hours)
    private boolean isValidDuration(String input) {
        input = input.trim();
        if (input.matches("^\\d+$")) {
            // Whole number
            int hours = Integer.parseInt(input);
            return hours >= 1 && hours <= 24;
        } else if (input.matches("^\\d{1,2}:\\d{2}$")) {
            String[] parts = input.split(":");
            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);

            if (minutes < 0 || minutes >= 60) return false;

            double totalHours = hours + (minutes / 60.0);
            return totalHours > 0 && totalHours <= 24;
        } else {
            return false;
        }
    }

    public void getSubjects()throws Exception{
        Statement statement = dc.con.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT * FROM subject WHERE instructor_id = '"+instructorID+"'");
        while (resultSet.next()) {
            String a = resultSet.getString("id");
            String b = resultSet.getString("code");
            String c = resultSet.getString("description");
            String d = resultSet.getString("sem");
            String r = resultSet.getString("year");
            subjects.add(new subject(a,b,c,d,r));
        }
    }
    
    public void addSubject() {
        // Code input
        TextInputDialog codeDialog = new TextInputDialog();
        codeDialog.setTitle("Add Subject");
        codeDialog.setHeaderText("Enter Subject Code");
        codeDialog.setContentText("Code:");
        Optional<String> codeResult = codeDialog.showAndWait();

        if (codeResult.isEmpty() || codeResult.get().trim().isEmpty()) return;

        // Description input
        TextInputDialog descDialog = new TextInputDialog();
        descDialog.setTitle("Add Subject");
        descDialog.setHeaderText("Enter Subject Description");
        descDialog.setContentText("Description:");
        Optional<String> descResult = descDialog.showAndWait();

        if (descResult.isEmpty() || descResult.get().trim().isEmpty()) return;

        String code = codeResult.get().trim();
        String description = descResult.get().trim();

        // Create a custom dialog for Semester & Year
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add Subject Details");

        ComboBox<Integer> semCombo = new ComboBox<>();
        semCombo.getItems().addAll(1, 2); // Summer is 3
        semCombo.setValue(1);

        ComboBox<String> yearCombo = new ComboBox<>();
        yearCombo.getItems().addAll(
            "2024-2025",
            "2025-2026",
            "2026-2027"
        );
        yearCombo.setValue("2024-2025");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.addRow(0, new Label("Semester:"), semCombo);
        grid.addRow(1, new Label("School Year:"), yearCombo);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;

        int sem = semCombo.getValue();
        String schoolYear = yearCombo.getValue();

        try {
            PreparedStatement ps = dc.con.prepareStatement(
                "INSERT INTO subject (code, description, instructor_id, sem, year) VALUES (?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, code);
            ps.setString(2, description);
            ps.setString(3, instructorID);
            ps.setInt(4, sem);
            ps.setString(5, schoolYear);

            int rows = ps.executeUpdate();

            if (rows > 0) {
                ResultSet keys = ps.getGeneratedKeys();
                if (keys.next()) {
                    String newId = keys.getString(1);
                    subject newSubject = new subject(newId, code, description, sem+"", schoolYear);
                    subjects.add(newSubject);
                    subjectTable.refresh();
                }
            }

            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML private Pane taskPane, subjectPane, studentPassPane;
    public void taskPaneShow()throws Exception{
        taskPane.setVisible(true);
        subjectPane.setVisible(false);
        studentPassPane.setVisible(false);
        loadTasksToTable();
    }
    public void subjectPaneShow()throws Exception{
        taskPane.setVisible(false);
        subjectPane.setVisible(true);
        studentPassPane.setVisible(false);
        loadSubTable();
    }
}
