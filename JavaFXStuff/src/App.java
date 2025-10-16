import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class App extends Application {
    @FXML
    private Button loginButton;

    @FXML
    private TextField usernameField, passwordField;

    public static void main(String[] args){
        launch(App.class); //Launches the program
    }

    //Gets userType from launch() and loads appropriate GUI
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader = new FXMLLoader(getClass().getResource("/login.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 950, 800); //What fxml, X-Size, Y-Size
        stage.setTitle("POS System"); //Name of the Application
        stage.setScene(scene); //Load this scene
        stage.show();
    }
}