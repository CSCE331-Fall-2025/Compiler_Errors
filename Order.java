package src;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;

public class Order {
    private Map<String, List<MenuItem>> personOrders; 
    private String notes;
    private LocalDateTime orderTime;
    private double subtotal;
    private double tax;
    private double totalAmount;
    private String status;
    private static final double TAX_RATE = 0.0825;// 8.25% 
    
    public Order() {
        this.personOrders = new HashMap<>();
        this.orderTime = LocalDateTime.now();
        this.status = "pending";
        this.notes = "";
        this.subtotal = 0.0;
        this.tax = 0.0;
        this.totalAmount = 0.0;
    }
    
    public void addItemToPerson(String personName, MenuItem item) {
        personOrders.putIfAbsent(personName, new ArrayList<>());
        personOrders.get(personName).add(item);
        calculateTotal();
    }
    
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
    
    public double getPersonSubtotal(String personName) {
        if (!personOrders.containsKey(personName)) {
            return 0.0;
        }
        return personOrders.get(personName).stream()
                .mapToDouble(MenuItem::getPrice)
                .sum();
    }
    
    public void calculateTotal() {
        subtotal = personOrders.values().stream()
                .flatMap(List::stream)
                .mapToDouble(MenuItem::getPrice)
                .sum();
        tax = subtotal * TAX_RATE;
        totalAmount = subtotal + tax;
    }
    
    public List<MenuItem> getAllItems() {
        List<MenuItem> allItems = new ArrayList<>();
        personOrders.values().forEach(allItems::addAll);
        return allItems;
    }
    
    public boolean isEmpty() {
        return personOrders.isEmpty();
    }
    
    public Map<String, List<MenuItem>> getPersonOrders() { return personOrders; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public LocalDateTime getOrderTime() { return orderTime; }
    public void setOrderTime(LocalDateTime orderTime) { this.orderTime = orderTime; }
    
    public double getSubtotal() { return subtotal; }
    public double getTax() { return tax; }
    public double getTotalAmount() { return totalAmount; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}