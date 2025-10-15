//import [library here]
import java.util.Scanner;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {
    public static void main(String[] args){
        Scanner scanner = new Scanner(System.in);
        DatabaseController dbContr = new DatabaseController();
        String username;
        String password;
        String userType;
        
        //Have program store users to prevent repeat calls
        dbContr.getUsers();

        while(true)
        {
            System.out.print("Enter username: ");
            username = scanner.nextLine();
            System.out.print("Enter password: ");
            password = scanner.nextLine();

            //Find valid user
            userType = dbContr.auth(username,password);
            if(userType.equals("CASHIER"))
            {
                System.out.println("Cashier Identified");
                break;
            }
            else if(userType.equals("MANAGER"))
            {
                System.out.println("Manager Identified");
                break;
            }
            System.out.println("Incorrect username or Password. Try Again");
        }
        launch(App.class, "--userType=" + userType); //Launches the program
        scanner.close();
    }

    //Gets userType from launch() and loads appropriate GUI
    @Override
    public void start(Stage stage) throws Exception {
        String userType = getParameters().getNamed().getOrDefault("userType", "UNKNOWN");

        FXMLLoader fxmlLoader = new FXMLLoader();
        if(userType.equals("CASHIER"))
        {
            //System.out.println("Cashier time!");
            //Load Cashier here
            fxmlLoader = new FXMLLoader(getClass().getResource("/Cashiermenu.fxml"));
        }
        else if(userType.equals("MANAGER"))
        {
            //System.out.println("Manager time!");
            //Load manager
            fxmlLoader = new FXMLLoader(getClass().getResource("/MANAGER.fxml"));
        }
        else
        {
            System.exit(0);
        }
        Scene scene = new Scene(fxmlLoader.load(), 950, 800); //What fxml, X-Size, Y-Size
        stage.setTitle("POS System"); //Name of the Application
        stage.setScene(scene); //Load this scene
        stage.show();
    }
}