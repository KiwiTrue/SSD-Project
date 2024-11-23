// Purpose: This file contains the code for the subscription renewal page for the clerk.
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.sql.Timestamp;
import java.time.LocalDate;

public final class CustomerRenew extends Application {

    // Instance variables
    private Stage stage;
    private Connection con;
    private TextField phoneNumberField; 
    private ToggleGroup durationGroup;
    private CheckBox trainerNeededCheckbox;
    private CheckBox nutritionistNeededCheckbox;

    // Main method to launch the application
    public static void main(String[] args) {
        launch(args);
    }

    // Method to start the subscription renewal page
    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        con = DBUtils.establishConnection();
        initializeComponents();
    }


    // Method to initialize the components of the subscription renewal page
    void initializeComponents() {
        VBox renewalLayout = new VBox(10);
        renewalLayout.setPadding(new Insets(10));

        phoneNumberField = new TextField();

        RadioButton oneMonthRadioButton = new RadioButton("1 Month");
        RadioButton threeMonthsRadioButton = new RadioButton("3 Months");
        RadioButton sixMonthsRadioButton = new RadioButton("6 Months");
        RadioButton twelveMonthsRadioButton = new RadioButton("12 Months");

        durationGroup = new ToggleGroup();
        oneMonthRadioButton.setToggleGroup(durationGroup);
        threeMonthsRadioButton.setToggleGroup(durationGroup);
        sixMonthsRadioButton.setToggleGroup(durationGroup);
        twelveMonthsRadioButton.setToggleGroup(durationGroup);

        trainerNeededCheckbox = new CheckBox("Trainer Needed");
        nutritionistNeededCheckbox = new CheckBox("Nutritionist Needed");

        Button renewButton = new Button("Renew Subscription");
        renewButton.setOnAction(arg0 -> {
            try {
                renewSubscription(arg0);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });

        Button backButton = new Button("Back");
        backButton.setOnAction(this::goBackToClerkLogin);

        renewalLayout.getChildren().addAll(
                new Label("Phone number:"), phoneNumberField,
                new Label("Subscription Duration:"),
                oneMonthRadioButton, threeMonthsRadioButton, sixMonthsRadioButton, twelveMonthsRadioButton,
                trainerNeededCheckbox, nutritionistNeededCheckbox,
                renewButton, backButton
        );

        Scene renewalScene = new Scene(renewalLayout, 400, 400);
        stage.setTitle("Subscription Renewal");
        stage.setScene(renewalScene);
        stage.show();
    }

    // Method to renew the subscription of a customer
    private void renewSubscription(ActionEvent event) throws SQLException {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd");
        LocalDateTime now = LocalDateTime.now();

        String selectedDuration = "";
        RadioButton selectedRadioButton = (RadioButton) durationGroup.getSelectedToggle();
        if (selectedRadioButton != null) {
            selectedDuration = selectedRadioButton.getText();
        } else {
            showAlert("Error", "Please select a subscription duration.");
            return;
        }

        String phoneNumber = phoneNumberField.getText();

        String phoneNumberString = retrievePhoneNumber(phoneNumber);
        if (phoneNumberString == null) {
            showAlert("Error", "Customer not found.");
            return;
        }

        try {
            // First, get the current subscription end date
            String checkQuery = "SELECT subscription_end FROM customers WHERE phone_number = ?";
            PreparedStatement checkStatement = con.prepareStatement(checkQuery);
            checkStatement.setString(1, phoneNumber);
            ResultSet rs = checkStatement.executeQuery();

            if (!rs.next()) {
                showAlert("Error", "Customer not found.");
                return;
            }

            String currentEndDateStr = rs.getString("subscription_end");
            // Fix: Parse as LocalDate instead of LocalDateTime
            LocalDateTime currentEndDate = LocalDate.parse(currentEndDateStr.replace("/", "-"), 
                DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                .atStartOfDay(); // Convert LocalDate to LocalDateTime

            LocalDateTime newEndDate;

            // If current subscription is expired, start from today
            // Otherwise, add the new duration to the current end date
            if (currentEndDate.isBefore(now)) {
                newEndDate = now;
            } else {
                newEndDate = currentEndDate;
            }

            // Add the selected duration
            if (selectedDuration.equals("1 Month")) {
                newEndDate = newEndDate.plusMonths(1);
            } else if (selectedDuration.equals("3 Months")) {
                newEndDate = newEndDate.plusMonths(3);
            } else if (selectedDuration.equals("6 Months")) {
                newEndDate = newEndDate.plusMonths(6);
            } else if (selectedDuration.equals("12 Months")) {
                newEndDate = newEndDate.plusMonths(12);
            }

            String subscriptionEndDateStr = dtf.format(newEndDate);

            boolean trainerNeeded = trainerNeededCheckbox.isSelected();
            boolean nutritionistNeeded = nutritionistNeededCheckbox.isSelected();
            String trainerAssigned = trainerNeeded ? "needed" : "not needed";
            String nutritionistAssigned = nutritionistNeeded ? "needed" : "not needed";

            String updateQuery = "UPDATE customers SET subscription_end = ?, trainer_assigned = ?, nutritionist_assigned = ? WHERE phone_number = ?";
            PreparedStatement updateStatement = con.prepareStatement(updateQuery);
            updateStatement.setString(1, subscriptionEndDateStr);
            updateStatement.setString(2, trainerAssigned);
            updateStatement.setString(3, nutritionistAssigned);
            updateStatement.setString(4, phoneNumber);

            int rowsUpdated = updateStatement.executeUpdate();

            if (rowsUpdated > 0) {
                showAlert("Success", "Subscription renewed successfully.");

                String logQuery = "INSERT INTO log (mac_address, log_time, action) VALUES (?, ?, ?)";
                Timestamp timeStamp = new Timestamp(System.currentTimeMillis());
                String action = "subscription renewed";
                PreparedStatement logStatement = con.prepareStatement(logQuery);
                logStatement.setString(1, GetMacAddress.getMacAddress());
                logStatement.setTimestamp(2, timeStamp);
                logStatement.setString(3, action);
                logStatement.executeUpdate();
            } else {
                showAlert("Error", "Failed to renew subscription.");
                
                String logQuery = "INSERT INTO log (mac_address, log_time, action) VALUES (?, ?, ?)";
                Timestamp timeStamp = new Timestamp(System.currentTimeMillis());
                String action = "subscription renewal failed";
                PreparedStatement logStatement = con.prepareStatement(logQuery);
                logStatement.setString(1, GetMacAddress.getMacAddress());
                logStatement.setTimestamp(2, timeStamp);
                logStatement.setString(3, action);
                logStatement.executeUpdate();
            }

            updateStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to renew subscription: " + e.getMessage());
        }
    }

    // Method to retrieve the phone number from the database
    private String retrievePhoneNumber(String phoneNumber) {
        String phoneNumberString = null;
        try {
            String query = "SELECT * FROM customers WHERE phone_number = ?";
            PreparedStatement statement = con.prepareStatement(query);
            statement.setString(1, phoneNumber);

            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                phoneNumberString = resultSet.getString("phone_number");
            }

            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to retrieve phone number from the database.");
        }
        return phoneNumberString;
    }


    private void goBackToClerkLogin(ActionEvent event) {
        ClerkLogin clerkLogin = new ClerkLogin();
        clerkLogin.start(stage); 
    }

    private void showAlert(String title, String content) {
        if (title.equals("Success")){
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        }else{
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        }
    }

    @Override
    public void stop() {
        try {
            if (con != null) {
                con.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
