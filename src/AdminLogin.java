import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.io.File;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.HBox;
import javafx.geometry.Pos;
import javafx.stage.DirectoryChooser;

public final class AdminLogin {
    private static final double MIN_WIDTH = 300;
    private static final double MIN_HEIGHT = 400;
    private static final double TABLE_MIN_WIDTH = 600;
    private static final double TABLE_MIN_HEIGHT = 400;
    private static final double DIALOG_MIN_WIDTH = 300;
    private static final double DIALOG_MIN_HEIGHT = 200;

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
        adminLayout.setPadding(new Insets(15));

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

        adminScene = new Scene(adminLayout);
        stage.setTitle("Admin Dashboard");
        stage.setScene(adminScene);
        stage.setMinWidth(MIN_WIDTH);
        stage.setMinHeight(MIN_HEIGHT);
        stage.show();
    }

    // Method to create a button with a specified text and action
    private Button createButton(String buttonText, Runnable action) {
        Button button = new Button(buttonText);
        button.setOnAction(event -> action.run());
        return button;
    }

    // Add this helper method near the top of the class
    private Optional<String> showPasswordDialog(String title, String message) {
        Stage dialogStage = new Stage();
        dialogStage.setTitle(title);
        
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        
        Label messageLabel = new Label(message);
        PasswordField passwordField = new PasswordField();
        Button okButton = new Button("OK");
        Button cancelButton = new Button("Cancel");
        
        HBox buttons = new HBox(10);
        buttons.getChildren().addAll(okButton, cancelButton);
        buttons.setAlignment(Pos.CENTER);
        
        content.getChildren().addAll(messageLabel, passwordField, buttons);
        
        Scene scene = new Scene(content);
        dialogStage.setScene(scene);
        
        final String[] password = {null};
        
        okButton.setOnAction(e -> {
            password[0] = passwordField.getText();
            dialogStage.close();
        });
        
        cancelButton.setOnAction(e -> dialogStage.close());
        
        dialogStage.showAndWait();
        
        return Optional.ofNullable(password[0]);
    }

    // Method to backup the database
    private void backupDatabase() {
        Optional<String> zipPassword = showPasswordDialog("Backup Database", "Enter password for the backup file:");
        if (zipPassword.isPresent() && !zipPassword.get().isEmpty()) {
            try {
                // Let user choose backup location
                DirectoryChooser directoryChooser = new DirectoryChooser();
                directoryChooser.setTitle("Select Backup Location");
                directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));
                File selectedDirectory = directoryChooser.showDialog(stage);
                
                if (selectedDirectory != null) {
                    String mysqlUser = "BackUpAdmin";
                    String mysqlPassword = "BackUppasswordforAdmin!!";
                    String backupPath = selectedDirectory.getAbsolutePath();
                    
                    // Perform the backup
                    MySQLBackup.performBackup(mysqlUser, mysqlPassword, zipPassword.get(), backupPath);
                    
                    // Log the backup attempt
                    Connection con = DBUtils.establishConnection();
                    String logQuery = "INSERT INTO log (mac_address, log_time, action) VALUES (?, ?, ?)";
                    PreparedStatement logStatement = con.prepareStatement(logQuery);
                    logStatement.setString(1, GetMacAddress.getMacAddress());
                    logStatement.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
                    logStatement.setString(3, "Database backup created at: " + backupPath);
                    logStatement.executeUpdate();
                    
                    showAlert("Backup Complete", "Database backup has been created successfully at:\n" + backupPath);
                } else {
                    showAlert("Backup Cancelled", "No directory was selected.");
                }
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Backup Failed", "Failed to create database backup: " + e.getMessage());
            }
        } else {
            showAlert("Backup Cancelled", "Backup requires a password for security.");
        }
    }

    private void changePassword() {
        try {
            // Fetch all usernames from database
            Connection con = DBUtils.establishConnection();
            String query = "SELECT username FROM users";
            PreparedStatement statement = con.prepareStatement(query);
            ResultSet rs = statement.executeQuery();
            
            List<String> usernames = new ArrayList<>();
            while (rs.next()) {
                usernames.add(rs.getString("username"));
            }
            
            // Show dialog to select user
            ChoiceDialog<String> dialog = new ChoiceDialog<>(usernames.get(0), usernames);
            dialog.setTitle("Select User");
            dialog.setHeaderText(null);
            dialog.setContentText("Choose user to change password:");
            Optional<String> selectedUser = dialog.showAndWait();
            
            selectedUser.ifPresent(username -> {
                try {
                    // Get user's salt from database
                    String saltQuery = "SELECT salt FROM users WHERE username=?";
                    PreparedStatement saltStmt = con.prepareStatement(saltQuery);
                    saltStmt.setString(1, username);
                    ResultSet saltRs = saltStmt.executeQuery();
                    
                    if (saltRs.next()) {
                        String salt = saltRs.getString("salt");
                        
                        // Get old password
                        Optional<String> oldPassword = showPasswordDialog("Enter Old Password", "Enter old password:");
                        
                        oldPassword.ifPresent(oldPass -> {
                            try {
                                String hashedOldPass = PasswordHasher.hashPassword(oldPass, salt);
                                
                                // Verify old password
                                String verifyQuery = "SELECT password FROM users WHERE username=? AND password=?";
                                PreparedStatement verifyStmt = con.prepareStatement(verifyQuery);
                                verifyStmt.setString(1, username);
                                verifyStmt.setString(2, hashedOldPass);
                                ResultSet verifyRs = verifyStmt.executeQuery();
                                
                                if (verifyRs.next()) {
                                    // Get new password
                                    Optional<String> newPassword = showPasswordDialog("Enter New Password", "Enter new password:");
                                    
                                    newPassword.ifPresent(newPass -> {
                                        // Get password confirmation
                                        Optional<String> confirmPassword = showPasswordDialog("Confirm Password", "Confirm new password:");
                                        
                                        confirmPassword.ifPresent(confirmPass -> {
                                            if (!newPass.equals(confirmPass)) {
                                                showAlert("Error", "Passwords do not match!");
                                                return;
                                            }
                                            
                                            if (!isValidPassword(newPass)) {
                                                showAlert("Invalid Password", "Password must be at least 8 characters long and contain at least one uppercase letter, one lowercase letter, one digit, and one special character.");
                                                return;
                                            }
                                            
                                            try {
                                                String hashedNewPass = PasswordHasher.hashPassword(newPass, salt);
                                                String updateQuery = "UPDATE users SET password=? WHERE username=?";
                                                PreparedStatement updateStmt = con.prepareStatement(updateQuery);
                                                updateStmt.setString(1, hashedNewPass);
                                                updateStmt.setString(2, username);
                                                int result = updateStmt.executeUpdate();
                                                
                                                if (result > 0) {
                                                    showAlert("Success", "Password changed successfully!");
                                                } else {
                                                    showAlert("Error", "Failed to update password!");
                                                }
                                            } catch (SQLException e) {
                                                e.printStackTrace();
                                                showAlert("Database Error", "Failed to update password.");
                                            }
                                        });
                                    });
                                } else {
                                    showAlert("Error", "Incorrect old password!");
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                showAlert("Error", "Failed to verify password.");
                            }
                        });
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    showAlert("Database Error", "Failed to retrieve user information.");
                }
            });
            
            DBUtils.closeConnection(con, statement);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Database Error", "Failed to connect to database.");
        }
    }
    
    private boolean isValidPassword(String password) {
        return password.length() >= 8 && password.matches(".*[a-z].*") && password.matches(".*[A-Z].*") && password.matches(".*\\d.*") && password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*");
    }
    

    // Method to update a user account
    private void updateAccount() {
        try {
            // First, fetch all usernames from database
            Connection con = DBUtils.establishConnection();
            String usersQuery = "SELECT username FROM users";
            PreparedStatement userStatement = con.prepareStatement(usersQuery);
            ResultSet rs = userStatement.executeQuery();
            
            List<String> usernames = new ArrayList<>();
            while (rs.next()) {
                usernames.add(rs.getString("username"));
            }
            
            // Show dialog to select user
            ChoiceDialog<String> userChoiceDialog = new ChoiceDialog<>(usernames.get(0), usernames);
            userChoiceDialog.setTitle("Select User");
            userChoiceDialog.setHeaderText(null);
            userChoiceDialog.setContentText("Choose the user to update:");
            Optional<String> selectedUser = userChoiceDialog.showAndWait();
            
            selectedUser.ifPresent(username -> {
                // Fetch the list of available fields for updating
                List<String> updateFields = List.of("Role", "FirstName", "LastName");

                // Show dialog for field selection
                ChoiceDialog<String> fieldChoiceDialog = new ChoiceDialog<>(updateFields.get(0), updateFields);
                fieldChoiceDialog.setTitle("Choose Field to Update");
                fieldChoiceDialog.setHeaderText(null);
                fieldChoiceDialog.setContentText("Choose the field to update:");
                Optional<String> chosenField = fieldChoiceDialog.showAndWait();

                chosenField.ifPresent(field -> {
                    TextInputDialog valueInputDialog = new TextInputDialog();
                    valueInputDialog.setTitle("Enter New Value");
                    valueInputDialog.setHeaderText(null);
                    valueInputDialog.setContentText("Enter the new value for " + field + ":");
                    Optional<String> newValue = valueInputDialog.showAndWait();

                    newValue.ifPresent(newValueStr -> {
                        try {
                            String updateQuery = "UPDATE users SET " + field.toLowerCase() + " = ? WHERE username = ?";
                            PreparedStatement updateStatement = con.prepareStatement(updateQuery);
                            updateStatement.setString(1, newValueStr);
                            updateStatement.setString(2, username);
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
                                // Log failed update
                                String logQuery = "INSERT INTO log (mac_address, log_time, action) VALUES (?, ?, ?)";
                                Timestamp timeStamp = new Timestamp(System.currentTimeMillis());
                                String action = "User account failed to update";
                                PreparedStatement logStatement = con.prepareStatement(logQuery);
                                logStatement.setString(1, GetMacAddress.getMacAddress());
                                logStatement.setTimestamp(2, timeStamp);
                                logStatement.setString(3, action);
                                logStatement.executeUpdate();
                            }
                        } catch (SQLException e) {
                            e.printStackTrace();
                            showAlert("Database Error", "Failed to update the account.");
                        }
                    });
                });
            });
            
            DBUtils.closeConnection(con, userStatement);
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

            ObservableList<UserData> users = FXCollections.observableArrayList();
            while (rs.next()) {
                users.add(new UserData(
                    rs.getString("username"),
                    rs.getString("role"),
                    rs.getString("firstname"),
                    rs.getString("lastname"),
                    rs.getString("account_status")
                ));
            }

            displayUserAccounts(users);
            DBUtils.closeConnection(con, statement);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Database Error", "Failed to connect to the database.");
        }
    }

    private void displayUserAccounts(ObservableList<UserData> users) {
        TableView<UserData> tableView = new TableView<>();
        
        TableColumn<UserData, String> usernameCol = new TableColumn<>("Username");
        usernameCol.setCellValueFactory(data -> data.getValue().usernameProperty());
        
        TableColumn<UserData, String> roleCol = new TableColumn<>("Role");
        roleCol.setCellValueFactory(data -> data.getValue().roleProperty());
        
        TableColumn<UserData, String> firstNameCol = new TableColumn<>("First Name");
        firstNameCol.setCellValueFactory(data -> data.getValue().firstNameProperty());
        
        TableColumn<UserData, String> lastNameCol = new TableColumn<>("Last Name");
        lastNameCol.setCellValueFactory(data -> data.getValue().lastNameProperty());
        
        TableColumn<UserData, String> statusCol = new TableColumn<>("Account Status");
        statusCol.setCellValueFactory(data -> data.getValue().statusProperty());

        tableView.getColumns().addAll(usernameCol, roleCol, firstNameCol, lastNameCol, statusCol);
        tableView.setItems(users);

        // Adjust column widths based on content
        double columnWidth = TABLE_MIN_WIDTH / 5;  // 5 columns
        for (TableColumn<UserData, ?> column : tableView.getColumns()) {
            column.setPrefWidth(columnWidth);
            column.setMinWidth(columnWidth);
        }

        VBox vbox = new VBox(tableView);
        vbox.setPadding(new Insets(10));

        Scene scene = new Scene(vbox);
        Stage stage = new Stage();
        stage.setTitle("All User Accounts");
        stage.setScene(scene);
        stage.setMinWidth(TABLE_MIN_WIDTH);
        stage.setMinHeight(TABLE_MIN_HEIGHT);
        stage.show();
    }

    // Add this inner class to handle table data
    private static class UserData {
        private final SimpleStringProperty username;
        private final SimpleStringProperty role;
        private final SimpleStringProperty firstName;
        private final SimpleStringProperty lastName;
        private final SimpleStringProperty status;

        UserData(String username, String role, String firstName, String lastName, String status) {
            this.username = new SimpleStringProperty(username);
            this.role = new SimpleStringProperty(role);
            this.firstName = new SimpleStringProperty(firstName);
            this.lastName = new SimpleStringProperty(lastName);
            this.status = new SimpleStringProperty(mapStatus(status));
        }

        private static String mapStatus(String status) {
            return switch (status) {
                case "0" -> "Locked";
                case "1" -> "First Login";
                case "2" -> "Password Reset";
                case "3" -> "Active";
                default -> "Unknown";
            };
        }

        public SimpleStringProperty usernameProperty() { return username; }
        public SimpleStringProperty roleProperty() { return role; }
        public SimpleStringProperty firstNameProperty() { return firstName; }
        public SimpleStringProperty lastNameProperty() { return lastName; }
        public SimpleStringProperty statusProperty() { return status; }
    }

    // Method to unlock a user account
    private void unlockAccount() {
        try {
            Connection con = DBUtils.establishConnection();
            String query = "SELECT username FROM users WHERE account_status < 3";
            PreparedStatement statement = con.prepareStatement(query);
            ResultSet rs = statement.executeQuery();

            VBox dialogContent = new VBox(10);
            dialogContent.setPadding(new Insets(15));
            Label label = new Label("Select users to unlock:");
            dialogContent.getChildren().add(label);

            List<CheckBox> checkBoxes = new ArrayList<>();
            boolean hasLockedUsers = false;
            
            while (rs.next()) {
                hasLockedUsers = true;
                String username = rs.getString("username");
                CheckBox checkBox = new CheckBox();
                checkBox.setText(username); // Set text directly instead of through constructor
                checkBox.setWrapText(true); // Enable text wrapping
                checkBox.setMaxWidth(Double.MAX_VALUE); // Allow checkbox to expand
                checkBoxes.add(checkBox);
                dialogContent.getChildren().add(checkBox);
            }

            if (!hasLockedUsers) {
                showAlert("No Locked Users", "There are no locked user accounts.");
                DBUtils.closeConnection(con, statement);
                return;
            }

            Button unlockButton = new Button("Unlock Selected");
            dialogContent.getChildren().add(unlockButton);

            Stage dialog = new Stage();
            Scene scene = new Scene(dialogContent);
            dialog.setScene(scene);
            dialog.setTitle("Unlock Accounts");
            dialog.setMinWidth(DIALOG_MIN_WIDTH);
            dialog.setMinHeight(DIALOG_MIN_HEIGHT);

            DBUtils.closeConnection(con, statement);

            unlockButton.setOnAction(e -> {
                boolean anySelected = checkBoxes.stream().anyMatch(CheckBox::isSelected);
                
                if (!anySelected) {
                    showAlert("Warning", "Please select at least one user to unlock.");
                    return;
                }

                boolean anyUnlocked = false;
                try {
                    Connection updateCon = DBUtils.establishConnection();
                    
                    for (CheckBox cb : checkBoxes) {
                        if (cb.isSelected()) {
                            try {
                                String updateQuery = "UPDATE users SET account_status = 3 WHERE username = ?";
                                PreparedStatement updateStatement = updateCon.prepareStatement(updateQuery);
                                updateStatement.setString(1, cb.getText());
                                int result = updateStatement.executeUpdate();

                                if (result > 0) {
                                    String logQuery = "INSERT INTO log (mac_address, log_time, action) VALUES (?, ?, ?)";
                                    PreparedStatement logStatement = updateCon.prepareStatement(logQuery);
                                    Timestamp timeStamp = new Timestamp(System.currentTimeMillis());
                                    String action = "account unlocked by admin: " + cb.getText();
                                    logStatement.setString(1, GetMacAddress.getMacAddress());
                                    logStatement.setTimestamp(2, timeStamp);
                                    logStatement.setString(3, action);
                                    logStatement.executeUpdate();
                                    anyUnlocked = true;
                                }
                            } catch (SQLException ex) {
                                ex.printStackTrace();
                                showAlert("Error", "Failed to unlock user: " + cb.getText());
                            }
                        }
                    }
                    
                    updateCon.close();
                    
                    if (anyUnlocked) {
                        dialog.close();
                        showAlert("Success", "Selected accounts have been unlocked");
                    }
                    
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    showAlert("Database Error", "Failed to unlock accounts");
                }
            });

            dialog.show();

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Database Error", "Failed to retrieve locked accounts");
        }
    }

    // Method to delete a user account
    private void deleteAccount() {
        try {
            // Fetch all usernames from database
            Connection con = DBUtils.establishConnection();
            String query = "SELECT username FROM users WHERE username != ?";
            PreparedStatement statement = con.prepareStatement(query);
            statement.setString(1, this.username); // Exclude current admin
            ResultSet rs = statement.executeQuery();
            
            List<String> usernames = new ArrayList<>();
            while (rs.next()) {
                usernames.add(rs.getString("username"));
            }
            
            if (usernames.isEmpty()) {
                showAlert("No Users", "There are no users available to delete.");
                return;
            }

            // Show dialog to select user
            ChoiceDialog<String> dialog = new ChoiceDialog<>(usernames.get(0), usernames);
            dialog.setTitle("Select User to Delete");
            dialog.setHeaderText("WARNING: This action is irreversible!");
            dialog.setContentText("Choose user to delete:");
            Optional<String> selectedUser = dialog.showAndWait();
            
            selectedUser.ifPresent(userToDelete -> {
                // Get admin password verification
                Optional<String> adminPassword = showPasswordDialog("Admin Verification", "Enter your admin password:");
                
                adminPassword.ifPresent(adminPass -> {
                    try {
                        // Verify admin password
                        String adminVerifyQuery = "SELECT password, salt FROM users WHERE username = ?";
                        PreparedStatement adminVerifyStmt = con.prepareStatement(adminVerifyQuery);
                        adminVerifyStmt.setString(1, this.username);
                        ResultSet adminVerifyRs = adminVerifyStmt.executeQuery();
                        
                        if (adminVerifyRs.next()) {
                            String salt = adminVerifyRs.getString("salt");
                            String hashedAdminPass = PasswordHasher.hashPassword(adminPass, salt);
                            
                            if (!hashedAdminPass.equals(adminVerifyRs.getString("password"))) {
                                showAlert("Error", "Invalid admin password!");
                                return;
                            }
                            
                            // Get user password verification
                            Optional<String> userPassword = showPasswordDialog("User Verification", "Enter password for user: " + userToDelete);
                            
                            userPassword.ifPresent(userPass -> {
                                try {
                                    // Verify user password
                                    String userVerifyQuery = "SELECT password, salt FROM users WHERE username = ?";
                                    PreparedStatement userVerifyStmt = con.prepareStatement(userVerifyQuery);
                                    userVerifyStmt.setString(1, userToDelete);
                                    ResultSet userVerifyRs = userVerifyStmt.executeQuery();
                                    
                                    if (userVerifyRs.next()) {
                                        String userSalt = userVerifyRs.getString("salt");
                                        String hashedUserPass = PasswordHasher.hashPassword(userPass, userSalt);
                                        
                                        if (!hashedUserPass.equals(userVerifyRs.getString("password"))) {
                                            showAlert("Error", "Invalid user password!");
                                            return;
                                        }
                                        
                                        // Final confirmation
                                        Alert confirmAlert = new Alert(Alert.AlertType.WARNING);
                                        confirmAlert.setTitle("Final Confirmation");
                                        confirmAlert.setHeaderText("This action is IRREVERSIBLE!");
                                        confirmAlert.setContentText("Are you absolutely sure you want to delete user: " + userToDelete + "?");
                                        confirmAlert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
                                        
                                        confirmAlert.showAndWait().ifPresent(response -> {
                                            if (response == ButtonType.YES) {
                                                try {
                                                    String deleteQuery = "DELETE FROM users WHERE username = ?";
                                                    PreparedStatement deleteStmt = con.prepareStatement(deleteQuery);
                                                    deleteStmt.setString(1, userToDelete);
                                                    int result = deleteStmt.executeUpdate();
                                                    
                                                    if (result > 0) {
                                                        // Log the deletion
                                                        String logQuery = "INSERT INTO log (mac_address, log_time, action) VALUES (?, ?, ?)";
                                                        PreparedStatement logStmt = con.prepareStatement(logQuery);
                                                        logStmt.setString(1, GetMacAddress.getMacAddress());
                                                        logStmt.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
                                                        logStmt.setString(3, "User account deleted: " + userToDelete);
                                                        logStmt.executeUpdate();
                                                        
                                                        showAlert("Success", "User " + userToDelete + " has been deleted.");
                                                    }
                                                } catch (SQLException e) {
                                                    e.printStackTrace();
                                                    showAlert("Error", "Failed to delete user account.");
                                                }
                                            }
                                        });
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    showAlert("Error", "Failed to verify user credentials.");
                                }
                            });
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        showAlert("Error", "Failed to verify admin credentials.");
                    }
                });
            });
            
            DBUtils.closeConnection(con, statement);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Database Error", "Failed to connect to database.");
        }
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
        alert.setResizable(true);
        alert.getDialogPane().setMinWidth(DIALOG_MIN_WIDTH);
        alert.getDialogPane().setMinHeight(DIALOG_MIN_HEIGHT / 2);
        alert.showAndWait();
    }
}