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

/**
 * Controller for the Cashier Menu UI.
 *
 * <p>Loads menu items from the database, renders them into a scrollable
 * menu, manages the current order (per-person), performs inventory
 * availability checks, and provides dialogs for inventory management
 * and adding seasonal menu items.</p>
 */
public class CashierMenuController {
    /**
     * No-argument constructor required by JavaFX when instantiating the controller.
     * Providing an explicit constructor so the generated Javadoc includes a
     * documented constructor instead of a default undocumented one.
     */
    public CashierMenuController() {}
    
    @FXML
    private ListView<String> notesListView;
    
    @FXML
    private ListView<String> receiptListView;
    
    @FXML
    private ComboBox<String> personSelector;
    
    @FXML
    private VBox menuContainer;
    
    @FXML
    private Button removeItemBtn;
    
    /** The current Order being constructed (supports multiple persons). */
    private Order currentOrder;
    /** Backing list for the receipt display ListView. */
    private ObservableList<String> receiptItems;
    /** Backing list for the notes display ListView. */
    private ObservableList<String> notesList;
    /** Observable list containing the person labels ("Person 1", ...). */
    private ObservableList<String> personList;
    /** Map of normalized menu key -> MenuItem for fast item lookup. */
    private Map<String, MenuItem> menuItemsMap;
    /** The currently selected person for adding items. */
    private String currentPerson;
    /** Counter used to assign new person labels. */
    private int personCounter = 1;
    /** The currently selected index in the receiptItems list (-1 when none). */
    private int selectedReceiptIndex = -1;
    
    /**
     * JavaFX initialization callback.
     *
     * <p>Sets up the initial controller state: creates the order and
     * observable lists, configures the person selector and receipt list
     * handlers, loads menu items from the database, and builds the
     * menu UI.</p>
     */
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
        
        if (personSelector != null) {
            personSelector.setItems(personList);
            personSelector.setValue(currentPerson);
            personSelector.setOnAction(_ -> {
                currentPerson = personSelector.getValue();
                notesList.add("Selected: " + currentPerson);
            });
        }
        
        setupReceiptListView();
        
        if (removeItemBtn != null) {
            removeItemBtn.setDisable(true);
        }
        
        loadMenuItemsFromDatabase();
        generateMenuButtons();
        updateReceipt();
    }
    
    /**
     * Sets up the receipt list view click handler.
     * 
     * <p>Configures the mouse click event handler for the receipt list view
     * to update the selected item index and enable/disable the remove button
     * based on the selection.</p>
     */
    private void setupReceiptListView() {
        receiptListView.setOnMouseClicked(_ -> {
            selectedReceiptIndex = receiptListView.getSelectionModel().getSelectedIndex();
            updateRemoveButtonState();
        });
    }
    
    /**
     * Updates the state of the remove item button.
     * 
     * <p>Enables or disables the remove button based on the current selection
     * in the receipt list view. The button is disabled if:
     * - No item is selected
     * - The selected item is a header or separator
     * - The selected item is a subtotal or tax line</p>
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
     * Removes the currently selected item from the order.
     * 
     * <p>This method is called when the remove item button is clicked. It:
     * 1. Validates the selection is a valid item
     * 2. Determines which person's order contains the item
     * 3. Finds the item's index within that person's order
     * 4. Removes the item and updates the receipt display</p>
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
     * Finds the person who owns an item in the receipt.
     * 
     * <p>This method searches backwards from the given receipt index to find
     * the person header that contains this item. Person headers are formatted
     * as "------ Person Name ------".</p>
     * 
     * @param receiptIndex The index of the item in the receipt list
     * @return The name of the person who owns the item, or null if not found
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
     * Checks if all ingredients for a menu item are available in inventory.
     * 
     * <p>This method queries the inventory database to check if each ingredient
     * required for the menu item has sufficient quantity (greater than 0).
     * The inventory check is case-insensitive.</p>
     * 
     * @param item The menu item to check ingredients for
     * @return A list of missing or insufficient ingredients, with their quantities
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
     * Loads all menu items from the database.
     * 
     * <p>This method:
     * 1. Queries the menu table for all items
     * 2. Creates MenuItem objects for each row
     * 3. Determines the category based on name/ingredients
     * 4. Stores items in the menuItemsMap for quick lookup
     * 5. Logs the loaded items to the notes list</p>
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
            
        } catch (SQLException e) {
            showError("Database Error", "Failed to load menu items: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Generates the menu buttons UI.
     * 
     * <p>Creates a scrollable grid of buttons for each menu item, where:
     * - Each button shows the item name and price
     * - Buttons are arranged in a grid with 2 columns
     * - Clicking a button adds the item to the current order
     * - The grid is placed in a scroll pane for overflow handling</p>
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
     * Determines the category of a menu item based on its name and ingredients.
     * 
     * <p>Categories are determined by these rules:
     * - Items with "rice" or "noodle" in the name are sides
     * - Items with "chicken" or "beef" in ingredients are entrees
     * - Default category is "side"</p>
     * 
     * @param name The name of the menu item
     * @param ingredients The comma-separated list of ingredients
     * @return The determined category ("side" or "entree")
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
     * Shows a dialog to add a new seasonal menu item.
     * 
     * <p>This method:
     * 1. Creates a dialog with fields for item details
     * 2. Validates the input data
     * 3. Saves the new item to the database
     * 4. Updates the menu buttons to include the new item
     * 5. Adds corresponding inventory entry</p>
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
     * Validates the data for a new seasonal menu item.
     * 
     * <p>Checks that:
     * - Item name is not empty
     * - Price is a valid number
     * - Ingredients list is not empty</p>
     * 
     * @param data The seasonal item data to validate
     * @return true if the data is valid, false otherwise
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
     * Saves a new seasonal menu item to the database.
     * 
     * <p>This method:
     * 1. Checks for duplicate items
     * 2. Inserts the new item into the menu table
     * 3. Creates a corresponding inventory entry
     * 4. Updates the local menu items map
     * 5. Logs the addition to the notes list</p>
     * 
     * @param data The seasonal item data to save
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
     * Adds a new person to the current order.
     * 
     * <p>This method:
     * 1. Increments the person counter
     * 2. Creates a new person entry ("Person X")
     * 3. Updates the person selector combobox
     * 4. Logs the addition to the notes list
     * 5. Updates the receipt display</p>
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
     * Adds a menu item to the current order by its name.
     * 
     * <p>This method:
     * 1. Attempts to find the item in the menu items map
     * 2. Uses fuzzy matching if exact match fails
     * 3. Checks inventory availability
     * 4. Adds the item to the current person's order
     * 5. Updates the receipt display</p>
     * 
     * @param itemName The name of the item to add
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
     * Finds a menu item key using fuzzy matching.
     * 
     * <p>This method tries several matching strategies in order:
     * 1. Exact match after normalization
     * 2. Word order reversal (e.g., "chicken orange" -> "orange chicken")
     * 3. All words contained check
     * 4. Substring containment check</p>
     * 
     * @param itemName The name to search for
     * @return The matching menu key, or null if no match found
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
     * Updates the receipt display with current order information.
     * 
     * <p>This method generates a formatted receipt showing:
     * - Each person's order with item names and prices
     * - Individual subtotals per person
     * - Overall subtotal
     * - Tax amount (8.25%)
     * - Final total amount</p>
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
     * Handles checkout: records the order into the database and updates inventory.
     *
     * <p>This method writes each ordered item to the order history table, updates
     * inventory quantities, checks for low inventory, and resets the UI for a new
     * order when complete. Errors attempt a rollback and display an error dialog.</p>
     */
    @FXML
    private void handleCheckout() {
        if (currentOrder.isEmpty()) {
            showError("Empty Order", "Please add items");
            return;
        }
        
        try {
            Connection conn = DatabaseConnection.getConnection();
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
            
        } catch (SQLException e) {
            try {
                DatabaseConnection.getConnection().rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            showError("checkout Error", "failed to process: " + e.getMessage());
        }
    }
    
    /**
     * Decrements inventory quantities based on the items in an order.
     *
     * @param conn Live DB connection (should be part of the checkout transaction)
     * @param order The processed order whose ingredients should be decremented
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
     * Shows an error alert to the user.
     *
     * @param title Dialog title
     * @param message Human-readable error message
     */
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Shows an informational alert to the user.
     *
     * @param title Dialog title
     * @param message Human-readable informational message
     */
    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Simple holder for data collected from the "Add Seasonal Item" dialog.
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
     * Represents a row in the inventory table used by the inventory management dialog.
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
            this.minimum = minimum;}
        
    }
    
    /**
     * Holder for restock dialog results.
     */
    private static class RestockData{
        String itemName;
        int quantity;
        
        RestockData(String itemName, int quantity){
            this.itemName = itemName;
            this.quantity = quantity;
        }}
}
