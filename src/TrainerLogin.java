import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.sql.Timestamp;

public final class TrainerLogin extends Application {

    // Instance variables
    private String username;
    private Stage stage;
    private Connection con;

    // Main method to launch the application
    public static void main(String[] args) {
        launch(args);
    }

    // Constructor
    public TrainerLogin(String username) {
        this.username = username;
    }

    // Start method to initialize the components
    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        con = DBUtils.establishConnection(); 
        initializeComponents();
    }

    // Method to initialize the components
    void initializeComponents() {

        VBox loginLayout = new VBox(10);
        loginLayout.setPadding(new Insets(10));

    
        Button renewButton = new Button("View Subscriptions");
        renewButton.setOnAction(this::viewSubscription);

        Button mySubscriptionsButton = new Button("My Subscriptions");
        mySubscriptionsButton.setOnAction(this::mySubscription);

        Button equipmentReportButton = new Button("Equipment Report");
        equipmentReportButton.setOnAction(this::EquipmentReport);
    
        Button backButton = new Button("Log Out");
        backButton.setOnAction(this::goBack);
    
        loginLayout.getChildren().addAll( renewButton,mySubscriptionsButton,equipmentReportButton, backButton);
    
        Scene loginScene = new Scene(loginLayout, 300, 450);
        stage.setTitle(username+" Login");
        stage.setScene(loginScene);
        stage.show();
    }

    // Method to show subscriptions assigned to the trainer
    private void mySubscription(ActionEvent event){
        try {
            Connection con = DBUtils.establishConnection();
            
            String query = "SELECT * FROM customers WHERE trainer_assigned = ?;";
            PreparedStatement statement = con.prepareStatement(query);
            statement.setString(1, username);
            
            ResultSet resultSet = statement.executeQuery();
            
            if (!resultSet.isBeforeFirst()) {
                showAlert("No Subscriptions Found", "No subscriptions assigned to the trainer.");
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append("Subscriptions assigned to the trainer:\n\n");
                while (resultSet.next()) {
                    String subscriptionInfo = "Name: " + resultSet.getString("firstname") +" "+resultSet.getString("lastname")+
                                              "\nPhone Number: " + resultSet.getString("phone_number") +
                                              "\nStart Date: " + resultSet.getString("subscription_start") +
                                              "\nEnd Date: " + resultSet.getString("subscription_end")+
                                              "\n"+"--------------------";
                    sb.append(subscriptionInfo).append("\n");
                }
                showAlert("Subscriptions Found", sb.toString());
            }
            
            DBUtils.closeConnection(con, statement);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Database Error", "Failed to retrieve subscriptions: " + e.getMessage());
        }
    }
    
    // Method to show All subscriptions regardless of the trainer
    private void viewSubscription(ActionEvent event) {
        try {
            Connection con = DBUtils.establishConnection();
            
            String query = "SELECT * FROM customers WHERE trainer_assigned != ?";
            PreparedStatement statement = con.prepareStatement(query);
            statement.setString(1, "not needed"); 
            
            ResultSet resultSet = statement.executeQuery();
            
            if (!resultSet.isBeforeFirst()) {
                showAlert("No Subscriptions Found", "No subscriptions assigned to the Trainer.");
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append("Subscriptions assigned to the Trainer:\n\n");
                while (resultSet.next()) {
                    String subscriptionInfo = "Name: " + resultSet.getString("firstname") +" "+resultSet.getString("lastname")+
                                              "\nPhone Number: " + resultSet.getString("phone_number") +
                                              "\nStart Date: " + resultSet.getString("subscription_start") +
                                              "\nEnd Date: " + resultSet.getString("subscription_end")+
                                              "\n"+"--------------------";
                    sb.append(subscriptionInfo).append("\n");
                }
                showAlert("Subscriptions Found", sb.toString());
            }
            
            DBUtils.closeConnection(con, statement);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Database Error", "Failed to retrieve subscriptions: " + e.getMessage());
        }
    }

    // Method to submit equipment report
    private void EquipmentReport(ActionEvent event) {
        try{
            Connection con = DBUtils.establishConnection();

            String query = "INSERT INTO Equipments_Reports (equipment_name, report_title, report_description) VALUES (?, ?, ?);";
            PreparedStatement statement = con.prepareStatement(query);
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Equipment Report");
            dialog.setHeaderText("Enter the equipment name");
            dialog.setContentText("Equipment Name:");
            String equipmentName = dialog.showAndWait().get();
            dialog.setHeaderText("Enter the report title");
            dialog.setContentText("Report Title:");
            String reportTitle = dialog.showAndWait().get();
            dialog.setHeaderText("Enter the report description");
            dialog.setContentText("Report Description:");
            String reportDescription = dialog.showAndWait().get();
            statement.setString(1, equipmentName);
            statement.setString(2, reportTitle);
            statement.setString(3, reportDescription);
            int result = statement.executeUpdate();
            if (result == 1) {
                showAlert("Report Submitted", "Equipment report submitted successfully.");
                
                String logQuery = "INSERT INTO log (mac_address, log_time, action) VALUES (?, ?, ?)";
                Timestamp timeStamp = new Timestamp(System.currentTimeMillis());
                String action = "equipment report submitted";
                PreparedStatement logStatement = con.prepareStatement(logQuery);
                logStatement.setString(1, GetMacAddress.getMacAddress());
                logStatement.setTimestamp(2, timeStamp);
                logStatement.setString(3, action);
                logStatement.executeUpdate();
            } else {
                showAlert("Report Submission Failed", "Failed to submit equipment report.");
                
                String logQuery = "INSERT INTO log (mac_address, log_time, action) VALUES (?, ?, ?)";
                Timestamp timeStamp = new Timestamp(System.currentTimeMillis());
                String action = "equipment report failed to submit";
                PreparedStatement logStatement = con.prepareStatement(logQuery);
                logStatement.setString(1, GetMacAddress.getMacAddress());
                logStatement.setTimestamp(2, timeStamp);
                logStatement.setString(3, action);
                logStatement.executeUpdate();

            }
            DBUtils.closeConnection(con, statement);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Database Error", "Failed to submit equipment report: " + e.getMessage());
        }
    }
    
    // Method to go back to the login page
    private void goBack(ActionEvent event) {
        stage.close();
        App userLogin = new App();
        userLogin.start(new Stage());
    }

    // Method to stop the application
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

    // Method to show an alert
    private void showAlert(String title, String content) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}