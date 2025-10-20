import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.geometry.Insets;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.swing.table.TableColumn;
import javax.swing.text.TableView;
import javax.swing.text.html.ListView;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

/**
 * The cashier side of the ordering system.
 * This class provides methods for initializing, swapping to manager page, finding person for item,  updating remove button state, setting up the reciept list
 * view, removing selected item, checking inventory availability, loading menu items from database, generating menu buttons, determining category, adding new seasonal item,
 * validating seasonal item data, saving seasonal item to database, adding new person, adding item by name, finding menu key for item name, updating receipt, handling checkout,
 * updating inventory from order, checking low inventory, and viewing inventory management.
 * Additionally it contained a restock dialog for inventory management, as well as displaying errors and showing info alerts on the UI.
 * @author Syed Kazmi and Evan Luu
 * @version 1.0
 * @since 2025-10-01
 */

public class CashierMenuController {
    
    @FXML
    private ListView<String> notesListView; //additional notes list view
    
    @FXML
    private ListView<String> receiptListView;// receipt list view
    
    @FXML
    private ComboBox<String> personSelector;// person selector for menu
    
    @FXML
    private VBox menuContainer; // menu container for buttons

    @FXML
    private Button removeItemBtn; // delete selected item button

    @FXML 
    private javafx.scene.control.MenuItem swapPage; // swap to manager page menu item
    
    private Order currentOrder; // The main object holding all items and traching orders by person
    private ObservableList<String> receiptItems; // Data model for the receipt list view
    private ObservableList<String> notesList; // Data model for the notes list view
    private ObservableList<String> personList; // Data model for the person selector
    private Map<String, MenuItem> menuItemsMap; // Map of menu items loaded from database
    private String currentPerson; // Current selected person for ordering
    private int personCounter = 1; // Counter for person numbering
    private int selectedReceiptIndex = -1; // Index of the currently selected receipt item

    // Public Methods

    /**
     * Intializes the controller and is called immediately after the Cashiermenu.fxml is loaded.
     * <p>
     * It initializes the current order, sets up event handlers, loads menu items from the database, and generates dynamic menu buttons based of the entries in the database.
     */
    @FXML
    public void initialize() {
        currentOrder = new Order();
        swapPage.setOnAction(event -> swapToManager(event));
        receiptItems = FXCollections.observableArrayList();
        notesList = FXCollections.observableArrayList();
        personList = FXCollections.observableArrayList("Person 1");
        menuItemsMap = new HashMap<>();
        currentPerson = "Person 1";
        
        receiptListView.setItems(receiptItems);
        notesListView.setItems(notesList);
        
        if (personSelector != null) {
            personSelector.setItems(personList);
            personSelector.setValue(currentPerson);
            personSelector.setOnAction(_ -> {
                currentPerson = personSelector.getValue();
                notesList.add("Selected: " + currentPerson);
            });
        }
        
        loadMenuItemsFromDatabase();
        generateMenuButtons();
        updateReceipt();
    }
    
    /** Handles page swap from cashier to manager view of the POS
     * It loads MANAGER.fxml specifically and sets the scene to the manager view.
     * 
     * @param event The ActionEvent triggered by selecting the swap page menu item.
     */
    public void swapToManager(ActionEvent event)
    {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/MANAGER.fxml"));
        try
        {
            Parent root = loader.load();
            Stage stage = (Stage)((javafx.scene.control.MenuItem)event.getSource()).getParentPopup().getOwnerWindow();
            stage.setScene(new Scene(root));
            stage.show();
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Finds the person associated with the item at the given receipt.
     * Searches backwards from the receipt index to locate the nearest person header.
     * @param receiptIndex
     * @return The name of the person (e.g. "Person 1") or null if not found.
     */
    private String findPersonForItem(int receiptIndex) {
        for (int i = receiptIndex; i >= 0; i--) {
            String line = receiptItems.get(i);
            if (line.startsWith("------ ") && line.endsWith(" ------")) {
                return line.substring(7, line.length() - 7); 
            }
        }
        return null;
    }

    /**
     * Update the state of the {@code removeItemBtn} based on the selected receipt item.
     * Food item lines enable the button, while headers, subtotals, taxes, and empty lines disable it.
     */
    private void updateRemoveButtonState() {
        if (selectedReceiptIndex < 0 || selectedReceiptIndex >= receiptItems.size()) {
            if (removeItemBtn != null) removeItemBtn.setDisable(true);
            return;
        }
        
        String selected = receiptItems.get(selectedReceiptIndex);
        
        if (selected.startsWith("------ ") || selected.contains("Subtotal:") || 
            selected.contains("Tax") || selected.startsWith("_") || selected.isEmpty()) {
            if (removeItemBtn != null) removeItemBtn.setDisable(true);
        } else {
            if (removeItemBtn != null) removeItemBtn.setDisable(false);
        }
    }

    /**
     * Sets the mouse click event handler for the receipt list view.
     * Updates the remove button state based on the selected item.
     */
    private void setupReceiptListView() {
        receiptListView.setOnMouseClicked(_ -> {
            selectedReceiptIndex = receiptListView.getSelectionModel().getSelectedIndex();
            updateRemoveButtonState();
        });
    }

    /**
     * Removes the selected item from the receipt and updates the order accordingly.
     * It validates the selection, identifies the associated person, removes the item from the order, while also updating the receipt display.
     */
    @FXML
    private void removeSelectedItem() {
        if (selectedReceiptIndex < 0 || selectedReceiptIndex >= receiptItems.size()) {
            showError("Selection Error", "Please select an item to remove");
            return;
        }
        
        String selectedLine = receiptItems.get(selectedReceiptIndex);
        
        if (selectedLine.startsWith("------ ") || selectedLine.contains("Subtotal:") || 
            selectedLine.contains("Tax") || selectedLine.startsWith("_") || selectedLine.isEmpty()) {
            showError("Invalid Selection", "Please select a food item to remove");
            return;
        }
        
        String itemName = selectedLine.trim().split("\\s+\\.+\\$")[0].trim();
        
        String personForItem = findPersonForItem(selectedReceiptIndex);
        
        if (personForItem == null) {
            showError("Error", "Could not determine which person this item belongs to");
            return;
        }
        
        int headerIndex = -1;
        for (int i = selectedReceiptIndex; i >= 0; i--) {
            String line = receiptItems.get(i);
            if (line.startsWith("------ ") && line.endsWith(" ------")) {
                headerIndex = i;
                break;
            }
        }
        if (headerIndex == -1) {
            showError("Error", "Could not find person header for the selected item");
            return;
        }
        
        int itemIndexWithinPerson = -1;
        int count = 0;
        for (int i = headerIndex + 1; i <= selectedReceiptIndex && i < receiptItems.size(); i++) {
            String line = receiptItems.get(i);
            if (line.startsWith("  ") && !line.contains("Subtotal:") && !line.contains("Tax")
                && !line.startsWith("------") && !line.trim().isEmpty()) {
                if (i == selectedReceiptIndex) {
                    itemIndexWithinPerson = count;
                    break;
                }
                count++;
            } else if (line.contains("Subtotal:")) {
                break;
            }
        }
        
        if (itemIndexWithinPerson < 0) {
            showError("Error", "Could not determine item index to remove");
            return;
        }
        
        currentOrder.removeItemFromPerson(personForItem, itemIndexWithinPerson);
        notesList.add("- Removed: " + itemName + " from " + personForItem);
        selectedReceiptIndex = -1;
        updateReceipt();
        updateRemoveButtonState();
    }

    /**
     * Check the inventoryce for all ingredients required for the given menu item.
     * @param item
     * @return A list of missing ingredients with their quantities if insufficient stock is found.
     */
    private List<String> checkInventoryAvailability(MenuItem item) {
    List<String> missingIngredients = new java.util.ArrayList<>();
    
    try {
        Connection conn = DatabaseConnection.getConnection();
        String ingredients = item.getIngredients();
        
        if (ingredients == null || ingredients.isEmpty()) {
            return missingIngredients;
        }
        
        String[] ingredientList = ingredients.split(",");
        
        for (String ingredient : ingredientList) {
            String ingredientName = ingredient.trim();
            
            String query = "SELECT name, quantity FROM inventoryce WHERE LOWER(name) = LOWER(?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, ingredientName);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                int quantity = rs.getInt("quantity");
                String actualName = rs.getString("name");
                
                if (quantity <= 0) {
                    missingIngredients.add(actualName + " (Qty: " + quantity + ")");
                }
            } else {
                missingIngredients.add(ingredientName + " (Not in inventory)");
            }
            
            rs.close();
            stmt.close();
        }
        
    } catch (SQLException e) {
        System.out.println("Error checking inventory: " + e.getMessage());
    }
    
    return missingIngredients;
}
    /**
     * Loads menu items from the database into the {@code menuItemsMap}.
     * It retrieves item details such as name, price, and ingredients, determines their category, and stores them in {@code menuItemsMap} for easy access.
     */
    private void loadMenuItemsFromDatabase() {
        try {
            Connection conn = DatabaseConnection.getConnection();
            String query = "SELECT name, price, ingredients FROM menuce ORDER BY name";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            
            while (rs.next()) {
                String name = rs.getString("name");
                double price = rs.getDouble("price");
                String ingredients = rs.getString("ingredients");
                
                String category = determineCategory(name, ingredients);
                MenuItem item = new MenuItem(name, price, ingredients, category);
                menuItemsMap.put(name.trim().toLowerCase(), item);
                
                System.out.println("Loaded: '" + name + "' -> price: $" + price);
            }
            
            rs.close();
            stmt.close();
            notesList.add("- Menu loaded: " + menuItemsMap.size() + " items");
            
        } catch (Exception e) {
            showError("Database Error", "Failed to load menu items: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Generates dynamic menu buttons based on the items loaded in {@code menuItemsMap}.
     */
    private void generateMenuButtons() {
        if (menuContainer == null) return;
        
        menuContainer.getChildren().clear();
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));
        grid.setStyle("-fx-border-color: #cccccc; -fx-border-width: 1;");
        
        int col = 0;
        int row = 0;
        int itemsPerRow = 2;
        
        List<String> sortedKeys = menuItemsMap.keySet().stream().sorted().toList();
        
        for (String key : sortedKeys) {
            MenuItem item = menuItemsMap.get(key);
            
            Button itemBtn = new Button();
            itemBtn.setText(item.getName() + "\n$" + String.format("%.2f", item.getPrice()));
            itemBtn.setPrefWidth(150);
            itemBtn.setPrefHeight(80);
            itemBtn.setStyle("-fx-font-size: 11; -fx-text-alignment: center; -fx-padding: 10;");
            itemBtn.setWrapText(true);
            itemBtn.setOnAction(_ -> addItemByName(item.getName()));
            
            grid.add(itemBtn, col, row);
            
            col++;
            if (col >= itemsPerRow) {
                col = 0;
                row++;
            }
        }
        
        ScrollPane scrollPane = new ScrollPane(grid);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-control-inner-background: #f5f5f5;");
        
        menuContainer.getChildren().add(scrollPane);
        VBox.setVgrow(scrollPane, javafx.scene.layout.Priority.ALWAYS);
    }
    
    /**
     * Determines the category for a menu item based on its name and ingredients.
     * (e.g., "entree", "side")
     * @param name The name of menu item
     * @param ingredients The ingredients string of the menu item
     * @return A category string such as "entree" or "side"
     */
    private String determineCategory(String name, String ingredients) {
        String nameLower = name.toLowerCase();
        String ingredientsLower = ingredients.toLowerCase();
        
        if (nameLower.contains("rice") || nameLower.contains("noodle")) {
            return "side";
        } else if (ingredientsLower.contains("chicken") || ingredientsLower.contains("beef")) {
            return "entree";
        }
        return "side";
    }
    
    /**
     * Displays text input dialog to add a new seasonal menu item along with (name, price, ingredients) needed for the item.
     * When its submitted, it validates the inputs and saves the new item to the database if valid for both menuce and inventoryce.
     */
    @FXML
    private void addNewSeasonalItem() {
        Dialog<SeasonalItemData> dialog = new Dialog<>();
        dialog.setTitle("Add New Seasonal Item");
        dialog.setHeaderText("Add a new seasonal menu item");
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        TextField nameField = new TextField();
        nameField.setPromptText("Item name");
        TextField priceField = new TextField();
        priceField.setPromptText("Price");
        TextField ingredientsField = new TextField();
        ingredientsField.setPromptText("Ingredients");
        
        ComboBox<String> categoryCombo = new ComboBox<>();
        categoryCombo.setItems(FXCollections.observableArrayList("entree", "side", "beverage"));
        categoryCombo.setValue("entree");
        
        grid.add(new Label("Item Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Price:"), 0, 1);
        grid.add(priceField, 1, 1);
        grid.add(new Label("Ingredients:"), 0, 2);
        grid.add(ingredientsField, 1, 2);
        grid.add(new Label("Category:"), 0, 3);
        grid.add(categoryCombo, 1, 3);
        
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return new SeasonalItemData(nameField.getText(), priceField.getText(), 
                    ingredientsField.getText(), categoryCombo.getValue());
            }
            return null;
        });
        
        Optional<SeasonalItemData> result = dialog.showAndWait();
        result.ifPresent(data -> {
            if (validateSeasonalItemData(data)) {
                saveSeasonalItemToDatabase(data);
                generateMenuButtons();
            }
        });
    }
    
    /**
     * Validates the seasonal item data from {@code addNewSeasonalItem} to make sure name is present and price is a valid number.
     * @param data The seasonal item data
     * @return true if valid, false otherwise (Shows a error alert if false)
     */
    private boolean validateSeasonalItemData(SeasonalItemData data) {
        if (data.name == null || data.name.trim().isEmpty()) {
            showError("Invalid Input", "Item name cannot be empty");
            return false;
        }
        try {
            Double.parseDouble(data.price);
        } catch (NumberFormatException e) {
            showError("Invalid Input", "Price must be a valid number");
            return false;
        }
        if (data.ingredients == null || data.ingredients.trim().isEmpty()) {
            showError("Invalid Input", "Ingredients cannot be empty");
            return false;
        }
        return true;
    }
    
    /**
     * Persists the new seasonal item data to both menuce and inventoryce tables in the database.
     * @param data The validated seasonal item data
     */
    private void saveSeasonalItemToDatabase(SeasonalItemData data) {
        try {
            Connection conn = DatabaseConnection.getConnection();
            
            String checkQuery = "SELECT COUNT(*) FROM menuce WHERE name = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkQuery);
            checkStmt.setString(1, data.name.trim());
            ResultSet checkRs = checkStmt.executeQuery();
            
            if (checkRs.next() && checkRs.getInt(1) > 0) {
                showError("Duplicate Item", "Item already exists");
                checkRs.close();
                checkStmt.close();
                return;
            }
            checkRs.close();
            checkStmt.close();
            
            String itemQuery = "INSERT INTO menuce (name, price, ingredients) VALUES (?, ?, ?)";
            PreparedStatement itemStmt = conn.prepareStatement(itemQuery);
            itemStmt.setString(1, data.name.trim());
            itemStmt.setDouble(2, Double.parseDouble(data.price.trim()));
            itemStmt.setString(3, data.ingredients.trim());
            itemStmt.executeUpdate();
            itemStmt.close();
            
            String inventoryQuery = "INSERT INTO inventoryce (name, quantity, unit_price, minimum) VALUES (?, ?, ?, ?)";
            PreparedStatement invStmt = conn.prepareStatement(inventoryQuery);
            invStmt.setString(1, data.name.trim());
            invStmt.setInt(2, 0);
            invStmt.setDouble(3, Double.parseDouble(data.price.trim()));
            invStmt.setInt(4, 100);
            invStmt.executeUpdate();
            invStmt.close();
            
            MenuItem newItem = new MenuItem(data.name.trim(), Double.parseDouble(data.price.trim()), 
                data.ingredients.trim(), data.category);
            menuItemsMap.put(data.name.trim().toLowerCase(), newItem);
            
            notesList.add("NEW SEASONAL ITEM: " + data.name);
            showInfo("Success", "Seasonal item added!");
            
        } catch (SQLException e) {
            showError("Database Error", "Failed to add item: " + e.getMessage());
        }
    }
    
    /**
     * Adds a new person to the order.
     * It increments the person counter, updates the person selector, and refreshes the receipt display.
     */
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

    /**
     * Adds an item to the current person's order after checking inventory availability.
     * It uses a key search to find the correct item from the {@code menuItemsMap} and updates the receipt display.
     * @param itemName
     */
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
             List<String> missingIngredients = checkInventoryAvailability(item);
        if (!missingIngredients.isEmpty()) {
            StringBuilder msg = new StringBuilder("Cannot add " + item.getName() + "\n\nInsufficient inventory:\n");
            for (String ing : missingIngredients) {
                msg.append("• ").append(ing).append("\n");
            }
            showError("Inventory Error", msg.toString());
            notesList.add("blocked: " + item.getName() + " (no stock)");
            return;
        }
            currentOrder.addItemToPerson(currentPerson, item);
            notesList.add("+ Added " + item.getName());
            updateReceipt();
        } else {
            showError("item Not Found", "could not find " + itemName);
        }
    }

    /**
     * Searches the {@code menuItemsMap} for a menu item key based on the provided name,
     * accomodating for minor variations such as word order or partial matches.
     * @param itemName The name of the item provided by user or button
     * @return The matched menu item key, or null if no match is found.
     */
    private String findMenuKeyFor(String itemName) {
        if (itemName == null) return null;
        String key = itemName.trim().toLowerCase().replaceAll("[^a-z0-9\\s]", "");

        if (menuItemsMap.containsKey(key)) return key;

        String[] parts = key.split("\\s+");
        if (parts.length == 2) {
            String swapped = parts[1] + " " + parts[0];
            if (menuItemsMap.containsKey(swapped)) return swapped;
        }

        String[] tokens = key.split("\\s+");
        for (String menuKey : menuItemsMap.keySet()) {
            boolean all = true;
            for (String t : tokens) {
                if (!menuKey.contains(t)) { all = false; break; }
            }
            if (all) return menuKey;
        }

        for (String menuKey : menuItemsMap.keySet()) {
            if (menuKey.contains(key) || key.contains(menuKey)) return menuKey;
        }

        return null;
    }
    
    /**
     * Clears and repopulates {@code recieptListView} based on the current order.
     * It constructs the reciept by iterating through each person's items, calculating subtotals, taxes, and the total amount.
     */
    private void updateReceipt() {
        receiptItems.clear();
        
        Map<String, List<MenuItem>> personOrders = currentOrder.getPersonOrders();
        
        if (personOrders.isEmpty()) {
            receiptItems.add("no items added yet");
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
    
    /**
     * Processes the current order for checkout.
     * It validates the order, saves it to the database, updates inventory, checks for low inventory items, and displays the receipt.
     * Additionally, it resets the order for the next transaction. The database operations are performed within a transaction to ensure data integrity.
     */
    @FXML
    private void handleCheckout() {
        if (currentOrder.isEmpty()) {
            showError("Empty Order", "Please add items");
            return;
        }
        
        try {
            Class.forName("org.postgresql.Driver");
            conn = DriverManager.getConnection(DB_URL, my.user, my.pswd);
            conn.setAutoCommit(false);
            
            LocalDate currentDate = LocalDate.now();
            LocalTime currentTime = LocalTime.now();
            
            Statement lockStmt = conn.createStatement();
            lockStmt.execute("LOCK TABLE orderhistoryce IN EXCLUSIVE MODE");
            lockStmt.close();
            
            String maxIdQuery = "SELECT COALESCE(MAX(id), 0) + 1 as next_id FROM orderhistoryce";
            Statement maxIdStmt = conn.createStatement();
            ResultSet maxIdRs = maxIdStmt.executeQuery(maxIdQuery);
            int nextId = 1;
            if (maxIdRs.next()) {
                nextId = maxIdRs.getInt("next_id");
            }
            maxIdRs.close();
            maxIdStmt.close();
            
            String orderQuery = "INSERT INTO orderhistoryce (id, date, time, item, qty, price) VALUES (?, ?, ?, ?, ?, ?)";
            PreparedStatement orderStmt = conn.prepareStatement(orderQuery);
            
            for (MenuItem item : currentOrder.getAllItems()) {
                orderStmt.setInt(1, nextId++);
                orderStmt.setDate(2, Date.valueOf(currentDate));
                orderStmt.setTime(3, Time.valueOf(currentTime));
                orderStmt.setString(4, item.getName());
                orderStmt.setInt(5, 1); 
                orderStmt.setDouble(6, item.getPrice());
                orderStmt.addBatch();
            }
            
            orderStmt.executeBatch();
            orderStmt.close();
            
            updateInventoryFromOrder(conn, currentOrder);
            
            List<String> lowInventoryItems = checkLowInventory(conn);
            
            conn.commit();
            conn.setAutoCommit(true);
            
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
            
            if (lowInventoryItems != null && !lowInventoryItems.isEmpty()) {
                StringBuilder warning = new StringBuilder("LOW INVENTORY:\n\n");
                for (String item : lowInventoryItems) {
                    warning.append("• ").append(item).append("\n");
                }
                showError("Low Inventory Warning", warning.toString());
                notesList.add("LOW INVENTORY: " + lowInventoryItems.size() + " items");
            }
            
            currentOrder = new Order();
            personCounter = 1;
            currentPerson = "Person 1";
            personList.clear();
            personList.add("Person 1");
            if (personSelector != null) {
                personSelector.setValue(currentPerson);
            }
            notesList.add("Ready for new order");
            updateReceipt();
            
        } catch (Exception e) {
            try {
                Class.forName("org.postgresql.Driver");
                Connection conn = DriverManager.getConnection(DB_URL, my.user, my.pswd);
                conn.rollback();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            showError("checkout Error", "failed to process: " + e.getMessage());
        }
    }
    
    /**
     * Updates the inventory by decrementing the quantities based on the items in the order.
     * @param conn The active database connection
     * @param order The completed  {@code Order} objects
     */
    private void updateInventoryFromOrder(Connection conn, Order order) {
        try {
            List<MenuItem> allItems = order.getAllItems();
            
            for (MenuItem item : allItems) {
                String ingredients = item.getIngredients();
                if (ingredients == null || ingredients.isEmpty()) continue;
                
                String[] ingredientList = ingredients.split(",");
                
                for (String ingredient : ingredientList) {
                    String ingredientName = ingredient.trim();
                    
                    String findQuery = "SELECT name FROM inventoryce WHERE LOWER(name) = LOWER(?)";
                    PreparedStatement findStmt = conn.prepareStatement(findQuery);
                    findStmt.setString(1, ingredientName);
                    ResultSet findRs = findStmt.executeQuery();
                    
                    if (findRs.next()) {
                        String actualName = findRs.getString("name");
                        
                        String updateQuery = "UPDATE inventoryce SET quantity = quantity - 1 WHERE name = ?";
                        PreparedStatement updateStmt = conn.prepareStatement(updateQuery);
                        updateStmt.setString(1, actualName);
                        updateStmt.executeUpdate();
                        updateStmt.close();
                    }
                    findRs.close();
                    findStmt.close();
                }
            }
        } catch (SQLException e) {
            System.out.println("Warning: Failed to update inventory: " + e.getMessage());
        }
    }
    
    /**
     * Checks the inventory for items that are at or below their minimum threshold.
     * @param conn The active database connection
     * @return A list of low inventory item names with their quantities.
     */
    private List<String> checkLowInventory(Connection conn) {
        List<String> lowItems = new java.util.ArrayList<>();
        try {
            String query = "SELECT name, quantity, minimum FROM inventoryce WHERE quantity <= minimum ORDER BY name";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            
            while (rs.next()) {
                String name = rs.getString("name");
                int quantity = rs.getInt("quantity");
                int minimum = rs.getInt("minimum");
                lowItems.add(name + " (Qty: " + quantity + "/" + minimum + ")");
            }
            
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            System.out.println("Warning: Failed to check inventory: " + e.getMessage());
        }
        return lowItems;
    }
    
    /**
     * Displays the inventory management dialog.
     * Loads inventory data from the databas, displays it in a table, and provides an option to restock items.
     */
    @FXML
    private void viewInventoryManagement() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Inventory Management");
        dialog.setHeaderText("View and Update Inventory");
        dialog.setWidth(900);
        dialog.setHeight(600);
        
        VBox content = new VBox(10);
        content.setPadding(new Insets(15));
        
        try {
            Connection conn = DatabaseConnection.getConnection();
            String query = "SELECT name, quantity, unit_price, minimum FROM inventoryce ORDER BY name";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            
            TableView<InventoryItem> table = new TableView<>();
            ObservableList<InventoryItem> items = FXCollections.observableArrayList();
            
            TableColumn<InventoryItem, String> nameCol = new TableColumn<>("Item Name");
            nameCol.setCellValueFactory(p -> new javafx.beans.property.SimpleStringProperty(p.getValue().name));
            nameCol.setPrefWidth(200);
            
            TableColumn<InventoryItem, Integer> qtyCol = new TableColumn<>("Quantity");
            qtyCol.setCellValueFactory(p -> new javafx.beans.property.SimpleObjectProperty<>(p.getValue().quantity));
            qtyCol.setPrefWidth(80);
            
            TableColumn<InventoryItem, Double> priceCol = new TableColumn<>("Unit Price");
            priceCol.setCellValueFactory(p -> new javafx.beans.property.SimpleObjectProperty<>(p.getValue().unitPrice));
            priceCol.setPrefWidth(100);
            
            TableColumn<InventoryItem, Integer> minCol = new TableColumn<>("Minimum");
            minCol.setCellValueFactory(p -> new javafx.beans.property.SimpleObjectProperty<>(p.getValue().minimum));
            minCol.setPrefWidth(80);
            
            TableColumn<InventoryItem, String> statusCol = new TableColumn<>("Status");
            statusCol.setCellValueFactory(p -> {
                String status = p.getValue().quantity <= p.getValue().minimum ? "LOW" : "OK";
                return new javafx.beans.property.SimpleStringProperty(status);
            });
            statusCol.setPrefWidth(100);
            
            table.getColumns().addAll(java.util.Arrays.asList(nameCol, qtyCol, priceCol, minCol, statusCol));
            
            while (rs.next()) {
                items.add(new InventoryItem(rs.getString("name"), rs.getInt("quantity"), 
                    rs.getDouble("unit_price"), rs.getInt("minimum")));
            }
            
            table.setItems(items);
            table.setPrefHeight(450);
            
            rs.close();
            stmt.close();
            
            content.getChildren().add(table);
            
            HBox buttonBox = new HBox(10);
            buttonBox.setPadding(new Insets(10));
            Button restockBtn = new Button("Restock Item");
            restockBtn.setStyle("-fx-font-size: 12; -fx-padding: 8;");
            restockBtn.setOnAction(_ -> showRestockDialog(items));
            buttonBox.getChildren().add(restockBtn);
            content.getChildren().add(buttonBox);
            
        } catch (SQLException e) {
            Label error = new Label("Error loading inventory: " + e.getMessage());
            content.getChildren().add(error);
        }
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }
    
    /**
     * Displays a dialog to restock an inventory item.
     * @param items The list of inventory items to choose from.
     */
    private void showRestockDialog(ObservableList<InventoryItem> items) {
        Dialog<RestockData> dialog = new Dialog<>();
        dialog.setTitle("Restock Inventory Item");
        dialog.setHeaderText("Add quantity to an inventory item");
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        ComboBox<String> itemCombo = new ComboBox<>();
        ObservableList<String> itemNames = FXCollections.observableArrayList();
        for (InventoryItem item : items) {
            itemNames.add(item.name + " (Current: " + item.quantity + ")");
        }
        itemCombo.setItems(itemNames);
        
        TextField quantityField = new TextField();
        quantityField.setPromptText("Quantity to add");
        
        grid.add(new Label("Item:"), 0, 0);
        grid.add(itemCombo, 1, 0);
        grid.add(new Label("Add Quantity:"), 0, 1);
        grid.add(quantityField, 1, 1);
        
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                String selected = itemCombo.getValue();
                if (selected == null || selected.isEmpty()) {
                    showError("Input Error", "Please select an item");
                    return null;
                }
                String itemName = selected.substring(0, selected.indexOf(" (Current"));
                try {
                    int quantity = Integer.parseInt(quantityField.getText());
                    return new RestockData(itemName, quantity);
                } catch (NumberFormatException e) {
                    showError("Input Error", "Quantity must be a number");
                }
            }
            return null;
        });
        
        Optional<RestockData> result = dialog.showAndWait();
        result.ifPresent(data -> updateInventoryQuantity(data.itemName, data.quantity));
    }
    
    /**
     * Updates the inventory quantity for a specific item in the database.
     * @param itemName Name of the item to restock
     * @param quantityToAdd The amount to add to the current quantity
     */
    private void updateInventoryQuantity(String itemName, int quantityToAdd) {
        try {
            Connection conn = DatabaseConnection.getConnection();
            String query = "UPDATE inventoryce SET quantity = quantity + ? WHERE name = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, quantityToAdd);
            stmt.setString(2, itemName);
            int updated = stmt.executeUpdate();
            stmt.close();
            
            if (updated > 0) {
                notesList.add("Restocked: " + itemName + " (+"+quantityToAdd+")");
                showInfo("Success", "Added " + quantityToAdd + " units");
            }
        } catch (SQLException e) {
            showError("Database Error", "Failed to update: " + e.getMessage());
        }
    }
    
    /**
     * Displays an error alert with the given title and message.
     * @param title Title of alert window
     * @param message The content message
     */
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Displays an information alert with the given title and message.
     * @param title Title of alert window
     * @param message The content message
     */
    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    // Helper data classes

    /**
     * Data class to hold seasonal item information.
     */
    private static class SeasonalItemData {
        String name;
        String price;
        String ingredients;
        String category;
        
        SeasonalItemData(String name, String price, String ingredients, String category) {
            this.name = name;
            this.price = price;
            this.ingredients = ingredients;
            this.category = category;
        }
    }
    
    /**
     * Data class to represent an inventory item.
     */
    private static class InventoryItem {
        String name;
        int quantity;
        double unitPrice;
        int minimum;
        
        InventoryItem(String name, int quantity, double unitPrice, int minimum) {
            this.name = name;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.minimum = minimum;
        }
    }
    
    /**
     * Data class to hold restock information.
     * Specifically its input fields from the restock dialog.
     */
    private static class RestockData {
        String itemName;
        int quantity;
        
        RestockData(String itemName, int quantity) {
            this.itemName = itemName;
            this.quantity = quantity;
        }
    }
}
