// This is the main class of the application. It extends the Application class from JavaFX and overrides the start method to initialize the UserLogin class.
import javafx.application.Application;
import javafx.stage.Stage;

public final class App extends Application {

    // Method to start the application
    @Override
    public void start(Stage primaryStage) {
        UserLogin login = new UserLogin(primaryStage);
        login.initializeComponents();
    }

    // Main method to launch the whole application
    public static void main(String[] args) {
        launch(args);
    }
}
