import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;

/**
 * Class responsible for storing the orders for any given person
 */
public class Order {
    private Map<String, List<MenuItem>> personOrders; 
    private String notes;
    private LocalDateTime orderTime;
    private double subtotal;
    private double tax;
    private double totalAmount;
    private String status;
    private static final double TAX_RATE = 0.0825;// 8.25% 
    
    /**
     * Default constructor
     */
    public Order() {
        this.personOrders = new HashMap<>();
        this.orderTime = LocalDateTime.now();
        this.status = "pending";
        this.notes = "";
        this.subtotal = 0.0;
        this.tax = 0.0;
        this.totalAmount = 0.0;
    }
    
    /**
     * Adds a menu item to a person's order
     * 
     * @param personName name of person ordering
     * @param item menu item being ordered
     */
    public void addItemToPerson(String personName, MenuItem item) {
        personOrders.putIfAbsent(personName, new ArrayList<>());
        personOrders.get(personName).add(item);
        calculateTotal();
    }
    
    /**
     * Cancels an item a person is ordering
     * @param personName name of person ordering
     * @param itemIndex index of ordered item to be removed
     */
    public void removeItemFromPerson(String personName, int itemIndex) {
        if (personOrders.containsKey(personName)) {
            List<MenuItem> items = personOrders.get(personName);
            if (itemIndex >= 0 && itemIndex < items.size()) {
                items.remove(itemIndex);
                if (items.isEmpty()) {
                    personOrders.remove(personName);
                }
                calculateTotal();
            }}
    }
    
    /**
     * Gets total before tax for a person's order
     * @param personName name of person ordering
     * @return double
     */
    public double getPersonSubtotal(String personName) {
        if (!personOrders.containsKey(personName)) {
            return 0.0;
        }
        return personOrders.get(personName).stream()
                .mapToDouble(MenuItem::getPrice)
                .sum();
    }
    
    /**
     * Calculates total after tax
     */
    public void calculateTotal() {
        subtotal = personOrders.values().stream()
                .flatMap(List::stream)
                .mapToDouble(MenuItem::getPrice)
                .sum();
        tax = subtotal * TAX_RATE;
        totalAmount = subtotal + tax;
    }
    
    /**
     * Returns all menu items
     * 
     * @return List of menu items
     */
    public List<MenuItem> getAllItems() {
        List<MenuItem> allItems = new ArrayList<>();
        personOrders.values().forEach(allItems::addAll);
        return allItems;
    }
    
    /**
     * Function to determine if an order is empty
     * @return True if empty
     */
    public boolean isEmpty() {
        return personOrders.isEmpty();
    }
    
    /**
     * Returns a person's orders
     * @return map containing name and list of menu items
     */
    public Map<String, List<MenuItem>> getPersonOrders() { return personOrders; }
    
    /**
     * Returns additional notes as specified by customer
     * @return String
     */
    public String getNotes() { return notes; }
    
    /**
     * Sets notes for customer
     * @param notes new notes
     */
    public void setNotes(String notes) { this.notes = notes; }
    
    /**
     * Gets current order time
     * @return LocalDateTime
     */
    public LocalDateTime getOrderTime() { return orderTime; }
    /**
     * Sets order time
     * @param orderTime new LocalDateTime to set
     */
    public void setOrderTime(LocalDateTime orderTime) { this.orderTime = orderTime; }
    
    /**
     * Getter function for subtotal
     * @return double
     */
    public double getSubtotal() { return subtotal; }
    /**
     * Getter function for total tax
     * @return double
     */
    public double getTax() { return tax; }
    /**
     * Getter function for sum of subtotal and tax
     * @return double
     */
    public double getTotalAmount() { return totalAmount; }
    
    /**
     * Getter function for current status of order
     * @return String
     */
    public String getStatus() { return status; }
    /**
     * Setter function for status
     * @param status String
     */
    public void setStatus(String status) { this.status = status; }
}