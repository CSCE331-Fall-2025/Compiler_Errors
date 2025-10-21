import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * The main class
 * App extends Application from JavaFX
 * @version 1.0
 */
public class App extends Application {
    /**
     * No-argument constructor required by JavaFX when instantiating the controller.
     * Providing an explicit constructor so the generated Javadoc includes a
     * documented constructor instead of a default undocumented one.
     */
    public App() {}

    /**
     * Launches javafx thread
     * @param args unused
     */
    public static void main(String[] args){
        launch(App.class); //Launches the program
    }

    /**
     * Overrides start from JavaFX
     * Loads login.fxml and displays it
     * @param stage passed implicitly via launch
     * @exception Exception thrown when invalid fxml
     */
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