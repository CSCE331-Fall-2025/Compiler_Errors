import java.io.IOException;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.stage.Stage;



/**
 * Bridge between login.fxml and backend
 */
public class LoginController{
    DatabaseController dbContr = new DatabaseController();

    @FXML
    private Button loginButton;
    @FXML
    private TextField usernameField, passwordField;

    /**
     * Initializes the login button and gets registered users
     */
    @FXML
    private void initialize()
    {
        loginButton.setOnAction(event -> login(event));   
        dbContr.getUsers();
    }

    /**
     * Verifies login. Returns when invalid login
     * Loads appropriate fxml upon validation
     * @param event - Used to link to stage. Passed in via initialize
     * @exception IOException Upon failing to register data from the appropriate fields
     */
    private void login(ActionEvent event)
    {
        String userType = dbContr.auth(usernameField.getText(),passwordField.getText());
        FXMLLoader loader;
        if(userType.equals("CASHIER"))
        {
            System.out.println("Cashier Identified");
            loader = new FXMLLoader(getClass().getResource("/Cashiermenu.fxml"));

        }
        else if(userType.equals("MANAGER"))
        {
            System.out.println("Manager Identified");
            loader = new FXMLLoader(getClass().getResource("/MANAGER.fxml"));
        }
        else
        {
            return;
        }
        
        try
        {
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }
}