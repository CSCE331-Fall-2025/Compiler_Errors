import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.HashSet;
import java.util.Set;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ChoiceBox;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class DatabaseController {
    
    //Main
    @FXML
    private TextField queryField;
    @FXML 
    private TextArea dbView;
    @FXML
    private DatePicker startDate, endDate;
    @FXML
    private Button queryButton, filterButton;
    
    //Menu
    @FXML
    private TextField addMenuNameField, addMenuPriceField, addInvNameField, addMenuIngredientsField;
    @FXML
    private TextField updateMenuNewNameField, updateMenuNewPriceField, updateMenuNameField;
    @FXML
    private Button addMenuButton, updateMenuButton;

    //Inventory
    @FXML
    private TextField addInvQtyField, addInvUPField;
    @FXML
    private TextField updateInvNameField, updateInvQtyField, updateInvUPField;
    @FXML
    private Button addInvButton, updateInvButton;

    //Employee
    @FXML
    private TextField addEmpNameField, addEmpTypeField, addEmpEmailField, addEmpPhoneField;
    @FXML
    private TextField updateEmpTargetNameField, updateEmpNewNameField, updateEmpTypeField, updateEmpEmailField, updateEmpPhoneField;
    @FXML
    private TextField fireEmpNameField;
    @FXML
    private Button addEmpButton, updateEmpButton, fireEmpButton;


    @FXML
    private ChoiceBox reportBox;
    @FXML 
    private Button reportButton;
    @FXML
    private TextArea reportView;
    // @FXML
    // private Button closeButton;
    
    
    private String lastStatement = "";
    
    private static final String DB_URL = "jdbc:postgresql://csce-315-db.engr.tamu.edu/CSCE315Database"; //database location
    private dbSetup my = new dbSetup();
    private record Entry(String username, String password, String userType){}; //Special subclass record type
    private List<Entry> users = new ArrayList<>();
    public HashSet<String> ingredients;

    //Connection Variable
    Connection conn;
    Statement stmt;

    // This method runs automatically when the FXML loads
    @FXML
    public void initialize() {
        // Set up what happens when button is clicked
        ObservableList<String> reports = FXCollections.observableArrayList(
             "Top 5 Menu items", "Top 10 Sales Days", "All time profit"
        );
        reportBox.setItems(reports);

        ingredients = getIngredients();

        reportButton.setOnAction(event -> reportBtn());
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

    public void reportBtn() {
        String value = reportBox.getValue().toString();

        try {
            // Get database creditials
 
            // Build the connection
            Class.forName("org.postgresql.Driver");
            Connection conn = DriverManager.getConnection(DB_URL, my.user, my.pswd);

            // Create statement
            Statement stmt = conn.createStatement();
            String qry = "";

            if(value.equals("Top 5 Menu items")) {
                qry = "SELECT item, COUNT(*) AS sales FROM orderhistoryce GROUP BY item ORDER BY sales DESC LIMIT 5;";
            }

            if(value.equals("Top 10 Sales Days")) {
                qry = "SELECT date, COUNT(*) AS sales FROM orderhistoryce GROUP BY date ORDER BY sales DESC LIMIT 10;";
            }

            if(value.equals("All time profit")) {
                qry = "SELECT SUM(price * qty) AS profit FROM orderhistoryce;";
            }

            if(qry.isEmpty()) { return; }

            ResultSet rs = stmt.executeQuery(qry);
            
            reportView.clear();

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            for (int i = 1; i <= columnCount; i++) {
                reportView.appendText(metaData.getColumnName(i));
                if (i < columnCount) reportView.appendText("\t");
            }
            reportView.appendText("\n");

            while (rs.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    String values = rs.getString(i);
                    reportView.appendText(values != null ? values : "NULL");
                    if (i < columnCount) reportView.appendText("\t");
                }
                reportView.appendText("\n");
            }
            


            stmt.close();
            conn.close();

        } catch (Exception e) {
            reportView.setText("Error connecting to database:\n" + e.getMessage());
             e.printStackTrace();
        }
        

    }

    public HashSet<String> getIngredients() {
        HashSet<String> ingredients1 = new HashSet<>();

        try {
            Class.forName("org.postgresql.Driver");
            conn = DriverManager.getConnection(DB_URL, my.user, my.pswd);
            stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT ingredients FROM menuce;");

            while(rs.next()) {
                String row = rs.getString("ingredients");
                
                if (row != null && !row.isEmpty()) {
                    String[] items = row.split(", ");
                    for (String item : items) {
                        if (!item.isEmpty()) {
                            ingredients1.add(item.toLowerCase());
                        }
                    }
                }

            }
        } catch (Exception e) {
            dbView.setText("Error connecting to database:\n" + e.getMessage());
            e.printStackTrace();
            System.exit(0);
        }

        return ingredients1;
    }

    private boolean matches(String re, String str) {
        Pattern p = Pattern.compile(re);
        Matcher m = p.matcher(str);

        return m.find();
    }

    private boolean validPhone(String phone) {
        System.out.println("Validating phone #: " + phone);

        if(phone.length() != "(123) 456-7890".length()) { 
            System.out.println("Phone # invalid.");
            return false; 
        }

        boolean flag = matches("\\([0-9]{3}\\) [0-9]{3}\\-[0-9]{4}", phone);
        
        if(flag) {
            System.out.println("Phone # validated.");
        } else {
            System.out.println("Phone # invalid.");
        }

        return flag;
    }

    private void query(String query) {
        System.out.println("Attempting query: " + query);
        try {
            Class.forName("org.postgresql.Driver");
            conn = DriverManager.getConnection(DB_URL, my.user, my.pswd);
            stmt = conn.createStatement();

            stmt.executeUpdate(query);
        } catch (Exception e) {
            dbView.setText("Error:\n" + e.getMessage());
             e.printStackTrace();
        }

    }

    private void addMenuBtn() {
        String name = addMenuNameField.getText();
        String price = addMenuPriceField.getText();
        String ing = addMenuIngredientsField.getText();
        
        if(name.isEmpty()) { return; }

        String[] ingredientsArr = ing.split(", ");
        for(String i : ingredientsArr) {
            System.out.println(i);
            if(!ingredients.contains(i.toLowerCase())) {
                System.out.println("Menu item add attempt with invalid ingredients.");
                return;
            }
        }

        if(!price.isEmpty() && !ing.isEmpty()) {
            query("INSERT INTO menuce (name, price, ingredients) VALUES (\'" + name + "\', " + price + ", \'" + ing + "\');");
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

        if(!type.isEmpty() && !email.isEmpty() && !phone.isEmpty() && validPhone(phone)) {
            query("INSERT INTO employeesce (name, employeetype, email, phonenum) VALUES (\'" + name + "\', \'" + type + "\', \'" + email + "\', \'" + phone + "\');");
        }
      
        
    }

    private void updateEmpBtn() {
        String targetName = updateEmpTargetNameField.getText();
        String newName = updateEmpNewNameField.getText();
        String type = updateEmpTypeField.getText();
        String email = updateEmpEmailField.getText();
        String phone = updateEmpPhoneField.getText();

        if(targetName.isEmpty()) { return; }

        if(!type.isEmpty()) {
            query("UPDATE employeesce SET employeetype = \'" + type + "\' WHERE name = \'" + targetName + "\';");
        }
        if(!email.isEmpty()) {
            query("UPDATE employeesce SET email = \'" + email + "\' WHERE name = \'" + targetName + "\';");
        }
        if(!phone.isEmpty() && validPhone(phone)) {
            query("UPDATE employeesce SET phonenum = \'" + phone + "\' WHERE name = \'" + targetName + "\';");
        }
        if(!newName.isEmpty()) {
            query("UPDATE employeesce SET name = \'" + newName + "\' WHERE name = \'" + targetName + "\';");
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
                    Class.forName("org.postgresql.Driver");
                    conn = DriverManager.getConnection(DB_URL, my.user, my.pswd);
                    stmt = conn.createStatement();

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

                } catch (Exception e) {
                    dbView.setText("Error connecting to database:\n" + e.getMessage());
                    e.printStackTrace();
                }
                
            }    
        }
    }

    // Your method to run the database query
    private void runQuery() {
        System.out.println("Querying");
        dbView.setText("Query will run here...");

        try {
            // Run sql query
            String sqlStatement = queryField.getText();
            sqlStatement += ";";
            lastStatement = sqlStatement;


            Class.forName("org.postgresql.Driver");
            conn = DriverManager.getConnection(DB_URL, my.user, my.pswd);
            stmt = conn.createStatement();

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

        } catch (Exception e) {
            dbView.setText("Error:\n" + e.getMessage());
             e.printStackTrace();
        }
    }


    public void getUsers()
    {
        try {
 
            // Build the connection
            Class.forName("org.postgresql.Driver");
            conn = DriverManager.getConnection(DB_URL, my.user, my.pswd);

            // Create statement
            stmt = conn.createStatement();

            // Run sql query
            String sqlStatement = "SELECT * FROM usersce";
            ResultSet rs = stmt.executeQuery(sqlStatement);

            // Output result
            while (rs.next()) {
                users.add(new Entry(rs.getString("username"), rs.getString("password"), rs.getString("usertype")));
            }

            // Close connection
            rs.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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