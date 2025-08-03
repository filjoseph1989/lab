package controllers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
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
    boolean isValid=false;
    @FXML
    private void handleLoginButtonClick() {
        // kani pang kuhaon niya ang mga value sa username, password og sa dropdown (admin, faculty, student)
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        String userType = userComboBox.getValue();
        
        // kani pang print niya ang userType
        System.out.println(userType);

        // i-Validate niya and  username and password, dapat dili empty
        // kung empty mag alert
        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Input Error", "username and password cannot be empty.");
            return;
        }

        // display niya ang loading spinner, para maka ingun ang user nga naga load pa ang dashboard
        loadingOverlay.setVisible(true);

        // Run login check in a new thread
        new Thread(() -> {
            
            try {
                System.out.println(userType);
                // tawagun niya ang validateLogin method sa baba
                isValid = validateLogin(username, password, userType);
            } catch (Exception ex) {
                Logger.getLogger(LoginController.class.getName()).log(Level.SEVERE, null, ex);

            }

            try {
                Thread.sleep(1000); // simulate delay
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            javafx.application.Platform.runLater(() -> {
                loadingOverlay.setVisible(false); // ----> diri gi-false siya diri kay na success na og login
                if (isValid) {
                    showAlert(Alert.AlertType.INFORMATION, "Login Successful", "Welcome, " + userType + "!");
                    navigateToDashboard(userType);
                } else {
                    showAlert(Alert.AlertType.ERROR, "Login Failed", "Invalid credentials. Please try again.");
                }
            });
        }).start();
    }

    String id = "";

    // dawatung dri ang username, password, userType
    private boolean validateLogin(String username, String password, String userType) throws Exception{
        String query = "";
        
        // kaning switch, kung ang gi-pili sa dropdown kay admin, faculty, student
        switch (userType) {
            case "ADMIN": // kung admin kani ang i-run
                query = "SELECT * FROM admin WHERE (username = ? OR email = ?) AND password = ?";
                // query = "SELECT * FROM admin WHERE (username = 'admin' OR email = 'admin') AND password = 'admin'";
                break;
            case "FACULTY": // kung faculty kani ang i-run
                query = "SELECT * FROM faculty WHERE (username = ? OR email = ?) AND password = ?";
                // query = "SELECT * FROM faculty WHERE (username = 'admin' OR email = 'admin') AND password = 'admin'";
                break;
            case "STUDENT": // kung student kani ang i-run
                query = "SELECT * FROM student WHERE (username = ? OR email = ?) AND password = ?";
                // query = "SELECT * FROM student WHERE (username = 'nathan' OR email = 'nathan') AND password = '090807'";
                break;
            default:
                return false;
        }

        // 
        try (Connection conn = DBConnector.getConnection(); // tawagun niya ang src/utils/DBConnector.java
            PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);
            stmt.setString(2, username); // username or email
            stmt.setString(3, password);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()){
                switch (userType) {
                    case "FACULTY":
                        id=rs.getString("facultyID");
                        break;
                    case "STUDENT":
                        id=rs.getString("studentID");
                        break;
                    case "ADMIN":
                        id=rs.getString("id");
                        System.out.println("ADMIN Log in");
                        break;
                    default:
                        return true;
                }
                return true;
            }
            return false;
             // true if record exists

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void navigateToDashboard(String userType) {
        try {
            FXMLLoader loader;
            String fxmlPath = "";

            switch (userType) {
                case "ADMIN":
                    fxmlPath = "/App/Views/admin.fxml";
                    break;
                case "FACULTY":
                    fxmlPath = "/App/Views/Session.fxml";
                    BufferedWriter write2 = new BufferedWriter(new FileWriter("instructorID.txt"));
                    write2.write(id);
                    write2.close();
                    break;
                case "STUDENT":
                    fxmlPath = "/App/Views/studentFrame.fxml";
                    FileWriter write3 = new FileWriter("studentID.txt");
                    write3.write(id);
                    write3.close();
                    break;
            }

            if (!fxmlPath.isEmpty()) {
                loader = new FXMLLoader(getClass().getResource(fxmlPath));
                AnchorPane dashboardPane = loader.load();

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
}
