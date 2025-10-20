import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.layout.HBox;
import javafx.geometry.Insets;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CashierMenuController {
    
    @FXML
    private ListView<String> notesListView;
    
    @FXML
    private ListView<String> receiptListView;
    
    @FXML
    private ComboBox<String> personSelector;
    
    @FXML
    private VBox menuContainer;

    @FXML 
    private javafx.scene.control.MenuItem swapPage;
    
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
            personSelector.setOnAction(e -> {
                currentPerson = personSelector.getValue();
                notesList.add("Selected: " + currentPerson);
            });
        }
        
        loadMenuItemsFromDatabase();
        generateMenuButtons();
        updateReceipt();
    }

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
            itemBtn.setOnAction(e -> addItemByName(item.getName()));
            
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
            notesList.add("+ Added " + item.getName());
            updateReceipt();
        } else {
            showError("Item Not Found", "Could not find " + itemName);
        }
    }

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
            
            int batchCount = 0;
            for (MenuItem item : currentOrder.getAllItems()) {
                orderStmt.setInt(1, nextId++);
                orderStmt.setDate(2, Date.valueOf(currentDate));
                orderStmt.setTime(3, Time.valueOf(currentTime));
                orderStmt.setString(4, item.getName());
                orderStmt.setInt(5, 1); 
                orderStmt.setDouble(6, item.getPrice());
                orderStmt.addBatch();
                batchCount++;
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
            showError("Checkout Error", "Failed to process: " + e.getMessage());
        }
    }
    
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
            
            table.getColumns().addAll(nameCol, qtyCol, priceCol, minCol, statusCol);
            
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
            restockBtn.setOnAction(e -> showRestockDialog(items));
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
    
    private static class RestockData {
        String itemName;
        int quantity;
        
        RestockData(String itemName, int quantity) {
            this.itemName = itemName;
            this.quantity = quantity;
        }
    }
}
