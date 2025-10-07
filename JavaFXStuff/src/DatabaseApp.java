import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class DatabaseApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        System.out.println(getClass().getResource("/MANAGER.fxml"));
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/MANAGER.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 600, 400);
        stage.setTitle("Panda Express Manager DB View");
        stage.setScene(scene);
        stage.show();
    }

}
