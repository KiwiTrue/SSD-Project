import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.sql.Timestamp;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public final class ClerkLogin extends Application {

    // Instance variables
    private Stage stage;
    private Connection con;

    // Main method to launch the application
    public static void main(String[] args) {
        launch(args);
    }

    // Method to start the clerk login page
    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        con = DBUtils.establishConnection(); // Establish database connection
        initializeComponents();
    }

    // Method to initialize the components of the clerk login page
    void initializeComponents() {
        // Initialize login components
        VBox loginLayout = new VBox(10);
        loginLayout.setPadding(new Insets(10));
    
        Button registerButton = new Button("Register Client");
        registerButton.setOnAction(this::openCustomerRegistration); // Connect to CustomerRegistration
    
        Button renewButton = new Button("Renew Subscription");
        renewButton.setOnAction(this::renewSubscription);
    
        Button updateButton = new Button("Update Customer Details");
        updateButton.setOnAction(this::updateCustomerDetails);
    
        Button trackButton = new Button("Track");
        trackButton.setOnAction(this::trackSubscription);
    
        Button backButton = new Button("Log Out");
        backButton.setOnAction(this::goBack);
    
        loginLayout.getChildren().addAll(registerButton, renewButton, updateButton, trackButton, backButton);
    
        Scene loginScene = new Scene(loginLayout, 300, 450);
        stage.setTitle("Clerk Login");
        stage.setScene(loginScene);
        stage.show();
    }
    
    // Method to open the customer registration page
    private void openCustomerRegistration(ActionEvent event) {
        // Replace the current scene with the CustomerRegistration interface
        CustomerRegistration customerRegistration = new CustomerRegistration();
        customerRegistration.start(stage);
    }

    // Method to open the subscription renewal page
    private void renewSubscription(ActionEvent event) {
        CustomerRenew customerRenew = new CustomerRenew();
        customerRenew.start(stage);
    }

    // Method to track subscriptions
    private void trackSubscription(ActionEvent event) {
        try {
            LocalDate currentDate = LocalDate.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            String formattedCurrentDate = currentDate.format(formatter);
            
            String query = "SELECT * FROM customers WHERE subscription_end < ?";
            PreparedStatement statement = con.prepareStatement(query);
            statement.setString(1, formattedCurrentDate);
            
            ResultSet resultSet = statement.executeQuery();
            
            if (!resultSet.isBeforeFirst()) {
                showAlert("Subscription Tracking", "No accounts with expired subscriptions found.");
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append("Accounts with expired subscriptions:\n\n");
                while (resultSet.next()) {
                    String customerId = resultSet.getString("phone_number");
                    String subscriptionEndDate = resultSet.getString("subscription_end");
                    sb.append("phone_number: ").append(customerId).append(", Subscription End Date: ").append(subscriptionEndDate).append("\n");
                }
                showAlert("Subscription Tracking", sb.toString());
            }
            
            DBUtils.closeConnection(con, statement);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Database Error", "Failed to track subscriptions: " + e.getMessage());
        }
    }
    
    // Method to update customer details
    private void updateCustomerDetails(ActionEvent event) {
        try {
            // Fetch the list of available fields for updating
            List<String> updateFields = List.of("email", "firstName", "lastName", "phone_number");  // Add more fields if needed
    
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
                    // Update the selected field for a specific user (in this case, updating by phone number)
                    String phoneNumberToUpdate = getPhoneNumberToUpdate();  // Implement the logic to get the phone number you want to update
                    if (phoneNumberToUpdate != null) {
                        try {
                            Connection con = DBUtils.establishConnection();
                            String updateQuery = "UPDATE users SET " + field.toLowerCase() + " = ? WHERE phone_number = ?";
                            PreparedStatement updateStatement = con.prepareStatement(updateQuery);
                            updateStatement.setString(1, newValueStr);
                            updateStatement.setString(2, phoneNumberToUpdate);
                            int rowsAffected = updateStatement.executeUpdate();
    
                            if (rowsAffected > 0) {
                                showAlert("Update Successful", "User account updated successfully.");
                                
                                String logQuery = "INSERT INTO log (mac_address, log_time, action) VALUES (?, ?, ?)";
                                Timestamp timeStamp = new Timestamp(System.currentTimeMillis());
                                String action = "Customer details updated";
                                PreparedStatement logStatement = con.prepareStatement(logQuery);
                                logStatement.setString(1, GetMacAddress.getMacAddress());
                                logStatement.setTimestamp(2, timeStamp);
                                logStatement.setString(3, action);
                                logStatement.executeUpdate();
                            } else {
                                showAlert("Update Failed", "Failed to update user account.");
                
                                String logQuery = "INSERT INTO log (mac_address, log_time, action) VALUES (?, ?, ?)";
                                Timestamp timeStamp = new Timestamp(System.currentTimeMillis());
                                String action = "Customer details update failed";
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
    
    // Method to retrieve the phone number from the database
    private String getPhoneNumberToUpdate() {
        TextInputDialog phoneNumberInputDialog = new TextInputDialog();
        phoneNumberInputDialog.setTitle("Enter Phone Number");
        phoneNumberInputDialog.setHeaderText(null);
        phoneNumberInputDialog.setContentText("Enter the phone number you want to update:");
        
        Optional<String> phoneNumberInput = phoneNumberInputDialog.showAndWait();
        
    
        return phoneNumberInput.orElse(null);
    }

    // Method to log out and go back to the main login screen
    private void goBack(ActionEvent event) {
        stage.close();
        App userLogin = new App();
        userLogin.start(new Stage());
    }

    @Override
    public void stop() {
        // Close database connection when the application stops
        try {
            if (con != null) {
                con.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Method to show an alert dialog
    private void showAlert(String title, String content) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
