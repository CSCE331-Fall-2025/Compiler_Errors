import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;

public class Controller {

    @FXML
    private ListView<String> purchasesList;

    @FXML
    private ListView<String> notesList;

    public void initialize() {
        // This method will run when the FXML is loaded
        // Optional: Any setup code
    }

    @FXML
    private void addOrder(ActionEvent event) {
        // Extract button text from the button that was clicked
        String item = ((javafx.scene.control.Button) event.getSource()).getText();
        purchasesList.getItems().add(item);
    }
}
