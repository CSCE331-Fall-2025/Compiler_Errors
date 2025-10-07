//import [library here]
import java.util.Scanner;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {
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
        //Tear the users of the database
        dbContr.getUsers();

        while(true)
        {
            System.out.print("Enter username: ");
            username = scanner.nextLine();
            System.out.print("Enter password: ");
            password = scanner.nextLine();

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
        
        
        Scanner scanner = new Scanner(System.in);
        DatabaseController dbContr = new DatabaseController();
        String username;
        String password;
        String userType;
        //Tear the users of the database
        dbContr.getUsers();

        while(true)
        {
            System.out.print("Enter username: ");
            username = scanner.nextLine();
            System.out.print("Enter password: ");
            password = scanner.nextLine();

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
        
        
        //Comments are like so!
        /*Multiline Comments! */


        /*
         * JavaFX Documentation!
         * Source: https://www.tutorialspoint.com/javafx/javafx_architecture.htm
         * using "javafx"
         * animation -> animation stuff (decoration/transition stuff)
         * application -> ?
         * css -> Adds CSSlike stuff
         * event -> handles events (like buttons presumably)
         * geometry -> 2D Shapes
         * stage -> top level container classes?
         * scene -> 
         */
    }

    //DatabaseController is where mappings between the database and the GUI take place
    //This is the selector for which display to show (with example being the temporary one)
    @Override
    public void start(Stage stage) throws Exception {
        String userType = getParameters().getNamed().getOrDefault("userType", "UNKNOWN");

        //Might cause memory leaks, not sure
        FXMLLoader fxmlLoader = new FXMLLoader();
        if(userType.equals("CASHIER"))
        {
            //System.out.println("Cashier time!");
            //Load Cashier here
            fxmlLoader = new FXMLLoader(getClass().getResource("database-view.fxml"));
        }
        else if(userType.equals("MANAGER"))
        {
            //System.out.println("Manager time!");
            //Load manager
            fxmlLoader = new FXMLLoader(getClass().getResource("database-view.fxml"));
        }
        else
        {
            //Else, load default temporary fxml
            fxmlLoader = new FXMLLoader(getClass().getResource("database-view.fxml"));
        }
        Scene scene = new Scene(fxmlLoader.load(), 600, 400); //What fxml, X-Size, Y-Size
        stage.setTitle("AWS PostgreSQL Query Example"); //Name of the Application
        stage.setScene(scene); //Load this scene
        stage.show();
    }
}