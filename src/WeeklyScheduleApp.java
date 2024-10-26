import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class WeeklyScheduleApp extends Application {

    // Start method to initialize the components
    @Override
    public void start(Stage primaryStage) {
        TableView<DaySchedule> tableView = new TableView<>();
        ObservableList<DaySchedule> data = FXCollections.observableArrayList();

        TableColumn<DaySchedule, String> dayCol = new TableColumn<>("Day");
        dayCol.setCellValueFactory(cellData -> cellData.getValue().dayProperty());

        for (int i = 1; i <= 8; i++) {
            TableColumn<DaySchedule, String> classCol = new TableColumn<>((i+8)+":00");
            final int classNumber = i;
            classCol.setCellValueFactory(cellData -> cellData.getValue().classProperty(classNumber));
            tableView.getColumns().add(classCol);
        }

        try {
            Connection connection = DBUtils.establishConnection();
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT * FROM weekly_schedule");

            while (resultSet.next()) {
                String day = resultSet.getString("day");
                DaySchedule schedule = new DaySchedule(day);
                for (int i = 1; i <= 8; i++) {
                    String className = resultSet.getString("class" + i);
                    schedule.setClass(i, className);
                }
                data.add(schedule);
            }

            DBUtils.closeConnection(connection, statement);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        tableView.setItems(data);
        tableView.getColumns().add(0, dayCol);

        VBox root = new VBox(tableView);
        Scene scene = new Scene(root, 800, 400);

        primaryStage.setTitle("Weekly Schedule");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // Main method to launch the application
    public static void main(String[] args) {
        launch(args);
    }

    // Inner class to represent the schedule for a day
    public static final class DaySchedule {

        // Properties for the day and classes
        private final StringProperty day;
        private final StringProperty[] classes = new StringProperty[8];

        // Constructor
        public DaySchedule(String day) {
            this.day = new SimpleStringProperty(day);
            for (int i = 0; i < 8; i++) {
                classes[i] = new SimpleStringProperty();
            }
        }

        // Getters and setters
        public StringProperty dayProperty() {
            return day;
        }

        // Get the property for a class
        public StringProperty classProperty(int classNumber) {
            return classes[classNumber - 1];
        }

        // Set the class name for a class
        public void setClass(int classNumber, String className) {
            classes[classNumber - 1].set(className);
        }
    }
}