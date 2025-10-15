package src;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.application.Platform;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class CashierMenuController {
    
    @FXML
    private ListView<String> notesListView;
    
    @FXML
    private ListView<String> receiptListView;
    
    @FXML
    private ComboBox<String> personSelector;
    
    // Menu item buttons
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
    
    // Map buttons to their associated menu item keys (for lookup)
    // This will be dynamically updated when menu changes
    private Map<Button, String> buttonToMenuKeyMap;
    
    private String currentPerson;
    private int personCounter = 1;
    
    // Real-time sync variables
    private Timer syncTimer;
    private Map<String, Integer> lastInventoryState;
    private Map<String, MenuItem> lastMenuState;
    private static final int SYNC_INTERVAL_MS = 3000; // Check every 3 seconds
    
    @FXML
    public void initialize() {
        currentOrder = new Order();
        receiptItems = FXCollections.observableArrayList();
        notesList = FXCollections.observableArrayList();
        personList = FXCollections.observableArrayList("Person 1");
        menuItemsMap = new HashMap<>();
        buttonToMenuKeyMap = new HashMap<>();
        lastInventoryState = new HashMap<>();
        lastMenuState = new HashMap<>();
        currentPerson = "Person 1";
        
        receiptListView.setItems(receiptItems);
        notesListView.setItems(notesList);
        
        // Map buttons to their initial menu item names
        initializeButtonMap();
        
        // Setup person selector
        if (personSelector != null) {
            personSelector.setItems(personList);
            personSelector.setValue(currentPerson);
            personSelector.setOnAction(e -> {
                currentPerson = personSelector.getValue();
                notesList.add("Selected: " + currentPerson);
            });
        }
        
        // Load initial data
        loadMenuItemsFromDatabase();
        checkInventoryAndUpdateButtons();
        updateReceipt();
        
        // Start real-time sync
        startRealtimeSync();
        
        notesList.add("📄 Real-time sync enabled");
    }
    
    private void initializeButtonMap() {
        // Map each button to its original database key
        if (orangeChickenBtn != null) {
            buttonToMenuKeyMap.put(orangeChickenBtn, "orange chicken");
        }
        if (grilledTeriyakiBtn != null) {
            buttonToMenuKeyMap.put(grilledTeriyakiBtn, "grilled teriyaki chicken");
        }
        if (chowMeinBtn != null) {
            buttonToMenuKeyMap.put(chowMeinBtn, "chow mein");
        }
        if (friedRiceBtn != null) {
            buttonToMenuKeyMap.put(friedRiceBtn, "fried rice");
        }
        if (stringBeanBtn != null) {
            buttonToMenuKeyMap.put(stringBeanBtn, "string bean chicken breast");
        }
        if (teriyakiChickenBtn != null) {
            buttonToMenuKeyMap.put(teriyakiChickenBtn, "teriyaki chicken");
        }
        if (mushroomChickenBtn != null) {
            buttonToMenuKeyMap.put(mushroomChickenBtn, "mushroom chicken");
        }
        if (steamedRiceBtn != null) {
            buttonToMenuKeyMap.put(steamedRiceBtn, "steamed white rice");
        }
    }
    
    /**
     * Starts a background timer that checks for database changes every few seconds
     */
    private void startRealtimeSync() {
        syncTimer = new Timer(true); // Daemon thread
        syncTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    checkForDatabaseChanges();
                } catch (Exception e) {
                    System.err.println("Sync error: " + e.getMessage());
                }
            }
        }, SYNC_INTERVAL_MS, SYNC_INTERVAL_MS);
    }
    
    /**
     * Checks if menu or inventory has changed in the database
     */
    private void checkForDatabaseChanges() {
        try {
            Connection conn = DatabaseConnection.getConnection();
            
            boolean menuChanged = checkMenuChanges(conn);
            boolean inventoryChanged = checkInventoryChanges(conn);
            
            if (menuChanged || inventoryChanged) {
                // Update UI on JavaFX thread
                Platform.runLater(() -> {
                    if (menuChanged) {
                        notesList.add("📋 Menu updated from database");
                        loadMenuItemsFromDatabase();
                        checkInventoryAndUpdateButtons(); // Also check inventory after menu update
                    }
                    if (inventoryChanged) {
                        notesList.add("📦 Inventory changed - updating availability");
                        checkInventoryAndUpdateButtons();
                    }
                });
            }
            
        } catch (SQLException e) {
            System.err.println("Error checking database changes: " + e.getMessage());
        }
    }
    
    /**
     * Checks if menu items have been added, removed, or modified
     */
    private boolean checkMenuChanges(Connection conn) throws SQLException {
        Map<String, MenuItem> currentMenu = new HashMap<>();
        
        String query = "SELECT name, price, ingredients FROM menuce";
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(query);
        
        while (rs.next()) {
            String name = rs.getString("name");
            double price = rs.getDouble("price");
            String ingredients = rs.getString("ingredients");
            String category = determineCategory(name, ingredients);
            
            MenuItem item = new MenuItem(name, price, ingredients, category);
            currentMenu.put(name.trim().toLowerCase(), item);
        }
        
        rs.close();
        stmt.close();
        
        // Check if menu has changed
        if (currentMenu.size() != lastMenuState.size()) {
            lastMenuState = currentMenu;
            return true;
        }
        
        for (Map.Entry<String, MenuItem> entry : currentMenu.entrySet()) {
            String key = entry.getKey();
            MenuItem newItem = entry.getValue();
            MenuItem oldItem = lastMenuState.get(key);
            
            if (oldItem == null || oldItem.getPrice() != newItem.getPrice() 
                || !oldItem.getName().equals(newItem.getName())
                || !oldItem.getIngredients().equals(newItem.getIngredients())) {
                lastMenuState = currentMenu;
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Checks if inventory quantities have changed
     */
    private boolean checkInventoryChanges(Connection conn) throws SQLException {
        Map<String, Integer> currentInventory = new HashMap<>();
        
        String query = "SELECT name, quantity FROM inventoryce";
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(query);
        
        while (rs.next()) {
            String name = rs.getString("name").trim().toLowerCase();
            int quantity = rs.getInt("quantity");
            currentInventory.put(name, quantity);
        }
        
        rs.close();
        stmt.close();
        
        // Check if any quantities changed
        if (currentInventory.size() != lastInventoryState.size()) {
            lastInventoryState = currentInventory;
            return true;
        }
        
        for (Map.Entry<String, Integer> entry : currentInventory.entrySet()) {
            String ingredient = entry.getKey();
            Integer newQty = entry.getValue();
            Integer oldQty = lastInventoryState.get(ingredient);
            
            if (oldQty == null || !oldQty.equals(newQty)) {
                lastInventoryState = currentInventory;
                return true;
            }
        }
        
        return false;
    }
    
    private void loadMenuItemsFromDatabase() {
        try {
            Connection conn = DatabaseConnection.getConnection();
            String query = "SELECT name, price, ingredients FROM menuce";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            
            menuItemsMap.clear();
            lastMenuState.clear();
            
            while (rs.next()) {
                String name = rs.getString("name");
                double price = rs.getDouble("price");
                String ingredients = rs.getString("ingredients");
                
                String category = determineCategory(name, ingredients);
                
                MenuItem item = new MenuItem(name, price, ingredients, category);
                String key = name.trim().toLowerCase();
                menuItemsMap.put(key, item);
                lastMenuState.put(key, item);
                
                System.out.println("Loaded: '" + name + "' -> $" + price);
            }
            
            rs.close();
            stmt.close();
            
            // Update all button displays
            updateAllButtonDisplays();
            
        } catch (SQLException e) {
            showError("Database Error", "Failed to load menu items: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Updates all button text to reflect current menu items from database.
     */
    private void updateAllButtonDisplays() {
        System.out.println("=== updateAllButtonDisplays() called ===");
        
        // Update each button by looking up its menu key
        updateButtonDisplay(orangeChickenBtn, "orange chicken");
        updateButtonDisplay(grilledTeriyakiBtn, "grilled teriyaki chicken");
        updateButtonDisplay(chowMeinBtn, "chow mein");
        updateButtonDisplay(friedRiceBtn, "fried rice");
        updateButtonDisplay(stringBeanBtn, "string bean chicken breast");
        updateButtonDisplay(teriyakiChickenBtn, "teriyaki chicken");
        updateButtonDisplay(mushroomChickenBtn, "mushroom chicken");
        updateButtonDisplay(steamedRiceBtn, "steamed white rice");
        
        System.out.println("=== updateAllButtonDisplays() completed ===");
    }
    
    /**
     * Helper method to update a single button's display
     */
    private void updateButtonDisplay(Button button, String menuKey) {
        if (button == null) return;
        
        MenuItem item = menuItemsMap.get(menuKey);
        if (item != null) {
            try {
                // Format: Name on first line, price on second line
                String displayText = item.getName() + "\n$" + String.format("%.2f", item.getPrice());
                button.setText(displayText);
                System.out.println("  Updated button for '" + menuKey + "' to: " + item.getName() + " $" + item.getPrice());
            } catch (Exception e) {
                System.err.println("  Error updating button for " + menuKey + ": " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("  WARNING: No menu item found for key '" + menuKey + "'");
        }
    }
    
    private void checkInventoryAndUpdateButtons() {
        try {
            Connection conn = DatabaseConnection.getConnection();
            
            lastInventoryState.clear();
            
            // Get current inventory state
            String invQuery = "SELECT name, quantity FROM inventoryce";
            Statement invStmt = conn.createStatement();
            ResultSet invRs = invStmt.executeQuery(invQuery);
            
            while (invRs.next()) {
                String name = invRs.getString("name").trim().toLowerCase();
                int quantity = invRs.getInt("quantity");
                lastInventoryState.put(name, quantity);
            }
            invRs.close();
            invStmt.close();
            
            // Check each button's menu item availability
            for (Map.Entry<Button, String> entry : buttonToMenuKeyMap.entrySet()) {
                Button btn = entry.getKey();
                String menuKey = entry.getValue();
                MenuItem item = menuItemsMap.get(menuKey);
                
                if (item != null) {
                    boolean isAvailable = checkIngredientAvailability(conn, item);
                    updateButtonState(btn, item, isAvailable);
                }
            }
            
        } catch (SQLException e) {
            showError("Inventory Check Error", "Failed to check inventory: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Updates button appearance based on availability
     */
    private void updateButtonState(Button btn, MenuItem item, boolean isAvailable) {
        String baseText = item.getName() + "\n$" + String.format("%.2f", item.getPrice());
        
        btn.setDisable(!isAvailable);
        
        if (!isAvailable) {
            btn.setStyle("-fx-opacity: 0.5; -fx-background-color: #cccccc;");
            btn.setText(baseText + "\n[OUT OF STOCK]");
            Tooltip tooltip = new Tooltip(item.getName() + " - OUT OF STOCK");
            btn.setTooltip(tooltip);
        } else {
            btn.setStyle(""); // Reset to default style
            btn.setText(baseText);
            Tooltip tooltip = new Tooltip(item.getName() + " - $" + String.format("%.2f", item.getPrice()));
            btn.setTooltip(tooltip);
        }
    }
    
    private boolean checkIngredientAvailability(Connection conn, MenuItem item) throws SQLException {
        String ingredients = item.getIngredients();
        if (ingredients == null || ingredients.isEmpty()) {
            return true;
        }
        
        String[] ingArray = ingredients.split(",");
        String checkQuery = "SELECT quantity FROM inventoryce WHERE LOWER(name) = LOWER(?)";
        PreparedStatement pstmt = conn.prepareStatement(checkQuery);
        
        for (String ing : ingArray) {
            String ingName = ing.trim();
            if (ingName.isEmpty()) continue;
            
            pstmt.setString(1, ingName);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                int quantity = rs.getInt("quantity");
                if (quantity < 1) {
                    rs.close();
                    pstmt.close();
                    return false;
                }
            } else {
                rs.close();
                pstmt.close();
                return false;
            }
            rs.close();
        }
        
        pstmt.close();
        return true;
    }
    
    private String determineCategory(String name, String ingredients) {
        String nameLower = name.toLowerCase();
        String ingredientsLower = ingredients != null ? ingredients.toLowerCase() : "";
        
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
    private void addOrder(javafx.event.ActionEvent event) {
        // Get the button that triggered the event
        Button pressedButton = (Button) event.getSource();
        
        // Look up the menu key for this button
        if (buttonToMenuKeyMap.containsKey(pressedButton)) {
            String menuKey = buttonToMenuKeyMap.get(pressedButton);
            addItemByMenuKey(menuKey);
        } else {
            notesList.add(" ERROR: Unknown button pressed");
            System.err.println("Button not found in buttonToMenuKeyMap");
        }
    }

    private void addItemByMenuKey(String menuKey) {
        if (menuItemsMap.containsKey(menuKey)) {
            MenuItem item = menuItemsMap.get(menuKey);
            
            // Double-check availability before adding
            try {
                Connection conn = DatabaseConnection.getConnection();
                if (!checkIngredientAvailability(conn, item)) {
                    showError("Item Unavailable", item.getName() + " is currently out of stock.");
                    return;
                }
            } catch (SQLException e) {
                showError("Error", "Could not verify item availability: " + e.getMessage());
                return;
            }
            
            currentOrder.addItemToPerson(currentPerson, item);
            notesList.add("+ Added " + item.getName() + " for " + currentPerson);
            updateReceipt();
            System.out.println("Added: " + item.getName() + " ($" + item.getPrice() + ")");
        } else {
            notesList.add(" ERROR: Menu item not found");
            showError("Item Not Found", "Could not find the item in the menu database.");
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
    
    private void updateReceipt() {
        receiptItems.clear();
        
        Map<String, List<MenuItem>> personOrders = currentOrder.getPersonOrders();
        
        if (personOrders.isEmpty()) {
            receiptItems.add("No items added yet");
            return;
        }
        
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
        
        receiptItems.add("_____________________");
        receiptItems.add("Subtotal (pre-tax)  $" + String.format("%.2f", currentOrder.getSubtotal()));
        receiptItems.add("Tax (8.25%) $" + String.format("%.2f", currentOrder.getTax()));
        receiptItems.add("________________");
        receiptItems.add("TOTAL $" + String.format("%.2f", currentOrder.getTotalAmount()));
    }
    
    @FXML
    private void handleCheckout() {
        if (currentOrder.isEmpty()) {
            showError("Empty Order", "Please add items before checkout");
            return;
        }
        
        try {
            Connection conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            
            LocalDate currentDate = LocalDate.now();
            LocalTime currentTime = LocalTime.now();
            
            String getMaxIdQuery = "SELECT COALESCE(MAX(id), 0) + 1 AS next_id FROM orderhistoryce";
            Statement idStmt = conn.createStatement();
            ResultSet idRs = idStmt.executeQuery(getMaxIdQuery);
            int orderId = 1;
            if (idRs.next()) {
                orderId = idRs.getInt("next_id");
            }
            idRs.close();
            idStmt.close();
            
            String orderQuery = "INSERT INTO orderhistoryce (id, date, time, item, qty, price) VALUES (?, ?, ?, ?, ?, ?)";
            PreparedStatement orderStmt = conn.prepareStatement(orderQuery);
            
            Map<String, Integer> ingredientUsage = new HashMap<>();
            
            for (MenuItem item : currentOrder.getAllItems()) {
                orderStmt.setInt(1, orderId);
                orderStmt.setDate(2, Date.valueOf(currentDate));
                orderStmt.setTime(3, Time.valueOf(currentTime));
                orderStmt.setString(4, item.getName());
                orderStmt.setInt(5, 1); 
                orderStmt.setDouble(6, item.getPrice());
                orderStmt.addBatch();
                
                String ingredients = item.getIngredients();
                if (ingredients != null && !ingredients.isEmpty()) {
                    String[] ingArray = ingredients.split(",");
                    for (String ing : ingArray) {
                        String ingName = ing.trim();
                        if (!ingName.isEmpty()) {
                            ingredientUsage.put(ingName, ingredientUsage.getOrDefault(ingName, 0) + 1);
                        }
                    }
                }
            }
            
            orderStmt.executeBatch();
            orderStmt.close();
            
            if (!ingredientUsage.isEmpty()) {
                String updateInventoryQuery = "UPDATE inventoryce SET quantity = quantity - ? WHERE LOWER(name) = LOWER(?)";
                PreparedStatement invStmt = conn.prepareStatement(updateInventoryQuery);
                
                for (Map.Entry<String, Integer> entry : ingredientUsage.entrySet()) {
                    invStmt.setInt(1, entry.getValue());
                    invStmt.setString(2, entry.getKey());
                    invStmt.addBatch();
                }
                
                invStmt.executeBatch();
                invStmt.close();
            }
            
            String checkInventoryQuery = "SELECT name, quantity FROM inventoryce WHERE quantity < 10";
            Statement checkStmt = conn.createStatement();
            ResultSet lowInvRs = checkStmt.executeQuery(checkInventoryQuery);
            
            StringBuilder lowInventoryWarning = new StringBuilder();
            while (lowInvRs.next()) {
                lowInventoryWarning.append(lowInvRs.getString("name"))
                    .append(": ").append(lowInvRs.getInt("quantity")).append(" remaining\n");
            }
            lowInvRs.close();
            checkStmt.close();
            
            conn.commit();
            conn.setAutoCommit(true);
            
            StringBuilder receipt = new StringBuilder();
            receipt.append("Order #").append(orderId).append(" Completed!\n");
            receipt.append("Date: ").append(currentDate).append("\n");
            receipt.append("Time: ").append(currentTime.toString().substring(0, 8)).append("\n");
            receipt.append("________________________\n\n");
            
            for (String person : currentOrder.getPersonOrders().keySet()) {
                receipt.append(person).append(":\n");
                for (MenuItem item : currentOrder.getPersonOrders().get(person)) {
                    receipt.append("  • ").append(item.getName())
                           .append(" - $").append(String.format("%.2f", item.getPrice())).append("\n");
                }
                receipt.append("\n");
            }
            
            receipt.append("________________________\n");
            receipt.append("Subtotal: $").append(String.format("%.2f", currentOrder.getSubtotal())).append("\n");
            receipt.append("Tax (8.25%): $").append(String.format("%.2f", currentOrder.getTax())).append("\n");
            receipt.append("TOTAL: $").append(String.format("%.2f", currentOrder.getTotalAmount())).append("\n");
            
            if (lowInventoryWarning.length() > 0) {
                receipt.append("\n LOW INVENTORY WARNING:\n").append(lowInventoryWarning);
            }
            
            showInfo("Checkout Successful", receipt.toString());
            
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
            notesList.add("Order #" + orderId + " completed");
            
            // Force immediate sync check after checkout
            checkInventoryAndUpdateButtons();
            updateReceipt();
            
        } catch (SQLException e) {
            try {
                DatabaseConnection.getConnection().rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            showError("Checkout Error", "Failed to process checkout: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Cleanup when controller is destroyed
     */
    public void cleanup() {
        if (syncTimer != null) {
            syncTimer.cancel();
            System.out.println("Real-time sync stopped");
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
