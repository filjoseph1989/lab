package controllers;

import App.Controllers.sessionController;
import App.Controllers.studentFrameController;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import utils.DBConnector;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginController {

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Button loginButton;
    @FXML
    private Hyperlink forgotPasswordLink;
    @FXML
    private AnchorPane loadingOverlay;
    @FXML
    private ProgressIndicator loadingSpinner;
    @FXML
    private VBox userSelectionVBox;
    @FXML
    private VBox loginFormVBox;
    @FXML
    private Text loginTitleText;
    @FXML
    private Button backButton;

    private String selectedUserType;

    @FXML
    public void initialize() {
        // Start with the login form hidden
        loadingOverlay.setVisible(false);
        loginFormVBox.setVisible(false);
        loginFormVBox.setManaged(false);
    }

    @FXML
    private void handleAdminLoginSelect(MouseEvent event) {
        showLoginForm("ADMIN");
    }

    @FXML
    private void handleFacultyLoginSelect(MouseEvent event) {
        showLoginForm("FACULTY");
    }

    @FXML
    private void handleStudentLoginSelect(MouseEvent event) {
        showLoginForm("STUDENT");
    }

    @FXML
    private void handleBackButtonClick() {
        // Hide the login form and show the user type selection
        userSelectionVBox.setVisible(true);
        userSelectionVBox.setManaged(true);
        loginFormVBox.setVisible(false);
        loginFormVBox.setManaged(false);
    }

    private void showLoginForm(String userType) {
        selectedUserType = userType;
        loginTitleText.setText(userType.substring(0, 1).toUpperCase() + userType.substring(1).toLowerCase() + " Login");

        // Hide the user type selection and show the login form
        userSelectionVBox.setVisible(false);
        userSelectionVBox.setManaged(false);
        loginFormVBox.setVisible(true);
        loginFormVBox.setManaged(true);
    }

    @FXML
    private void handleLoginButtonClick() {
        final String username = usernameField.getText().trim();
        final String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Input Error", "Please fill in all fields.");
            return;
        }

        // Use a Task for background login validation to keep the UI responsive
        Task<LoginResult> loginTask = new Task<>() {
            @Override
            protected LoginResult call() throws Exception {
                Thread.sleep(500); // Simulate network latency
                return validateLogin(username, password, selectedUserType);
            }
        };

        loginTask.setOnSucceeded(event -> {
            LoginResult result = loginTask.getValue();
            if (result != null) {
                showAlert(Alert.AlertType.INFORMATION, "Login Successful", "Welcome, " + result.getUserName() + "!");
                navigateToDashboard(selectedUserType, result.getUserId());
            } else {
                showAlert(Alert.AlertType.ERROR, "Login Failed", "Invalid credentials. Please try again.");
            }
        });

        loginTask.setOnFailed(event -> {
            showAlert(Alert.AlertType.ERROR, "Login Error", "An error occurred during login. Please check the console.");
            loginTask.getException().printStackTrace();
        });

        // Bind the loading overlay's visibility to the task's running state
        loadingOverlay.visibleProperty().bind(loginTask.runningProperty());

        new Thread(loginTask).start();
    }

    private LoginResult validateLogin(String username, String password, String userType) throws SQLException {
        String query = "";
        String idColumn = "";
        String nameColumn = "";

        switch (userType) {
            case "ADMIN":
                query = "SELECT id, username FROM admin WHERE (username = ? OR email = ?) AND password = ?";
                idColumn = "id";
                nameColumn = "username";
                break;
            case "FACULTY":
                query = "SELECT facultyID, fullname FROM faculty WHERE (username = ? OR email = ?) AND password = ?";
                idColumn = "facultyID";
                nameColumn = "fullname";
                break;
            case "STUDENT":
                query = "SELECT studentID, fullname FROM student WHERE (username = ? OR email = ?) AND password = ?";
                idColumn = "studentID";
                nameColumn = "fullname";
                break;
            default:
                return null; // Should not happen
        }

        try (Connection conn = DBConnector.getConnection();
            PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);
            stmt.setString(2, username);
            stmt.setString(3, password);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String userId = rs.getString(idColumn);
                    String userName = rs.getString(nameColumn);
                    return new LoginResult(userId, userName);
                }
            }
            return null; // Login failed
        } catch (SQLException e) {
            e.printStackTrace();
            throw e; // Re-throw to be caught by the onFailed handler
        }
    }

    private void navigateToDashboard(String userType, String userId) {
        try {
            String fxmlPath = "";
            switch (userType) {
                case "ADMIN":
                    fxmlPath = "/App/Views/admin.fxml";
                    break;
                case "FACULTY":
                    fxmlPath = "/App/Views/Session.fxml";
                    break;
                case "STUDENT":
                    fxmlPath = "/App/Views/studentFrame.fxml";
                    break;
            }

            if (!fxmlPath.isEmpty()) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                Parent dashboardPane = loader.load();

                // Pass user data to the new controller
                Object controller = loader.getController();
                if (userType.equals("FACULTY") && controller instanceof sessionController) {
                    ((sessionController) controller).initData(userId);
                } else if (userType.equals("STUDENT") && controller instanceof studentFrameController) {
                    ((studentFrameController) controller).initData(userId);
                }

                Stage dashboardStage = new Stage();
                dashboardStage.setTitle(userType + " Dashboard");
                dashboardStage.setScene(new Scene(dashboardPane));
                dashboardStage.show();

                // Close the login window
                Stage currentStage = (Stage) loginButton.getScene().getWindow();
                currentStage.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Could not load the dashboard.");
        }
    }

    @FXML
    private void handleForgotPasswordClick() {
        showAlert(Alert.AlertType.INFORMATION, "Forgot Password", "Password reset functionality is not yet implemented.");
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Helper class to return multiple values from the login task
    private static class LoginResult {
        private final String userId;
        private final String userName;

        public LoginResult(String userId, String userName) {
            this.userId = userId;
            this.userName = userName;
        }

        public String getUserId() {
            return userId;
        }

        public String getUserName() {
            return userName;
        }
    }
}
