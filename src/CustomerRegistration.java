// This class is responsible for registering a new customer to the gym. It takes the customer's 
// first name, last name, email, phone number, subscription duration, and whether they need a 
// trainer or nutritionist. It then inserts this information into the database. It also has 
// methods to check if the name, email, and phone number are valid. If the registration is 
// successful, it shows a success message. If the registration fails, it shows an error message. 
// It also has a method to go back to the clerk login screen.
import javafx.application.Application;
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
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

public final class CustomerRegistration extends Application {

    // Instance variables
    private Stage stage;
    private Connection con;
    private TextField firstnameField;
    private TextField lastnameField;
    private TextField emailField;
    private TextField phoneNumberField;
    private RadioButton oneMonthRadioButton;
    private RadioButton threeMonthsRadioButton;
    private RadioButton sixMonthsRadioButton;
    private RadioButton twelveMonthsRadioButton;
    private CheckBox trainerNeededCheckbox;
    private CheckBox nutritionistNeededCheckbox;

    // Main method to launch the application
    public static void main(String[] args) {
        launch(args);
    }

    // Method to start the customer registration page
    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        con = DBUtils.establishConnection();
        initializeComponents();
    }

    // Method to initialize the components of the customer registration page
    void initializeComponents() {
        VBox registrationLayout = new VBox(10);
        registrationLayout.setPadding(new Insets(10));

        firstnameField = new TextField();
        lastnameField = new TextField();
        emailField = new TextField(); 
        phoneNumberField = new TextField();

        oneMonthRadioButton = new RadioButton("1 Month");
        threeMonthsRadioButton = new RadioButton("3 Months");
        sixMonthsRadioButton = new RadioButton("6 Months");
        twelveMonthsRadioButton = new RadioButton("12 Months");

        ToggleGroup durationGroup = new ToggleGroup();
        oneMonthRadioButton.setToggleGroup(durationGroup);
        threeMonthsRadioButton.setToggleGroup(durationGroup);
        sixMonthsRadioButton.setToggleGroup(durationGroup);
        twelveMonthsRadioButton.setToggleGroup(durationGroup);

        trainerNeededCheckbox = new CheckBox("Trainer Needed");
        nutritionistNeededCheckbox = new CheckBox("Nutritionist Needed");

        Button registerButton = new Button("Register Customer");
        registerButton.setOnAction(event -> registerCustomer());
        Button backButton = new Button("Back");
        backButton.setOnAction(event -> goBack());

        registrationLayout.getChildren().addAll(
                new Label("First Name:"), firstnameField,
                new Label("Last Name:"), lastnameField,
                new Label("Email:"), emailField, 
                new Label("Phone Number:"), phoneNumberField,
                new Label("Subscription Duration:"),
                oneMonthRadioButton, threeMonthsRadioButton, sixMonthsRadioButton, twelveMonthsRadioButton,
                trainerNeededCheckbox,
                nutritionistNeededCheckbox,
                registerButton, backButton
        );

        Scene registrationScene = new Scene(registrationLayout, 550, 550);
        stage.setTitle("Customer Registration");
        stage.setScene(registrationScene);
        stage.show();
    }


    // Method to register a new customer
    private void registerCustomer() {
        String firstname = firstnameField.getText();
        String lastname = lastnameField.getText();
        String email = emailField.getText();
        String phoneNumber = phoneNumberField.getText();
        boolean trainerNeeded = trainerNeededCheckbox.isSelected();
        boolean nutritionistNeeded = nutritionistNeededCheckbox.isSelected();
        String trainerAssigned = trainerNeeded ? "needed" : "not needed";
        String nutritionistAssigned = nutritionistNeeded ? "needed" : "not needed";

        if (!isValidName(firstname) || !isValidName(lastname)) {
            showAlert("Invalid Name", "First name and last name must be at least 2 characters long.");
            return;
        }

        if (!isValidEmail(email)) {
            showAlert("Invalid Email", "Please enter a valid email address.");
            return;
        }

        if (!isValidPhoneNumber(phoneNumber)) {
            showAlert("Invalid Phone Number", "Phone number must be eight '8' digits.");
            return;
        }

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd");
        LocalDateTime now = LocalDateTime.now();
        String subscriptionStartDate = dtf.format(now);

        LocalDateTime subscriptionEndDate;
        if (oneMonthRadioButton.isSelected()) {
            subscriptionEndDate = now.plusMonths(1);
        } else if (threeMonthsRadioButton.isSelected()) {
            subscriptionEndDate = now.plusMonths(3);
        } else if (sixMonthsRadioButton.isSelected()) {
            subscriptionEndDate = now.plusMonths(6);
        } else if (twelveMonthsRadioButton.isSelected()) {
            subscriptionEndDate = now.plusMonths(12);
        } else {
            showAlert("Error", "Please select a subscription duration.");
            return;
        }

        String subscriptionEndDateStr = dtf.format(subscriptionEndDate);

        try {
            String query = "INSERT INTO customers (firstname, lastname, email, subscription_start, subscription_end, phone_number, trainer_assigned, nutritionist_assigned, first_subscription, schedule) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement statement = con.prepareStatement(query);
            statement.setString(1, firstname);
            statement.setString(2, lastname);
            statement.setString(3, email);
            statement.setString(4, subscriptionStartDate);
            statement.setString(5, subscriptionEndDateStr);
            statement.setString(6, phoneNumber);
            statement.setString(7, trainerAssigned);
            statement.setString(8, nutritionistAssigned);
            statement.setString(9, subscriptionStartDate);
            statement.setString(10, "No schedule yet");

            int rowsInserted = statement.executeUpdate();
            if (rowsInserted > 0) {
                showAlert("Success", "Customer registered successfully.");
                
                String logQuery = "INSERT INTO log (mac_address, log_time, action) VALUES (?, ?, ?)";
                Timestamp timeStamp = new Timestamp(System.currentTimeMillis());
                String action = "customer registered";
                PreparedStatement logStatement = con.prepareStatement(logQuery);
                logStatement.setString(1, GetMacAddress.getMacAddress());
                logStatement.setTimestamp(2, timeStamp);
                logStatement.setString(3, action);
                logStatement.executeUpdate();
            } else {
                showAlert("Error", "Failed to register customer.");
                
                String logQuery = "INSERT INTO log (mac_address, log_time, action) VALUES (?, ?, ?)";
                Timestamp timeStamp = new Timestamp(System.currentTimeMillis());
                String action = "customer registration failed";
                PreparedStatement logStatement = con.prepareStatement(logQuery);
                logStatement.setString(1, GetMacAddress.getMacAddress());
                logStatement.setTimestamp(2, timeStamp);
                logStatement.setString(3, action);
                logStatement.executeUpdate();
            }
        } catch (SQLException e) {
            showAlert("Error", "An error occurred while registering customer. Maybe the number is registered already.");
        }
    }

    // Method to check if the name is valid
    private boolean isValidName(String name) {
        return name.length() >= 2 && name.matches("[a-zA-Z]+");
    }

    // Method to check if the email is valid
    private boolean isValidEmail(String email) {
        return email.matches("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
    }

    // Method to check if the phone number is valid
    private boolean isValidPhoneNumber(String phoneNumber) {
        //must start with 00974 and have 8 digits after that
        return phoneNumber.matches("^\\d{8}$");
    }

    // Method to go back to the clerk login screen
    private void goBack() {
        stage.close();
        ClerkLogin clerkLogin = new ClerkLogin();
        clerkLogin.start(new Stage());
    }

    // Method to show an alert
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // Method to close the database connection when the application stops
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
