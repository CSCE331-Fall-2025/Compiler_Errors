import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.DatePicker;
import javafx.stage.Stage;

//I do not know how we're going to end up making one controller work for two fxmls, but if we don't, refactoring will be key
public class DatabaseController {
    
    @FXML
    private Button queryButton; //match the fx:id value from Scene Builder
    @FXML
    private TextField queryField; //match the fx:id value from Scene Builder
    @FXML
    private Button filterButton; //match the fx:id value from Scene Builder
    @FXML
    private DatePicker startDate;
    @FXML
    private DatePicker endDate;
    @FXML
    private TextArea dbView;
    @FXML
    private Button closeButton;
    
    
    private String lastStatement = "";
    
    private static final String DB_URL = "jdbc:postgresql://csce-315-db.engr.tamu.edu/CSCE315Database"; //database location
    private dbSetup my = new dbSetup();
    private record Entry(String username, String password, String userType){}; //Special subclass record type
    private List<Entry> users = new ArrayList<>();
    
    // This method runs automatically when the FXML loads
    @FXML
    public void initialize() {
        // Set up what happens when button is clicked
        queryButton.setOnAction(event -> runQuery());
        filterButton.setOnAction(event -> filterBtn());
        closeButton.setOnAction(event -> closeWindow());
    }
    
    private void filterBtn() {
        String db = dbView.getText();
        if(!"".equals(lastStatement) && (db.substring(0, db.indexOf('\n'))).contains("date")) {
            LocalDate start = startDate.getValue();
            LocalDate end = endDate.getValue();            
            if(start != null && end != null) {

                String startStr = start.toString();
                String endStr = end.toString();
                
                String sqlStatement = "SELECT * FROM (" + lastStatement.substring(0, lastStatement.length() - 1) + ") AS sub " +
                    "WHERE sub.\"date\" BETWEEN '" + startStr + "' AND '" + endStr + "';";

                try {
                    // Get database creditials
        
                    // Build the connection
                    Class.forName("org.postgresql.Driver");
                    Connection conn = DriverManager.getConnection(DB_URL, my.user, my.pswd);

                    // Create statement
                    Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(sqlStatement);

                    dbView.clear();

                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();

                    for (int i = 1; i <= columnCount; i++) {
                        dbView.appendText(metaData.getColumnName(i));
                        if (i < columnCount) dbView.appendText("\t");
                    }
                    dbView.appendText("\n");

                    while (rs.next()) {
                        for (int i = 1; i <= columnCount; i++) {
                            String value = rs.getString(i);
                            dbView.appendText(value != null ? value : "NULL");
                            if (i < columnCount) dbView.appendText("\t");
                        }
                        dbView.appendText("\n");
                    }

                    // Close connection
                    rs.close();
                    stmt.close();
                    conn.close();

                } catch (Exception e) {
                    dbView.setText("Error connecting to database:\n" + e.getMessage());
                    e.printStackTrace();
                    System.exit(0);
                }
                
            }    
        }
    }

    // Your method to run the database query
    private void runQuery() {
        System.out.println("Querying");
        dbView.setText("Query will run here...");

        try {
            // Get database creditials
 
            // Build the connection
            Class.forName("org.postgresql.Driver");
            Connection conn = DriverManager.getConnection(DB_URL, databaseCon.user, databaseCon.pswd);

            // Create statement
            Statement stmt = conn.createStatement();

            // Run sql query
            String sqlStatement = queryField.getText();
            sqlStatement += ";";
            lastStatement = sqlStatement;
            
            ResultSet rs = stmt.executeQuery(sqlStatement);

            dbView.clear();

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            for (int i = 1; i <= columnCount; i++) {
                dbView.appendText(metaData.getColumnName(i));
                if (i < columnCount) dbView.appendText("\t");
            }
            dbView.appendText("\n");

            while (rs.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    String value = rs.getString(i);
                    dbView.appendText(value != null ? value : "NULL");
                    if (i < columnCount) dbView.appendText("\t");
                }
                dbView.appendText("\n");
            }

            // Close connection
            rs.close();
            stmt.close();
            conn.close();

        } catch (Exception e) {
            dbView.setText("Error connecting to database:\n" + e.getMessage());
             e.printStackTrace();
            System.exit(0);
        }
    }

    private void closeWindow() { 
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }

    public void getUsers()
    {
        try {
 
            // Build the connection
            Class.forName("org.postgresql.Driver");
            Connection conn = DriverManager.getConnection(DB_URL, my.user, my.pswd);

            // Create statement
            Statement stmt = conn.createStatement();

            // Run sql query [update to pull data properly?]
            String sqlStatement = "SELECT * FROM usersce";
            ResultSet rs = stmt.executeQuery(sqlStatement);

            // Output result
            while (rs.next()) {
                users.add(new Entry(rs.getString("username"), rs.getString("password"), rs.getString("usertype")));
            }

            // Close connection
            rs.close();
            stmt.close();
            conn.close();

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    //Complete
    public String auth(String username, String password)
    {
        //Is this slow? Yes, but for refactor later
        for(Entry entry : users)
        {
            if(entry.username.equals(username) && entry.password.equals(password))
            {
                return entry.userType;
            }
        }
        return "No User Found";
    }
}
