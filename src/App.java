import javafx.application.Application;
import javafx.stage.Stage;

// This is the main class of the application. It extends the Application class from JavaFX and overrides the start method to initialize the UserLogin class.

public final class App extends Application {
    
    // Method to start the application
    @Override
    public void start(Stage primaryStage) {
        UserLogin login = new UserLogin(primaryStage);
        login.initializeComponents();
    }

    // Main method to launch the whole application
    public static void main(String[] args) {
        Application.launch(args);  // Call the static launch method from Application class
    }
}

//admin password
//manager 12345!Aa
//trainer ttt Tarik123!
//nutritionist nnn Noora123!
//clerk ccc Carl123!