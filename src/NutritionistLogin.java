import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public final class NutritionistLogin extends Application {

    // Instance variables
    private Stage stage;
    private Connection con;

    // Main method to launch the application
    public static void main(String[] args) {
        launch(args);
    }

    // Method to start the nutritionist login page
    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        con = DBUtils.establishConnection(); 
        initializeComponents();
    }

    // Method to initialize the components of the nutritionist login page
    void initializeComponents() {

        VBox loginLayout = new VBox(10);
        loginLayout.setPadding(new Insets(10));

    
        Button viewButton = new Button("View Subscriptions");
        viewButton.setOnAction(this::viewSubscription);
    
        Button backButton = new Button("Log Out");
        backButton.setOnAction(this::goBack);
    
        loginLayout.getChildren().addAll( viewButton, backButton);
    
        Scene loginScene = new Scene(loginLayout, 300, 450);
        stage.setTitle("Nutritionist Login");
        stage.setScene(loginScene);
        stage.show();
    }
    
    // Method to view the subscriptions assigned to the nutritionist
    private void viewSubscription(ActionEvent event) {
        try {
            Connection con = DBUtils.establishConnection();
            
            String query = "SELECT * FROM customers WHERE nutritionist_assigned != ?";
            PreparedStatement statement = con.prepareStatement(query);
            statement.setString(1, "not needed"); 
            
            ResultSet resultSet = statement.executeQuery();
            
            if (!resultSet.isBeforeFirst()) {
                showAlert("No Subscriptions Found", "No subscriptions assigned to the nutritionist.");
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append("Subscriptions assigned to the nutritionist:\n\n");
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

    // Method to log out of the nutritionist account
    private void goBack(ActionEvent event) {
        stage.close();
        App userLogin = new App();
        userLogin.start(new Stage());
    }

    // Method to close the connection to the database
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

    // Method to show an alert dialog
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
}