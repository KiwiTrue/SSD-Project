import java.sql.*;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public final class UserLogin {

    // Instance variables
    private Scene loginScene;
    private TextField usernameField = new TextField();
    private PasswordField passwordField = new PasswordField();
    private Stage stage;

    // Constructor
    public UserLogin(Stage primaryStage) {
        this.stage = primaryStage;
    }

    // Method to initialize the components
    public void initializeComponents() {
        VBox loginLayout = new VBox(10);
        loginLayout.setPadding(new Insets(10));
        Button loginButton = new Button("Sign In");
        loginButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                authenticate();
            }
        });
        loginLayout.getChildren().addAll(new Label("Username:"), usernameField,
                new Label("Password:"), passwordField,
                loginButton);

        loginScene = new Scene(loginLayout, 300, 250);
        stage.setTitle("User Login");
        stage.setScene(loginScene);
        stage.show();
    }

    // Method to authenticate the user
    private void authenticate() {
        String macAddress = GetMacAddress.getMacAddress();
        String username = usernameField.getText();
        String password = passwordField.getText();
        Timestamp timeStamp = new Timestamp(System.currentTimeMillis());
        Connection con = DBUtils.establishConnection();
        String query = "SELECT * FROM users WHERE username=?";
        String saltQuery = "SELECT salt FROM users WHERE username=?";
        String logQuery = "INSERT INTO log (mac_address, log_time, action) VALUES (?, ?, ?)";
        
        try {

            PreparedStatement saltStatement = con.prepareStatement(saltQuery);
            saltStatement.setString(1, username);
            ResultSet saltRs = saltStatement.executeQuery();
            if (saltRs.next()) {
                String salt = saltRs.getString("salt");
                String hashedPassword = PasswordHasher.hashPassword(password, salt);
                
                PreparedStatement statement = con.prepareStatement(query);
                statement.setString(1, username);
                ResultSet rs = statement.executeQuery();

                if (rs.next()) {

                    int accountStatus = rs.getInt("account_status");
                    if (accountStatus > 0) {
                        String storedPassword = rs.getString("password");
                        if (hashedPassword.equals(storedPassword)) {

                            
                            PreparedStatement logStatement = con.prepareStatement(logQuery);
                            logStatement.setString(1, macAddress);
                            logStatement.setTimestamp(2, timeStamp);
                            logStatement.setString(3, "login attempt successful");
                            logStatement.executeUpdate();

                            int newAccountStatus = 3;
                            updateAccountStatus(username, newAccountStatus);

                            
                            String role = rs.getString("role");
                            switch (role) {
                                case "admin":
                                    
                                    stage.close();
                                    AdminLogin adminLogin = new AdminLogin(stage, username);
                                    adminLogin.initializeComponents();
                                    break;
                                case "manager":
                                    
                                    stage.close();
                                    ManagerLogin managerLogin = new ManagerLogin();
                                    managerLogin.start(new Stage());
                                    break;
                                case "trainer":
                                    
                                    stage.close();
                                    TrainerLogin trainerLogin = new TrainerLogin(username); 
                                    trainerLogin.start(new Stage());
                                    break;
                                case "nutritionist":
                                    
                                    stage.close();
                                    NutritionistLogin nutritionistLogin = new NutritionistLogin(); 
                                    nutritionistLogin.start(new Stage());
                                    break;
                                case "clerk":
                                    
                                    stage.close();
                                    ClerkLogin clerkLogin = new ClerkLogin();
                                    clerkLogin.start(new Stage());
                                    break;
                                default:
                                    showAlert("Role Not Defined", "User role is not defined.");
                                    break;
                            }
                        } else {
                            // potential integer overflow
                            // mitigate

                            int newAccountStatus = accountStatus - 1;
                            updateAccountStatus(username, newAccountStatus);
                            showAlert("Authentication Failed", "Invalid username or password.");

                            PreparedStatement logStatement = con.prepareStatement(logQuery);
                            logStatement.setString(1, macAddress);
                            logStatement.setTimestamp(2, timeStamp);
                            logStatement.setString(3, "login attempt failed");
                            logStatement.executeUpdate();
                        }
                    } else {
                        
                        showAlert("Account Locked", "Your account is locked. Please contact the administrator.");
                    }
                } else {
                    showAlert("Authentication Failed", "Invalid username or password.");

                    String action = "login attempt failed";
                    PreparedStatement logStatement = con.prepareStatement(logQuery);
                    logStatement.setString(1, GetMacAddress.getMacAddress());
                    logStatement.setTimestamp(2, timeStamp);
                    logStatement.setString(3, action);
                    logStatement.executeUpdate();

                }
                DBUtils.closeConnection(con, statement);
            } else {
                showAlert("Authentication Failed", "Invalid username or password.");

                String action = "login attempt failed";
                PreparedStatement logStatement = con.prepareStatement(logQuery);
                logStatement.setString(1, GetMacAddress.getMacAddress());
                logStatement.setTimestamp(2, timeStamp);
                logStatement.setString(3, action);
                logStatement.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Database Error", "Failed to connect to the database.");
        }
    }

    // Method to update the account status
    private void updateAccountStatus(String username, int newAccountStatus) throws SQLException {
        Connection con = DBUtils.establishConnection();

        if(newAccountStatus == 0){
            String logQuery = "INSERT INTO log (mac_address, log_time, action) VALUES (?, ?, ?)";
            Timestamp timeStamp = new Timestamp(System.currentTimeMillis());
            String action = "account locked";
            PreparedStatement logStatement = con.prepareStatement(logQuery);
            logStatement.setString(1, GetMacAddress.getMacAddress());
            logStatement.setTimestamp(2, timeStamp);
            logStatement.setString(3, action);
            logStatement.executeUpdate();
        }
        String updateQuery = "UPDATE users SET account_status=? WHERE username=?";
        PreparedStatement updateStatement = con.prepareStatement(updateQuery);
        updateStatement.setInt(1, newAccountStatus);
        updateStatement.setString(2, username);
        updateStatement.executeUpdate();
        DBUtils.closeConnection(con, updateStatement);
    }

    // Method to show an alert
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}