package src;

import javafx.fxml.FXML;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CashierMenuController {
    
    @FXML
    private ListView<String> notesListView;
    
    @FXML
    private ListView<String> receiptListView;
    
    @FXML
    private ComboBox<String> personSelector;

    // Menu buttons (from FXML)
    @FXML
    private Button orangeChickenBtn;
    @FXML
    private Button grilledTeriyakiBtn;
    @FXML
    private Button chowMeinBtn;
    @FXML
    private Button friedRiceBtn;
    @FXML
    private Button stringBeanBtn;
    @FXML
    private Button teriyakiChickenBtn;
    @FXML
    private Button mushroomChickenBtn;
    @FXML
    private Button steamedRiceBtn;
    
    private Order currentOrder;
    private ObservableList<String> receiptItems;
    private ObservableList<String> notesList;
    private ObservableList<String> personList;
    private Map<String, MenuItem> menuItemsMap;
    private String currentPerson;
    private int personCounter = 1;
    
    @FXML
    public void initialize() {
        currentOrder = new Order();
        receiptItems = FXCollections.observableArrayList();
        notesList = FXCollections.observableArrayList();
        personList = FXCollections.observableArrayList("Person 1");
        menuItemsMap = new HashMap<>();
        currentPerson = "Person 1";
        
        receiptListView.setItems(receiptItems);
        notesListView.setItems(notesList);
        
        /* setup person selector if it exists in fxml */
            if (personSelector != null) {
            personSelector.setItems(personList);
            personSelector.setValue(currentPerson);
            personSelector.setOnAction(e -> {
                currentPerson = personSelector.getValue();
                notesList.add("Selected: " + currentPerson);
                // reference the event parameter so static checks don't flag it as unused
                if (e == null) { }
            });
        }
        
        loadMenuItemsFromDatabase();
        updateReceipt();
    }
    
    private void loadMenuItemsFromDatabase() {
        try {
            Connection conn = DatabaseConnection.getConnection();
            String query = "SELECT name, price, ingredients FROM menuce";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            
            while (rs.next()) {
                String name = rs.getString("name");
                double price = rs.getDouble("price");
                String ingredients = rs.getString("ingredients");
                
                /* determines the category based on name/ingredients */
                String category = determineCategory(name, ingredients);
                
                MenuItem item = new MenuItem(name, price, ingredients, category);
                menuItemsMap.put(name.trim().toLowerCase(), item);
                
                /* debug output in case it is stucks */
                System.out.println("Loaded: '" + name + "' -> key: '" + name.trim().toLowerCase() + "' price: $" + price);
            }
            
            rs.close();
            stmt.close();
            
            notesList.add("- Menu loaded: " + menuItemsMap.size() + " items");
            
        } catch (SQLException e) {
            showError("Database Error", "Failed to load menu items: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private String determineCategory(String name, String ingredients) {
        String nameLower = name.toLowerCase();
        String ingredientsLower = ingredients.toLowerCase();
        
        if (nameLower.contains("rice") || nameLower.contains("noodle") || 
            nameLower.contains("chow mein")) {
            return "side";
        } else if (ingredientsLower.contains("chicken") || ingredientsLower.contains("beef") ||
                   ingredientsLower.contains("shrimp") || ingredientsLower.contains("steak")) {
            return "entree";
        } else {
            return "side";
        }
    }
    
    @FXML
    private void addNewPerson() {
        personCounter++;
        String newPerson = "Person " + personCounter;
        personList.add(newPerson);
        currentPerson = newPerson;
        if (personSelector != null) {
            personSelector.setValue(currentPerson);
        }
        notesList.add("+ Added " + newPerson);
        updateReceipt();
    }

    @FXML
    private void addOrder(ActionEvent event) {
        // Determine which button was clicked and add that item
        if (!(event.getSource() instanceof Button)) return;
        Button btn = (Button) event.getSource();

        // Prefer DB canonical name if available (we'll derive from button text)
        String text = btn.getText();
        if (text == null) return;

        // Strip price and any [OUT] markers from the button label
        text = text.replaceAll("\\[OUT\\]", "").replaceAll("\\$[0-9.,]+", "").trim();

        // Use the cleaned label as the requested item name
        addItemByName(text);
    }

    // Helper to add a menu item to the current order using fuzzy lookup
    private void addItemByName(String itemName) {
        if (itemName == null) return;
        String key = itemName.trim().toLowerCase();

        String foundKey = null;
        if (menuItemsMap.containsKey(key)) {
            foundKey = key;
        } else {
            foundKey = findMenuKeyFor(itemName);
        }

        if (foundKey != null && menuItemsMap.containsKey(foundKey)) {
            MenuItem item = menuItemsMap.get(foundKey);
            currentOrder.addItemToPerson(currentPerson, item);
            notesList.add("+ Added " + item.getName() + " for " + currentPerson);
            updateReceipt();
            System.out.println("Added: " + item.getName() + " ($" + item.getPrice() + ")");
        } else {
            notesList.add("⚠ ERROR: " + itemName + " not found in menu");
            showError("Item Not Found", "Could not find " + itemName + " in the menu database.");
        }
    }

    // Simple fuzzy matching: swap two-word tokens, token containment, strip punctuation
    private String findMenuKeyFor(String itemName) {
        if (itemName == null) return null;
        String key = itemName.trim().toLowerCase().replaceAll("[^a-z0-9\\s]", "");

        // direct match
        if (menuItemsMap.containsKey(key)) return key;

        // two-word swap
        String[] parts = key.split("\\s+");
        if (parts.length == 2) {
            String swapped = parts[1] + " " + parts[0];
            if (menuItemsMap.containsKey(swapped)) return swapped;
        }

        // token containment
        String[] tokens = key.split("\\s+");
        for (String menuKey : menuItemsMap.keySet()) {
            boolean all = true;
            for (String t : tokens) {
                if (!menuKey.contains(t)) { all = false; break; }
            }
            if (all) return menuKey;
        }

        // fallback contains
        for (String menuKey : menuItemsMap.keySet()) {
            if (menuKey.contains(key) || key.contains(menuKey)) return menuKey;
        }

        return null;
    }
    
    private void updateReceipt() {
        receiptItems.clear();
        
        Map<String, List<MenuItem>> personOrders = currentOrder.getPersonOrders();
        
        if (personOrders.isEmpty()) {
            receiptItems.add("No items added yet");
            return;
        }
        
        /* it displays the items per person */
        for (String person : personOrders.keySet()) {
            receiptItems.add("------ " + person + " ------");
            List<MenuItem> items = personOrders.get(person);
            
            for (MenuItem item : items) {
                receiptItems.add("  " + item.getName() + " ........$" + 
                               String.format("%.2f", item.getPrice()));
            }
            
            double personSubtotal = currentOrder.getPersonSubtotal(person);
            receiptItems.add("  Subtotal: $" + String.format("%.2f", personSubtotal));
            receiptItems.add("");
        }
        
        //totals
        receiptItems.add("_____________________");
        receiptItems.add("Subtotal (pre-tax)  $" + String.format("%.2f", currentOrder.getSubtotal()));
        receiptItems.add("Tax (8.25%) $" + String.format("%.2f", currentOrder.getTax()));
        receiptItems.add("________________");
        receiptItems.add("TOTAL $" + String.format("%.2f", currentOrder.getTotalAmount()));
    }
    
    @FXML
    private void handleCheckout() {
        if (currentOrder.isEmpty()) {
            showError("Empty Order", "pls add items");
            return;
        }
        
        try {
            Connection conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            
            LocalDate currentDate = LocalDate.now();
            LocalTime currentTime = LocalTime.now();
            
            /*gets the next available id */
            String getMaxIdQuery = "SELECT COALESCE(MAX(id), 0) + 1 AS next_id FROM orderhistoryce";
            Statement idStmt = conn.createStatement();
            ResultSet idRs = idStmt.executeQuery(getMaxIdQuery);
            int nextId = 1;
            if (idRs.next()) {
                nextId = idRs.getInt("next_id");
            }
            idRs.close();
            idStmt.close();
            
          /*  inserts the item as a separate row in orderhistoryce*/
            String orderQuery = "INSERT INTO orderhistoryce (id, date, time, item, qty, price) VALUES (?, ?, ?, ?, ?, ?)";
            PreparedStatement orderStmt = conn.prepareStatement(orderQuery);
            
            int orderId = nextId;  // Use same ID for all items in this order
            int batchCount = 0;
            for (MenuItem item : currentOrder.getAllItems()) {
                orderStmt.setInt(1, orderId); 
                orderStmt.setDate(2, Date.valueOf(currentDate));
                orderStmt.setTime(3, Time.valueOf(currentTime));
                orderStmt.setString(4, item.getName());
                orderStmt.setInt(5, 1); 
                orderStmt.setDouble(6, item.getPrice());
                orderStmt.addBatch();
                batchCount++;
            }
            
            System.out.println("Executing batch insert for " + batchCount + " items starting from ID " + (nextId - batchCount) + "...");
            int[] results = orderStmt.executeBatch();
            System.out.println("Batch insert completed: " + results.length + " rows affected");
            orderStmt.close();
            
            conn.commit();
            conn.setAutoCommit(true);
            
            /*receipt*/
            StringBuilder receipt = new StringBuilder();
            receipt.append("Order Completed!\n");
            receipt.append("________________\n\n");
            
            for (String person : currentOrder.getPersonOrders().keySet()) {
                receipt.append(person).append(":\n");
                for (MenuItem item : currentOrder.getPersonOrders().get(person)) {
                    receipt.append("  • ").append(item.getName())
                           .append(" - $").append(String.format("%.2f", item.getPrice())).append("\n");
                }
                receipt.append("\n");
            }
            
            receipt.append("_______________________\n");
            receipt.append("Subtotal: $").append(String.format("%.2f", currentOrder.getSubtotal())).append("\n");
            receipt.append("Tax: $").append(String.format("%.2f", currentOrder.getTax())).append("\n");
            receipt.append("TOTAL: $").append(String.format("%.2f", currentOrder.getTotalAmount())).append("\n");
            
            showInfo("Checkout Successful", receipt.toString());
            
            //it then reset for new order
            currentOrder = new Order();
            personCounter = 1;
            currentPerson = "Person 1";
            personList.clear();
            personList.add("Person 1");
            if (personSelector != null) {
                personSelector.setValue(currentPerson);
            }
            receiptItems.clear();
            notesList.add("--------------------");
            notesList.add("order completed ready for new order");
            updateReceipt();
            
        } catch (SQLException e) {
            try {
                DatabaseConnection.getConnection().rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            showError("checkout Error", "failed to process checkout: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
