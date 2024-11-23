import java.sql.*;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;  // Add this import
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public final class UserSignup {

    // Instance variables
    private Scene signupScene;
    private TextField UsernameField = new TextField();
    private PasswordField PasswordField = new PasswordField();
    private ComboBox<String> RoleField = new ComboBox<>();  // Replace TextField with ComboBox
    private TextField FirstNameField = new TextField();
    private TextField LastNameField = new TextField();
    private Stage stage;

    // Constructor
    public UserSignup(Stage primaryStage) {
        this.stage = primaryStage;
    }

    // Method to initialize the components
    public void initializeComponents() {
        VBox signUpLayout = new VBox(10);
        signUpLayout.setPadding(new Insets(10));
        Button signUpButton = new Button("Sign Up");
        Button backButton = new Button("Back to Admin Login");

        // Initialize ComboBox with roles
        RoleField.getItems().addAll("Admin", "Manager", "Clerk", "Trainer", "Nutritionist");
        RoleField.setPromptText("Select Role");

        signUpButton.setOnAction(event -> signUp());
        backButton.setOnAction(event -> goBackToAdminLogin());

        signUpLayout.getChildren().addAll(
                new Label("Username:"), UsernameField,
                new Label("Password:"), PasswordField,
                new Label("Role:"), RoleField,
                new Label("First Name:"), FirstNameField,
                new Label("Last Name:"), LastNameField,
                signUpButton,
                backButton
        );

        signupScene = new Scene(signUpLayout, 300, 550);
        stage.setTitle("User Sign-up");
        stage.setScene(signupScene);
        stage.show();
    }

    // Method to sign up the user
    private void signUp() {
        String username = UsernameField.getText();
        String password = PasswordField.getText();
        String role = RoleField.getValue();  // Get selected value from ComboBox
        String firstname = FirstNameField.getText();
        String lastname = LastNameField.getText();

        if (role == null) {
            showAlert("Invalid Role", "Please select a role.");
            return;
        }

        if (!isValidUsername(username)) {
            showAlert("Invalid Username", "Username must be at least 4 characters long and contain at least one lowercase and one uppercase letter.");
            return;
        }

        if (!isValidPassword(password)) {
            showAlert("Invalid Password", "Password must be at least 8 characters long and contain at least one lowercase letter, one uppercase letter, one digit, and one special character.");
            return;
        }

        Connection con = DBUtils.establishConnection();
        String query = "INSERT INTO users (username, password, salt, role, firstname, lastname, account_status) VALUES (?, ?, ?, ?, ?, ?, ?);";

        String salt = PasswordHasher.generateSalt();
        String hashedPassword = PasswordHasher.hashPassword(password, salt);

        try {
            PreparedStatement statement = con.prepareStatement(query);
            statement.setString(1, username);
            statement.setString(2, hashedPassword);
            statement.setString(3, salt);
            statement.setString(4, role);
            statement.setString(5, firstname);
            statement.setString(6, lastname);
            statement.setString(7, "3");
            int result = statement.executeUpdate();

            if (result == 1) {
                showAlert("Sign Up Successful", "User registered successfully.");
                goBackToAdminLogin();
                
                String logQuery = "INSERT INTO log (mac_address, log_time, action) VALUES (?, ?, ?)";
                Timestamp timeStamp = new Timestamp(System.currentTimeMillis());
                String action = "user sign-up successful";
                PreparedStatement logStatement = con.prepareStatement(logQuery);
                logStatement.setString(1, GetMacAddress.getMacAddress());
                logStatement.setTimestamp(2, timeStamp);
                logStatement.setString(3, action);
            } else {
                showAlert("Authentication Failed", "Invalid username or password.");
                
                String logQuery = "INSERT INTO log (mac_address, log_time, action) VALUES (?, ?, ?)";
                Timestamp timeStamp = new Timestamp(System.currentTimeMillis());
                String action = "user sign-up failed attempt";
                PreparedStatement logStatement = con.prepareStatement(logQuery);
                logStatement.setString(1, GetMacAddress.getMacAddress());
                logStatement.setTimestamp(2, timeStamp);
                logStatement.setString(3, action);
            }
            DBUtils.closeConnection(con, statement);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Database Error", "Failed to connect to the database.");
        }
    }

    // Method to validate the username
    private boolean isValidUsername(String username) {
        return username.length() >= 4 && username.matches(".*[a-z].*") && username.matches(".*[A-Z].*");
    }

    // Method to validate the password
    private boolean isValidPassword(String password) {
        return password.length() >= 8 && password.matches(".*[a-z].*") && password.matches(".*[A-Z].*") && password.matches(".*\\d.*") && password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*");
    }

    // Method to go back to the admin login
    private void goBackToAdminLogin() {
        AdminLogin adminLogin = new AdminLogin(stage, "");
        adminLogin.initializeComponents();
    }

    // Method to show an alert
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}