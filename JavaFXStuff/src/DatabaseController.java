import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.DatePicker;

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
    private TextField addMenuNameField;
    @FXML
    private TextField addMenuPriceField;
    @FXML
    private TextField addMenuIngredientsField;
    @FXML
    private TextField updateMenuNameField;
    @FXML
    private TextField updateMenuNewNameField;
    @FXML
    private TextField updateMenuNewPriceField;
    @FXML
    private TextField addInvNameField;
    @FXML
    private TextField addInvQtyField;
    @FXML
    private TextField addInvUPField;
    @FXML
    private TextField updateInvNameField;
    @FXML
    private TextField updateInvQtyField;
    @FXML
    private TextField updateInvUPField;
    @FXML
    private TextField addEmpNameField;
    @FXML
    private TextField addEmpTypeField;
    @FXML
    private TextField addEmpEmailField;
    @FXML
    private TextField addEmpPhoneField;
    @FXML
    private TextField updateEmpNameField;
    @FXML
    private TextField updateEmpTypeField;
    @FXML
    private TextField updateEmpEmailField;
    @FXML
    private TextField updateEmpPhoneField;
    @FXML
    private TextField fireEmpNameField;
    @FXML
    private Button addMenuButton;
    @FXML
    private Button updateMenuButton;
    @FXML
    private Button addInvButton;
    @FXML
    private Button updateInvButton;
    @FXML
    private Button addEmpButton;
    @FXML
    private Button updateEmpButton;
    @FXML
    private Button fireEmpButton;

    // @FXML
    // private Button closeButton;
    
    
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
        addMenuButton.setOnAction(event -> addMenuBtn());
        updateMenuButton.setOnAction(event -> updateMenuBtn());
        addInvButton.setOnAction(event -> addInvBtn());
        updateInvButton.setOnAction(event -> updateInvBtn());
        addEmpButton.setOnAction(event -> addEmpBtn());
        updateEmpButton.setOnAction(event -> updateEmpBtn());
        fireEmpButton.setOnAction(event -> fireBtn());
        // closeButton.setOnAction(event -> closeWindow());
    }

    private void query(String query) {
        System.out.println("Attempting query: " + query);
        try {
            // Get database creditials
 
            // Build the connection
            Class.forName("org.postgresql.Driver");
            Connection conn = DriverManager.getConnection(DB_URL, my.user, my.pswd);
<<<<<<< HEAD
=======

            // Create statement
            Statement stmt = conn.createStatement();
            
            stmt.executeUpdate(query);

            // Close connection

            stmt.close();
            conn.close();

        } catch (Exception e) {
            dbView.setText("Error connecting to database:\n" + e.getMessage());
             e.printStackTrace();
            System.exit(0);
        }

    }

    private void addMenuBtn() {
        String name = addMenuNameField.getText();
        String price = addMenuPriceField.getText();
        String ingredients = addMenuIngredientsField.getText();
        
        if(name.isEmpty()) { return; }

        if(!price.isEmpty() && !ingredients.isEmpty()) {
            query("INSERT INTO menuce (name, price, ingredients) VALUES (\'" + name + "\', " + price + ", \'" + ingredients + "\');");
        }


    }

    private void updateMenuBtn() {
        String name = updateMenuNameField.getText();
        String newname = updateMenuNewNameField.getText();
        String newprice = updateMenuNewPriceField.getText();
        
        if(name.isEmpty()) { return; }

        if(!newname.isEmpty()) {
            query("UPDATE menuce SET name = \'" + newname + "\' WHERE name = \'" + name + "\';");
        }
        if(!newprice.isEmpty()) {
            query("UPDATE menuce SET price = \'" + newprice + "\' WHERE name = \'" + name + "\';");
        }

    }
    
    private void addInvBtn() {
        String name = addInvNameField.getText();
        String qty = addInvQtyField.getText();
        String up = addInvUPField.getText();

        if(name.isEmpty()) { return; }

        if(!qty.isEmpty() && !up.isEmpty()) {
            query("INSERT INTO inventoryce (name, quantity, unit_price) VALUES (\'" + name + "\', " + qty + ", " + up + ");");
        }
    }

    private void updateInvBtn() {
        String name = updateInvNameField.getText();
        String qty = updateInvQtyField.getText();
        String up = updateInvUPField.getText();


        if(name.isEmpty()) { return; }

        if(!qty.isEmpty()) {
            query("UPDATE inventoryce SET quantity = " + qty + " WHERE name = \'" + name + "\';");
        }
        if(!up.isEmpty()) {
            query("UPDATE inventoryce SET unit_price = " + up + " WHERE name = \'" + name + "\';");
        }

    }

    private void addEmpBtn() {
        String name = addEmpNameField.getText();
        String type = addEmpTypeField.getText();
        String email = addEmpEmailField.getText();
        String phone = addEmpPhoneField.getText();

        if(name.isEmpty()) { return; }

        if(!type.isEmpty() && !email.isEmpty() && !phone.isEmpty()) {
            query("INSERT INTO employeesce (name, employeetype, email, phonenum) VALUES (\'" + name + "\', \'" + type + "\', \'" + email + "\', \'" + phone + "\');");
        }
      
        
    }

    private void updateEmpBtn() {
        String name = updateEmpNameField.getText();
        String type = updateEmpTypeField.getText();
        String email = updateEmpEmailField.getText();
        String phone = updateEmpPhoneField.getText();

        if(name.isEmpty()) { return; }

        if(!type.isEmpty()) {
            query("UPDATE employeesce SET employeetype = \'" + type + "\' WHERE name = \'" + name + "\';");
        }
        if(!email.isEmpty()) {
            query("UPDATE employeesce SET email = \'" + email + "\' WHERE name = \'" + name + "\';");
        }
        if(!phone.isEmpty()) {
            query("UPDATE employeesce SET phonenum = \'" + phone + "\' WHERE name = \'" + name + "\';");
        }
      
    }

    private void fireBtn() {
        String name = fireEmpNameField.getText();
        query("DELETE FROM employeesce WHERE name = \'" + name + "\';");   
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
            Connection conn = DriverManager.getConnection(DB_URL, my.user, my.pswd);
>>>>>>> 79b2d737af60829bf80d1aa2150cbac56acecfc5

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

    /*
     * Validates user login information and returns associated user type
     * 
     * Does not throw exceptions.
     * 
     * @param username - username associated with login information
     * @param password - password associated with login information
     */
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
