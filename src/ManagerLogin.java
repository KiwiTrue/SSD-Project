import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.GridPane;
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
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import javafx.scene.control.TextField;

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
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Create Class Schedule");

        // Create form elements
        ComboBox<String> trainerCombo = new ComboBox<>();
        DatePicker datePicker = new DatePicker();
        
        // Create time picker combo boxes
        ComboBox<String> startTimeCombo = new ComboBox<>();
        ComboBox<String> endTimeCombo = new ComboBox<>();
        
        // Populate time options (24-hour format)
        for (int hour = 6; hour <= 22; hour++) { // Assuming gym hours 6:00 - 22:00
            String hourStr = String.format("%02d:00", hour);
            String halfHourStr = String.format("%02d:30", hour);
            startTimeCombo.getItems().add(hourStr);
            startTimeCombo.getItems().add(halfHourStr);
            endTimeCombo.getItems().add(hourStr);
            endTimeCombo.getItems().add(halfHourStr);
        }
        
        startTimeCombo.setPromptText("Start Time");
        endTimeCombo.setPromptText("End Time");

        // Load available trainers
        try {
            ResultSet rs = con.createStatement().executeQuery(
                "SELECT name FROM trainers WHERE available = true"
            );
            while (rs.next()) {
                trainerCombo.getItems().add(rs.getString("name"));
            }
        } catch (SQLException e) {
            showError("Database Error", "Could not fetch trainers: " + e.getMessage());
            return;
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.addRow(0, new Label("Select Trainer:"), trainerCombo);
        grid.addRow(1, new Label("Select Date:"), datePicker);
        grid.addRow(2, new Label("Start Time:"), startTimeCombo);
        grid.addRow(3, new Label("End Time:"), endTimeCombo);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (trainerCombo.getValue() == null || datePicker.getValue() == null || 
                startTimeCombo.getValue() == null || endTimeCombo.getValue() == null) {
                showError("Invalid Input", "Please fill all fields");
                return;
            }

            String timeRange = startTimeCombo.getValue() + "-" + endTimeCombo.getValue();
            
            // Check if end time is after start time
            if (!isValidTimeRange(startTimeCombo.getValue(), endTimeCombo.getValue())) {
                showError("Invalid Time Range", "End time must be after start time");
                return;
            }

            // Check trainer availability
            if (!isTrainerAvailable(trainerCombo.getValue(), datePicker.getValue(), timeRange)) {
                showError("Trainer Unavailable", "Selected trainer is not available at this time");
                return;
            }

            createClassSchedule(trainerCombo.getValue(), datePicker.getValue(), timeRange);
        }
    }

    // This method is used to assign a trainer to a customer
    private void assignTrainer() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Assign Trainer");
        
        // Get available trainers
        List<String> trainers = new ArrayList<>();
        try {
            ResultSet rs = con.createStatement().executeQuery("SELECT name FROM trainers WHERE available = true");
            while (rs.next()) {
                trainers.add(rs.getString("name"));
            }
        } catch (SQLException e) {
            showError("Database Error", "Could not fetch trainers: " + e.getMessage());
            return;
        }

        if (trainers.isEmpty()) {
            showError("No Trainers", "No available trainers found");
            return;
        }

        // Create a combo box for trainer selection
        ComboBox<String> trainerCombo = new ComboBox<>();
        trainerCombo.getItems().addAll(trainers);
        
        dialog.getDialogPane().setContent(new VBox(10, 
            new Label("Select Trainer:"), 
            trainerCombo,
            new Label("Enter Customer Email:"),
            new TextField()));

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && trainerCombo.getValue() != null) {
            String trainer = trainerCombo.getValue();
            String customerEmail = ((TextField)dialog.getDialogPane().lookup("TextField")).getText();
            
            // Check trainer capacity
            if (!checkTrainerCapacity(trainer)) {
                showError("Trainer Unavailable", "Selected trainer has reached maximum customer capacity");
                return;
            }
            
            updateTrainerAssignment(customerEmail, trainer);
        }
    }

    // This method is used to schedule maintenance for equipment
    private void scheduleMaintenance() {
        VBox content = new VBox(10);
        ComboBox<String> equipmentCombo = new ComboBox<>();
        DatePicker datePicker = new DatePicker();
        
        // Load equipment list
        try {
            ResultSet rs = con.createStatement().executeQuery("SELECT name FROM equipment_reports");
            while (rs.next()) {
                equipmentCombo.getItems().add(rs.getString("name"));
            }
        } catch (SQLException e) {
            showError("Database Error", "Could not fetch equipment list: " + e.getMessage());
            return;
        }

        content.getChildren().addAll(
            new Label("Select Equipment:"),
            equipmentCombo,
            new Label("Select Maintenance Date:"),
            datePicker
        );

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Check if equipment needs maintenance
            if (!checkEquipmentStatus(equipmentCombo.getValue())) {
                showError("Maintenance Not Required", "Selected equipment does not require maintenance at this time");
                return;
            }
            updateMaintenanceSchedule(equipmentCombo.getValue(), datePicker.getValue());
        }
    }

    // This method is used to submit financial reports
    private void submitFinancialReport() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Submit Financial Report");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField titleField = new TextField();
        DatePicker datePicker = new DatePicker();
        TextField revenueField = new TextField();
        TextField expensesField = new TextField();

        grid.addRow(0, new Label("Title:"), titleField);
        grid.addRow(1, new Label("Date:"), datePicker);
        grid.addRow(2, new Label("Revenue:"), revenueField);
        grid.addRow(3, new Label("Expenses:"), expensesField);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                double revenue = Double.parseDouble(revenueField.getText());
                double expenses = Double.parseDouble(expensesField.getText());
                if (datePicker.getValue() == null || titleField.getText().isEmpty()) {
                    throw new IllegalArgumentException("All fields are required");
                }
                submitFinancialReportToDb(
                    titleField.getText(),
                    datePicker.getValue(),
                    revenue,
                    expenses
                );
            } catch (NumberFormatException e) {
                showError("Invalid Input", "Revenue and expenses must be valid numbers");
            } catch (IllegalArgumentException e) {
                showError("Invalid Input", e.getMessage());
            }
        }
    }

    // This method is used to view financial reports
    private void viewFinancialReports() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("View Financial Reports");

        // Add month/year picker
        ComboBox<String> monthPicker = new ComboBox<>();
        monthPicker.getItems().addAll("January", "February", "March", "April", "May", "June", 
                                    "July", "August", "September", "October", "November", "December");
        TextField yearField = new TextField();
        yearField.setPromptText("YYYY");

        GridPane grid = new GridPane();
        grid.addRow(0, new Label("Month:"), monthPicker);
        grid.addRow(1, new Label("Year:"), yearField);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            displayMonthlyReport(monthPicker.getValue(), yearField.getText());
        }
    }

    // This method is used to log out
    private void goBack(ActionEvent event) {
        stage.close();
        App userLogin = new App();
        userLogin.start(new Stage());
    }

    // Helper methods
    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private boolean isValidTimeFormat(String time) {
        return time.matches("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]-([0-1]?[0-9]|2[0-3]):[0-5][0-9]$");
    }

    private void logAction(String action) {
        try {
            PreparedStatement ps = con.prepareStatement(
                "INSERT INTO log (mac_address, log_time, action) VALUES (?, ?, ?)"
            );
            ps.setString(1, GetMacAddress.getMacAddress());
            ps.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            ps.setString(3, action);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Failed to log action: " + e.getMessage());
        }
    }

    private void updateScheduleAndLog(String phone, String weeklySchedule) {
        String query = "UPDATE customers SET schedule = ? WHERE phone = ?";
        try (PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, weeklySchedule);
            ps.setString(2, phone);
            ps.executeUpdate();
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText(null);
            alert.setContentText("Schedule created successfully.");
            alert.showAndWait();
            logAction("Schedule created for customer with phone: " + phone);
        } catch (SQLException e) {
            showError("Failed to create schedule", e.getMessage());
        }
    }

    private void updateTrainerAssignment(String email, String trainer) {
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
            logAction("Trainer " + trainer + " assigned to " + email);
        } catch (SQLException e) {
            showError("Failed to assign trainer", e.getMessage());
        }
    }

    private void updateMaintenanceSchedule(String equipment, LocalDate date) {
        String query = "UPDATE equipment_reports SET maintenance_date = ? WHERE name = ?";
        try (PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, date.format(DateTimeFormatter.ISO_LOCAL_DATE));
            ps.setString(2, equipment);
            ps.executeUpdate();
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText(null);
            alert.setContentText("Maintenance scheduled successfully.");
            alert.showAndWait();
            logAction("Maintenance scheduled for " + equipment + " on " + date);
        } catch (SQLException e) {
            showError("Failed to schedule maintenance", e.getMessage());
        }
    }

    private void submitFinancialReportToDb(String title, LocalDate date, double revenue, double expenses) {
        double profit = revenue - expenses;
        String query = "INSERT INTO financial_reports (title, date_written, revenue, expenses, total) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, title);
            ps.setString(2, date.format(DateTimeFormatter.ISO_LOCAL_DATE));
            ps.setDouble(3, revenue);
            ps.setDouble(4, expenses);
            ps.setDouble(5, profit);
            ps.executeUpdate();
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText(null);
            alert.setContentText("Financial report submitted successfully.");
            alert.showAndWait();
            logAction("Financial report submitted: " + title);
        } catch (SQLException e) {
            showError("Failed to submit financial report", e.getMessage());
        }
    }

    private static class CustomerInfo {
        private final String name;
        private final String phone;

        public CustomerInfo(String name, String phone) {
            this.name = name;
            this.phone = phone;
        }

        @Override
        public String toString() {
            return name + " (" + phone + ")";
        }
    }

    // New helper methods
    private boolean isTrainerAvailable(String trainer, LocalDate date, String time) {
        try {
            PreparedStatement ps = con.prepareStatement(
                "SELECT COUNT(*) FROM class_schedules WHERE trainer = ? AND date = ? AND time_range = ?"
            );
            ps.setString(1, trainer);
            ps.setString(2, date.toString());
            ps.setString(3, time);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) == 0;
        } catch (SQLException e) {
            return false;
        }
    }

    private boolean checkTrainerCapacity(String trainer) {
        try {
            PreparedStatement ps = con.prepareStatement(
                "SELECT COUNT(*) FROM customers WHERE trainer_assigned = ?"
            );
            ps.setString(1, trainer);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) < 5; // Assuming max 5 customers per trainer
        } catch (SQLException e) {
            return false;
        }
    }

    private boolean checkEquipmentStatus(String equipment) {
        try {
            PreparedStatement ps = con.prepareStatement(
                "SELECT needs_maintenance FROM equipment_reports WHERE name = ?"
            );
            ps.setString(1, equipment);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getBoolean("needs_maintenance");
        } catch (SQLException e) {
            return false;
        }
    }

    private void createClassSchedule(String trainer, LocalDate date, String timeRange) {
        try {
            PreparedStatement ps = con.prepareStatement(
                "INSERT INTO class_schedules (trainer, date, time_range) VALUES (?, ?, ?)"
            );
            ps.setString(1, trainer);
            ps.setString(2, date.toString());
            ps.setString(3, timeRange);
            ps.executeUpdate();
            
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setContentText("Class schedule created successfully");
            alert.showAndWait();
            
            logAction("Created class schedule for " + trainer + " on " + date);
        } catch (SQLException e) {
            showError("Database Error", "Could not create class schedule: " + e.getMessage());
        }
    }

    private void displayMonthlyReport(String month, String year) {
        // Implementation for displaying filtered monthly reports
        // ...existing code...
    }

    private boolean isValidTimeRange(String startTime, String endTime) {
        try {
            String[] startParts = startTime.split(":");
            String[] endParts = endTime.split(":");
            int startHour = Integer.parseInt(startParts[0]);
            int startMinute = Integer.parseInt(startParts[1]);
            int endHour = Integer.parseInt(endParts[0]);
            int endMinute = Integer.parseInt(endParts[1]);
            
            return (endHour > startHour) || (endHour == startHour && endMinute > startMinute);
        } catch (Exception e) {
            return false;
        }
    }
}