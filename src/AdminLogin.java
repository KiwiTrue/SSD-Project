import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.sql.Timestamp;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public final class AdminLogin {
    private Scene adminScene;
    private String username;
    private Stage stage;

    // Constructor to initialize the AdminLogin class with the primary stage and the username
    public AdminLogin(Stage primaryStage, String username) {
        this.stage = primaryStage;
        this.username = username;
    }

    // Method to initialize the components of the admin dashboard
    public void initializeComponents() {
        VBox adminLayout = new VBox(8);
        adminLayout.setPadding(new Insets(8));

        Button registrationButton = createButton("Register Account", this::openRegistrationPage);
        Button viewAccountsButton = createButton("View All Accounts", this::viewAllAccounts);
        Button unlockAccountButton = createButton("Unlock Account", this::unlockAccount);
        Button updateButton = createButton("Update Account", this::updateAccount);
        Button changePasswordButton = createButton("Change password", this::changePassword);

        Button deleteButton = createButton("Delete Account", () -> deleteAccount());
        Button backupButton = createButton("Backup Database", this::backupDatabase); // Backup button
        Button backButton = createButton("Log Out", this::goBackToLogin);

        adminLayout.getChildren().addAll(
                new Label("Welcome " + username),
                registrationButton,
                unlockAccountButton,
                viewAccountsButton,
                updateButton,
                changePasswordButton,
                deleteButton,
                backupButton, // Backup button
                backButton
        );

        adminScene = new Scene(adminLayout, 400, 300);
        stage.setTitle("Admin Dashboard");
        stage.setScene(adminScene);
        stage.show();
    }

    // Method to create a button with a specified text and action
    private Button createButton(String buttonText, Runnable action) {
        Button button = new Button(buttonText);
        button.setOnAction(event -> action.run());
        return button;
    }

    // Method to backup the database
    private void backupDatabase() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Backup Database");
        dialog.setHeaderText(null);
        dialog.setContentText("Enter password for the backup file:");
        Optional<String> result = dialog.showAndWait();
        String zipPassword = result.orElse(null);
        String mysqlUser = "BackUpAdmin";
        String mysqlPassword = "BackUppasswordforAdmin!!";
        String backupPath = "C:/Users/admin/Desktop/Backup";
        MySQLBackup.performBackup(mysqlUser, mysqlPassword, zipPassword, backupPath);

    }
    private void changePassword() {
        // Prompt admin to enter the username of the user whose password they want to change
        TextInputDialog usernameInputDialog = new TextInputDialog();
        usernameInputDialog.setTitle("Enter Username to Change Password");
        usernameInputDialog.setHeaderText(null);
        usernameInputDialog.setContentText("Enter the username whose password you want to change:");
        Optional<String> usernameInput = usernameInputDialog.showAndWait();
    
        usernameInput.ifPresent(username -> {
            try {
                Connection con = DBUtils.establishConnection();
                
                // Retrieve user's information from the database, including their salt
                String query = "SELECT * FROM users WHERE username=?";
                PreparedStatement statement = con.prepareStatement(query);
                statement.setString(1, username);
                ResultSet rs = statement.executeQuery();
    
                if (rs.next()) {
                    String salt = rs.getString("salt");
    
                    TextInputDialog passwordInputDialog = new TextInputDialog();
                    passwordInputDialog.setTitle("Enter Old Password");
                    passwordInputDialog.setHeaderText(null);
                    passwordInputDialog.setContentText("Enter the old password:");
                    Optional<String> oldPasswordInput = passwordInputDialog.showAndWait();
    
                    oldPasswordInput.ifPresent(oldPassword -> {
                        try {
                            String hashedOldPassword = PasswordHasher.hashPassword(oldPassword, salt);
    
                            String storedPassword = rs.getString("password");
                            if (hashedOldPassword.equals(storedPassword)) {
                                TextInputDialog newPasswordInputDialog = new TextInputDialog();
                                newPasswordInputDialog.setTitle("Enter New Password");
                                newPasswordInputDialog.setHeaderText(null);
                                newPasswordInputDialog.setContentText("Enter the new password:");
    
                                Optional<String> newPasswordInput = newPasswordInputDialog.showAndWait();
    
                                newPasswordInput.ifPresent(newPassword -> {
                                    if (isValidPassword(newPassword)) {
                                        try {
                                            String hashedNewPassword = PasswordHasher.hashPassword(newPassword, salt);
    
                                            String updateQuery = "UPDATE users SET password = ? WHERE username = ?";
                                            PreparedStatement updateStatement = con.prepareStatement(updateQuery);
                                            updateStatement.setString(1, hashedNewPassword);
                                            updateStatement.setString(2, username);
                                            int rowsAffected = updateStatement.executeUpdate();
    
                                            if (rowsAffected > 0) {
                                                showAlert("Password Changed", "Password changed successfully.");
                                            } else {
                                                showAlert("Password Change Failed", "Failed to change password.");
                                            }
    
                                            DBUtils.closeConnection(con, updateStatement);
                                        } catch (SQLException e) {
                                            e.printStackTrace();
                                            showAlert("Database Error", "Failed to update password.");
                                        }
                                    } else {
                                        showAlert("Invalid Password", "Password must be at least 8 characters long and contain at least one uppercase letter, one lowercase letter, one digit, and one special character.");
                                    }
                                });
                            } else {
                                showAlert("Invalid Password", "Old password entered is incorrect.");
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            showAlert("Error", "An error occurred while hashing the password.");
                        }
                    });
                } else {
                    showAlert("User Not Found", "No user found with the provided username.");
                }
    
                DBUtils.closeConnection(con, statement);
            } catch (SQLException e) {
                e.printStackTrace();
                showAlert("Database Error", "Failed to connect to the database.");
            }
        });
    }
    
    private boolean isValidPassword(String password) {
        return password.length() >= 8 && password.matches(".*[a-z].*") && password.matches(".*[A-Z].*") && password.matches(".*\\d.*") && password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*");
    }
    

    // Method to update a user account
    private void updateAccount() {
        try {
            // Fetch the list of available fields for updating
            List<String> updateFields = List.of("Role", "FirstName", "LastName");  // Add more fields if needed

            // Show a dialog for the admin to choose the field to update
            ChoiceDialog<String> fieldChoiceDialog = new ChoiceDialog<>(updateFields.get(0), updateFields);
            fieldChoiceDialog.setTitle("Choose Field to Update");
            fieldChoiceDialog.setHeaderText(null);
            fieldChoiceDialog.setContentText("Choose the field to update:");
            Optional<String> chosenField = fieldChoiceDialog.showAndWait();

            chosenField.ifPresent(field -> {
                // Show a dialog to input the new value for the chosen field
                TextInputDialog valueInputDialog = new TextInputDialog();
                valueInputDialog.setTitle("Enter New Value");
                valueInputDialog.setHeaderText(null);
                valueInputDialog.setContentText("Enter the new value for " + field + ":");
                Optional<String> newValue = valueInputDialog.showAndWait();

                newValue.ifPresent(newValueStr -> {
                    // Update the selected field for a specific user (in this case, updating by username)
                    String usernameToUpdate = getUsernameToUpdate();  // Implement the logic to get the username you want to update
                    if (usernameToUpdate != null) {
                        try {
                            Connection con = DBUtils.establishConnection();
                            String updateQuery = "UPDATE users SET " + field.toLowerCase() + " = ? WHERE username = ?";
                            PreparedStatement updateStatement = con.prepareStatement(updateQuery);
                            updateStatement.setString(1, newValueStr);
                            updateStatement.setString(2, usernameToUpdate);
                            int rowsAffected = updateStatement.executeUpdate();

                            if (rowsAffected > 0) {
                                showAlert("Update Successful", "User account updated successfully.");  
                    
                                String logQuery = "INSERT INTO log (mac_address, log_time, action) VALUES (?, ?, ?)";
                                Timestamp timeStamp = new Timestamp(System.currentTimeMillis());
                                String action = "User account updated";
                                PreparedStatement logStatement = con.prepareStatement(logQuery);
                                logStatement.setString(1, GetMacAddress.getMacAddress());
                                logStatement.setTimestamp(2, timeStamp);
                                logStatement.setString(3, action);
                                logStatement.executeUpdate();
                            } else {
                                showAlert("Update Failed", "Failed to update user account.");
                    
                                String logQuery = "INSERT INTO log (mac_address, log_time, action) VALUES (?, ?, ?)";
                                Timestamp timeStamp = new Timestamp(System.currentTimeMillis());
                                String action = "User account failed to update";
                                PreparedStatement logStatement = con.prepareStatement(logQuery);
                                logStatement.setString(1, GetMacAddress.getMacAddress());
                                logStatement.setTimestamp(2, timeStamp);
                                logStatement.setString(3, action);
                                logStatement.executeUpdate();
                            }

                            DBUtils.closeConnection(con, updateStatement);
                        } catch (SQLException e) {
                            e.printStackTrace();
                            showAlert("Database Error", "Failed to connect to the database.");
                        }
                    }
                });
            });
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "An error occurred while updating the account.");
        }
    }

    // Method to get the username to update
    private String getUsernameToUpdate() {
        TextInputDialog usernameInputDialog = new TextInputDialog();
        usernameInputDialog.setTitle("Enter Username");
        usernameInputDialog.setHeaderText(null);
        usernameInputDialog.setContentText("Enter the username you want to update:");
        
        Optional<String> usernameInput = usernameInputDialog.showAndWait();

        return usernameInput.orElse(null);
    }

    // Method to open the registration page
    private void openRegistrationPage() {
        UserSignup registration = new UserSignup(stage);
        registration.initializeComponents();
    }

    // Method to view all user accounts
    private void viewAllAccounts() {
            try {
                Connection con = DBUtils.establishConnection();
                String query = "SELECT * FROM users";
                PreparedStatement statement = con.prepareStatement(query);
                ResultSet rs = statement.executeQuery();

                displayUserAccounts(rs);

                DBUtils.closeConnection(con, statement);
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Database Error", "Failed to connect to the database.");
            }
        }

    // Method to display all user accounts
    private void displayUserAccounts(ResultSet rs) throws SQLException {
        VBox accountLayout = new VBox(10);
        accountLayout.setPadding(new Insets(10));

        while (rs.next()) {
            String username = rs.getString("username");
            String role = rs.getString("role");
            String firstName = rs.getString("firstname");
            String lastName = rs.getString("lastname");

            Label accountLabel = new Label("Username: " + username +
                    ", Role: " + role +
                    ", First Name: " + firstName +
                    ", Last Name: " + lastName);

            accountLayout.getChildren().add(accountLabel);
        }

        Scene accountScene = new Scene(accountLayout, 400, 300);
        Stage accountStage = new Stage();
        accountStage.setTitle("All User Accounts");
        accountStage.setScene(accountScene);
        accountStage.show();
    }

    // Method to unlock a user account
    private void unlockAccount() {
        String usernameToUnlock = getUsernameToUnlock();

        if (usernameToUnlock != null && !usernameToUnlock.isEmpty()) {
            try {
                Connection con = DBUtils.establishConnection();
                String updateQuery = "UPDATE users SET account_status = '3' WHERE username = ?";
                PreparedStatement updateStatement = con.prepareStatement(updateQuery);
                updateStatement.setString(1, usernameToUnlock);

                int result = updateStatement.executeUpdate();

                if (result == 1) {
                    showAlert("Unlock Successful", "User account unlocked successfully.");
                
                    String logQuery = "INSERT INTO log (mac_address, log_time, action) VALUES (?, ?, ?)";
                    Timestamp timeStamp = new Timestamp(System.currentTimeMillis());
                    String action = "account unlocked by admin";
                    PreparedStatement logStatement = con.prepareStatement(logQuery);
                    logStatement.setString(1, GetMacAddress.getMacAddress());
                    logStatement.setTimestamp(2, timeStamp);
                    logStatement.setString(3, action);
                    logStatement.executeUpdate();
                } else {
                    showAlert("Unlock Failed", "Failed to unlock user account.");

                    String logQuery = "INSERT INTO log (mac_address, log_time, action) VALUES (?, ?, ?)";
                    Timestamp timeStamp = new Timestamp(System.currentTimeMillis());
                    String action = "account unlock failed";
                    PreparedStatement logStatement = con.prepareStatement(logQuery);
                    logStatement.setString(1, GetMacAddress.getMacAddress());
                    logStatement.setTimestamp(2, timeStamp);
                    logStatement.setString(3, action);
                    logStatement.executeUpdate();
                }

                DBUtils.closeConnection(con, updateStatement);
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Database Error", "Failed to connect to the database.");
            }
        } else {
            showAlert("No User Selected", "Please select a user to unlock.");
        }
    }

    // Method to get the username to unlock
    private String getUsernameToUnlock() {
        TextInputDialog usernameInputDialog = new TextInputDialog();
        usernameInputDialog.setTitle("Enter Username to Unlock");
        Optional<String> usernameInput = usernameInputDialog.showAndWait();

        return usernameInput.orElse(null);
    }

    // Method to delete a user account
    private void deleteAccount() {
        String usernameToDelete = getUsernameToDelete();


        // Check if a username is selected
        if (usernameToDelete != null && !usernameToDelete.isEmpty()) {
            // Confirm deletion with the user
            Alert confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmationAlert.setTitle("Delete Account");
            confirmationAlert.setHeaderText("Are you sure you want to delete the account for user: " + usernameToDelete + "?");

            confirmationAlert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    // Delete the user's account from the database
                    try {
                        Connection con = DBUtils.establishConnection();
                        String deleteQuery = "DELETE FROM users WHERE username = ?";
                        PreparedStatement deleteStatement = con.prepareStatement(deleteQuery);
                        deleteStatement.setString(1, usernameToDelete);

                        int result = deleteStatement.executeUpdate();

                        if (result == 1) {
                            showAlert("Deletion Successful", "User account deleted successfully.");
                    
                            String logQuery = "INSERT INTO log (mac_address, log_time, action) VALUES (?, ?, ?)";
                            Timestamp timeStamp = new Timestamp(System.currentTimeMillis());
                            String action = "User account deleted by admin";
                            PreparedStatement logStatement = con.prepareStatement(logQuery);
                            logStatement.setString(1, GetMacAddress.getMacAddress());
                            logStatement.setTimestamp(2, timeStamp);
                            logStatement.setString(3, action);
                            logStatement.executeUpdate();
                            
                        } else {
                            showAlert("Deletion Failed", "Failed to delete user account.");
                    
                            String logQuery = "INSERT INTO log (mac_address, log_time, action) VALUES (?, ?, ?)";
                            Timestamp timeStamp = new Timestamp(System.currentTimeMillis());
                            String action = "User account deletion failed";
                            PreparedStatement logStatement = con.prepareStatement(logQuery);
                            logStatement.setString(1, GetMacAddress.getMacAddress());
                            logStatement.setTimestamp(2, timeStamp);
                            logStatement.setString(3, action);
                            logStatement.executeUpdate();
                        }

                        DBUtils.closeConnection(con, deleteStatement);
                    } catch (Exception e) {
                        e.printStackTrace();
                        showAlert("Database Error", "Failed to connect to the database.");
                    }
                }
            });
        } else {
            showAlert("No User Selected", "Please select a user to delete.");
        }
    }

    // Method to get the username to delete
    private String getUsernameToDelete() {
        TextInputDialog usernameInputDialog = new TextInputDialog();
        usernameInputDialog.setTitle("Enter Username want to delete");
        Optional<String> usernameInput = usernameInputDialog.showAndWait();

        return usernameInput.orElse(null);
    }

    // Method to go back to the login page
    private void goBackToLogin() {
        UserLogin login = new UserLogin(stage);
        login.initializeComponents();
    }

    // Method to show an alert with a specified title and content
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}