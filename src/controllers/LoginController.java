package controllers;

import java.io.BufferedWriter;
import java.io.File;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import utils.DBConnector;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoginController {

    @FXML
    private AnchorPane anchorPane;
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private ComboBox<String> userComboBox;
    @FXML
    private Button loginButton;
    @FXML
    private Hyperlink forgotPasswordLink;
    @FXML
    private AnchorPane loadingOverlay;
    @FXML
    private ProgressIndicator loadingSpinner;

    @FXML
    public void initialize() {
        userComboBox.getItems().addAll("ADMIN", "FACULTY", "STUDENT");
        userComboBox.setValue("ADMIN");
        loadingOverlay.setVisible(false);
    }

    @FXML
    private void handleLoginButtonClick() {
        final String username = usernameField.getText().trim();
        final String password = passwordField.getText().trim();
        final String userType = userComboBox.getValue();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Input Error", "Please fill in both fields.");
            return;
        }

        // Use a Task for background work to keep UI responsive
        Task<LoginResult> loginTask = new Task<>() {
            @Override
            protected LoginResult call() throws Exception {
                // Simulate network latency
                Thread.sleep(500);
                return validateLogin(username, password, userType);
            }
        };

        // What to do when the task succeeds
        loginTask.setOnSucceeded(event -> {
            LoginResult result = loginTask.getValue();
            if (result != null) {
                showAlert(Alert.AlertType.INFORMATION, "Login Successful", "Welcome, " + result.getUserName() + "!");
                navigateToDashboard(userType, result.getUserId());
                return;
            }
            showAlert(Alert.AlertType.ERROR, "Login Failed", "Invalid credentials. Please try again.");
        });

        // What to do if the task fails
        loginTask.setOnFailed(event -> {
            showAlert(Alert.AlertType.ERROR, "Login Error", "An error occurred during login. Please check the console.");
            loginTask.getException().printStackTrace();
        });

        // Bind loading overlay visibility to task running state
        loadingOverlay.visibleProperty().bind(loginTask.runningProperty());

        // Start the task
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
                return null;
        }

        try (Connection conn = DBConnector.getConnection();
            PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);
            stmt.setString(2, username);
            stmt.setString(3, password);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String userId = rs.getString(idColumn);
                String userName = rs.getString(nameColumn);
                return new LoginResult(userId, userName);
            }
            return null; // Login failed
        } catch (SQLException e) {
            e.printStackTrace();
            throw e; // Re-throw to be caught by onFailed handler
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

                // Pass the userId to the new controller AFTER loading the FXML
                switch (userType) {
                    case "FACULTY":
                        // Assuming sessionController has an initData(String userId) method
                        App.Controllers.sessionController sessionCtrl = loader.getController();
                        sessionCtrl.initData(userId); 
                        break;
                    case "STUDENT":
                        // Assuming studentFrameController has an initData(String userId) method
                        App.Controllers.studentFrameController studentCtrl = loader.getController();
                        studentCtrl.initData(userId);
                        break;
                }

                Stage dashboardStage = new Stage();
                dashboardStage.setTitle(userType + " Dashboard");
                dashboardStage.setScene(new Scene(dashboardPane));
                dashboardStage.show();

                // Close current login window
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
        showAlert(Alert.AlertType.INFORMATION, "Forgot Password", "Password reset functionality coming soon!");
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
