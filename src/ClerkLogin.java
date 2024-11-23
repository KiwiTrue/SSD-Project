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
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.FXCollections;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.time.temporal.ChronoUnit;

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
            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Expired Subscriptions");
            dialog.setHeaderText("Customers with Expired Subscriptions");

            // Create TableView
            TableView<ExpiredCustomer> table = new TableView<>();
            
            // Create columns
            TableColumn<ExpiredCustomer, String> phoneCol = new TableColumn<>("Phone");
            phoneCol.setCellValueFactory(new PropertyValueFactory<>("phoneNumber"));
            
            TableColumn<ExpiredCustomer, String> nameCol = new TableColumn<>("Name");
            nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
            
            TableColumn<ExpiredCustomer, String> endDateCol = new TableColumn<>("End Date");
            endDateCol.setCellValueFactory(new PropertyValueFactory<>("endDate"));
            
            TableColumn<ExpiredCustomer, Long> daysExpiredCol = new TableColumn<>("Days Expired");
            daysExpiredCol.setCellValueFactory(new PropertyValueFactory<>("daysExpired"));

            table.getColumns().addAll(phoneCol, nameCol, endDateCol, daysExpiredCol);

            // Fetch expired subscriptions
            LocalDate currentDate = LocalDate.now();
            String query = "SELECT phone_number, firstname, lastname, subscription_end FROM customers WHERE subscription_end < ?";
            PreparedStatement statement = con.prepareStatement(query);
            statement.setString(1, currentDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            
            ResultSet rs = statement.executeQuery();
            ObservableList<ExpiredCustomer> expiredCustomers = FXCollections.observableArrayList();
            
            while (rs.next()) {
                String phone = rs.getString("phone_number");
                String name = rs.getString("firstname") + " " + rs.getString("lastname");
                String endDate = rs.getString("subscription_end");
                
                // Calculate days since expiry
                LocalDate endLocalDate = LocalDate.parse(endDate.replace("/", "-"));
                long daysExpired = ChronoUnit.DAYS.between(endLocalDate, currentDate);
                
                expiredCustomers.add(new ExpiredCustomer(phone, name, endDate, daysExpired));
            }

            table.setItems(expiredCustomers);

            // Set minimum dimensions
            table.setMinWidth(500);
            table.setMinHeight(300);
            
            // Make columns auto-resize
            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

            GridPane content = new GridPane();
            content.add(table, 0, 0);
            GridPane.setVgrow(table, Priority.ALWAYS);
            GridPane.setHgrow(table, Priority.ALWAYS);
            content.setPadding(new Insets(10));

            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            
            // If no expired subscriptions found
            if (expiredCustomers.isEmpty()) {
                showAlert("No Expired Subscriptions", "There are no expired subscriptions at this time.");
                return;
            }

            dialog.showAndWait();
            
        } catch (SQLException e) {
            showAlert("Error", "Failed to fetch expired subscriptions: " + e.getMessage());
        }
    }

    // Method to update customer details
    private void updateCustomerDetails(ActionEvent event) {
        try {
            // Create a dialog for customer selection
            Dialog<String> dialog = new Dialog<>();
            dialog.setTitle("Update Customer Details");
            dialog.setHeaderText("Select a customer to update");
    
            // Create a dropdown list of customers
            ComboBox<String> customerComboBox = new ComboBox<>();
            ObservableList<String> customers = FXCollections.observableArrayList();
            
            // Fetch customers from database
            String query = "SELECT phone_number, firstname, lastname FROM customers";
            PreparedStatement stmt = con.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                String customerInfo = String.format("%s - %s %s", 
                    rs.getString("phone_number"),
                    rs.getString("firstname"),
                    rs.getString("lastname"));
                customers.add(customerInfo);
            }
            
            customerComboBox.setItems(customers);
            
            // Create the dialog layout
            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(20, 150, 10, 10));
            grid.add(new Label("Select Customer:"), 0, 0);
            grid.add(customerComboBox, 1, 0);
            
            dialog.getDialogPane().setContent(grid);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            
            // Show customer selection dialog
            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == ButtonType.OK) {
                    return customerComboBox.getValue();
                }
                return null;
            });
            
            Optional<String> customerResult = dialog.showAndWait();
            
            customerResult.ifPresent(customer -> {
                String phoneNumber = customer.split("-")[0].trim();
                
                // Create a dialog for field selection
                List<String> updateFields = List.of(
                    "Email",
                    "First Name",
                    "Last Name",
                    "Phone Number"
                );
                
                ChoiceDialog<String> fieldChoice = new ChoiceDialog<>(updateFields.get(0), updateFields);
                fieldChoice.setTitle("Select Field to Update");
                fieldChoice.setHeaderText("Which field would you like to update?");
                fieldChoice.setContentText("Choose field:");
                
                Optional<String> fieldResult = fieldChoice.showAndWait();
                
                fieldResult.ifPresent(field -> {
                    boolean validInput = false;
                    while (!validInput) {
                        TextInputDialog valueDialog = new TextInputDialog();
                        valueDialog.setTitle("Enter New Value");
                        valueDialog.setHeaderText("Enter new " + field);
                        valueDialog.setContentText("New value:");
                        
                        Optional<String> valueResult = valueDialog.showAndWait();
                        
                        if (!valueResult.isPresent()) {
                            // User clicked cancel
                            break;
                        }
                        
                        String newValue = valueResult.get();
                        
                        // Validate input based on field type
                        if (field.equals("Email")) {
                            if (!isValidEmail(newValue)) {
                                showAlert("Error", "Invalid email format. Please try again.");
                                continue;
                            }
                        } else if (field.equals("Phone Number")) {
                            if (!isValidPhoneNumber(newValue)) {
                                showAlert("Error", "Phone number must be 8 digits. Please try again.");
                                continue;
                            }
                        }
                        
                        // If we reach here, input is valid
                        validInput = true;
                        
                        try {
                            String updateQuery = "UPDATE customers SET " + 
                                field.toLowerCase().replace(" ", "") + 
                                " = ? WHERE phone_number = ?";
                            PreparedStatement updateStmt = con.prepareStatement(updateQuery);
                            updateStmt.setString(1, newValue);
                            updateStmt.setString(2, phoneNumber);
                            
                            int rowsAffected = updateStmt.executeUpdate();
                            if (rowsAffected > 0) {
                                showAlert("Success", "Customer details updated successfully");
                                logAction("Customer details updated");
                            } else {
                                showAlert("Error", "Failed to update customer details");
                                logAction("Customer details update failed");
                            }
                        } catch (SQLException e) {
                            showAlert("Error", "Database error: " + e.getMessage());
                        }
                    }
                });
            });
            
        } catch (SQLException e) {
            showAlert("Error", "Failed to fetch customers: " + e.getMessage());
        }
    }
    
    private boolean isValidEmail(String email) {
        return email.matches("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
    }

    private boolean isValidPhoneNumber(String phoneNumber) {
        return phoneNumber.matches("^\\d{8}$");
    }
    
    private void logAction(String action) throws SQLException {
        String logQuery = "INSERT INTO log (mac_address, log_time, action) VALUES (?, ?, ?)";
        try (PreparedStatement logStatement = con.prepareStatement(logQuery)) {
            logStatement.setString(1, GetMacAddress.getMacAddress());
            logStatement.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            logStatement.setString(3, action);
            logStatement.executeUpdate();
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

    private static class ExpiredCustomer {
        private final String phoneNumber;
        private final String name;
        private final String endDate;
        private final long daysExpired;

        public ExpiredCustomer(String phoneNumber, String name, String endDate, long daysExpired) {
            this.phoneNumber = phoneNumber;
            this.name = name;
            this.endDate = endDate;
            this.daysExpired = daysExpired;
        }

        // Getters
        public String getPhoneNumber() { return phoneNumber; }
        public String getName() { return name; }
        public String getEndDate() { return endDate; }
        public long getDaysExpired() { return daysExpired; }
    }
}
