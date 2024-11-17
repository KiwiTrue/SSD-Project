import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.DatePicker;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.sql.Connection;
import java.util.Optional;
import java.sql.Timestamp;

public final class ManagerLogin extends Application {

    // Declare the stage and connection variables
    private Stage stage;
    private Connection con;

    // Main method
    public static void main(String[] args) {
        launch(args);
    }

    // Start method
    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        initializeComponents();
    }

    // This method is used to initialize the components of the Manager Page
    void initializeComponents() {
        this.con = DBUtils.establishConnection();
        VBox loginLayout = new VBox(10);
        loginLayout.setPadding(new Insets(10));
        
        Button createClassButton = new Button("Create Class");
        createClassButton.setOnAction(event -> createSchedule());

        Button assignTrainerButton = new Button("Assign Trainer");
        assignTrainerButton.setOnAction(event -> assignTrainer());

        Button scheduleMaintenanceButton = new Button("Schedule Maintenance");
        scheduleMaintenanceButton.setOnAction(event -> scheduleMaintenance());

        Button financialReportSubmissionButton = new Button("Submit Financial Report");
        financialReportSubmissionButton.setOnAction(event -> submitFinancialReport());

        Button financialReportsButton = new Button("View Financial Reports");
        financialReportsButton.setOnAction(event -> viewFinancialReports());

        Button backButton = new Button("Log Out");
        backButton.setOnAction(this::goBack);

        loginLayout.getChildren().addAll(createClassButton, assignTrainerButton, scheduleMaintenanceButton, financialReportSubmissionButton, financialReportsButton, backButton);

        Scene loginScene = new Scene(loginLayout, 300, 250);
        stage.setTitle("Manager Login");
        stage.setScene(loginScene);
        stage.show();
    }

    // This method is used to create a schedule for a customer
    private void createSchedule() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Create Schedule");
        dialog.setHeaderText("Enter the Email of the customer");
        dialog.setContentText("Email:");
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            String email = result.get();
            dialog.setHeaderText("Enter the schedule for Sunday");
            dialog.setContentText("Schedule:");
            Optional<String> sundayResult = dialog.showAndWait();

            dialog.setHeaderText("Enter the schedule for Monday");
            dialog.setContentText("Schedule:");
            Optional<String> mondayResult = dialog.showAndWait();

            dialog.setHeaderText("Enter the schedule for Tuesday");
            dialog.setContentText("Schedule:");
            Optional<String> tuesdayResult = dialog.showAndWait();

            dialog.setHeaderText("Enter the schedule for Wednesday");
            dialog.setContentText("Schedule:");
            Optional<String> wednesdayResult = dialog.showAndWait();

            dialog.setHeaderText("Enter the schedule for Thursday");
            dialog.setContentText("Schedule:");
            Optional<String> thursdayResult = dialog.showAndWait();

            dialog.setHeaderText("Enter the schedule for Friday");
            dialog.setContentText("Schedule:");
            Optional<String> fridayResult = dialog.showAndWait();

            dialog.setHeaderText("Enter the schedule for Saturday");
            dialog.setContentText("Schedule:");
            Optional<String> saturdayResult = dialog.showAndWait();
            if (sundayResult.isPresent() && mondayResult.isPresent() && tuesdayResult.isPresent() && wednesdayResult.isPresent() && thursdayResult.isPresent() && fridayResult.isPresent() && saturdayResult.isPresent()) {
                String weeklySchedule = "Sunday: " + sundayResult.get() + "\nMonday: " + mondayResult.get() + "\nTuesday: " + tuesdayResult.get() + "\nWednesday: " + wednesdayResult.get() + "\nThursday: " + thursdayResult.get() + "\nFriday: " + fridayResult.get() + "\nSaturday: " + saturdayResult.get();
                String query = "UPDATE customers SET schedule = ? WHERE email = ?";
                try (PreparedStatement ps = con.prepareStatement(query)) {
                    ps.setString(1, weeklySchedule);
                    ps.setString(2, email);
                    ps.executeUpdate();
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Success");
                    alert.setHeaderText(null);
                    alert.setContentText("Schedule created successfully.");
                    alert.showAndWait();
                } catch (SQLException e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText(null);
                    alert.setContentText("Failed to create schedule: " + e.getMessage());
                    alert.showAndWait();
                }
            }
        }
    }

    // This method is used to assign a trainer to a customer
    private void assignTrainer() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Assign Trainer");
        dialog.setHeaderText("Enter the Email of the customer");
        dialog.setContentText("Email:");
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            String email = result.get();
            dialog.setHeaderText("Enter the trainer to be assigned");
            dialog.setContentText("Trainer:");
            Optional<String> trainerResult = dialog.showAndWait();
            if (trainerResult.isPresent()) {
                String trainer = trainerResult.get();
                String query = "UPDATE customers SET trainer_assigned = ? WHERE email = ?";
                try (PreparedStatement ps = con.prepareStatement(query)) {
                    ps.setString(1, trainer);
                    ps.setString(2, email);
                    ps.executeUpdate();
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Success");
                    alert.setHeaderText(null);
                    alert.setContentText("Trainer assigned successfully.");
                    alert.showAndWait();

                    String logQuery = "INSERT INTO log (mac_address, log_time, action) VALUES (?, ?, ?)";
                    Timestamp timeStamp = new Timestamp(System.currentTimeMillis());
                    String action = "Trainer assigned";
                    PreparedStatement logStatement = con.prepareStatement(logQuery);
                    logStatement.setString(1, GetMacAddress.getMacAddress());
                    logStatement.setTimestamp(2, timeStamp);
                    logStatement.setString(3, action);
                    logStatement.executeUpdate();

                } catch (SQLException e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText(null);
                    alert.setContentText("Failed to assign trainer: " + e.getMessage());
                    alert.showAndWait();
                }
            }
        }
    }

    // This method is used to schedule maintenance for equipment
    private void scheduleMaintenance() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Schedule Maintenance");
        dialog.setHeaderText("Enter the Equipment name");
        dialog.setContentText("Equipment:");
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            String equipmentName = result.get();
            dialog.setHeaderText("Enter the date of maintenance");
            dialog.setContentText("Date:");
            DatePicker datePicker = new DatePicker();
            datePicker.setPromptText("Select Date");
            dialog.getDialogPane().setContent(datePicker);
            dialog.showAndWait();
            LocalDate maintenanceDate = datePicker.getValue();
            if (maintenanceDate != null) {
                String query = "UPDATE equipment_reports SET maintenance_date = ? WHERE name = ?";
                try (PreparedStatement ps = con.prepareStatement(query)) {
                    ps.setString(1, maintenanceDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
                    ps.setString(2, equipmentName);
                    ps.executeUpdate();
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Success");
                    alert.setHeaderText(null);
                    alert.setContentText("Maintenance scheduled successfully.");
                    alert.showAndWait();

                    String logQuery = "INSERT INTO log (mac_address, log_time, action) VALUES (?, ?, ?)";
                    Timestamp timeStamp = new Timestamp(System.currentTimeMillis());
                    String action = "Maintenance scheduled";
                    PreparedStatement logStatement = con.prepareStatement(logQuery);
                    logStatement.setString(1, GetMacAddress.getMacAddress());
                    logStatement.setTimestamp(2, timeStamp);
                    logStatement.setString(3, action);
                    logStatement.executeUpdate();

                } catch (SQLException e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText(null);
                    alert.setContentText("Failed to schedule maintenance: " + e.getMessage());
                    alert.showAndWait();
                }
            }
        }
    }

    // This method is used to submit financial reports
    private void submitFinancialReport() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Submit Financial Report");
        dialog.setHeaderText("Enter the Title of the report");
        dialog.setContentText("Title:");
        Optional<String> titleResult = dialog.showAndWait();
        if (titleResult.isPresent()) {
            String title = titleResult.get();
            dialog.setHeaderText("Enter the Date of the report");
            dialog.setContentText("Date:");
            DatePicker datePicker = new DatePicker();
            datePicker.setPromptText("Select Date");
            dialog.getDialogPane().setContent(datePicker);
            dialog.showAndWait();
            LocalDate reportDate = datePicker.getValue();
            if (reportDate != null) {
                dialog.setHeaderText("Enter the Revenue");
                dialog.setContentText("Revenue:");
                Optional<String> revenueResult = dialog.showAndWait();
                if (revenueResult.isPresent()) {
                    double revenueDouble;
                    try {
                        revenueDouble = Double.parseDouble(revenueResult.get());
                    } catch (NumberFormatException e) {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Error");
                        alert.setHeaderText(null);
                        alert.setContentText("Invalid revenue format.");
                        alert.showAndWait();
                        return;
                    }

                    dialog.setHeaderText("Enter the Expenses");
                    dialog.setContentText("Expenses:");
                    Optional<String> expensesResult = dialog.showAndWait();
                    if (expensesResult.isPresent()) {
                        double expensesDouble;
                        try {
                            expensesDouble = Double.parseDouble(expensesResult.get());
                        } catch (NumberFormatException e) {
                            Alert alert = new Alert(Alert.AlertType.ERROR);
                            alert.setTitle("Error");
                            alert.setHeaderText(null);
                            alert.setContentText("Invalid expenses format.");
                            alert.showAndWait();
                            return;
                        }

                        double profitDouble = revenueDouble - expensesDouble;
                        String profit = String.format("%.2f", profitDouble);
                        String query = "INSERT INTO financial_reports (title, date_written, revenue, expenses, total) VALUES (?, ?, ?, ?, ?)";

                        try (PreparedStatement ps = con.prepareStatement(query)) {
                            ps.setString(1, title);
                            ps.setString(2, reportDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
                            ps.setDouble(3, revenueDouble);
                            ps.setDouble(4, expensesDouble);
                            ps.setString(5, profit);
                            ps.executeUpdate();

                            Alert alert = new Alert(Alert.AlertType.INFORMATION);
                            alert.setTitle("Success");
                            alert.setHeaderText(null);
                            alert.setContentText("Financial report submitted successfully.");
                            alert.showAndWait();
                        } catch (SQLException e) {
                            Alert alert = new Alert(Alert.AlertType.ERROR);
                            alert.setTitle("Error");
                            alert.setHeaderText(null);
                            alert.setContentText("Failed to submit financial report: " + e.getMessage());
                            alert.showAndWait();
                        }
                    }
                }
            }
        }
    }

    // This method is used to view financial reports
    private void viewFinancialReports() {
        VBox layout = new VBox(10);
        layout.setPadding(new Insets(10));
        String query = "SELECT title, date_written, revenue, expenses, total FROM financial_reports";
        try (PreparedStatement ps = con.prepareStatement(query)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String title = rs.getString("title");
                String date = rs.getString("date_written");
                String revenue = rs.getString("revenue");
                String expenses = rs.getString("expenses");
                String profit = rs.getString("total");
                Label reportLabel = new Label("Title: " + title + "\nDate: " + date + "\nRevenue: " + revenue + "\nExpenses: " + expenses + "\nProfit: " + profit);
                layout.getChildren().add(reportLabel);
            }
            Scene scene = new Scene(layout, 400, 300);
            Stage stage = new Stage();
            stage.setTitle("Financial Reports");
            stage.setScene(scene);
            stage.show();
        } catch (SQLException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("Failed to load financial reports: " + e.getMessage());
            alert.showAndWait();
        }
    }

    // This method is used to log out
    private void goBack(ActionEvent event) {
        stage.close();
        App userLogin = new App();
        userLogin.start(new Stage());
    }
}